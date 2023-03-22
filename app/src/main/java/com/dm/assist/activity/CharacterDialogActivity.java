package com.dm.assist.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dm.assist.R;
import com.dm.assist.adapter.CharacterAdapter;
import com.dm.assist.adapter.DialogAdapter;
import com.dm.assist.adapter.OneShotAdapter;
import com.dm.assist.chatgpt.GenerateDialogTask;
import com.dm.assist.common.AsyncHook;
import com.dm.assist.common.CharacterDialog;
import com.dm.assist.common.OnItemClickListener;
import com.dm.assist.model.Campaign;
import com.dm.assist.model.WorldCharacter;

public class CharacterDialogActivity extends AppCompatActivity {

    Button sendButton;
    EditText messageEditText;
    RecyclerView messageRecyclerView;
    DialogAdapter messageAdapter;
    CharacterDialog activeDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character_dialog);

        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        messageRecyclerView = findViewById(R.id.messageRecyclerView);

        activeDialog = this.getIntent().getParcelableExtra("dialog");
        messageAdapter = new DialogAdapter(activeDialog);
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageRecyclerView.setAdapter(messageAdapter);


        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });
    }

    private void sendMessage() {
        String msg = messageEditText.getText().toString().trim();
        System.out.println("Sent: " + msg);
        activeDialog.conversation.add(msg);
        messageAdapter.notifyItemInserted(activeDialog.conversation.size() - 1);
        messageEditText.setText("");
        messageRecyclerView.scrollToPosition(activeDialog.conversation.size() - 1);
        AsyncHook<CharacterDialog> hook = new AsyncHook<CharacterDialog>() {
            @Override
            public void onPostExecute(CharacterDialog d) {
                String lastMessage = d.conversation.get(d.conversation.size()-1);
                System.out.println("Received: " + lastMessage);
                messageAdapter.notifyItemInserted(activeDialog.conversation.size() - 1);
                messageRecyclerView.scrollToPosition(activeDialog.conversation.size() - 1);
            }
        };

        Campaign activeCampaign = this.getIntent().getParcelableExtra("campaign");
        WorldCharacter wc = this.getIntent().getParcelableExtra("character");
        new GenerateDialogTask(this.getApplicationContext(), activeCampaign, wc, activeDialog, hook).execute();
    }
}
