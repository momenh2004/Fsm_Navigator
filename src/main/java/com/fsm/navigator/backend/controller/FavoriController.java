package com.fsm.navigator.backend.controller;

import com.fsm.navigator.backend.model.Bloc;
import com.fsm.navigator.backend.model.Favori;
import com.fsm.navigator.backend.model.Salle;
import com.fsm.navigator.backend.repository.BlocRepository;
import com.fsm.navigator.backend.repository.FavoriRepository;
import com.fsm.navigator.backend.repository.SalleRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/favoris")
@CrossOrigin(origins = "*")
public class FavoriController {

    @Autowired private FavoriRepository favoriRepo;
    @Autowired private SalleRepository  salleRepo;
    @Autowired private BlocRepository   blocRepo;

    // ── GET /api/favoris?email=xxx ────────────────────────────
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getFavoris(
            @RequestParam String email) {

        List<Favori> favoris = favoriRepo.findByUserEmail(email);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Favori f : favoris) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id",        f.getId());
            map.put("type",      f.getType().name());
            map.put("nom",       f.getNom());
            map.put("createdAt", f.getCreatedAt().toString());

            if (f.getType() == Favori.TypeFavori.SALLE && f.getSalle() != null) {
                map.put("salleId",  f.getSalle().getId());
                map.put("blocCode", f.getSalle().getEtage().getBloc().getCode());
                map.put("blocNom",  f.getSalle().getEtage().getBloc().getNom());
            } else if (f.getType() == Favori.TypeFavori.BLOC && f.getBloc() != null) {
                map.put("blocId",   f.getBloc().getId());
                map.put("blocCode", f.getBloc().getCode());
            }
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    // ── POST /api/favoris ─────────────────────────────────────
    @PostMapping
    public ResponseEntity<Map<String, Object>> addFavori(
            @RequestBody Map<String, Object> body) {

        String email = (String) body.get("email");
        String type  = (String) body.get("type");
        String nom   = (String) body.get("nom");

        Map<String, Object> response = new LinkedHashMap<>();

        if ("SALLE".equals(type)) {
            Long salleId = Long.parseLong(body.get("salleId").toString());

            // Vérifier si déjà en favori
            if (favoriRepo.existsByUserEmailAndSalle_Id(email, salleId)) {
                response.put("message", "Déjà en favori");
                return ResponseEntity.ok(response);
            }

            Salle salle = salleRepo.findById(salleId).orElse(null);
            if (salle == null) return ResponseEntity.notFound().build();

            Favori f = new Favori();
            f.setUserEmail(email);
            f.setType(Favori.TypeFavori.SALLE);
            f.setSalle(salle);
            f.setNom(nom);
            favoriRepo.save(f);

            response.put("id",      f.getId());
            response.put("message", "Ajouté aux favoris");

        } else if ("BLOC".equals(type)) {
            Long blocId = Long.parseLong(body.get("blocId").toString());

            if (favoriRepo.existsByUserEmailAndBloc_Id(email, blocId)) {
                response.put("message", "Déjà en favori");
                return ResponseEntity.ok(response);
            }

            Bloc bloc = blocRepo.findById(blocId).orElse(null);
            if (bloc == null) return ResponseEntity.notFound().build();

            Favori f = new Favori();
            f.setUserEmail(email);
            f.setType(Favori.TypeFavori.BLOC);
            f.setBloc(bloc);
            f.setNom(nom);
            favoriRepo.save(f);

            response.put("id",      f.getId());
            response.put("message", "Ajouté aux favoris");

        } else {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(response);
    }

    // ── DELETE /api/favoris/{id} ──────────────────────────────
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteFavori(
            @PathVariable Long id,
            @RequestParam String email) {

        Map<String, Object> response = new LinkedHashMap<>();
        Optional<Favori> opt = favoriRepo.findById(id);

        if (opt.isEmpty()) {
            response.put("message", "Favori introuvable");
            return ResponseEntity.notFound().build();
        }

        Favori f = opt.get();
        if (!f.getUserEmail().equals(email)) {
            response.put("message", "Non autorisé");
            return ResponseEntity.status(403).body(response);
        }

        favoriRepo.delete(f);
        response.put("message", "Supprimé des favoris");
        return ResponseEntity.ok(response);
    }

    // ── GET /api/favoris/check ────────────────────────────────
    // Vérifier si un élément est déjà en favori
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkFavori(
            @RequestParam String email,
            @RequestParam String type,
            @RequestParam(required = false) Long salleId,
            @RequestParam(required = false) Long blocId) {

        Map<String, Object> response = new LinkedHashMap<>();
        boolean isFavori = false;
        Long favoriId = null;

        if ("SALLE".equals(type) && salleId != null) {
            isFavori = favoriRepo.existsByUserEmailAndSalle_Id(email, salleId);
            if (isFavori) {
                favoriId = favoriRepo.findByUserEmailAndSalle_Id(email, salleId)
                        .map(Favori::getId).orElse(null);
            }
        } else if ("BLOC".equals(type) && blocId != null) {
            isFavori = favoriRepo.existsByUserEmailAndBloc_Id(email, blocId);
            if (isFavori) {
                favoriId = favoriRepo.findByUserEmailAndBloc_Id(email, blocId)
                        .map(Favori::getId).orElse(null);
            }
        }

        response.put("isFavori", isFavori);
        response.put("favoriId", favoriId);
        return ResponseEntity.ok(response);
    }
}