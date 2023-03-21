package com.dm.assist.chatgpt;

import com.dm.assist.common.AsyncHook;
import com.dm.assist.common.CharacterDialog;
import com.dm.assist.model.Campaign;
import com.dm.assist.model.WorldCharacter;

import org.json.JSONException;

import java.io.IOException;


public class GenerateDialogTask extends ChatGPTTask<CharacterDialog> {

    WorldCharacter wc;
    CharacterDialog activeDialog;

    public GenerateDialogTask(Campaign campaign, WorldCharacter wc, CharacterDialog activeDialog, AsyncHook<CharacterDialog> hook)
    {
        super(campaign, hook);
        this.wc = wc;
        this.activeDialog = activeDialog;
    }

    @Override
    protected CharacterDialog run() throws IOException, JSONException {
        return new ChatGPT().talkToCharacter(this.campaign, this.wc, this.activeDialog);
    }
}