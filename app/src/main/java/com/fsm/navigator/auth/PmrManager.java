package com.fsm.navigator.auth;

/**
 * PmrManager.java
 *
 * Gère l'état du mode PMR pour la session en cours.
 * (Non sauvegardé entre sessions — variable statique)
 *
 * Utilisation :
 *   PmrManager.setEnabled(true);
 *   boolean pmr = PmrManager.isEnabled();
 */
public class PmrManager {

    // État PMR pour la session (false par défaut)
    private static boolean pmrEnabled = false;

    // ===== ACTIVER / DÉSACTIVER =====
    public static void setEnabled(boolean enabled) {
        pmrEnabled = enabled;
    }

    // ===== LIRE L'ÉTAT =====
    public static boolean isEnabled() {
        return pmrEnabled;
    }

    // ===== BASCULER =====
    public static void toggle() {
        pmrEnabled = !pmrEnabled;
    }

    // ===== RÉINITIALISER (à la déconnexion) =====
    public static void reset() {
        pmrEnabled = false;
    }
}