package com.fsm.navigator.location;

import com.fsm.navigator.AppConfig;
import com.fsm.navigator.controller.KalmanFilter;
import com.fsm.navigator.controller.WeightedKNN;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

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
 * LocationManager.java – Orchestrateur de localisation
 *
 * Pipeline :
 *   1. Vérifier connexion WiFi FSM
 *   2. Scan WiFi brut
 *   3. Filtre de Kalman
 *   4. Weighted k-NN
 *   5. Filtre de stabilité
 *   6. Fallback IMU si signal faible
 */
public class LocationManager {

    private static final String BASE_URL = AppConfig.BASE_URL;

    private final Context         context;
    private final KalmanFilter    kalmanFilter;
    private final StabilityFilter stabilityFilter;
    private final ImuHelper       imuHelper;
    private final WifiManager     wifiManager;

    private float  lastX       = 0f;
    private float  lastY       = 0f;
    private String lastSalleId = null;

    private List<WeightedKNN.Fingerprint> cachedFingerprints = null;

    // ===== INTERFACE CALLBACK =====
    public interface LocationCallback {
        void onLocationFound(WeightedKNN.LocationResult result, boolean isWifi);
        void onLocationFailed(String reason);
    }

    // Initialise le gestionnaire de localisation avec capteurs et filtres.
    public LocationManager(Context context) {
        this.context         = context;
        this.kalmanFilter    = new KalmanFilter();
        this.stabilityFilter = new StabilityFilter();
        this.imuHelper       = new ImuHelper(context);
        this.wifiManager     = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
    }

    // Démarre la fusion IMU (capteurs).
    public void start() { imuHelper.start(); }

    // Arrête la fusion IMU.
    public void stop() { imuHelper.stop(); }

    // Lance la localisation WiFi asynchrone (Kalman → k-NN → Stabilité ou fallback IMU).
    public void locate(LocationCallback callback) {
        android.os.Handler mainHandler = new android.os.Handler(
                android.os.Looper.getMainLooper());

        new Thread(() -> {
            Object result;
            try {
                // 1. Vérifier connexion WiFi FSM
                if (!isConnectedToFsmWifi()) {
                    result = "NOT_FSM_WIFI";
                } else {
                    // 2. Scanner le WiFi
                    Map<String, Integer> rawScan = scanWifi();

                    // 3. Filtre de Kalman
                    Map<String, Double> filtered = kalmanFilter.filter(rawScan);

                    // 4. Signal faible → fallback IMU
                    if (KalmanFilter.isSignalWeak(filtered)) {
                        result = "IMU_FALLBACK";
                    } else {
                        // 5. Fingerprints
                        if (cachedFingerprints == null)
                            cachedFingerprints = fetchFingerprints();

                        if (cachedFingerprints.isEmpty()) {
                            result = "NO_FINGERPRINTS";
                        } else {
                            // 6. Weighted k-NN
                            WeightedKNN.LocationResult knnResult =
                                    WeightedKNN.locate(filtered, cachedFingerprints);

                            if (knnResult == null) {
                                result = "KNN_FAILED";
                            } else {
                                // 7. Ratio de confiance relatif (meilleur vs 2e meilleur)
                                double secondConf = Math.max(0, 1.0 - knnResult.secondBestDistance / 30.0);
                                double ratio = secondConf > 0.01
                                        ? knnResult.confidence / secondConf
                                        : 2.0;

                                // 8. Stabilité (scoring cumulatif)
                                String stable = stabilityFilter.update(knnResult.salleId, knnResult.confidence);
                                knnResult.salleId = stable;

                                // 9. Si trop incertain → afficher le bloc au lieu de la salle
                                if (ratio < 1.25) {
                                    knnResult.salleNom = "Zone " + knnResult.blocId;
                                }

                                lastX = knnResult.x;
                                lastY = knnResult.y;
                                lastSalleId = stable;
                                imuHelper.resetPdr();
                                result = knnResult;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                result = "ERROR: " + e.getMessage();
            }

            // Retourner au thread UI
            final Object finalResult = result;
            mainHandler.post(() -> {
                if (finalResult instanceof WeightedKNN.LocationResult) {
                    callback.onLocationFound((WeightedKNN.LocationResult) finalResult, true);

                } else if ("IMU_FALLBACK".equals(finalResult)) {
                    float[] estimated = imuHelper.getEstimatedPosition(lastX, lastY);
                    WeightedKNN.LocationResult imuResult = new WeightedKNN.LocationResult(
                            lastSalleId != null ? lastSalleId : "INCONNU",
                            lastSalleId != null ? "Dernière position connue" : "Position inconnue",
                            "", estimated[0], estimated[1], 0.4, Double.MAX_VALUE
                    );
                    callback.onLocationFound(imuResult, false);

                } else {
                    callback.onLocationFailed(finalResult.toString());
                }
            });

        }).start();
    }

    private boolean isConnectedToFsmWifi() {
        if (wifiManager == null || !wifiManager.isWifiEnabled()) return false;
        android.net.wifi.WifiInfo info = wifiManager.getConnectionInfo();
        if (info == null) return false;
        String ssid = info.getSSID();
        // Android wraps the SSID in quotes on some versions: "\"Wifi-FSM\""
        return AppConfig.FSM_WIFI_SSID.equals(ssid)
            || ("\"" + AppConfig.FSM_WIFI_SSID + "\"").equals(ssid);
    }

    // Scanne les points d'accès WiFi visibles et récupère les RSSI bruts.
    private Map<String, Integer> scanWifi() {
        Map<String, Integer> result = new HashMap<>();
        if (wifiManager == null || !wifiManager.isWifiEnabled()) return result;
        wifiManager.startScan();
        for (ScanResult sr : wifiManager.getScanResults()) {
            result.put(sr.BSSID.toLowerCase(), sr.level);
        }
        return result;
    }

    // Récupère la base de fingerprints depuis le serveur (GET /api/fingerprints).
    private List<WeightedKNN.Fingerprint> fetchFingerprints() throws Exception {
        List<WeightedKNN.Fingerprint> list = new ArrayList<>();

        URL url = new URL(BASE_URL + "/api/fingerprints");
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
            JSONObject fp       = array.getJSONObject(i);
            String salleId  = fp.optString("salleId",  String.valueOf(i));
            String salleNom = fp.optString("salleNom", "");
            String blocCode = fp.optString("blocCode", "");
            float  x        = (float) fp.optDouble("x", 0.0);
            float  y        = (float) fp.optDouble("y", 0.0);
            String bssid    = fp.optString("bssid", "");
            double rssi     = fp.optDouble("rssiMoyen", -80.0);

            if (!bssid.isEmpty() && !salleNom.isEmpty())
                list.add(new WeightedKNN.Fingerprint(
                        salleId, salleNom, blocCode, x, y, bssid, rssi));
        }
        return list;
    }

    // Invalide le cache des fingerprints (force un rechargement au prochain scan).
    public void invalidateCache() { cachedFingerprints = null; }

    // Retourne l'instance IMU (accès pour PDR/pas/azimut).
    public ImuHelper getImuHelper() { return imuHelper; }

    // Retourne le filtre de stabilité (confirmation de salle).
    public StabilityFilter getStabilityFilter() { return stabilityFilter; }
}