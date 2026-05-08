package com.fsm.navigator.location;

import java.util.HashMap;
import java.util.Map;

/**
 * KalmanFilter.java – Filtre de Kalman pour le lissage RSSI
 *
 * Le filtre de Kalman est un estimateur optimal qui :
 *   1. PRÉDIT la prochaine valeur à partir de l'état actuel
 *   2. CORRIGE la prédiction avec la mesure réelle
 *
 * Paramètres :
 *   Q = bruit du processus (variation naturelle du RSSI)
 *   R = bruit de mesure    (imprécision du capteur WiFi)
 */
public class KalmanFilter {

    private static final double Q = 0.008; // bruit processus
    private static final double R = 0.1;   // bruit mesure

    private static class KalmanState {
        double estimate;
        double errorCov;
        KalmanState(double init) { this.estimate = init; this.errorCov = 1.0; }
    }

    private final Map<String, KalmanState> states = new HashMap<>();

    /**
     * Filtre un scan WiFi brut via Kalman.
     * @param rawScan Map { bssid → rssi mesuré }
     * @return        Map { bssid → rssi filtré }
     */
    public Map<String, Double> filter(Map<String, Integer> rawScan) {
        Map<String, Double> filtered = new HashMap<>();

        for (Map.Entry<String, Integer> entry : rawScan.entrySet()) {
            String bssid    = entry.getKey().toLowerCase();
            double measured = entry.getValue();

            if (!states.containsKey(bssid)) {
                states.put(bssid, new KalmanState(measured));
                filtered.put(bssid, measured);
                continue;
            }

            KalmanState s = states.get(bssid);

            // Prédiction
            double predEst = s.estimate;
            double predCov = s.errorCov + Q;

            // Gain de Kalman
            double gain = predCov / (predCov + R);

            // Correction
            s.estimate = predEst + gain * (measured - predEst);
            s.errorCov = (1 - gain) * predCov;

            filtered.put(bssid, s.estimate);
        }
        return filtered;
    }

    public static boolean isSignalWeak(Map<String, Double> scan) {
        if (scan == null || scan.isEmpty()) return true;
        return scan.values().stream().mapToDouble(d -> d).max().orElse(-100) < -85.0;
    }

    public void reset() { states.clear(); }
}