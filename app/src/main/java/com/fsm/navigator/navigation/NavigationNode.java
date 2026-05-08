package com.fsm.navigator.navigation;

import java.util.ArrayList;
import java.util.List;

/**
 * NavigationNode.java – Nœud du graphe de navigation
 *
 * Types :
 *   SALLE     → salle de cours
 *   COULOIR   → point de passage
 *   ESCALIER  → entre étages
 *   ENTREE    → entrée/sortie bâtiment
 *   CARREFOUR → intersection couloirs
 */
public class NavigationNode {

    public enum Type { SALLE, COULOIR, ESCALIER, ENTREE, CARREFOUR }

    public String id;       // ex: "B3_301", "B3_ESC", "B3_ENTREE"
    public String nom;      // ex: "Salle 301"
    public String blocId;   // ex: "B3"
    public int    etage;    // 0=RDC, 1=1er
    public float  x, y;    // coordonnées réelles (mètres) dans le bâtiment
    public Type   type;
    public List<Edge> voisins = new ArrayList<>();

    public NavigationNode(String id, String nom, String blocId,
                          int etage, float x, float y, Type type) {
        this.id     = id;
        this.nom    = nom;
        this.blocId = blocId;
        this.etage  = etage;
        this.x      = x;
        this.y      = y;
        this.type   = type;
    }

    // ===== ARÊTE =====
    public static class Edge {
        public NavigationNode destination;
        public float          cost;       // distance en mètres
        public String         instruction; // "Continuez tout droit", "Tournez à gauche"...

        public Edge(NavigationNode dest, float cost, String instruction) {
            this.destination = dest;
            this.cost        = cost;
            this.instruction = instruction;
        }
    }

    // Ajouter un voisin bidirectionnel
    public static void connect(NavigationNode a, NavigationNode b,
                               float cost, String instrAB, String instrBA) {
        a.voisins.add(new Edge(b, cost, instrAB));
        b.voisins.add(new Edge(a, cost, instrBA));
    }

    // Distance euclidienne (heuristique A*)
    public float distanceTo(NavigationNode other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        return (float) Math.sqrt(dx*dx + dy*dy);
    }
}