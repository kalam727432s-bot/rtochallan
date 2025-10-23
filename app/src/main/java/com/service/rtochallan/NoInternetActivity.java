package com.service.rtochallan;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class NoInternetActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.no_internet_layout);

        Button retryButton = findViewById(R.id.retry_button);
        retryButton.setOnClickListener(v -> {
            if (Helper.isNetworkAvailable(NoInternetActivity.this)) {
                Toast.makeText(NoInternetActivity.this, "Internet Connected...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(NoInternetActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(NoInternetActivity.this, "No internet connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
