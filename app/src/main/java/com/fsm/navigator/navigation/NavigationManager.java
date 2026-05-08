package com.fsm.navigator.navigation;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import com.fsm.navigator.location.KalmanFilter;
import com.fsm.navigator.location.WeightedKNN;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NavigationManager.java – Navigation en temps réel
 *
 * Pipeline :
 *   1. Scan WiFi toutes les 4 secondes
 *   2. Kalman → W-kNN → position actuelle
 *   3. A* → chemin vers la destination
 *   4. Callback → mise à jour de l'UI
 */
public class NavigationManager {

    private static final String  BASE_URL      = "http://10.0.2.2:8080/api/fingerprints";
    private static final int     SCAN_INTERVAL = 4000; // ms

    private final Context         context;
    private final NavigationGraph graph;
    private final KalmanFilter    kalman;

    private Handler  handler    = new Handler(Looper.getMainLooper());
    private Runnable scanRunnable;
    private boolean  isRunning  = false;

    // Destination choisie
    private String targetNodeId = null;
    private String targetNom    = null;

    // Dernière position connue
    private NavigationNode currentNode = null;
    private String         lastNodeId  = null;

    // Cache fingerprints
    private List<WeightedKNN.Fingerprint> fingerprints = null;

    // ===== CALLBACK =====
    public interface NavigationCallback {
        void onPositionUpdated(NavigationNode current, NavigationGraph.NavPath path);
        void onArrived(String destination);
        void onError(String message);
    }

    private NavigationCallback callback;

    public NavigationManager(Context context) {
        this.context = context;
        this.graph   = new NavigationGraph();
        this.kalman  = new KalmanFilter();
    }

    // ===== DÉMARRER LA NAVIGATION =====
    public void startNavigation(String targetNodeId, String targetNom,
                                NavigationCallback callback) {
        this.targetNodeId = targetNodeId;
        this.targetNom    = targetNom;
        this.callback     = callback;
        this.isRunning    = true;

        // Charger les fingerprints puis démarrer le scan
        new Thread(() -> {
            try {
                if (fingerprints == null) fingerprints = fetchFingerprints();
                startScanLoop();
            } catch (Exception e) {
                handler.post(() -> callback.onError("Impossible de charger les données"));
            }
        }).start();
    }

    // ===== ARRÊTER LA NAVIGATION =====
    public void stopNavigation() {
        isRunning = false;
        if (scanRunnable != null) handler.removeCallbacks(scanRunnable);
    }

    // ===== BOUCLE DE SCAN =====
    private void startScanLoop() {
        scanRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;
                performScanAndNavigate();
                handler.postDelayed(this, SCAN_INTERVAL);
            }
        };
        handler.post(scanRunnable);
    }

    private void performScanAndNavigate() {
        new Thread(() -> {
            try {
                // 1. Scanner WiFi
                Map<String, Integer> rawScan = scanWifi();
                if (rawScan.isEmpty()) {
                    handler.post(() -> callback.onError("Connectez-vous au WiFi FSM"));
                    return;
                }

                // 2. Kalman
                Map<String, Double> filtered = kalman.filter(rawScan);
                if (KalmanFilter.isSignalWeak(filtered)) {
                    handler.post(() -> callback.onError("Signal faible — position approximative"));
                    return;
                }

                // 3. W-kNN
                if (fingerprints == null || fingerprints.isEmpty()) return;
                WeightedKNN.LocationResult loc = WeightedKNN.locate(filtered, fingerprints);
                if (loc == null) return;

                // 4. Trouver le nœud correspondant dans le graphe
                NavigationNode detected = graph.findBySalleNom(loc.salleNom);
                if (detected == null) {
                    // Chercher par coordonnées
                    detected = graph.findNearest(loc.x, loc.y, loc.blocId, 0);
                }
                if (detected == null) return;

                final NavigationNode finalDetected = detected;

                // 5. A* vers la destination
                NavigationGraph.NavPath path = graph.findPath(detected.id, targetNodeId);

                handler.post(() -> {
                    currentNode = finalDetected;

                    // Vérifier si arrivé
                    if (finalDetected.id.equals(targetNodeId)) {
                        callback.onArrived(targetNom);
                        stopNavigation();
                        return;
                    }

                    callback.onPositionUpdated(finalDetected, path);
                });

            } catch (Exception e) {
                handler.post(() -> callback.onError("Erreur localisation"));
            }
        }).start();
    }

    // ===== SCAN WIFI =====
    private Map<String, Integer> scanWifi() {
        Map<String, Integer> result = new HashMap<>();
        WifiManager wm = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wm == null || !wm.isWifiEnabled()) return result;
        wm.startScan();
        for (ScanResult sr : wm.getScanResults()) {
            result.put(sr.BSSID.toLowerCase(), sr.level);
        }
        return result;
    }

    // ===== RÉCUPÉRER FINGERPRINTS =====
    private List<WeightedKNN.Fingerprint> fetchFingerprints() throws Exception {
        List<WeightedKNN.Fingerprint> list = new ArrayList<>();
        URL url = new URL(BASE_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        if (conn.getResponseCode() != 200) return list;

        BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();

        JSONArray array = new JSONArray(sb.toString());
        for (int i = 0; i < array.length(); i++) {
            JSONObject fp    = array.getJSONObject(i);
            JSONObject salle = fp.getJSONObject("salle");
            JSONObject etage = salle.getJSONObject("etage");
            JSONObject bloc  = etage.getJSONObject("bloc");

            float x = (float) salle.optDouble("x", 0.0);
            float y = (float) salle.optDouble("y", 0.0);

            list.add(new WeightedKNN.Fingerprint(
                    String.valueOf(salle.getLong("id")),
                    salle.getString("nom"),
                    bloc.getString("code"),
                    x, y,
                    fp.getString("bssid"),
                    fp.getDouble("rssiMoyen")
            ));
        }
        return list;
    }

    // ===== GETTERS =====
    public NavigationGraph getGraph()       { return graph; }
    public NavigationNode  getCurrentNode() { return currentNode; }
}