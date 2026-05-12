package com.fsm.navigator.navigation;

import java.util.*;

/**
 * NavigationGraph.java — Graphe de navigation A*
 *
 * Bloc 3 — RDC + 1er étage
 * Référentiel portrait : x=largeur(0→17.76m), y=longueur(0→30.74m)
 *
 * Architecture du graphe RDC :
 *
 *   SORTIE
 *     |
 *   INT_HG ─────── INT_HD
 *     |       cour      |
 *   INT_BG ─────── INT_BD
 *     |                 |
 *   ESC (accès étage)   |
 *     |                 |
 *   ENTREE─────────────/
 *
 * Salles gauche  : connectées à INT_HG ou INT_BG
 * Salles droite  : connectées à INT_HD ou INT_BD
 * Salles bas     : connectées à ENTREE
 *
 * RÈGLE ESCALIER :
 *   - ESC est connecté UNIQUEMENT à INT_BG et ESC_E1
 *   - A* filtre ESC si même étage (sameFloor = true)
 */
public class NavigationGraph {

    private Map<String, NavigationNode> nodes = new HashMap<>();

    public static class NavPath {
        public List<NavigationNode> nodes;
        public List<String>         instructions;
        public float                totalDistance;

        public NavPath(List<NavigationNode> nodes,
                       List<String> instructions, float dist) {
            this.nodes         = nodes;
            this.instructions  = instructions;
            this.totalDistance = dist;
        }
    }

    public NavigationGraph() {
        buildBloc3RDC();
        buildBloc3Etage1();
        connectEtages();
    }

