package com.dm.assist;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;

import com.dm.assist.activity.AddPlayerCharacterActivity;
import com.dm.assist.activity.CreateCampaignActivity;
import com.dm.assist.adapter.CampaignAdapter;
import com.dm.assist.common.DM;
import com.dm.assist.common.OnItemClickListener;
import com.dm.assist.model.Campaign;
import com.dm.assist.model.WorldCharacter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView campaignsRecyclerView;
    private List<Campaign> campaignList;
    private CampaignAdapter campaignAdapter;
    private Button createCampaignButton;

    private static final int REQUEST_NEW_CAMPAIGN = 1;
    private static final int REQUEST_EDIT_CAMPAIGN = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        campaignsRecyclerView = findViewById(R.id.campaignsRecyclerView);
        // Add any existing campaigns to the campaignList
        try (DBHelper dbHelper = new DBHelper(this.getApplicationContext())) {
            campaignList = dbHelper.getAllCampaigns();
        }
        campaignAdapter = new CampaignAdapter(campaignList);
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
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (campaignList.size() == 0)
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_NEW_CAMPAIGN && resultCode == RESULT_OK && data != null) {
            Campaign c = data.getParcelableExtra("campaign");
            campaignList.add(c);
            campaignAdapter.notifyItemInserted(campaignList.size() - 1);
        } else if (requestCode == REQUEST_EDIT_CAMPAIGN && resultCode == RESULT_OK && data != null) {
            System.out.println("Edited campaign");
            Campaign c = data.getParcelableExtra("campaign");
            System.out.println(c.id + " " + c.name + " " + c.desc);
            for (int i = 0; i < campaignList.size(); i++) {
                System.out.println(i + ": " + campaignList.get(i).id);
                if (campaignList.get(i).id.equals(c.id)) {
                    campaignList.set(i, c);
                    campaignAdapter.notifyItemChanged(i);
                }
            }
        }
    }
}
