package com.fsm.navigator.controller;

import com.fsm.navigator.model.NavigationGraph;
import com.fsm.navigator.model.NavigationNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * AStar.java – Algorithme A* de calcul d'itinéraire (couche contrôleur).
 * Opère sur un NavigationGraph via son API publique (getNode).
 */
public class AStar {

    private static class AStarNode {
        NavigationNode node;
        float g, f;
        AStarNode(NavigationNode n, float g, float f) {
            this.node = n; this.g = g; this.f = f;
        }
    }

    // Calcule le chemin optimal via A* (ignore les escaliers si même étage).
    public static NavigationGraph.NavPath findPath(NavigationGraph graph,
                                                   String startId, String goalId) {
        NavigationNode start = graph.getNode(startId);
        NavigationNode goal  = graph.getNode(goalId);

        if (start == null || goal == null) return null;
        if (startId.equals(goalId)) {
            return new NavigationGraph.NavPath(
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
                return reconstructPath(graph, current.node, cameFrom, edgeInst,
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

    // Reconstruit le chemin final (nœuds + instructions) en remontant cameFrom.
    private static NavigationGraph.NavPath reconstructPath(NavigationGraph graph,
                                    NavigationNode goal,
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
                ? allNodes.get(current).node : graph.getNode(current));

        instructions.add("🎉 Vous êtes arrivé à " + goal.nom + " !");
        return new NavigationGraph.NavPath(path, instructions, totalDist);
    }
}
