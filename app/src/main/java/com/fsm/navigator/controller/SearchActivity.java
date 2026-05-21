package com.fsm.navigator.controller;

import com.fsm.navigator.R;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fsm.navigator.AppConfig;
import com.fsm.navigator.adapter.SearchResultAdapter;
import com.fsm.navigator.auth.PmrDialogHelper;
import com.fsm.navigator.auth.PmrManager;
import com.fsm.navigator.auth.TtsManager;
import com.fsm.navigator.model.PointInteret;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends BaseDrawerActivity
        implements SearchResultAdapter.OnNavigateClickListener {

    private static final String BASE_URL = AppConfig.BASE_URL;

    private EditText     etSearch;
    private ImageButton  btnClear;
    private LinearLayout layoutHistory, layoutResults, layoutEmpty;
    private TextView     tvResultCount, tvClearHistory;
    private RecyclerView recyclerResults;
    private ProgressBar  progressSearch;

    private TextView filterAll, filterAmphi, filterAdmin,
            filterLabo, filterBiblio, filterDept, filterSalle;

    private LinearLayout historyItem1, historyItem2, historyItem3;

    private List<PointInteret> allPoi      = new ArrayList<>();
    private List<PointInteret> filteredPoi = new ArrayList<>();
    private SearchResultAdapter adapter;
    private String activeFilter = "Tous";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initViews();
        setupDrawer();
        setupHamburger(R.id.btnHamburger);
        setupRecyclerView();
        setupSearchBar();
        setupFilters();
        setupHistory();
        loadFromBackend();

        // Gérer filtre/query reçus depuis MainActivity
        String filter = getIntent().getStringExtra("FILTER");
        String query  = getIntent().getStringExtra("QUERY");
        if (query != null && !query.isEmpty()) {
            etSearch.setText(query);
        }
        if (filter != null && !filter.isEmpty()) {
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    applyFilter(filter, getFilterViewByName(filter)), 1500);
        }
    }

    // =========================================================
    private void loadFromBackend() {
        progressSearch.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/api/blocs");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                if (conn.getResponseCode() != 200) {
                    runOnUiThread(() -> {
                        progressSearch.setVisibility(View.GONE);
                        Toast.makeText(getApplicationContext(), "Impossible de charger les données", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                List<PointInteret> pois = parseBlocs(sb.toString());

                runOnUiThread(() -> {
                    progressSearch.setVisibility(View.GONE);
                    allPoi      = pois;
                    filteredPoi = new ArrayList<>(allPoi);
                    adapter.updateList(filteredPoi);
                    showHistory();
                    announceResultsIfVisuallyImpaired(filteredPoi, null);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressSearch.setVisibility(View.GONE);
                    Toast.makeText(this.getApplicationContext(), "Erreur connexion serveur", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // =========================================================
    private List<PointInteret> parseBlocs(String json) throws Exception {
        List<PointInteret> list = new ArrayList<>();
        JSONArray blocs = new JSONArray(json);
        int idCounter = 1;

        for (int i = 0; i < blocs.length(); i++) {
            JSONObject bloc    = blocs.getJSONObject(i);
            String     blocNom = bloc.optString("nom", "");
            String     blocCode= bloc.optString("code", "");
            JSONArray  etages  = bloc.optJSONArray("etages");
            if (etages == null) continue;

            for (int j = 0; j < etages.length(); j++) {
                JSONObject etage      = etages.getJSONObject(j);
                String     etageLabel = etage.optString("label", "RDC");
                JSONArray  salles     = etage.optJSONArray("salles");
                if (salles == null) continue;

                for (int k = 0; k < salles.length(); k++) {
                    JSONObject salle    = salles.getJSONObject(k);
                    long       salleId  = salle.optLong("id", idCounter);
                    String     salleNom = salle.optString("nom", "");
                    String     categorie= salle.optString("categorie", "Salle");
                    boolean    pmr      = salle.optBoolean("accessiblePmr", false);

                    PointInteret poi = new PointInteret(
                            (int) salleId, salleNom,
                            mapCategorie(categorie, salleNom),
                            blocNom, etageLabel);
                    poi.setBlocId(blocCode);
                    poi.setAccessiblePmr(pmr);
                    list.add(poi);
                    idCounter++;
                }
            }
        }
        return list;
    }

    private String mapCategorie(String categorie, String nom) {
        String c = categorie.toUpperCase(), n = nom.toUpperCase();
        if (c.contains("AMPHI") || n.contains("AMPHI"))                       return "Amphithéâtres";
        if (c.contains("ADMIN") || n.contains("SCOLAR") || n.contains("DIRECTION")) return "Administration";
        if (c.contains("LABO")  || c.contains("TP"))                          return "Laboratoires";
        if (c.contains("BIBLIO")|| n.contains("BIBLIO"))                      return "Bibliothèque";
        if (c.contains("DEPT")  || n.contains("DÉPARTEMENT") || n.contains("DEPARTEMENT")) return "Départements";
        return "Salles";
    }

    // =========================================================
    private void initViews() {
        etSearch        = findViewById(R.id.etSearch);
        btnClear        = findViewById(R.id.btnClear);
        layoutHistory   = findViewById(R.id.layoutHistory);
        layoutResults   = findViewById(R.id.layoutResults);
        layoutEmpty     = findViewById(R.id.layoutEmpty);
        tvResultCount   = findViewById(R.id.tvResultCount);
        tvClearHistory  = findViewById(R.id.tvClearHistory);
        recyclerResults = findViewById(R.id.recyclerResults);
        progressSearch  = findViewById(R.id.progressSearch);

        filterAll    = findViewById(R.id.filterAll);
        filterAmphi  = findViewById(R.id.filterAmphi);
        filterAdmin  = findViewById(R.id.filterAdmin);
        filterLabo   = findViewById(R.id.filterLabo);
        filterBiblio = findViewById(R.id.filterBiblio);
        filterDept   = findViewById(R.id.filterDept);
        filterSalle  = findViewById(R.id.filterSalle);

        historyItem1 = findViewById(R.id.historyItem1);
        historyItem2 = findViewById(R.id.historyItem2);
        historyItem3 = findViewById(R.id.historyItem3);
    }

    private void setupRecyclerView() {
        adapter = new SearchResultAdapter(this, filteredPoi, this);
        recyclerResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerResults.setAdapter(adapter);
    }

    // =========================================================
    private void setupSearchBar() {
        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void afterTextChanged(Editable s) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                String query = s.toString().trim();
                if (btnClear != null)
                    btnClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                if (query.isEmpty()) showHistory();
                else filterResults(query);
            }
        });

        if (btnClear != null) btnClear.setOnClickListener(v -> {
            etSearch.setText("");
            showHistory();
        });
    }

    // =========================================================
    private void setupFilters() {
        if (filterAll    != null) filterAll.setOnClickListener(v    -> applyFilter("Tous",           filterAll));
        if (filterAmphi  != null) filterAmphi.setOnClickListener(v  -> applyFilter("Amphithéâtres",  filterAmphi));
        if (filterAdmin  != null) filterAdmin.setOnClickListener(v  -> applyFilter("Administration",  filterAdmin));
        if (filterLabo   != null) filterLabo.setOnClickListener(v   -> applyFilter("Laboratoires",    filterLabo));
        if (filterBiblio != null) filterBiblio.setOnClickListener(v -> applyFilter("Bibliothèque",    filterBiblio));
        if (filterDept   != null) filterDept.setOnClickListener(v   -> applyFilter("Départements",    filterDept));
        if (filterSalle  != null) filterSalle.setOnClickListener(v  -> applyFilter("Salles",          filterSalle));
    }

    private void applyFilter(String category, TextView selected) {
        activeFilter = category;
        resetFilterStyles();
        if (selected != null) {
            selected.setBackgroundResource(R.drawable.bg_filter_selected);
            selected.setTextColor(getResources().getColor(R.color.accent_cyan, null));
        }
        filterResults(etSearch.getText().toString().trim());
    }

    private void resetFilterStyles() {
        for (TextView f : new TextView[]{filterAll, filterAmphi, filterAdmin,
                filterLabo, filterBiblio, filterDept, filterSalle}) {
            if (f != null) {
                f.setBackgroundResource(R.drawable.bg_filter_unselected);
                f.setTextColor(getResources().getColor(R.color.text_secondary, null));
            }
        }
    }

    private TextView getFilterViewByName(String name) {
        switch (name) {
            case "Amphithéâtres":  return filterAmphi;
            case "Administration": return filterAdmin;
            case "Laboratoires":   return filterLabo;
            case "Bibliothèque":   return filterBiblio;
            case "Départements":   return filterDept;
            case "Salles":         return filterSalle;
            default:               return filterAll;
        }
    }

    // =========================================================
    private void filterResults(String query) {
        List<PointInteret> results = new ArrayList<>();
        for (PointInteret poi : allPoi) {
            boolean matchQuery = query.isEmpty()
                    || poi.getNom().toLowerCase().contains(query.toLowerCase())
                    || poi.getCategorie().toLowerCase().contains(query.toLowerCase())
                    || poi.getBatiment().toLowerCase().contains(query.toLowerCase());
            boolean matchCat = activeFilter.equals("Tous")
                    || poi.getCategorie().equals(activeFilter);
            if (matchQuery && matchCat) results.add(poi);
        }

        filteredPoi = results;
        adapter.updateList(filteredPoi);

        int count = filteredPoi.size();
        tvResultCount.setText(count + (count > 1 ? " résultats" : " résultat"));
        announceResultsIfVisuallyImpaired(filteredPoi, query.isEmpty() ? activeFilter : query);

        layoutHistory.setVisibility(View.GONE);
        if (count == 0) {
            layoutResults.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            layoutResults.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    // =========================================================
    private void setupHistory() {
        if (historyItem1 != null) historyItem1.setOnClickListener(v -> etSearch.setText("Amphithéâtre"));
        if (historyItem2 != null) historyItem2.setOnClickListener(v -> etSearch.setText("Salle 301"));
        if (historyItem3 != null) historyItem3.setOnClickListener(v -> etSearch.setText("Bibliothèque"));
        if (tvClearHistory != null) tvClearHistory.setOnClickListener(v -> {
            ProfileActivity.clearHistory(this);
            layoutHistory.setVisibility(View.GONE);
            Toast.makeText(getApplicationContext(), "Historique effacé", Toast.LENGTH_SHORT).show();
        });
    }

    private void showHistory() {
        layoutHistory.setVisibility(View.VISIBLE);
        layoutResults.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
    }

    // =========================================================
    private void announceResultsIfVisuallyImpaired(List<PointInteret> results, String context) {
        if (PmrManager.getProfile() != PmrManager.PmrProfile.VISUALLY_IMPAIRED) return;
        int count = results.size();
        if (count == 0) {
            TtsManager.speak("Aucun résultat trouvé.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        if (context != null && !context.equals("Tous")) {
            sb.append(context).append(". ");
        }
        sb.append(count).append(count > 1 ? " résultats. " : " résultat. ");
        int max = Math.min(3, count);
        for (int i = 0; i < max; i++) {
            sb.append(results.get(i).getNom())
              .append(", ").append(results.get(i).getBatiment()).append(". ");
        }
        if (count > 3) sb.append("Et ").append(count - 3).append(" autres.");
        TtsManager.speak(sb.toString());
    }

    @Override
    public void onNavigateClick(PointInteret poi) {
        // Bloquer si PMR actif et salle non accessible
        boolean blocked = PmrDialogHelper.checkAndShow(this, poi, () -> launchNav(poi));
        if (blocked) return;
        launchNav(poi);
    }

    private void launchNav(PointInteret poi) {
        TtsManager.speak("Navigation vers " + poi.getNom()
                + ", " + poi.getBatiment() + ", " + poi.getEtage() + ".");
        ProfileActivity.addToHistory(this, poi.getNom());
        String nodeId = buildNodeId(poi);
        String blocId = poi.getBlocId() != null ? poi.getBlocId() : "B3";
        if (nodeId.startsWith("A16_"))   blocId = "A1-6";
        if (nodeId.startsWith("BMATH_")) blocId = "BMATH";
        if (nodeId.startsWith("BP_"))    blocId = "BPAL";
        Intent intent = new Intent(this, NavigationActivity.class);
        intent.putExtra("TARGET_NODE_ID",  nodeId);
        intent.putExtra("TARGET_NOM",      poi.getNom());
        intent.putExtra("TARGET_BLOC_ID",  blocId);
        startActivity(intent);
    }


    private String buildNodeId(PointInteret poi) {
        String blocId   = poi.getBlocId() != null ? poi.getBlocId() : "B3";
        String batiment = poi.getBatiment();

        // Amphis 1→6
        boolean isA16 = "COUR".equals(blocId) || "A1-6".equals(blocId)
                || (batiment != null && batiment.toLowerCase().contains("a1-6"));
        if (isA16 && poi.getNom().toLowerCase().contains("amphi")) {
            String num = poi.getNom().replaceAll("[^0-9]", "");
            if (!num.isEmpty()) return "A16_AMPHI_" + num;
        }

        boolean isPalestine = "B1".equals(blocId) || "BPAL".equals(blocId)
                || (batiment != null && batiment.toLowerCase().contains("palestine"));
        if (isPalestine) {
            String num = poi.getNom().replaceAll("[^0-9]", "");
            if (!num.isEmpty()) return "BP_" + num;
            String n = poi.getNom().toUpperCase().trim();
            if (n.contains("AMPHI")) {
                if (n.endsWith(" A") || n.equals("AMPHI A")) return "BP_AA";
                if (n.endsWith(" B") || n.equals("AMPHI B")) return "BP_AB";
                if (n.endsWith(" C") || n.equals("AMPHI C")) return "BP_AC";
                if (n.endsWith(" D") || n.equals("AMPHI D")) return "BP_AD";
            }
            return "BPAL_RDC_ENTREE";
        }

        boolean isBmath = "BMATH".equals(blocId)
                || (batiment != null && batiment.toLowerCase().contains("math"));
        if (isBmath) {
            String n = poi.getNom().toUpperCase().trim();
            if (n.contains("101M") || n.equals("SALLE 101M")) return "BMATH_101M";
            if (n.contains("102M") || n.equals("SALLE 102M")) return "BMATH_102M";
            if (n.contains("117M") || n.equals("SALLE 117M")) return "BMATH_117M";
            if (n.contains("BUREAU")) return "BMATH_BUREAU_G1";
            return "BMATH_ENTREE";
        }

        String etage     = poi.getEtage();
        String etageCode = "RDC";
        if (etage != null && etage.contains("1er")) etageCode = "E1";
        else if (etage != null && etage.contains("2")) etageCode = "E2";
        String nom = poi.getNom().replaceAll("[^0-9]", "");
        if (nom.isEmpty()) nom = poi.getNom().replace(" ", "_");
        return blocId + "_" + etageCode + "_" + nom;
    }
}
