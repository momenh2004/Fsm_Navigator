package com.fsm.navigator;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.fsm.navigator.auth.PmrManager;
import com.fsm.navigator.auth.TokenManager;

/**
 * MainActivity.java – Page d'accueil avec Navigation Drawer
 * Étend BaseDrawerActivity pour le hamburger menu.
 */
public class MainActivity extends BaseDrawerActivity {

    private TextView  tvWelcome, tvPmrBadge, tvAvatarInitial, etSearch;
    private CardView  cardDest1, cardDest2, cardDest3;
    private android.widget.LinearLayout catAmphitheatre, catAdmin, catBiblio, catDept;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupWelcome();
        setupDrawer();
        setupHamburger(R.id.btnHamburger);
        setupSearchBar();
        setupQuickAccessCategories();
        setupPopularDestinations();
    }

    private void initViews() {
        tvWelcome       = findViewById(R.id.tvWelcome);
        tvPmrBadge      = findViewById(R.id.tvPmrBadge);
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        etSearch        = findViewById(R.id.etSearch);
        cardDest1       = findViewById(R.id.cardDest1);
        cardDest2       = findViewById(R.id.cardDest2);
        cardDest3       = findViewById(R.id.cardDest3);
        catAmphitheatre = findViewById(R.id.catAmphitheatre);
        catAdmin        = findViewById(R.id.catAdmin);
        catBiblio       = findViewById(R.id.catBiblio);
        catDept         = findViewById(R.id.catDept);
    }

    private void setupWelcome() {
        String email = TokenManager.getEmail(this);
        if (email != null && !email.isEmpty()) {
            String name = email.contains("@") ? email.split("@")[0] : email;
            String display = name.substring(0,1).toUpperCase() + name.substring(1);
            tvWelcome.setText("Bonjour " + display + " !");
            if (tvAvatarInitial != null)
                tvAvatarInitial.setText(String.valueOf(display.charAt(0)));
        } else {
            tvWelcome.setText("Bienvenue à la FSM !");
        }
        tvPmrBadge.setVisibility(PmrManager.isEnabled() ? View.VISIBLE : View.GONE);
    }

    private void setupSearchBar() {
        if (etSearch != null) {
            etSearch.setFocusable(false);
            etSearch.setOnClickListener(v ->
                    startActivity(new Intent(this, SearchActivity.class)));
        }
    }

    private void setupQuickAccessCategories() {
        if (catAmphitheatre != null)
            catAmphitheatre.setOnClickListener(v -> goToBlocPlan("B1", "Bloc 1 (Palestine)"));
        if (catAdmin != null)
            catAdmin.setOnClickListener(v -> openSearchWithFilter("Administration"));
        if (catBiblio != null)
            catBiblio.setOnClickListener(v -> openSearchWithFilter("Bibliothèque"));
        if (catDept != null)
            catDept.setOnClickListener(v -> openSearchWithFilter("Départements"));
    }

    private void setupPopularDestinations() {
        if (cardDest1 != null)
            cardDest1.setOnClickListener(v ->
                    goToNavigation("B1_RDC_AMPHI_A", "Amphithéâtre A", "B1"));
        if (cardDest2 != null)
            cardDest2.setOnClickListener(v ->
                    goToNavigation("B3_RDC_301", "Salle 301", "B3"));
        if (cardDest3 != null)
            cardDest3.setOnClickListener(v ->
                    goToNavigation("BIB", "Bibliothèque Centrale", "BIB"));
    }

    private void goToBlocPlan(String blocId, String blocNom) {
        Intent intent = new Intent(this, BlockDetailActivity.class);
        intent.putExtra("BLOC_ID",  blocId);
        intent.putExtra("BLOC_NOM", blocNom);
        startActivity(intent);
    }

    private void openSearchWithFilter(String filter) {
        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra("FILTER", filter);
        startActivity(intent);
    }

    private void goToNavigation(String nodeId, String nom, String blocId) {
        Intent intent = new Intent(this, NavigationActivity.class);
        intent.putExtra("TARGET_NODE_ID",  nodeId);
        intent.putExtra("TARGET_NOM",      nom);
        intent.putExtra("TARGET_BLOC_ID",  blocId);
        startActivity(intent);
    }
}