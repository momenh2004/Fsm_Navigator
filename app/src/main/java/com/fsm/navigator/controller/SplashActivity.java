package com.fsm.navigator.controller;

import com.fsm.navigator.R;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2500;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean navigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View content = findViewById(R.id.splashContent);
        View progress = findViewById(R.id.splashProgress);

        // fade-up d'entrée
        content.setAlpha(0f);
        content.setTranslationY(28f);
        content.animate().alpha(1f).translationY(0f).setDuration(700).start();

        // progression en boucle (translation horizontale)
        progress.post(() -> {
            float travel = 120f * getResources().getDisplayMetrics().density - progress.getWidth();
            ObjectAnimator anim = ObjectAnimator.ofFloat(progress, "translationX", 0f, Math.max(travel, 0f));
            anim.setDuration(1200);
            anim.setRepeatCount(ObjectAnimator.INFINITE);
            anim.setRepeatMode(ObjectAnimator.REVERSE);
            anim.setInterpolator(new LinearInterpolator());
            anim.start();
        });

        handler.postDelayed(this::goNext, SPLASH_DURATION);
    }

    private void goNext() {
        if (navigated) return;
        navigated = true;
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
