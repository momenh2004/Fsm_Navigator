package com.fsm.navigator;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fsm.navigator.model.PointInteret;
import com.fsm.navigator.navigation.NavigationGraph;
import com.fsm.navigator.navigation.NavigationManager;
import com.fsm.navigator.navigation.NavigationNode;
import com.fsm.navigator.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * NavigationActivity.java
 *
 * Deux modes :
 *   1. Avec extras (TARGET_NODE_ID) → navigation directe
 *   2. Sans extras → affiche écran de recherche de destination
 */
public class NavigationActivity extends AppCompatActivity {

    private static final String BASE_URL = AppConfig.BASE_URL;

    // ── Mode navigation ──────────────────────────────────────
    private NavigationView    navView;
    private NavigationManager navManager;
    private ImageButton       btnBack, btnStop;
    private TextView          tvDestination, tvInstruction, tvDistance, tvStatus;
    private CardView          cardInstruction;
    private View              progressNav;

    // ── Mode recherche ───────────────────────────────────────
    private LinearLayout      layoutSearch, layoutNavigation;
    private EditText          etSearchDest;
    private RecyclerView      recyclerDest;
    private TextView          tvSearchEmpty;
    private View              progressSearch;

    private String targetNodeId;
    private String targetNom;
    private String targetBlocId;

    private List<PointInteret> allPoi      = new ArrayList<>();
    private List<PointInteret> filteredPoi = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        targetNodeId = getIntent().getStringExtra("TARGET_NODE_ID");
        targetNom    = getIntent().getStringExtra("TARGET_NOM");
        targetBlocId = getIntent().getStringExtra("TARGET_BLOC_ID");

        initViews();
        setupListeners();

        if (targetNodeId != null) {
            showNavigationMode();
            navManager = new NavigationManager(this);
            if (!navManager.isConnectedToFsmWifi()) {
                showOfflineDialog();
            } else {
                startNavigation();
            }
        }
        if (targetBlocId != null) {
            navView.setBlocId(targetBlocId); // On appelle la méthode qu'on va créer
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (navManager != null) navManager.stopNavigation();
    }

    // =========================================================
    private void initViews() {
        // Navigation
        navView         = findViewById(R.id.navigationView);
        btnBack         = findViewById(R.id.btnBack);
        btnStop         = findViewById(R.id.btnStopNav);
        tvDestination   = findViewById(R.id.tvNavDestination);
        tvInstruction   = findViewById(R.id.tvNavInstruction);
        tvDistance      = findViewById(R.id.tvNavDistance);
        tvStatus        = findViewById(R.id.tvNavStatus);
        cardInstruction = findViewById(R.id.cardNavInstruction);
        progressNav     = findViewById(R.id.progressNav);

        // Recherche
        layoutSearch    = findViewById(R.id.layoutSearch);
        layoutNavigation= findViewById(R.id.layoutNavigation);
        etSearchDest    = findViewById(R.id.etSearchDest);
        recyclerDest    = findViewById(R.id.recyclerDest);
        tvSearchEmpty   = findViewById(R.id.tvSearchEmpty);
        progressSearch  = findViewById(R.id.progressSearch);

        if (recyclerDest != null)
            recyclerDest.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnStop != null) btnStop.setOnClickListener(v -> {
            if (navManager != null) navManager.stopNavigation();
            // Retour à la recherche
            showSearchMode();
            targetNodeId = null;
        });

