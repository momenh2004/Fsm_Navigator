package com.fsm.navigator.service;

import com.fsm.navigator.AppConfig;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.fsm.navigator.auth.TokenManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * HistoryService.java — Historique de navigation côté serveur.
 *   - logNavigation() : enregistre une navigation (POST /api/admin/stats/log)
 *   - getHistory()    : récupère l'historique du membre connecté (GET /api/history)
 */
public class HistoryService {

    private static final String LOG_URL     = AppConfig.BASE_URL + "/api/admin/stats/log";
    private static final String HISTORY_URL = AppConfig.BASE_URL + "/api/history";

    public interface ListCallback {
        void onSuccess(List<HistoryItem> items);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess(String message);
        void onError(String message);
    }

    // ── Modèle ────────────────────────────────────────────────
    public static class HistoryItem {
        public long   id;
        public String type;
        public long   salleId;
        public String salleNom;
        public String blocCode;
        public String blocNom;
        public String createdAt;
    }

    // ── Enregistrer une navigation (type NAVIGATION par défaut) ─
    public static void logNavigation(Context ctx, long salleId) {
        logNavigation(ctx, salleId, "NAVIGATION");
    }

    public static void logNavigation(Context ctx, long salleId, String type) {
        String token = TokenManager.getToken(ctx);
        if (token == null) return;   // invité non connecté → pas d'historique serveur
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("salleId", salleId);
                body.put("type", type);

                URL url = new URL(LOG_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();
                conn.getResponseCode();   // déclenche l'envoi
            } catch (Exception ignored) {}
        }).start();
    }

    // ── Récupérer l'historique du membre connecté ─────────────
    public static void getHistory(Context ctx, ListCallback cb) {
        String token = TokenManager.getToken(ctx);
        if (token == null) { cb.onError("Non connecté"); return; }
        new Thread(() -> {
            try {
                URL url = new URL(HISTORY_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    post(() -> cb.onError("Erreur serveur (" + code + ")"));
                    return;
                }

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONArray arr = new JSONArray(sb.toString());
                List<HistoryItem> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    HistoryItem it = new HistoryItem();
                    it.id        = o.optLong("id");
                    it.type      = o.optString("type");
                    it.salleId   = o.optLong("salleId", -1);
                    it.salleNom  = o.optString("salleNom");
                    it.blocCode  = o.optString("blocCode");
                    it.blocNom   = o.optString("blocNom");
                    it.createdAt = o.optString("createdAt");
                    list.add(it);
                }
                post(() -> cb.onSuccess(list));
            } catch (Exception e) {
                post(() -> cb.onError(e.getMessage()));
            }
        }).start();
    }

    // ── Vider l'historique du membre connecté ─────────────────
    public static void clearHistory(Context ctx, SimpleCallback cb) {
        String token = TokenManager.getToken(ctx);
        if (token == null) { cb.onError("Non connecté"); return; }
        new Thread(() -> {
            try {
                URL url = new URL(HISTORY_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setConnectTimeout(8000);
                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) post(() -> cb.onSuccess("Historique vidé"));
                else                           post(() -> cb.onError("Erreur serveur (" + code + ")"));
            } catch (Exception e) {
                post(() -> cb.onError(e.getMessage()));
            }
        }).start();
    }

    private static void post(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }
}
