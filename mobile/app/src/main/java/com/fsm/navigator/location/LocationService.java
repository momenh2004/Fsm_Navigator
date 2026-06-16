package com.fsm.navigator.location;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LocationService.java
 *
 * Gère la localisation de l'utilisateur par WiFi Fingerprinting.
 *
 * Étapes :
 *   1. Scanner le WiFi → obtenir {BSSID, RSSI} en temps réel
 *   2. Récupérer les fingerprints stockés depuis le backend
 *   3. Appliquer l'algorithme k-NN pour trouver la salle la plus proche
 *   4. Retourner la salle détectée
 *
 * Couche MVC : Service (helper du Contrôleur)
 */
public class LocationService {

    private static final int K = 3; // nombre de voisins k-NN

    // ===== MODÈLE LOCAL D'UN FINGERPRINT =====
    public static class FingerprintLocal {
        public String salleId;
        public String salleNom;
        public String blocId;
        public String bssid;
        public double rssiMoyen;

        public FingerprintLocal(String salleId, String salleNom,
                                String blocId, String bssid, double rssiMoyen) {
            this.salleId   = salleId;
            this.salleNom  = salleNom;
            this.blocId    = blocId;
            this.bssid     = bssid;
            this.rssiMoyen = rssiMoyen;
        }
    }

    // ===== RÉSULTAT DE LOCALISATION =====
    public static class LocationResult {
        public String salleId;
        public String salleNom;
        public String blocId;
        public double confidence; // 0.0 → 1.0

        public LocationResult(String salleId, String salleNom,
                              String blocId, double confidence) {
            this.salleId   = salleId;
            this.salleNom  = salleNom;
            this.blocId    = blocId;
            this.confidence = confidence;
        }
    }

    // =========================================================
    // ÉTAPE 1 — SCANNER LE WIFI
    // =========================================================
    /**
     * Scanne les réseaux WiFi visibles et retourne
     * une map { BSSID → RSSI mesuré }.
     *
     * Ne nécessite PAS d'être connecté au WiFi FSM.
     * Nécessite : ACCESS_FINE_LOCATION + WIFI activé.
     */
    public static Map<String, Integer> scanWifi(Context context) {
        Map<String, Integer> result = new HashMap<>();

        WifiManager wifiManager =
                (WifiManager) context.getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);

        if (wifiManager == null || !wifiManager.isWifiEnabled()) {
            return result; // WiFi désactivé
        }

        // Lancer un nouveau scan
        wifiManager.startScan();

        // Récupérer les résultats
        List<ScanResult> scanResults = wifiManager.getScanResults();
        for (ScanResult scan : scanResults) {
            // Normaliser le BSSID en minuscules
            result.put(scan.BSSID.toLowerCase(), scan.level);
        }

