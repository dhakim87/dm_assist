package com.dm.assist.chatgpt;

import android.content.Context;

import com.dm.assist.common.AsyncHook;
import com.dm.assist.model.Campaign;
import com.dm.assist.model.WorldCharacter;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class GenerateNPCTask extends ChatGPTTask<WorldCharacter> {
    String npcPrompt;
    public GenerateNPCTask(Context c, Campaign campaign, AsyncHook<WorldCharacter> hook, String npcPrompt)
    {
        super(c, campaign, hook);
        this.npcPrompt = npcPrompt;
    }

    @Override
    protected WorldCharacter run() throws IOException, JSONException, ExecutionException, InterruptedException {
        return new ChatGPT(this.appContext).generateNPC(this.campaign, npcPrompt);
    }
}