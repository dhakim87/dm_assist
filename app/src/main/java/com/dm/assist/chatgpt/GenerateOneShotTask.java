package com.dm.assist.chatgpt;

import android.content.Context;

import com.dm.assist.common.AsyncHook;
import com.dm.assist.model.Campaign;
import com.dm.assist.model.OneShot;
import com.dm.assist.model.WorldCharacter;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;


public class GenerateOneShotTask extends ChatGPTTask<OneShot> {
    public GenerateOneShotTask(Context c, Campaign campaign, AsyncHook<OneShot> hook)
    {
        super(c, campaign, hook);
    }

    @Override
    protected OneShot run() throws IOException, JSONException, ExecutionException, InterruptedException {
        return new ChatGPT(this.appContext).generateOneShot(this.campaign);
    }
}