        return result;
    }

    // =========================================================
    // ÉTAPE 2 — GROUPER LES FINGERPRINTS PAR SALLE
    // =========================================================
    /**
     * Groupe les fingerprints par salle.
     * Chaque salle peut avoir plusieurs BSSID.
     *
     * Input  : liste plate de fingerprints
     * Output : Map { salleId → liste de fingerprints }
     */
    private static Map<String, List<FingerprintLocal>> groupBySalle(
            List<FingerprintLocal> fingerprints) {

        Map<String, List<FingerprintLocal>> grouped = new HashMap<>();
        for (FingerprintLocal fp : fingerprints) {
            if (!grouped.containsKey(fp.salleId)) {
                grouped.put(fp.salleId, new ArrayList<>());
            }
            grouped.get(fp.salleId).add(fp);
        }
        return grouped;
    }

    // =========================================================
    // ÉTAPE 3 — ALGORITHME K-NN
    // =========================================================
    /**
     * Applique l'algorithme k-NN pour trouver la salle
     * dont le profil WiFi ressemble le plus au scan actuel.
     *
     * @param measuredRssi  Map { BSSID → RSSI mesuré en temps réel }
     * @param fingerprints  Liste des fingerprints stockés en base
     * @return              La salle détectée avec un score de confiance
     */
    public static LocationResult findLocation(
            Map<String, Integer> measuredRssi,
            List<FingerprintLocal> fingerprints) {

        if (measuredRssi.isEmpty() || fingerprints.isEmpty()) {
            return null;
        }

        // Grouper les fingerprints par salle
        Map<String, List<FingerprintLocal>> bySalle = groupBySalle(fingerprints);

        // Calculer la distance euclidienne avec chaque salle
        List<double[]> distances = new ArrayList<>();
        // distances[i] = { distance, salleIndex }

        List<String> salleIds = new ArrayList<>(bySalle.keySet());

        for (int i = 0; i < salleIds.size(); i++) {
            String salleId = salleIds.get(i);
            List<FingerprintLocal> salleFps = bySalle.get(salleId);

            double distance = calculateDistance(measuredRssi, salleFps);
            distances.add(new double[]{distance, i});
        }

        // Trier par distance croissante
        Collections.sort(distances, (a, b) -> Double.compare(a[0], b[0]));

        // Prendre les K plus proches voisins et voter
        Map<String, Integer> votes    = new HashMap<>();
        Map<String, Double>  distSum  = new HashMap<>();

        int kEffectif = Math.min(K, distances.size());
        for (int i = 0; i < kEffectif; i++) {
            String salleId = salleIds.get((int) distances.get(i)[1]);
            votes.put(salleId, votes.getOrDefault(salleId, 0) + 1);
            distSum.put(salleId, distSum.getOrDefault(salleId, 0.0) + distances.get(i)[0]);
        }

        // Trouver la salle avec le plus de votes
        // En cas d'égalité, prendre celle avec la distance totale minimale
        String bestSalleId = null;
        int    maxVotes    = -1;
        double minDist     = Double.MAX_VALUE;

        for (Map.Entry<String, Integer> entry : votes.entrySet()) {
            String sid = entry.getKey();
            int    v   = entry.getValue();
            double d   = distSum.get(sid);

            if (v > maxVotes || (v == maxVotes && d < minDist)) {
                maxVotes    = v;
                minDist     = d;
                bestSalleId = sid;
            }
        }

        if (bestSalleId == null) return null;

        // Récupérer les infos de la salle gagnante
        FingerprintLocal winner = bySalle.get(bestSalleId).get(0);

        // Calculer la confiance (0.0 → 1.0)
        // Plus la distance est petite, plus la confiance est grande
        double bestDist   = distances.get(0)[0];
        double confidence = Math.max(0, 1.0 - (bestDist / 30.0));
        confidence        = Math.min(1.0, confidence);

        return new LocationResult(
                winner.salleId,
                winner.salleNom,
                winner.blocId,
                confidence
        );
    }

    // =========================================================
    // CALCUL DE LA DISTANCE EUCLIDIENNE
    // =========================================================
    /**
     * Calcule la distance euclidienne entre le scan mesuré
     * et les fingerprints stockés d'une salle.
     *
     * Formule : √ Σ (rssi_mesuré - rssi_stocké)²
     *
     * Si un BSSID est dans la base mais pas détecté,
     * on lui attribue une pénalité de -100 dBm.
     */
    private static double calculateDistance(
            Map<String, Integer> measured,
            List<FingerprintLocal> storedFps) {

        double sumSquares = 0.0;
        int    count      = 0;

        for (FingerprintLocal fp : storedFps) {
            String bssid = fp.bssid.toLowerCase();
            double rssiStored = fp.rssiMoyen;

            double rssiMeasured;
            if (measured.containsKey(bssid)) {
                rssiMeasured = measured.get(bssid);
            } else {
                // BSSID non détecté → pénalité
                rssiMeasured = -100.0;
            }

            sumSquares += Math.pow(rssiMeasured - rssiStored, 2);
            count++;
        }

        if (count == 0) return Double.MAX_VALUE;
        return Math.sqrt(sumSquares / count); // distance euclidienne normalisée
    }

    // =========================================================
    // INTERFACE CALLBACK (pour appel asynchrone)
    // =========================================================
    public interface LocationCallback {
        void onLocationFound(LocationResult result);
        void onLocationFailed(String reason);
    }
}