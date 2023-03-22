package com.dm.assist.chatgpt;

import android.content.Context;

import com.dm.assist.common.AsyncHook;
import com.dm.assist.model.Campaign;
import com.dm.assist.model.WorldCharacter;

import org.json.JSONException;

import java.io.IOException;

public class GenerateNPCTask extends ChatGPTTask<WorldCharacter> {
    public GenerateNPCTask(Context c, Campaign campaign, AsyncHook<WorldCharacter> hook)
    {
        super(c, campaign, hook);
    }

    @Override
    protected WorldCharacter run() throws IOException, JSONException {
        return new ChatGPT(this.appContext).generateNPC(this.campaign);
    }
}