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
 * FavoriService.java — Gestion des favoris via API REST
 */
public class FavoriService {

    private static final String BASE_URL = AppConfig.BASE_URL + "/api/favoris";

    public interface FavoriCallback {
        void onSuccess(boolean isFavori, long favoriId);
        void onError(String message);
    }

    public interface ListCallback {
        void onSuccess(List<FavoriItem> favoris);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess(String message);
        void onError(String message);
    }

    // ── Modèle ────────────────────────────────────────────────
    public static class FavoriItem {
        public long   id;
        public String type;
        public String nom;
        public long   salleId;
        public long   blocId;
        public String blocCode;
        public String blocNom;
    }

    // ── Check si favori ───────────────────────────────────────
    public static void checkFavori(Context ctx, String type,
                                   long salleId, long blocId,
                                   FavoriCallback cb) {
        String token = TokenManager.getToken(ctx);
        if (token == null) { cb.onError("Non connecté"); return; }

        new Thread(() -> {
            try {
                String params = "?salleId=" + salleId;

                URL url = new URL(BASE_URL + "/check" + params);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setConnectTimeout(8000);

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    String resp = readResponse(conn);
                    JSONObject json = new JSONObject(resp);
                    boolean is = json.optBoolean("isFavori", false);
                    long    fid= json.optLong("favoriId", -1);
                    new Handler(Looper.getMainLooper()).post(() -> cb.onSuccess(is, fid));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> cb.onError("Erreur serveur (" + code + ")"));
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> cb.onError(e.getMessage()));
            }
        }).start();
    }

    // ── Ajouter favori ────────────────────────────────────────
    public static void addFavori(Context ctx, String type, String nom,
                                 long salleId, long blocId,
                                 SimpleCallback cb) {
        String token = TokenManager.getToken(ctx);
        if (token == null) { cb.onError("Non connecté"); return; }

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                if ("SALLE".equals(type)) body.put("salleId", salleId);
                else                       body.put("blocId",  blocId);

                URL url = new URL(BASE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    new Handler(Looper.getMainLooper()).post(() -> cb.onSuccess("Ajouté aux favoris"));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> cb.onError("Erreur serveur (" + code + ")"));
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> cb.onError(e.getMessage()));
            }
        }).start();
    }

    // ── Supprimer favori ──────────────────────────────────────
    public static void deleteFavori(Context ctx, long favoriId, SimpleCallback cb) {
        String token = TokenManager.getToken(ctx);
        if (token == null) { cb.onError("Non connecté"); return; }

        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/" + favoriId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setConnectTimeout(8000);

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    new Handler(Looper.getMainLooper()).post(() -> cb.onSuccess("Supprimé des favoris"));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> cb.onError("Erreur serveur (" + code + ")"));
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> cb.onError(e.getMessage()));
            }
        }).start();
    }

    // ── Liste des favoris ─────────────────────────────────────
    public static void getFavoris(Context ctx, ListCallback cb) {
        String token = TokenManager.getToken(ctx);
        if (token == null) { cb.onError("Non connecté"); return; }

        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setConnectTimeout(8000);

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    String resp = readResponse(conn);
                    JSONArray arr = new JSONArray(resp);
                    List<FavoriItem> list = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        FavoriItem item = new FavoriItem();
                        item.id       = obj.optLong("id");
                        item.nom      = obj.optString("salleNom");
                        item.salleId  = obj.optLong("salleId", -1);
                        item.blocCode = obj.optString("blocCode");
                        item.blocNom  = obj.optString("blocNom");
                        list.add(item);
                    }
                    new Handler(Looper.getMainLooper()).post(() -> cb.onSuccess(list));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> cb.onError("Erreur serveur (" + code + ")"));
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> cb.onError(e.getMessage()));
            }
        }).start();
    }

    // ── Helper ────────────────────────────────────────────────
    private static String readResponse(HttpURLConnection conn) throws Exception {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }
}
