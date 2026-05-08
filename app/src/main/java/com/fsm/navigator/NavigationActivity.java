package com.fsm.navigator;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.fsm.navigator.navigation.NavigationGraph;
import com.fsm.navigator.navigation.NavigationManager;
import com.fsm.navigator.navigation.NavigationNode;
import com.fsm.navigator.navigation.NavigationView;

/**
 * NavigationActivity.java – Écran de navigation en temps réel
 *
 * Reçoit via Intent :
 *   "TARGET_NODE_ID" → ex: "B3_RDC_305"
 *   "TARGET_NOM"     → ex: "Salle 305"
 *   "TARGET_BLOC_ID" → ex: "B3"
 */
public class NavigationActivity extends AppCompatActivity {

    private NavigationView    navView;
    private NavigationManager navManager;
    private ImageButton       btnBack, btnStop;
    private TextView          tvDestination, tvInstruction, tvDistance, tvStatus;
    private CardView          cardInstruction;
    private View              progressNav;

    private String targetNodeId;
    private String targetNom;
    private String targetBlocId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        targetNodeId = getIntent().getStringExtra("TARGET_NODE_ID");
        targetNom    = getIntent().getStringExtra("TARGET_NOM");
        targetBlocId = getIntent().getStringExtra("TARGET_BLOC_ID");

        if (targetNodeId == null) { finish(); return; }

        initViews();
        setupListeners();
        startNavigation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (navManager != null) navManager.stopNavigation();
    }

    private void initViews() {
        navView        = findViewById(R.id.navigationView);
        btnBack        = findViewById(R.id.btnBack);
        btnStop        = findViewById(R.id.btnStopNav);
        tvDestination  = findViewById(R.id.tvNavDestination);
        tvInstruction  = findViewById(R.id.tvNavInstruction);
        tvDistance     = findViewById(R.id.tvNavDistance);
        tvStatus       = findViewById(R.id.tvNavStatus);
        cardInstruction= findViewById(R.id.cardNavInstruction);
        progressNav    = findViewById(R.id.progressNav);

        tvDestination.setText("→ " + targetNom);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnStop.setOnClickListener(v -> {
            if (navManager != null) navManager.stopNavigation();
            finish();
        });
    }

    private void startNavigation() {
        progressNav.setVisibility(View.VISIBLE);
        tvStatus.setText("Localisation en cours...");

        navManager = new NavigationManager(this);
        navManager.startNavigation(targetNodeId, targetNom,
                new NavigationManager.NavigationCallback() {

                    @Override
                    public void onPositionUpdated(NavigationNode current,
                                                  NavigationGraph.NavPath path) {
                        progressNav.setVisibility(View.GONE);

                        // Récupérer le nœud destination
                        NavigationNode destNode = navManager.getGraph().getNode(targetNodeId);

                        // Mettre à jour la vue Canvas
                        navView.setNavigationData(
                                navManager.getGraph(), path, current, destNode);

                        // Mettre à jour les instructions
                        if (path != null && !path.instructions.isEmpty()) {
                            tvInstruction.setText(path.instructions.get(0));
                            tvDistance.setText(String.format("%.0f m restants",
                                    path.totalDistance));
                            cardInstruction.setVisibility(View.VISIBLE);
                        }

                        tvStatus.setText("📍 " + current.nom);
                    }

                    @Override
                    public void onArrived(String destination) {
                        progressNav.setVisibility(View.GONE);
                        tvInstruction.setText("🎉 Vous êtes arrivé à " + destination + " !");
                        tvDistance.setText("0 m");
                        tvStatus.setText("Destination atteinte");
                        cardInstruction.setVisibility(View.VISIBLE);
                        Toast.makeText(NavigationActivity.this,
                                "🎉 Vous êtes arrivé à " + destination,
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String message) {
                        progressNav.setVisibility(View.GONE);
                        tvStatus.setText("⚠️ " + message);
                    }
                });
    }
}