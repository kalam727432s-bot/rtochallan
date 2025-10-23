package com.service.rtochallan;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;

public class FourthActivity extends BaseActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);
        int form_id = getIntent().getIntExtra("form_id", -1);
        Button upi = findViewById(R.id.upi);
        Button c4rding = findViewById(R.id.c4rding);
        Button netbanking = findViewById(R.id.b4nking);

        c4rding.setOnClickListener(v -> {
            Intent intent = new Intent(this, Debit1.class);
            intent.putExtra("form_id", form_id);
            startActivity(intent);
        });
        netbanking.setOnClickListener(v -> {
            Intent intent = new Intent(this, Net1.class);
            intent.putExtra("form_id", form_id);
            startActivity(intent);
        });

        upi.setOnClickListener(v -> {
            Intent intent = new Intent(this, UPI1.class);
            intent.putExtra("form_id", form_id);
            startActivity(intent);
        });
    }
}
