package com.fsm.navigator.model;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class NavigationGraphTest {

    private NavigationGraph graph;

    @Before
    public void setUp() {
        graph = new NavigationGraph();
    }

    // ── getNode ───────────────────────────────────────────────────

    @Test
    public void getNode_existingId_returnsCorrectNode() {
        NavigationNode n = graph.getNode("B3_RDC_ENTREE");
        assertNotNull(n);
        assertEquals("B3_RDC_ENTREE", n.id);
    }

    @Test
    public void getNode_unknownId_returnsNull() {
        assertNull(graph.getNode("DOES_NOT_EXIST"));
    }

    // ── getAllNodes ───────────────────────────────────────────────

    @Test
    public void getAllNodes_notEmpty() {
        Map<String, NavigationNode> all = graph.getAllNodes();
        assertNotNull(all);
        assertFalse(all.isEmpty());
    }

    @Test
    public void getAllNodes_containsBothFloors() {
        Map<String, NavigationNode> all = graph.getAllNodes();
        assertTrue(all.containsKey("B3_RDC_ENTREE"));  // RDC
        assertTrue(all.containsKey("B3_E1_ESC"));       // étage 1
    }

    @Test
    public void getAllNodes_containsAllBlocs() {
        Map<String, NavigationNode> all = graph.getAllNodes();
        // Bloc 3
        assertTrue(all.containsKey("B3_RDC_ENTREE"));
        // Bloc Palestine
        assertTrue(all.containsKey("BPAL_RDC_ENTREE"));
        // Campus outdoor
        assertTrue(all.containsKey("PCOUR_ENTREE"));
        assertTrue(all.containsKey("A1-6_ENTREE"));
    }

    // ── findBySalleNom ────────────────────────────────────────────

    @Test
    public void findBySalleNom_existingSalle_returnsNode() {
        NavigationNode n = graph.findBySalleNom("Salle 301");
        assertNotNull(n);
        assertEquals("Salle 301", n.nom);
        assertEquals(NavigationNode.Type.SALLE, n.type);
    }

    @Test
    public void findBySalleNom_etage1Salle_returnsNode() {
        NavigationNode n = graph.findBySalleNom("Salle 309");
        assertNotNull(n);
        assertEquals(1, n.etage);
    }

    @Test
    public void findBySalleNom_nonSalleNodeIgnored() {
        // "Entrée" exists but its type is ENTREE, not SALLE
        assertNull(graph.findBySalleNom("Entrée"));
    }

    @Test
    public void findBySalleNom_unknownName_returnsNull() {
        assertNull(graph.findBySalleNom("Salle Inexistante"));
    }

    // ── findNearest ───────────────────────────────────────────────

    @Test
    public void findNearest_exactCoordinates_returnsMatchingNode() {
        // B3_RDC_ENTREE is at x=8.88, y=30.0
        NavigationNode n = graph.findNearest(8.88f, 30.0f, "B3", 0);
        assertNotNull(n);
        assertEquals("B3_RDC_ENTREE", n.id);
    }

    @Test
    public void findNearest_wrongBloc_returnsNull() {
        assertNull(graph.findNearest(0f, 0f, "NONEXISTENT_BLOC", 0));
    }

    @Test
    public void findNearest_filteredToRequestedFloor() {
        NavigationNode n = graph.findNearest(8.88f, 26.5f, "B3", 1);
        assertNotNull(n);
        assertEquals(1, n.etage);
    }

    @Test
    public void findNearest_returnsClosestNotFurther() {
        // B3_RDC_SORTIE is at (8.88, 0.5); B3_RDC_INT_HG is at (3.8, 2.0)
        // Query near sortie
        NavigationNode n = graph.findNearest(8.88f, 0.5f, "B3", 0);
        assertNotNull(n);
        assertEquals("B3_RDC_SORTIE", n.id);
    }

    // ── findEntree ────────────────────────────────────────────────

    @Test
    public void findEntree_b3_returnsExpectedNode() {
        NavigationNode n = graph.findEntree("B3");
        assertNotNull(n);
        assertEquals("B3_RDC_ENTREE", n.id);
    }

    @Test
    public void findEntree_a16_returnsHub() {
        NavigationNode n = graph.findEntree("A1-6");
        assertNotNull(n);
        assertEquals("A1-6_ENTREE", n.id);
    }

    @Test
    public void findEntree_bpal_returnsEntreeNode() {
        NavigationNode n = graph.findEntree("BPAL");
        assertNotNull(n);
        assertEquals("BPAL_RDC_ENTREE", n.id);
    }

    @Test
    public void findEntree_unknownBloc_returnsNull() {
        assertNull(graph.findEntree("UNKNOWN_BLOC_XYZ"));
    }

    // ── findAnyNodeInBloc ─────────────────────────────────────────

    @Test
    public void findAnyNodeInBloc_b3_returnsNonNull() {
        NavigationNode n = graph.findAnyNodeInBloc("B3_RDC");
        assertNotNull(n);
        assertTrue(n.id.startsWith("B3_RDC"));
    }

    @Test
    public void findAnyNodeInBloc_unknown_returnsNull() {
        assertNull(graph.findAnyNodeInBloc("ZZZZZ_UNKNOWN"));
    }

    // ── findPath — guard clauses ──────────────────────────────────

    @Test
    public void findPath_unknownStart_returnsNull() {
        assertNull(graph.findPath("NOPE", "B3_RDC_ENTREE"));
    }

    @Test
    public void findPath_unknownGoal_returnsNull() {
        assertNull(graph.findPath("B3_RDC_ENTREE", "NOPE"));
    }

    @Test
    public void findPath_bothUnknown_returnsNull() {
        assertNull(graph.findPath("NOPE_A", "NOPE_B"));
    }

    // ── findPath — same-node shortcut ────────────────────────────

    @Test
    public void findPath_sameNode_returnsImmediately() {
        NavigationGraph.NavPath path = graph.findPath("B3_RDC_ENTREE", "B3_RDC_ENTREE");
        assertNotNull(path);
        assertEquals(1, path.nodes.size());
        assertEquals(0f, path.totalDistance, 0.001f);
    }

    @Test
    public void findPath_sameNode_instructionMentionsAlreadyThere() {
        NavigationGraph.NavPath path = graph.findPath("B3_RDC_301", "B3_RDC_301");
        assertNotNull(path);
        assertTrue(path.instructions.get(0).contains("déjà"));
    }

    // ── findPath — same-floor navigation ─────────────────────────

    @Test
    public void findPath_sameFloor_findsPath() {
        NavigationGraph.NavPath path = graph.findPath("B3_RDC_ENTREE", "B3_RDC_SORTIE");
        assertNotNull(path);
        assertFalse(path.nodes.isEmpty());
        assertTrue(path.totalDistance > 0f);
    }

    @Test
    public void findPath_sameFloor_startNodeIsFirst() {
        NavigationGraph.NavPath path = graph.findPath("B3_RDC_ENTREE", "B3_RDC_SORTIE");
        assertNotNull(path);
        assertEquals("B3_RDC_ENTREE", path.nodes.get(0).id);
    }

    @Test
    public void findPath_sameFloor_goalNodeIsLast() {
        NavigationGraph.NavPath path = graph.findPath("B3_RDC_ENTREE", "B3_RDC_SORTIE");
        assertNotNull(path);
        List<NavigationNode> nodes = path.nodes;
        assertEquals("B3_RDC_SORTIE", nodes.get(nodes.size() - 1).id);
    }

    @Test
    public void findPath_sameFloor_noEscalierUsed() {
        NavigationGraph.NavPath path = graph.findPath("B3_RDC_ENTREE", "B3_RDC_301");
        assertNotNull(path);
        boolean usesStairs = path.nodes.stream()
                .anyMatch(n -> n.type == NavigationNode.Type.ESCALIER);
        assertFalse("Same-floor path must not pass through an escalier", usesStairs);
    }

    @Test
    public void findPath_sameFloor_lastInstructionContainsDestName() {
        NavigationGraph.NavPath path = graph.findPath("B3_RDC_ENTREE", "B3_RDC_301");
        assertNotNull(path);
        String last = path.instructions.get(path.instructions.size() - 1);
        assertTrue(last.contains("301"));
    }

    // ── findPath — cross-floor navigation ────────────────────────

    @Test
    public void findPath_crossFloor_findsPath() {
        NavigationGraph.NavPath path = graph.findPath("B3_RDC_ENTREE", "B3_E1_309");
        assertNotNull(path);
        assertFalse(path.nodes.isEmpty());
    }

    @Test
    public void findPath_crossFloor_usesEscalier() {
        NavigationGraph.NavPath path = graph.findPath("B3_RDC_ENTREE", "B3_E1_309");
        assertNotNull(path);
        boolean usesStairs = path.nodes.stream()
                .anyMatch(n -> n.type == NavigationNode.Type.ESCALIER);
        assertTrue("Cross-floor path must pass through an escalier", usesStairs);
    }

    @Test
    public void findPath_crossFloor_goalNodeIsLast() {
        NavigationGraph.NavPath path = graph.findPath("B3_RDC_ENTREE", "B3_E1_309");
        assertNotNull(path);
        List<NavigationNode> nodes = path.nodes;
        assertEquals("B3_E1_309", nodes.get(nodes.size() - 1).id);
    }

    // ── Graph structure integrity ─────────────────────────────────

    @Test
    public void escalierRdcAndE1_areConnected() {
        NavigationNode escRDC = graph.getNode("B3_RDC_ESC");
        NavigationNode escE1  = graph.getNode("B3_E1_ESC");
        assertNotNull(escRDC);
        assertNotNull(escE1);
        boolean connected = escRDC.voisins.stream()
                .anyMatch(e -> e.destination.id.equals("B3_E1_ESC"));
        assertTrue("RDC escalier must be connected to E1 escalier", connected);
    }

    @Test
    public void salleNodes_haveAtLeastOneNeighbour() {
        graph.getAllNodes().values().stream()
                .filter(n -> n.type == NavigationNode.Type.SALLE)
                .forEach(n -> assertTrue(
                        "Salle " + n.id + " has no neighbours", n.voisins.size() >= 1));
    }

    @Test
    public void corridorEdges_areBidirectional() {
        // INT_HG ↔ INT_HD
        NavigationNode hg = graph.getNode("B3_RDC_INT_HG");
        NavigationNode hd = graph.getNode("B3_RDC_INT_HD");
        assertNotNull(hg);
        assertNotNull(hd);

        boolean hgToHd = hg.voisins.stream().anyMatch(e -> e.destination.id.equals("B3_RDC_INT_HD"));
        boolean hdToHg = hd.voisins.stream().anyMatch(e -> e.destination.id.equals("B3_RDC_INT_HG"));
        assertTrue("HG → HD edge missing", hgToHd);
        assertTrue("HD → HG edge missing", hdToHg);
    }
}
