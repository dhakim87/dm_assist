package com.dm.assist.common;

import android.app.Activity;

import java.util.ArrayList;

public class NetworkRequestTracker {
    private static int activeRequests = 0;
    private static ArrayList<Activity> watchers = new ArrayList<>();

    public static void startRequest()
    {
        activeRequests += 1;
        if (activeRequests > 3) //Theoretically possible, but not within the app at this moment, can up that if we allow for multiple network requests open at once.
            throw new RuntimeException("Bad programming, called startRequest without corresponding endRequest");
    }

    public static void endRequest()
    {
        activeRequests -= 1;
        if (activeRequests == 0)
        {
            for (Activity w : watchers)
                w.finish();
            watchers.clear();
        }
        if (activeRequests < 0)
            throw new RuntimeException("Bad programming, called endRequest without corresponding startRequest");
    }

    public static void watch(Activity a)
    {
        if (activeRequests == 0)
        {
            a.finish();
            return;
        }
        watchers.add(a);
    }
}
