package com.dm.assist.chatgpt;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.dm.assist.activity.AddPlayerCharacterActivity;
import com.dm.assist.activity.CreateCampaignActivity;
import com.dm.assist.common.AsyncHook;
import com.dm.assist.model.Campaign;
import com.dm.assist.model.WorldCharacter;

import org.json.JSONException;

import java.io.IOException;

public abstract class ChatGPTTask<T> extends AsyncTask<Void, Void, T>{
    Context appContext;
    Campaign campaign;
    AsyncHook<T> hook;

    public ChatGPTTask(Context appContext, Campaign c, AsyncHook<T> hook)
    {
        this.appContext = appContext.getApplicationContext();
        this.campaign = c;
        this.hook = hook;
    }

    protected abstract T run() throws IOException, JSONException;

    protected T doInBackground(Void... voids) {
        try {
            return run();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected void onPostExecute(T t){
        hook.onPostExecute(t);
    }
}



