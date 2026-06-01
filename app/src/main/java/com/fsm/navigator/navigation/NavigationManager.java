package com.fsm.navigator.navigation;

import com.fsm.navigator.AppConfig;
import com.fsm.navigator.model.NavigationGraph;
import com.fsm.navigator.model.NavigationNode;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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

    private static final String  BASE_URL      = AppConfig.BASE_URL + "/api/fingerprints";
    private static final int     SCAN_INTERVAL = 4000;


    private final Context         context;
    private       NavigationGraph graph;
    private final KalmanFilter    kalman;
    private final WifiManager     wifiManager;

    private Handler  handler   = new Handler(Looper.getMainLooper());
    private Runnable scanRunnable;
    private boolean  isRunning = false;

    private String targetNodeId;
    private String targetNom;

    private NavigationNode currentNode = null;
    private List<WeightedKNN.Fingerprint> fingerprints = null;

    // ===== CALLBACK =====
    public interface NavigationCallback {
        void onPositionUpdated(NavigationNode current, NavigationGraph.NavPath path);
        void onArrived(String destination);
        void onError(String message);
        void onNotOnFsmWifi();   // ← NOUVEAU : pas connecté au WiFi FSM
    }

    private NavigationCallback callback;

    // Initialise le gestionnaire de navigation (graphe cache + rafraîchissement réseau).
    public NavigationManager(Context context) {
        this.context     = context;
        this.kalman      = new KalmanFilter();
        this.wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        NavigationGraphLoader loader = new NavigationGraphLoader();

        // Chargement synchrone depuis le cache (instantané si disponible)
        this.graph = loader.loadFromCache(context);
        Log.d("NAV_MANAGER", "Cache : " + this.graph.getAllNodes().size() + " noeuds");

        // Rafraîchissement en background depuis le réseau
        new Thread(() -> {
            try {
                NavigationGraph loaded = loader.loadFromNetwork(context);
                this.graph = loaded;
                Log.d("NAV_MANAGER", "Graphe mis à jour depuis réseau : " + loaded.getAllNodes().size() + " noeuds");
            } catch (Exception e) {
                Log.w("NAV_MANAGER", "Réseau KO, graphe cache conservé");
            }
        }).start();
    }

    // Lance la navigation en ligne (localisation WiFi → A* → suivi temps réel).
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
                if (fingerprints.isEmpty()) {
                    // Server reachable but no fingerprint data → offer offline mode
                    handler.post(() -> callback.onNotOnFsmWifi());
                    return;
                }
                startScanLoop();
            } catch (Exception e) {
                // Server unreachable (timeout, refused, wrong network) → offer offline mode
                handler.post(() -> callback.onNotOnFsmWifi());
            }
        }).start();
    }

    // Lance la navigation hors ligne (A* direct depuis entrée du bloc, sans WiFi).
    public void startOfflineNavigation(String targetNodeId, String targetNom,
                                       NavigationCallback callback) {
        this.targetNodeId = targetNodeId;
        this.targetNom    = targetNom;
        this.callback     = callback;
        this.isRunning    = false;

        if (graph == null || graph.getAllNodes().isEmpty()) {
            graph = NavigationGraph.buildFallback();
        }
        computeOfflinePath(targetNodeId, targetNom, callback);
    }

    private void computeOfflinePath(String targetNodeId, String targetNom,
                                    NavigationCallback callback) {
        Log.d("OFFLINE", "computeOfflinePath, targetNodeId=" + targetNodeId);

        NavigationNode targetNode = graph.getNode(targetNodeId);
        Log.d("OFFLINE", "targetNode = " + (targetNode != null ? targetNode.nom : "null"));
        if (targetNode == null) {
            callback.onError("Destination inconnue : " + targetNodeId);
            return;
        }

        String blocId = targetNode.blocId;
        if (blocId == null || blocId.isEmpty()) {
            callback.onError("Bloc introuvable pour : " + targetNodeId);
            return;
        }
        NavigationNode entree = graph.findEntree(blocId);
        if (entree == null) entree = graph.findAnyNodeInBloc(blocId);
        if (entree == null) {
            callback.onError("Point de départ introuvable pour " + blocId);
            return;
        }

        final NavigationNode startNode = entree;
        Log.d("OFFLINE", "startNode=" + startNode.id + " → target=" + targetNode.id);

        NavigationGraph.NavPath path = graph.findPath(startNode.id, targetNodeId);
        Log.d("OFFLINE", "path = " + (path != null ? path.nodes.size() + " nœuds" : "null"));

        currentNode = startNode;
        if (path == null) {
            callback.onError("Chemin introuvable");
            return;
        }
        callback.onPositionUpdated(startNode, path);
    }

    // Arrête la boucle de scan et la navigation en cours.
    public void stopNavigation() {
        isRunning = false;
        if (scanRunnable != null) handler.removeCallbacks(scanRunnable);
    }

    // Vérifie si l'appareil est connecté au WiFi FSM.
    public boolean isConnectedToFsmWifi() {
        if (wifiManager == null || !wifiManager.isWifiEnabled()) return false;
        android.net.wifi.WifiInfo info = wifiManager.getConnectionInfo();
        if (info == null) return false;
        String ssid = info.getSSID();
        // Android wraps the SSID in quotes on some versions: "\"Wifi-FSM\""
        return AppConfig.FSM_WIFI_SSID.equals(ssid)
            || ("\"" + AppConfig.FSM_WIFI_SSID + "\"").equals(ssid);
    }

    // Démarre la boucle de scan WiFi périodique (tous les 4 secondes).
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

                if (fingerprints == null || fingerprints.isEmpty()) {
                    handler.post(() -> callback.onError("Données de localisation indisponibles"));
                    return;
                }

                WeightedKNN.LocationResult loc = WeightedKNN.locate(filtered, fingerprints);
                if (loc == null) {
                    handler.post(() -> callback.onError("Signal insuffisant, réessai..."));
                    return;
                }

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

    // Scanne le WiFi et retourne les RSSI détectés (gère les erreurs de permission).
    private Map<String, Integer> scanWifi() {
        Map<String, Integer> result = new HashMap<>();
        if (wifiManager == null || !wifiManager.isWifiEnabled()) return result;
        try {
            // On Android 9+, startScan() is throttled — use system scan results directly
            for (ScanResult sr : wifiManager.getScanResults()) {
                result.put(sr.BSSID.toLowerCase(), sr.level);
            }
        } catch (SecurityException e) {
            // Location permission not granted → stop online mode and offer offline
            Log.w("NavManager", "Location permission denied, switching to offline");
            isRunning = false;
            handler.post(() -> { if (callback != null) callback.onNotOnFsmWifi(); });
        }
        return result;
    }

    // Récupère la base de fingerprints depuis le serveur (GET /api/fingerprints).
    private List<WeightedKNN.Fingerprint> fetchFingerprints() throws Exception {
        List<WeightedKNN.Fingerprint> list = new ArrayList<>();
        URL url = new URL(BASE_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
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

    // Retourne le graphe de navigation (nœuds et arêtes).
    public NavigationGraph getGraph() { return graph; }

    // Retourne le nœud actuel détecté par localisation.
    public NavigationNode getCurrentNode() { return currentNode; }
}
