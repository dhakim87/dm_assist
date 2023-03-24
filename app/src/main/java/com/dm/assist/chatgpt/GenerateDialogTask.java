package com.dm.assist.chatgpt;

import android.content.Context;

import com.dm.assist.common.AsyncHook;
import com.dm.assist.common.CharacterDialog;
import com.dm.assist.model.Campaign;
import com.dm.assist.model.WorldCharacter;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;


public class GenerateDialogTask extends ChatGPTTask<CharacterDialog> {

    WorldCharacter wc;
    CharacterDialog activeDialog;

    public GenerateDialogTask(Context c, Campaign campaign, WorldCharacter wc, CharacterDialog activeDialog, AsyncHook<CharacterDialog> hook)
    {
        super(c, campaign, hook);
        this.wc = wc;
        this.activeDialog = activeDialog;
    }

    @Override
    protected CharacterDialog run() throws IOException, JSONException, ExecutionException, InterruptedException {
        return new ChatGPT(this.appContext).talkToCharacter(this.campaign, this.wc, this.activeDialog);
    }
}