package com.fsm.navigator.backend.controller;

import com.fsm.navigator.backend.model.User;
import com.fsm.navigator.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.fsm.navigator.backend.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * AuthController.java – Endpoints REST d'authentification
 *
 * POST /api/auth/login    → connexion
 * POST /api/auth/register → inscription
 *
 * Réponse JSON : { "token": "...", "email": "...", "role": "..." }
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Autorise les appels depuis Android
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // =========================================================
    // POST /api/auth/login
    // Body : { "email": "...", "password": "..." }
    // =========================================================
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String email    = body.get("email");
        String password = body.get("password");

        // Chercher l'utilisateur en base
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(error("Email ou mot de passe incorrect"));
        }

        User user = userOpt.get();

        // Vérifier le mot de passe
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401)
                    .body(error("Email ou mot de passe incorrect"));
        }

        // Générer le token JWT
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return ResponseEntity.ok(success(token, user.getEmail(), user.getRole().name()));
    }

    // =========================================================
    // POST /api/auth/register
    // Body : { "email": "...", "password": "...", "role": "ETUDIANT" }
    // =========================================================
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> body) {
        String email    = body.get("email");
        String password = body.get("password");
        String roleStr  = body.getOrDefault("role", "ETUDIANT");

        // Vérifier si l'email existe déjà
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(409)
                    .body(error("Cet email est déjà utilisé"));
        }

        // Valider l'email
        if (email == null || !email.contains("@")) {
            return ResponseEntity.status(400)
                    .body(error("Adresse email invalide"));
        }

        // Valider le mot de passe
        if (password == null || password.length() < 6) {
            return ResponseEntity.status(400)
                    .body(error("Le mot de passe doit contenir au moins 6 caractères"));
        }

        // Créer et sauvegarder l'utilisateur
        User newUser = new User(
                email,
                passwordEncoder.encode(password), // Hash BCrypt
                User.Role.valueOf(roleStr)
        );
        userRepository.save(newUser);

        // Générer le token JWT
        String token = jwtUtil.generateToken(email, roleStr);

        return ResponseEntity.status(201)
                .body(success(token, email, roleStr));
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private Map<String, String> success(String token, String email, String role) {
        Map<String, String> res = new HashMap<>();
        res.put("token", token);
        res.put("email", email);
        res.put("role", role);
        return res;
    }

    private Map<String, String> error(String message) {
        Map<String, String> res = new HashMap<>();
        res.put("message", message);
        return res;
    }
    // =====================================================
// À AJOUTER dans AuthController.java
// Ajoute cette méthode dans la classe AuthController
// =====================================================

    // =========================================================
    // POST /api/auth/change-password
    // Header : Authorization: Bearer <token>
    // Body   : { "oldPassword": "...", "newPassword": "..." }
    // =========================================================
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {

        // Extraire le token du header "Bearer <token>"
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(error("Token manquant"));
        }
        String token = authHeader.substring(7);

        // Valider le token
        if (!jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(401).body(error("Token invalide ou expiré"));
        }

        // Extraire l'email depuis le token
        String email = jwtUtil.extractEmail(token);

        // Récupérer l'utilisateur
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(error("Utilisateur introuvable"));
        }

        User user = userOpt.get();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        // Vérifier l'ancien mot de passe
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return ResponseEntity.status(400).body(error("Ancien mot de passe incorrect"));
        }

        // Valider le nouveau mot de passe
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.status(400)
                    .body(error("Le nouveau mot de passe doit contenir au moins 6 caractères"));
        }

        // Sauvegarder le nouveau mot de passe hashé
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        Map<String, String> res = new HashMap<>();
        res.put("message", "Mot de passe modifié avec succès");
        return ResponseEntity.ok(res);
    }
    @DeleteMapping("/account")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @RequestHeader("Authorization") String authHeader) {

        // Extraire et valider le token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(error("Token manquant"));
        }
        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(401).body(error("Token invalide ou expiré"));
        }

        // Extraire l'email et supprimer l'utilisateur
        String email = jwtUtil.extractEmail(token);
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(error("Utilisateur introuvable"));
        }

        userRepository.delete(userOpt.get());

        Map<String, String> res = new HashMap<>();
        res.put("message", "Compte supprimé avec succès");
        return ResponseEntity.ok(res);
    }
}