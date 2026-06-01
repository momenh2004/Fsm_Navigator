package com.fsm.navigator;

import android.os.Build;

// Point de configuration unique pour l'URL du serveur (ngrok ou émulateur).
public class AppConfig {

    private static final String NGROK_URL = "https://scientist-flaring-fondness.ngrok-free.dev";

    public static final String FSM_WIFI_SSID = "Wifi-FSM";

    public static final String BASE_URL;

    static {
        BASE_URL = isEmulator() ? "http://10.0.2.2:8080" : NGROK_URL;
    }

    // Détecte si l'app tourne sur émulateur Android (utilise alors 10.0.2.2).
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
