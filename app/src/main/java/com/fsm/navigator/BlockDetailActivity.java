package com.fsm.navigator;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.cardview.widget.CardView;

import com.fsm.navigator.navigation.NavigationGraph;

/**
 * BlockDetailActivity.java
 * Affiche le plan interne d'un bloc avec Navigation Drawer.
 * Étend BaseDrawerActivity pour le hamburger menu.
 */
public class BlockDetailActivity extends BaseDrawerActivity {

    private TextView       tvBlocTitle, tvBlocSubtitle, tvSalleNom, tvSalleDetails;
    private ImageButton    btnBack, btnHamburger;
    private BlocDetailView blocDetailView;
    private LinearLayout   btnNavigerSalle;
    private CardView cardSalleInfo;
    private TextView       btnRDC, btnEtage1;

    private String blocId, blocNom;
    private String selectedSalleNodeId = null;
    private String selectedSalleNom    = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_detail);

        blocId  = getIntent().getStringExtra("BLOC_ID");
        blocNom = getIntent().getStringExtra("BLOC_NOM");

        initViews();
        setupDrawer();
        setupHamburger(R.id.btnHamburger);
        setupPlan();
        setupListeners();
    }

    // =========================================================
    private void initViews() {
        tvBlocTitle    = findViewById(R.id.tvBlocTitle);
        tvBlocSubtitle = findViewById(R.id.tvBlocSubtitle);
        tvSalleNom     = findViewById(R.id.tvSalleNom);
        tvSalleDetails = findViewById(R.id.tvSalleDetails);
        btnBack        = findViewById(R.id.btnBack);
        btnHamburger   = findViewById(R.id.btnHamburger);
        blocDetailView = findViewById(R.id.blocDetailView);
        btnNavigerSalle= findViewById(R.id.btnNavigerSalle);
        cardSalleInfo  = findViewById(R.id.cardSalleInfo);
        btnRDC         = findViewById(R.id.btnRDC);
        btnEtage1      = findViewById(R.id.btnEtage1);

        tvBlocTitle.setText(blocNom != null ? blocNom : "Plan du bloc");
    }

    // =========================================================
    private void setupPlan() {
        if (blocDetailView == null) return;
        blocDetailView.setBlocId(blocId);

        // Clic sur une salle dans le Canvas → afficher la card infos
        blocDetailView.setOnSalleClickListener((salleNom, etage) -> {
            selectedSalleNom    = salleNom;
            selectedSalleNodeId = buildNodeId(blocId, etage, salleNom);

            if (tvSalleNom     != null) tvSalleNom.setText(salleNom);
            if (tvSalleDetails != null) tvSalleDetails.setText(
                    blocNom + " • " + (etage == 0 ? "RDC" : "1er étage"));
            if (cardSalleInfo  != null) cardSalleInfo.setVisibility(View.VISIBLE);
        });
    }

    // =========================================================
    private void setupListeners() {
        // Bouton retour
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Sélecteur d'étage
        if (btnRDC != null) btnRDC.setOnClickListener(v -> {
            blocDetailView.setEtage(0);
            btnRDC.setBackgroundResource(R.drawable.bg_filter_selected);
            btnRDC.setTextColor(getResources().getColor(R.color.accent_cyan, null));
            if (btnEtage1 != null) {
                btnEtage1.setBackgroundResource(R.drawable.bg_filter_unselected);
                btnEtage1.setTextColor(getResources().getColor(R.color.text_secondary, null));
            }
        });

        if (btnEtage1 != null) btnEtage1.setOnClickListener(v -> {
            blocDetailView.setEtage(1);
            btnEtage1.setBackgroundResource(R.drawable.bg_filter_selected);
            btnEtage1.setTextColor(getResources().getColor(R.color.accent_cyan, null));
            if (btnRDC != null) {
                btnRDC.setBackgroundResource(R.drawable.bg_filter_unselected);
                btnRDC.setTextColor(getResources().getColor(R.color.text_secondary, null));
            }
        });

        // Bouton Naviguer → NavigationActivity
        if (btnNavigerSalle != null) btnNavigerSalle.setOnClickListener(v -> {
            if (selectedSalleNodeId == null) return;
            Intent intent = new Intent(this, NavigationActivity.class);
            intent.putExtra("TARGET_NODE_ID",  selectedSalleNodeId);
            intent.putExtra("TARGET_NOM",      selectedSalleNom);
            intent.putExtra("TARGET_BLOC_ID",  blocId);
            startActivity(intent);
        });
    }

    // =========================================================
    // Construire l'ID du nœud A* depuis le bloc + étage + nom salle
    // Ex: "B3" + 0 + "Salle 301" → "B3_RDC_301"
    // =========================================================
    private String buildNodeId(String blocId, int etage, String salleNom) {
        if ("B1".equals(blocId)) {
            String n = salleNom.toUpperCase();
            if (n.contains("AMPHI A")) return "BP_AA";
            if (n.contains("AMPHI B")) return "BP_AB";
            if (n.contains("AMPHI C")) return "BP_AC";
            if (n.contains("AMPHI D")) return "BP_AD";
            String num = salleNom.replaceAll("[^0-9]", "");
            return "BP_" + (num.isEmpty() ? salleNom.replace(" ", "_") : num);
        }
        String num = salleNom.replaceAll("[^0-9]", "");
        if (num.isEmpty()) num = salleNom.replace(" ", "_");
        String etageCode = etage == 0 ? "RDC" : "E1";
        return blocId + "_" + etageCode + "_" + num;
    }

    // =========================================================
    // Interface callback pour BlocDetailView
    // =========================================================
    public interface OnSalleClickListener {
        void onSalleClick(String salleNom, int etage);
    }
}