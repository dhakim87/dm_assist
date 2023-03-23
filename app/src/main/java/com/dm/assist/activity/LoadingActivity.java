package com.dm.assist.activity;

import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dm.assist.R;
import com.dm.assist.common.NetworkRequestTracker;

import java.util.Timer;
import java.util.TimerTask;

public class LoadingActivity extends AppCompatActivity {

    private ProgressBar mProgressBar;
    private TextView mLoadingText;
    private String[] mLoadingMessages;
    private int mCurrentMessageIndex;

    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        mProgressBar = findViewById(R.id.progressBar);
        mLoadingText = findViewById(R.id.loadingText);
        mLoadingMessages = getResources().getStringArray(R.array.loading_messages);
        mCurrentMessageIndex = 0;

        // Start animation
        Animation rotation = AnimationUtils.loadAnimation(this, R.anim.spinner);
        rotation.setRepeatCount(Animation.INFINITE);
        mProgressBar.startAnimation(rotation);

        // Start timer to switch between loading messages
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    mCurrentMessageIndex = (mCurrentMessageIndex + 1) % mLoadingMessages.length;
                    mLoadingText.setText(mLoadingMessages[mCurrentMessageIndex]);
                });
            }
        }, 0, 3000); // Switch message every 3 seconds

        NetworkRequestTracker.watch(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop animation and timer to prevent memory leaks
        mProgressBar.clearAnimation();
        mProgressBar = null;
        timer.cancel();
    }
}
