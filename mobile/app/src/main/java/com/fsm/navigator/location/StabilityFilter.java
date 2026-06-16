package com.fsm.navigator.location;

/**
 * StabilityFilter.java – Filtre anti-saut entre salles
 *
 * Problème : le k-NN peut détecter des salles différentes à chaque scan
 *            → l'affichage saute entre salles constamment.
 *
 * Solution : n'accepter un changement de salle que si elle est confirmée
 *            CONFIRM_THRESHOLD fois consécutives.
 *
 * Exemple avec CONFIRM_THRESHOLD = 3 :
 *   Scan 1 → 301  (actuelle)
 *   Scan 2 → 302  (candidat, count=1)
 *   Scan 3 → 302  (candidat, count=2)
 *   Scan 4 → 302  (candidat, count=3 → CONFIRMÉ → actuelle = 302)
 */
public class StabilityFilter {

    // Somme de confidences nécessaire pour confirmer un changement de salle.
    // Équivalent à ~2 scans à haute confiance (0.8+) ou ~3 scans moyens (0.55+).
    private static final double CONFIRM_SCORE_THRESHOLD = 1.5;

    private String currentSalle   = null;
    private String candidateSalle = null;
    private double confirmScore   = 0.0;

    // Met à jour le filtre avec la salle détectée (confirme changement si score ≥ seuil).
    public String update(String detected, double confidence) {
        if (detected == null) return currentSalle;

        // Déjà dans la salle actuelle → pas de changement
        if (detected.equals(currentSalle)) {
            confirmScore   = 0.0;
            candidateSalle = null;
            return currentSalle;
        }

        // Même candidat → accumuler le score de confiance
        if (detected.equals(candidateSalle)) {
            confirmScore += confidence;
            if (confirmScore >= CONFIRM_SCORE_THRESHOLD) {
                currentSalle   = detected;
                candidateSalle = null;
                confirmScore   = 0.0;
            }
        } else {
            // Nouveau candidat → réinitialiser avec la confiance du premier scan
            candidateSalle = detected;
            confirmScore   = confidence;
        }

        return currentSalle != null ? currentSalle : detected;
    }

    // Retourne la salle stable (confirmée).
    public String getCurrentSalle() { return currentSalle; }

    // Réinitialise le filtre de stabilité.
    public void reset() { currentSalle = null; candidateSalle = null; confirmScore = 0.0; }
}