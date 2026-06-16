package com.fsm.navigator.backend.controller;

import com.fsm.navigator.backend.model.Membre;
import com.fsm.navigator.backend.model.Favori;
import com.fsm.navigator.backend.model.Salle;
import com.fsm.navigator.backend.model.User;
import com.fsm.navigator.backend.repository.FavoriRepository;
import com.fsm.navigator.backend.repository.SalleRepository;
import com.fsm.navigator.backend.repository.UserRepository;
import com.fsm.navigator.backend.security.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RestController
@RequestMapping("/api/favoris")
@CrossOrigin(origins = "*")
public class FavoriController {

    private static final Logger log = LoggerFactory.getLogger(FavoriController.class);

    @Autowired private FavoriRepository favoriRepo;
    @Autowired private SalleRepository  salleRepo;
    @Autowired private UserRepository   userRepo;
    @Autowired private JwtUtil          jwtUtil;

    // Récupérer le membre depuis le token JWT
    private Membre getMembreFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            String email = jwtUtil.extractEmail(authHeader.substring(7));
            Optional<User> userOpt = userRepo.findByEmail(email);
            if (userOpt.isEmpty() || !(userOpt.get() instanceof Membre)) return null;
            return (Membre) userOpt.get();
        } catch (Exception e) {
            log.warn("JWT invalide ou expiré : {}", e.getMessage());
            return null;
        }
    }

    // ── GET /api/favoris ──────────────────────────────────────
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getFavoris(
            @RequestHeader("Authorization") String authHeader) {

        Membre membre = getMembreFromToken(authHeader);
        if (membre == null)
            return ResponseEntity.status(401).build();

        List<Favori> favoris = favoriRepo.findByMembre(membre);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Favori f : favoris) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id",        f.getId());
            map.put("salleId",   f.getSalle().getId());
            map.put("salleNom",  f.getSalle().getNom());
            map.put("blocCode",  f.getSalle().getEtage().getBloc().getCode());
            map.put("blocNom",   f.getSalle().getEtage().getBloc().getNom());
            map.put("createdAt", f.getCreatedAt().toString());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    // ── POST /api/favoris ─────────────────────────────────────
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> addFavori(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {

        Membre membre = getMembreFromToken(authHeader);
        if (membre == null)
            return ResponseEntity.status(401).build();

        Object salleIdRaw = body.get("salleId");
        if (salleIdRaw == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("message", "salleId manquant dans la requête");
            return ResponseEntity.badRequest().body(err);
        }

        Long salleId;
        try {
            salleId = Long.parseLong(salleIdRaw.toString());
        } catch (NumberFormatException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("message", "salleId invalide : " + salleIdRaw);
            return ResponseEntity.badRequest().body(err);
        }

        Map<String, Object> response = new LinkedHashMap<>();

        if (favoriRepo.existsByMembreAndSalle_Id(membre, salleId)) {
            response.put("message", "Déjà en favori");
            return ResponseEntity.ok(response);
        }

        Salle salle = salleRepo.findById(salleId).orElse(null);
        if (salle == null) return ResponseEntity.notFound().build();

        Favori f = new Favori(membre, salle);
        Favori saved = favoriRepo.save(f);

        response.put("id",      saved.getId());
        response.put("message", "Ajouté aux favoris");
        return ResponseEntity.ok(response);
    }

    // ── DELETE /api/favoris/{id} ──────────────────────────────
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteFavori(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        Membre membre = getMembreFromToken(authHeader);
        if (membre == null)
            return ResponseEntity.status(401).build();

        Map<String, Object> response = new LinkedHashMap<>();
        Optional<Favori> opt = favoriRepo.findById(id);

        if (opt.isEmpty()) {
            response.put("message", "Favori introuvable");
            return ResponseEntity.notFound().build();
        }

        Favori f = opt.get();
        if (!f.getMembre().getId().equals(membre.getId())) {
            response.put("message", "Non autorisé");
            return ResponseEntity.status(403).body(response);
        }

        favoriRepo.delete(f);
        response.put("message", "Supprimé des favoris");
        return ResponseEntity.ok(response);
    }

    // ── GET /api/favoris/check ────────────────────────────────
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkFavori(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long salleId) {

        Membre membre = getMembreFromToken(authHeader);
        if (membre == null)
            return ResponseEntity.status(401).build();

        Map<String, Object> response = new LinkedHashMap<>();
        boolean isFavori = favoriRepo.existsByMembreAndSalle_Id(membre, salleId);
        Long favoriId = null;

        if (isFavori) {
            favoriId = favoriRepo.findByMembreAndSalle_Id(membre, salleId)
                    .map(Favori::getId).orElse(null);
        }

        response.put("isFavori", isFavori);
        response.put("favoriId", favoriId);
        return ResponseEntity.ok(response);
    }
}