        if (etSearchDest != null) etSearchDest.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void afterTextChanged(Editable s) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                filterDestinations(s.toString().trim());
            }
        });
    }

    // =========================================================
    // MODES
    // =========================================================
    private void showSearchMode() {
        if (layoutSearch     != null) layoutSearch.setVisibility(View.VISIBLE);
        if (layoutNavigation != null) layoutNavigation.setVisibility(View.GONE);
    }

    private void showNavigationMode() {
        if (layoutSearch     != null) layoutSearch.setVisibility(View.GONE);
        if (layoutNavigation != null) layoutNavigation.setVisibility(View.VISIBLE);
        if (tvDestination    != null) tvDestination.setText("→ " + targetNom);
    }

    // =========================================================
    // CHARGEMENT DES DESTINATIONS
    // =========================================================
    private void loadDestinations() {
        if (progressSearch != null) progressSearch.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/api/blocs");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                if (conn.getResponseCode() != 200) {
                    runOnUiThread(() -> {
                        if (progressSearch != null) progressSearch.setVisibility(View.GONE);
                    });
                    return;
                }
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = br.readLine()) != null) sb.append(line); br.close();

                List<PointInteret> pois = parseBlocs(sb.toString());
                runOnUiThread(() -> {
                    if (progressSearch != null) progressSearch.setVisibility(View.GONE);
                    allPoi = pois;
                    filteredPoi = new ArrayList<>(allPoi);
                    updateRecycler();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (progressSearch != null) progressSearch.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private List<PointInteret> parseBlocs(String json) throws Exception {
        List<PointInteret> list = new ArrayList<>();
        JSONArray blocs = new JSONArray(json);
        int idCounter = 1;
        for (int i = 0; i < blocs.length(); i++) {
            JSONObject bloc   = blocs.getJSONObject(i);
            String blocNom    = bloc.optString("nom", "");
            String blocCode   = bloc.optString("code", "");
            JSONArray etages  = bloc.optJSONArray("etages");
            if (etages == null) continue;
            for (int j = 0; j < etages.length(); j++) {
                JSONObject etage      = etages.getJSONObject(j);
                String     etageLabel = etage.optString("label", "RDC");
                JSONArray  salles     = etage.optJSONArray("salles");
                if (salles == null) continue;
                for (int k = 0; k < salles.length(); k++) {
                    JSONObject salle    = salles.getJSONObject(k);
                    long       salleId  = salle.optLong("id", idCounter);
                    String     salleNom = salle.optString("nom", "");
                    String     categorie= salle.optString("categorie", "Salle");
                    PointInteret poi = new PointInteret(
                            (int) salleId, salleNom, categorie, blocNom, etageLabel);
                    poi.setBlocId(blocCode);
                    list.add(poi);
                    idCounter++;
                }
            }
        }
        return list;
    }

    // =========================================================
    // FILTRAGE
    // =========================================================
    private void filterDestinations(String query) {
        filteredPoi.clear();
        for (PointInteret poi : allPoi) {
            if (query.isEmpty()
                    || poi.getNom().toLowerCase().contains(query.toLowerCase())
                    || poi.getBatiment().toLowerCase().contains(query.toLowerCase())) {
                filteredPoi.add(poi);
            }
        }
        updateRecycler();
    }

    private void updateRecycler() {
        if (recyclerDest == null) return;
        boolean empty = filteredPoi.isEmpty();
        if (tvSearchEmpty != null) tvSearchEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (recyclerDest  != null) recyclerDest.setVisibility(empty ? View.GONE : View.VISIBLE);

        recyclerDest.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                android.widget.LinearLayout row = new android.widget.LinearLayout(NavigationActivity.this);
                row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                row.setPadding(dp(16), dp(14), dp(16), dp(14));
                row.setBackgroundColor(0x11FFFFFF);
                android.view.ViewGroup.LayoutParams lp = new android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                row.setLayoutParams(lp);

                // Icône
                TextView tvIcon = new TextView(NavigationActivity.this);
                tvIcon.setText("📍");
                tvIcon.setTextSize(20f);
                tvIcon.setPadding(0, 0, dp(12), 0);
                row.addView(tvIcon);

                // Textes
                android.widget.LinearLayout col = new android.widget.LinearLayout(NavigationActivity.this);
                col.setOrientation(android.widget.LinearLayout.VERTICAL);
                android.widget.LinearLayout.LayoutParams colLp =
                        new android.widget.LinearLayout.LayoutParams(0, -2, 1f);
                col.setLayoutParams(colLp);

                TextView tvNom = new TextView(NavigationActivity.this);
                tvNom.setTag("nom");
                tvNom.setTextColor(0xFFFFFFFF);
                tvNom.setTextSize(14f);
                col.addView(tvNom);

                TextView tvSub = new TextView(NavigationActivity.this);
                tvSub.setTag("sub");
                tvSub.setTextColor(0xFF90A4AE);
                tvSub.setTextSize(12f);
                col.addView(tvSub);
                row.addView(col);

                // Bouton naviguer
                TextView btnNav = new TextView(NavigationActivity.this);
                btnNav.setText("Y aller →");
                btnNav.setTextColor(0xFF1A1A2E);
                btnNav.setBackgroundResource(R.drawable.bg_button_cyan);
                btnNav.setTextSize(12f);
                btnNav.setPadding(dp(12), dp(8), dp(12), dp(8));
                btnNav.setTag("btn");
                row.addView(btnNav);

                return new RecyclerView.ViewHolder(row) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
                PointInteret poi = filteredPoi.get(pos);
                android.widget.LinearLayout row = (android.widget.LinearLayout) h.itemView;

                android.widget.LinearLayout col = (android.widget.LinearLayout) row.getChildAt(1);
                TextView tvNom = (TextView) col.getChildAt(0);
                TextView tvSub = (TextView) col.getChildAt(1);
                TextView btnNav= (TextView) row.getChildAt(2);

                tvNom.setText(poi.getNom());
                tvSub.setText(poi.getBatiment() + " • " + poi.getEtage());
                btnNav.setOnClickListener(v -> navigateTo(poi));

                // Divider
                View divider = new View(NavigationActivity.this);
                divider.setBackgroundColor(0x22FFFFFF);
                divider.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, 1));
            }

            @Override public int getItemCount() { return filteredPoi.size(); }
        });
    }

    // =========================================================
    // LANCER LA NAVIGATION VERS UN POI
    // =========================================================
    private void navigateTo(PointInteret poi) {
        ProfileActivity.addToHistory(this, poi.getNom());

        String blocCode = poi.getBlocId() != null ? poi.getBlocId() : "B3";
        String batiment = poi.getBatiment(); // ex: "Palestine", "Bloc 3", etc.
        String etage = poi.getEtage();
        String etageCode = "RDC";
        if (etage != null && etage.contains("1er")) etageCode = "E1";
        else if (etage != null && etage.contains("2")) etageCode = "E2";

        String num = poi.getNom().replaceAll("[^0-9]", "");
        if (num.isEmpty()) num = poi.getNom().replace(" ", "_");

        String nodeId;

        // DÉTECTION PAR LE NOM DU BÂTIMENT (plus fiable)
        if (batiment != null && batiment.toLowerCase().contains("palestine")) {
            // Bloc Palestine
            if (!num.isEmpty()) {
                nodeId = "BP_" + num;
            } else {
                String nomUpper = poi.getNom().toUpperCase().trim();
                if (nomUpper.contains("AMPHI")) {
                    if (nomUpper.endsWith(" A") || nomUpper.equals("AMPHI A")) nodeId = "BP_AA";
                    else if (nomUpper.endsWith(" B") || nomUpper.equals("AMPHI B")) nodeId = "BP_AB";
                    else if (nomUpper.endsWith(" C") || nomUpper.equals("AMPHI C")) nodeId = "BP_AC";
                    else if (nomUpper.endsWith(" D") || nomUpper.equals("AMPHI D")) nodeId = "BP_AD";
                    else nodeId = "BP_105";
                } else nodeId = "BP_105";
            }
            // Forcer le blocId pour la vue et le graphe
            blocCode = "BPAL";
            Log.d("NAV_ACT", "Palestine détecté → nodeId = " + nodeId);
        } else {
            // Bloc 3 ou autre
            nodeId = blocCode + "_" + etageCode + "_" + num;
            Log.d("NAV_ACT", "Autre bloc → nodeId = " + nodeId);
        }

        targetNodeId = nodeId;
        targetNom = poi.getNom();
        targetBlocId = blocCode;

        // Important : informer la vue du bon bloc
        navView.setBlocId(blocCode);

        // Vérification rapide
        NavigationGraph testGraph = new NavigationGraph();
        boolean exists = testGraph.getNode(nodeId) != null;
        Log.d("NAV_ACT", "Node existe dans graphe ? " + exists);
        Toast.makeText(this, "Navigation vers " + nodeId + (exists ? "" : " (inexistant)"), Toast.LENGTH_SHORT).show();

        showNavigationMode();
        navManager = new NavigationManager(this);
        if (!navManager.isConnectedToFsmWifi()) {
            showOfflineDialog();
        } else {
            startNavigation();
        }
    }

    // =========================================================
    // NAVIGATION
    // =========================================================
    private void startNavigation() {
        if (progressNav != null) progressNav.setVisibility(View.VISIBLE);
        if (tvStatus    != null) tvStatus.setText("Localisation en cours...");

        navManager = new NavigationManager(this);
        navManager.startNavigation(targetNodeId, targetNom, buildCallback());
    }

    private void startOfflineNavigation() {
        if (progressNav != null) progressNav.setVisibility(View.VISIBLE);
        if (tvStatus    != null) tvStatus.setText("Mode hors ligne...");
        navManager = new NavigationManager(this);
        navManager.startOfflineNavigation(targetNodeId, targetNom, buildCallback());
    }

    private NavigationManager.NavigationCallback buildCallback() {
        return new NavigationManager.NavigationCallback() {

            @Override
            public void onPositionUpdated(NavigationNode current,
                                          NavigationGraph.NavPath path) {
                Log.d("CALLBACK", "onPositionUpdated called, path=" + (path != null ? path.nodes.size() : "null"));
                if (progressNav != null) progressNav.setVisibility(View.GONE);
                NavigationNode destNode = navManager.getGraph().getNode(targetNodeId);
                navView.setBlocId(path.nodes.get(0).blocId);
                navView.setNavigationData(navManager.getGraph(), path, current, destNode);
                if (path != null && !path.instructions.isEmpty()) {
                    if (tvInstruction != null) tvInstruction.setText(path.instructions.get(0));
                    if (tvDistance    != null) tvDistance.setText(
                            String.format("%.0f m restants", path.totalDistance));
                    if (cardInstruction != null) cardInstruction.setVisibility(View.VISIBLE);
                }
                if (tvStatus != null) tvStatus.setText("📍 " + current.nom);
            }

            @Override
            public void onArrived(String destination) {
                if (progressNav     != null) progressNav.setVisibility(View.GONE);
                if (tvInstruction   != null) tvInstruction.setText("🎉 Vous êtes arrivé à " + destination + " !");
                if (tvDistance      != null) tvDistance.setText("0 m");
                if (tvStatus        != null) tvStatus.setText("Destination atteinte");
                if (cardInstruction != null) cardInstruction.setVisibility(View.VISIBLE);
                Toast.makeText(getApplicationContext(),
                        "🎉 Vous êtes arrivé à " + destination, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                if (progressNav != null) progressNav.setVisibility(View.GONE);
                if (tvStatus    != null) tvStatus.setText("⚠️ " + message);
            }

            @Override
            public void onNotOnFsmWifi() {
                if (progressNav != null) progressNav.setVisibility(View.GONE);
                showOfflineDialog();
            }

        };

    }

    private void showOfflineDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("⚠️ WiFi FSM non détecté")
                .setMessage("Vous n'êtes pas connecté au WiFi FSM.\n\nVoulez-vous voir le chemin hors ligne ?")
                .setPositiveButton("Voir le chemin", (d, w) -> {
                    showNavigationMode();
                    startOfflineNavigation();
                })
                .setNegativeButton("Annuler", (d, w) -> {
                    if (tvStatus != null)
                        tvStatus.setText("Connectez-vous au WiFi FSM et réessayez");
                    if (cardInstruction != null) {
                        cardInstruction.setVisibility(View.VISIBLE);
                        if (tvInstruction != null)
                            tvInstruction.setText("📶 Connectez-vous au WiFi FSM et réessayez");
                        if (tvDistance != null) tvDistance.setText("");
                    }
                })
                .setCancelable(false)
                .show();
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }
}