package com.fsm.navigator.auth;

import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * AuthService.java – VERSION COMPLÈTE
 *
 * Appels HTTP vers le backend Spring Boot :
 *   POST /api/auth/login
 *   POST /api/auth/register
 *   POST /api/auth/change-password   ← NOUVEAU
 */
public class AuthService {

    private static final String BASE_URL = "http://10.0.2.2:8080/api/auth";

    // ===== INTERFACES CALLBACK =====
    public interface AuthCallback {
        void onSuccess(String token, String email, String role);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess(String message);
        void onError(String message);
    }

    // =========================================================
    // LOGIN
    // =========================================================
    public static void login(String email, String password, AuthCallback callback) {
        new AsyncTask<Void, Void, String[]>() {
            @Override
            protected String[] doInBackground(Void... voids) {
                try {
                    JSONObject body = new JSONObject();
                    body.put("email", email);
                    body.put("password", password);

                    URL url = new URL(BASE_URL + "/login");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();

                    int status = conn.getResponseCode();
                    if (status == 200) {
                        String resp = readResponse(conn.getInputStream());
                        JSONObject response = new JSONObject(resp);
                        return new String[]{"OK",
                                response.getString("token"),
                                response.getString("email"),
                                response.optString("role", "ETUDIANT")};
                    } else {
                        String resp = readResponse(conn.getErrorStream());
                        JSONObject err = new JSONObject(resp);
                        return new String[]{"ERROR", err.optString("message", "Erreur de connexion")};
                    }
                } catch (Exception e) {
                    return new String[]{"ERROR", "Impossible de joindre le serveur"};
                }
            }

            @Override
            protected void onPostExecute(String[] result) {
                if ("OK".equals(result[0])) callback.onSuccess(result[1], result[2], result[3]);
                else callback.onError(result[1]);
            }
        }.execute();
    }

    // =========================================================
    // REGISTER
    // =========================================================
    public static void register(String email, String password, AuthCallback callback) {
        new AsyncTask<Void, Void, String[]>() {
            @Override
            protected String[] doInBackground(Void... voids) {
                try {
                    JSONObject body = new JSONObject();
                    body.put("email", email);
                    body.put("password", password);
                    body.put("role", "ETUDIANT");

                    URL url = new URL(BASE_URL + "/register");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();

                    int status = conn.getResponseCode();
                    if (status == 200 || status == 201) {
                        String resp = readResponse(conn.getInputStream());
                        JSONObject response = new JSONObject(resp);
                        return new String[]{"OK",
                                response.getString("token"),
                                response.getString("email"),
                                response.optString("role", "ETUDIANT")};
                    } else {
                        String resp = readResponse(conn.getErrorStream());
                        JSONObject err = new JSONObject(resp);
                        return new String[]{"ERROR", err.optString("message", "Erreur d'inscription")};
                    }
                } catch (Exception e) {
                    return new String[]{"ERROR", "Impossible de joindre le serveur"};
                }
            }

            @Override
            protected void onPostExecute(String[] result) {
                if ("OK".equals(result[0])) callback.onSuccess(result[1], result[2], result[3]);
                else callback.onError(result[1]);
            }
        }.execute();
    }

    // =========================================================
    // CHANGE PASSWORD
    // =========================================================
    public static void changePassword(String token, String oldPassword,
                                      String newPassword, SimpleCallback callback) {
        new AsyncTask<Void, Void, String[]>() {
            @Override
            protected String[] doInBackground(Void... voids) {
                try {
                    JSONObject body = new JSONObject();
                    body.put("oldPassword", oldPassword);
                    body.put("newPassword", newPassword);

                    URL url = new URL(BASE_URL + "/change-password");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + token); // JWT
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();

                    int status = conn.getResponseCode();
                    if (status == 200) {
                        return new String[]{"OK", "Mot de passe modifié"};
                    } else {
                        String resp = readResponse(conn.getErrorStream());
                        JSONObject err = new JSONObject(resp);
                        return new String[]{"ERROR", err.optString("message", "Erreur")};
                    }
                } catch (Exception e) {
                    return new String[]{"ERROR", "Impossible de joindre le serveur"};
                }
            }

            @Override
            protected void onPostExecute(String[] result) {
                if ("OK".equals(result[0])) callback.onSuccess(result[1]);
                else callback.onError(result[1]);
            }
        }.execute();
    }

    // =========================================================
    // HELPER
    // =========================================================
    private static String readResponse(java.io.InputStream stream) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }
    public static void deleteAccount(String token, SimpleCallback callback) {
        new AsyncTask<Void, Void, String[]>() {
            @Override
            protected String[] doInBackground(Void... voids) {
                try {
                    URL url = new URL(BASE_URL + "/account");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("DELETE");
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    int status = conn.getResponseCode();
                    if (status == 200) {
                        return new String[]{"OK", "Compte supprimé"};
                    } else {
                        String resp = readResponse(conn.getErrorStream());
                        JSONObject err = new JSONObject(resp);
                        return new String[]{"ERROR", err.optString("message", "Erreur")};
                    }
                } catch (Exception e) {
                    return new String[]{"ERROR", "Impossible de joindre le serveur"};
                }
            }

            @Override
            protected void onPostExecute(String[] result) {
                if ("OK".equals(result[0])) callback.onSuccess(result[1]);
                else callback.onError(result[1]);
            }
        }.execute();
    }
}