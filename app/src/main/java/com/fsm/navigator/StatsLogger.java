package com.fsm.navigator;

import com.fsm.navigator.auth.TokenManager;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * StatsLogger.java
 * Enregistre les navigations et consultations de salles pour les stats admin.
 */
public class StatsLogger {

    private static final String BASE_URL = "http://10.0.2.2:8080/api/admin/stats/log";

    public static void logView(android.content.Context ctx,
                               long salleId, String salleNom, String blocCode) {
        log(ctx, salleId, salleNom, blocCode, "VIEW");
    }

    public static void logNavigation(android.content.Context ctx,
                                     long salleId, String salleNom, String blocCode) {
        log(ctx, salleId, salleNom, blocCode, "NAVIGATION");
    }

    private static void log(android.content.Context ctx, long salleId,
                            String salleNom, String blocCode, String type) {
        String email = TokenManager.getEmail(ctx);
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("salleId",   salleId);
                body.put("salleNom",  salleNom != null ? salleNom : "");
                body.put("blocCode",  blocCode  != null ? blocCode  : "");
                body.put("type",      type);
                body.put("userEmail", email != null ? email : "anonyme");

                URL url = new URL(BASE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception ignored) {}
        }).start();
    }
}