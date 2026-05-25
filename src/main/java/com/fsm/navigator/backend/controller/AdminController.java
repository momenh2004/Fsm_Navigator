package com.fsm.navigator.backend.controller;

import com.fsm.navigator.backend.model.*;
import com.fsm.navigator.backend.repository.*;
import com.fsm.navigator.backend.service.EmailService;
import com.fsm.navigator.backend.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired private OtpService                   otpService;
    @Autowired private EmailService                 emailService;
    @Autowired private UserRepository               userRepo;
    @Autowired private BlocRepository               blocRepo;
    @Autowired private SalleRepository              salleRepo;
    @Autowired private EtageRepository              etageRepo;
    @Autowired private WifiFingerprintRepository    fpRepo;
    @Autowired private PointLocalisationRepository  poiRepo;

    @Value("${admin.email}")
    private String adminEmail;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ===================================================
    // OTP
    // ===================================================

    @PostMapping("/request-otp")
    public ResponseEntity<Map<String, String>> requestOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (!adminEmail.equalsIgnoreCase(email.trim()))
            return ResponseEntity.status(403).body(error("Email non autorisé"));
        String code = otpService.generateOtp(email);
        emailService.sendOtp(email, code);
        return ResponseEntity.ok(success("Code envoyé à " + email));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code  = body.get("code");
        if (otpService.validateOtp(email, code)) {
            Map<String, String> res = new HashMap<>();
            res.put("token",   "ADMIN_" + email);
            res.put("message", "Accès autorisé");
            return ResponseEntity.ok(res);
        }
        return ResponseEntity.status(401).body(error("Code invalide ou expiré"));
    }

    // ===================================================
    // UTILISATEURS
    // ===================================================

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepo.findAll());
    }

    @PostMapping("/users")
    public ResponseEntity<?> addUser(@RequestBody Map<String, String> body) {
        try {
            String email    = body.get("email");
            String password = body.get("password");
            String roleStr  = body.getOrDefault("role", "MEMBRE").toUpperCase();

            if (email == null || email.isEmpty())
                return ResponseEntity.badRequest().body(error("Email requis"));
            if (password == null || password.isEmpty())
                return ResponseEntity.badRequest().body(error("Mot de passe requis"));
            if (userRepo.findByEmail(email).isPresent())
                return ResponseEntity.badRequest().body(error("Email déjà utilisé"));

            User user;
            if ("ADMIN".equals(roleStr)) {
                user = new Admin(email, passwordEncoder.encode(password));
            } else {
                String nom    = body.getOrDefault("nom", "");
                String prenom = body.getOrDefault("prenom", "");
                user = new Membre(email, passwordEncoder.encode(password), nom, prenom);
            }

            return ResponseEntity.ok(userRepo.save(user));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(error("Erreur: " + e.getMessage()));
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                         @RequestBody Map<String, String> body) {
        return userRepo.findById(id).map(u -> {
            if (body.containsKey("email"))
                u.setEmail(body.get("email"));
            if (body.containsKey("password") && !body.get("password").isEmpty())
                u.setPassword(passwordEncoder.encode(body.get("password")));
            if (u instanceof Membre membre) {
                if (body.containsKey("nom"))    membre.setNom(body.get("nom"));
                if (body.containsKey("prenom")) membre.setPrenom(body.get("prenom"));
            }
            return ResponseEntity.ok(userRepo.save(u));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        if (!userRepo.existsById(id)) return ResponseEntity.notFound().build();
        userRepo.deleteById(id);
        return ResponseEntity.ok(success("Utilisateur supprimé"));
    }

    // ===================================================
    // BLOCS
    // ===================================================

    @GetMapping("/blocs")
    public ResponseEntity<List<Bloc>> getAllBlocs() {
        return ResponseEntity.ok(blocRepo.findAll());
    }

    @PostMapping("/blocs")
    public ResponseEntity<?> addBloc(@RequestBody Map<String, Object> body) {
        try {
            String  code = (String) body.get("code");
            String  nom  = (String) body.get("nom");
            String  desc = (String) body.getOrDefault("description", "");
            boolean pmr  = Boolean.TRUE.equals(body.get("accessiblePmr"));

            if (code == null || code.isEmpty())
                return ResponseEntity.badRequest().body(error("Code requis"));
            if (nom == null || nom.isEmpty())
                return ResponseEntity.badRequest().body(error("Nom requis"));
            if (blocRepo.findByCode(code).isPresent())
                return ResponseEntity.badRequest().body(error("Code déjà utilisé"));

            Bloc bloc = new Bloc(code, nom, desc, pmr);
            return ResponseEntity.ok(blocRepo.save(bloc));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(error("Erreur: " + e.getMessage()));
        }
    }

    @PutMapping("/blocs/{id}")
    public ResponseEntity<?> updateBloc(@PathVariable Long id,
                                         @RequestBody Map<String, Object> body) {
        return blocRepo.findById(id).map(b -> {
            if (body.containsKey("code"))         b.setCode((String) body.get("code"));
            if (body.containsKey("nom"))          b.setNom((String) body.get("nom"));
            if (body.containsKey("description"))  b.setDescription((String) body.get("description"));
            if (body.containsKey("accessiblePmr"))
                b.setAccessiblePmr(Boolean.TRUE.equals(body.get("accessiblePmr")));
            return ResponseEntity.ok(blocRepo.save(b));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/blocs/{id}")
    public ResponseEntity<Map<String, String>> deleteBloc(@PathVariable Long id) {
        if (!blocRepo.existsById(id)) return ResponseEntity.notFound().build();

        List<Etage> etages = etageRepo.findByBlocId(id);
        for (Etage etage : etages) {
            poiRepo.findByEtage_Id(etage.getId()).forEach(poi -> {
                fpRepo.findByPoi_Id(poi.getId()).forEach(fp -> fpRepo.deleteById(fp.getId()));
                poiRepo.deleteById(poi.getId());
            });
            salleRepo.findByEtageId(etage.getId()).forEach(salle -> {
                poiRepo.findBySalle_Id(salle.getId()).forEach(poi -> {
                    fpRepo.findByPoi_Id(poi.getId()).forEach(fp -> fpRepo.deleteById(fp.getId()));
                    poiRepo.deleteById(poi.getId());
                });
                salleRepo.deleteById(salle.getId());
            });
            etageRepo.deleteById(etage.getId());
        }
        poiRepo.findByBloc_Id(id).forEach(poi -> {
            fpRepo.findByPoi_Id(poi.getId()).forEach(fp -> fpRepo.deleteById(fp.getId()));
            poiRepo.deleteById(poi.getId());
        });

        blocRepo.deleteById(id);
        return ResponseEntity.ok(success("Bloc supprimé"));
    }

    // ===================================================
    // ÉTAGES
    // ===================================================

    @GetMapping("/etages")
    public ResponseEntity<List<Etage>> getAllEtages() {
        return ResponseEntity.ok(etageRepo.findAll());
    }

    @GetMapping("/etages/bloc/{blocId}")
    public ResponseEntity<List<Etage>> getEtagesByBloc(@PathVariable Long blocId) {
        return ResponseEntity.ok(etageRepo.findByBlocId(blocId));
    }

    @PostMapping("/etages")
    public ResponseEntity<?> addEtage(@RequestBody Map<String, Object> body) {
        try {
            int     numero = Integer.parseInt(body.get("numero").toString());
            String  label  = (String) body.getOrDefault("label", "Étage " + numero);
            boolean pmr    = Boolean.TRUE.equals(body.get("accessiblePmr"));
            Long    blocId = Long.parseLong(body.get("blocId").toString());

            Bloc bloc = blocRepo.findById(blocId).orElse(null);
            if (bloc == null)
                return ResponseEntity.badRequest().body(error("Bloc introuvable"));

            Etage etage = new Etage(numero, label, pmr, bloc);
            return ResponseEntity.ok(etageRepo.save(etage));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(error("Erreur: " + e.getMessage()));
        }
    }

    @PutMapping("/etages/{id}")
    public ResponseEntity<?> updateEtage(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body) {
        return etageRepo.findById(id).map(e -> {
            if (body.containsKey("label"))  e.setLabel((String) body.get("label"));
            if (body.containsKey("numero")) e.setNumero(Integer.parseInt(body.get("numero").toString()));
            if (body.containsKey("accessiblePmr"))
                e.setAccessiblePmr(Boolean.TRUE.equals(body.get("accessiblePmr")));
            return ResponseEntity.ok(etageRepo.save(e));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/etages/{id}")
    public ResponseEntity<Map<String, String>> deleteEtage(@PathVariable Long id) {
        if (!etageRepo.existsById(id)) return ResponseEntity.notFound().build();
        etageRepo.deleteById(id);
        return ResponseEntity.ok(success("Étage supprimé"));
    }

    // ===================================================
    // SALLES
    // ===================================================

    @GetMapping("/salles")
    public ResponseEntity<List<Salle>> getAllSalles() {
        return ResponseEntity.ok(salleRepo.findAll());
    }

    @PostMapping("/salles")
    public ResponseEntity<?> addSalle(@RequestBody Map<String, Object> body) {
        try {
            String  nom       = (String) body.get("nom");
            String  catStr    = (String) body.getOrDefault("categorie", "SALLE_ETUDE");
            boolean pmr       = Boolean.TRUE.equals(body.get("accessiblePmr"));
            Long    etageId   = Long.parseLong(body.get("etageId").toString());

            if (nom == null || nom.isEmpty())
                return ResponseEntity.badRequest().body(error("Nom requis"));

            Etage etage = etageRepo.findById(etageId).orElse(null);
            if (etage == null)
                return ResponseEntity.badRequest().body(error("Étage introuvable"));

            CategorieSalle categorie;
            try { categorie = CategorieSalle.valueOf(catStr.toUpperCase()); }
            catch (Exception e) { categorie = CategorieSalle.SALLE_ETUDE; }

            Salle salle = new Salle(nom, categorie, pmr, etage);
            if (body.containsKey("disponible"))
                salle.setDisponible(Boolean.TRUE.equals(body.get("disponible")));
            Salle saved = salleRepo.save(salle);

            // Créer automatiquement un POI de type SALLE
            PointLocalisation poi = new PointLocalisation(nom, 0f, 0f, pmr, saved);
            poiRepo.save(poi);

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(error("Erreur: " + e.getMessage()));
        }
    }

    @PutMapping("/salles/{id}")
    public ResponseEntity<?> updateSalle(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body) {
        return salleRepo.findById(id).map(s -> {
            if (body.containsKey("nom"))
                s.setNom((String) body.get("nom"));
            if (body.containsKey("categorie")) {
                try {
                    s.setCategorie(CategorieSalle.valueOf(
                        body.get("categorie").toString().toUpperCase()));
                } catch (Exception ignored) {}
            }
            if (body.containsKey("accessiblePmr"))
                s.setAccessiblePmr(Boolean.TRUE.equals(body.get("accessiblePmr")));
            if (body.containsKey("disponible"))
                s.setDisponible(Boolean.TRUE.equals(body.get("disponible")));
            return ResponseEntity.ok(salleRepo.save(s));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/salles/{id}")
    public ResponseEntity<Map<String, String>> deleteSalle(@PathVariable Long id) {
        if (!salleRepo.existsById(id)) return ResponseEntity.notFound().build();
        salleRepo.deleteById(id);
        return ResponseEntity.ok(success("Salle supprimée"));
    }

    // ===================================================
    // POI
    // ===================================================

    @GetMapping("/poi")
    public ResponseEntity<List<PointLocalisation>> getAllPoi() {
        return ResponseEntity.ok(poiRepo.findAll());
    }

    @GetMapping("/poi/bloc/{blocId}")
    public ResponseEntity<List<PointLocalisation>> getPoiByBloc(@PathVariable Long blocId) {
        return ResponseEntity.ok(poiRepo.findByBlocIdAll(blocId));
    }

    @GetMapping("/poi/etage/{etageId}")
    public ResponseEntity<List<PointLocalisation>> getPoiByEtage(@PathVariable Long etageId) {
        return ResponseEntity.ok(poiRepo.findByEtageIdAll(etageId));
    }

    @PostMapping("/poi")
    public ResponseEntity<?> addPoi(@RequestBody Map<String, Object> body) {
        try {
            String  nom   = (String) body.get("nom");
            String  typeS = (String) body.get("type");
            float   x     = body.containsKey("x") ? Float.parseFloat(body.get("x").toString()) : 0f;
            float   y     = body.containsKey("y") ? Float.parseFloat(body.get("y").toString()) : 0f;
            boolean pmr   = Boolean.TRUE.equals(body.get("accessiblePmr"));

            if (nom == null || nom.isEmpty())
                return ResponseEntity.badRequest().body(error("Nom requis"));
            if (typeS == null)
                return ResponseEntity.badRequest().body(error("Type requis"));

            PointLocalisation.Type type;
            try { type = PointLocalisation.Type.valueOf(typeS.toUpperCase()); }
            catch (Exception e) { return ResponseEntity.badRequest().body(error("Type invalide")); }

            PointLocalisation poi = new PointLocalisation();
            poi.setNom(nom);
            poi.setType(type);
            poi.setX(x);
            poi.setY(y);
            poi.setAccessiblePmr(pmr);

            switch (type) {
                case SALLE -> {
                    Long salleId = Long.parseLong(body.get("salleId").toString());
                    Salle salle = salleRepo.findById(salleId).orElse(null);
                    if (salle == null) return ResponseEntity.badRequest().body(error("Salle introuvable"));
                    poi.setSalle(salle);
                }
                case COULOIR, ESCALIER-> {
                    Long etageId = Long.parseLong(body.get("etageId").toString());
                    Etage etage = etageRepo.findById(etageId).orElse(null);
                    if (etage == null) return ResponseEntity.badRequest().body(error("Étage introuvable"));
                    poi.setEtage(etage);
                }
                case ENTREE, SORTIE, INTERSECTION -> {
                    Long blocId = Long.parseLong(body.get("blocId").toString());
                    Bloc bloc = blocRepo.findById(blocId).orElse(null);
                    if (bloc == null) return ResponseEntity.badRequest().body(error("Bloc introuvable"));
                    poi.setBloc(bloc);
                }
            }

            return ResponseEntity.ok(poiRepo.save(poi));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(error("Erreur: " + e.getMessage()));
        }
    }

    @PutMapping("/poi/{id}")
    public ResponseEntity<?> updatePoi(@PathVariable Long id,
                                        @RequestBody Map<String, Object> body) {
        return poiRepo.findById(id).map(p -> {
            if (body.containsKey("nom"))           p.setNom((String) body.get("nom"));
            if (body.containsKey("x"))             p.setX(Float.parseFloat(body.get("x").toString()));
            if (body.containsKey("y"))             p.setY(Float.parseFloat(body.get("y").toString()));
            if (body.containsKey("accessiblePmr")) p.setAccessiblePmr(Boolean.TRUE.equals(body.get("accessiblePmr")));
            return ResponseEntity.ok(poiRepo.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/poi/{id}")
    public ResponseEntity<Map<String, String>> deletePoi(@PathVariable Long id) {
        if (!poiRepo.existsById(id)) return ResponseEntity.notFound().build();
        poiRepo.deleteById(id);
        return ResponseEntity.ok(success("POI supprimé"));
    }

    // ===================================================
    // FINGERPRINTS
    // ===================================================

    @GetMapping("/fingerprints")
    public ResponseEntity<List<WifiFingerprint>> getAllFingerprints() {
        return ResponseEntity.ok(fpRepo.findAll());
    }

    @GetMapping("/fingerprints/bloc/{blocId}")
    public ResponseEntity<List<WifiFingerprint>> getFingerprintsByBloc(@PathVariable Long blocId) {
        return ResponseEntity.ok(fpRepo.findByBlocId(blocId));
    }

    @PostMapping("/fingerprints")
    public ResponseEntity<?> addFingerprint(@RequestBody Map<String, Object> body) {
        try {
            String bssid = (String) body.get("bssid");
            String ssid  = (String) body.getOrDefault("ssid", "");
            double rssi  = Double.parseDouble(body.get("rssiMoyen").toString());
            Long   poiId = Long.parseLong(body.get("poiId").toString());

            if (bssid == null || bssid.isEmpty())
                return ResponseEntity.badRequest().body(error("BSSID requis"));

            PointLocalisation poi = poiRepo.findById(poiId).orElse(null);
            if (poi == null)
                return ResponseEntity.badRequest().body(error("POI introuvable"));

            WifiFingerprint fp = new WifiFingerprint(bssid.toLowerCase().trim(), ssid, rssi, poi);
            return ResponseEntity.ok(fpRepo.save(fp));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(error("Erreur: " + e.getMessage()));
        }
    }

    @PutMapping("/fingerprints/{id}")
    public ResponseEntity<?> updateFingerprint(@PathVariable Long id,
                                                @RequestBody Map<String, Object> body) {
        return fpRepo.findById(id).map(f -> {
            if (body.containsKey("bssid"))
                f.setBssid(((String) body.get("bssid")).toLowerCase().trim());
            if (body.containsKey("ssid"))
                f.setSsid((String) body.get("ssid"));
            if (body.containsKey("rssiMoyen"))
                f.setRssiMoyen(Double.parseDouble(body.get("rssiMoyen").toString()));
            return ResponseEntity.ok(fpRepo.save(f));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/fingerprints/{id}")
    public ResponseEntity<Map<String, String>> deleteFingerprint(@PathVariable Long id) {
        if (!fpRepo.existsById(id)) return ResponseEntity.notFound().build();
        fpRepo.deleteById(id);
        return ResponseEntity.ok(success("Fingerprint supprimé"));
    }

    // ===================================================
    // EXPORT / IMPORT BLOCS
    // ===================================================

    @GetMapping("/blocs/{id}/export")
    public ResponseEntity<?> exportBloc(@PathVariable Long id) {
        Bloc bloc = blocRepo.findById(id).orElse(null);
        if (bloc == null) return ResponseEntity.notFound().build();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code",         bloc.getCode());
        result.put("nom",          bloc.getNom());
        result.put("description",  bloc.getDescription());
        result.put("accessiblePmr", bloc.isAccessiblePmr());

        List<Map<String, Object>> etagesList = new ArrayList<>();
        for (Etage etage : etageRepo.findByBlocId(bloc.getId())) {
            Map<String, Object> em = new LinkedHashMap<>();
            em.put("numero",       etage.getNumero());
            em.put("label",        etage.getLabel());
            em.put("accessiblePmr", etage.isAccessiblePmr());

            List<Map<String, Object>> sallesList = new ArrayList<>();
            for (Salle salle : salleRepo.findByEtageId(etage.getId())) {
                Map<String, Object> sm = new LinkedHashMap<>();
                sm.put("nom",               salle.getNom());
                sm.put("categorie",         salle.getCategorie().name());
                sm.put("accessiblePmr",     salle.isAccessiblePmr());
                sm.put("disponible",        salle.isDisponible());
                sallesList.add(sm);
            }
            em.put("salles", sallesList);
            etagesList.add(em);
        }
        result.put("etages", etagesList);
        return ResponseEntity.ok(result);
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/blocs/import")
    public ResponseEntity<?> importBloc(@RequestBody Map<String, Object> body) {
        try {
            String  code = (String) body.get("code");
            String  nom  = (String) body.get("nom");
            String  desc = (String) body.getOrDefault("description", "");
            boolean pmr  = Boolean.TRUE.equals(body.get("accessiblePmr"));

            if (code == null || code.isEmpty())
                return ResponseEntity.badRequest().body(error("Code requis"));
            if (nom == null || nom.isEmpty())
                return ResponseEntity.badRequest().body(error("Nom requis"));
            if (blocRepo.findByCode(code).isPresent())
                return ResponseEntity.badRequest().body(error("Code déjà utilisé : " + code));

            Bloc bloc = blocRepo.save(new Bloc(code, nom, desc, pmr));

            List<Map<String, Object>> etages = (List<Map<String, Object>>) body.getOrDefault("etages", List.of());
            for (Map<String, Object> em : etages) {
                int     numero    = Integer.parseInt(em.getOrDefault("numero", 0).toString());
                String  label     = (String) em.getOrDefault("label", "Étage " + numero);
                boolean etPmr     = Boolean.TRUE.equals(em.get("accessiblePmr"));
                Etage   etage     = etageRepo.save(new Etage(numero, label, etPmr, bloc));

                List<Map<String, Object>> salles = (List<Map<String, Object>>) em.getOrDefault("salles", List.of());
                for (Map<String, Object> sm : salles) {
                    String sNom   = (String) sm.getOrDefault("nom", "");
                    String catStr = (String) sm.getOrDefault("categorie", "SALLE_ETUDE");
                    boolean sPmr  = Boolean.TRUE.equals(sm.get("accessiblePmr"));
                    boolean disp  = !Boolean.FALSE.equals(sm.get("disponible"));

                    CategorieSalle cat;
                    try { cat = CategorieSalle.valueOf(catStr.toUpperCase()); }
                    catch (Exception e) { cat = CategorieSalle.SALLE_ETUDE; }

                    Salle salle = new Salle(sNom, cat, sPmr, etage);
                    salle.setDisponible(disp);
                    Salle saved = salleRepo.save(salle);
                    poiRepo.save(new PointLocalisation(sNom, 0f, 0f, sPmr, saved));
                }
            }

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("message", "Bloc importé avec succès");
            res.put("blocId",  bloc.getId());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(error("Erreur: " + e.getMessage()));
        }
    }

    // ===================================================
    // HELPERS
    // ===================================================
    private Map<String, String> error(String msg) {
        Map<String, String> r = new HashMap<>(); r.put("message", msg); return r;
    }
    private Map<String, String> success(String msg) {
        Map<String, String> r = new HashMap<>(); r.put("message", msg); return r;
    }
}