package com.fsm.navigator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import com.fsm.navigator.map.FsmMapView;

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
            if (selectedBloc != null) {
                Intent intent = new Intent(this, NavigationActivity.class);
                intent.putExtra("TARGET_NODE_ID",  selectedBloc.id + "_RDC_ENTREE");
                intent.putExtra("TARGET_NOM",      selectedBloc.nom.replace("\n", " "));
                intent.putExtra("TARGET_BLOC_ID",  selectedBloc.id);
                startActivity(intent);
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
                    fsmMapView.setCurrentLocation(result.blocId);

                    int pct  = (int)(result.confidence * 100);
                    String mode = isWifi ? "WiFi" : "IMU";
                    if (tvBlocNom  != null) tvBlocNom.setText(result.salleNom);
                    if (tvBlocDesc != null) tvBlocDesc.setText(mode + " • Confiance : " + pct + "%");
                    if (cardBlocInfo != null) cardBlocInfo.setVisibility(View.VISIBLE);

                    String emoji = isWifi ? "📍" : "🧭";
                    Toast.makeText(MapActivity.this,
                            emoji + " " + result.salleNom + " (" + pct + "%)",
                            Toast.LENGTH_LONG).show();
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