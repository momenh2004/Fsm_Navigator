package com.fsm.navigator.backend.controller;

import com.fsm.navigator.backend.model.Etudiant;
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

import java.util.*;

@RestController
@RequestMapping("/api/favoris")
@CrossOrigin(origins = "*")
public class FavoriController {

    @Autowired private FavoriRepository favoriRepo;
    @Autowired private SalleRepository  salleRepo;
    @Autowired private UserRepository   userRepo;
    @Autowired private JwtUtil          jwtUtil;

    // Récupérer l'étudiant depuis le token JWT
    private Etudiant getEtudiantFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String email = jwtUtil.extractEmail(authHeader.substring(7));
        Optional<User> userOpt = userRepo.findByEmail(email);
        if (userOpt.isEmpty() || !(userOpt.get() instanceof Etudiant)) return null;
        return (Etudiant) userOpt.get();
    }

    // ── GET /api/favoris ──────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getFavoris(
            @RequestHeader("Authorization") String authHeader) {

        Etudiant etudiant = getEtudiantFromToken(authHeader);
        if (etudiant == null)
            return ResponseEntity.status(401).build();

        List<Favori> favoris = favoriRepo.findByEtudiant(etudiant);
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
    public ResponseEntity<Map<String, Object>> addFavori(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {

        Etudiant etudiant = getEtudiantFromToken(authHeader);
        if (etudiant == null)
            return ResponseEntity.status(401).build();

        Long salleId = Long.parseLong(body.get("salleId").toString());

        Map<String, Object> response = new LinkedHashMap<>();

        if (favoriRepo.existsByEtudiantAndSalle_Id(etudiant, salleId)) {
            response.put("message", "Déjà en favori");
            return ResponseEntity.ok(response);
        }

        Salle salle = salleRepo.findById(salleId).orElse(null);
        if (salle == null) return ResponseEntity.notFound().build();

        Favori f = new Favori(etudiant, salle);
        favoriRepo.save(f);

        response.put("id",      f.getId());
        response.put("message", "Ajouté aux favoris");
        return ResponseEntity.ok(response);
    }

    // ── DELETE /api/favoris/{id} ──────────────────────────────
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteFavori(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        Etudiant etudiant = getEtudiantFromToken(authHeader);
        if (etudiant == null)
            return ResponseEntity.status(401).build();

        Map<String, Object> response = new LinkedHashMap<>();
        Optional<Favori> opt = favoriRepo.findById(id);

        if (opt.isEmpty()) {
            response.put("message", "Favori introuvable");
            return ResponseEntity.notFound().build();
        }

        Favori f = opt.get();
        if (!f.getEtudiant().getId().equals(etudiant.getId())) {
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

        Etudiant etudiant = getEtudiantFromToken(authHeader);
        if (etudiant == null)
            return ResponseEntity.status(401).build();

        Map<String, Object> response = new LinkedHashMap<>();
        boolean isFavori = favoriRepo.existsByEtudiantAndSalle_Id(etudiant, salleId);
        Long favoriId = null;

        if (isFavori) {
            favoriId = favoriRepo.findByEtudiantAndSalle_Id(etudiant, salleId)
                    .map(Favori::getId).orElse(null);
        }

        response.put("isFavori", isFavori);
        response.put("favoriId", favoriId);
        return ResponseEntity.ok(response);
    }
}