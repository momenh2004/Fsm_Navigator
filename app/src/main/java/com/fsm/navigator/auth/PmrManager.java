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

    // ===== ACTIVER / DÉSACTIVER =====
    public static void setEnabled(boolean enabled) {
        pmrEnabled = enabled;
        if (!enabled) pmrProfile = PmrProfile.NONE;
    }

    public static boolean isEnabled() { return pmrEnabled; }

    public static void toggle() {
        pmrEnabled = !pmrEnabled;
        if (!pmrEnabled) pmrProfile = PmrProfile.NONE;
    }

    // ===== PROFIL =====
    public static void setProfile(PmrProfile profile) {
        pmrProfile = profile;
        pmrEnabled = (profile != PmrProfile.NONE);
    }

    public static PmrProfile getProfile() { return pmrProfile; }

    // ===== TTS AUTORISÉ pour ce profil ? =====
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

    // ===== ÉVITER LES ESCALIERS ? =====
    public static boolean avoidsStairs() {
        return pmrProfile == PmrProfile.WHEELCHAIR
            || pmrProfile == PmrProfile.CRUTCHES;
    }

    // ===== RÉINITIALISER (à la déconnexion) =====
    public static void reset() {
        pmrEnabled = false;
        pmrProfile = PmrProfile.NONE;
    }
}