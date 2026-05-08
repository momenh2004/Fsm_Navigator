package com.fsm.navigator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fsm.navigator.auth.AuthService;
import com.fsm.navigator.auth.PmrManager;
import com.fsm.navigator.auth.TokenManager;

/**
 * ProfileActivity.java – Page profil utilisateur
 * Étend BaseDrawerActivity pour le hamburger menu.
 */
public class ProfileActivity extends BaseDrawerActivity {

    private static final String PREF_HISTORY = "fsm_history";
    private static final String KEY_HISTORY  = "history_items";
    private static final int    MAX_HISTORY  = 10;

    private TextView     tvAvatarInitial, tvProfileName, tvProfileEmail;
    private TextView     tvProfileFullName, tvProfileRole;
    private TextView     tvClearHistory, tvHistoryEmpty;
    private LinearLayout layoutHistoryItems;
    private LinearLayout btnChangePassword, btnLogout, btnDeleteAccount;
    private TextView     tvAdminAccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupDrawer();
        setupHamburger(R.id.btnHamburger);
        loadUserData();
        loadHistory();
        setupListeners();
    }

    // =========================================================
    private void initViews() {
        tvAvatarInitial   = findViewById(R.id.tvAvatarInitial);
        tvProfileName     = findViewById(R.id.tvProfileName);
        tvProfileEmail    = findViewById(R.id.tvProfileEmail);
        tvProfileFullName = findViewById(R.id.tvProfileFullName);
        tvProfileRole     = findViewById(R.id.tvProfileRole);
        tvClearHistory    = findViewById(R.id.tvClearHistory);
        tvHistoryEmpty    = findViewById(R.id.tvHistoryEmpty);
        layoutHistoryItems= findViewById(R.id.layoutHistoryItems);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout         = findViewById(R.id.btnLogout);
        btnDeleteAccount  = findViewById(R.id.btnDeleteAccount);
        tvAdminAccess     = findViewById(R.id.tvAdminAccess);
    }

    // =========================================================
    private void loadUserData() {
        String email = TokenManager.getEmail(this);
        String role  = TokenManager.getRole(this);

        if (email == null || email.isEmpty()) email = "utilisateur@fsm.rnu.tn";

        String namePart    = email.contains("@") ? email.split("@")[0] : email;
        String displayName = namePart.substring(0, 1).toUpperCase() + namePart.substring(1);

        if (tvAvatarInitial != null) tvAvatarInitial.setText(String.valueOf(displayName.charAt(0)));
        if (tvProfileName   != null) tvProfileName.setText(displayName);
        if (tvProfileEmail  != null) tvProfileEmail.setText(email);
        if (tvProfileFullName != null) tvProfileFullName.setText(displayName);

        String roleLabel = "ETUDIANT".equals(role) ? "Étudiant"
                : "VISITEUR".equals(role) ? "Visiteur"
                : role != null ? role : "Étudiant";
        if (tvProfileRole != null) tvProfileRole.setText(roleLabel);
    }

    // =========================================================
    private void loadHistory() {
        String[] items = getHistory(this);
        layoutHistoryItems.removeAllViews();

        if (items == null || items.length == 0) {
            if (tvHistoryEmpty != null) tvHistoryEmpty.setVisibility(View.VISIBLE);
            return;
        }

        if (tvHistoryEmpty != null) tvHistoryEmpty.setVisibility(View.GONE);

        for (String item : items) {
            if (item == null || item.isEmpty()) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 16, 0, 16);
            row.setClickable(true);
            row.setFocusable(true);

            TextView tv = new TextView(this);
            tv.setText("🕐  " + item);
            tv.setTextColor(getResources().getColor(R.color.text_primary, null));
            tv.setTextSize(14);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            final String query = item;
            row.setOnClickListener(v -> {
                Intent intent = new Intent(this, SearchActivity.class);
                intent.putExtra("QUERY", query);
                startActivity(intent);
            });

            row.addView(tv);
            layoutHistoryItems.addView(row);

            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(
                    getResources().getColor(R.color.glass_border, null));
            layoutHistoryItems.addView(divider);
        }
    }

    // =========================================================
    private void setupListeners() {

        if (tvClearHistory != null) tvClearHistory.setOnClickListener(v -> {
            clearHistory(this);
            layoutHistoryItems.removeAllViews();
            if (tvHistoryEmpty != null) tvHistoryEmpty.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Historique effacé", Toast.LENGTH_SHORT).show();
        });

        if (btnChangePassword != null) btnChangePassword.setOnClickListener(v ->
                startActivity(new Intent(this, ChangePasswordActivity.class)));

        if (btnLogout != null) btnLogout.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Déconnexion")
                        .setMessage("Voulez-vous vraiment vous déconnecter ?")
                        .setPositiveButton("Oui", (d, w) -> {
                            TokenManager.clearToken(this);
                            clearHistory(this);
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        })
                        .setNegativeButton("Annuler", null)
                        .show());

        if (btnDeleteAccount != null) btnDeleteAccount.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Supprimer le compte")
                        .setMessage("Cette action est irréversible. Voulez-vous vraiment supprimer votre compte ?")
                        .setPositiveButton("Supprimer", (d, w) -> {
                            String token = TokenManager.getToken(this);
                            AuthService.deleteAccount(token, new AuthService.SimpleCallback() {
                                @Override public void onSuccess(String msg) {
                                    TokenManager.clearToken(ProfileActivity.this);
                                    clearHistory(ProfileActivity.this);
                                    PmrManager.reset();
                                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                }
                                @Override public void onError(String msg) {
                                    Toast.makeText(ProfileActivity.this, msg, Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Annuler", null)
                        .show());

        if (tvAdminAccess != null) tvAdminAccess.setOnClickListener(v ->
                startActivity(new Intent(this, com.fsm.navigator.admin.AdminLoginActivity.class)));
    }

    // =========================================================
    // API STATIQUE — Historique
    // =========================================================
    public static void addToHistory(Context ctx, String item) {
        if (item == null || item.trim().isEmpty()) return;
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_HISTORY, Context.MODE_PRIVATE);
        String existing = prefs.getString(KEY_HISTORY, "");
        StringBuilder sb = new StringBuilder();
        sb.append(item.trim());
        if (!existing.isEmpty()) {
            String[] parts = existing.split("\\|");
            int count = 1;
            for (String part : parts) {
                if (!part.trim().equalsIgnoreCase(item.trim()) && count < MAX_HISTORY) {
                    sb.append("|").append(part.trim());
                    count++;
                }
            }
        }
        prefs.edit().putString(KEY_HISTORY, sb.toString()).apply();
    }

    public static String[] getHistory(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_HISTORY, Context.MODE_PRIVATE);
        String stored = prefs.getString(KEY_HISTORY, "");
        if (stored.isEmpty()) return new String[0];
        return stored.split("\\|");
    }

    public static void clearHistory(Context ctx) {
        ctx.getSharedPreferences(PREF_HISTORY, Context.MODE_PRIVATE)
                .edit().remove(KEY_HISTORY).apply();
    }
}