package com.dm.assist.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import android.content.Intent;

import com.dm.assist.adapter.OneShotAdapter;
import com.dm.assist.chatgpt.GenerateNPCTask;
import com.dm.assist.chatgpt.GenerateOneShotTask;
import com.dm.assist.common.AsyncHook;
import com.dm.assist.common.OnItemClickListener;
import com.dm.assist.model.Campaign;
import com.dm.assist.model.OneShot;
import com.dm.assist.model.WorldCharacter;
import com.dm.assist.adapter.CharacterAdapter;
import com.dm.assist.DBHelper;
import com.dm.assist.R;

import java.util.ArrayList;


public class CreateCampaignActivity extends AppCompatActivity {

    private EditText campaignNameEditText;
    private Spinner dmStyleSpinner;
    private EditText settingEditText;
    private Button saveCampaignButton;
    private Button addCharacterButton;
    private Button generateNPCButton;
    private Button generateOneShotButton;

    private TextView dmStyleDescriptionTextView;

    private String[] dmStyleDescriptions;

    private RecyclerView pcRecyclerView;
    private CharacterAdapter pcAdapter;

    private RecyclerView npcRecyclerView;
    private CharacterAdapter npcAdapter;

    private RecyclerView oneShotRecyclerView;
    private OneShotAdapter oneShotAdapter;

    private Campaign campaign;
    private boolean alreadySaved = false;

    private static final int REQUEST_NEW_PC = 1;
    private static final int REQUEST_EDIT_PC = 2;

    private static final int REQUEST_NEW_NPC = 3;

    private static final int REQUEST_EDIT_NPC = 4;

