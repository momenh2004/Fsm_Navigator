package com.fsm.navigator.navigation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

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
 *   1. Vérifier connexion WiFi FSM
 *   2. Si non connecté → onNotOnFsmWifi()
 *   3. Si connecté → Scan → Kalman → W-kNN → A* → callback
 */
public class NavigationManager {

    private static final String  BASE_URL      = "http://10.0.2.2:8080/api/fingerprints";
    private static final int     SCAN_INTERVAL = 4000;

    // SSIDs reconnus comme étant le WiFi FSM
    private static final String[] FSM_SSIDS = {
            "FSM-WiFi", "FSM_WiFi", "fsm-wifi", "fsm_wifi",
            "Universite-Monastir", "UM-WiFi", "FSM"
    };

    private final Context         context;
    private final NavigationGraph graph;
    private final KalmanFilter    kalman;

    private Handler  handler   = new Handler(Looper.getMainLooper());
    private Runnable scanRunnable;
    private boolean  isRunning = false;

    private String targetNodeId;
    private String targetNom;

    private NavigationNode currentNode = null;
    private List<WeightedKNN.Fingerprint> fingerprints = null;
    private static final int PERMISSION_REQUEST_CODE = 100;

    // ===== CALLBACK =====
    public interface NavigationCallback {
        void onPositionUpdated(NavigationNode current, NavigationGraph.NavPath path);
        void onArrived(String destination);
        void onError(String message);
        void onNotOnFsmWifi();   // ← NOUVEAU : pas connecté au WiFi FSM
    }

    private NavigationCallback callback;

    public NavigationManager(Context context) {
        this.context = context;
        this.graph   = new NavigationGraph();
        this.kalman  = new KalmanFilter();
    }

    // =========================================================
    // DÉMARRER
    // =========================================================
    public void startNavigation(String targetNodeId, String targetNom,
                                NavigationCallback callback) {
        this.targetNodeId = targetNodeId;
        this.targetNom    = targetNom;
        this.callback     = callback;
        this.isRunning    = true;

        // Vérifier WiFi FSM avant tout
        if (!isConnectedToFsmWifi()) {
            handler.post(() -> callback.onNotOnFsmWifi());
            return;
        }

        new Thread(() -> {
            try {
                if (fingerprints == null) fingerprints = fetchFingerprints();
                startScanLoop();
            } catch (Exception e) {
                handler.post(() -> callback.onError("Impossible de charger les données"));
            }
        }).start();
    }

    /**
     * Mode hors ligne : calcule directement le chemin A* sans localisation WiFi.
     * Utilise le nœud d'entrée du bloc comme position de départ.
     */
    public void startOfflineNavigation(String targetNodeId, String targetNom,
                                       NavigationCallback callback) {
        this.targetNodeId = targetNodeId;
        this.targetNom    = targetNom;
        this.callback     = callback;
        this.isRunning    = false; // pas de scan continu

        // Extraire le blocId depuis targetNodeId (ex: "B3_RDC_305" → "B3")
        String blocId = targetNodeId.contains("_")
                ? targetNodeId.split("_")[0] : "B3";

        // Trouver le nœud d'entrée du bloc
        NavigationNode entree = graph.findEntree(blocId);
        if (entree == null) {
            // Prendre le premier nœud disponible du bloc
            entree = graph.findAnyNodeInBloc(blocId);
        }
        if (entree == null) {
            handler.post(() -> callback.onError("Impossible de trouver le point de départ"));
            return;
        }

        final NavigationNode startNode = entree;

        // Calculer A* depuis l'entrée vers la destination
        NavigationGraph.NavPath path = graph.findPath(startNode.id, targetNodeId);

        handler.post(() -> {
            currentNode = startNode;
            if (path == null) {
                callback.onError("Chemin introuvable");
                return;
            }
            callback.onPositionUpdated(startNode, path);
        });
        android.util.Log.d("NAV", "start=" + startNode.id);
        android.util.Log.d("NAV", "target=" + targetNodeId);
        android.util.Log.d("NAV", "target exists=" + (graph.getNode(targetNodeId) != null));
        android.util.Log.d("NAV", "path=" + (path == null ? "NULL" : path.nodes.size() + " nodes"));
        for (NavigationNode n : path.nodes) {
            android.util.Log.d("NAV", "node=" + n.id + " x=" + n.x + " y=" + n.y);
        }
    }

