package com.fsm.navigator.controller;

import com.fsm.navigator.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fsm.navigator.auth.AuthService;
import com.fsm.navigator.auth.PmrManager;
import com.fsm.navigator.auth.TokenManager;
import com.fsm.navigator.service.FavoriService;

import java.util.List;

public class ProfileActivity extends BaseDrawerActivity {

    private static final String PREF_HISTORY = "fsm_history";
    private static final String KEY_HISTORY  = "history_items";
    private static final int    MAX_HISTORY  = 10;

    private TextView     tvAvatarInitial, tvProfileName, tvProfileEmail;
    private TextView     tvProfileFullName, tvProfileRole;
    private TextView     tvClearHistory, tvHistoryEmpty;
    private LinearLayout layoutHistoryItems;
    private LinearLayout btnChangePassword, btnLogout, btnDeleteAccount;
    private LinearLayout btnToggleFavoris, btnToggleHistory;

    // Favoris
    private LinearLayout layoutFavorisItems;
    private TextView     tvFavorisEmpty;
    private ProgressBar  progressFavoris;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!TokenManager.isLoggedIn(this)) {
            Toast.makeText(getApplicationContext(),
                    "Connectez-vous pour accéder à votre profil",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupDrawer();
        setupHamburger(R.id.btnHamburger);
        loadUserData();
        loadHistory();
        loadFavoris();
        setupListeners();
    }

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
        layoutFavorisItems= findViewById(R.id.layoutFavorisItems);
        tvFavorisEmpty    = findViewById(R.id.tvFavorisEmpty);
        progressFavoris   = findViewById(R.id.progressFavoris);
        btnToggleFavoris  = findViewById(R.id.btnToggleFavoris);
        btnToggleHistory  = findViewById(R.id.btnToggleHistory);
    }

    private void loadUserData() {
        String email = TokenManager.getEmail(this);
        String role  = TokenManager.getRole(this);
        if (email == null || email.isEmpty()) email = "utilisateur@fsm.rnu.tn";
        String namePart    = email.contains("@") ? email.split("@")[0] : email;
        String displayName = namePart.substring(0, 1).toUpperCase() + namePart.substring(1);
        if (tvAvatarInitial  != null) tvAvatarInitial.setText(String.valueOf(displayName.charAt(0)));
        if (tvProfileName    != null) tvProfileName.setText(displayName);
        if (tvProfileEmail   != null) tvProfileEmail.setText(email);
        if (tvProfileFullName!= null) tvProfileFullName.setText(displayName);
        String roleLabel = "ETUDIANT".equals(role) ? "Étudiant"
                : "VISITEUR".equals(role) ? "Visiteur"
                : role != null ? role : "Étudiant";
        if (tvProfileRole != null) tvProfileRole.setText(roleLabel);
    }

    // ── FAVORIS ───────────────────────────────────────────────
    private void loadFavoris() {
        if (layoutFavorisItems == null) return;
        if (progressFavoris != null) progressFavoris.setVisibility(View.VISIBLE);

        FavoriService.getFavoris(this, new FavoriService.ListCallback() {
            @Override
            public void onSuccess(List<FavoriService.FavoriItem> favoris) {
                if (progressFavoris != null) progressFavoris.setVisibility(View.GONE);
                layoutFavorisItems.removeAllViews();
                if (favoris.isEmpty()) {
                    if (tvFavorisEmpty != null) tvFavorisEmpty.setVisibility(View.VISIBLE);
                    return;
                }
                if (tvFavorisEmpty != null) tvFavorisEmpty.setVisibility(View.GONE);
                for (FavoriService.FavoriItem item : favoris) addFavoriRow(item);
            }

            @Override
            public void onError(String message) {
                if (progressFavoris != null) progressFavoris.setVisibility(View.GONE);
                if (tvFavorisEmpty  != null) tvFavorisEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void addFavoriRow(FavoriService.FavoriItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(12), 0, dp(12));
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Icône
        TextView tvIcon = new TextView(this);
        tvIcon.setText("SALLE".equals(item.type) ? "🚪" : "🏢");
        tvIcon.setTextSize(18);
        tvIcon.setPadding(0, 0, dp(12), 0);
        row.addView(tvIcon);

        // Nom + sous-titre
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView tvNom = new TextView(this);
        tvNom.setText(item.nom);
        tvNom.setTextColor(getResources().getColor(R.color.text_primary, null));
        tvNom.setTextSize(14);
        col.addView(tvNom);
        if (item.blocNom != null && !item.blocNom.isEmpty()) {
            TextView tvSub = new TextView(this);
            tvSub.setText(item.blocNom);
            tvSub.setTextColor(getResources().getColor(R.color.text_secondary, null));
            tvSub.setTextSize(12);
            col.addView(tvSub);
        }
        row.addView(col);

        // Bouton Naviguer
        TextView btnNav = new TextView(this);
        btnNav.setText("Y aller");
        btnNav.setTextColor(0xFF1A1A2E);
        btnNav.setBackgroundResource(R.drawable.bg_button_cyan);
        btnNav.setTextSize(12f);
        btnNav.setPadding(dp(12), dp(6), dp(12), dp(6));
        btnNav.setOnClickListener(v -> navigateToFavori(item));
        row.addView(btnNav);

        // Bouton Supprimer
        ImageButton btnDel = new ImageButton(this);
        btnDel.setImageResource(android.R.drawable.ic_menu_delete);
        btnDel.setBackgroundResource(android.R.color.transparent);
        btnDel.setPadding(dp(8), 0, 0, 0);
        btnDel.setColorFilter(0xFFFF4B6E);
        btnDel.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Supprimer le favori")
                        .setMessage("Retirer \"" + item.nom + "\" des favoris ?")
                        .setPositiveButton("Oui", (d, w) ->
                                FavoriService.deleteFavori(this, item.id,
                                        new FavoriService.SimpleCallback() {
                                            @Override public void onSuccess(String msg) {
                                                Toast.makeText(getApplicationContext(),
                                                        "Retiré des favoris", Toast.LENGTH_SHORT).show();
                                                loadFavoris();
                                            }
                                            @Override public void onError(String msg) {
                                                Toast.makeText(getApplicationContext(),
                                                        "Erreur : " + msg, Toast.LENGTH_SHORT).show();
                                            }
                                        }))
                        .setNegativeButton("Annuler", null)
                        .show());
        row.addView(btnDel);

        layoutFavorisItems.addView(row);

        // Divider
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(getResources().getColor(R.color.glass_border, null));
        layoutFavorisItems.addView(divider);
    }

    private void navigateToFavori(FavoriService.FavoriItem item) {
        if ("SALLE".equals(item.type)) {
            String nodeId = item.blocCode + "_RDC_" +
                    item.nom.replaceAll("[^0-9]", "");
            Intent intent = new Intent(this, NavigationActivity.class);
            intent.putExtra("TARGET_NODE_ID", nodeId);
            intent.putExtra("TARGET_NOM",     item.nom);
            intent.putExtra("TARGET_BLOC_ID", item.blocCode);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, BlockDetailActivity.class);
            intent.putExtra("BLOC_ID",  item.blocCode);
            intent.putExtra("BLOC_NOM", item.nom);
            startActivity(intent);
        }
    }

    // ── HISTORIQUE ────────────────────────────────────────────
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
            row.setClickable(true); row.setFocusable(true);
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
            divider.setBackgroundColor(getResources().getColor(R.color.glass_border, null));
            layoutHistoryItems.addView(divider);
        }
    }

    private void setupListeners() {
        // Toggle sections dépliables
        if (btnToggleFavoris != null && layoutFavorisItems != null) {
            btnToggleFavoris.setOnClickListener(v -> {
                boolean visible = layoutFavorisItems.getVisibility() == View.VISIBLE;
                layoutFavorisItems.setVisibility(visible ? View.GONE : View.VISIBLE);
                View chevron = btnToggleFavoris.findViewById(R.id.chevronFavoris);
                if (chevron != null) chevron.setRotation(visible ? 0 : 90);
            });
        }
        if (btnToggleHistory != null && layoutHistoryItems != null) {
            btnToggleHistory.setOnClickListener(v -> {
                boolean visible = layoutHistoryItems.getVisibility() == View.VISIBLE;
                layoutHistoryItems.setVisibility(visible ? View.GONE : View.VISIBLE);
                View chevron = btnToggleHistory.findViewById(R.id.chevronHistory);
                if (chevron != null) chevron.setRotation(visible ? 0 : 90);
            });
        }

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
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        })
                        .setNegativeButton("Annuler", null).show());

        if (btnDeleteAccount != null) btnDeleteAccount.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Supprimer le compte")
                        .setMessage("Cette action est irréversible.")
                        .setPositiveButton("Supprimer", (d, w) -> {
                            AuthService.deleteAccount(TokenManager.getToken(this),
                                    new AuthService.SimpleCallback() {
                                        @Override public void onSuccess(String msg) {
                                            TokenManager.clearToken(ProfileActivity.this);
                                            clearHistory(ProfileActivity.this);
                                            PmrManager.reset();
                                            Intent i = new Intent(ProfileActivity.this, LoginActivity.class);
                                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(i);
                                        }
                                        @Override public void onError(String msg) {
                                            Toast.makeText(ProfileActivity.this,
                                                    msg, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        })
                        .setNegativeButton("Annuler", null).show());

    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }

    // ── API STATIQUE — Historique ─────────────────────────────
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
