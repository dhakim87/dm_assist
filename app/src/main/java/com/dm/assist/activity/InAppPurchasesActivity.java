package com.dm.assist.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.dm.assist.MainActivity;
import com.dm.assist.R;
import com.dm.assist.common.PurchaseTracker;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.util.ArrayList;
import java.util.List;

public class InAppPurchasesActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ProductListAdapter productListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_app_purchases);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        productListAdapter = new ProductListAdapter(new ArrayList<>());
        recyclerView.setAdapter(productListAdapter);

        checkPurchaseTracker();
        queryProductDetails();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void checkPurchaseTracker() {
        if (!PurchaseTracker.getInstance(this.getApplicationContext()).connected)
        {
            new AlertDialog.Builder(InAppPurchasesActivity.this)
                .setTitle("Billing Unavailable")
                .setMessage(Html.fromHtml(
            "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<body>\n" +
                    "    <p>We're sorry, but we can't connect to Google Play Services to purchase more AI tokens at the moment. Please try the following:</p>\n" +
                    "    <ul>\n" +
                    "        <li>1. Check your internet connection.</li>\n" +
                    "        <li>2. Ensure you have the latest version of Google Play Services.</li>\n" +
                    "        <li>3. Restart DungeonMind.</li>\n" +
                    "    </ul>\n" +
                    "</body>\n" +
                    "</html>"))
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        finish();
                    }
                })
                .show();
        }
    }

    private void queryProductDetails() {
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(
            QueryProductDetailsParams.Product.newBuilder()
                    .setProductId("aitokens_500000")
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
        );

        QueryProductDetailsParams productDetailsParams = QueryProductDetailsParams .newBuilder()
                .setProductList(productList)
                .build();

        System.out.println("Querying productDetails");
        PurchaseTracker pt = PurchaseTracker.getInstance(this.getApplicationContext());
        pt.billingClient.queryProductDetailsAsync(productDetailsParams, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> productDetailsList) {
                new Handler(Looper.getMainLooper()).post(new Runnable(){
                    @Override
                    public void run() {
                        System.out.println("Got queryProductDetails response" + billingResult + " " + productDetailsList);
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && productDetailsList != null) {
                            productListAdapter.setProductDetailsList(productDetailsList);
                        }
                    }
                });
            }
        });
    }

    private class ProductListAdapter extends RecyclerView.Adapter<ProductListAdapter.ViewHolder> {
        private List<ProductDetails> productDetailsList;

        ProductListAdapter(List<ProductDetails> productDetailsList) {
            this.productDetailsList = productDetailsList;
        }

        void setProductDetailsList(List<ProductDetails> productDetailsList) {
            this.productDetailsList.clear();
            this.productDetailsList.addAll(productDetailsList);
            System.out.println("Setting product details list: " + productDetailsList);
            System.out.println("Active Thread:" + Thread.currentThread().getName());
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.product_details_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProductDetails productDetails = productDetailsList.get(position);
            holder.title.setText(productDetails.getTitle());

            // Looks like subscriptions have some extra offer info that we'd need to grab
            // but this should work for one time purchases?
            ProductDetails.OneTimePurchaseOfferDetails oneTimeDetails = productDetails.getOneTimePurchaseOfferDetails();
            if (oneTimeDetails != null)
                holder.price.setText(productDetails.getOneTimePurchaseOfferDetails().getFormattedPrice());

            ArrayList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
            productDetailsParamsList.add(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            );

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                // How the heck did they manage to log out?
                AlertDialog builder = new AlertDialog.Builder(InAppPurchasesActivity.this)
                        .setTitle("Login Required")
                        .setMessage(Html.fromHtml(
                                "<!DOCTYPE html>\n" +
                                        "<html>\n" +
                                        "<body>\n" +
                                        "    <p>We're sorry, but we can't purchase AI tokens without a logged in account. Please try the following:</p>\n" +
                                        "    <ul>\n" +
                                        "        <li>1. Restart the application and complete the login procedure</li>\n" +
                                        "    </ul>\n" +
                                        "</body>\n" +
                                        "</html>"))
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                finish();
                            }
                        })
                        .show();
            }
            else
            {
                holder.purchaseButton.setOnClickListener(view -> {
                    BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .setObfuscatedAccountId(user.getUid()) //TODO Google might want me to further obfuscate this token, unclear if this is already accomplished by firebase.
                        .build();
                    PurchaseTracker.getInstance(getApplicationContext()).billingClient.launchBillingFlow(InAppPurchasesActivity.this, billingFlowParams);
                });
            }
        }

        @Override
        public int getItemCount() {
            return productDetailsList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView price;
            Button purchaseButton;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.title);
                price = itemView.findViewById(R.id.price);
                purchaseButton = itemView.findViewById(R.id.purchase_button);
            }
        }
    }
}