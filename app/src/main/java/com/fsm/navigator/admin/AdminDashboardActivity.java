package com.fsm.navigator.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fsm.navigator.LoginActivity;
import com.fsm.navigator.R;
import com.fsm.navigator.auth.TokenManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String BASE_URL   = "http://10.0.2.2:8080/api/admin";
    private static final String STATS_URL  = "http://10.0.2.2:8080/api/admin/stats";

    private String adminToken;
    private String adminEmail;

    // Cards
    private TextView tvStatBlocs, tvStatSalles, tvStatUsers,
            tvStatFps, tvStatNav, tvStatViews;

    // Section containers
    private LinearLayout sectionUsers, sectionBlocs, sectionSalles, sectionFps;

    // Lists
    private LinearLayout listUsers, listBlocs, listSalles, listFps;

    // Search fields
    private EditText etSearchUsers, etSearchBlocs, etSearchSalles, etSearchFps;

    // Data
    private List<JSONObject> allUsers  = new ArrayList<>();
    private List<JSONObject> allBlocs  = new ArrayList<>();
    private List<JSONObject> allSalles = new ArrayList<>();
    private List<JSONObject> allFps    = new ArrayList<>();

    // Chart containers
    private LinearLayout containerWifi, containerRssi, containerTypes, containerTopNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adminToken = getIntent().getStringExtra("ADMIN_TOKEN");
        adminEmail = getIntent().getStringExtra("ADMIN_EMAIL");

        buildUI();
        loadAll();
    }

    // =========================================================
    // BUILD UI PROGRAMMATICALLY
    // =========================================================
    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF1A1A2E);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(48), dp(16), dp(64));
        scroll.addView(root);

        // ── HEADER ──────────────────────────────────────────
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        lp(header, -1, -2, 0, 0, 0, dp(24));
        root.addView(header);

        ImageButton btnBack = new ImageButton(this);
        btnBack.setImageResource(android.R.drawable.ic_media_previous);
        btnBack.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        btnBack.setColorFilter(0xFF00D4FF);
        btnBack.setOnClickListener(v -> finish());
        lp(btnBack, dp(40), dp(40), 0, 0, dp(12), 0);
        header.addView(btnBack);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("FSM Admin Dashboard");
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTextSize(20f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lpTitle = new LinearLayout.LayoutParams(0, -2, 1f);
        tvTitle.setLayoutParams(lpTitle);
        header.addView(tvTitle);

        TextView tvLogout = new TextView(this);
        tvLogout.setText("⏻");
        tvLogout.setTextColor(0xFFEF5350);
        tvLogout.setTextSize(22f);
        tvLogout.setPadding(dp(8), dp(4), dp(8), dp(4));
        tvLogout.setOnClickListener(v -> showLogoutDialog());
        header.addView(tvLogout);

        // ── CARDS ────────────────────────────────────────────
        root.addView(sectionTitle("📊 Vue d'ensemble"));
        LinearLayout row1 = makeRow(); root.addView(row1);
        tvStatBlocs  = addCard(row1, "Blocs",        "...", 0xFF00D4FF);
        tvStatSalles = addCard(row1, "Salles",       "...", 0xFF7B2FBE);
        tvStatUsers  = addCard(row1, "Utilisateurs", "...", 0xFF00B894);
        LinearLayout row2 = makeRow(); root.addView(row2);
        tvStatFps    = addCard(row2, "Fingerprints", "...", 0xFFE17055);
        tvStatNav    = addCard(row2, "Navigations",  "...", 0xFF0984E3);
        tvStatViews  = addCard(row2, "Consultations","...", 0xFFF39C12);

        // ── GRAPHIQUES ───────────────────────────────────────
        root.addView(sectionTitle("📶 Couverture WiFi par Bloc"));
        root.addView(legendText("Nombre de fingerprints par bloc"));
        containerWifi = chartBox(root);

        root.addView(sectionTitle("📡 Distribution RSSI"));
        root.addView(legendText("Qualité du signal — idéal entre -50 et -70 dBm"));
        containerRssi = chartBox(root);

        root.addView(sectionTitle("🏛️ Types de Salles"));
        containerTypes = chartBox(root);

        root.addView(sectionTitle("🧭 Top 7 Salles Naviguées"));
        containerTopNav = chartBox(root);

        // ── UTILISATEURS ─────────────────────────────────────
        root.addView(sectionHeader("👥 Utilisateurs", () -> {
            sectionUsers.setVisibility(
                    sectionUsers.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
        }));
        sectionUsers = new LinearLayout(this);
        sectionUsers.setOrientation(LinearLayout.VERTICAL);
        root.addView(sectionUsers);

        LinearLayout rowSearchUsers = makeSearchRow(sectionUsers);
        etSearchUsers = (EditText) rowSearchUsers.getChildAt(0);
        addActionButton(rowSearchUsers, "➕", () -> showAddUserDialog());
        etSearchUsers.addTextChangedListener(new SimpleWatcher(q -> filterAndRenderUsers(q)));
        listUsers = new LinearLayout(this); listUsers.setOrientation(LinearLayout.VERTICAL);
        sectionUsers.addView(listUsers);

        // ── BLOCS ────────────────────────────────────────────
        root.addView(sectionHeader("🏗️ Blocs", () -> {
            sectionBlocs.setVisibility(
                    sectionBlocs.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
        }));
        sectionBlocs = new LinearLayout(this);
        sectionBlocs.setOrientation(LinearLayout.VERTICAL);
        root.addView(sectionBlocs);

        LinearLayout rowSearchBlocs = makeSearchRow(sectionBlocs);
        etSearchBlocs = (EditText) rowSearchBlocs.getChildAt(0);
        addActionButton(rowSearchBlocs, "➕ Wizard", () -> showWizardStep1());
        etSearchBlocs.addTextChangedListener(new SimpleWatcher(q -> filterAndRenderBlocs(q)));
        listBlocs = new LinearLayout(this); listBlocs.setOrientation(LinearLayout.VERTICAL);
        sectionBlocs.addView(listBlocs);

        // ── SALLES ───────────────────────────────────────────
        root.addView(sectionHeader("🚪 Salles", () -> {
            sectionSalles.setVisibility(
                    sectionSalles.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
        }));
        sectionSalles = new LinearLayout(this);
        sectionSalles.setOrientation(LinearLayout.VERTICAL);
        root.addView(sectionSalles);

        LinearLayout rowSearchSalles = makeSearchRow(sectionSalles);
        etSearchSalles = (EditText) rowSearchSalles.getChildAt(0);
        etSearchSalles.addTextChangedListener(new SimpleWatcher(q -> filterAndRenderSalles(q)));
        listSalles = new LinearLayout(this); listSalles.setOrientation(LinearLayout.VERTICAL);
        sectionSalles.addView(listSalles);

        // ── FINGERPRINTS ─────────────────────────────────────
        root.addView(sectionHeader("📡 Fingerprints", () -> {
            sectionFps.setVisibility(
                    sectionFps.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
        }));
        sectionFps = new LinearLayout(this);
        sectionFps.setOrientation(LinearLayout.VERTICAL);
        root.addView(sectionFps);

        LinearLayout rowSearchFps = makeSearchRow(sectionFps);
        etSearchFps = (EditText) rowSearchFps.getChildAt(0);
        etSearchFps.addTextChangedListener(new SimpleWatcher(q -> filterAndRenderFps(q)));
        listFps = new LinearLayout(this); listFps.setOrientation(LinearLayout.VERTICAL);
        sectionFps.addView(listFps);

        setContentView(scroll);
    }

    // =========================================================
    // LOAD ALL DATA
    // =========================================================
    private void loadAll() {
        loadStats();
        loadWifiCoverage();
        loadRssiDistribution();
        loadSalleTypes();
        loadTopNavigated();
        loadUsers();
        loadBlocs();
        loadSalles();
        loadFps();
    }

    private void loadStats() {
        fetchObj(STATS_URL + "/overview", result -> {
            tvStatBlocs.setText(result.optString("totalBlocs",        "0"));
            tvStatSalles.setText(result.optString("totalSalles",      "0"));
            tvStatUsers.setText(result.optString("totalUsers",        "0"));
            tvStatFps.setText(result.optString("totalFingerprints",   "0"));
            tvStatNav.setText(result.optString("totalNavigations",    "0"));
            tvStatViews.setText(result.optString("totalViews",        "0"));
        });
    }

    private void loadWifiCoverage() {
        fetchArr(STATS_URL + "/wifi-coverage", array -> {
            List<String> labels = new ArrayList<>();
            List<Float>  values = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.optJSONObject(i); if (o == null) continue;
                labels.add(o.optString("blocCode", "?"));
                values.add((float) o.optLong("fingerprints", 0));
            }
            drawBar(containerWifi, labels, values, null, 0xFF00D4FF);
        });
    }

    private void loadRssiDistribution() {
        fetchObj(STATS_URL + "/rssi-distribution", result -> {
            String[] lbl = {"<-80","-80~-70","-70~-60","-60~-50",">-50"};
            int[] col = {0xFFE74C3C,0xFFE67E22,0xFFF1C40F,0xFF2ECC71,0xFF3498DB};
            JSONArray vals = result.optJSONArray("values");
            if (vals == null) return;
            List<String> labels = new ArrayList<>();
            List<Float>  values = new ArrayList<>();
            List<Integer>colors = new ArrayList<>();
            for (int i = 0; i < vals.length(); i++) {
                labels.add(lbl[i]); values.add((float) vals.optInt(i,0)); colors.add(col[i]);
            }
            drawBar(containerRssi, labels, values, colors, 0);
        });
    }

    private void loadSalleTypes() {
        fetchObj(STATS_URL + "/salle-types", result -> {
            List<String> labels = new ArrayList<>();
            List<Float>  values = new ArrayList<>();
            List<Integer>colors = new ArrayList<>();
            int[] palette = {0xFF00D4FF,0xFF7B2FBE,0xFF00B894,0xFFE17055,0xFF0984E3,0xFFF39C12};
            int ci = 0;
            Iterator<String> keys = result.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                labels.add(key); values.add((float) result.optLong(key, 0));
                colors.add(palette[ci % palette.length]); ci++;
            }
            drawPie(containerTypes, labels, values, colors);
        });
    }

    private void loadTopNavigated() {
        fetchArr(STATS_URL + "/top-navigated", array -> {
            List<String> labels = new ArrayList<>();
            List<Float>  values = new ArrayList<>();
            for (int i = 0; i < Math.min(array.length(), 7); i++) {
                JSONObject o = array.optJSONObject(i); if (o == null) continue;
                labels.add(o.optString("salleNom","?").replace("Salle ","S"));
                values.add((float) o.optLong("count", 0));
            }
            if (labels.isEmpty()) showEmptyChart(containerTopNav, "Aucune navigation");
            else drawBar(containerTopNav, labels, values, null, 0xFF00B894);
        });
    }

    // ── USERS ────────────────────────────────────────────────
    private void loadUsers() {
        fetchArr(BASE_URL + "/users", array -> {
            allUsers.clear();
            for (int i = 0; i < array.length(); i++) allUsers.add(array.optJSONObject(i));
            filterAndRenderUsers("");
        });
    }

    private void filterAndRenderUsers(String q) {
        listUsers.removeAllViews();
        int count = 0;
        for (JSONObject u : allUsers) {
            String email = u.optString("email","");
            if (!q.isEmpty() && !email.toLowerCase().contains(q.toLowerCase())) continue;
            if (count >= 20) { addMoreButton(listUsers, "Voir plus d'utilisateurs", () -> filterAndRenderUsers("")); break; }
            listUsers.addView(makeListItem(
                    email,
                    "ID: " + u.optLong("id") + " • " + u.optString("role",""),
                    String.valueOf(email.isEmpty() ? 'U' : Character.toUpperCase(email.charAt(0))),
                    0xFF00B894,
                    () -> showEditUserDialog(u),
                    () -> deleteUser(u.optLong("id"), email)
            ));
            count++;
        }
        if (allUsers.isEmpty()) addEmpty(listUsers, "Aucun utilisateur");
    }

    // ── BLOCS ─────────────────────────────────────────────────
    private void loadBlocs() {
        fetchArr(BASE_URL + "/blocs", array -> {
            allBlocs.clear();
            for (int i = 0; i < array.length(); i++) allBlocs.add(array.optJSONObject(i));
            filterAndRenderBlocs("");
        });
    }

    private void filterAndRenderBlocs(String q) {
        listBlocs.removeAllViews();
        int count = 0;
        for (JSONObject b : allBlocs) {
            String nom  = b.optString("nom","");
            String code = b.optString("code","");
            if (!q.isEmpty() && !nom.toLowerCase().contains(q.toLowerCase())
                    && !code.toLowerCase().contains(q.toLowerCase())) continue;
            if (count >= 20) { addMoreButton(listBlocs, "Voir plus de blocs", () -> filterAndRenderBlocs("")); break; }
            int nbEtages = b.optJSONArray("etages") != null ? b.optJSONArray("etages").length() : 0;
            listBlocs.addView(makeListItem(
                    nom,
                    code + " • " + nbEtages + " étage(s)",
                    code.isEmpty() ? "B" : String.valueOf(code.charAt(0)),
                    0xFF00D4FF,
                    () -> showEditBlocDialog(b),
                    () -> deleteBloc(b.optLong("id"), nom)
            ));
            count++;
        }
        if (allBlocs.isEmpty()) addEmpty(listBlocs, "Aucun bloc");
    }

    // ── SALLES ────────────────────────────────────────────────
    private void loadSalles() {
        fetchArr(BASE_URL + "/salles", array -> {
            allSalles.clear();
            for (int i = 0; i < array.length(); i++) allSalles.add(array.optJSONObject(i));
            filterAndRenderSalles("");
        });
    }

    private void filterAndRenderSalles(String q) {
        listSalles.removeAllViews();
        int count = 0;
        for (JSONObject s : allSalles) {
            String nom = s.optString("nom","");
            String cat = s.optString("categorie","");
            if (!q.isEmpty() && !nom.toLowerCase().contains(q.toLowerCase())) continue;
            if (count >= 20) { addMoreButton(listSalles, "Voir plus de salles", () -> filterAndRenderSalles("")); break; }
            boolean etude = s.optBoolean("estSalleEtude", true);
            listSalles.addView(makeListItem(
                    nom,
                    cat + (etude ? " • Étude" : " • Autre"),
                    nom.isEmpty() ? "S" : String.valueOf(nom.charAt(0)),
                    0xFF7B2FBE,
                    () -> showEditSalleDialog(s),
                    () -> deleteSalle(s.optLong("id"), nom)
            ));
            count++;
        }
        if (allSalles.isEmpty()) addEmpty(listSalles, "Aucune salle");
    }

    // ── FINGERPRINTS ──────────────────────────────────────────
    private void loadFps() {
        fetchArr(BASE_URL + "/fingerprints", array -> {
            allFps.clear();
            for (int i = 0; i < array.length(); i++) allFps.add(array.optJSONObject(i));
            filterAndRenderFps("");
        });
    }

    private void filterAndRenderFps(String q) {
        listFps.removeAllViews();
        int count = 0;
        for (JSONObject f : allFps) {
            String bssid = f.optString("bssid","");
            String poi   = f.optString("poiNom","");
            if (!q.isEmpty() && !bssid.contains(q.toLowerCase()) && !poi.toLowerCase().contains(q.toLowerCase())) continue;
            if (count >= 20) { addMoreButton(listFps, "Voir plus de fingerprints", () -> filterAndRenderFps("")); break; }
            listFps.addView(makeListItem(
                    bssid,
                    f.optString("ssid","") + (poi.isEmpty() ? "" : " • " + poi)
                            + "  " + String.format("%.1f dBm", f.optDouble("rssiMoyen",0)),
                    "F",
                    0xFFE17055,
                    () -> showEditFpDialog(f),
                    () -> deleteFp(f.optLong("id"), bssid)
            ));
            count++;
        }
        if (allFps.isEmpty()) addEmpty(listFps, "Aucun fingerprint");
    }

    // =========================================================
    // DIALOGS — USERS
    // =========================================================
    private void showAddUserDialog()          { showUserDialog(null); }
    private void showEditUserDialog(JSONObject u) { showUserDialog(u); }

    private void showUserDialog(JSONObject existing) {
        boolean isEdit = existing != null;
        LinearLayout layout = dialog();
        EditText etEmail    = input("Email *", false, false);
        EditText etPassword = input(isEdit ? "Nouveau mot de passe (optionnel)" : "Mot de passe *", true, false);
        CheckBox cbVisiteur = check("Visiteur (décoché = Étudiant)");
        if (isEdit) {
            etEmail.setText(existing.optString("email",""));
            if ("VISITEUR".equals(existing.optString("role",""))) cbVisiteur.setChecked(true);
        }
        layout.addView(label("Email *")); layout.addView(etEmail);
        layout.addView(label("Mot de passe")); layout.addView(etPassword);
        layout.addView(cbVisiteur);

        new AlertDialog.Builder(this)
                .setTitle(isEdit ? "✏️ Modifier utilisateur" : "➕ Nouvel utilisateur")
                .setView(layout)
                .setPositiveButton("Enregistrer", (d, w) -> {
                    try {
                        String email = etEmail.getText().toString().trim();
                        String pass  = etPassword.getText().toString().trim();
                        String role  = cbVisiteur.isChecked() ? "VISITEUR" : "ETUDIANT";
                        if (email.isEmpty()) { toast("Email requis"); return; }
                        JSONObject body = new JSONObject();
                        body.put("email", email); body.put("role", role);
                        if (!pass.isEmpty()) body.put("password", pass);
                        String url = isEdit
                                ? BASE_URL + "/users/" + existing.getLong("id")
                                : BASE_URL + "/users";
                        String method = isEdit ? "PUT" : "POST";
                        apiCall(method, url, body, () -> { toast("✅ Enregistré"); loadUsers(); loadStats(); });
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("Annuler", null).show();
    }

    private void deleteUser(long id, String email) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer").setMessage("Supprimer " + email + " ?")
                .setPositiveButton("Supprimer", (d, w) ->
                        apiCall("DELETE", BASE_URL + "/users/" + id, null,
                                () -> { toast("✅ Supprimé"); loadUsers(); loadStats(); }))
                .setNegativeButton("Annuler", null).show();
    }

    // =========================================================
    // DIALOGS — BLOCS
    // =========================================================
    private void showEditBlocDialog(JSONObject bloc) {
        LinearLayout layout = dialog();
        EditText etNom  = input("Nom", false, false);
        EditText etDesc = input("Description", false, false);
        CheckBox cbPmr  = check("Accessible PMR");
        etNom.setText(bloc.optString("nom",""));
        etDesc.setText(bloc.optString("description",""));
        cbPmr.setChecked(bloc.optBoolean("accessiblePmr", false));
        layout.addView(label("Nom")); layout.addView(etNom);
        layout.addView(label("Description")); layout.addView(etDesc);
        layout.addView(cbPmr);

        new AlertDialog.Builder(this)
                .setTitle("✏️ Modifier le bloc")
                .setView(layout)
                .setPositiveButton("Enregistrer", (d, w) -> {
                    try {
                        JSONObject body = new JSONObject();
                        body.put("nom", etNom.getText().toString().trim());
                        body.put("description", etDesc.getText().toString().trim());
                        body.put("accessiblePmr", cbPmr.isChecked());
                        apiCall("PUT", BASE_URL + "/blocs/" + bloc.getLong("id"), body,
                                () -> { toast("✅ Modifié"); loadBlocs(); loadStats(); });
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("Annuler", null).show();
    }

    private void deleteBloc(long id, String nom) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer").setMessage("Supprimer " + nom + " et toutes ses salles ?")
                .setPositiveButton("Supprimer", (d, w) ->
                        apiCall("DELETE", BASE_URL + "/blocs/" + id, null,
                                () -> { toast("✅ Supprimé"); loadBlocs(); loadSalles(); loadStats(); }))
                .setNegativeButton("Annuler", null).show();
    }

    // =========================================================
    // DIALOGS — SALLES
    // =========================================================
    private void showEditSalleDialog(JSONObject s) {
        LinearLayout layout = dialog();
        EditText etNom = input("Nom", false, false);
        CheckBox cbEtude = check("Salle d'étude");
        CheckBox cbPmr   = check("Accessible PMR");
        etNom.setText(s.optString("nom",""));
        cbEtude.setChecked(s.optBoolean("estSalleEtude", true));
        cbPmr.setChecked(s.optBoolean("accessiblePmr", false));
        layout.addView(etNom); layout.addView(cbEtude); layout.addView(cbPmr);

        new AlertDialog.Builder(this)
                .setTitle("✏️ Modifier la salle")
                .setView(layout)
                .setPositiveButton("Enregistrer", (d, w) -> {
                    try {
                        JSONObject body = new JSONObject();
                        body.put("nom", etNom.getText().toString().trim());
                        body.put("estSalleEtude", cbEtude.isChecked());
                        body.put("accessiblePmr", cbPmr.isChecked());
                        apiCall("PUT", BASE_URL + "/salles/" + s.getLong("id"), body,
                                () -> { toast("✅ Modifiée"); loadSalles(); });
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("Annuler", null).show();
    }

    private void deleteSalle(long id, String nom) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer").setMessage("Supprimer " + nom + " ?")
                .setPositiveButton("Supprimer", (d, w) ->
                        apiCall("DELETE", BASE_URL + "/salles/" + id, null,
                                () -> { toast("✅ Supprimée"); loadSalles(); loadStats(); }))
                .setNegativeButton("Annuler", null).show();
    }

    // =========================================================
    // DIALOGS — FINGERPRINTS
    // =========================================================
    private void showEditFpDialog(JSONObject f) {
        LinearLayout layout = dialog();
        EditText etBssid = input("BSSID", false, false);
        EditText etSsid  = input("SSID", false, false);
        EditText etRssi  = input("RSSI (ex: -65.5)", false, true);
        etBssid.setText(f.optString("bssid",""));
        etSsid.setText(f.optString("ssid",""));
        etRssi.setText(String.valueOf(f.optDouble("rssiMoyen", 0)));
        layout.addView(label("BSSID")); layout.addView(etBssid);
        layout.addView(label("SSID"));  layout.addView(etSsid);
        layout.addView(label("RSSI"));  layout.addView(etRssi);

        new AlertDialog.Builder(this)
                .setTitle("✏️ Modifier fingerprint")
                .setView(layout)
                .setPositiveButton("Enregistrer", (d, w) -> {
                    try {
                        JSONObject body = new JSONObject();
                        body.put("bssid", etBssid.getText().toString().trim().toLowerCase());
                        body.put("ssid",  etSsid.getText().toString().trim());
                        body.put("rssiMoyen", Double.parseDouble(etRssi.getText().toString().trim()));
                        apiCall("PUT", BASE_URL + "/fingerprints/" + f.getLong("id"), body,
                                () -> { toast("✅ Modifié"); loadFps(); });
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("Annuler", null).show();
    }

    private void deleteFp(long id, String bssid) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer").setMessage("Supprimer " + bssid + " ?")
                .setPositiveButton("Supprimer", (d, w) ->
                        apiCall("DELETE", BASE_URL + "/fingerprints/" + id, null,
                                () -> { toast("✅ Supprimé"); loadFps(); loadStats(); }))
                .setNegativeButton("Annuler", null).show();
    }

    // =========================================================
    // WIZARD AJOUT BLOC
    // =========================================================
    private void showWizardStep1() {
        LinearLayout layout = dialog();
        EditText etNom  = input("Nom du bloc (ex: Bloc 5)", false, false);
        EditText etCode = input("Code (ex: B5)", false, false);
        EditText etDesc = input("Description", false, false);
        CheckBox cbPmr  = check("Accessible PMR");
        layout.addView(label("Nom *"));  layout.addView(etNom);
        layout.addView(label("Code *")); layout.addView(etCode);
        layout.addView(label("Description")); layout.addView(etDesc);
        layout.addView(cbPmr);

        new AlertDialog.Builder(this)
                .setTitle("➕ Nouveau Bloc — 1/3")
                .setView(layout)
                .setPositiveButton("Suivant →", (d, w) -> {
                    String nom  = etNom.getText().toString().trim();
                    String code = etCode.getText().toString().trim();
                    if (nom.isEmpty() || code.isEmpty()) { toast("Nom et code requis"); return; }
                    try {
                        JSONObject body = new JSONObject();
                        body.put("nom", nom); body.put("code", code);
                        body.put("description", etDesc.getText().toString().trim());
                        body.put("accessiblePmr", cbPmr.isChecked());
                        apiCallWithResult("POST", BASE_URL + "/blocs", body, result -> {
                            long blocId = result.optLong("id", -1);
                            if (blocId != -1)
                                new Handler(Looper.getMainLooper()).postDelayed(
                                        () -> showWizardStep2(blocId, nom), 300);
                            else toast("Erreur création bloc");
                        });
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("Annuler", null).show();
    }

    private void showWizardStep2(long blocId, String blocNom) {
        LinearLayout layout = dialog();
        EditText etNb = input("Nombre d'étages (ex: 2)", false, true);
        layout.addView(label("Combien d'étages pour " + blocNom + " ?"));
        layout.addView(etNb);

        new AlertDialog.Builder(this)
                .setTitle("➕ Nouveau Bloc — 2/3")
                .setView(layout)
                .setPositiveButton("Suivant →", (d, w) -> {
                    String val = etNb.getText().toString().trim();
                    if (val.isEmpty()) { toast("Requis"); return; }
                    int nb = Integer.parseInt(val);
                    new Handler(Looper.getMainLooper()).postDelayed(
                            () -> showWizardStep3(blocId, blocNom, nb, 0), 300);
                })
                .setNegativeButton("Annuler", null).show();
    }

    private void showWizardStep3(long blocId, String blocNom, int total, int idx) {
        if (idx >= total) {
            toast("✅ Bloc " + blocNom + " créé !");
            loadBlocs(); loadSalles(); loadStats(); return;
        }
        String defLabel = idx == 0 ? "Rez-de-chaussée" : idx == 1 ? "1er étage" : idx + "ème étage";
        LinearLayout layout = dialog();
        EditText etLabel = input("Label (ex: " + defLabel + ")", false, false);
        etLabel.setText(defLabel);
        CheckBox cbPmr  = check("Accessible PMR");
        EditText etNbS  = input("Nombre de salles", false, true);
        layout.addView(label("Étage " + (idx+1) + "/" + total));
        layout.addView(etLabel); layout.addView(cbPmr);
        layout.addView(label("Salles *")); layout.addView(etNbS);

        new AlertDialog.Builder(this)
                .setTitle("🏢 Étage " + (idx+1) + "/" + total)
                .setView(layout)
                .setPositiveButton("Suivant →", (d, w) -> {
                    String lbl = etLabel.getText().toString().trim();
                    String nb  = etNbS.getText().toString().trim();
                    if (lbl.isEmpty() || nb.isEmpty()) { toast("Requis"); return; }
                    try {
                        JSONObject body = new JSONObject();
                        body.put("numero", idx); body.put("label", lbl);
                        body.put("accessiblePmr", cbPmr.isChecked()); body.put("blocId", blocId);
                        apiCallWithResult("POST", BASE_URL + "/etages", body, result -> {
                            long etageId = result.optLong("id", -1);
                            if (etageId != -1)
                                new Handler(Looper.getMainLooper()).postDelayed(
                                        () -> showWizardStep4(etageId, lbl, blocId, blocNom,
                                                Integer.parseInt(nb), 0, total, idx), 300);
                            else toast("Erreur étage");
                        });
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("Annuler", null).show();
    }

    private void showWizardStep4(long etageId, String etageLabel, long blocId,
                                 String blocNom, int totalS, int idxS,
                                 int totalE, int idxE) {
        if (idxS >= totalS) {
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> showWizardStep3(blocId, blocNom, totalE, idxE + 1), 300);
            return;
        }
        LinearLayout layout = dialog();
        EditText etNom = input("Nom (ex: Salle 301)", false, false);
        CheckBox cbEtude = check("Salle d'étude"); cbEtude.setChecked(true);
        CheckBox cbPmr   = check("Accessible PMR");
        layout.addView(label("Salle " + (idxS+1) + "/" + totalS + " — " + etageLabel));
        layout.addView(etNom); layout.addView(cbEtude); layout.addView(cbPmr);

        new AlertDialog.Builder(this)
                .setTitle("🚪 Salle " + (idxS+1) + "/" + totalS)
                .setView(layout)
                .setPositiveButton("Suivant →", (d, w) -> {
                    String nom = etNom.getText().toString().trim();
                    if (nom.isEmpty()) { toast("Nom requis"); return; }
                    try {
                        JSONObject body = new JSONObject();
                        body.put("nom", nom);
                        body.put("categorie", cbEtude.isChecked() ? "SALLE" : "AUTRE");
                        body.put("estSalleEtude", cbEtude.isChecked());
                        body.put("accessiblePmr", cbPmr.isChecked());
                        body.put("ordreDepuisEntree", idxS + 1);
                        body.put("entreeReference", "");
                        body.put("etageId", etageId);
                        new Handler(Looper.getMainLooper()).postDelayed(() ->
                                apiCall("POST", BASE_URL + "/salles", body, () ->
                                        showWizardStep4(etageId, etageLabel, blocId, blocNom,
                                                totalS, idxS + 1, totalE, idxE)), 300);
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("Annuler", null).show();
    }

    // =========================================================
    // GRAPHIQUES — Canvas Views
    // =========================================================
    private void drawBar(LinearLayout container, List<String> labels,
                         List<Float> values, List<Integer> colors, int defColor) {
        container.removeAllViews();
        if (values.isEmpty()) { showEmptyChart(container, "Pas de données"); return; }
        AdminCharts.BarChartView chart =
                new AdminCharts.BarChartView(this, labels, values, colors, defColor);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(200));
        chart.setLayoutParams(lp);
        chart.setOnItemClickListener((idx, label, value) ->
                new AlertDialog.Builder(this)
                        .setTitle("📊 " + label)
                        .setMessage("Valeur : " + (int)value)
                        .setPositiveButton("OK", null).show());
        container.addView(chart);
    }

    private void drawPie(LinearLayout container, List<String> labels,
                         List<Float> values, List<Integer> colors) {
        container.removeAllViews();
        if (values.isEmpty()) { showEmptyChart(container, "Pas de données"); return; }
        AdminCharts.PieChartView chart =
                new AdminCharts.PieChartView(this, labels, values, colors);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(260));
        chart.setLayoutParams(lp);
        chart.setOnItemClickListener((idx, label, value) -> {
            float total = 0; for (float v : values) total += v;
            int pct = total > 0 ? Math.round(value * 100 / total) : 0;
            new AlertDialog.Builder(this)
                    .setTitle("🏛️ " + label)
                    .setMessage("Nombre : " + (int)value +
                            "Pourcentage : " + pct + "%")
                    .setPositiveButton("OK", null).show();
        });
        container.addView(chart);
    }

    private void showEmptyChart(LinearLayout container, String msg) {
        container.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText(msg); tv.setTextColor(0xFF607080); tv.setTextSize(13f);
        tv.setPadding(dp(8), dp(24), dp(8), dp(24));
        container.addView(tv);
    }

    // =========================================================
    // API HELPERS
    // =========================================================
    interface SimpleCallback { void onDone(); }
    interface ResultCallback { void onResult(JSONObject result); }
    interface ArrayCallback  { void onResult(JSONArray array); }

    private void apiCall(String method, String urlStr, JSONObject body, SimpleCallback cb) {
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(method);
                conn.setRequestProperty("Authorization", "Bearer " + adminToken);
                conn.setConnectTimeout(10000);
                if (body != null) {
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.getOutputStream().write(body.toString().getBytes("UTF-8"));
                }
                conn.getResponseCode();
                runOnUiThread(cb::onDone);
            } catch (Exception e) { runOnUiThread(() -> toast("Erreur réseau")); }
        }).start();
    }

    private void apiCallWithResult(String method, String urlStr, JSONObject body, ResultCallback cb) {
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(method);
                conn.setRequestProperty("Authorization", "Bearer " + adminToken);
                conn.setConnectTimeout(10000);
                if (body != null) {
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.getOutputStream().write(body.toString().getBytes("UTF-8"));
                }
                int status = conn.getResponseCode();
                if (status == 200 || status == 201) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder(); String line;
                    while ((line = br.readLine()) != null) sb.append(line); br.close();
                    JSONObject result = new JSONObject(sb.toString());
                    runOnUiThread(() -> cb.onResult(result));
                }
            } catch (Exception e) { runOnUiThread(() -> toast("Erreur réseau")); }
        }).start();
    }

    private void fetchObj(String urlStr, ResultCallback cb) {
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + adminToken);
                conn.setConnectTimeout(10000);
                if (conn.getResponseCode() != 200) return;
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = br.readLine()) != null) sb.append(line); br.close();
                JSONObject result = new JSONObject(sb.toString());
                runOnUiThread(() -> cb.onResult(result));
            } catch (Exception ignored) {}
        }).start();
    }

    private void fetchArr(String urlStr, ArrayCallback cb) {
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + adminToken);
                conn.setConnectTimeout(10000);
                if (conn.getResponseCode() != 200) return;
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = br.readLine()) != null) sb.append(line); br.close();
                JSONArray result = new JSONArray(sb.toString());
                runOnUiThread(() -> cb.onResult(result));
            } catch (Exception ignored) {}
        }).start();
    }

    // =========================================================
    // UI BUILDER HELPERS
    // =========================================================
    private TextView sectionTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text); tv.setTextColor(0xFF00D4FF);
        tv.setTextSize(15f); tv.setTypeface(null, android.graphics.Typeface.BOLD);
        lp(tv, -1, -2, 0, dp(28), 0, dp(6));
        return tv;
    }

    private LinearLayout sectionHeader(String text, Runnable onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(0x22FFFFFF);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        lp(row, -1, -2, 0, dp(20), 0, 0);
        row.setClickable(true); row.setFocusable(true);
        row.setOnClickListener(v -> onClick.run());

        TextView tv = new TextView(this);
        tv.setText(text); tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(15f); tv.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        tv.setLayoutParams(lp);
        row.addView(tv);

        TextView arrow = new TextView(this);
        arrow.setText("▼"); arrow.setTextColor(0xFF00D4FF); arrow.setTextSize(12f);
        row.addView(arrow);
        return row;
    }

    private TextView legendText(String text) {
        TextView tv = new TextView(this);
        tv.setText(text); tv.setTextColor(0xFF607080); tv.setTextSize(12f);
        lp(tv, -1, -2, 0, 0, 0, dp(6));
        return tv;
    }

    private LinearLayout makeRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        lp(row, -1, -2, 0, dp(8), 0, 0);
        return row;
    }

    private TextView addCard(LinearLayout parent, String lbl, String val, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0x22000000);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(dp(3), dp(3), dp(3), dp(3)); card.setLayoutParams(lp);

        View bar = new View(this); bar.setBackgroundColor(color);
        bar.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(3)));
        card.addView(bar);

        TextView tvVal = new TextView(this);
        tvVal.setText(val); tvVal.setTextColor(color);
        tvVal.setTextSize(26f); tvVal.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams vLP = new LinearLayout.LayoutParams(-2, -2);
        vLP.setMargins(0, dp(8), 0, dp(4)); tvVal.setLayoutParams(vLP);
        card.addView(tvVal);

        TextView tvLbl = new TextView(this);
        tvLbl.setText(lbl); tvLbl.setTextColor(0xFF90A4AE); tvLbl.setTextSize(11f);
        card.addView(tvLbl);

        parent.addView(card);
        return tvVal;
    }

    private LinearLayout chartBox(LinearLayout parent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(0x22000000);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        lp(box, -1, -2, 0, dp(4), 0, dp(20));
        TextView loading = new TextView(this);
        loading.setText("Chargement..."); loading.setTextColor(0xFF607080);
        loading.setTextSize(13f); loading.setPadding(0, dp(12), 0, dp(12));
        box.addView(loading);
        parent.addView(box);
        return box;
    }

    private LinearLayout makeSearchRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        lp(row, -1, -2, 0, dp(8), 0, dp(4));

        EditText et = new EditText(this);
        et.setHint("🔍 Rechercher..."); et.setHintTextColor(0xFF607080);
        et.setTextColor(0xFFFFFFFF); et.setBackgroundResource(R.drawable.bg_glass_input);
        et.setPadding(dp(16), dp(10), dp(16), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(0, 0, dp(8), 0); et.setLayoutParams(lp);
        row.addView(et);
        parent.addView(row);
        return row;
    }

    private void addActionButton(LinearLayout row, String text, Runnable action) {
        TextView btn = new TextView(this);
        btn.setText(text); btn.setTextColor(0xFF1A1A2E);
        btn.setBackgroundResource(R.drawable.bg_button_cyan);
        btn.setTextSize(13f); btn.setPadding(dp(14), dp(10), dp(14), dp(10));
        btn.setClickable(true); btn.setFocusable(true);
        btn.setOnClickListener(v -> action.run());
        row.addView(btn);
    }

    private View makeListItem(String title, String subtitle, String initial,
                              int color, Runnable onEdit, Runnable onDelete) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(0x11FFFFFF);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        lp(row, -1, -2, 0, 0, 0, dp(2));

        // Avatar
        TextView tvInit = new TextView(this);
        tvInit.setText(initial); tvInit.setTextColor(color);
        tvInit.setTextSize(16f); tvInit.setTypeface(null, android.graphics.Typeface.BOLD);
        tvInit.setGravity(android.view.Gravity.CENTER);
        tvInit.setBackgroundColor(color & 0x33FFFFFF);
        LinearLayout.LayoutParams lpA = new LinearLayout.LayoutParams(dp(40), dp(40));
        lpA.setMargins(0, 0, dp(12), 0); tvInit.setLayoutParams(lpA);
        row.addView(tvInit);

        // Textes
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title); tvTitle.setTextColor(0xFFFFFFFF); tvTitle.setTextSize(14f);
        col.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(subtitle); tvSub.setTextColor(0xFF90A4AE); tvSub.setTextSize(12f);
        col.addView(tvSub);
        row.addView(col);

        // Boutons
        TextView btnEdit = new TextView(this);
        btnEdit.setText("✏️"); btnEdit.setTextSize(18f); btnEdit.setPadding(dp(8), 0, dp(8), 0);
        btnEdit.setClickable(true); btnEdit.setFocusable(true);
        btnEdit.setOnClickListener(v -> onEdit.run());
        row.addView(btnEdit);

        TextView btnDel = new TextView(this);
        btnDel.setText("🗑️"); btnDel.setTextSize(18f); btnDel.setPadding(dp(8), 0, 0, 0);
        btnDel.setClickable(true); btnDel.setFocusable(true);
        btnDel.setOnClickListener(v -> onDelete.run());
        row.addView(btnDel);

        return row;
    }

    private void addEmpty(LinearLayout parent, String msg) {
        TextView tv = new TextView(this);
        tv.setText(msg); tv.setTextColor(0xFF607080); tv.setTextSize(13f);
        tv.setPadding(dp(8), dp(16), dp(8), dp(16));
        parent.addView(tv);
    }

    private void addMoreButton(LinearLayout parent, String text, Runnable action) {
        TextView btn = new TextView(this);
        btn.setText(text + " →"); btn.setTextColor(0xFF00D4FF);
        btn.setTextSize(13f); btn.setPadding(dp(8), dp(12), dp(8), dp(12));
        btn.setClickable(true); btn.setFocusable(true);
        btn.setOnClickListener(v -> action.run());
        parent.addView(btn);
    }

    // =========================================================
    // DIALOG / INPUT HELPERS
    // =========================================================
    private LinearLayout dialog() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(48), dp(24), dp(48), dp(8));
        l.setBackgroundColor(0xFF1A1A2E);
        return l;
    }

    private EditText input(String hint, boolean password, boolean numeric) {
        EditText et = new EditText(this);
        et.setHint(hint); et.setHintTextColor(0xFF607080);
        et.setTextColor(0xFFFFFFFF);
        et.setBackgroundResource(R.drawable.bg_glass_input);
        et.setPadding(dp(16), dp(12), dp(16), dp(12));
        if (password) et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        else if (numeric) et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(6), 0, dp(12)); et.setLayoutParams(lp);
        return et;
    }

    private CheckBox check(String text) {
        CheckBox cb = new CheckBox(this);
        cb.setText(text); cb.setTextColor(0xFFB0BEC5);
        lp(cb, -1, -2, 0, dp(4), 0, dp(8));
        return cb;
    }

    private TextView label(String text) {
        TextView tv = new TextView(this);
        tv.setText(text); tv.setTextColor(0xFFB0BEC5); tv.setTextSize(12f);
        lp(tv, -1, -2, 0, dp(4), 0, dp(2));
        return tv;
    }

    private void lp(View v, int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
        lp.setMargins(l, t, r, b); v.setLayoutParams(lp);
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }

    private void toast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Déconnexion").setMessage("Quitter le dashboard ?")
                .setPositiveButton("Oui", (d, w) -> finish())
                .setNegativeButton("Annuler", null).show();
    }

    // =========================================================
    // SIMPLE TEXT WATCHER
    // =========================================================
    private static class SimpleWatcher implements TextWatcher {
        interface OnChange { void onChange(String q); }
        private final OnChange cb;
        SimpleWatcher(OnChange cb) { this.cb = cb; }
        public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        public void afterTextChanged(Editable s) {}
        public void onTextChanged(CharSequence s, int st, int b, int c) { cb.onChange(s.toString()); }
    }
}