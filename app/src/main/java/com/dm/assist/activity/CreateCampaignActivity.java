package com.dm.assist.activity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import android.content.Intent;

import com.dm.assist.adapter.OneShotAdapter;
import com.dm.assist.chatgpt.GenerateDialogTask;
import com.dm.assist.chatgpt.GenerateNPCTask;
import com.dm.assist.chatgpt.GenerateOneShotTask;
import com.dm.assist.common.AsyncHook;
import com.dm.assist.common.CharacterDialog;
import com.dm.assist.common.DM;
import com.dm.assist.common.OnItemClickListener;
import com.dm.assist.model.Campaign;
import com.dm.assist.model.OneShot;
import com.dm.assist.model.WorldCharacter;
import com.dm.assist.adapter.CharacterAdapter;
import com.dm.assist.DBHelper;
import com.dm.assist.R;

import java.util.ArrayList;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class CreateCampaignActivity extends AppCompatActivity {
    public static boolean showedPlea = false;

    public static final String FAKE_AD_ID = "ca-app-pub-3940256099942544/1033173712";
    public static final String REAL_AD_ID = "ca-app-pub-1045037164525954/6565292166";
    public static final String AD_ID = FAKE_AD_ID;

    private boolean loadingAd;
    private InterstitialAd interstitialAd;

    private EditText campaignNameEditText;
    private Spinner dmStyleSpinner;
    private EditText settingEditText;
    private Button addCharacterButton;
    private Button generateNPCButton;
    private Button generateOneShotButton;
    private Button chatWithDMButton;

    private TextView dmStyleDescriptionTextView;
    private TextView remainingTokensTextView;

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
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                System.out.println("MobileAd initialization complete");
                System.out.println("initialization status:" + initializationStatus);
            }
        });

        campaignNameEditText = findViewById(R.id.campaignNameEditText);
        dmStyleSpinner = findViewById(R.id.dmStyleSpinner);
        settingEditText = findViewById(R.id.settingEditText);
        dmStyleDescriptionTextView = findViewById(R.id.dmStyleDescriptionTextView);
        remainingTokensTextView = findViewById(R.id.remainingTokensTextView);

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

        chatWithDMButton = findViewById(R.id.chatWithDMButton);
        chatWithDMButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { talkToDM(); }
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
    protected void onResume() {
        super.onResume();

        try (DBHelper helper = new DBHelper(this)) {
            long tokens = helper.getTokens();

            remainingTokensTextView.setText("AI Tokens: " + tokens);
            if (tokens < 0)
                remainingTokensTextView.setTextColor(0xFFFF0000);
            else
                remainingTokensTextView.setTextColor(0xFF000000);

            // TODO Give plea from ChatGPT to get more tokens when user is between 0 and X tokens
            //  Then if they hit 0, start showing ads.
            if (tokens > 0 && tokens < 2500 && !showedPlea)
            {
                updateCampaign();
                showedPlea = true;
                new AlertDialog.Builder(this)
                    .setTitle("A Passionate Plea")
                    .setMessage(Html.fromHtml(new DM().pleas.get(this.campaign.dm), 0))
                    // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.out.println("User says Yes give me money");
                        }
                    })
                    // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.out.println("User says no, they'd rather watch ads");
                        }
                    })
                    .show();
            }

            if (tokens < 0) {
                System.out.println("Has an ad to show: " + (this.interstitialAd != null));
                if (this.interstitialAd == null && !this.loadingAd) {
                    System.out.println("Loading ad...");
                    this.loadingAd = true;
                    AdRequest adRequest = new AdRequest.Builder().build();
                    InterstitialAd.load(this, AD_ID, adRequest,
                            new InterstitialAdLoadCallback() {
                                @Override
                                public void onAdLoaded(InterstitialAd interstitialAd) {
                                    System.out.println("Loaded new ad");
                                    CreateCampaignActivity.this.interstitialAd = interstitialAd;
                                    CreateCampaignActivity.this.loadingAd = false;
                                }

                                @Override
                                public void onAdFailedToLoad(LoadAdError loadAdError) {
                                    System.out.println("Failed to load ad");
                                    CreateCampaignActivity.this.interstitialAd = null;
                                    CreateCampaignActivity.this.loadingAd = false;
                                }
                            });
                } else if (this.interstitialAd != null) {
                    this.interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdClicked() {
                            // Called when a click is recorded for an ad.
                            System.out.println("Ad was clicked.");
                        }

                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // Called when ad is dismissed.
                            // Set the ad reference to null so you don't show the ad a second time.
                            System.out.println("Ad dismissed fullscreen content.");
                            CreateCampaignActivity.this.interstitialAd = null;
                            CreateCampaignActivity.this.loadingAd = false;
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            // Called when ad fails to show.
                            System.out.println("Ad failed to show fullscreen content.");
                            CreateCampaignActivity.this.interstitialAd = null;
                            CreateCampaignActivity.this.loadingAd = false;
                        }

                        @Override
                        public void onAdImpression() {
                            // Called when an impression is recorded for an ad.
                            System.out.println("Ad recorded an impression.");
                            // Is there a way to find out how much this impression was worth?
                            // A video ad should be worth 2500 AI tokens, full screen banners should be
                            // worth way way less.
                            try (DBHelper helper = new DBHelper(CreateCampaignActivity.this.getApplicationContext())) {
                                helper.addTokens(2500);
                            }

                            //And possibly start loading an ad, ugh feels so dirty.
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            // Called when ad is shown.
                            System.out.println("Ad showed fullscreen content.");
                        }
                    });

                    this.interstitialAd.show(this);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        saveCampaign(true);
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
        new GenerateNPCTask(this.getApplicationContext(), this.campaign, hook).execute();
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
        new GenerateOneShotTask(this.getApplicationContext(), this.campaign, hook).execute();
    }

    private void talkToDM() {
        this.updateCampaign();
        String dmDesc = dmStyleDescriptions[dmStyleSpinner.getSelectedItemPosition()];
        final WorldCharacter dmChar = new WorldCharacter(CreateCampaignActivity.this.campaign.dm, dmDesc);
        AsyncHook<CharacterDialog> hook = new AsyncHook<CharacterDialog>() {
            @Override
            public void onPostExecute(CharacterDialog dialog) {
                updateCampaign();
                Intent intent = new Intent(CreateCampaignActivity.this, CharacterDialogActivity.class);
                intent.putExtra("campaign", CreateCampaignActivity.this.campaign);
                intent.putExtra("character", dmChar);
                intent.putExtra("dialog", dialog);
                startActivity(intent);
            }
        };
        new GenerateDialogTask(this.getApplicationContext(), this.campaign, dmChar, null, hook).execute();
    }

    private void updateCampaign() {
        this.campaign.name = campaignNameEditText.getText().toString().trim();
        if (this.campaign.name.isEmpty())
            this.campaign.name = "Unnamed Campaign";
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