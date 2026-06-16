package com.fsm.navigator.controller;

import com.fsm.navigator.R;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.fsm.navigator.auth.PmrDialogHelper;
import com.fsm.navigator.auth.PmrManager;
import com.fsm.navigator.auth.TokenManager;
import com.fsm.navigator.auth.TtsManager;

public abstract class BaseDrawerActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;

    protected void setupDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout);
        if (drawerLayout == null) return;

        boolean isLoggedIn = TokenManager.isLoggedIn(this);

        // Header drawer
        TextView drawerName   = findViewById(R.id.drawerName);
        TextView drawerEmail  = findViewById(R.id.drawerEmail);
        TextView drawerAvatar = findViewById(R.id.drawerAvatarInitial);

        if (isLoggedIn) {
            String email = TokenManager.getEmail(this);
            if (email != null && !email.isEmpty()) {
                String name        = email.contains("@") ? email.split("@")[0] : email;
                String displayName = name.substring(0,1).toUpperCase() + name.substring(1);
                if (drawerName   != null) drawerName.setText(displayName);
                if (drawerEmail  != null) drawerEmail.setText(email);
                if (drawerAvatar != null) drawerAvatar.setText(String.valueOf(displayName.charAt(0)));
            }
        } else {
            if (drawerName   != null) drawerName.setText("Visiteur");
            if (drawerEmail  != null) drawerEmail.setText("Mode invité");
            if (drawerAvatar != null) drawerAvatar.setText("V");
        }

        setupDrawerItems(isLoggedIn);
    }

    private void setupDrawerItems(boolean isLoggedIn) {
        // Toujours visibles
        setupDrawerItem(R.id.drawerHome,   () -> navigateTo(MainActivity.class));
        setupDrawerItem(R.id.drawerMap,    () -> navigateTo(MapActivity.class));
        setupDrawerItem(R.id.drawerSearch, () -> navigateTo(SearchActivity.class));
        setupDrawerItem(R.id.drawerNav,    () -> navigateTo(SearchActivity.class));

        // Profil — visible uniquement si connecté
        LinearLayout drawerProfile = findViewById(R.id.drawerProfile);
        if (drawerProfile != null) {
            drawerProfile.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
            if (isLoggedIn) {
                drawerProfile.setOnClickListener(v -> {
                    closeDrawer();
                    drawerProfile.postDelayed(() -> navigateTo(ProfileActivity.class), 200);
                });
            }
        }

        // Mode PMR
        LinearLayout drawerPmr = findViewById(R.id.drawerPmr);
        if (drawerPmr != null) {
            updatePmrDrawerLabel(drawerPmr);
            drawerPmr.setOnClickListener(v -> {
                closeDrawer();
                drawerPmr.postDelayed(() -> onPmrToggleClicked(drawerPmr), 200);
            });
        }

        // Déconnexion / Se connecter
        LinearLayout drawerLogout = findViewById(R.id.drawerLogout);
        if (drawerLogout != null) {
            // Trouver le TextView du label
            TextView tvLabel = null;
            for (int i = 0; i < drawerLogout.getChildCount(); i++) {
                View child = drawerLogout.getChildAt(i);
                if (child instanceof TextView) { tvLabel = (TextView) child; break; }
            }

            if (isLoggedIn) {
                if (tvLabel != null) tvLabel.setText("Déconnexion");
                drawerLogout.setOnClickListener(v -> {
                    closeDrawer();
                    drawerLogout.postDelayed(this::showLogoutDialog, 200);
                });
            } else {
                if (tvLabel != null) tvLabel.setText("Se connecter");
                drawerLogout.setOnClickListener(v -> {
                    closeDrawer();
                    drawerLogout.postDelayed(() ->
                            startActivity(new Intent(this, LoginActivity.class)), 200);
                });
            }
        }
    }

    private void setupDrawerItem(int viewId, Runnable action) {
        LinearLayout item = findViewById(viewId);
        if (item != null) {
            item.setOnClickListener(v -> {
                closeDrawer();
                item.postDelayed(action::run, 200);
            });
        }
    }

    protected void setupSheetSwipe(View sheet) {
        if (sheet == null) return;
        View handle = sheet.findViewById(R.id.handleDismiss);
        if (handle == null) return;
        final float[] startY = {0};
        handle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    startY[0] = event.getRawY();
                    return true;
                case android.view.MotionEvent.ACTION_MOVE:
                    float dy = event.getRawY() - startY[0];
                    if (dy > 0) sheet.setTranslationY(dy);
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                    float total = event.getRawY() - startY[0];
                    if (total > 150) {
                        sheet.setTranslationY(0);
                        sheet.setVisibility(View.GONE);
                    } else {
                        sheet.animate().translationY(0).setDuration(150).start();
                    }
                    return true;
            }
            return false;
        });
    }

    protected void setupHamburger(int btnId) {
        ImageButton btn = findViewById(btnId);
        if (btn != null) btn.setOnClickListener(v -> toggleDrawer());
    }

    protected void toggleDrawer() {
        if (drawerLayout == null) return;
        if (drawerLayout.isDrawerOpen(Gravity.START))
            drawerLayout.closeDrawer(Gravity.START);
        else
            drawerLayout.openDrawer(Gravity.START);
    }

    protected void closeDrawer() {
        if (drawerLayout != null) drawerLayout.closeDrawer(Gravity.START);
    }

    private void navigateTo(Class<?> activityClass) {
        if (activityClass == getClass()) { closeDrawer(); return; }
        startActivity(new Intent(this, activityClass));
    }

    // =========================================================
    // MODE PMR
    // =========================================================
    private void onPmrToggleClicked(LinearLayout drawerPmr) {
        if (PmrManager.isEnabled()) {
            PmrManager.reset();
            TtsManager.speakForce("Mode accessibilité désactivé.");
            updatePmrDrawerLabel(drawerPmr);
        } else {
            // Init TTS lazily — only when the user actually enables PMR,
            // to avoid triggering the system voice-data download dialog on startup.
            TtsManager.init(this);
            TtsManager.speakForce(
                "Mode accessibilité activé. "
              + "Veuillez sélectionner votre profil de mobilité.");
            showPmrProfileDialog(drawerPmr);
        }
    }

    private void showPmrProfileDialog(LinearLayout drawerPmr) {
        PmrDialogHelper.showProfileDialog(this, () -> {
            if (drawerPmr != null) updatePmrDrawerLabel(drawerPmr);
        });
    }

    private void updatePmrDrawerLabel(LinearLayout drawerPmr) {
        for (int i = 0; i < drawerPmr.getChildCount(); i++) {
            View child = drawerPmr.getChildAt(i);
            if (child instanceof TextView) {
                String label = PmrManager.isEnabled()
                    ? "♿ Accessibilité ON"
                    : "♿ Accessibilité OFF";
                ((TextView) child).setText(label);
                break;
            }
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Voulez-vous vraiment vous déconnecter ?")
                .setPositiveButton("Oui", (d, w) -> {
                    TokenManager.clearToken(this);
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
}
