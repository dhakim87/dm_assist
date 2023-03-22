package com.dm.assist.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.content.Intent;

import com.dm.assist.R;
import com.dm.assist.chatgpt.GenerateDialogTask;
import com.dm.assist.chatgpt.GenerateOneShotTask;
import com.dm.assist.common.AsyncHook;
import com.dm.assist.common.CharacterDialog;
import com.dm.assist.model.Campaign;
import com.dm.assist.model.OneShot;
import com.dm.assist.model.WorldCharacter;

public class AddPlayerCharacterActivity extends AppCompatActivity {

    private EditText characterNameEditText;
    private EditText characterDescriptionEditText;

    private Button talkToCharacterButton;

    private Button addCharacterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_player_character);

        characterNameEditText = findViewById(R.id.characterNameEditText);
        characterDescriptionEditText = findViewById(R.id.characterDescriptionEditText);
        addCharacterButton = findViewById(R.id.addCharacterButton);
        talkToCharacterButton = findViewById(R.id.talkToCharacterButton);

        WorldCharacter editChar = this.getIntent().getParcelableExtra("character");
        if (editChar != null) {
            characterNameEditText.setText(editChar.name);
            characterDescriptionEditText.setText(editChar.description);
        }

        addCharacterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addCharacter();
            }
        });

        talkToCharacterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { talkToCharacter(); }
        });
    }

    private void addCharacter() {
        WorldCharacter toSave = constructCharacter();

        Intent resultIntent = new Intent();
        resultIntent.putExtra("character", toSave);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private WorldCharacter constructCharacter() {
        WorldCharacter toSave = new WorldCharacter(
                characterNameEditText.getText().toString().trim(),
                characterDescriptionEditText.getText().toString().trim()
        );

        WorldCharacter editChar = this.getIntent().getParcelableExtra("character");
        if (editChar != null) {
            toSave.id = editChar.id;
        }

        if (toSave.name.equals(""))
            toSave.name = "Unnamed Character";

        return toSave;
    }

    private void talkToCharacter() {
        AsyncHook<CharacterDialog> hook = new AsyncHook<CharacterDialog>() {
            @Override
            public void onPostExecute(CharacterDialog d) {
                Intent intent = new Intent(AddPlayerCharacterActivity.this, CharacterDialogActivity.class);
                Campaign c = getIntent().getParcelableExtra("campaign");
                intent.putExtra("campaign", c);
                intent.putExtra("character", constructCharacter());
                intent.putExtra("dialog", d);
                startActivity(intent);
            }
        };

        Campaign activeCampaign = this.getIntent().getParcelableExtra("campaign");
        WorldCharacter wc = constructCharacter();
        new GenerateDialogTask(this.getApplicationContext(), activeCampaign, wc, null, hook).execute();
    }
}
