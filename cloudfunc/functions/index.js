const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");
const {GoogleAuth} = require("google-auth-library");
const {google} = require("googleapis");

admin.initializeApp();

// Replace this with the target URL you want to post the data to
const targetUrl = "https://api.openai.com/v1/chat/completions";
const moderationUrl = "https://api.openai.com/v1/moderations";

function checkAuth(req, res) {
  // Check for the Authorization header
  if (!req.headers.authorization ||
      !req.headers.authorization.startsWith("Bearer ")) {
    res.status(403).send("Unauthorized: No token provided.");
    return false;
  }
  return true;
}

async function getUid(req, res) {
  const idToken = req.headers.authorization.split("Bearer ")[1];
  try {
    // Authenticate the user and get their UID
    const decodedToken = await admin.auth().verifyIdToken(idToken); // Throws on failure.
    // TODO pass this into chatgpt to track malicious users.
    const uid = decodedToken.uid;
    return uid;
  } catch (error) {
    console.error("Error verifying token: ", error.message);
    res.status(403).send("Unauthorized: Invalid token.");
    return null;
  }
}

async function addTokens(uid, toAdd) {
  // Add/Subtract the user's token count in the Firebase Realtime Database
  const db = admin.database();
  const userTokensRef = db.ref("token/" + uid);

  try {
    userTokensRef.transaction((currentTokens) => {
      if (currentTokens === null) {
        // If the user has no token data yet, set their initial allotment to 10000
        return 10000 + toAdd;
      }
      // Add toAdd tokens
      return currentTokens + toAdd;
    });
    return true;
  } catch (error) {
    console.error("Error setting user tokens: ", error.message);
    return false;
  }
}

async function chatWithGPT(postData) {
  try {
    const response = await axios.post(targetUrl, postData, {
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer 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",
      },
    });
    return response;
  } catch (error) {
    console.error("Error posting data to external URL: ", error.message);
    return null;
  }
}

async function checkOpenAIModeration(inputText) {
  try {
    const response = await axios.post(moderationUrl, {input: inputText}, {
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer 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",
      },
    });
    return response;
  } catch (error) {
    console.error("Error posting data to external URL: ", error.message);
    return null;
  }
}

async function getPurchase(playDeveloper, productId, purchaseToken) {
  try {
    const response = await playDeveloper.purchases.products.get({
      packageName: "com.dm.assist",
      productId: productId,
      token: purchaseToken,
    });
    console.log("Response: ", response);
    return response.data;
  } catch (error) {
    console.error("Error retrieving purchase data from google: ", error.response);
    return null; // Oof. Can't accept the purchase right now because we can't talk to google.
  }
}

function productIdToToken(productId) {
  // Add other skus here.
  let tokenAmount = -1;
  if (productId == "aitokens_500000") {
    tokenAmount = 500000;
  }
  if (tokenAmount == -1) {
    console.error("Unknown sku: " + productId);
  }
  return tokenAmount;
}

async function savePurchase(uid, purchaseToken) {
  const db = admin.database();
  const userPurchasesRef = db.ref("purchase/" + uid);

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
        console.log("Received token already in database: " + purchaseToken);
        return purchaseList;
      }
    });
    return true;
  } catch (error) {
    console.error("Error saving token to rtdb: ", error.message);
    return false; // Oof. Can't ack the purchase because we can't talk to firebase
  }
}

async function ackPurchase(playDeveloper, productId, purchaseToken) {
  // Ack it on google's servers.
  try {
    const response = await playDeveloper.purchases.products.acknowledge({
      packageName: "com.dm.assist",
      productId: productId,
      token: purchaseToken,
      requestBody: {},
    });
    console.log("Purchase acknowledged", response.status);
    return true;
  } catch (error) {
    console.error("Error acknowledging purchase: ", error.message);
    return false; // Dang.  Couldn't ack the purchase.
  }
}

async function consumePurchase(playDeveloper, productId, purchaseToken) {
  // Consume it on google's servers.
  try {
    const response = await playDeveloper.purchases.products.consume({
      packageName: "com.dm.assist",
      productId: productId,
      token: purchaseToken,
      requestBody: {},
    });
    console.log("Purchase consumed", response);
    return true;
  } catch (error) {
    console.error("Error consuming purchase: ", error.message);
    return false; // Dang. Couldn't consume the purchase.
  }
}

exports.chatGPT = functions.https.onRequest(async (req, res) => {
  // Check for the Authorization header
  if (!checkAuth(req, res)) {
    return;
  }

  // Authenticate the user and get their UID
  const uid = await getUid(req, res);
  if (uid === null) {
    return;
  }

  console.log("Awaiting chatGPT");
  req.body.user = uid; // Tag the request by the user id for tracking bad behavior.
  const response = await chatWithGPT(req.body);
  if (response === null) {
    res.status(500).send("Error posting data to ChatGPT");
    return;
  }

  const totalTokens = response.data.usage.total_tokens;
  if (!(await addTokens(uid, -totalTokens))) {
    res.status(500).send("Error updating user tokens");
    return;
  }

  const reply = response.data.choices[0].message.content;
  const moderation = await checkOpenAIModeration(reply);
  if (moderation === null) {
    res.status(500).send("Couldn't reach moderation api");
    return;
  }

  console.log(moderation.data);
  if (moderation.data.results[0].flagged) {
    response.data.choices[0].message.content = "DungeonMind here. " +
    "Unfortunately, the response was flagged by our automatic content moderation. " +
    "You'll probably want to tone down your campaign setting to avoid offending your players.";
  }

  // Respond with the JSON from ChatGPT
  res.status(200).json(response.data);
});

