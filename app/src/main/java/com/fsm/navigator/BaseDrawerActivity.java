package com.fsm.navigator;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.fsm.navigator.auth.TokenManager;
import com.fsm.navigator.admin.AdminLoginActivity;

/**
 * BaseDrawerActivity.java
 * Classe parent pour toutes les activités avec Navigation Drawer.
 */
public abstract class BaseDrawerActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;

    protected void setupDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout);
        if (drawerLayout == null) return;

        // Remplir infos utilisateur
        String email = TokenManager.getEmail(this);
        if (email != null && !email.isEmpty()) {
            String name        = email.contains("@") ? email.split("@")[0] : email;
            String displayName = name.substring(0,1).toUpperCase() + name.substring(1);

            TextView drawerName   = findViewById(R.id.drawerName);
            TextView drawerEmail  = findViewById(R.id.drawerEmail);
            TextView drawerAvatar = findViewById(R.id.drawerAvatarInitial);

            if (drawerName   != null) drawerName.setText(displayName);
            if (drawerEmail  != null) drawerEmail.setText(email);
            if (drawerAvatar != null) drawerAvatar.setText(String.valueOf(displayName.charAt(0)));
        }

        setupDrawerItems();
    }

    private void setupDrawerItems() {
        // Accueil
        setupDrawerItem(R.id.drawerHome, () -> navigateTo(MainActivity.class));

        // Carte
        setupDrawerItem(R.id.drawerMap, () -> navigateTo(MapActivity.class));

        // Recherche
        setupDrawerItem(R.id.drawerSearch, () -> navigateTo(SearchActivity.class));

        // Navigation — ouvre SearchActivity pour choisir la destination
        // NavigationActivity nécessite des extras obligatoires donc ne peut pas
        // être lancée directement depuis le drawer
        setupDrawerItem(R.id.drawerNav, () -> navigateTo(SearchActivity.class));

        // Profil
        setupDrawerItem(R.id.drawerProfile, () -> navigateTo(ProfileActivity.class));

        // Admin
        setupDrawerItem(R.id.drawerAdmin, () -> navigateTo(AdminLoginActivity.class));

        // Déconnexion
        setupDrawerItem(R.id.drawerLogout, this::showLogoutDialog);
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
        if (activityClass == getClass()) {
            closeDrawer();
            return;
        }
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