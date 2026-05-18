package com.fsm.navigator.controller;

import com.fsm.navigator.R;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fsm.navigator.location.LocationManager;
import com.fsm.navigator.location.WeightedKNN;
import com.fsm.navigator.view.FsmMapView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * MapActivity.java – Carte + Localisation + Navigation Drawer
 */
public class MapActivity extends BaseDrawerActivity {

    private static final int PERMISSION_CODE = 100;

    private FsmMapView  fsmMapView;
    private CardView    cardBlocInfo;
    private TextView    tvBlocNom, tvBlocDesc;
    private LinearLayout btnVoirPlan, btnNavigerBloc, btnLocate;
    private ImageButton btnHamburger, btnResetZoom;
    private ImageButton btnZoomIn, btnZoomOut;
    private ProgressBar progressLocate;

    private LocationManager   locationManager;
    private FsmMapView.Bloc   selectedBloc = null;

    private String  currentBlocId       = null;
    private String  outdoorTargetBlocId = null;
    private String  outdoorTargetNodeId = null;
    private String  outdoorTargetNom    = null;
    private boolean isMonitoring        = false;

    private final Handler  monitorHandler  = new Handler();
    private final Runnable monitorRunnable = this::pollLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        locationManager = new LocationManager(this);
        locationManager.start();

