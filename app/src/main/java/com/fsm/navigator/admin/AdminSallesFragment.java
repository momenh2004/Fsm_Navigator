package com.fsm.navigator.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fsm.navigator.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AdminSallesFragment extends Fragment {

    private static final String BASE_URL = "http://10.0.2.2:8080/api/admin";
    private String adminToken;

    private RecyclerView recyclerAdmin;
    private TextView     tvCount;
    private View         progressAdmin;

    private List<JSONObject> allSalles      = new ArrayList<>();
    private List<JSONObject> filteredSalles = new ArrayList<>();

    public static AdminSallesFragment newInstance(String token) {
        AdminSallesFragment f = new AdminSallesFragment();
        Bundle b = new Bundle(); b.putString("token", token); f.setArguments(b); return f;
    }

    @Override public void onCreate(Bundle s) {
        super.onCreate(s);
        if (getArguments() != null) adminToken = getArguments().getString("token");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle s) {
        View v = inflater.inflate(R.layout.fragment_admin_salles, container, false);
        recyclerAdmin = v.findViewById(R.id.recyclerAdmin);
        tvCount       = v.findViewById(R.id.tvCount);
        progressAdmin = v.findViewById(R.id.progressAdmin);
        recyclerAdmin.setLayoutManager(new LinearLayoutManager(getContext()));

        // Salles créées via wizard Blocs — bouton ajout caché
        LinearLayout btnAdd = v.findViewById(R.id.btnAdd);
        if (btnAdd != null) btnAdd.setVisibility(View.GONE);

        EditText etSearch = v.findViewById(R.id.etAdminSearch);
        if (etSearch != null) etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { filter(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });

        loadSalles();
        return v;
    }

    private void loadSalles() {
        showProgress(true);
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/salles");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + adminToken);
                conn.setConnectTimeout(10000);
                if (conn.getResponseCode() != 200) {
                    requireActivity().runOnUiThread(() -> showProgress(false)); return;
                }
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = br.readLine()) != null) sb.append(line); br.close();
                JSONArray array = new JSONArray(sb.toString());
                allSalles.clear();
                for (int i = 0; i < array.length(); i++) allSalles.add(array.getJSONObject(i));
                filteredSalles = new ArrayList<>(allSalles);
                requireActivity().runOnUiThread(() -> {
                    showProgress(false); updateRecycler();
                    tvCount.setText(filteredSalles.size() + " salles");
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> { showProgress(false); toast("Erreur chargement"); });
            }
        }).start();
    }

    private void filter(String q) {
        filteredSalles.clear();
        for (JSONObject s : allSalles)
            if (s.optString("nom","").toLowerCase().contains(q.toLowerCase())) filteredSalles.add(s);
        updateRecycler();
        tvCount.setText(filteredSalles.size() + " salles");
    }

    private void updateRecycler() {
        recyclerAdmin.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p, int t) {
                return new RecyclerView.ViewHolder(LayoutInflater.from(getContext())
                        .inflate(R.layout.item_admin_card, p, false)) {};
            }
            public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
                try {
                    JSONObject s  = filteredSalles.get(pos);
                    long id       = s.optLong("id");
                    String nom    = s.optString("nom", "");
                    String cat    = s.optString("categorie", "");
                    boolean pmr   = s.optBoolean("accessiblePmr", false);
                    boolean etude = s.optBoolean("estSalleEtude", true);

                    TextView tvTitle   = h.itemView.findViewById(R.id.tvItemTitle);
                    TextView tvSub     = h.itemView.findViewById(R.id.tvItemSubtitle);
                    TextView tvBadge   = h.itemView.findViewById(R.id.tvItemBadge);
                    TextView tvInitial = h.itemView.findViewById(R.id.tvItemInitial);
                    android.widget.ImageButton btnEdit   = h.itemView.findViewById(R.id.btnEdit);
                    android.widget.ImageButton btnDelete = h.itemView.findViewById(R.id.btnDelete);

                    if (tvTitle   != null) tvTitle.setText(nom);
                    if (tvSub     != null) tvSub.setText(cat + (etude ? " • Étude" : " • Autre"));
                    if (tvInitial != null) tvInitial.setText(nom.isEmpty() ? "S" : String.valueOf(nom.charAt(0)));
                    if (tvBadge   != null) { tvBadge.setVisibility(pmr ? View.VISIBLE : View.GONE); tvBadge.setText("♿ PMR"); }
                    if (btnEdit   != null) btnEdit.setOnClickListener(v -> showEditDialog(s));
                    if (btnDelete != null) btnDelete.setOnClickListener(v -> deleteSalle(id, nom));
                } catch (Exception e) { android.util.Log.e("AdminSalles","Bind:"+e.getMessage()); }
            }
            public int getItemCount() { return filteredSalles.size(); }
        });
    }

    private void showEditDialog(JSONObject existing) {
        LinearLayout layout = new LinearLayout(requireActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);
        layout.setBackgroundColor(0xFF1A1A2E);

        EditText etNom = new EditText(requireActivity());
        etNom.setHint("Nom"); etNom.setHintTextColor(0xFF607080);
        etNom.setTextColor(0xFFFFFFFF);
        etNom.setBackgroundResource(R.drawable.bg_glass_input);
        etNom.setPadding(32, 24, 32, 24);
        etNom.setText(existing.optString("nom", ""));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 8, 0, 16); etNom.setLayoutParams(lp);

        CheckBox cbEtude = new CheckBox(requireActivity());
        cbEtude.setText("Salle d'étude"); cbEtude.setTextColor(0xFFB0BEC5);
        cbEtude.setChecked(existing.optBoolean("estSalleEtude", true));

        CheckBox cbPmr = new CheckBox(requireActivity());
        cbPmr.setText("Accessible PMR"); cbPmr.setTextColor(0xFFB0BEC5);
        cbPmr.setChecked(existing.optBoolean("accessiblePmr", false));

        layout.addView(etNom); layout.addView(cbEtude); layout.addView(cbPmr);

        new AlertDialog.Builder(requireActivity())
                .setTitle("✏️ Modifier la salle")
                .setView(layout)
                .setPositiveButton("Enregistrer", (d, w) -> {
                    try {
                        JSONObject body = new JSONObject();
                        body.put("nom",           etNom.getText().toString().trim());
                        body.put("estSalleEtude", cbEtude.isChecked());
                        body.put("accessiblePmr", cbPmr.isChecked());
                        updateSalle(existing.getLong("id"), body);
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("Annuler", null).show();
    }

    private void updateSalle(long id, JSONObject body) {
        showProgress(true);
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/salles/" + id);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + adminToken);
                conn.setDoOutput(true);
                conn.getOutputStream().write(body.toString().getBytes("UTF-8"));
                conn.getResponseCode();
                requireActivity().runOnUiThread(() -> { showProgress(false); toast("✅ Modifiée"); loadSalles(); });
            } catch (Exception e) { requireActivity().runOnUiThread(() -> showProgress(false)); }
        }).start();
    }

    private void deleteSalle(long id, String nom) {
        new AlertDialog.Builder(requireActivity())
                .setTitle("Supprimer").setMessage("Supprimer " + nom + " ?")
                .setPositiveButton("Supprimer", (d, w) -> {
                    showProgress(true);
                    new Thread(() -> {
                        try {
                            URL url = new URL(BASE_URL + "/salles/" + id);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("DELETE");
                            conn.setRequestProperty("Authorization", "Bearer " + adminToken);
                            conn.getResponseCode();
                            requireActivity().runOnUiThread(() -> { showProgress(false); toast("✅ Supprimée"); loadSalles(); });
                        } catch (Exception e) { requireActivity().runOnUiThread(() -> showProgress(false)); }
                    }).start();
                }).setNegativeButton("Annuler", null).show();
    }

    private void toast(String msg) {
        if (getActivity() != null)
            Toast.makeText(requireActivity().getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void showProgress(boolean show) {
        if (progressAdmin != null)
            progressAdmin.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}