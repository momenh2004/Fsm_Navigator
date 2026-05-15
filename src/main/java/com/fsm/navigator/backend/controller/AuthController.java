package com.fsm.navigator.backend.controller;

import com.fsm.navigator.backend.model.Admin;
import com.fsm.navigator.backend.model.Etudiant;
import com.fsm.navigator.backend.model.User;
import com.fsm.navigator.backend.security.JwtUtil;
import com.fsm.navigator.backend.service.EmailService;
import com.fsm.navigator.backend.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.fsm.navigator.backend.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * AuthController.java
 *
 * POST /api/auth/login          → Admin → OTP ; Etudiant → JWT direct
 * POST /api/auth/verify-otp     → vérifie OTP admin → retourne JWT
 * POST /api/auth/register       → inscription Etudiant
 * POST /api/auth/change-password
 * DELETE /api/auth/account
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil        jwtUtil;
    @Autowired private OtpService     otpService;
    @Autowired private EmailService   emailService;

    @Value("${admin.email}")
    private String adminEmail;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // =========================================================
    // POST /api/auth/login
    // Admin   → OTP envoyé → { requiresOtp: true, email }
    // Etudiant → JWT direct → { token, email, role }
    // =========================================================
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String email    = body.get("email");
        String password = body.get("password");

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty())
            return ResponseEntity.status(401).body(error("Email ou mot de passe incorrect"));

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword()))
            return ResponseEntity.status(401).body(error("Email ou mot de passe incorrect"));

        // Admin → flow OTP
        if (user instanceof Admin) {
            String otp = otpService.generateOtp(email);
            emailService.sendOtp(adminEmail, otp);
            Map<String, Object> res = new HashMap<>();
            res.put("requiresOtp", true);
            res.put("email", email);
            res.put("message", "Code OTP envoyé à " + adminEmail);
            return ResponseEntity.ok(res);
        }

        // Etudiant → JWT direct
        String token = jwtUtil.generateToken(user.getEmail(), user.getRoleAsString());
        return ResponseEntity.ok(successObj(token, user.getEmail(), user.getRoleAsString()));
    }

    // =========================================================
    // POST /api/auth/verify-otp
    // =========================================================
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp   = body.get("otp");

        if (email == null || otp == null)
            return ResponseEntity.status(400).body(error("Email et OTP requis"));

        if (!otpService.validateOtp(email, otp))
            return ResponseEntity.status(401).body(error("Code OTP invalide ou expiré"));

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty())
            return ResponseEntity.status(404).body(error("Utilisateur introuvable"));

        User user = userOpt.get();
        if (!(user instanceof Admin))
            return ResponseEntity.status(403).body(error("Accès réservé aux administrateurs"));

        String token = jwtUtil.generateToken(user.getEmail(), user.getRoleAsString());
        Map<String, Object> res = successObj(token, user.getEmail(), user.getRoleAsString());

        // Objet admin pour le dashboard React
        Map<String, Object> adminObj = new HashMap<>();
        adminObj.put("email", user.getEmail());
        adminObj.put("role",  user.getRoleAsString());
        res.put("admin", adminObj);

        return ResponseEntity.ok(res);
    }

    // =========================================================
    // POST /api/auth/register  → crée un Etudiant
    // =========================================================
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String email    = body.get("email");
        String password = body.get("password");
        String nom      = body.getOrDefault("nom", "");
        String prenom   = body.getOrDefault("prenom", "");

        if (email == null || !email.contains("@"))
            return ResponseEntity.status(400).body(error("Adresse email invalide"));
        if (password == null || password.length() < 6)
            return ResponseEntity.status(400).body(error("Le mot de passe doit contenir au moins 6 caractères"));
        if (userRepository.findByEmail(email).isPresent())
            return ResponseEntity.status(409).body(error("Cet email est déjà utilisé"));

        Etudiant etudiant = new Etudiant(email, passwordEncoder.encode(password), nom, prenom);
        userRepository.save(etudiant);

        String token = jwtUtil.generateToken(email, "ETUDIANT");
        return ResponseEntity.status(201).body(successObj(token, email, "ETUDIANT"));
    }

    // =========================================================
    // POST /api/auth/change-password
    // =========================================================
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {

        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return ResponseEntity.status(401).body(error("Token manquant"));

        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token))
            return ResponseEntity.status(401).body(error("Token invalide ou expiré"));

        String email = jwtUtil.extractEmail(token);
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty())
            return ResponseEntity.status(404).body(error("Utilisateur introuvable"));

        User user = userOpt.get();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (!passwordEncoder.matches(oldPassword, user.getPassword()))
            return ResponseEntity.status(400).body(error("Ancien mot de passe incorrect"));
        if (newPassword == null || newPassword.length() < 6)
            return ResponseEntity.status(400).body(error("Le nouveau mot de passe doit contenir au moins 6 caractères"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Mot de passe modifié avec succès");
        return ResponseEntity.ok(res);
    }

    // =========================================================
    // DELETE /api/auth/account
    // =========================================================
    @DeleteMapping("/account")
    public ResponseEntity<Map<String, Object>> deleteAccount(
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return ResponseEntity.status(401).body(error("Token manquant"));

        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token))
            return ResponseEntity.status(401).body(error("Token invalide ou expiré"));

        String email = jwtUtil.extractEmail(token);
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty())
            return ResponseEntity.status(404).body(error("Utilisateur introuvable"));

        userRepository.delete(userOpt.get());
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Compte supprimé avec succès");
        return ResponseEntity.ok(res);
    }

    // =========================================================
    private Map<String, Object> successObj(String token, String email, String role) {
        Map<String, Object> res = new HashMap<>();
        res.put("token", token);
        res.put("email", email);
        res.put("role",  role);
        return res;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> res = new HashMap<>();
        res.put("message", message);
        return res;
    }
}