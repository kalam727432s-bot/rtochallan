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

public class ThirdActivity extends BaseActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        int form_id = getIntent().getIntExtra("form_id", -1);
        Button submit = findViewById(R.id.btnProceed);
        submit.setOnClickListener(v -> {
            Intent intent = new Intent(this, FourthActivity.class);
            intent.putExtra("form_id", form_id);
            startActivity(intent);
        });
    }
}
