package com.dm.assist.chatgpt;

import com.dm.assist.common.AsyncHook;
import com.dm.assist.model.Campaign;
import com.dm.assist.model.OneShot;
import com.dm.assist.model.WorldCharacter;

import org.json.JSONException;

import java.io.IOException;


public class GenerateOneShotTask extends ChatGPTTask<OneShot> {
    public GenerateOneShotTask(Campaign campaign, AsyncHook<OneShot> hook)
    {
        super(campaign, hook);
    }

    @Override
    protected OneShot run() throws IOException, JSONException {
        return new ChatGPT().generateOneShot(this.campaign);
    }
}