    // =========================================================
    // ARRÊTER
    // =========================================================
    public void stopNavigation() {
        isRunning = false;
        if (scanRunnable != null) handler.removeCallbacks(scanRunnable);
    }

    // =========================================================
    // VÉRIFICATION WIFI FSM
    // =========================================================
    public boolean isConnectedToFsmWifi() {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wm == null || !wm.isWifiEnabled()) return false;

            WifiInfo info = wm.getConnectionInfo();
            if (info == null) return false;

            String ssid = info.getSSID();
            if (ssid == null) return false;

            ssid = ssid.replace("\"", "").trim();

            for (String fsmSsid : FSM_SSIDS) {
                if (ssid.equalsIgnoreCase(fsmSsid)) return true;
            }
            return false;

        } catch (SecurityException e) {
            // Permission non accordée → considérer comme non connecté
            return false;
        }
    }

    // =========================================================
    // BOUCLE DE SCAN
    // =========================================================
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
                Map<String, Integer> rawScan = scanWifi();
                if (rawScan.isEmpty()) {
                    handler.post(() -> callback.onError("Signal WiFi introuvable"));
                    return;
                }

                Map<String, Double> filtered = kalman.filter(rawScan);
                if (KalmanFilter.isSignalWeak(filtered)) {
                    handler.post(() -> callback.onError("Signal faible — position approximative"));
                    return;
                }

                if (fingerprints == null || fingerprints.isEmpty()) return;

                WeightedKNN.LocationResult loc = WeightedKNN.locate(filtered, fingerprints);
                if (loc == null) return;

                NavigationNode detected = graph.findBySalleNom(loc.salleNom);
                if (detected == null)
                    detected = graph.findNearest(loc.x, loc.y, loc.blocId, 0);
                if (detected == null) return;

                final NavigationNode finalDetected = detected;
                NavigationGraph.NavPath path = graph.findPath(detected.id, targetNodeId);

                handler.post(() -> {
                    currentNode = finalDetected;
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

    // =========================================================
    // SCAN WIFI
    // =========================================================
    private Map<String, Integer> scanWifi() {
        Map<String, Integer> result = new HashMap<>();
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wm == null || !wm.isWifiEnabled()) return result;
            wm.startScan();
            for (ScanResult sr : wm.getScanResults()) {
                result.put(sr.BSSID.toLowerCase(), sr.level);
            }
        } catch (SecurityException e) {
            // Permission non accordée → retourner liste vide
            android.util.Log.w("NavManager", "WiFi scan permission denied: " + e.getMessage());
        }
        return result;
    }

    // =========================================================
    // FETCH FINGERPRINTS
    // =========================================================
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
            JSONObject fp  = array.getJSONObject(i);
            String salleId  = fp.optString("salleId",  String.valueOf(i));
            String salleNom = fp.optString("salleNom", "");
            String blocCode = fp.optString("blocCode",  "B3");
            float  x        = (float) fp.optDouble("x", 0.0);
            float  y        = (float) fp.optDouble("y", 0.0);
            String bssid    = fp.optString("bssid", "");
            double rssi     = fp.optDouble("rssiMoyen", -80.0);

            if (!bssid.isEmpty())
                list.add(new WeightedKNN.Fingerprint(
                        salleId, salleNom, blocCode, x, y, bssid, rssi));
        }
        return list;
    }

    // =========================================================
    // GETTERS
    // =========================================================
    public NavigationGraph getGraph()       { return graph; }
    public NavigationNode  getCurrentNode() { return currentNode; }
}