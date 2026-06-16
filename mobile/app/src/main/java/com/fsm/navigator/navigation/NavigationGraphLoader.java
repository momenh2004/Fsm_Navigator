package com.fsm.navigator.navigation;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.fsm.navigator.AppConfig;
import com.fsm.navigator.model.NavigationGraph;
import com.fsm.navigator.model.NavigationNode;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

// Charge le graphe de navigation depuis le réseau ou depuis le cache local.
public class NavigationGraphLoader {

    private static final String TAG       = "GraphLoader";
    private static final String GRAPH_URL = AppConfig.BASE_URL + "/api/navigation/graph";
    private static final String PREF_NAME = "nav_graph_cache";
    private static final String PREF_KEY  = "graph_json";

    // Récupère le graphe depuis le serveur (GET /api/navigation/graph) et le cache.
    public NavigationGraph loadFromNetwork(Context ctx) throws Exception {
        URL url = new URL(GRAPH_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

        if (conn.getResponseCode() != 200)
            throw new Exception("HTTP " + conn.getResponseCode());

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();

        String json = sb.toString();
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(PREF_KEY, json).apply();

        Log.d(TAG, "Graphe chargé depuis le réseau");
        return parseGraph(json);
    }

    // Charge le graphe depuis le cache (fallback embarqué si absent/invalide).
    public NavigationGraph loadFromCache(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(PREF_KEY, null);
        if (json == null) {
            Log.w(TAG, "Aucun cache — utilisation du graphe embarqué");
            return NavigationGraph.buildFallback();
        }
        try {
            Log.d(TAG, "Graphe chargé depuis le cache");
            return parseGraph(json);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lecture cache, utilisation du graphe embarqué", e);
            return NavigationGraph.buildFallback();
        }
    }

    // Parse le JSON graphe (nœuds + arêtes) et construit le NavigationGraph.
    private NavigationGraph parseGraph(String json) throws Exception {
        JSONObject root   = new JSONObject(json);
        JSONArray  jNodes = root.getJSONArray("nodes");
        JSONArray  jEdges = root.getJSONArray("edges");

        NavigationGraph graph = new NavigationGraph();

        for (int i = 0; i < jNodes.length(); i++) {
            JSONObject n = jNodes.getJSONObject(i);
            graph.addNodePublic(
                    n.getString("id"),
                    n.getString("nom"),
                    n.getString("blocId"),
                    n.getInt("etage"),
                    (float) n.getDouble("x"),
                    (float) n.getDouble("y"),
                    NavigationNode.Type.valueOf(n.getString("type"))
            );
        }

        for (int i = 0; i < jEdges.length(); i++) {
            JSONObject e    = jEdges.getJSONObject(i);
            NavigationNode from = graph.getNode(e.getString("fromNode"));
            NavigationNode to   = graph.getNode(e.getString("toNode"));
            if (from == null || to == null) continue;
            NavigationNode.connect(
                    from, to,
                    (float) e.getDouble("weight"),
                    e.getString("instrAtoB"),
                    e.getString("instrBtoA")
            );
        }

        Log.d(TAG, "Graph parsé : " + jNodes.length() + " noeuds, " + jEdges.length() + " aretes");
        return graph;
    }
}
