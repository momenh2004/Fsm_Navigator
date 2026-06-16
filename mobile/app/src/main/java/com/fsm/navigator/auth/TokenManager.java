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

    // Sauvegarde le token JWT dans SharedPreferences.
    public static void saveToken(Context context, String token) {
        getPrefs(context).edit().putString(KEY_TOKEN, token).apply();
    }

    // Récupère le token JWT sauvegardé (null si non connecté).
    public static String getToken(Context context) {
        return getPrefs(context).getString(KEY_TOKEN, null);
    }

    // Vérifie si un token valide est stocké (utilisateur connecté).
    public static boolean isLoggedIn(Context context) {
        return getToken(context) != null;
    }

    // Sauvegarde l'email de l'utilisateur.
    public static void saveEmail(Context context, String email) {
        getPrefs(context).edit().putString(KEY_EMAIL, email).apply();
    }

    // Récupère l'email sauvegardé.
    public static String getEmail(Context context) {
        return getPrefs(context).getString(KEY_EMAIL, "");
    }

    // Sauvegarde le rôle utilisateur (ETUDIANT, etc).
    public static void saveRole(Context context, String role) {
        getPrefs(context).edit().putString(KEY_ROLE, role).apply();
    }

    // Récupère le rôle utilisateur.
    public static String getRole(Context context) {
        return getPrefs(context).getString(KEY_ROLE, "ETUDIANT");
    }

    // Supprime tous les éléments (déconnexion).
    public static void clearToken(Context context) {
        getPrefs(context).edit().clear().apply();
    }

    // Accède aux préférences partagées sécurisées.
    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}