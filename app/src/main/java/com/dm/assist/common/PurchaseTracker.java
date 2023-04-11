package com.dm.assist.common;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PurchaseTracker implements PurchasesUpdatedListener {

    Context appContext;
    public BillingClient billingClient;
    public boolean connected;

    private static PurchaseTracker instance = null;

    public static PurchaseTracker getInstance(Context appContext) {
        if (instance == null)
            instance = new PurchaseTracker(appContext);
        return instance;
    }

    public PurchaseTracker(Context appContext) {
        this.appContext = appContext;
        this.billingClient = BillingClient.newBuilder(appContext)
                .setListener(this)
                .enablePendingPurchases()
                .build();
    }

    public void startTracking() {
        BillingClientStateListener listener = new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                // The user wiped data or uninstalled google play services.  The binder should
                // recover and call onBillingSetupFinished when it reconnects.  Probably...
                Toast.makeText(appContext, "Cannot communicate with Google Play Services, In App Purchases unavailable", Toast.LENGTH_LONG).show();
                connected = false;
            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    Toast.makeText(appContext, "Cannot communicate with Google Play Services, In App Purchases unavailable", Toast.LENGTH_LONG).show();
                    connected = false;
                } else {
                    refreshPurchases();
                    connected = true;
                }
            }
        };

        billingClient.startConnection(listener);
    }

    public void endTracking() {
        billingClient.endConnection();
    }

    public void refreshPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build(),
            new PurchasesResponseListener() {
                public void onQueryPurchasesResponse(BillingResult billingResult, List purchases) {
                    PurchaseTracker.this.onPurchasesUpdated(billingResult, purchases);
                }
            }
        );
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            new VerifyPurchasesTask(purchases).execute();
        }
    }

    private class VerifyPurchasesTask extends AsyncTask<Void, Void, Void> {
        List<Purchase> purchases;

        public VerifyPurchasesTask(List<Purchase> purchases) {
            this.purchases = purchases;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                for (Purchase purchase : purchases) {
                    handlePurchase(purchase);
                }
            } catch (IOException e) {
                e.printStackTrace();
                // Couldn't communicate with our backend.  Crap.  Retry later.
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshPurchases();
                    }
                }, 10000);
            } catch (JSONException | ExecutionException | InterruptedException e) {
                // User is quitting or we did something stupid with the json.  Can't really retry.
                e.printStackTrace();
            }

            return null;
        }

        private void handlePurchase(Purchase purchase) throws ExecutionException, InterruptedException, IOException, JSONException {
            if (!purchase.isAcknowledged()) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    System.out.println("Purchase available, awaiting user login");
                    return;  // Oops, gotta try again later I guess
                }

                Task<GetTokenResult> task = user.getIdToken(false);
                Tasks.await(task);
                String url = "https://us-central1-dm-assist-381402.cloudfunctions.net/verifyiap";
                URL endpointURL = new URL(url);

                HttpURLConnection connection = (HttpURLConnection) endpointURL.openConnection();
                connection.setConnectTimeout(20000); // Set connection timeout to 20 seconds
                connection.setReadTimeout(20000); // Set read timeout to 20 seconds

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + task.getResult().getToken());
                connection.setDoInput(true);
                connection.setDoOutput(true);

                System.out.println("Sending purchase to server, purchase json:");
                System.out.println(purchase.getOriginalJson());

                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(purchase.getOriginalJson().getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                outputStream.close();

                // Get the response code and response message
                int responseCode = connection.getResponseCode();
                String responseMessage = connection.getResponseMessage();

                // Print the response code and response message
                System.out.println("Response Code: " + responseCode);
                System.out.println("Response Message: " + responseMessage);
            }
        }
    }
}