        initViews();
        setupDrawer();
        setupHamburger(R.id.btnHamburger);
        setupMap();
        setupListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationManager.stop();
        stopOutdoorMonitoring();
    }

    // =========================================================
    private void initViews() {
        fsmMapView    = findViewById(R.id.mapView);
        cardBlocInfo  = findViewById(R.id.cardBlocInfo);
        tvBlocNom     = findViewById(R.id.tvBlocNom);
        tvBlocDesc    = findViewById(R.id.tvBlocDesc);
        btnVoirPlan   = findViewById(R.id.btnVoirPlan);
        btnNavigerBloc= findViewById(R.id.btnNavigerBloc);
        btnLocate     = findViewById(R.id.btnLocate);
        btnHamburger  = findViewById(R.id.btnHamburger);
        btnResetZoom  = findViewById(R.id.btnResetZoom);
        btnZoomIn     = findViewById(R.id.btnZoomIn);
        btnZoomOut    = findViewById(R.id.btnZoomOut);
    }

    // =========================================================
    private void setupMap() {
        if (fsmMapView == null) return;
        fsmMapView.setOnBlocClickListener(bloc -> {
            selectedBloc = bloc;
            tvBlocNom.setText(bloc.nom.replace("\n", " "));
            tvBlocDesc.setText(getBlocDetails(bloc.id));
            cardBlocInfo.setVisibility(View.VISIBLE);
            if (btnVoirPlan != null)
                btnVoirPlan.setVisibility(hasPlan(bloc.id) ? View.VISIBLE : View.GONE);
        });
    }

    private boolean hasPlan(String blocId) {
        switch (blocId) {
            case "COUR":
            case "PCOUR":
            case "ADM":
            case "STH":
            case "D1":
            case "D2":
                return false;
            default:
                return true;
        }
    }

    // =========================================================
    private void setupListeners() {
        // Zoom
        if (btnZoomIn    != null) btnZoomIn.setOnClickListener(v    -> fsmMapView.zoomIn());
        if (btnZoomOut   != null) btnZoomOut.setOnClickListener(v   -> fsmMapView.zoomOut());
        if (btnResetZoom != null) btnResetZoom.setOnClickListener(v -> fsmMapView.resetZoom());

        // Voir plan interne du bloc
        if (btnVoirPlan != null) btnVoirPlan.setOnClickListener(v -> {
            if (selectedBloc != null) {
                Intent intent = new Intent(this, BlockDetailActivity.class);
                intent.putExtra("BLOC_ID",  selectedBloc.id);
                intent.putExtra("BLOC_NOM", selectedBloc.nom.replace("\n", " "));
                startActivity(intent);
            }
        });

        // Naviguer vers le bloc
        if (btnNavigerBloc != null) btnNavigerBloc.setOnClickListener(v -> {
            if (selectedBloc == null) return;
            if (currentBlocId != null && currentBlocId.equals(selectedBloc.id)) {
                // Already at destination → indoor navigation immediately
                launchIndoorNav(selectedBloc.id + "_RDC_ENTREE",
                                selectedBloc.nom.replace("\n", " "),
                                selectedBloc.id);
            } else {
                startOutdoorNavigation(selectedBloc);
            }
        });

        // Localisation au clic sur btnLocate
        if (btnLocate != null) btnLocate.setOnClickListener(v -> {
            if (checkPermissions()) startLocalization();
            else requestPermissions();
        });

        // Localisation au clic sur la carte
        if (fsmMapView != null) {
            fsmMapView.setOnClickListener(v -> {
                if (checkPermissions()) startLocalization();
                else requestPermissions();
            });
        }
    }

    // =========================================================
    // LOCALISATION
    // =========================================================
    private void startLocalization() {
        if (cardBlocInfo != null) cardBlocInfo.setVisibility(View.VISIBLE);
        if (tvBlocDesc   != null) tvBlocDesc.setText("Localisation en cours...");

        locationManager.locate(new LocationManager.LocationCallback() {

            @Override
            public void onLocationFound(WeightedKNN.LocationResult result, boolean isWifi) {
                runOnUiThread(() -> {
                    boolean firstFix = (currentBlocId == null);
                    currentBlocId = result.blocId;
                    fsmMapView.setCurrentLocation(result.blocId);

                    int pct  = (int)(result.confidence * 100);
                    String mode = isWifi ? "WiFi" : "IMU";
                    if (tvBlocNom  != null) tvBlocNom.setText(result.salleNom);
                    if (tvBlocDesc != null) tvBlocDesc.setText(mode + " • Confiance : " + pct + "%");
                    if (cardBlocInfo != null) cardBlocInfo.setVisibility(View.VISIBLE);

                    Toast.makeText(getApplicationContext(),
                            result.salleNom + " (" + pct + "%)",
                            Toast.LENGTH_LONG).show();

                    // If localization was triggered because we had a pending target
                    if (firstFix && outdoorTargetBlocId != null && !isMonitoring) {
                        List<String> path = buildOutdoorPath(currentBlocId, outdoorTargetBlocId);
                        if (path == null || path.size() <= 1) {
                            onArrivedAtTarget();
                        } else {
                            fsmMapView.setNavigationPath(path);
                            startOutdoorMonitoring();
                        }
                    }
                });
            }

            @Override
            public void onLocationFailed(String reason) {
                runOnUiThread(() -> {
                    // Cacher la card bloc
                    if (cardBlocInfo != null) cardBlocInfo.setVisibility(View.GONE);

                    switch (reason) {
                        case "NOT_FSM_WIFI":
                            // Afficher AlertDialog comme dans NavigationActivity
                            new android.app.AlertDialog.Builder(MapActivity.this)
                                    .setTitle("⚠️ WiFi FSM non détecté")
                                    .setMessage("Vous n'êtes pas connecté au WiFi FSM.\n\n" +
                                            "Connectez-vous au réseau WiFi de la faculté " +
                                            "pour utiliser la localisation en temps réel.")
                                    .setPositiveButton("OK", null)
                                    .show();
                            break;

                        case "NO_FINGERPRINTS":
                            Toast.makeText(getApplicationContext(),
                                    "Données de localisation indisponibles",
                                    Toast.LENGTH_SHORT).show();
                            break;

                        case "KNN_FAILED":
                            Toast.makeText(getApplicationContext(),
                                    "Localisation impossible, réessayez",
                                    Toast.LENGTH_SHORT).show();
                            break;

                        default:
                            Toast.makeText(getApplicationContext(),
                                    "Erreur de localisation",
                                    Toast.LENGTH_SHORT).show();
                            break;
                    }
                });
            }
        });
    }

    // =========================================================
    // PERMISSIONS
    // =========================================================
    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == PERMISSION_CODE && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            startLocalization();
        } else {
            Toast.makeText(this, "Permission localisation requise", Toast.LENGTH_LONG).show();
        }
    }

    // =========================================================
    // OUTDOOR NAVIGATION
    // =========================================================
    private void startOutdoorNavigation(FsmMapView.Bloc target) {
        outdoorTargetBlocId = target.id;
        outdoorTargetNodeId = target.id + "_RDC_ENTREE";
        outdoorTargetNom    = target.nom.replace("\n", " ");

        if (cardBlocInfo != null) cardBlocInfo.setVisibility(View.GONE);

        if (currentBlocId == null) {
            // Localize first; onLocationFound will start the path once we have a fix
            Toast.makeText(getApplicationContext(), "Localisation en cours...", Toast.LENGTH_SHORT).show();
            startLocalization();
            return;
        }

        List<String> path = buildOutdoorPath(currentBlocId, outdoorTargetBlocId);
        if (path == null || path.size() <= 1) {
            onArrivedAtTarget();
        } else {
            fsmMapView.setNavigationPath(path);
            startOutdoorMonitoring();
            Toast.makeText(getApplicationContext(), "Navigation vers " + outdoorTargetNom, Toast.LENGTH_SHORT).show();
        }
    }

    private void startOutdoorMonitoring() {
        if (isMonitoring) return;
        isMonitoring = true;
        monitorHandler.postDelayed(monitorRunnable, 5000);
    }

    private void stopOutdoorMonitoring() {
        isMonitoring = false;
        monitorHandler.removeCallbacks(monitorRunnable);
        if (fsmMapView != null) fsmMapView.clearNavigationPath();
        outdoorTargetBlocId = null;
        outdoorTargetNodeId = null;
        outdoorTargetNom    = null;
    }

    private void pollLocation() {
        if (!isMonitoring) return;
        if (!checkPermissions()) {
            monitorHandler.postDelayed(monitorRunnable, 5000);
            return;
        }
        locationManager.locate(new LocationManager.LocationCallback() {
            @Override
            public void onLocationFound(WeightedKNN.LocationResult r, boolean isWifi) {
                runOnUiThread(() -> {
                    currentBlocId = r.blocId;
                    fsmMapView.setCurrentLocation(r.blocId);
                    if (outdoorTargetBlocId != null && outdoorTargetBlocId.equals(r.blocId)) {
                        onArrivedAtTarget();
                    } else if (isMonitoring) {
                        monitorHandler.postDelayed(monitorRunnable, 5000);
                    }
                });
            }
            @Override
            public void onLocationFailed(String reason) {
                if (isMonitoring) monitorHandler.postDelayed(monitorRunnable, 5000);
            }
        });
    }

    private void onArrivedAtTarget() {
        String targetBlocId = outdoorTargetBlocId;
        String targetNodeId = outdoorTargetNodeId;
        String targetNom    = outdoorTargetNom;
        stopOutdoorMonitoring();

        if (hasPlan(targetBlocId)) {
            launchIndoorNav(targetNodeId, targetNom, targetBlocId);
        } else {
            Toast.makeText(getApplicationContext(), "Vous etes arrive a " + targetNom, Toast.LENGTH_LONG).show();
        }
    }

    private void launchIndoorNav(String nodeId, String nom, String blocId) {
        Intent intent = new Intent(this, NavigationActivity.class);
        intent.putExtra("TARGET_NODE_ID",  nodeId);
        intent.putExtra("TARGET_NOM",      nom);
        intent.putExtra("TARGET_BLOC_ID",  blocId);
        startActivity(intent);
    }

    private List<String> buildOutdoorPath(String from, String to) {
        if (from.equals(to)) return Collections.singletonList(from);

        Map<String, List<String>> adj = new HashMap<>();
        adj.put("PCOUR", Arrays.asList("COUR", "ADM", "B1"));
        adj.put("COUR",  Arrays.asList("PCOUR", "A1-6", "BIB", "BP1"));
        adj.put("A1-6",  Arrays.asList("COUR", "B2", "BM", "BC1"));
        adj.put("B2",    Arrays.asList("A1-6", "BC1", "BM"));
        adj.put("BM",    Arrays.asList("COUR", "BIB", "A1-6", "BC"));
        adj.put("BIB",   Arrays.asList("BM", "COUR", "BC"));
        adj.put("BC1",   Arrays.asList("A1-6", "B2", "BP1", "BC2"));
        adj.put("BP1",   Arrays.asList("COUR", "BC1", "BP2"));
        adj.put("BC2",   Arrays.asList("BC1", "BP2", "B3"));
        adj.put("BP2",   Arrays.asList("BP1", "BC2", "B1", "B4"));
        adj.put("B3",    Arrays.asList("B2", "BC2", "B4"));
        adj.put("B4",    Arrays.asList("B3", "BP2", "BC2"));
        adj.put("B1",    Arrays.asList("PCOUR", "BP2"));
        adj.put("ADM",   Arrays.asList("PCOUR", "INF"));
        adj.put("INF",   Arrays.asList("ADM", "STH"));
        adj.put("STH",   Arrays.asList("INF", "D1", "D2"));
        adj.put("D1",    Arrays.asList("STH", "D2", "BC"));
        adj.put("D2",    Arrays.asList("STH", "D1", "BC"));
        adj.put("BC",    Arrays.asList("D1", "D2", "BIB", "BM"));

        Map<String, String> parent = new HashMap<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(from);
        parent.put(from, null);

        while (!queue.isEmpty()) {
            String cur = queue.poll();
            if (cur.equals(to)) {
                List<String> path = new ArrayList<>();
                String node = to;
                while (node != null) { path.add(0, node); node = parent.get(node); }
                return path;
            }
            for (String n : adj.getOrDefault(cur, Collections.emptyList())) {
                if (!parent.containsKey(n)) { parent.put(n, cur); queue.add(n); }
            }
        }
        return null;
    }

    // =========================================================
    private String getBlocDetails(String blocId) {
        switch (blocId) {
            case "B1":    return "Amphi A→D • Salles 101→118";
            case "B2":    return "Salles 201→220 • Bureaux profs";
            case "B3":    return "Salles informatiques 301→316";
            case "B4":    return "Salles 401→409";
            case "BM":    return "Département Mathématiques";
            case "BP1":   return "Département Physique 1";
            case "BC1":   return "Département Chimie 1";
            case "BIB":   return "Bibliothèque Centrale";
            case "AMPHIS":return "Amphithéâtres 1→6";
            case "HORS":  return "D1, D2, Thèses, Infirmerie...";
            default:      return "Appuyez pour voir le plan";
        }
    }
}
