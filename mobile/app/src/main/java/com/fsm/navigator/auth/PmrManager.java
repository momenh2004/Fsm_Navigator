package com.fsm.navigator.auth;

/**
 * PmrManager — Gère le mode accessibilité PMR et le profil de l'utilisateur.
 */
public class PmrManager {

    public enum PmrProfile {
        NONE,
        WHEELCHAIR,        // fauteuil roulant
        VISUALLY_IMPAIRED, // malvoyant / non-voyant
        CRUTCHES,          // béquilles / mobilité réduite
        HEARING_IMPAIRED   // malentendant / sourd
    }

    private static boolean    pmrEnabled = false;
    private static PmrProfile pmrProfile = PmrProfile.NONE;

    // Active/désactive le mode PMR.
    public static void setEnabled(boolean enabled) {
        pmrEnabled = enabled;
        if (!enabled) pmrProfile = PmrProfile.NONE;
    }

    // Vérifie si le mode PMR est activé.
    public static boolean isEnabled() { return pmrEnabled; }

    // Bascule le mode PMR.
    public static void toggle() {
        pmrEnabled = !pmrEnabled;
        if (!pmrEnabled) pmrProfile = PmrProfile.NONE;
    }

    // Définit le profil PMR (réactive le mode si différent de NONE).
    public static void setProfile(PmrProfile profile) {
        pmrProfile = profile;
        pmrEnabled = (profile != PmrProfile.NONE);
    }

    // Retourne le profil PMR actif (type de handicap).
    public static PmrProfile getProfile() { return pmrProfile; }

    // Vérifie si la synthèse vocale est autorisée pour le profil courant.
    public static boolean ttsEnabled() {
        switch (pmrProfile) {
            case WHEELCHAIR:
            case VISUALLY_IMPAIRED:
            case CRUTCHES:
                return true;
            default:
                return false;
        }
    }

    // Vérifie si le profil doit éviter les escaliers (fauteuil roulant ou béquilles).
    public static boolean avoidsStairs() {
        return pmrProfile == PmrProfile.WHEELCHAIR
            || pmrProfile == PmrProfile.CRUTCHES;
    }

    // Réinitialise le mode PMR (à la déconnexion).
    public static void reset() {
        pmrEnabled = false;
        pmrProfile = PmrProfile.NONE;
    }
}