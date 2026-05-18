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

import com.fsm.navigator.auth.TokenManager;

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
