package com.fsm.navigator.model;

import android.util.Log;

import java.util.*;

/**
 * NavigationGraph.java — Graphe de navigation A*
 *
 * Bloc 3 — RDC + 1er étage
 * Référentiel portrait : x=largeur(0→17.76m), y=longueur(0→30.74m)
 *
 * Architecture du graphe RDC (couloir à x=3.8 G / x=13.9 D, hors salles) :
 *
 *   SORTIE
 *     |
 *   INT_HG ── G1 ── G2 ─────── G3 ── INT_BG
 *     |   cour             cour   |
 *   INT_HD ── D1 ── D2 ── D3 ── D4 ── INT_BD
 *                                   |
 *                               ESC (accès étage)
 *                                   |
 *                               ENTREE
 *
 * Salles gauche  : connectées horizontalement au nœud Gx de même y
 * Salles droite  : connectées horizontalement au nœud Dx de même y
 * Salles bas     : connectées à ENTREE
 *
 * RÈGLE ESCALIER :
 *   - ESC est connecté à INT_BG, INT_BD et ESC_E1
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

    // Constructeur vide (graphe rempli dynamiquement par NavigationGraphLoader).
    public NavigationGraph() {
    }

    // Construit le graphe complet embarqué (fallback hors ligne).
    public static NavigationGraph buildFallback() {
        NavigationGraph g = new NavigationGraph();
        g.buildBloc3RDC();
        g.buildBloc3Etage1();
        g.buildBlocPalestineRDC();
        g.buildBlocMath();
        g.buildCampusOutdoor();
        g.connectEtages();
        return g;
    }

    // =========================================================
    // BLOC 3 — RDC
    // Couloir positionné à x=3.8 (G) / x=13.9 (D), hors zones salles [0..3.5] et [14.26..17.76]
    // =========================================================
    private void buildBloc3RDC() {

        // ── Salles (centres calés sur NavigationView) ─────────
        NavigationNode s301   = addNode("B3_RDC_301",   "Salle 301", "B3", 0, 14.01f, 28.75f, NavigationNode.Type.SALLE);
        NavigationNode s302   = addNode("B3_RDC_302",   "Salle 302", "B3", 0, 16.01f, 23.5f,  NavigationNode.Type.SALLE);
        NavigationNode s303   = addNode("B3_RDC_303",   "Salle 303", "B3", 0, 16.01f,  8.3f,  NavigationNode.Type.SALLE);
        NavigationNode s304   = addNode("B3_RDC_304",   "Salle 304", "B3", 0, 16.01f,  5.0f,  NavigationNode.Type.SALLE);
        NavigationNode s305   = addNode("B3_RDC_305",   "Salle 305", "B3", 0,  1.75f,  5.0f,  NavigationNode.Type.SALLE);
        NavigationNode s306   = addNode("B3_RDC_306",   "Salle 306", "B3", 0,  1.75f,  8.3f,  NavigationNode.Type.SALLE);
        NavigationNode s307   = addNode("B3_RDC_307",   "Salle 307", "B3", 0,  1.75f, 23.5f,  NavigationNode.Type.SALLE);
        NavigationNode s308   = addNode("B3_RDC_308",   "Salle 308", "B3", 0,  3.75f, 28.75f, NavigationNode.Type.SALLE);
        NavigationNode bureau = addNode("B3_RDC_BUREAU","Bureau",    "B3", 0, 16.01f, 15.0f,  NavigationNode.Type.SALLE);

        // ── Points clés ───────────────────────────────────────
        NavigationNode entree = addNode("B3_RDC_ENTREE", "Entrée",   "B3", 0,  8.88f, 30.0f, NavigationNode.Type.ENTREE);
        NavigationNode sortie = addNode("B3_RDC_SORTIE", "Sortie",   "B3", 0,  8.88f,  0.5f, NavigationNode.Type.ENTREE);
        NavigationNode esc    = addNode("B3_RDC_ESC",    "Escalier", "B3", 0,  8.88f, 26.5f, NavigationNode.Type.ESCALIER);

        // ── 4 coins du couloir (x=3.8 G / x=13.9 D) ──────────
        NavigationNode intHG = addNode("B3_RDC_INT_HG", "Couloir HG", "B3", 0,  3.8f,  2.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intHD = addNode("B3_RDC_INT_HD", "Couloir HD", "B3", 0, 13.9f,  2.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intBG = addNode("B3_RDC_INT_BG", "Couloir BG", "B3", 0,  3.8f, 26.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intBD = addNode("B3_RDC_INT_BD", "Couloir BD", "B3", 0, 13.9f, 26.0f, NavigationNode.Type.CARREFOUR);

        // ── Nœuds intermédiaires GAUCHE (x=3.8, un par salle) ─
        NavigationNode intG1 = addNode("B3_RDC_INT_G1", "Couloir G1", "B3", 0,  3.8f,  5.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intG2 = addNode("B3_RDC_INT_G2", "Couloir G2", "B3", 0,  3.8f,  8.3f, NavigationNode.Type.CARREFOUR);
        NavigationNode intG3 = addNode("B3_RDC_INT_G3", "Couloir G3", "B3", 0,  3.8f, 23.5f, NavigationNode.Type.CARREFOUR);

        // ── Nœuds intermédiaires DROIT (x=13.9, un par salle) ─
        NavigationNode intD1 = addNode("B3_RDC_INT_D1", "Couloir D1", "B3", 0, 13.9f,  5.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intD2 = addNode("B3_RDC_INT_D2", "Couloir D2", "B3", 0, 13.9f,  8.3f, NavigationNode.Type.CARREFOUR);
        NavigationNode intD3 = addNode("B3_RDC_INT_D3", "Couloir D3", "B3", 0, 13.9f, 15.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intD4 = addNode("B3_RDC_INT_D4", "Couloir D4", "B3", 0, 13.9f, 23.5f, NavigationNode.Type.CARREFOUR);

        // ── Connexions salles → couloir (horizontal ~2m) ───────
        NavigationNode.connect(s305,   intG1, 2.05f, "Entrez dans la salle 305", "Sortez de la salle 305");
        NavigationNode.connect(s306,   intG2, 2.05f, "Entrez dans la salle 306", "Sortez de la salle 306");
        NavigationNode.connect(s307,   intG3, 2.05f, "Entrez dans la salle 307", "Sortez de la salle 307");
        NavigationNode.connect(s304,   intD1, 2.11f, "Entrez dans la salle 304", "Sortez de la salle 304");
        NavigationNode.connect(s303,   intD2, 2.11f, "Entrez dans la salle 303", "Sortez de la salle 303");
        NavigationNode.connect(bureau, intD3, 2.11f, "Entrez dans le bureau",    "Sortez du bureau");
        NavigationNode.connect(s302,   intD4, 2.11f, "Entrez dans la salle 302", "Sortez de la salle 302");

        // Salles bas
        NavigationNode.connect(s308, entree, 5.3f, "Tournez à gauche vers 308", "Continuez vers l'entrée");
        NavigationNode.connect(s301, entree, 5.3f, "Tournez à droite vers 301", "Continuez vers l'entrée");

        // ── Couloir horizontal HAUT ────────────────────────────
        NavigationNode.connect(intHG, intHD, 10.1f, "Continuez tout droit", "Continuez tout droit");

        // ── Couloir horizontal BAS ─────────────────────────────
        NavigationNode.connect(intBG, intBD, 10.1f, "Continuez tout droit", "Continuez tout droit");

        // ── Couloir vertical GAUCHE (x=3.8, segmenté) ─────────
        NavigationNode.connect(intHG, intG1,  3.0f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intG1, intG2,  3.3f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intG2, intG3, 15.2f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intG3, intBG,  2.5f, "Continuez tout droit", "Continuez tout droit");

        // ── Couloir vertical DROIT (x=13.9, segmenté) ─────────
        NavigationNode.connect(intHD, intD1,  3.0f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intD1, intD2,  3.3f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intD2, intD3,  6.7f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intD3, intD4,  8.5f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intD4, intBD,  2.5f, "Continuez tout droit", "Continuez tout droit");

        // ── Entrée → coins BAS ────────────────────────────────
        NavigationNode.connect(entree, intBG, 6.5f, "Tournez à gauche",  "Continuez vers l'entrée");
        NavigationNode.connect(entree, intBD, 6.5f, "Tournez à droite",  "Continuez vers l'entrée");

        // ── Sortie → coins HAUT ───────────────────────────────
        NavigationNode.connect(sortie, intHG, 5.3f, "Tournez à gauche",  "Continuez vers la sortie");
        NavigationNode.connect(sortie, intHD, 5.3f, "Tournez à droite",  "Continuez vers la sortie");

        // ── Escalier → coins BAS (des deux côtés) ─────────────
        NavigationNode.connect(esc, intBG, 5.1f, "Accédez à l'escalier", "Quittez l'escalier");
        NavigationNode.connect(esc, intBD, 5.1f, "Accédez à l'escalier", "Quittez l'escalier");
    }

    // =========================================================
    // BLOC 3 — 1er ÉTAGE (même structure que RDC)
    // =========================================================
    private void buildBloc3Etage1() {

        // ── Salles ────────────────────────────────────────────
        NavigationNode s309 = addNode("B3_E1_309", "Salle 309", "B3", 1, 14.01f, 28.75f, NavigationNode.Type.SALLE);
        NavigationNode s310 = addNode("B3_E1_310", "Salle 310", "B3", 1, 16.01f, 23.5f,  NavigationNode.Type.SALLE);
        NavigationNode s311 = addNode("B3_E1_311", "Salle 311", "B3", 1, 16.01f,  8.3f,  NavigationNode.Type.SALLE);
        NavigationNode s312 = addNode("B3_E1_312", "Salle 312", "B3", 1, 16.01f,  5.0f,  NavigationNode.Type.SALLE);
        NavigationNode s313 = addNode("B3_E1_313", "Salle 313", "B3", 1,  1.75f,  5.0f,  NavigationNode.Type.SALLE);
        NavigationNode s314 = addNode("B3_E1_314", "Salle 314", "B3", 1,  1.75f,  8.3f,  NavigationNode.Type.SALLE);
        NavigationNode s315 = addNode("B3_E1_315", "Salle 315", "B3", 1,  1.75f, 23.5f,  NavigationNode.Type.SALLE);
        NavigationNode s316 = addNode("B3_E1_316", "Salle 316", "B3", 1,  3.75f, 28.75f, NavigationNode.Type.SALLE);

        NavigationNode escE1 = addNode("B3_E1_ESC", "Escalier E1", "B3", 1, 8.88f, 26.5f, NavigationNode.Type.ESCALIER);

        // ── 4 coins du couloir ────────────────────────────────
        NavigationNode intHGE = addNode("B3_E1_INT_HG", "Couloir HG E1", "B3", 1,  3.8f,  2.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intHDE = addNode("B3_E1_INT_HD", "Couloir HD E1", "B3", 1, 13.9f,  2.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intBGE = addNode("B3_E1_INT_BG", "Couloir BG E1", "B3", 1,  3.8f, 26.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intBDE = addNode("B3_E1_INT_BD", "Couloir BD E1", "B3", 1, 13.9f, 26.0f, NavigationNode.Type.CARREFOUR);

        // ── Nœuds intermédiaires GAUCHE ───────────────────────
        NavigationNode intG1E = addNode("B3_E1_INT_G1", "Couloir G1 E1", "B3", 1,  3.8f,  5.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intG2E = addNode("B3_E1_INT_G2", "Couloir G2 E1", "B3", 1,  3.8f,  8.3f, NavigationNode.Type.CARREFOUR);
        NavigationNode intG3E = addNode("B3_E1_INT_G3", "Couloir G3 E1", "B3", 1,  3.8f, 23.5f, NavigationNode.Type.CARREFOUR);

        // ── Nœuds intermédiaires DROIT ────────────────────────
        NavigationNode intD1E = addNode("B3_E1_INT_D1", "Couloir D1 E1", "B3", 1, 13.9f,  5.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intD2E = addNode("B3_E1_INT_D2", "Couloir D2 E1", "B3", 1, 13.9f,  8.3f, NavigationNode.Type.CARREFOUR);
        NavigationNode intD3E = addNode("B3_E1_INT_D3", "Couloir D3 E1", "B3", 1, 13.9f, 15.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intD4E = addNode("B3_E1_INT_D4", "Couloir D4 E1", "B3", 1, 13.9f, 23.5f, NavigationNode.Type.CARREFOUR);

        // ── Connexions salles → couloir ───────────────────────
        NavigationNode.connect(s313, intG1E, 2.05f, "Entrez dans la salle 313", "Sortez");
        NavigationNode.connect(s314, intG2E, 2.05f, "Entrez dans la salle 314", "Sortez");
        NavigationNode.connect(s315, intG3E, 2.05f, "Entrez dans la salle 315", "Sortez");
        NavigationNode.connect(s312, intD1E, 2.11f, "Entrez dans la salle 312", "Sortez");
        NavigationNode.connect(s311, intD2E, 2.11f, "Entrez dans la salle 311", "Sortez");
        NavigationNode.connect(s310, intD4E, 2.11f, "Entrez dans la salle 310", "Sortez");

        // Salles bas
        NavigationNode escEntry1 = addNode("B3_E1_ENTREE_ESC", "Couloir esc E1", "B3", 1, 8.88f, 28.5f, NavigationNode.Type.COULOIR);
        NavigationNode.connect(s316,      escEntry1, 2.0f, "Tournez à gauche vers 316", "Continuez");
        NavigationNode.connect(s309,      escEntry1, 4.0f, "Tournez à droite vers 309", "Continuez");
        NavigationNode.connect(escEntry1, intBGE,    5.7f, "Continuez", "Continuez");
        NavigationNode.connect(escEntry1, intBDE,    5.7f, "Continuez", "Continuez");

        // ── Couloir horizontal ────────────────────────────────
        NavigationNode.connect(intHGE, intHDE, 10.1f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intBGE, intBDE, 10.1f, "Continuez tout droit", "Continuez tout droit");

        // ── Couloir vertical GAUCHE (x=3.8, segmenté) ─────────
        NavigationNode.connect(intHGE, intG1E,  3.0f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intG1E, intG2E,  3.3f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intG2E, intG3E, 15.2f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intG3E, intBGE,  2.5f, "Continuez tout droit", "Continuez tout droit");

        // ── Couloir vertical DROIT (x=13.9, segmenté) ─────────
        NavigationNode.connect(intHDE, intD1E,  3.0f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intD1E, intD2E,  3.3f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intD2E, intD3E,  6.7f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intD3E, intD4E,  8.5f, "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intD4E, intBDE,  2.5f, "Continuez tout droit", "Continuez tout droit");

        // ── Escalier E1 → coins BAS (des deux côtés) ──────────
        NavigationNode.connect(escE1, intBGE, 5.1f, "Accédez à l'escalier E1", "Quittez l'escalier");
        NavigationNode.connect(escE1, intBDE, 5.1f, "Accédez à l'escalier E1", "Quittez l'escalier");
    }
    private void buildBlocPalestineRDC() {
        String bId = "BPAL";

        // ── 1. Nœuds des Salles (Coordonnées basées sur ton drawBlocPalestine) ────
        // Côté GAUCHE
        NavigationNode s105 = addNode("BP_105", "Salle 105", bId, 0, 1.75f, 7.0f, NavigationNode.Type.SALLE);
        NavigationNode s106 = addNode("BP_106", "Salle 106", bId, 0, 1.75f, 12.0f, NavigationNode.Type.SALLE);
        NavigationNode s107 = addNode("BP_107", "Salle 107", bId, 0, 1.75f, 20.5f, NavigationNode.Type.SALLE);

        // Côté DROIT
        NavigationNode s103 = addNode("BP_103", "Salle 103", bId, 0, 23.7f, 7.0f, NavigationNode.Type.SALLE);
        NavigationNode s102 = addNode("BP_102", "Salle 102", bId, 0, 23.7f, 12.0f, NavigationNode.Type.SALLE);
        NavigationNode s101 = addNode("BP_101", "Salle 101", bId, 0, 23.7f, 20.5f, NavigationNode.Type.SALLE);

        // Côté HAUT
        NavigationNode s104 = addNode("BP_104", "Salle 104", bId, 0, 15.75f, 1.75f, NavigationNode.Type.SALLE);

        // Amphithéâtres (Centres des zones définies)
        NavigationNode amphiA = addNode("BP_AA", "Amphi A", bId, 0, 21.0f, 24.5f, NavigationNode.Type.SALLE);
        NavigationNode amphiB = addNode("BP_AB", "Amphi B", bId, 0, 21.0f, 2.5f, NavigationNode.Type.SALLE);
        NavigationNode amphiC = addNode("BP_AC", "Amphi C", bId, 0, 4.5f, 2.5f, NavigationNode.Type.SALLE);
        NavigationNode amphiD = addNode("BP_AD", "Amphi D", bId, 0, 4.5f, 24.5f, NavigationNode.Type.SALLE);

        // ── 2. Points d'accès ──────────────────────────────────────────────────
        NavigationNode entree = addNode("BPAL_RDC_ENTREE", "Entrée", bId, 0, 12.76f, 25.5f, NavigationNode.Type.ENTREE);
        NavigationNode sortie = addNode("BP_SORTIE", "Sortie", bId, 0, 12.0f, 0.5f, NavigationNode.Type.ENTREE);
        NavigationNode escG   = addNode("BP_ESC_G", "Escalier G", bId, 0, 8.0f, 25.4f, NavigationNode.Type.ESCALIER);
        NavigationNode escD   = addNode("BP_ESC_D", "Escalier D", bId, 0, 18.0f, 25.4f, NavigationNode.Type.ESCALIER);

        // ── 3. Architecture du Couloir ─────────────────────────────────────────
        // 4 intersections aux coins
        NavigationNode intHG = addNode("BP_INT_HG", "Int HG", bId, 0, 4.0f,  4.0f,  NavigationNode.Type.CARREFOUR);
        NavigationNode intHD = addNode("BP_INT_HD", "Int HD", bId, 0, 21.0f, 4.0f,  NavigationNode.Type.CARREFOUR);
        NavigationNode intBG = addNode("BP_INT_BG", "Int BG", bId, 0, 4.0f,  22.0f, NavigationNode.Type.CARREFOUR);
        NavigationNode intBD = addNode("BP_INT_BD", "Int BD", bId, 0, 21.0f, 22.0f, NavigationNode.Type.CARREFOUR);

        // Nœuds intermédiaires couloir GAUCHE (x=4.0), alignés sur les salles 105 et 106
        NavigationNode intMG1 = addNode("BP_INT_MG1", "Int MG1", bId, 0, 4.0f, 7.0f,  NavigationNode.Type.CARREFOUR);
        NavigationNode intMG2 = addNode("BP_INT_MG2", "Int MG2", bId, 0, 4.0f, 12.0f, NavigationNode.Type.CARREFOUR);

        // Nœuds intermédiaires couloir DROIT (x=21.0), alignés sur les salles 103 et 102
        NavigationNode intMD1 = addNode("BP_INT_MD1", "Int MD1", bId, 0, 21.0f, 7.0f,  NavigationNode.Type.CARREFOUR);
        NavigationNode intMD2 = addNode("BP_INT_MD2", "Int MD2", bId, 0, 21.0f, 12.0f, NavigationNode.Type.CARREFOUR);

        // ── 4. Connexions Salles & Amphis ──────────────────────────────────────
        // Côté gauche → nœud intermédiaire le plus proche
        NavigationNode.connect(s105, intMG1, 2.25f, "Entrez en 105", "Sortez vers couloir");
        NavigationNode.connect(s106, intMG2, 2.25f, "Entrez en 106", "Sortez vers couloir");
        NavigationNode.connect(s107, intBG,  2.7f,  "Entrez en 107", "Sortez vers couloir");

        // Côté droit → nœud intermédiaire le plus proche
        NavigationNode.connect(s103, intMD1, 2.7f, "Entrez en 103", "Sortez vers couloir");
        NavigationNode.connect(s102, intMD2, 2.7f, "Entrez en 102", "Sortez vers couloir");
        NavigationNode.connect(s101, intBD,  1.5f, "Entrez en 101", "Sortez vers couloir");

        // Côté haut
        NavigationNode.connect(s104,   intHD, 5.0f, "Entrez en 104",       "Sortez vers couloir");
        NavigationNode.connect(sortie, intHG, 8.0f, "Allez vers la sortie", "Entrez dans le bloc");

        // Amphis → intersections aux coins (restent inchangés)
        NavigationNode.connect(amphiC, intHG, 1.5f, "Entrez Amphi C", "Sortez vers couloir");
        NavigationNode.connect(amphiD, intBG, 1.5f, "Entrez Amphi D", "Sortez vers couloir");
        NavigationNode.connect(amphiB, intHD, 1.5f, "Entrez Amphi B", "Sortez vers couloir");
        NavigationNode.connect(amphiA, intBD, 1.5f, "Entrez Amphi A", "Sortez vers couloir");

        // ── 5. Couloirs horizontaux (inchangés) ────────────────────────────────
        NavigationNode.connect(intHG, intHD, 17.0f, "Allez tout droit", "Allez tout droit");
        NavigationNode.connect(intBD, intBG, 17.0f, "Allez tout droit", "Allez tout droit");

        // ── 6. Couloir GAUCHE segmenté : HG → MG1 → MG2 → BG (total 18 m) ───
        NavigationNode.connect(intHG,  intMG1,  3.0f,  "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intMG1, intMG2,  5.0f,  "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intMG2, intBG,   10.0f, "Continuez tout droit", "Continuez tout droit");

        // ── 7. Couloir DROIT segmenté : HD → MD1 → MD2 → BD (total 18 m) ────
        NavigationNode.connect(intHD,  intMD1,  3.0f,  "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intMD1, intMD2,  5.0f,  "Continuez tout droit", "Continuez tout droit");
        NavigationNode.connect(intMD2, intBD,   10.0f, "Continuez tout droit", "Continuez tout droit");

        // ── 8. Entrée et Escaliers ─────────────────────────────────────────────
        NavigationNode.connect(entree, intBG, 8.5f, "Tournez à gauche", "Allez vers l'entrée");
        NavigationNode.connect(entree, intBD, 8.5f, "Tournez à droite", "Allez vers l'entrée");
        NavigationNode.connect(escG,   intBG, 1.5f, "Accédez à l'escalier", "Vers couloir");
        NavigationNode.connect(escD,   intBD, 1.5f, "Accédez à l'escalier", "Vers couloir");

        Log.d("GRAPH", "Total nodes = " + nodes.size());
        Log.d("GRAPH", "Entrée voisins = " + entree.voisins.size());
        Log.d("GRAPH", "Salle 105 voisins = " + s105.voisins.size());
    }

    // =========================================================
    // BLOC MATH — RDC
    // Référentiel portrait : x=largeur(0→17.21m), y=longueur(0→48.27m)
    // Origine : coin bas-gauche (Entrée = y=0)
    // Couloir central : x=8.59
    // =========================================================
    private void buildBlocMath() {
        String bId = "BMATH";

        // Coordonnées converties : y_nav = 48.27 - y_doc (y=0 en haut = sortie, y=48.27 en bas = entrée)
        // ── Salles RDC ────────────────────────────────────────
        NavigationNode s101M = addNode("BMATH_101M", "Salle 101M", bId, 0, 17.21f, 41.70f, NavigationNode.Type.SALLE);
        NavigationNode s102M = addNode("BMATH_102M", "Salle 102M", bId, 0,  0.00f, 41.70f, NavigationNode.Type.SALLE);

        // ── Bureaux (de bas en haut) ──────────────────────────
        NavigationNode bureauG1 = addNode("BMATH_BUREAU_G1", "Bureau", bId, 0,  0.00f, 33.81f, NavigationNode.Type.SALLE);
        NavigationNode bureauD1 = addNode("BMATH_BUREAU_D1", "Bureau", bId, 0, 17.21f, 33.81f, NavigationNode.Type.SALLE);
        NavigationNode bureauG2 = addNode("BMATH_BUREAU_G2", "Bureau", bId, 0,  0.00f, 25.70f, NavigationNode.Type.SALLE);
        NavigationNode bureauD2 = addNode("BMATH_BUREAU_D2", "Bureau", bId, 0, 17.21f, 25.70f, NavigationNode.Type.SALLE);
        NavigationNode bureauG3 = addNode("BMATH_BUREAU_G3", "Bureau", bId, 0,  0.00f, 17.59f, NavigationNode.Type.SALLE);
        NavigationNode bureauD3 = addNode("BMATH_BUREAU_D3", "Bureau", bId, 0, 17.21f, 17.59f, NavigationNode.Type.SALLE);
        NavigationNode s117M    = addNode("BMATH_117M",      "Salle 117M", bId, 0, 17.21f,  7.31f, NavigationNode.Type.SALLE);
        NavigationNode bureauG4 = addNode("BMATH_BUREAU_G4", "Bureau", bId, 0,  0.00f,  7.31f, NavigationNode.Type.SALLE);

        // ── Points d'entrée/sortie ────────────────────────────
        NavigationNode entree = addNode("BMATH_ENTREE", "Entrée",  bId, 0, 8.59f, 48.27f, NavigationNode.Type.ENTREE);
        NavigationNode sortie = addNode("BMATH_SORTIE", "Sortie",  bId, 0, 8.59f,  0.00f, NavigationNode.Type.ENTREE);

        // ── Intersections (couloir central à x=8.59) ─────────
        NavigationNode intBas  = addNode("BMATH_INT_BAS",  "Couloir Bas",  bId, 0, 8.59f, 41.70f, NavigationNode.Type.CARREFOUR);
        NavigationNode int1    = addNode("BMATH_INT_1",    "Couloir 1",    bId, 0, 8.59f, 33.81f, NavigationNode.Type.CARREFOUR);
        NavigationNode int2    = addNode("BMATH_INT_2",    "Couloir 2",    bId, 0, 8.59f, 25.70f, NavigationNode.Type.CARREFOUR);
        NavigationNode int3    = addNode("BMATH_INT_3",    "Couloir 3",    bId, 0, 8.59f, 17.59f, NavigationNode.Type.CARREFOUR);
        NavigationNode int4    = addNode("BMATH_INT_4",    "Couloir 4",    bId, 0, 8.59f,  7.31f, NavigationNode.Type.CARREFOUR);

        // ── Connexions couloir principal (vertical) ───────────
        NavigationNode.connect(entree, intBas,  6.57f, "Continuez tout droit",       "Continuez vers l'entrée");
        NavigationNode.connect(intBas, int1,    7.89f, "Continuez tout droit",       "Continuez tout droit");
        NavigationNode.connect(int1,   int2,    8.11f, "Continuez tout droit",       "Continuez tout droit");
        NavigationNode.connect(int2,   int3,    8.11f, "Continuez tout droit",       "Continuez tout droit");
        NavigationNode.connect(int3,   int4,   10.28f, "Continuez tout droit",       "Continuez tout droit");
        NavigationNode.connect(int4,   sortie,  7.31f, "Continuez vers la sortie",   "Continuez vers l'entrée");

        // ── Connexions salles ↔ intersections (horizontal) ───
        NavigationNode.connect(intBas, s101M,    8.62f, "Tournez à droite vers 101M",   "Sortez vers le couloir");
        NavigationNode.connect(intBas, s102M,    8.59f, "Tournez à gauche vers 102M",   "Sortez vers le couloir");
        NavigationNode.connect(int1,   bureauD1, 8.62f, "Tournez à droite vers Bureau", "Sortez vers le couloir");
        NavigationNode.connect(int1,   bureauG1, 8.59f, "Tournez à gauche vers Bureau", "Sortez vers le couloir");
        NavigationNode.connect(int2,   bureauD2, 8.62f, "Tournez à droite vers Bureau", "Sortez vers le couloir");
        NavigationNode.connect(int2,   bureauG2, 8.59f, "Tournez à gauche vers Bureau", "Sortez vers le couloir");
        NavigationNode.connect(int3,   bureauD3, 8.62f, "Tournez à droite vers Bureau", "Sortez vers le couloir");
        NavigationNode.connect(int3,   bureauG3, 8.59f, "Tournez à gauche vers Bureau", "Sortez vers le couloir");
        NavigationNode.connect(int4,   s117M,    8.62f, "Tournez à droite vers 117M",   "Sortez vers le couloir");
        NavigationNode.connect(int4,   bureauG4, 8.59f, "Tournez à gauche vers Bureau", "Sortez vers le couloir");
    }

    // =========================================================
    // CAMPUS OUTDOOR — Petite Cour → Cour Rouge → Amphis 1→6
    // Coordonnées outdoor : canvas FsmMapView / 5  (référentiel campus)
    // Coordonnées A1-6 indoor : mètres réels dans le bâtiment (21.65×41.65m)
    //
    //   [PCOUR] ──30m──> [COUR] ──25m──> [A1-6_ENTREE]
    //                                         │
    //              ┌──────┬──────┬────────────┼────────────┬──────┬──────┐
    //            ENT_6  ENT_5  ENT_4        ENT_1        ENT_2  ENT_3
    //              │      │      │             │             │      │
    //           AMPHI_6 AMPHI_5 AMPHI_4    AMPHI_1      AMPHI_2 AMPHI_3
    // =========================================================
    private void buildCampusOutdoor() {

        // ── Nœuds outdoor (blocId = code du bloc dans FsmMapView) ─────────
        NavigationNode pcour = addNode("PCOUR_ENTREE", "Petite Cour", "PCOUR", 0,
                94f, 176f, NavigationNode.Type.ENTREE);
        NavigationNode cour  = addNode("COUR_ENTREE",  "Cour Rouge",  "COUR",  0,
                88f, 145f, NavigationNode.Type.ENTREE);

        // ── Hub d'entrée du bâtiment A1-6 (transition outdoor → indoor) ───
        NavigationNode a16hub = addNode("A1-6_ENTREE", "Entrée Amphis 1→6", "A1-6", 0,
                86f, 117f, NavigationNode.Type.ENTREE);

        // ── Entrées individuelles ──────────────────────────────────────────
        // Mur GAUCHE (x=0) : Amphis 6, 5, 4  — midpoint de chaque ligne
        NavigationNode ent6 = addNode("A16_ENT_6", "Entrée Amphi 6", "A1-6", 0,
                 0f,    6.94f, NavigationNode.Type.ENTREE);
        NavigationNode ent5 = addNode("A16_ENT_5", "Entrée Amphi 5", "A1-6", 0,
                 0f,   20.83f, NavigationNode.Type.ENTREE);
        NavigationNode ent4 = addNode("A16_ENT_4", "Entrée Amphi 4", "A1-6", 0,
                 0f,   34.72f, NavigationNode.Type.ENTREE);
        // Mur DROIT (x=21.65) : Amphis 1, 2, 3
        NavigationNode ent1 = addNode("A16_ENT_1", "Entrée Amphi 1", "A1-6", 0,
                21.65f,  6.94f, NavigationNode.Type.ENTREE);
        NavigationNode ent2 = addNode("A16_ENT_2", "Entrée Amphi 2", "A1-6", 0,
                21.65f, 20.83f, NavigationNode.Type.ENTREE);
        NavigationNode ent3 = addNode("A16_ENT_3", "Entrée Amphi 3", "A1-6", 0,
                21.65f, 34.72f, NavigationNode.Type.ENTREE);

        // ── Salles amphis (centres, grille 2×3 dans 21.65×41.65m) ─────────
        NavigationNode a1 = addNode("A16_AMPHI_1", "Amphi 1", "A1-6", 0,
                16.24f,  6.94f, NavigationNode.Type.SALLE);
        NavigationNode a2 = addNode("A16_AMPHI_2", "Amphi 2", "A1-6", 0,
                16.24f, 20.83f, NavigationNode.Type.SALLE);
        NavigationNode a3 = addNode("A16_AMPHI_3", "Amphi 3", "A1-6", 0,
                16.24f, 34.72f, NavigationNode.Type.SALLE);
        NavigationNode a4 = addNode("A16_AMPHI_4", "Amphi 4", "A1-6", 0,
                 5.41f, 34.72f, NavigationNode.Type.SALLE);
        NavigationNode a5 = addNode("A16_AMPHI_5", "Amphi 5", "A1-6", 0,
                 5.41f, 20.83f, NavigationNode.Type.SALLE);
        NavigationNode a6 = addNode("A16_AMPHI_6", "Amphi 6", "A1-6", 0,
                 5.41f,  6.94f, NavigationNode.Type.SALLE);

        // ── Connexions outdoor ─────────────────────────────────────────────
        NavigationNode.connect(pcour, cour,   30f,
                "Continuez vers la Cour Rouge",
                "Continuez vers la Petite Cour");
        NavigationNode.connect(cour,  a16hub, 25f,
                "Dirigez-vous vers les Amphis 1→6",
                "Retournez vers la Cour Rouge");

        // ── Hub → entrées individuelles ────────────────────────────────────
        NavigationNode.connect(a16hub, ent6, 20f,
                "Entrez par l'entrée Amphi 6", "Sortez vers la Cour Rouge");
        NavigationNode.connect(a16hub, ent5, 22f,
                "Entrez par l'entrée Amphi 5", "Sortez vers la Cour Rouge");
        NavigationNode.connect(a16hub, ent4, 24f,
                "Entrez par l'entrée Amphi 4", "Sortez vers la Cour Rouge");
        NavigationNode.connect(a16hub, ent1, 20f,
                "Entrez par l'entrée Amphi 1", "Sortez vers la Cour Rouge");
        NavigationNode.connect(a16hub, ent2, 22f,
                "Entrez par l'entrée Amphi 2", "Sortez vers la Cour Rouge");
        NavigationNode.connect(a16hub, ent3, 24f,
                "Entrez par l'entrée Amphi 3", "Sortez vers la Cour Rouge");

        // ── Entrées → salles ───────────────────────────────────────────────
        NavigationNode.connect(ent6, a6, 5.41f, "Entrez dans l'Amphi 6", "Sortez de l'Amphi 6");
        NavigationNode.connect(ent5, a5, 5.41f, "Entrez dans l'Amphi 5", "Sortez de l'Amphi 5");
        NavigationNode.connect(ent4, a4, 5.41f, "Entrez dans l'Amphi 4", "Sortez de l'Amphi 4");
        NavigationNode.connect(ent1, a1, 5.41f, "Entrez dans l'Amphi 1", "Sortez de l'Amphi 1");
        NavigationNode.connect(ent2, a2, 5.41f, "Entrez dans l'Amphi 2", "Sortez de l'Amphi 2");
        NavigationNode.connect(ent3, a3, 5.41f, "Entrez dans l'Amphi 3", "Sortez de l'Amphi 3");
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
    // Calcule le chemin optimal via A* (ignore escaliers si même étage).
    public NavPath findPath(String startId, String goalId) {
        NavigationNode start = nodes.get(startId);
        NavigationNode goal  = nodes.get(goalId);
        Log.d("A_STAR", "start = " + startId + " (" + (start != null) + ")");
        Log.d("A_STAR", "goal  = " + goalId + " (" + (goal != null) + ")");
        if (start != null && goal != null) {
            Log.d("A_STAR", "start voisins = " + start.voisins.size());
        }

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

    // Ajoute un nœud au graphe (wrapper public).
    public NavigationNode addNodePublic(String id, String nom, String blocId,
                                        int etage, float x, float y,
                                        NavigationNode.Type type) {
        return addNode(id, nom, blocId, etage, x, y, type);
    }

    // Récupère un nœud par son identifiant.
    public NavigationNode getNode(String id) { return nodes.get(id); }

    // Retourne tous les nœuds du graphe.
    public Map<String, NavigationNode> getAllNodes() { return nodes; }

    // Trouve une salle par nom.
    public NavigationNode findBySalleNom(String nom) {
        for (NavigationNode n : nodes.values())
            if (n.nom.equals(nom) && n.type == NavigationNode.Type.SALLE) return n;
        return null;
    }

    // Trouve le nœud le plus proche des coordonnées (x, y) sur l'étage donné.
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

    // Trouve l'entrée d'un bloc (fallback: premier nœud contenant "entrée").
    public NavigationNode findEntree(String blocId) {
        // Lookup direct : B3 → "B3_RDC_ENTREE", A1-6 → "A1-6_ENTREE"
        NavigationNode n = nodes.get(blocId + "_RDC_ENTREE");
        if (n == null) n = nodes.get(blocId + "_ENTREE");
        if (n != null) return n;
        // Lookup outdoor : PCOUR → "PCOUR_ENTREE", COUR → "COUR_ENTREE"
        n = nodes.get(blocId + "_ENTREE");
        if (n != null) return n;
        // Fallback : premier nœud du bloc contenant "entrée"
        for (NavigationNode node : nodes.values())
            if (node.blocId.equals(blocId) &&
                    (node.nom.toLowerCase().contains("entrée") ||
                            node.nom.toLowerCase().contains("entree")))
                return node;
        return null;
    }

    // Retourne le premier nœud trouvé dans un bloc (fallback si entrée manquante).
    public NavigationNode findAnyNodeInBloc(String blocId) {
        for (NavigationNode n : nodes.values())
            if (n.id.startsWith(blocId)) return n;
        return null;
    }
}
