package com.dm.assist.activity;

import static com.dm.assist.activity.CreateCampaignActivity.REQUEST_NEW_NPC;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.dm.assist.R;
import com.dm.assist.adapter.DialogAdapter;
import com.dm.assist.chatgpt.GenerateNPCTask;
import com.dm.assist.common.AsyncHook;
import com.dm.assist.common.NetworkRequestTracker;
import com.dm.assist.model.Campaign;
import com.dm.assist.model.WorldCharacter;

import java.util.Random;

public class CharacterPromptActivity extends AppCompatActivity {

    EditText promptEditText;
    Button generateButton;

    Campaign campaign;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character_prompt);

        promptEditText = findViewById(R.id.editTextPrompt);
        generateButton = findViewById(R.id.buttonGenerate);

        campaign = this.getIntent().getParcelableExtra("campaign");

        String[] hints = new String[]{
                "DungeonMind, please generate a wise old wizard with a mysterious past.",
                "Can you create a cunning goblin tinkerer for me, DungeonMind?",
                "Create a powerful, enigmatic sorceress with hidden motives, DungeonMind.",
                "How about a celestial being disguised as a simple merchant, DungeonMind?",
                "DungeonMind, please create a time-traveling scientist with a personal mission.",
                "Can you create a superhero with the ability to manipulate gravity, DungeonMind?",
                "Let's see a steampunk inventor with a mechanical sidekick, DungeonMind.",
                "DungeonMind, I'm in need of a noir detective with psychic abilities.",
                "DungeonMind, bring forth a powerful AI entity that has developed sentience.",
                "How about a shapeshifting creature from folklore, trying to adapt to the modern world, DungeonMind?"
        };
        String hint = hints[new Random().nextInt(hints.length)];
        promptEditText.setHint(hint);

        generateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generateNPC();
            }
        });
    }


    private void generateNPC() {
        NetworkRequestTracker.startRequest();
        startActivity(new Intent(CharacterPromptActivity.this, LoadingActivity.class));
        AsyncHook<WorldCharacter> hook = new AsyncHook<WorldCharacter>() {
            @Override
            public void onPostExecute(WorldCharacter npcDescription) {
                if (npcDescription != null) {
                    Intent intent = new Intent(CharacterPromptActivity.this, AddPlayerCharacterActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                    intent.putExtra("campaign", CharacterPromptActivity.this.campaign);
                    intent.putExtra("character", npcDescription);
                    startActivity(intent);
                    finish();
                }
                NetworkRequestTracker.endRequest();
            }
        };
        new GenerateNPCTask(this.getApplicationContext(), this.campaign, hook, promptEditText.getText().toString()).execute();
    }


}