exports.verifyiap = functions.https.onRequest(async (req, res) => {
  console.log("auth");
  // Check for the Authorization header
  if (!checkAuth(req, res)) {
    return;
  }
  console.log("Auth 2");
  // Authenticate the user and get their UID
  let uid = await getUid(req, res);
  if (uid == null) {
    return;
  }

  console.log("Validate purchase");
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
  // Argh, did they change this field name?
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

  console.log("Retrieving purchase");
  const productId = purchase.productId;
  const purchaseToken = purchase.purchaseToken;
  purchase = await getPurchase(playDeveloper, productId, purchaseToken);
  if (purchase === null) {
    res.status(500).send("Error retrieving purchase data");
    return;
  }

  if (purchase.obfuscatedExternalAccountId == null) {
    console.error("No user specified");
    res.status(500).send("Unknown buyer id");
    return;
  }

  const tokenAmount = productIdToToken(productId);
  if (tokenAmount == -1) {
    res.status(400).send("Unsupported product id");
    return;
  }

  // TODO: May require additional obfuscation steps here. Unclear if firebase token is sufficient
  uid = purchase.obfuscatedExternalAccountId;
  console.log("Handling Purchase:", purchase);

  // This is an unacknowledged PURCHASE, unclear what we should do with cancellations or pending.
  if (purchase.acknowledgementState == 0 && purchase.purchaseState == 0) {
    if (!(await savePurchase(uid, purchaseToken))) {
      res.status(500).send("Failed to record purchase token");
      return;
    }

    if (!(await ackPurchase(playDeveloper, productId, purchaseToken))) {
      res.status(500).send("Failed to acknowledge purchase");
      return;
    }
  }

  // TODO: We could probably immediately consume pending purchases, then depending on the pubsub
  // acknowledge or revoke tokens and acknowledge.  Blah.

  // PurchaseState 0 indicates a purchase, 1 indicates a cancellation, which we should not consume
  // PurchaseState 2 is tricky, it is a pending transaction.  We can't even ack pending ones.
  // Notifications on pending transactions may only be possible through
  // pub/sub with google play.
  if (purchase.consumptionState == 0 && purchase.purchaseState == 0) {
    // If the purchase hasn't been consumed, consume it (for things like token buys at least)
    if (!(await addTokens(uid, tokenAmount))) {
      res.status(500).send("Failed to add tokens");
      return;
    }

    // Now tell google it has been consumed.
    if (!(await consumePurchase(playDeveloper, productId, purchaseToken))) {
      res.status(500).send("Failed to consume purchase");
      return;
    }
  }

  res.status(200).send("Purchase Complete!");
});

exports.verifyiapPending = functions.pubsub.topic("iapTopic").onPublish(async (message) => {
  const encodedData = message.data;
  const decodedData = Buffer.from(encodedData, "base64").toString("utf8");
  const purchaseData = JSON.parse(decodedData);

  console.log(purchaseData);

  if (purchaseData.testNotification != null) {
    console.log("Its a test.");
    return null;
  }

  const auth = new GoogleAuth({
    scopes: "https://www.googleapis.com/auth/androidpublisher",
  });

  const client = await auth.getClient();
  const playDeveloper = google.androidpublisher({
    version: "v3",
    auth: client,
  });

  if (purchaseData.oneTimeProductNotification != null) {
    console.log("It's a product notification");

    const otp = purchaseData.oneTimeProductNotification;

    const purchase = await getPurchase(playDeveloper, otp.sku, otp.purchaseToken);
    const uid = purchase.obfuscatedExternalAccountId;
    const tokenAmount = productIdToToken(otp.sku);
    if (tokenAmount == -1) {
      console.error("Unsupported product id");
      return null;
    }

    // For the moment, we don't grant tokens until pending purchases transition to purchased
    // so there's nothing to do when a pending transaction is canceled.  But that could change.
    // Anyway, all we have to do right now is give tokens if a pending transaction goes through.
    if (purchase.acknowledgementState == 0 && purchase.purchaseState == 0) {
      if (!(await savePurchase(uid, otp.purchaseToken))) {
        return null;
      }

      if (!(await ackPurchase(playDeveloper, otp.sku, otp.purchaseToken))) {
        return null;
      }
    }

    if (purchase.consumptionState == 0 && purchase.purchaseState == 0) {
      // If the purchase hasn't been consumed, consume it (for things like token buys at least)
      if (!(await addTokens(uid, tokenAmount))) {
        return null;
      }

      // Now tell google it has been consumed.
      if (!(await consumePurchase(playDeveloper, otp.sku, otp.purchaseToken))) {
        return null;
      }
    }

    if (purchase.purchaseState == 1) {
      // We get an error if we try to ack a canceled purchase.
      console.log("Purchase was canceled");
      return null;
    }

    if (purchase.purchaseState == 2) {
      console.error("We were notified but purchase is still pending, what does this mean??");
      console.error(purchase);
      return null;
    }
  }

  if (purchaseData.subscriptionNotification != null) {
    console.log("It's a subscription notification");
    return null;
  }

  return null;
});
