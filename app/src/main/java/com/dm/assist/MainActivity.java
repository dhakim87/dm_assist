package com.dm.assist;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.View;
import android.widget.Button;

import com.dm.assist.activity.AddPlayerCharacterActivity;
import com.dm.assist.activity.CreateCampaignActivity;
import com.dm.assist.adapter.CampaignAdapter;
import com.dm.assist.common.DM;
import com.dm.assist.common.Observable;
import com.dm.assist.common.OnItemClickListener;
import com.dm.assist.common.PurchaseTracker;
import com.dm.assist.db.CloudDB;
import com.dm.assist.model.Campaign;
import com.dm.assist.model.WorldCharacter;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView campaignsRecyclerView;
    private List<Campaign> campaignList;
    private CampaignAdapter campaignAdapter;
    private Button createCampaignButton;

    private boolean newUser = true;

    private static final int REQUEST_NEW_CAMPAIGN = 1;
    private static final int REQUEST_EDIT_CAMPAIGN = 2;

    private ValueEventListener campaignWatch = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        campaignsRecyclerView = findViewById(R.id.campaignsRecyclerView);

        campaignList = new ArrayList<Campaign>();
        campaignAdapter = new CampaignAdapter(campaignList);
        // Start syncing with backend
        CloudDB db = new CloudDB();
        this.campaignWatch = db.watchAllCampaigns(new Observable<List<Campaign>>(){
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onChange(List<Campaign> val) {
                campaignList.clear();
                campaignList.addAll(val);
                campaignAdapter.notifyDataSetChanged();

                if (newUser && campaignList.isEmpty())
                    tryIntro();
                else
                    newUser = false;
            }
        });

        campaignAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Intent intent = new Intent(MainActivity.this, CreateCampaignActivity.class);
                intent.putExtra("campaign", campaignList.get(position));
                startActivityForResult(intent, REQUEST_EDIT_CAMPAIGN);
            }
        });
        campaignsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        campaignsRecyclerView.setAdapter(campaignAdapter);

        createCampaignButton = findViewById(R.id.createCampaignButton);
        createCampaignButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CreateCampaignActivity.class);
                startActivityForResult(intent, REQUEST_NEW_CAMPAIGN);
            }
        });

        PurchaseTracker.getInstance(this.getApplicationContext()).startTracking();
    }

    private void tryIntro()
    {
        new AlertDialog.Builder(this)
                .setTitle("Greetings, adventurer!")
                .setMessage(Html.fromHtml(DM.INTRODUCTION, 0))
                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        System.out.println("User acknowledges welcome dialog");
                    }
                })
                .show();
    }

    protected void onDestroy()
    {
        super.onDestroy();
        new CloudDB().unwatchAllCampaigns(this.campaignWatch);
        PurchaseTracker.getInstance(this.getApplicationContext()).endTracking();
    }
}
