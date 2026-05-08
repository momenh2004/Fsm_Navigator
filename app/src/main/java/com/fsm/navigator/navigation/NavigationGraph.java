package com.fsm.navigator.navigation;

import java.util.*;

/**
 * NavigationGraph.java – Graphe de navigation + algorithme A*
 *
 * Graphe du Bloc 3 (RDC + 1er étage) basé sur les coordonnées réelles :
 *
 * Bloc 3 RDC (17m x 15m) :
 *   Sortie(8.5,0) | 305(0,3.75) | 306(0,6.5) | 307(0,12.1)
 *   308(4,15) | Entrée(8.5,15) | 301(13,15)
 *   302(17,12.1) | 303(17,6.5) | 304(17,3.75)
 *   Escalier(8.5,13)
 */
public class NavigationGraph {

    private Map<String, NavigationNode> nodes = new HashMap<>();

    // Résultat A*
    public static class NavPath {
        public List<NavigationNode> nodes;        // nœuds du chemin
        public List<String>         instructions; // instructions texte
        public float                totalDistance; // distance totale en mètres

        public NavPath(List<NavigationNode> nodes,
                       List<String> instructions, float dist) {
            this.nodes        = nodes;
            this.instructions = instructions;
            this.totalDistance = dist;
        }
    }

    public NavigationGraph() {
        buildBloc3RDC();
        buildBloc3Etage1();
        connectEtages();
    }

