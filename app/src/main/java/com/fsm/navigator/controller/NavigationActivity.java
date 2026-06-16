package com.fsm.navigator.controller;

import com.fsm.navigator.R;

import android.Manifest;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fsm.navigator.AppConfig;
import com.fsm.navigator.auth.PmrDialogHelper;
import com.fsm.navigator.auth.TtsManager;
import com.fsm.navigator.model.NavigationGraph;
import com.fsm.navigator.model.NavigationNode;
import com.fsm.navigator.model.PointInteret;
import com.fsm.navigator.navigation.NavigationManager;
import com.fsm.navigator.service.HistoryService;
import com.fsm.navigator.view.NavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * NavigationActivity.java
 *
 * Deux modes :
 *   1. Avec extras (TARGET_NODE_ID) → navigation directe
 *   2. Sans extras → affiche écran de recherche de destination
 */
public class NavigationActivity extends AppCompatActivity {

    private static final String BASE_URL            = AppConfig.BASE_URL;
    private static final int    PERMISSION_REQUEST_NAV = 102;

    private static final java.util.Map<String, String> BLOC_NAMES =
            new java.util.HashMap<String, String>() {{
        put("PCOUR", "Portail principal");     put("COUR",  "Cour centrale");
        put("B1",    "Bloc 1 (Palestine)");    put("B2",    "Bloc 2");
        put("B3",    "Bloc 3 (Informatique)"); put("B4",    "Bloc 4");
        put("BM",    "Bloc Mathématiques");    put("BP1",   "Bât. Physique 1");
        put("BP2",   "Bât. Physique 2");       put("BC1",   "Bât. Chimie 1");
        put("BC2",   "Bât. Chimie 2");         put("BIB",   "Bibliothèque");
        put("ADM",   "Administration");        put("INF",   "Infirmerie");
        put("STH",   "STH");                   put("D1",    "Département 1");
        put("D2",    "Département 2");         put("BC",    "Bât. Central");
        put("A1-6",  "Amphithéâtres 1-6");
    }};

    // ── Mode navigation ──────────────────────────────────────
    private NavigationView    navView;
    private NavigationManager navManager;
    private ImageButton       btnBack, btnStop;
    private TextView          tvDestination, tvInstruction, tvDistance, tvStatus;
    private View              cardInstruction;
    private View              progressNav;

    // ── Mode extérieur ──────────────────────────────────────
    private View                        layoutOutdoor;
    private com.fsm.navigator.view.FsmMapView mapViewOutdoor;
    private TextView                    tvOutdoorDestination;
    private LinearLayout                layoutOutdoorSteps;
    private com.google.android.material.button.MaterialButton btnArrive;
    private View                        btnStopOutdoor;
    private View                        layoutOutdoorContent;
    private android.widget.Button       btnToggleOutdoor;
    private View                        headerOutdoorSheet;

    // ── Mode recherche ───────────────────────────────────────
    private LinearLayout      layoutSearch, layoutNavigation;
    private EditText          etSearchDest;
    private RecyclerView      recyclerDest;
    private TextView          tvSearchEmpty;
    private View              progressSearch;

    private String  targetNodeId;
    private String  targetNom;
    private String  targetBlocId;
    private boolean isOfflineMode = false;

    private List<PointInteret> allPoi      = new ArrayList<>();
    private List<PointInteret> filteredPoi = new ArrayList<>();

    private final Handler   safetyHandler  = new Handler(Looper.getMainLooper());
    private       Runnable  safetyRunnable = null;

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
        if (targetBlocId != null && navView != null) {
            navView.setBlocId(targetBlocId);
        }

