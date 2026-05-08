package com.fsm.navigator.admin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.fsm.navigator.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DashboardStatsFragment extends Fragment {

    private static final String BASE_URL = "http://10.0.2.2:8080/api/admin/stats";
    private String adminToken;

    // Cards
    private TextView tvBlocs, tvSalles, tvUsers, tvFps, tvNav, tvViews;

    // Chart containers
    private LinearLayout containerWifi, containerRssi, containerTypes,
            containerTopNav, containerTopViewed,
            containerActivity, containerUncovered;
    private TextView tvUncoveredList;

    public static DashboardStatsFragment newInstance(String token) {
        DashboardStatsFragment f = new DashboardStatsFragment();
        Bundle b = new Bundle(); b.putString("token", token); f.setArguments(b); return f;
    }

    @Override public void onCreate(Bundle s) {
        super.onCreate(s);
        if (getArguments() != null) adminToken = getArguments().getString("token");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle s) {
        ScrollView scroll = new ScrollView(getContext());
        scroll.setBackgroundColor(0xFF1A1A2E);

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 48);
        scroll.addView(root);

        // ===== TITRE =====
        root.addView(makeTitle("📊 Tableau de Bord"));

        // ===== CARDS =====
        root.addView(makeSubtitle("Vue d'ensemble"));
        LinearLayout cardsRow1 = makeRow();
        tvBlocs  = makeCard(cardsRow1, "Blocs",        "...", 0xFF00D4FF);
        tvSalles = makeCard(cardsRow1, "Salles",       "...", 0xFF7B2FBE);
        tvUsers  = makeCard(cardsRow1, "Utilisateurs", "...", 0xFF00B894);
        root.addView(cardsRow1);

        LinearLayout cardsRow2 = makeRow();
        tvFps    = makeCard(cardsRow2, "Fingerprints", "...", 0xFFE17055);
        tvNav    = makeCard(cardsRow2, "Navigations",  "...", 0xFF0984E3);
        tvViews  = makeCard(cardsRow2, "Consultations","...", 0xFFF39C12);
        root.addView(cardsRow2);

        // ===== COUVERTURE WIFI =====
        root.addView(makeSubtitle("📶 Couverture WiFi par Bloc"));
        root.addView(makeLegend("Nombre de fingerprints par bloc"));
        containerWifi = makeChartContainer(root, 220);

        // ===== RSSI DISTRIBUTION =====
        root.addView(makeSubtitle("📡 Distribution RSSI"));
        root.addView(makeLegend("Qualité du signal — idéal entre -50 et -70 dBm"));
        containerRssi = makeChartContainer(root, 200);

        // ===== TYPES DE SALLES =====
        root.addView(makeSubtitle("🏛️ Types de Salles"));
        containerTypes = makeChartContainer(root, 240);

        // ===== ACTIVITÉ 7 JOURS =====
        root.addView(makeSubtitle("📅 Activité — 7 derniers jours"));
        containerActivity = makeChartContainer(root, 180);

        // ===== TOP SALLES NAVIGUÉES =====
        root.addView(makeSubtitle("🧭 Salles les plus naviguées"));
        containerTopNav = makeChartContainer(root, 200);

        // ===== TOP SALLES CONSULTÉES =====
        root.addView(makeSubtitle("👁️ Salles les plus consultées"));
        containerTopViewed = makeChartContainer(root, 200);

        // ===== SALLES SANS FINGERPRINT =====
        root.addView(makeSubtitle("⚠️ Salles sans couverture WiFi"));
        root.addView(makeLegend("Ces salles ne peuvent pas être localisées"));
        containerUncovered = new LinearLayout(getContext());
        containerUncovered.setOrientation(LinearLayout.VERTICAL);
        containerUncovered.setBackgroundColor(0x33FF6B6B);
        containerUncovered.setPadding(24, 16, 24, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 8, 0, 24); containerUncovered.setLayoutParams(lp);
        tvUncoveredList = new TextView(getContext());
        tvUncoveredList.setTextColor(0xFFFFCDD2); tvUncoveredList.setTextSize(13f);
        tvUncoveredList.setText("Chargement...");
        containerUncovered.addView(tvUncoveredList);
        root.addView(containerUncovered);

        loadAll();
        return scroll;
    }

    // =========================================================
    // CHARGER TOUTES LES STATS
    // =========================================================
    private void loadAll() {
        loadOverview();
        loadWifiCoverage();
        loadRssiDistribution();
        loadSalleTypes();
        loadActivity();
        loadTopNavigated();
        loadTopViewed();
        loadUncovered();
    }

    private void loadOverview() {
        fetch("/overview", result -> {
            tvBlocs.setText(result.optString("totalBlocs", "0"));
            tvSalles.setText(result.optString("totalSalles", "0"));
            tvUsers.setText(result.optString("totalUsers", "0"));
            tvFps.setText(result.optString("totalFingerprints", "0"));
            tvNav.setText(result.optString("totalNavigations", "0"));
            tvViews.setText(result.optString("totalViews", "0"));
        });
    }

    private void loadWifiCoverage() {
        fetchArray("/wifi-coverage", array -> {
            List<String> labels = new ArrayList<>();
            List<Float>  values = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.optJSONObject(i);
                if (o == null) continue;
                labels.add(o.optString("blocCode", "?"));
                values.add((float) o.optLong("fingerprints", 0));
            }
            drawBarChart(containerWifi, labels, values, 0xFF00D4FF);
        });
    }

    private void loadRssiDistribution() {
        fetch("/rssi-distribution", result -> {
            String[] labels = {"<-80", "-80~-70", "-70~-60", "-60~-50", ">-50"};
            int[]    colors = {0xFFE74C3C, 0xFFE67E22, 0xFFF1C40F, 0xFF2ECC71, 0xFF3498DB};
            JSONArray vals  = result.optJSONArray("values");
            if (vals == null) return;
            List<String> lbl = new ArrayList<>();
            List<Float>  val = new ArrayList<>();
            List<Integer>col = new ArrayList<>();
            for (int i = 0; i < vals.length(); i++) {
                lbl.add(labels[i]); val.add((float) vals.optInt(i, 0)); col.add(colors[i]);
            }
            drawColorBarChart(containerRssi, lbl, val, col);
        });
    }

    private void loadSalleTypes() {
        fetch("/salle-types", result -> {
            List<String>  labels = new ArrayList<>();
            List<Float>   values = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            int[] palette = {0xFF00D4FF, 0xFF7B2FBE, 0xFF00B894, 0xFFE17055,
                    0xFF0984E3, 0xFFF39C12, 0xFF6C5CE7};
            int ci = 0;
            Iterator<String> keys = result.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                labels.add(key); values.add((float) result.optLong(key, 0));
                colors.add(palette[ci % palette.length]); ci++;
            }
            drawPieChart(containerTypes, labels, values, colors);
        });
    }

    private void loadActivity() {
        fetchArray("/activity", array -> {
            List<String> labels = new ArrayList<>();
            List<Float>  values = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.optJSONObject(i);
                if (o == null) continue;
                String day = o.optString("day", "");
                if (day.length() >= 10) day = day.substring(5); // MM-DD
                labels.add(day);
                values.add((float) o.optLong("count", 0));
            }
            drawBarChart(containerActivity, labels, values, 0xFF0984E3);
        });
    }

    private void loadTopNavigated() {
        fetchArray("/top-navigated", array -> {
            List<String> labels = new ArrayList<>();
            List<Float>  values = new ArrayList<>();
            for (int i = 0; i < Math.min(array.length(), 7); i++) {
                JSONObject o = array.optJSONObject(i);
                if (o == null) continue;
                labels.add(o.optString("salleNom","?").replace("Salle ","S"));
                values.add((float) o.optLong("count", 0));
            }
            if (labels.isEmpty()) {
                showEmpty(containerTopNav, "Aucune navigation enregistrée");
            } else {
                drawBarChart(containerTopNav, labels, values, 0xFF00B894);
            }
        });
    }

    private void loadTopViewed() {
        fetchArray("/top-viewed", array -> {
            List<String> labels = new ArrayList<>();
            List<Float>  values = new ArrayList<>();
            for (int i = 0; i < Math.min(array.length(), 7); i++) {
                JSONObject o = array.optJSONObject(i);
                if (o == null) continue;
                labels.add(o.optString("salleNom","?").replace("Salle ","S"));
                values.add((float) o.optLong("count", 0));
            }
            if (labels.isEmpty()) {
                showEmpty(containerTopViewed, "Aucune consultation enregistrée");
            } else {
                drawBarChart(containerTopViewed, labels, values, 0xFFF39C12);
            }
        });
    }

    private void loadUncovered() {
        fetchArray("/uncovered-salles", array -> {
            if (array.length() == 0) {
                tvUncoveredList.setText("✅ Toutes les salles ont une couverture WiFi !");
                tvUncoveredList.setTextColor(0xFF00B894);
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("⚠️ ").append(array.length()).append(" salle(s) sans fingerprint :\n\n");
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.optJSONObject(i);
                if (o == null) continue;
                sb.append("• ").append(o.optString("salleNom","?"))
                        .append(" — ").append(o.optString("bloc","?"))
                        .append(" / ").append(o.optString("etage","?"))
                        .append("\n");
            }
            tvUncoveredList.setText(sb.toString());
        });
    }

    // =========================================================
    // DESSIN DES GRAPHIQUES
    // =========================================================

    private void drawBarChart(LinearLayout container, List<String> labels,
                              List<Float> values, int color) {
        container.removeAllViews();
        if (values.isEmpty()) { showEmpty(container, "Pas de données"); return; }

        BarChartView chart = new BarChartView(getContext(), labels, values,
                null, color);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(200));
        chart.setLayoutParams(lp);
        container.addView(chart);
    }

    private void drawColorBarChart(LinearLayout container, List<String> labels,
                                   List<Float> values, List<Integer> colors) {
        container.removeAllViews();
        if (values.isEmpty()) { showEmpty(container, "Pas de données"); return; }

        BarChartView chart = new BarChartView(getContext(), labels, values, colors, 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(200));
        chart.setLayoutParams(lp);
        container.addView(chart);
    }

    private void drawPieChart(LinearLayout container, List<String> labels,
                              List<Float> values, List<Integer> colors) {
        container.removeAllViews();
        if (values.isEmpty()) { showEmpty(container, "Pas de données"); return; }

        PieChartView chart = new PieChartView(getContext(), labels, values, colors);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(260));
        chart.setLayoutParams(lp);
        container.addView(chart);
    }

    private void showEmpty(LinearLayout container, String msg) {
        container.removeAllViews();
        TextView tv = new TextView(getContext());
        tv.setText(msg); tv.setTextColor(0xFF607080); tv.setTextSize(14f);
        tv.setPadding(16, 32, 16, 32);
        container.addView(tv);
    }

    // =========================================================
    // HTTP HELPERS
    // =========================================================
    interface JsonCallback  { void onResult(JSONObject result); }
    interface ArrayCallback { void onResult(JSONArray array); }

    private void fetch(String path, JsonCallback cb) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + adminToken);
                conn.setConnectTimeout(10000);
                if (conn.getResponseCode() != 200) return;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = br.readLine()) != null) sb.append(line); br.close();
                JSONObject result = new JSONObject(sb.toString());
                requireActivity().runOnUiThread(() -> cb.onResult(result));
            } catch (Exception e) { android.util.Log.e("Dashboard","fetch error:"+e.getMessage()); }
        }).start();
    }

    private void fetchArray(String path, ArrayCallback cb) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + adminToken);
                conn.setConnectTimeout(10000);
                if (conn.getResponseCode() != 200) return;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = br.readLine()) != null) sb.append(line); br.close();
                JSONArray result = new JSONArray(sb.toString());
                requireActivity().runOnUiThread(() -> cb.onResult(result));
            } catch (Exception e) { android.util.Log.e("Dashboard","fetchArray error:"+e.getMessage()); }
        }).start();
    }

    // =========================================================
    // UI HELPERS
    // =========================================================
    private TextView makeTitle(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text); tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(20f); tv.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 24); tv.setLayoutParams(lp); return tv;
    }

    private TextView makeSubtitle(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text); tv.setTextColor(0xFF00D4FF);
        tv.setTextSize(15f); tv.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 24, 0, 4); tv.setLayoutParams(lp); return tv;
    }

    private TextView makeLegend(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text); tv.setTextColor(0xFF607080); tv.setTextSize(12f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 8); tv.setLayoutParams(lp); return tv;
    }

    private LinearLayout makeRow() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 8, 0, 0); row.setLayoutParams(lp); return row;
    }

    private TextView makeCard(LinearLayout parent, String label, String value, int color) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0x22000000);
        card.setPadding(20, 20, 20, 20);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(4, 4, 4, 4); card.setLayoutParams(lp);

        // Couleur top
        View bar = new View(getContext());
        bar.setBackgroundColor(color);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(3)));
        card.addView(bar);

        TextView tvVal = new TextView(getContext());
        tvVal.setTextColor(color); tvVal.setTextSize(28f);
        tvVal.setTypeface(null, Typeface.BOLD);
        tvVal.setText(value);
        LinearLayout.LayoutParams tvlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tvlp.setMargins(0, 8, 0, 4); tvVal.setLayoutParams(tvlp);
        card.addView(tvVal);

        TextView tvLbl = new TextView(getContext());
        tvLbl.setText(label); tvLbl.setTextColor(0xFF90A4AE); tvLbl.setTextSize(11f);
        card.addView(tvLbl);

        parent.addView(card);
        return tvVal;
    }

    private LinearLayout makeChartContainer(LinearLayout parent, int heightDp) {
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(0x22000000);
        container.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 8, 0, 8); container.setLayoutParams(lp);
        // Placeholder
        TextView loading = new TextView(getContext());
        loading.setText("Chargement..."); loading.setTextColor(0xFF607080);
        loading.setTextSize(13f); loading.setPadding(0, 16, 0, 16);
        container.addView(loading);
        parent.addView(container);
        return container;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // =========================================================
    // CUSTOM VIEWS — Graphiques
    // =========================================================

    static class BarChartView extends View {
        private final List<String>  labels;
        private final List<Float>   values;
        private final List<Integer> colors;
        private final int           defaultColor;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        BarChartView(Context ctx, List<String> labels, List<Float> values,
                     List<Integer> colors, int defaultColor) {
            super(ctx);
            this.labels       = labels;
            this.values       = values;
            this.colors       = colors;
            this.defaultColor = defaultColor;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (values.isEmpty()) return;
            int w = getWidth(), h = getHeight();
            int padL = 16, padR = 16, padT = 16, padB = 48;
            int chartH = h - padT - padB;
            int chartW = w - padL - padR;

            float max = 0;
            for (float v : values) if (v > max) max = v;
            if (max == 0) max = 1;

            int n = values.size();
            float barW = (float) chartW / n * 0.6f;
            float gap  = (float) chartW / n;

            // Grille
            paint.setColor(0x33FFFFFF); paint.setStrokeWidth(1f);
            for (int i = 0; i <= 4; i++) {
                float y = padT + chartH - (chartH * i / 4f);
                canvas.drawLine(padL, y, w - padR, y, paint);
            }

            for (int i = 0; i < n; i++) {
                float barH = chartH * values.get(i) / max;
                float x    = padL + gap * i + (gap - barW) / 2f;
                float top  = padT + chartH - barH;
                float bot  = padT + chartH;

                int color = (colors != null && i < colors.size())
                        ? colors.get(i) : defaultColor;

                paint.setColor(color);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(new RectF(x, top, x + barW, bot), 8, 8, paint);

                // Valeur
                paint.setColor(0xFFFFFFFF); paint.setTextSize(22f);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(String.valueOf((int) values.get(i).floatValue()),
                        x + barW / 2, top - 6, paint);

                // Label
                paint.setColor(0xFF90A4AE); paint.setTextSize(20f);
                canvas.drawText(labels.get(i), x + barW / 2, h - 8, paint);
            }
        }
    }

    static class PieChartView extends View {
        private final List<String>  labels;
        private final List<Float>   values;
        private final List<Integer> colors;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        PieChartView(Context ctx, List<String> labels, List<Float> values, List<Integer> colors) {
            super(ctx);
            this.labels = labels; this.values = values; this.colors = colors;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (values.isEmpty()) return;
            int w = getWidth(), h = getHeight();
            int size   = Math.min(w, h - 80);
            int cx     = w / 2;
            int cy     = size / 2 + 20;
            int radius = size / 2 - 20;

            float total = 0;
            for (float v : values) total += v;
            if (total == 0) return;

            float startAngle = -90f;
            for (int i = 0; i < values.size(); i++) {
                float sweep = 360f * values.get(i) / total;
                paint.setColor(colors.get(i));
                paint.setStyle(Paint.Style.FILL);
                canvas.drawArc(new RectF(cx - radius, cy - radius, cx + radius, cy + radius),
                        startAngle, sweep, true, paint);
                startAngle += sweep;
            }

            // Trou central
            paint.setColor(0xFF1A1A2E);
            canvas.drawCircle(cx, cy, radius * 0.5f, paint);

            // Légende
            int legendY = size + 20;
            paint.setTextSize(22f); paint.setTextAlign(Paint.Align.LEFT);
            float legendX = 16f;
            for (int i = 0; i < labels.size(); i++) {
                if (legendX + 200 > w) { legendX = 16f; legendY += 32; }
                paint.setColor(colors.get(i)); paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(legendX + 8, legendY, 8, paint);
                paint.setColor(0xFFB0BEC5);
                canvas.drawText(labels.get(i) + " (" + (int)values.get(i).floatValue() + ")",
                        legendX + 22, legendY + 7, paint);
                legendX += 180;
            }
        }
    }
}