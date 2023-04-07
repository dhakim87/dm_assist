const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");
const {GoogleAuth} = require("google-auth-library");
const {google} = require("googleapis");

admin.initializeApp();

// Replace this with the target URL you want to post the data to
const targetUrl = "https://api.openai.com/v1/chat/completions";

exports.chatGPT = functions.https.onRequest(async (req, res) => {
  // Check for the Authorization header
  if (!req.headers.authorization ||
      !req.headers.authorization.startsWith("Bearer ")) {
    res.status(403).send("Unauthorized: No token provided.");
    return;
  }

  const idToken = req.headers.authorization.split("Bearer ")[1];

  try {
    // Authenticate the user and get their UID
    const decodedToken = await admin.auth().verifyIdToken(idToken); // Throws on failure.
    // TODO pass this into chatgpt to track malicious users.
    const uid = decodedToken.uid;

    // Get the request POST data
    const postData = req.body;

    // POST the data to the target URL
    try {
      const response = await axios.post(targetUrl, postData, {
        headers: {
          "Content-Type": "application/json",
          "Authorization": "Bearer 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",
        },
      });

      // Respond with the JSON from the external URL
      res.status(200).json(response.data);
      const totalTokens = response.data.usage.total_tokens;

      // Decrement the user's token count in the Firebase Realtime Database
      const db = admin.database();
      const userTokensRef = db.ref("token/" + uid);

      try {
        userTokensRef.transaction((currentTokens) => {
          if (currentTokens === null) {
            // If the user has no token data yet, set their initial allotment to 10000
            return 10000 - totalTokens;
          }
          // Decrement the tokens by the totalTokens value
          return currentTokens - totalTokens;
        });
      } catch (error) {
        console.error("Error setting user tokens: ", error.message);
        res.status(500).send("Error updating user tokens");
      }
    } catch (error) {
      console.error("Error posting data to external URL: ", error.message);
      res.status(500).send("Error posting data to external URL.");
    }
  } catch (error) {
    console.error("Error verifying token: ", error.message);
    res.status(403).send("Unauthorized: Invalid token.");
  }
});

exports.verifyiap = functions.https.onRequest(async (req, res) => {
  // Check for the Authorization header
  if (!req.headers.authorization ||
      !req.headers.authorization.startsWith("Bearer ")) {
    res.status(403).send("Unauthorized: No token provided.");
    return;
  }

  // Verify and retrieve the firebase UID
  let uid = null;
  try {
    const idToken = req.headers.authorization.split("Bearer ")[1];
    // Authenticate the user and get their UID
    const decodedToken = await admin.auth().verifyIdToken(idToken); // Throws on failure.
    // TODO pass this into chatgpt to track malicious users.
    uid = decodedToken.uid;
  } catch (error) {
    console.error("Error verifying token: ", error.message);
    res.status(403).send("Unauthorized: Invalid token.");
    return;
  }

  // Get the purchase info from android
  let purchase = req.body;

  // Validate the purchase info
  if (purchase.packageName != "com.dm.assist" ||
    purchase.productId != "aitokens_500000" ||
    purchase.purchaseToken == null) {
    console.error("Bad purchase data: ", purchase);
    res.status(400).send("Invalid purchase data");
    return;
  }

  // Log any potential weirdness
  if (purchase.obfuscatedAccountId != uid) {
    console.log("Firebase uid does not match google play id.  Track this down.");
  }

  const auth = new GoogleAuth({
    scopes: "https://www.googleapis.com/auth/androidpublisher",
  });

  const client = await auth.getClient();
  const playDeveloper = google.androidpublisher({
    version: "v3",
    auth: client,
  });

  const productId = purchase.productId;
  const purchaseToken = purchase.purchaseToken;
  try {
    const response = await playDeveloper.purchases.products.get({
      packageName: "com.dm.assist",
      productId: productId,
      token: purchaseToken,
    });
    console.log("Response: ", response);
    purchase = response.data;
  } catch (error) {
    console.error("Error retrieving purchase data from google: ", error.response);
    res.status(500).send("Error retrieving purchase data");
    return; // Oof. Can't accept the purchase right now because we can't talk to google.
  }

  // Add other skus here.
  let tokenAmount = 0;
  if (productId == "aitokens_500000") {
    tokenAmount = 500000;
  }
  if (tokenAmount == 0) {
    console.error("Unknown sku: " + productId);
    res.status(500).send("Unsupported product id");
    return;
  }

  if (purchase.obfuscatedExternalAccountId == null) {
    console.error("No user specified");
    res.status(500).send("Unknown buyer id");
    return;
  }

  // TODO: May require additional obfuscation steps here. Unclear if firebase token is sufficient
  uid = purchase.obfuscatedExternalAccountId;

  const db = admin.database();
  const userTokensRef = db.ref("token/" + uid);
  const userPurchasesRef = db.ref("purchase/" + uid);

  if (purchase.acknowledgementState == 0) {
    // Save the purchase to our database.
    try {
      // Store purchase in database then acknowledge receipt of the purchase
      userPurchasesRef.transaction((purchaseList) => {
        if (purchaseList == null) {
          // First purchase by user.
          return [purchaseToken];
        } else if (purchaseList.indexOf(purchaseToken) == -1) {
          // Add purchase to list.
          purchaseList.push(purchaseToken);
          return purchaseList;
        } else {
          // We have seen this token before.  This is going to be complicated.
          console.log("Received unacknowledged token already in database: " +
            purchaseToken);
          return purchaseList;
        }
      });
    } catch (error) {
      console.error("Error saving token to rtdb: ", error.message);
      res.status(500).send("Failed to record purchase token");
      return; // Oof. Can't ack the purchase because we can't talk to firebase
    }

    // Then ack it on google's servers.
    try {
      const response = await playDeveloper.purchases.products.acknowledge({
        packageName: "com.dm.assist",
        productId: productId,
        token: purchaseToken,
        requestBody: {},
      });
      console.log("Purchase acknowledged", response.status);
    } catch (error) {
      console.error("Error acknowledging purchase: ", error.message);
      res.status(500).send("Failed to acknowledge purchase");
      return; // Dang.  Couldn't ack the purchase.
    }
  }

  // PurchaseState 0 indicates a purchase, 1 indicates a cancellation, which we should not consume
  // PurchaseState 2 is tricky, it is a pending transaction.  We should not consume until the
  // transaction is completed.  Notifications on pending transactions may only be possible through
  // pub/sub with google play.
  if (purchase.purchaseState == 0 && purchase.consumptionState == 0) {
    // If the purchase hasn't been consumed, consume it (for things like token buys at least)
    try {
      userTokensRef.transaction((currentTokens) => {
        if (currentTokens === null) {
          // If the user has no token data yet, set their initial allotment to 10000
          currentTokens = 10000;
        }
        // Increment by sku tokens.
        return currentTokens + tokenAmount;
      });
    } catch (error) {
      console.error("Couldn't increase tokens in firebase: ", error.message);
      res.status(500).send("Failed to add tokens");
      return;
    }

    // Now tell google it has been consumed.
    try {
      const response = await playDeveloper.purchases.products.consume({
        packageName: "com.dm.assist",
        productId: productId,
        token: purchaseToken,
        requestBody: {},
      });
      console.log("Purchase consumed", response);
    } catch (error) {
      console.error("Error consuming purchase: ", error.message);
      res.status(500).send("Failed to consume purchase");
      return; // Dang. Couldn't consume the purchase.
    }
  }

  res.status(200).send("Purchase Complete!");
});