    // =========================================================
    // BLOC 3 — RDC
    // =========================================================
    private void buildBloc3RDC() {

        // ── Salles ────────────────────────────────────────────
        NavigationNode s301   = addNode("B3_RDC_301",   "Salle 301", "B3", 0, 14.0f, 28.5f, NavigationNode.Type.SALLE);
        NavigationNode s302   = addNode("B3_RDC_302",   "Salle 302", "B3", 0, 16.5f, 23.5f, NavigationNode.Type.SALLE);
        NavigationNode s303   = addNode("B3_RDC_303",   "Salle 303", "B3", 0, 16.5f,  8.0f, NavigationNode.Type.SALLE);
        NavigationNode s304   = addNode("B3_RDC_304",   "Salle 304", "B3", 0, 16.5f,  2.0f, NavigationNode.Type.SALLE);
        NavigationNode s305   = addNode("B3_RDC_305",   "Salle 305", "B3", 0,  1.25f, 2.0f, NavigationNode.Type.SALLE);
        NavigationNode s306   = addNode("B3_RDC_306",   "Salle 306", "B3", 0,  1.25f, 8.0f, NavigationNode.Type.SALLE);
        NavigationNode s307   = addNode("B3_RDC_307",   "Salle 307", "B3", 0,  1.25f,23.5f, NavigationNode.Type.SALLE);
        NavigationNode s308   = addNode("B3_RDC_308",   "Salle 308", "B3", 0,  3.75f,28.5f, NavigationNode.Type.SALLE);
        NavigationNode bureau = addNode("B3_RDC_BUREAU","Bureau",    "B3", 0, 16.5f, 15.0f, NavigationNode.Type.SALLE);

        // ── Points clés ───────────────────────────────────────
        NavigationNode entree = addNode("B3_RDC_ENTREE", "Entrée",   "B3", 0,  8.88f, 30.0f, NavigationNode.Type.ENTREE);
        NavigationNode sortie = addNode("B3_RDC_SORTIE", "Sortie",   "B3", 0,  8.88f,  0.5f, NavigationNode.Type.ENTREE);
        NavigationNode esc    = addNode("B3_RDC_ESC",    "Escalier", "B3", 0,  8.88f, 26.5f, NavigationNode.Type.ESCALIER);

        // ── 4 Intersections ───────────────────────────────────
        // Ce sont les 4 coins du couloir autour de la cour centrale
        NavigationNode intHG  = addNode("B3_RDC_INT_HG", "Intersect HG", "B3", 0,  1.75f,  2.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intHD  = addNode("B3_RDC_INT_HD", "Intersect HD", "B3", 0, 16.0f,   2.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intBG  = addNode("B3_RDC_INT_BG", "Intersect BG", "B3", 0,  1.75f, 26.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intBD  = addNode("B3_RDC_INT_BD", "Intersect BD", "B3", 0, 16.0f,  26.0f, NavigationNode.Type.CARREFOUR);

        // ── Intersections intermédiaires (niveau salles 303/306) ──
        NavigationNode intMG = addNode("B3_RDC_INT_MG", "Int MG", "B3", 0,  1.75f,  8.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intMD = addNode("B3_RDC_INT_MD", "Int MD", "B3", 0, 16.0f,   8.0f, NavigationNode.Type.CARREFOUR);
        // Intersection intermédiaire niveau bureau
        NavigationNode intBurD = addNode("B3_RDC_INT_BURD", "Int Bureau D", "B3", 0, 16.0f, 15.0f, NavigationNode.Type.CARREFOUR);

        // ── Connexions salles → intersections ─────────────────
        // Salles gauche — chacune connectée à son intersection la plus proche
        NavigationNode.connect(s305, intHG,  0.5f, "Entrez dans la salle 305", "Sortez de la salle 305");
        NavigationNode.connect(s306, intMG,  0.5f, "Entrez dans la salle 306", "Sortez de la salle 306");
        NavigationNode.connect(s307, intBG,  0.5f, "Entrez dans la salle 307", "Sortez de la salle 307");

        // Salles droite — chacune connectée à son intersection la plus proche
        NavigationNode.connect(s304, intHD,   0.5f, "Entrez dans la salle 304", "Sortez de la salle 304");
        NavigationNode.connect(s303, intMD,   0.5f, "Entrez dans la salle 303", "Sortez de la salle 303");
        NavigationNode.connect(s302, intBD,   0.5f, "Entrez dans la salle 302", "Sortez de la salle 302");
        NavigationNode.connect(bureau, intBurD, 0.5f, "Entrez dans le bureau",  "Sortez du bureau");

        // Salles bas
        NavigationNode.connect(s308, entree, 4.0f, "Tournez à gauche vers 308", "Continuez vers l'entrée");
        NavigationNode.connect(s301, entree, 4.0f, "Tournez à droite vers 301", "Continuez vers l'entrée");

        // ── Couloir horizontal HAUT (INT_HG ↔ INT_HD) ─────────
        NavigationNode.connect(intHG, intHD, 14.25f, "Continuez tout droit", "Continuez tout droit");

        // ── Couloir horizontal BAS (INT_BG ↔ INT_BD) ──────────
        NavigationNode.connect(intBG, intBD, 14.25f, "Continuez tout droit", "Continuez tout droit");

        // ── Couloir vertical GAUCHE segmenté ───────────────────
        NavigationNode.connect(intHG,  intMG,  6.0f,  "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intMG,  intBG, 18.0f,  "Continuez tout droit", "Continuez tout droit");

        // ── Couloir vertical DROIT segmenté ────────────────────
        NavigationNode.connect(intHD,    intMD,   6.0f,  "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intMD,    intBurD, 7.0f,  "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intBurD,  intBD,   11.0f, "Continuez tout droit", "Continuez tout droit");

        // ── Entrée → intersections BAS ─────────────────────────
        NavigationNode.connect(entree, intBG, 7.13f, "Tournez à gauche",  "Continuez vers l'entrée");
        NavigationNode.connect(entree, intBD, 7.13f, "Tournez à droite",  "Continuez vers l'entrée");

        // ── Sortie → intersections HAUT ────────────────────────
        NavigationNode.connect(sortie, intHG, 7.13f, "Tournez à gauche",  "Continuez vers la sortie");
        NavigationNode.connect(sortie, intHD, 7.13f, "Tournez à droite",  "Continuez vers la sortie");

        // ── Escalier → INT_BG UNIQUEMENT ───────────────────────
        NavigationNode.connect(esc, intBG, 0.5f, "Accédez à l'escalier", "Quittez l'escalier");
    }

    // =========================================================
    // BLOC 3 — 1er ÉTAGE (même structure)
    // =========================================================
    private void buildBloc3Etage1() {

        NavigationNode s309   = addNode("B3_E1_309",   "Salle 309", "B3", 1, 14.0f, 28.5f, NavigationNode.Type.SALLE);
        NavigationNode s310   = addNode("B3_E1_310",   "Salle 310", "B3", 1, 16.5f, 23.5f, NavigationNode.Type.SALLE);
        NavigationNode s311   = addNode("B3_E1_311",   "Salle 311", "B3", 1, 16.5f,  8.0f, NavigationNode.Type.SALLE);
        NavigationNode s312   = addNode("B3_E1_312",   "Salle 312", "B3", 1, 16.5f,  2.0f, NavigationNode.Type.SALLE);
        NavigationNode s313   = addNode("B3_E1_313",   "Salle 313", "B3", 1,  1.25f, 2.0f, NavigationNode.Type.SALLE);
        NavigationNode s314   = addNode("B3_E1_314",   "Salle 314", "B3", 1,  1.25f, 8.0f, NavigationNode.Type.SALLE);
        NavigationNode s315   = addNode("B3_E1_315",   "Salle 315", "B3", 1,  1.25f,23.5f, NavigationNode.Type.SALLE);
        NavigationNode s316   = addNode("B3_E1_316",   "Salle 316", "B3", 1,  3.75f,28.5f, NavigationNode.Type.SALLE);

        NavigationNode escE1  = addNode("B3_E1_ESC",     "Escalier E1", "B3", 1,  8.88f, 26.5f, NavigationNode.Type.ESCALIER);
        NavigationNode intHGE = addNode("B3_E1_INT_HG",  "Int HG E1",   "B3", 1,  1.75f,  2.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intHDE = addNode("B3_E1_INT_HD",  "Int HD E1",   "B3", 1, 16.0f,   2.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intBGE = addNode("B3_E1_INT_BG",  "Int BG E1",   "B3", 1,  1.75f, 26.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intBDE = addNode("B3_E1_INT_BD",  "Int BD E1",   "B3", 1, 16.0f,  26.0f, NavigationNode.Type.CARREFOUR);

        // Intersections intermédiaires E1
        NavigationNode intMGE   = addNode("B3_E1_INT_MG",   "Int MG E1",   "B3", 1,  1.75f,  8.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intMDE   = addNode("B3_E1_INT_MD",   "Int MD E1",   "B3", 1, 16.0f,   8.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intBurDE = addNode("B3_E1_INT_BURD", "Int Bureau E1","B3",1, 16.0f,  15.0f, NavigationNode.Type.CARREFOUR);

        // Salles gauche
        NavigationNode.connect(s313, intHGE, 0.5f, "Entrez dans la salle 313", "Sortez");
        NavigationNode.connect(s314, intMGE, 0.5f, "Entrez dans la salle 314", "Sortez");
        NavigationNode.connect(s315, intBGE, 0.5f, "Entrez dans la salle 315", "Sortez");

        // Salles droite
        NavigationNode.connect(s312, intHDE,   0.5f, "Entrez dans la salle 312", "Sortez");
        NavigationNode.connect(s311, intMDE,   0.5f, "Entrez dans la salle 311", "Sortez");
        NavigationNode.connect(s310, intBDE,   0.5f, "Entrez dans la salle 310", "Sortez");

        // Salles bas
        NavigationNode escEntry1 = addNode("B3_E1_ENTREE_ESC", "Couloir esc E1", "B3", 1, 8.88f, 28.5f, NavigationNode.Type.COULOIR);
        NavigationNode.connect(s316, escEntry1, 2.0f, "Tournez à gauche vers 316","Continuez");
        NavigationNode.connect(s309, escEntry1, 4.0f, "Tournez à droite vers 309","Continuez");
        NavigationNode.connect(escEntry1, intBGE, 4.0f, "Continuez", "Continuez");
        NavigationNode.connect(escEntry1, intBDE, 4.0f, "Continuez", "Continuez");

        // Couloirs segmentés E1
        NavigationNode.connect(intHGE, intHDE,   14.25f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intBGE, intBDE,   14.25f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intHGE,  intMGE,   6.0f,  "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intMGE,  intBGE,  18.0f,  "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intHDE,  intMDE,   6.0f,  "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intMDE,  intBurDE, 7.0f,  "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intBurDE,intBDE,  11.0f,  "Continuez tout droit", "Continuez tout droit");

        // Escalier E1 → INT_BG E1
        NavigationNode.connect(escE1, intBGE, 0.5f, "Accédez à l'escalier E1", "Quittez l'escalier");
    }

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
    public NavPath findPath(String startId, String goalId) {
        NavigationNode start = nodes.get(startId);
        NavigationNode goal  = nodes.get(goalId);

        if (start == null || goal == null) return null;
        if (startId.equals(goalId)) {
            return new NavPath(
                    Collections.singletonList(start),
                    Collections.singletonList("Vous êtes déjà à destination !"),
                    0f);
        }

        // Même étage → escalier interdit
        boolean sameFloor = (start.etage == goal.etage);

        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(
                Comparator.comparingDouble(n -> n.f));
        Map<String, Float>     gScore   = new HashMap<>();
        Map<String, AStarNode> allNodes = new HashMap<>();
        Map<String, String>    cameFrom = new HashMap<>();
        Map<String, String>    edgeInst = new HashMap<>();

        AStarNode startAStar = new AStarNode(start, 0f, start.distanceTo(goal));
        openSet.add(startAStar);
        gScore.put(startId, 0f);
        allNodes.put(startId, startAStar);

        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();

            if (current.node.id.equals(goalId)) {
                return reconstructPath(current.node, cameFrom, edgeInst,
                        allNodes, gScore.get(goalId));
            }

            for (NavigationNode.Edge edge : current.node.voisins) {
                NavigationNode neighbor = edge.destination;

                // Ignorer l'escalier si même étage
                if (sameFloor && neighbor.type == NavigationNode.Type.ESCALIER) {
                    continue;
                }

                String neighborId = neighbor.id;
                float  tentativeG = gScore.getOrDefault(current.node.id,
                        Float.MAX_VALUE) + edge.cost;

                if (tentativeG < gScore.getOrDefault(neighborId, Float.MAX_VALUE)) {
                    gScore.put(neighborId, tentativeG);
                    cameFrom.put(neighborId, current.node.id);
                    edgeInst.put(neighborId, edge.instruction);

                    float f = tentativeG + neighbor.distanceTo(goal);
                    allNodes.put(neighborId, new AStarNode(neighbor, tentativeG, f));
                    openSet.add(new AStarNode(neighbor, tentativeG, f));
                }
            }
        }
        return null;
    }

    private NavPath reconstructPath(NavigationNode goal,
                                    Map<String, String> cameFrom,
                                    Map<String, String> edgeInst,
                                    Map<String, AStarNode> allNodes,
                                    float totalDist) {
        List<NavigationNode> path         = new ArrayList<>();
        List<String>         instructions = new ArrayList<>();
        String current = goal.id;

        while (cameFrom.containsKey(current)) {
            path.add(0, allNodes.get(current).node);
            String inst = edgeInst.get(current);
            if (inst != null) instructions.add(0, inst);
            current = cameFrom.get(current);
        }
        path.add(0, allNodes.containsKey(current)
                ? allNodes.get(current).node : nodes.get(current));

        instructions.add("🎉 Vous êtes arrivé à " + goal.nom + " !");
        return new NavPath(path, instructions, totalDist);
    }

    private static class AStarNode {
        NavigationNode node;
        float g, f;
        AStarNode(NavigationNode n, float g, float f) {
            this.node = n; this.g = g; this.f = f;
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

    public NavigationNode getNode(String id)            { return nodes.get(id); }
    public Map<String, NavigationNode> getAllNodes()     { return nodes; }

    public NavigationNode findBySalleNom(String nom) {
        for (NavigationNode n : nodes.values())
            if (n.nom.equals(nom) && n.type == NavigationNode.Type.SALLE) return n;
        return null;
    }

    public NavigationNode findNearest(float x, float y, String blocId, int etage) {
        NavigationNode best = null;
        float minDist = Float.MAX_VALUE;
        for (NavigationNode n : nodes.values()) {
            if (!n.blocId.equals(blocId) || n.etage != etage) continue;
            float d = (n.x-x)*(n.x-x) + (n.y-y)*(n.y-y);
            if (d < minDist) { minDist = d; best = n; }
        }
        return best;
    }

    public NavigationNode findEntree(String blocId) {
        NavigationNode n = nodes.get(blocId + "_RDC_ENTREE");
        if (n != null) return n;
        for (NavigationNode node : nodes.values())
            if (node.id.startsWith(blocId) &&
                    (node.nom.toLowerCase().contains("entrée") ||
                            node.nom.toLowerCase().contains("entree")))
                return node;
        return null;
    }

    public NavigationNode findAnyNodeInBloc(String blocId) {
        for (NavigationNode n : nodes.values())
            if (n.id.startsWith(blocId)) return n;
        return null;
    }
}