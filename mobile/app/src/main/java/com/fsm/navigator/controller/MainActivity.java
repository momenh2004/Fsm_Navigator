package com.fsm.navigator.controller;

import com.fsm.navigator.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fsm.navigator.auth.PmrManager;
import com.fsm.navigator.auth.TokenManager;
import com.fsm.navigator.auth.TtsManager;

public class MainActivity extends BaseDrawerActivity {

    // Shortcuts: label, icon drawable res, target (bloc or search filter)
    private static final Object[][] SHORTCUTS = {
        {"Amphithéâtres", android.R.drawable.ic_menu_agenda,    "bloc",   "B1", "Bloc 1 (Palestine)"},
        {"Bibliothèque",  android.R.drawable.ic_menu_info_details, "search", "Bibliothèque", null},
        {"Labos Info",    android.R.drawable.ic_menu_manage,    "search", "Informatique", null},
        {"Départements",  android.R.drawable.ic_menu_compass,   "search", "Départements", null},
    };

    // Recents: title, subtitle, target node, name, bloc
    private static final String[][] RECENTS = {
        {"Amphithéâtre A",       "Bloc 1 Palestine · RDC", "BP_AA",    "Amphithéâtre A",       "B1"},
        {"Salle 301",            "Bloc 3 Informatique",    "B3_RDC_301","Salle 301",            "B3"},
        {"Bibliothèque Centrale","Bâtiment central",       "BIB",       "Bibliothèque Centrale","BIB"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupWelcome();
        setupDrawer();
        setupSearchHero();
        setupAvatarBtn();
        bindShortcuts();
        bindRecents();
        setupMapBtn();
        announceScreenIfVisuallyImpaired();
    }

    private void setupWelcome() {
        TextView greetingName = findViewById(R.id.greetingName);
        TextView avatarInitial = findViewById(R.id.avatarInitial);
        String email = TokenManager.getEmail(this);
        if (email != null && !email.isEmpty()) {
            String name = email.contains("@") ? email.split("@")[0] : email;
            String display = name.substring(0, 1).toUpperCase() + name.substring(1);
            if (greetingName != null) greetingName.setText(display);
            if (avatarInitial != null) avatarInitial.setText(String.valueOf(display.charAt(0)));
        }
    }

    private void setupSearchHero() {
        View searchHero = findViewById(R.id.searchHero);
        if (searchHero != null)
            searchHero.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
    }

    private void setupAvatarBtn() {
        View avatarBtn = findViewById(R.id.avatarBtn);
        if (avatarBtn != null)
            avatarBtn.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void bindShortcuts() {
        int[] ids = {R.id.shortcut1, R.id.shortcut2, R.id.shortcut3, R.id.shortcut4};
        for (int i = 0; i < ids.length; i++) {
            View card = findViewById(ids[i]);
            if (card == null) continue;
            TextView label = card.findViewById(R.id.shortcutLabel);
            ImageView icon = card.findViewById(R.id.shortcutIcon);
            if (label != null) label.setText((String) SHORTCUTS[i][0]);
            if (icon != null)  icon.setImageResource((int) SHORTCUTS[i][1]);

            final int idx = i;
            card.setOnClickListener(v -> onShortcutClicked(idx));
        }
    }

    private void onShortcutClicked(int idx) {
        String type = (String) SHORTCUTS[idx][2];
        if ("bloc".equals(type)) {
            Intent it = new Intent(this, BlockDetailActivity.class);
            it.putExtra("BLOC_ID",  (String) SHORTCUTS[idx][3]);
            it.putExtra("BLOC_NOM", (String) SHORTCUTS[idx][4]);
            startActivity(it);
        } else {
            Intent it = new Intent(this, SearchActivity.class);
            it.putExtra("FILTER", (String) SHORTCUTS[idx][3]);
            startActivity(it);
        }
    }

    private void bindRecents() {
        LinearLayout list = findViewById(R.id.recentList);
        if (list == null) return;
        LayoutInflater inf = LayoutInflater.from(this);
        for (String[] dest : RECENTS) {
            View row = inf.inflate(R.layout.item_recent, (ViewGroup) list, false);
            ((TextView) row.findViewById(R.id.rowTitle)).setText(dest[0]);
            ((TextView) row.findViewById(R.id.rowSub)).setText(dest[1]);
            final String nodeId = dest[2], name = dest[3], blocId = dest[4];
            row.setOnClickListener(v -> {
                Intent it = new Intent(this, NavigationActivity.class);
                it.putExtra("TARGET_NODE_ID", nodeId);
                it.putExtra("TARGET_NOM",     name);
                it.putExtra("TARGET_BLOC_ID", blocId);
                startActivity(it);
            });
            list.addView(row);
            // divider
            View div = new View(this);
            div.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            div.setBackgroundColor(getColor(R.color.divider_white));
            list.addView(div);
        }
        // FAB Naviguer
        View fab = findViewById(R.id.fabNaviguer);
        if (fab != null) fab.setOnClickListener(v -> startActivity(new Intent(this, NavigationActivity.class)));
    }

    private void setupMapBtn() {
        View btn = findViewById(R.id.btnAccederCarte);
        if (btn != null) btn.setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
    }

    private void announceScreenIfVisuallyImpaired() {
        if (PmrManager.getProfile() != PmrManager.PmrProfile.VISUALLY_IMPAIRED) return;
        String email = TokenManager.getEmail(this);
        String prenom = (email != null && email.contains("@"))
                ? email.split("@")[0] : "étudiant";
        TtsManager.speak(
            "Bienvenue " + prenom + " sur FSM Navigator. "
          + "Vous êtes sur l'écran d'accueil. "
          + "Options disponibles : Rechercher une salle, "
          + "Carte du campus, Navigation, Profil.");
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
