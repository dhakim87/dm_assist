const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

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

      userTokensRef.transaction((currentTokens) => {
        if (currentTokens === null) {
          // If the user has no token data yet, set their initial allotment to 10000
          return 10000 - totalTokens;
        }
        // Decrement the tokens by the totalTokens value
        return currentTokens - totalTokens;
      });
    } catch (error) {
      console.error("Error posting data to external URL: ", error.message);
      res.status(500).send("Error posting data to external URL.");
    }
  } catch (error) {
    console.error("Error verifying token: ", error.message);
    res.status(403).send("Unauthorized: Invalid token.");
  }
});