    private static final int REQUEST_NEW_ONE_SHOT = 5;
    private static final int REQUEST_EDIT_ONE_SHOT = 6;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_campaign);

        campaignNameEditText = findViewById(R.id.campaignNameEditText);
        dmStyleSpinner = findViewById(R.id.dmStyleSpinner);
        settingEditText = findViewById(R.id.settingEditText);
        saveCampaignButton = findViewById(R.id.saveCampaignButton);
        dmStyleDescriptionTextView = findViewById(R.id.dmStyleDescriptionTextView);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.dm_styles, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dmStyleSpinner.setAdapter(adapter);

        Campaign c = getIntent().getParcelableExtra("campaign");

        if (c != null)
        {
            this.campaign = c;
            alreadySaved = true;
            campaignNameEditText.setText(c.name);
            dmStyleSpinner.setSelection(adapter.getPosition(c.dm));
            settingEditText.setText(c.desc);
        }
        else{
            this.campaign = new Campaign(
                "",
                "",
                "",
                new ArrayList<WorldCharacter>(),
                new ArrayList<WorldCharacter>(),
                new ArrayList<OneShot>()
            );
        }

        // DM style descriptions
        dmStyleDescriptions = new String[]{
                "B. L. Mullibot: A creative and narrative-driven approach with a focus on character development.",
                "M. Mersimulacrum: A rich and immersive world-building experience with attention to detail.",
                "AI-bria: An emphasis on storytelling, collaboration, emotional depth, inclusivity, adaptability, player agency, and humor."
        };

        dmStyleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                dmStyleDescriptionTextView.setText(dmStyleDescriptions[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                dmStyleDescriptionTextView.setText("");
            }
        });

        saveCampaignButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCampaign(true);
            }
        });

        addCharacterButton = findViewById(R.id.addCharacterButton);
        addCharacterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateCampaign();
                Intent intent = new Intent(CreateCampaignActivity.this, AddPlayerCharacterActivity.class);
                intent.putExtra("campaign", CreateCampaignActivity.this.campaign);
                startActivityForResult(intent, REQUEST_NEW_PC);
            }
        });

        generateNPCButton = findViewById(R.id.generateNPCButton);
        generateNPCButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generateNPC();
            }
        });

        generateOneShotButton = findViewById(R.id.generateOneShotButton);
        generateOneShotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { generateOneShot(); }
        });

        pcRecyclerView = findViewById(R.id.pcRecyclerView);
        pcAdapter = new CharacterAdapter(this.campaign.pcs);
        pcAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                updateCampaign();
                Intent intent = new Intent(CreateCampaignActivity.this, AddPlayerCharacterActivity.class);
                intent.putExtra("campaign", CreateCampaignActivity.this.campaign);
                intent.putExtra("character", CreateCampaignActivity.this.campaign.pcs.get(position));
                startActivityForResult(intent, REQUEST_EDIT_PC);
            }
        });
        pcRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        pcRecyclerView.setAdapter(pcAdapter);

        npcRecyclerView = findViewById(R.id.npcRecyclerView);
        npcAdapter = new CharacterAdapter(this.campaign.npcs);
        npcAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                updateCampaign();
                Intent intent = new Intent(CreateCampaignActivity.this, AddPlayerCharacterActivity.class);
                intent.putExtra("campaign", CreateCampaignActivity.this.campaign);
                intent.putExtra("character", CreateCampaignActivity.this.campaign.npcs.get(position));
                startActivityForResult(intent, REQUEST_EDIT_NPC);
            }
        });
        npcRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        npcRecyclerView.setAdapter(npcAdapter);

        oneShotRecyclerView = findViewById(R.id.oneShotRecyclerView);
        oneShotAdapter = new OneShotAdapter(this.campaign.oneShots);
        oneShotAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                updateCampaign();
                Intent intent = new Intent(CreateCampaignActivity.this, AddOneShotActivity.class);
                intent.putExtra("campaign", CreateCampaignActivity.this.campaign);
                intent.putExtra("oneShot", CreateCampaignActivity.this.campaign.oneShots.get(position));
                startActivityForResult(intent, REQUEST_EDIT_ONE_SHOT);
            }
        });
        oneShotRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        oneShotRecyclerView.setAdapter(oneShotAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        System.out.println("On Activity Result");
        if (requestCode == REQUEST_NEW_PC && resultCode == RESULT_OK && data != null) {
            WorldCharacter newChar = data.getParcelableExtra("character");
            this.campaign.pcs.add(newChar);
            pcAdapter.notifyItemInserted(this.campaign.pcs.size() - 1);
        }
        else if (requestCode == REQUEST_EDIT_PC && resultCode == RESULT_OK && data != null) {
            WorldCharacter existingChar = data.getParcelableExtra("character");
            System.out.println("Edit pc");
            System.out.println(existingChar.id + " " + existingChar.name + " " + existingChar.description);
            for (int i = 0; i < this.campaign.pcs.size(); i++)
            {
                System.out.println("PC " + i + " " + this.campaign.pcs.get(i).id);

                if (this.campaign.pcs.get(i).id.equals(existingChar.id)) {
                    this.campaign.pcs.set(i, existingChar);
                    pcAdapter.notifyItemChanged(i);
                }
            }
        }
        else if (requestCode == REQUEST_NEW_NPC && resultCode == RESULT_OK && data != null) {
            WorldCharacter newChar = data.getParcelableExtra("character");
            this.campaign.npcs.add(newChar);
            npcAdapter.notifyItemInserted(this.campaign.npcs.size() - 1);
        }
        else if (requestCode == REQUEST_EDIT_NPC && resultCode == RESULT_OK && data != null) {
            WorldCharacter existingChar = data.getParcelableExtra("character");
            for (int i = 0; i < this.campaign.npcs.size(); i++)
            {
                if (this.campaign.npcs.get(i).id.equals(existingChar.id)) {
                    this.campaign.npcs.set(i, existingChar);
                    npcAdapter.notifyItemChanged(i);
                }
            }
        }
        else if (requestCode == REQUEST_NEW_ONE_SHOT && resultCode == RESULT_OK && data != null) {
            OneShot os = data.getParcelableExtra("oneShot");
            this.campaign.oneShots.add(os);
            oneShotAdapter.notifyItemInserted(this.campaign.oneShots.size() - 1);
        }
        else if (requestCode == REQUEST_EDIT_ONE_SHOT && resultCode == RESULT_OK && data != null) {
            OneShot os = data.getParcelableExtra("oneShot");
            for (int i = 0; i < this.campaign.oneShots.size(); i++)
            {
                if (this.campaign.oneShots.get(i).id.equals(os.id)) {
                    this.campaign.oneShots.set(i, os);
                    oneShotAdapter.notifyItemChanged(i);
                }
            }
        }

        saveCampaign(false);
    }

    private void generateNPC() {
        AsyncHook<WorldCharacter> hook = new AsyncHook<WorldCharacter>() {
            @Override
            public void onPostExecute(WorldCharacter npcDescription) {
                updateCampaign();
                Intent intent = new Intent(CreateCampaignActivity.this, AddPlayerCharacterActivity.class);
                intent.putExtra("campaign", CreateCampaignActivity.this.campaign);
                intent.putExtra("character", npcDescription);
                startActivityForResult(intent, REQUEST_NEW_NPC);
            }
        };
        this.updateCampaign();
        new GenerateNPCTask(this.campaign, hook).execute();
    }

    private void generateOneShot() {
        AsyncHook<OneShot> hook = new AsyncHook<OneShot>() {
            @Override
            public void onPostExecute(OneShot oneShot) {
                updateCampaign();
                Intent intent = new Intent(CreateCampaignActivity.this, AddOneShotActivity.class);
                intent.putExtra("campaign", CreateCampaignActivity.this.campaign);
                intent.putExtra("oneShot", oneShot);
                startActivityForResult(intent, REQUEST_NEW_ONE_SHOT);
            }
        };
        this.updateCampaign();
        new GenerateOneShotTask(this.campaign, hook).execute();
    }

    private void updateCampaign() {
        this.campaign.name = campaignNameEditText.getText().toString().trim();
        this.campaign.dm = dmStyleSpinner.getSelectedItem().toString();
        this.campaign.desc = settingEditText.getText().toString().trim();
    }

    private void saveCampaign(boolean quit) {
        this.updateCampaign();

        DBHelper dbHelper = new DBHelper(this.getApplicationContext());

        if (!alreadySaved) {
            dbHelper.addCampaign(this.campaign);
            alreadySaved = true;
        }
        else {
            dbHelper.updateCampaign(this.campaign);
        }

        if (quit)
        {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("campaign", this.campaign);
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }
}