package com.fsm.navigator.auth;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * TokenManager.java
 *
 * Gère la sauvegarde et la lecture du token JWT
 * dans SharedPreferences (stockage local sécurisé).
 *
 * Utilisation :
 *   TokenManager.saveToken(context, "eyJhbGci...");
 *   String token = TokenManager.getToken(context);
 *   TokenManager.clearToken(context);   // déconnexion
 */
public class TokenManager {

    private static final String PREFS_NAME = "fsm_auth_prefs";
    private static final String KEY_TOKEN  = "jwt_token";
    private static final String KEY_EMAIL  = "user_email";
    private static final String KEY_ROLE   = "user_role";

    // ===== SAUVEGARDER LE TOKEN =====
    public static void saveToken(Context context, String token) {
        getPrefs(context).edit().putString(KEY_TOKEN, token).apply();
    }

    // ===== LIRE LE TOKEN =====
    public static String getToken(Context context) {
        return getPrefs(context).getString(KEY_TOKEN, null);
    }

    // ===== VÉRIFIER SI CONNECTÉ =====
    public static boolean isLoggedIn(Context context) {
        return getToken(context) != null;
    }

    // ===== SAUVEGARDER EMAIL =====
    public static void saveEmail(Context context, String email) {
        getPrefs(context).edit().putString(KEY_EMAIL, email).apply();
    }

    // ===== LIRE EMAIL =====
    public static String getEmail(Context context) {
        return getPrefs(context).getString(KEY_EMAIL, "");
    }

    // ===== SAUVEGARDER RÔLE =====
    public static void saveRole(Context context, String role) {
        getPrefs(context).edit().putString(KEY_ROLE, role).apply();
    }

    // ===== LIRE RÔLE =====
    public static String getRole(Context context) {
        return getPrefs(context).getString(KEY_ROLE, "ETUDIANT");
    }

    // ===== DÉCONNEXION (effacer tout) =====
    public static void clearToken(Context context) {
        getPrefs(context).edit().clear().apply();
    }

    // ===== ACCÈS AUX PREFERENCES =====
    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}