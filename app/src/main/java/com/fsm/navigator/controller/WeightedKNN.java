package com.fsm.navigator.controller;

import java.util.*;
import java.util.stream.Collectors;

/**
 * WeightedKNN.java – Algorithme k-NN Pondéré pour localisation indoor
 *
 * Différence avec k-NN classique :
 *   - k-NN classique  : vote majoritaire entre les k voisins (poids égaux)
 *   - Weighted k-NN   : chaque voisin a un poids = 1/distance²
 *     → les salles plus proches en RSSI influencent plus le résultat
 *
 * Retourne :
 *   - La salle la plus probable
 *   - La position interpolée (x, y) en mètres
 *   - Un score de confiance (0.0 → 1.0)
 */
public class WeightedKNN {

    private static final int    K                   = 3;      // nombre de voisins
    private static final double PENALTY_RSSI        = -100.0; // pénalité si BSSID absent
    private static final double WEAK_FP_THRESHOLD   = -87.0;  // rssiMoyen trop faible
    private static final double WEAK_FP_PENALTY     = 13.0;   // pénalité additive (distance)
    private static final double STRONG_SIGNAL_FLOOR = -83.0;  // max scan pour être "en zone couverte"

    // ===== MODÈLES =====

    public static class Fingerprint {
        public String salleId;
        public String salleNom;
        public String blocId;
        public float  x, y;       // coordonnées réelles (mètres)
        public String bssid;
        public double rssiMoyen;

        public Fingerprint(String salleId, String salleNom, String blocId,
                           float x, float y, String bssid, double rssiMoyen) {
            this.salleId   = salleId;
            this.salleNom  = salleNom;
            this.blocId    = blocId;
            this.x         = x;
            this.y         = y;
            this.bssid     = bssid.toLowerCase();
            this.rssiMoyen = rssiMoyen;
        }
    }

    public static class LocationResult {
        public String  salleId;
        public String  salleNom;
        public String  blocId;
        public float   x, y;               // position interpolée
        public double  confidence;         // 0.0 → 1.0
        public double  secondBestDistance; // distance du 2e candidat (pour ratio)

        public LocationResult(String salleId, String salleNom, String blocId,
                              float x, float y, double confidence, double secondBestDistance) {
            this.salleId            = salleId;
            this.salleNom           = salleNom;
            this.blocId             = blocId;
            this.x                  = x;
            this.y                  = y;
            this.confidence         = confidence;
            this.secondBestDistance = secondBestDistance;
        }
    }

    private static class Candidate {
        String      salleId;
        String      salleNom;
        String      blocId;
        float       x, y;
        double      distance;
        double      weight;
    }

    // ===== ALGORITHME PRINCIPAL =====

    // Localise l'utilisateur via Weighted k-NN (distance RSSI + poids inversé).
    public static LocationResult locate(Map<String, Double> filteredScan,
                                        List<Fingerprint> fingerprints) {

        if (filteredScan.isEmpty() || fingerprints.isEmpty()) return null;

        // max RSSI détecté → indique si l'utilisateur est dans une zone bien couverte
        double maxDetectedRssi = filteredScan.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(-100.0);
        boolean userInStrongArea = maxDetectedRssi > STRONG_SIGNAL_FLOOR;

        // 1. Grouper les fingerprints par salle
        Map<String, List<Fingerprint>> bySalle = fingerprints.stream()
                .collect(Collectors.groupingBy(f -> f.salleId));

        // 2. Calculer la distance euclidienne normalisée pour chaque salle
        List<Candidate> candidates = new ArrayList<>();

        for (Map.Entry<String, List<Fingerprint>> entry : bySalle.entrySet()) {
            String           salleId = entry.getKey();
            List<Fingerprint> fps    = entry.getValue();

            double sumSq = 0;
            int    count = 0;

            for (Fingerprint fp : fps) {
                double measured = filteredScan.getOrDefault(fp.bssid, PENALTY_RSSI);
                double diff     = measured - fp.rssiMoyen;
                sumSq += diff * diff;
                count++;
            }

            double distance = count > 0
                    ? Math.sqrt(sumSq / count)
                    : Double.MAX_VALUE;

            // Pénalité additive si tous les fingerprints de la salle sont faibles
            // ET que l'utilisateur est dans une zone bien couverte (signal fort détecté)
            boolean allWeak = fps.stream().allMatch(fp -> fp.rssiMoyen < WEAK_FP_THRESHOLD);
            if (allWeak && userInStrongArea) {
                distance += WEAK_FP_PENALTY;
            }

            Candidate c = new Candidate();
            c.salleId  = salleId;
            c.salleNom = fps.get(0).salleNom;
            c.blocId   = fps.get(0).blocId;
            c.x        = fps.get(0).x;
            c.y        = fps.get(0).y;
            c.distance = distance;
            // Poids = 1/distance² (Weighted k-NN)
            c.weight   = 1.0 / (distance * distance + 0.0001);

            candidates.add(c);
        }

        // 3. Trier par distance croissante et prendre les K plus proches
        candidates.sort(Comparator.comparingDouble(c -> c.distance));
        List<Candidate> kNearest = candidates.subList(0, Math.min(K, candidates.size()));

        // 4. Salle gagnante = celle avec le plus grand poids total
        //    (dans le k-NN pondéré, on accumule les poids par salle)
        Map<String, Double> weightBySalle = new HashMap<>();
        for (Candidate c : kNearest) {
            weightBySalle.merge(c.salleId, c.weight, Double::sum);
        }

        String bestSalleId = weightBySalle.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(kNearest.get(0).salleId);

        // 5. Interpolation de position pondérée
        double totalWeight = kNearest.stream().mapToDouble(c -> c.weight).sum();
        double wx = 0, wy = 0;
        for (Candidate c : kNearest) {
            wx += c.weight * c.x;
            wy += c.weight * c.y;
        }
        float interpX = (float)(wx / totalWeight);
        float interpY = (float)(wy / totalWeight);

        // 6. Score de confiance basé sur la distance du meilleur voisin
        double bestDist       = kNearest.get(0).distance;
        double secondBestDist = kNearest.size() > 1 ? kNearest.get(1).distance : bestDist * 2;
        double confidence     = Math.max(0, 1.0 - bestDist / 30.0);
        confidence            = Math.min(1.0, confidence);

        // 7. Récupérer les infos de la salle gagnante
        Candidate winner = kNearest.stream()
                .filter(c -> c.salleId.equals(bestSalleId))
                .findFirst()
                .orElse(kNearest.get(0));

        return new LocationResult(
                winner.salleId, winner.salleNom, winner.blocId,
                interpX, interpY, confidence, secondBestDist
        );
    }
}