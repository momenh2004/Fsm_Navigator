package com.fsm.navigator;

import android.os.Build;

/**
 * Point de configuration unique pour l'URL du serveur.
 *
 * Pour utiliser l'app sur un appareil physique :
 *   → modifier uniquement DEVICE_IP ci-dessous avec l'IP locale du serveur
 *     (ex. : l'IP de votre PC sur le réseau WiFi, visible via `ipconfig`)
 *
 * Sur émulateur Android → 10.0.2.2 est utilisé automatiquement.
 */
public class AppConfig {

    // ─── Seule ligne à modifier pour un appareil physique ───────────────────
    private static final String DEVICE_IP = "192.168.0.201";
    // ────────────────────────────────────────────────────────────────────────

    public static final String BASE_URL;

    static {
        BASE_URL = "http://" + (isEmulator() ? "10.0.2.2" : DEVICE_IP) + ":8080";
    }

    private static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK")
            || Build.MANUFACTURER.contains("Genymotion")
            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
            || "google_sdk".equals(Build.PRODUCT);
    }
}
