package com.service.rtochallan;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class NoInternetActivity extends AppCompatActivity {

    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.no_internet_layout);
        Helper helper  = new Helper();

        Button retryButton = findViewById(R.id.retry_button);

        retryButton.setOnClickListener(v -> {
            if (helper.isNetworkAvailable(NoInternetActivity.this)) {
                openMain();
            } else {
                Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ Start listening for network connection changes
        registerNetworkListener();
    }

    private void registerNetworkListener() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);

                runOnUiThread(() -> {
                    Toast.makeText(NoInternetActivity.this, "Internet Connected!", Toast.LENGTH_SHORT).show();
                    openMain();
                });
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                runOnUiThread(() ->
                        Toast.makeText(NoInternetActivity.this, "Internet Lost!", Toast.LENGTH_SHORT).show()
                );
            }
        };

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        cm.registerNetworkCallback(networkRequest, networkCallback);
    }

    private void openMain() {
        Intent intent = new Intent(NoInternetActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && networkCallback != null) {
            try {
                cm.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
        }
    }
}
