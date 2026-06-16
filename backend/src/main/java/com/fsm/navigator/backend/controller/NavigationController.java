package com.fsm.navigator.backend.controller;

import com.fsm.navigator.backend.model.*;
import com.fsm.navigator.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class NavigationController {

    @Autowired private BlocRepository              blocRepo;
    @Autowired private WifiFingerprintRepository   fpRepo;
    @Autowired private PointLocalisationRepository poiRepo;

    /**
     * GET /api/blocs
     * Retourne tous les blocs avec étages et salles (pour SearchActivity)
     */
    @GetMapping("/api/blocs")
    public ResponseEntity<List<Bloc>> getAllBlocs() {
        return ResponseEntity.ok(blocRepo.findAll());
    }

    /**
     * GET /api/fingerprints
     * Retourne tous les fingerprints avec infos POI (pour W-kNN)
     *
     * Format réponse :
     * [{ id, bssid, ssid, rssiMoyen, poiId, poiNom, poiType,
     *    salleId, blocCode, x, y, etageNumero }]
     */
   
    /**
     * GET /api/poi
     * Retourne tous les POI (pour le graphe de navigation A*)
     */
    @GetMapping("/api/poi")
    public ResponseEntity<List<PointLocalisation>> getAllPoi() {
        return ResponseEntity.ok(poiRepo.findAll());
    }

    /**
     * GET /api/poi/bloc/{blocId}
     * POI d'un bloc (pour charger le graphe d'un bloc)
     */
    @GetMapping("/api/poi/bloc/{blocId}")
    public ResponseEntity<List<PointLocalisation>> getPoiByBloc(@PathVariable Long blocId) {
        return ResponseEntity.ok(poiRepo.findByBlocIdAll(blocId));
    }

    /**
     * GET /api/salles
     * Liste simplifiée des salles
     */
    @GetMapping("/api/salles")
    public ResponseEntity<List<Salle>> getAllSalles() {
        List<Salle> salles = new ArrayList<>();
        blocRepo.findAll().forEach(bloc ->
            bloc.getEtages().forEach(etage ->
                salles.addAll(etage.getSalles())
            )
        );
        return ResponseEntity.ok(salles);
    }
}