    // =========================================================
    // GRAPHE BLOC 3 — RDC
    // Coordonnées réelles (mètres), origine = coin haut-gauche
    // =========================================================
    private void buildBloc3RDC() {

        // ===== SALLES =====
        NavigationNode s301 = addNode("B3_RDC_301", "Salle 301", "B3", 0, 13f,  15f,  NavigationNode.Type.SALLE);
        NavigationNode s302 = addNode("B3_RDC_302", "Salle 302", "B3", 0, 17f,  12.1f,NavigationNode.Type.SALLE);
        NavigationNode s303 = addNode("B3_RDC_303", "Salle 303", "B3", 0, 17f,  6.5f, NavigationNode.Type.SALLE);
        NavigationNode s304 = addNode("B3_RDC_304", "Salle 304", "B3", 0, 17f,  3.75f,NavigationNode.Type.SALLE);
        NavigationNode s305 = addNode("B3_RDC_305", "Salle 305", "B3", 0, 0f,   3.75f,NavigationNode.Type.SALLE);
        NavigationNode s306 = addNode("B3_RDC_306", "Salle 306", "B3", 0, 0f,   6.5f, NavigationNode.Type.SALLE);
        NavigationNode s307 = addNode("B3_RDC_307", "Salle 307", "B3", 0, 0f,   12.1f,NavigationNode.Type.SALLE);
        NavigationNode s308 = addNode("B3_RDC_308", "Salle 308", "B3", 0, 4f,   15f,  NavigationNode.Type.SALLE);

        // ===== POINTS CLÉS =====
        NavigationNode entree  = addNode("B3_RDC_ENTREE",  "Entrée Bloc 3",  "B3", 0, 8.5f, 15f,  NavigationNode.Type.ENTREE);
        NavigationNode sortie  = addNode("B3_RDC_SORTIE",  "Sortie Bloc 3",  "B3", 0, 8.5f, 0f,   NavigationNode.Type.ENTREE);
        NavigationNode esc     = addNode("B3_RDC_ESC",     "Escalier",        "B3", 0, 8.5f, 13f,  NavigationNode.Type.ESCALIER);

        // Points de couloir
        NavigationNode cG1 = addNode("B3_RDC_CG1", "Couloir gauche 1", "B3", 0, 3.5f, 3.75f, NavigationNode.Type.COULOIR);
        NavigationNode cG2 = addNode("B3_RDC_CG2", "Couloir gauche 2", "B3", 0, 3.5f, 6.5f,  NavigationNode.Type.COULOIR);
        NavigationNode cG3 = addNode("B3_RDC_CG3", "Couloir gauche 3", "B3", 0, 3.5f, 12.1f, NavigationNode.Type.COULOIR);
        NavigationNode cD1 = addNode("B3_RDC_CD1", "Couloir droit 1",  "B3", 0, 13.5f,3.75f, NavigationNode.Type.COULOIR);
        NavigationNode cD2 = addNode("B3_RDC_CD2", "Couloir droit 2",  "B3", 0, 13.5f,6.5f,  NavigationNode.Type.COULOIR);
        NavigationNode cD3 = addNode("B3_RDC_CD3", "Couloir droit 3",  "B3", 0, 13.5f,12.1f, NavigationNode.Type.COULOIR);

        // Couloir principal (axe horizontal centre)
        NavigationNode cpH1 = addNode("B3_RDC_CPH1","Couloir centre haut",  "B3", 0, 8.5f, 3.75f, NavigationNode.Type.COULOIR);
        NavigationNode cpH2 = addNode("B3_RDC_CPH2","Couloir centre milieu","B3", 0, 8.5f, 7f,    NavigationNode.Type.COULOIR);
        NavigationNode cpH3 = addNode("B3_RDC_CPH3","Couloir centre bas",   "B3", 0, 8.5f, 12.1f, NavigationNode.Type.CARREFOUR);

        // ===== CONNEXIONS SALLES → COULOIRS =====
        // Salles gauche
        NavigationNode.connect(s305, cG1, 3.5f, "Entrez dans la salle 305", "Sortez de la salle 305");
        NavigationNode.connect(s306, cG2, 3.5f, "Entrez dans la salle 306", "Sortez de la salle 306");
        NavigationNode.connect(s307, cG3, 3.5f, "Entrez dans la salle 307", "Sortez de la salle 307");

        // Salles droite
        NavigationNode.connect(s302, cD3, 3.5f, "Entrez dans la salle 302", "Sortez de la salle 302");
        NavigationNode.connect(s303, cD2, 3.5f, "Entrez dans la salle 303", "Sortez de la salle 303");
        NavigationNode.connect(s304, cD1, 3.5f, "Entrez dans la salle 304", "Sortez de la salle 304");

        // Salles bas
        NavigationNode.connect(s308, entree,  4.5f, "Tournez à gauche vers la salle 308", "Continuez vers l'entrée");
        NavigationNode.connect(s301, entree,  4.5f, "Tournez à droite vers la salle 301", "Continuez vers l'entrée");
        NavigationNode.connect(esc,  entree,  2f,   "Continuez vers l'escalier",           "Continuez vers l'entrée");

        // ===== COULOIR PRINCIPAL VERTICAL (axe Y) =====
        NavigationNode.connect(sortie, cpH1, 3.75f, "Avancez vers l'intérieur",  "Continuez vers la sortie");
        NavigationNode.connect(cpH1,   cpH2, 3.25f, "Continuez tout droit",       "Continuez tout droit");
        NavigationNode.connect(cpH2,   cpH3, 5.1f,  "Continuez tout droit",       "Continuez tout droit");
        NavigationNode.connect(cpH3,   esc,  0.9f,  "Continuez vers l'escalier",  "Continuez");
        NavigationNode.connect(esc,    entree,2f,   "Continuez vers l'entrée",    "Continuez vers l'escalier");

        // ===== COULOIR HORIZONTAL GAUCHE =====
        NavigationNode.connect(cG1, cpH1, 5f, "Tournez à droite",  "Tournez à gauche");
        NavigationNode.connect(cG2, cpH2, 5f, "Tournez à droite",  "Tournez à gauche");
        NavigationNode.connect(cG3, cpH3, 5f, "Tournez à droite",  "Tournez à gauche");

        // Connexions verticales couloir gauche
        NavigationNode.connect(cG1, cG2, 2.75f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(cG2, cG3, 5.6f,  "Continuez tout droit", "Continuez tout droit");

        // ===== COULOIR HORIZONTAL DROIT =====
        NavigationNode.connect(cD1, cpH1, 5f, "Tournez à gauche",  "Tournez à droite");
        NavigationNode.connect(cD2, cpH2, 5f, "Tournez à gauche",  "Tournez à droite");
        NavigationNode.connect(cD3, cpH3, 5f, "Tournez à gauche",  "Tournez à droite");

        // Connexions verticales couloir droit
        NavigationNode.connect(cD1, cD2, 2.75f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(cD2, cD3, 5.6f,  "Continuez tout droit", "Continuez tout droit");
    }

    // =========================================================
    // GRAPHE BLOC 3 — 1er ÉTAGE (même structure que RDC)
    // =========================================================
    private void buildBloc3Etage1() {

        NavigationNode s309 = addNode("B3_E1_309", "Salle 309", "B3", 1, 13f,  15f,   NavigationNode.Type.SALLE);
        NavigationNode s310 = addNode("B3_E1_310", "Salle 310", "B3", 1, 17f,  12.1f, NavigationNode.Type.SALLE);
        NavigationNode s311 = addNode("B3_E1_311", "Salle 311", "B3", 1, 17f,  6.5f,  NavigationNode.Type.SALLE);
        NavigationNode s312 = addNode("B3_E1_312", "Salle 312", "B3", 1, 17f,  3.75f, NavigationNode.Type.SALLE);
        NavigationNode s313 = addNode("B3_E1_313", "Salle 313", "B3", 1, 0f,   3.75f, NavigationNode.Type.SALLE);
        NavigationNode s314 = addNode("B3_E1_314", "Salle 314", "B3", 1, 0f,   6.5f,  NavigationNode.Type.SALLE);
        NavigationNode s315 = addNode("B3_E1_315", "Salle 315", "B3", 1, 0f,   12.1f, NavigationNode.Type.SALLE);
        NavigationNode s316 = addNode("B3_E1_316", "Salle 316", "B3", 1, 4f,   15f,   NavigationNode.Type.SALLE);

        NavigationNode escE1  = addNode("B3_E1_ESC",  "Escalier (1er)",  "B3", 1, 8.5f, 13f,   NavigationNode.Type.ESCALIER);
        NavigationNode cG1E1  = addNode("B3_E1_CG1",  "Couloir G1 E1",  "B3", 1, 3.5f, 3.75f,  NavigationNode.Type.COULOIR);
        NavigationNode cG2E1  = addNode("B3_E1_CG2",  "Couloir G2 E1",  "B3", 1, 3.5f, 6.5f,   NavigationNode.Type.COULOIR);
        NavigationNode cG3E1  = addNode("B3_E1_CG3",  "Couloir G3 E1",  "B3", 1, 3.5f, 12.1f,  NavigationNode.Type.COULOIR);
        NavigationNode cD1E1  = addNode("B3_E1_CD1",  "Couloir D1 E1",  "B3", 1, 13.5f,3.75f,  NavigationNode.Type.COULOIR);
        NavigationNode cD2E1  = addNode("B3_E1_CD2",  "Couloir D2 E1",  "B3", 1, 13.5f,6.5f,   NavigationNode.Type.COULOIR);
        NavigationNode cD3E1  = addNode("B3_E1_CD3",  "Couloir D3 E1",  "B3", 1, 13.5f,12.1f,  NavigationNode.Type.COULOIR);
        NavigationNode cpH1E1 = addNode("B3_E1_CPH1", "Couloir C1 E1",  "B3", 1, 8.5f, 3.75f,  NavigationNode.Type.COULOIR);
        NavigationNode cpH2E1 = addNode("B3_E1_CPH2", "Couloir C2 E1",  "B3", 1, 8.5f, 7f,     NavigationNode.Type.COULOIR);
        NavigationNode cpH3E1 = addNode("B3_E1_CPH3", "Couloir C3 E1",  "B3", 1, 8.5f, 12.1f,  NavigationNode.Type.CARREFOUR);

        // Connexions salles → couloirs
        NavigationNode.connect(s313, cG1E1, 3.5f, "Entrez dans la salle 313", "Sortez");
        NavigationNode.connect(s314, cG2E1, 3.5f, "Entrez dans la salle 314", "Sortez");
        NavigationNode.connect(s315, cG3E1, 3.5f, "Entrez dans la salle 315", "Sortez");
        NavigationNode.connect(s310, cD3E1, 3.5f, "Entrez dans la salle 310", "Sortez");
        NavigationNode.connect(s311, cD2E1, 3.5f, "Entrez dans la salle 311", "Sortez");
        NavigationNode.connect(s312, cD1E1, 3.5f, "Entrez dans la salle 312", "Sortez");
        NavigationNode.connect(s316, escE1, 4.5f, "Tournez à gauche vers 316", "Continuez");
        NavigationNode.connect(s309, escE1, 4.5f, "Tournez à droite vers 309", "Continuez");

        // Couloir principal
        NavigationNode.connect(cpH1E1, cpH2E1, 3.25f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(cpH2E1, cpH3E1, 5.1f,  "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(cpH3E1, escE1,  0.9f,  "Continuez vers l'escalier", "Continuez");

        // Couloirs gauche/droit
        NavigationNode.connect(cG1E1, cpH1E1, 5f,    "Tournez à droite", "Tournez à gauche");
        NavigationNode.connect(cG2E1, cpH2E1, 5f,    "Tournez à droite", "Tournez à gauche");
        NavigationNode.connect(cG3E1, cpH3E1, 5f,    "Tournez à droite", "Tournez à gauche");
        NavigationNode.connect(cD1E1, cpH1E1, 5f,    "Tournez à gauche", "Tournez à droite");
        NavigationNode.connect(cD2E1, cpH2E1, 5f,    "Tournez à gauche", "Tournez à droite");
        NavigationNode.connect(cD3E1, cpH3E1, 5f,    "Tournez à gauche", "Tournez à droite");
        NavigationNode.connect(cG1E1, cG2E1,  2.75f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(cG2E1, cG3E1,  5.6f,  "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(cD1E1, cD2E1,  2.75f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(cD2E1, cD3E1,  5.6f,  "Continuez tout droit", "Continuez tout droit");
    }

    // =========================================================
    // CONNEXION RDC ↔ 1er ÉTAGE via escalier
    // =========================================================
    private void connectEtages() {
        NavigationNode escRDC = nodes.get("B3_RDC_ESC");
        NavigationNode escE1  = nodes.get("B3_E1_ESC");
        if (escRDC != null && escE1 != null) {
            NavigationNode.connect(escRDC, escE1, 5f,
                    "Montez l'escalier (1er étage)",
                    "Descendez l'escalier (RDC)");
        }
    }

    // =========================================================
    // ALGORITHME A*
    // =========================================================

    /**
     * Calcule le chemin optimal entre deux nœuds via A*.
     *
     * @param startId ID du nœud de départ (ex: "B3_RDC_301")
     * @param goalId  ID du nœud d'arrivée  (ex: "B3_RDC_305")
     * @return NavPath avec la liste de nœuds + instructions, ou null si impossible
     */
    public NavPath findPath(String startId, String goalId) {
        NavigationNode start = nodes.get(startId);
        NavigationNode goal  = nodes.get(goalId);

        if (start == null || goal == null) return null;
        if (startId.equals(goalId)) {
            return new NavPath(
                    Collections.singletonList(start),
                    Collections.singletonList("Vous êtes déjà à destination !"),
                    0f
            );
        }

        // Files de priorité et maps A*
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(
                Comparator.comparingDouble(n -> n.f));
        Map<String, Float>          gScore   = new HashMap<>();
        Map<String, AStarNode>      allNodes = new HashMap<>();
        Map<String, String>         cameFrom = new HashMap<>(); // nodeId → parentId
        Map<String, String>         edgeInst = new HashMap<>(); // nodeId → instruction

        AStarNode startAStar = new AStarNode(start, 0f, start.distanceTo(goal));
        openSet.add(startAStar);
        gScore.put(startId, 0f);
        allNodes.put(startId, startAStar);

        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();

            // Arrivé !
            if (current.node.id.equals(goalId)) {
                return reconstructPath(current.node, cameFrom, edgeInst, allNodes, gScore.get(goalId));
            }

            for (NavigationNode.Edge edge : current.node.voisins) {
                String neighborId = edge.destination.id;
                float  tentativeG = gScore.getOrDefault(current.node.id, Float.MAX_VALUE) + edge.cost;

                if (tentativeG < gScore.getOrDefault(neighborId, Float.MAX_VALUE)) {
                    gScore.put(neighborId, tentativeG);
                    cameFrom.put(neighborId, current.node.id);
                    edgeInst.put(neighborId, edge.instruction);

                    float f = tentativeG + edge.destination.distanceTo(goal);
                    AStarNode neighborAStar = new AStarNode(edge.destination, tentativeG, f);
                    allNodes.put(neighborId, neighborAStar);
                    openSet.add(neighborAStar);
                }
            }
        }

        return null; // pas de chemin trouvé
    }

    // Reconstruire le chemin depuis la map cameFrom
    private NavPath reconstructPath(NavigationNode goal,
                                    Map<String, String> cameFrom,
                                    Map<String, String> edgeInst,
                                    Map<String, AStarNode> allNodes,
                                    float totalDist) {
        List<NavigationNode> path = new ArrayList<>();
        List<String> instructions = new ArrayList<>();
        String current = goal.id;

        while (cameFrom.containsKey(current)) {
            path.add(0, allNodes.get(current).node);
            String inst = edgeInst.get(current);
            if (inst != null) instructions.add(0, inst);
            current = cameFrom.get(current);
        }
        path.add(0, allNodes.containsKey(current)
                ? allNodes.get(current).node
                : nodes.get(current));

        // Instruction finale
        instructions.add("🎉 Vous êtes arrivé à " + goal.nom + " !");

        return new NavPath(path, instructions, totalDist);
    }

    // Nœud interne A*
    private static class AStarNode {
        NavigationNode node;
        float g, f;
        AStarNode(NavigationNode node, float g, float f) {
            this.node = node; this.g = g; this.f = f;
        }
    }

    // =========================================================
    // UTILITAIRES
    // =========================================================

    private NavigationNode addNode(String id, String nom, String blocId,
                                   int etage, float x, float y,
                                   NavigationNode.Type type) {
        NavigationNode n = new NavigationNode(id, nom, blocId, etage, x, y, type);
        nodes.put(id, n);
        return n;
    }

    public NavigationNode getNode(String id) { return nodes.get(id); }

    /**
     * Trouver le nœud de salle le plus proche d'un ID de salle BD.
     * Ex: salleId="301" → "B3_RDC_301"
     */
    public NavigationNode findBySalleNom(String nom) {
        for (NavigationNode n : nodes.values()) {
            if (n.nom.equals(nom) && n.type == NavigationNode.Type.SALLE) return n;
        }
        return null;
    }

    /**
     * Trouver le nœud le plus proche d'une position WiFi (x,y)
     */
    public NavigationNode findNearest(float x, float y, String blocId, int etage) {
        NavigationNode best = null;
        float minDist = Float.MAX_VALUE;
        for (NavigationNode n : nodes.values()) {
            if (!n.blocId.equals(blocId) || n.etage != etage) continue;
            float dx = n.x - x, dy = n.y - y;
            float d  = dx*dx + dy*dy;
            if (d < minDist) { minDist = d; best = n; }
        }
        return best;
    }

    public Map<String, NavigationNode> getAllNodes() { return nodes; }
}