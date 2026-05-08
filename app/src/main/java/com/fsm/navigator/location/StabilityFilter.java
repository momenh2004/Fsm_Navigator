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

    private static final int CONFIRM_THRESHOLD = 3;

    private String currentSalle   = null;
    private String candidateSalle = null;
    private int    confirmCount   = 0;

    /**
     * Met à jour le filtre avec la nouvelle salle détectée.
     * @param detected  Salle détectée par le Weighted k-NN
     * @return          Salle stable (après filtrage)
     */
    public String update(String detected) {
        if (detected == null) return currentSalle;

        // Déjà dans la salle actuelle → pas de changement
        if (detected.equals(currentSalle)) {
            confirmCount  = 0;
            candidateSalle = null;
            return currentSalle;
        }

        // Même candidat qu'avant → incrémenter le compteur
        if (detected.equals(candidateSalle)) {
            confirmCount++;
            if (confirmCount >= CONFIRM_THRESHOLD) {
                // Changement confirmé
                currentSalle   = detected;
                candidateSalle = null;
                confirmCount   = 0;
            }
        } else {
            // Nouveau candidat → réinitialiser
            candidateSalle = detected;
            confirmCount   = 1;
        }

        // Retourner la salle actuelle stable
        return currentSalle != null ? currentSalle : detected;
    }

    public String getCurrentSalle()   { return currentSalle; }
    public void   reset()             { currentSalle = null; candidateSalle = null; confirmCount = 0; }
}