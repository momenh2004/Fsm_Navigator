package com.fsm.navigator.controller;

import com.fsm.navigator.R;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

/**
 * SplashActivity – Écran de démarrage de FSM Navigator
 *
 * Affiche le logo et le nom de l'application pendant 2.5 secondes
 * avec une animation d'apparition, puis redirige vers MainActivity.
 *
 * Couche MVC : Contrôleur (Controller)
 */
public class SplashActivity extends AppCompatActivity {

    // Durée d'affichage du splash en millisecondes
    private static final int SPLASH_DURATION = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Lancer l'animation d'apparition
        animateSplashContent();

        // Rediriger vers MainActivity après SPLASH_DURATION ms
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            // Animation de transition : fondu
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION);
    }

    /**
     * Anime le contenu central du splash screen :
     *  - Fade in (opacité 0 → 1)
     *  - Scale up (taille 0.8 → 1.0)
     */
    private void animateSplashContent() {
        LinearLayout centerContainer = findViewById(R.id.centerContainer);

        // Animation d'opacité
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(800);

        // Animation d'échelle
        ScaleAnimation scaleUp = new ScaleAnimation(
                0.8f, 1f,
                0.8f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleUp.setDuration(800);

        // Combiner les deux animations
        AnimationSet animSet = new AnimationSet(true);
        animSet.addAnimation(fadeIn);
        animSet.addAnimation(scaleUp);
        animSet.setFillAfter(true);

        if (centerContainer != null) {
            centerContainer.startAnimation(animSet);
        }
    }
}
