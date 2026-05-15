package com.fsm.navigator.backend.controller;

import com.fsm.navigator.backend.model.Etudiant;
import com.fsm.navigator.backend.model.NavigationHistory;
import com.fsm.navigator.backend.model.Salle;
import com.fsm.navigator.backend.model.User;
import com.fsm.navigator.backend.repository.*;
import com.fsm.navigator.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/stats")
@CrossOrigin(origins = "*")
public class StatsController {

    @Autowired private BlocRepository               blocRepo;
    @Autowired private SalleRepository              salleRepo;
    @Autowired private UserRepository               userRepo;
    @Autowired private FingerprintRepository        fpRepo;
    @Autowired private PointLocalisationRepository  poiRepo;
    @Autowired private NavigationHistoryRepository  histRepo;
    @Autowired private EtageRepository              etageRepo;
    @Autowired private JwtUtil                      jwtUtil;

    // ===================================================
    // CARDS — Chiffres généraux
    // ===================================================
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalBlocs",        blocRepo.count());
        stats.put("totalSalles",       salleRepo.count());
        stats.put("totalUsers",        userRepo.count());
        stats.put("totalFingerprints", fpRepo.count());
        stats.put("totalPoi",          poiRepo.count());
        stats.put("totalNavigations",
            histRepo.countByType(NavigationHistory.TypeHistorique.NAVIGATION));
        stats.put("totalViews",
            histRepo.countByType(NavigationHistory.TypeHistorique.VIEW));
        return ResponseEntity.ok(stats);
    }

    // ===================================================
    // COUVERTURE WIFI PAR BLOC
    // ===================================================
    @GetMapping("/wifi-coverage")
    public ResponseEntity<List<Map<String, Object>>> getWifiCoverage() {
        List<Map<String, Object>> result = new ArrayList<>();
        blocRepo.findAll().forEach(bloc -> {
            long count = fpRepo.findByBlocId(bloc.getId()).size();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("blocCode",     bloc.getCode());
            entry.put("blocNom",      bloc.getNom());
            entry.put("fingerprints", count);
            result.add(entry);
        });
        result.sort((a, b) -> Long.compare(
                (Long) b.get("fingerprints"), (Long) a.get("fingerprints")));
        return ResponseEntity.ok(result);
    }

    // ===================================================
    // DISTRIBUTION RSSI
    // ===================================================
    @GetMapping("/rssi-distribution")
    public ResponseEntity<Map<String, Object>> getRssiDistribution() {
        int[] buckets = new int[5];
        String[] labels = {"< -80", "-80~-70", "-70~-60", "-60~-50", "> -50"};

        fpRepo.findAll().forEach(fp -> {
            double rssi = fp.getRssiMoyen();
            if      (rssi < -80) buckets[0]++;
            else if (rssi < -70) buckets[1]++;
            else if (rssi < -60) buckets[2]++;
            else if (rssi < -50) buckets[3]++;
            else                 buckets[4]++;
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", labels);
        result.put("values", buckets);
        return ResponseEntity.ok(result);
    }

    // ===================================================
    // SALLES SANS FINGERPRINT
    // ===================================================
    @GetMapping("/uncovered-salles")
    public ResponseEntity<List<Map<String, Object>>> getUncoveredSalles() {
        List<Map<String, Object>> result = new ArrayList<>();
        salleRepo.findAll().forEach(salle -> {
            List<com.fsm.navigator.backend.model.PointLocalisation> pois =
                    poiRepo.findBySalle_Id(salle.getId());
            boolean hasFp = false;
            for (var poi : pois) {
                if (!fpRepo.findByPoi_Id(poi.getId()).isEmpty()) { hasFp = true; break; }
            }
            if (!hasFp) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("salleId",  salle.getId());
                entry.put("salleNom", salle.getNom());
                entry.put("etage",    salle.getEtage().getLabel());
                entry.put("bloc",     salle.getEtage().getBloc().getCode());
                result.add(entry);
            }
        });
        return ResponseEntity.ok(result);
    }

    // ===================================================
    // RÉPARTITION DES CATÉGORIES DE SALLES
    // ===================================================
    @GetMapping("/salle-types")
    public ResponseEntity<Map<String, Long>> getSalleTypes() {
        Map<String, Long> types = new LinkedHashMap<>();
        salleRepo.findAll().forEach(s -> {
            String cat = s.getCategorie().name();
            types.put(cat, types.getOrDefault(cat, 0L) + 1);
        });
        return ResponseEntity.ok(types);
    }

    // ===================================================
    // ACCESSIBILITÉ PMR PAR BLOC
    // ===================================================
    @GetMapping("/pmr-coverage")
    public ResponseEntity<List<Map<String, Object>>> getPmrCoverage() {
        List<Map<String, Object>> result = new ArrayList<>();
        blocRepo.findAll().forEach(bloc -> {
            long total = 0, pmr = 0;
            for (var etage : etageRepo.findByBlocId(bloc.getId())) {
                for (var salle : etage.getSalles()) {
                    total++;
                    if (salle.isAccessiblePmr()) pmr++;
                }
            }
            if (total == 0) return;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("blocCode", bloc.getCode());
            entry.put("total",    total);
            entry.put("pmr",      pmr);
            entry.put("pct",      Math.round(pmr * 100.0 / total));
            result.add(entry);
        });
        return ResponseEntity.ok(result);
    }

    // ===================================================
    // SALLES LES PLUS NAVIGUÉES / CONSULTÉES
    // ===================================================
    @GetMapping("/top-navigated")
    public ResponseEntity<List<Map<String, Object>>> getTopNavigated() {
        List<Map<String, Object>> result = new ArrayList<>();
        histRepo.findTopNavigated().stream().limit(10).forEach(row -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("salleNom", row[0]);
            entry.put("blocNom",  row[1]);
            entry.put("count",    row[2]);
            result.add(entry);
        });
        return ResponseEntity.ok(result);
    }

    @GetMapping("/top-viewed")
    public ResponseEntity<List<Map<String, Object>>> getTopViewed() {
        List<Map<String, Object>> result = new ArrayList<>();
        histRepo.findTopViewed().stream().limit(10).forEach(row -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("salleNom", row[0]);
            entry.put("blocNom",  row[1]);
            entry.put("count",    row[2]);
            result.add(entry);
        });
        return ResponseEntity.ok(result);
    }

    // ===================================================
    // ACTIVITÉ 7 DERNIERS JOURS
    // ===================================================
    @GetMapping("/activity")
    public ResponseEntity<List<Map<String, Object>>> getActivity() {
        List<Map<String, Object>> result = new ArrayList<>();
        histRepo.findActivityLast7Days().forEach(row -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("day",   row[0].toString());
            entry.put("count", row[1]);
            result.add(entry);
        });
        return ResponseEntity.ok(result);
    }

    // ===================================================
    // TOP UTILISATEURS
    // ===================================================
    @GetMapping("/top-users")
    public ResponseEntity<List<Map<String, Object>>> getTopUsers() {
        List<Map<String, Object>> result = new ArrayList<>();
        histRepo.findTopUsers().stream().limit(10).forEach(row -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("email", row[0]);
            entry.put("count", row[1]);
            result.add(entry);
        });
        return ResponseEntity.ok(result);
    }

    // ===================================================
    // LOG — Enregistrer une navigation ou vue
    // ===================================================
    @PostMapping("/log")
    public ResponseEntity<Void> logNavigation(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {
        try {
            String email = jwtUtil.extractEmail(authHeader.substring(7));
            Optional<User> userOpt = userRepo.findByEmail(email);
            if (userOpt.isEmpty() || !(userOpt.get() instanceof Etudiant))
                return ResponseEntity.status(401).build();

            Etudiant etudiant = (Etudiant) userOpt.get();
            Long salleId = Long.parseLong(body.get("salleId").toString());
            String typeStr = (String) body.getOrDefault("type", "VIEW");

            Salle salle = salleRepo.findById(salleId).orElse(null);
            if (salle == null) return ResponseEntity.notFound().build();

            NavigationHistory.TypeHistorique type =
                NavigationHistory.TypeHistorique.valueOf(typeStr.toUpperCase());

            histRepo.save(new NavigationHistory(etudiant, salle, type));
        } catch (Exception ignored) {}
        return ResponseEntity.ok().build();
    }
}