        if (targetNodeId == null) {
            showSearchMode();
            loadDestinations();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (navManager    != null) navManager.stopNavigation();
        if (safetyRunnable != null) safetyHandler.removeCallbacks(safetyRunnable);
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

        // Extérieur
        layoutOutdoor        = findViewById(R.id.layoutOutdoor);
        mapViewOutdoor       = findViewById(R.id.mapViewOutdoor);
        tvOutdoorDestination = findViewById(R.id.tvOutdoorDestination);
        layoutOutdoorSteps   = findViewById(R.id.layoutOutdoorSteps);
        btnArrive            = findViewById(R.id.btnArrive);
        btnStopOutdoor       = findViewById(R.id.btnStopOutdoor);
        layoutOutdoorContent = findViewById(R.id.layoutOutdoorContent);
        btnToggleOutdoor     = findViewById(R.id.btnToggleOutdoor);
        headerOutdoorSheet   = findViewById(R.id.headerOutdoorSheet);

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

    private void toggleOutdoorSheet() {
        if (layoutOutdoorContent == null || btnToggleOutdoor == null) return;
        boolean expanded = layoutOutdoorContent.getVisibility() == View.VISIBLE;
        if (expanded) {
            layoutOutdoorContent.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                layoutOutdoorContent.setVisibility(View.GONE);
                layoutOutdoorContent.setAlpha(1f);
            }).start();
            btnToggleOutdoor.setText("▼");
            btnToggleOutdoor.setContentDescription("Développer");
        } else {
            layoutOutdoorContent.setAlpha(0f);
            layoutOutdoorContent.setVisibility(View.VISIBLE);
            layoutOutdoorContent.animate().alpha(1f).setDuration(150).start();
            btnToggleOutdoor.setText("▲");
            btnToggleOutdoor.setContentDescription("Réduire");
        }
    }

    private void setupListeners() {
        setupSheetSwipe(cardInstruction);
        if (btnBack           != null) btnBack.setOnClickListener(v -> finish());
        if (btnStopOutdoor    != null) btnStopOutdoor.setOnClickListener(v -> finish());
        if (btnToggleOutdoor  != null) btnToggleOutdoor.setOnClickListener(v -> toggleOutdoorSheet());
        if (headerOutdoorSheet != null) headerOutdoorSheet.setOnClickListener(v -> toggleOutdoorSheet());
        if (btnArrive         != null) btnArrive.setOnClickListener(v -> startIndoorOfflineNavigation());
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
        boolean blocked = PmrDialogHelper.checkAndShow(this, poi, () -> doNavigateTo(poi));
        if (blocked) return;
        doNavigateTo(poi);
    }

    private void doNavigateTo(PointInteret poi) {
        ProfileActivity.addToHistory(this, poi.getNom());
        HistoryService.logNavigation(this, poi.getId());

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
            blocCode = "BPAL";
            Log.d("NAV_ACT", "Palestine détecté → nodeId = " + nodeId);
        } else if (("COUR".equals(blocCode) || "A1-6".equals(blocCode))
                && poi.getNom().toLowerCase().contains("amphi")) {
            // Amphis 1→6
            nodeId   = "A16_AMPHI_" + num;
            blocCode = "A1-6";
            Log.d("NAV_ACT", "Amphi 1→6 détecté → nodeId = " + nodeId);
        } else if ("BMATH".equals(blocCode)
                || (batiment != null && batiment.toLowerCase().contains("math"))) {
            // Bloc Math
            String n = poi.getNom().toUpperCase().trim();
            if (n.contains("101M"))   nodeId = "BMATH_101M";
            else if (n.contains("102M")) nodeId = "BMATH_102M";
            else if (n.contains("117M")) nodeId = "BMATH_117M";
            else if (n.contains("BUREAU")) nodeId = "BMATH_BUREAU_G1";
            else nodeId = "BMATH_ENTREE";
            blocCode = "BMATH";
            Log.d("NAV_ACT", "Bloc Math détecté → nodeId = " + nodeId);
        } else {
            // Bloc 3 ou autre
            nodeId = blocCode + "_" + etageCode + "_" + num;
            Log.d("NAV_ACT", "Autre bloc → nodeId = " + nodeId);
        }

        targetNodeId = nodeId;
        targetNom = poi.getNom();
        targetBlocId = blocCode;

        navView.setBlocId(blocCode);
        showNavigationMode();

        navManager = new NavigationManager(this);
        Log.d("NAV_ACT", "Node existe dans graphe ? " + (navManager.getGraph().getNode(nodeId) != null)
                + " (" + nodeId + ")");

        if (!navManager.isConnectedToFsmWifi()) {
            showOfflineDialog();
        } else {
            startNavigation();
        }
    }

    // =========================================================
    // NAVIGATION
    // =========================================================
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void startNavigation() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_NAV);
            return;
        }
        if (progressNav != null) progressNav.setVisibility(View.VISIBLE);
        if (tvStatus    != null) tvStatus.setText("Localisation en cours...");

        // Safety net: if the server doesn't respond within 5 s, jump to offline mode.
        // This fires regardless of what the network stack does (timeouts, hangs, etc.).
        if (safetyRunnable != null) safetyHandler.removeCallbacks(safetyRunnable);
        safetyRunnable = () -> {
            if (progressNav != null && progressNav.getVisibility() == View.VISIBLE) {
                if (navManager != null) navManager.stopNavigation();
                showOfflineDialog();
            }
        };
        safetyHandler.postDelayed(safetyRunnable, 5000);

        navManager.startNavigation(targetNodeId, targetNom, buildCallback());
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == PERMISSION_REQUEST_NAV) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                startNavigation();
            } else {
                showOfflineDialog();
            }
        }
    }

    private void startOfflineNavigation() {
        isOfflineMode = true;
        // Si le bloc cible n'est pas l'entrée principale, montrer le trajet extérieur d'abord
        if (targetBlocId != null && !targetBlocId.equals("PCOUR") && !targetBlocId.equals("COUR")) {
            List<String> campusPath = buildCampusPath("PCOUR", targetBlocId);
            if (campusPath != null && campusPath.size() > 1) {
                showOutdoorPhase(campusPath);
                return;
            }
        }
        startIndoorOfflineNavigation();
    }

    private void startIndoorOfflineNavigation() {
        if (layoutOutdoor    != null) layoutOutdoor.setVisibility(View.GONE);
        if (layoutNavigation != null) layoutNavigation.setVisibility(View.VISIBLE);
        if (progressNav      != null) progressNav.setVisibility(View.VISIBLE);
        if (tvStatus         != null) tvStatus.setText("Phase 2 — Navigation intérieure...");
        navManager.startOfflineNavigation(targetNodeId, targetNom, buildCallback());
    }

    private void showOutdoorPhase(List<String> path) {
        String blocName = BLOC_NAMES.getOrDefault(targetBlocId, targetBlocId);

        // Carte avec chemin surligné
        if (mapViewOutdoor != null) mapViewOutdoor.setNavigationPath(path);

        // Texte destination
        if (tvOutdoorDestination != null) tvOutdoorDestination.setText(blocName);

        // Bouton arrivée
        if (btnArrive != null) btnArrive.setText("Je suis arrivé au " + blocName);

        // Générer les étapes textuelles
        buildOutdoorStepViews(path);

        // Afficher le mode extérieur
        if (layoutSearch     != null) layoutSearch.setVisibility(View.GONE);
        if (layoutNavigation != null) layoutNavigation.setVisibility(View.GONE);
        if (layoutOutdoor    != null) layoutOutdoor.setVisibility(View.VISIBLE);
    }

    private void buildOutdoorStepViews(List<String> path) {
        if (layoutOutdoorSteps == null) return;
        layoutOutdoorSteps.removeAllViews();
        android.graphics.Typeface tf = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.plex_sans);

        for (int i = 0; i < path.size() - 1; i++) {
            String from = BLOC_NAMES.getOrDefault(path.get(i),   path.get(i));
            String to   = BLOC_NAMES.getOrDefault(path.get(i+1), path.get(i+1));

            android.widget.TextView tv = new android.widget.TextView(this);
            tv.setText((i + 1) + ".  " + from + "  →  " + to);
            tv.setTextColor(getColor(R.color.text_secondary));
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
            tv.setLineSpacing(0, 1.4f);
            if (tf != null) tv.setTypeface(tf);
            android.widget.LinearLayout.LayoutParams lp =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);
            tv.setLayoutParams(lp);
            layoutOutdoorSteps.addView(tv);
        }
    }

    // BFS sur le graphe campus (même logique que MapActivity)
    private List<String> buildCampusPath(String from, String to) {
        if (from.equals(to)) return java.util.Collections.singletonList(from);
        java.util.Map<String, List<String>> adj = new java.util.HashMap<>();
        adj.put("PCOUR", java.util.Arrays.asList("COUR", "ADM", "B1"));
        adj.put("COUR",  java.util.Arrays.asList("PCOUR", "A1-6", "BIB", "BP1"));
        adj.put("A1-6",  java.util.Arrays.asList("COUR", "B2", "BM", "BC1"));
        adj.put("B2",    java.util.Arrays.asList("A1-6", "BC1", "BM"));
        adj.put("BM",    java.util.Arrays.asList("COUR", "BIB", "A1-6", "BC"));
        adj.put("BIB",   java.util.Arrays.asList("BM", "COUR", "BC"));
        adj.put("BC1",   java.util.Arrays.asList("A1-6", "B2", "BP1", "BC2"));
        adj.put("BP1",   java.util.Arrays.asList("COUR", "BC1", "BP2"));
        adj.put("BC2",   java.util.Arrays.asList("BC1", "BP2", "B3"));
        adj.put("BP2",   java.util.Arrays.asList("BP1", "BC2", "B1", "B4"));
        adj.put("B3",    java.util.Arrays.asList("B2", "BC2", "B4"));
        adj.put("B4",    java.util.Arrays.asList("B3", "BP2", "BC2"));
        adj.put("B1",    java.util.Arrays.asList("PCOUR", "BP2"));
        adj.put("ADM",   java.util.Arrays.asList("PCOUR", "INF"));
        adj.put("INF",   java.util.Arrays.asList("ADM", "STH"));
        adj.put("STH",   java.util.Arrays.asList("INF", "D1", "D2"));
        adj.put("D1",    java.util.Arrays.asList("STH", "D2", "BC"));
        adj.put("D2",    java.util.Arrays.asList("STH", "D1", "BC"));
        adj.put("BC",    java.util.Arrays.asList("D1", "D2", "BIB", "BM"));

        java.util.Map<String, String> parent = new java.util.HashMap<>();
        java.util.Queue<String> queue = new java.util.LinkedList<>();
        queue.add(from);
        parent.put(from, null);
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            if (cur.equals(to)) {
                List<String> path = new java.util.ArrayList<>();
                String node = to;
                while (node != null) { path.add(0, node); node = parent.get(node); }
                return path;
            }
            for (String n : adj.getOrDefault(cur, java.util.Collections.emptyList())) {
                if (!parent.containsKey(n)) { parent.put(n, cur); queue.add(n); }
            }
        }
        return null;
    }

    private NavigationManager.NavigationCallback buildCallback() {
        return new NavigationManager.NavigationCallback() {

            @Override
            public void onPositionUpdated(NavigationNode current,
                                          NavigationGraph.NavPath path) {
                Log.d("CALLBACK", "onPositionUpdated: " + path.nodes.size() + " nœuds");
                try {
                    if (progressNav != null) progressNav.setVisibility(View.GONE);
                    NavigationNode destNode = navManager.getGraph().getNode(targetNodeId);

                    if (navView != null && !path.nodes.isEmpty()) {
                        navView.setBlocId(path.nodes.get(0).blocId);
                        navView.setNavigationData(navManager.getGraph(), path, current, destNode);
                    }

                    if (cardInstruction != null) cardInstruction.setVisibility(View.VISIBLE);
                    if (tvStatus        != null) tvStatus.setText("📍 " + current.nom);

                    if (isOfflineMode) {
                        if (tvInstruction != null) tvInstruction.setText(
                                "Voici l'itinéraire hors ligne vers " + targetNom);
                        if (tvDistance != null) tvDistance.setText(
                                String.format("%.0f m", path.totalDistance));
                    } else if (!path.instructions.isEmpty()) {
                        String instruction = path.instructions.get(0);
                        if (tvInstruction != null) tvInstruction.setText(instruction);
                        if (tvDistance    != null) tvDistance.setText(
                                String.format("%.0f m restants", path.totalDistance));
                        try { TtsManager.speak(instruction); } catch (Exception ignored) {}
                    }
                } catch (Exception e) {
                    Log.e("CALLBACK", "Erreur onPositionUpdated", e);
                    showError("Erreur d'affichage : " + e.getMessage());
                }
            }

            @Override
            public void onArrived(String destination) {
                if (progressNav     != null) progressNav.setVisibility(View.GONE);
                if (cardInstruction != null) cardInstruction.setVisibility(View.VISIBLE);
                if (tvInstruction   != null) tvInstruction.setText("Vous êtes arrivé à " + destination + " !");
                if (tvDistance      != null) tvDistance.setText("0 m");
                if (tvStatus        != null) tvStatus.setText("Destination atteinte");
                try { TtsManager.speak("Vous êtes arrivé à " + destination + " !"); }
                catch (Exception ignored) {}
                Toast.makeText(getApplicationContext(),
                        "Vous êtes arrivé à " + destination, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                Log.e("CALLBACK", "onError: " + message);
                showError(message);
            }

            @Override
            public void onNotOnFsmWifi() {
                if (progressNav != null) progressNav.setVisibility(View.GONE);
                showOfflineDialog();
            }
        };
    }

    private void showError(String message) {
        if (progressNav     != null) progressNav.setVisibility(View.GONE);
        if (cardInstruction != null) cardInstruction.setVisibility(View.VISIBLE);
        if (tvStatus        != null) tvStatus.setText("⚠️ " + message);
        if (tvInstruction   != null) tvInstruction.setText(message);
        if (tvDistance      != null) tvDistance.setText("");
    }

    private void showOfflineDialog() {
        if (isFinishing() || isDestroyed()) return;
        new android.app.AlertDialog.Builder(this)
                .setTitle("WiFi FSM non détecté")
                .setMessage("Vous n'êtes pas connecté au WiFi FSM.\n\nVoulez-vous voir le chemin hors ligne ?")
                .setPositiveButton("Voir le chemin", (d, w) -> {
                    showNavigationMode();
                    startOfflineNavigation();
                })
                .setNegativeButton("Annuler", (d, w) -> {
                    showError("Connectez-vous au WiFi FSM et réessayez");
                })
                .setCancelable(true)
                .show();
    }

    private void setupSheetSwipe(View sheet) {
        if (sheet == null) return;
        final float[] startY = {0};
        android.view.View.OnTouchListener swipe = (v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    startY[0] = event.getRawY();
                    return true;
                case android.view.MotionEvent.ACTION_MOVE:
                    float dy = event.getRawY() - startY[0];
                    sheet.setTranslationY(dy);
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                    float total = event.getRawY() - startY[0];
                    if (Math.abs(total) > 150) {
                        sheet.animate()
                                .translationY(total > 0 ? sheet.getHeight() : -sheet.getHeight())
                                .setDuration(200)
                                .withEndAction(() -> {
                                    sheet.setVisibility(View.GONE);
                                    sheet.setTranslationY(0);
                                })
                                .start();
                    } else {
                        sheet.animate().translationY(0).setDuration(150).start();
                    }
                    return true;
            }
            return false;
        };
        sheet.setOnTouchListener(swipe);
        View handle = sheet.findViewById(R.id.handleDismiss);
        if (handle != null) handle.setOnTouchListener(swipe);
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }
}
