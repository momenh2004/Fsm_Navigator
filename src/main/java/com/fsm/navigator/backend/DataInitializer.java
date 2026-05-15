package com.fsm.navigator.backend;

import com.fsm.navigator.backend.model.*;
import com.fsm.navigator.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * DataInitializer.java
 * Peuple automatiquement la base de données au démarrage si elle est vide.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private BlocRepository              blocRepo;
    @Autowired private EtageRepository             etageRepo;
    @Autowired private SalleRepository             salleRepo;
    @Autowired private PointLocalisationRepository poiRepo;
    @Autowired private FingerprintRepository       fpRepo;

    @Override
    public void run(String... args) throws Exception {
        if (blocRepo.count() > 0) return;
        System.out.println("Initialisation des données FSM...");

        // =====================================================
        // BLOC 3 — Informatique
        // =====================================================
        Bloc b3 = blocRepo.save(new Bloc("B3", "Bloc 3 (Informatique)",
                "Salles informatiques 301→316", false));

        Etage b3rdc = etageRepo.save(new Etage(0, "Rez-de-chaussée", true,  b3));
        Etage b3et1 = etageRepo.save(new Etage(1, "1er étage",       false, b3));

        // RDC — Côté DROITE
        Salle s301 = save(new Salle("Salle 301", CategorieSalle.SALLE_ETUDE, 1, "DROITE", true,  b3rdc), true);
        Salle s302 = save(new Salle("Salle 302", CategorieSalle.SALLE_ETUDE, 2, "DROITE", true,  b3rdc), true);
        Salle s303 = save(new Salle("Salle 303", CategorieSalle.SALLE_ETUDE, 3, "DROITE", true,  b3rdc), true);
        Salle s304 = save(new Salle("Salle 304", CategorieSalle.SALLE_ETUDE, 4, "DROITE", true,  b3rdc), true);

        // RDC — Côté GAUCHE
        Salle s308 = save(new Salle("Salle 308", CategorieSalle.SALLE_ETUDE, 1, "GAUCHE", true,  b3rdc), true);
        Salle s307 = save(new Salle("Salle 307", CategorieSalle.SALLE_ETUDE, 2, "GAUCHE", true,  b3rdc), true);
        Salle s306 = save(new Salle("Salle 306", CategorieSalle.SALLE_ETUDE, 3, "GAUCHE", true,  b3rdc), true);
        Salle s305 = save(new Salle("Salle 305", CategorieSalle.SALLE_ETUDE, 4, "GAUCHE", true,  b3rdc), true);

        // 1er étage
        Salle s309 = save(new Salle("Salle 309", CategorieSalle.SALLE_ETUDE, 1, "DROITE", false, b3et1), true);
        Salle s310 = save(new Salle("Salle 310", CategorieSalle.SALLE_ETUDE, 2, "DROITE", false, b3et1), true);
        Salle s311 = save(new Salle("Salle 311", CategorieSalle.SALLE_ETUDE, 3, "DROITE", false, b3et1), true);
        Salle s312 = save(new Salle("Salle 312", CategorieSalle.SALLE_ETUDE, 4, "DROITE", false, b3et1), true);
        Salle s313 = save(new Salle("Salle 313", CategorieSalle.SALLE_ETUDE, 1, "GAUCHE", false, b3et1), true);
        Salle s314 = save(new Salle("Salle 314", CategorieSalle.SALLE_ETUDE, 2, "GAUCHE", false, b3et1), true);
        Salle s315 = save(new Salle("Salle 315", CategorieSalle.SALLE_ETUDE, 3, "GAUCHE", false, b3et1), true);
        Salle s316 = save(new Salle("Salle 316", CategorieSalle.SALLE_ETUDE, 4, "GAUCHE", false, b3et1), true);

        // POI de passage Bloc 3 RDC
        poiRepo.save(new PointLocalisation("Entrée Bloc 3",   PointLocalisation.Type.ENTREE,   8.5f, 15f,   true,  b3));
        poiRepo.save(new PointLocalisation("Sortie Bloc 3",   PointLocalisation.Type.SORTIE,   8.5f,  0f,   true,  b3));
        poiRepo.save(new PointLocalisation("Rampe Bloc 3",    PointLocalisation.Type.RAMPE,    2f,   15f,   true,  b3));
        poiRepo.save(new PointLocalisation("Escalier B3 RDC", PointLocalisation.Type.ESCALIER, 8.5f, 13f,   false, b3rdc));
        poiRepo.save(new PointLocalisation("Couloir B3 G1",   PointLocalisation.Type.COULOIR,  3.5f,  3.75f,true,  b3rdc));
        poiRepo.save(new PointLocalisation("Couloir B3 G2",   PointLocalisation.Type.COULOIR,  3.5f,  6.5f, true,  b3rdc));
        poiRepo.save(new PointLocalisation("Couloir B3 G3",   PointLocalisation.Type.COULOIR,  3.5f, 12.1f, true,  b3rdc));
        poiRepo.save(new PointLocalisation("Couloir B3 D1",   PointLocalisation.Type.COULOIR, 13.5f,  3.75f,true,  b3rdc));
        poiRepo.save(new PointLocalisation("Couloir B3 D2",   PointLocalisation.Type.COULOIR, 13.5f,  6.5f, true,  b3rdc));
        poiRepo.save(new PointLocalisation("Couloir B3 D3",   PointLocalisation.Type.COULOIR, 13.5f, 12.1f, true,  b3rdc));

        // Fingerprints RDC Bloc 3
        fp("40:ed:00:90:89:4a", "FSM-WiFi", -71.25, s301);
        fp("cc:b2:55:91:37:d0", "FSM-WiFi", -57.50, s302);
        fp("cc:b2:55:91:37:d0", "FSM-WiFi", -60.75, s303);
        fp("cc:b2:55:91:37:d0", "FSM-WiFi", -62.25, s304);
        fp("40:ed:00:90:88:8a", "FSM-WiFi", -68.50, s305);
        fp("cc:b2:55:8e:34:48", "FSM-WiFi", -68.50, s305);
        fp("cc:b2:55:8e:34:48", "FSM-WiFi", -70.25, s306);
        fp("cc:b2:55:8e:34:48", "FSM-WiFi", -64.25, s307);
        fp("cc:b2:55:8e:34:48", "FSM-WiFi", -54.50, s308);

        // Fingerprints 1er étage Bloc 3
        fp("10:be:f5:2a:93:10", "FSM-WiFi", -47.00, s312);
        fp("80:26:98:ad:24:80", "FSM-WiFi", -62.50, s316);
        fp("cc:b2:55:8e:34:48", "FSM-WiFi", -45.00, s313);
        fp("cc:b2:55:8e:34:48", "FSM-WiFi", -59.25, s315);

        // =====================================================
        // BLOC 1 — Palestine
        // =====================================================
        Bloc b1 = blocRepo.save(new Bloc("B1", "Bloc 1 (Palestine)",
                "Amphi A→D • Salles 101→117", false));
        Etage b1rdc = etageRepo.save(new Etage(0, "Rez-de-chaussée", true, b1));
        Etage b1et1 = etageRepo.save(new Etage(1, "1er Etage",       true, b1));

        poiRepo.save(new PointLocalisation("Entrée Bloc 1", PointLocalisation.Type.ENTREE,   0f, 0f, true, b1));
        poiRepo.save(new PointLocalisation("Escalier B1",   PointLocalisation.Type.ESCALIER, 0f, 0f, true, b1rdc));

        Salle AA  = save(new Salle("Amphithéâtre A", CategorieSalle.SALLE_ETUDE, 1, "DROITE", true, b1rdc), true);
        Salle AB  = save(new Salle("Amphithéâtre B", CategorieSalle.SALLE_ETUDE, 2, "DROITE", true, b1rdc), true);
        Salle AC  = save(new Salle("Amphithéâtre C", CategorieSalle.SALLE_ETUDE, 3, "GAUCHE", true, b1rdc), true);
        Salle AD  = save(new Salle("Amphithéâtre D", CategorieSalle.SALLE_ETUDE, 4, "GAUCHE", true, b1rdc), true);

        Salle s101 = save(new Salle("Salle 101", CategorieSalle.SALLE_ETUDE, 1, "DROITE", true, b1rdc), true);
        Salle s102 = save(new Salle("Salle 102", CategorieSalle.SALLE_ETUDE, 2, "DROITE", true, b1rdc), true);
        Salle s103 = save(new Salle("Salle 103", CategorieSalle.SALLE_ETUDE, 3, "DROITE", true, b1rdc), true);
        Salle s104 = save(new Salle("Salle 104", CategorieSalle.SALLE_ETUDE, 4, "DROITE", true, b1rdc), true);
        Salle s105 = save(new Salle("Salle 105", CategorieSalle.SALLE_ETUDE, 1, "GAUCHE", true, b1rdc), true);
        Salle s106 = save(new Salle("Salle 106", CategorieSalle.SALLE_ETUDE, 2, "GAUCHE", true, b1rdc), true);
        Salle s107 = save(new Salle("Salle 107", CategorieSalle.SALLE_ETUDE, 3, "GAUCHE", true, b1rdc), true);

        Salle s111 = save(new Salle("Salle 111", CategorieSalle.SALLE_ETUDE, 1, "DROITE", true, b1et1), true);
        Salle s112 = save(new Salle("Salle 112", CategorieSalle.SALLE_ETUDE, 2, "DROITE", true, b1et1), true);
        Salle s113 = save(new Salle("Salle 113", CategorieSalle.SALLE_ETUDE, 3, "DROITE", true, b1et1), true);
        Salle s114 = save(new Salle("Salle 114", CategorieSalle.SALLE_ETUDE, 4, "DROITE", true, b1et1), true);
        Salle s115 = save(new Salle("Salle 115", CategorieSalle.SALLE_ETUDE, 1, "GAUCHE", true, b1et1), true);
        Salle s116 = save(new Salle("Salle 116", CategorieSalle.SALLE_ETUDE, 2, "GAUCHE", true, b1et1), true);
        Salle s117 = save(new Salle("Salle 117", CategorieSalle.SALLE_ETUDE, 3, "GAUCHE", true, b1et1), true);

        fp("cc:b2:55:91:3c:c0", "FSM-WiFi", -72.50, AA);
        fp("cc:b2:55:91:3c:c0", "FSM-WiFi", -45.75, AB);
        fp("ec:ad:e0:94:1b:28", "FSM-WiFi", -60.50, AC);
        fp("cc:b2:55:91:3c:c0", "FSM-WiFi", -62.00, AD);
        fp("cc:b2:55:91:3c:c0", "FSM-WiFi", -55.75, s101);
        fp("cc:b2:55:91:3c:c0", "FSM-WiFi", -67.50, s102);
        fp("cc:b2:55:91:3c:c0", "FSM-WiFi", -63.50, s103);
        fp("cc:b2:55:91:3c:c0", "FSM-WiFi", -50.00, s104);
        fp("cc:b2:55:91:3c:c0", "FSM-WiFi", -79.50, s105);
        fp("cc:b2:55:91:3c:c0", "FSM-WiFi", -83.50, s106);
        fp("cc:b2:55:91:3c:c0", "FSM-WiFi", -65.50, s107);
        fp("cc:b2:55:91:3c:c0", "FSM-WiFi", -63.00, s111);
        fp("cc:b2:55:91:3c:c0", "FSM-WiFi", -75.00, s112);
        fp("cc:b2:55:91:3c:c0", "FSM-WiFi", -75.00, s113);
        fp("cc:b2:55:91:3c:c0", "FSM-WiFi", -61.25, s114);
        fp("cc:b2:55:91:3c:c0", "FSM-WiFi", -84.00, s115);
        fp("cc:b2:55:91:3c:c0", "FSM-WiFi", -91.00, s116);
        fp("cc:b2:55:91:3c:c0", "FSM-WiFi", -64.25, s117);

        // =====================================================
        // BLOC 2 — Préparatoire
        // =====================================================
        Bloc b2 = blocRepo.save(new Bloc("B2", "Bloc 2 (Préparatoire)",
                "Salles 201→217", false));
        Etage b2rdc = etageRepo.save(new Etage(0, "Rez-de-chaussée", true, b2));
        Etage b2et1 = etageRepo.save(new Etage(1, "1er Etage",       true, b2));

        poiRepo.save(new PointLocalisation("Entrée Bloc 2", PointLocalisation.Type.ENTREE, 0f, 0f, true, b2));

        Salle s201 = save(new Salle("Salle 201", CategorieSalle.SALLE_ETUDE, 1, "DROITE", true, b2rdc), true);
        Salle s202 = save(new Salle("Salle 202", CategorieSalle.SALLE_ETUDE, 2, "DROITE", true, b2rdc), true);
        Salle s203 = save(new Salle("Salle 203", CategorieSalle.SALLE_ETUDE, 3, "DROITE", true, b2rdc), true);
        Salle s204 = save(new Salle("Salle 204", CategorieSalle.SALLE_ETUDE, 1, "GAUCHE", true, b2rdc), true);
        Salle s205 = save(new Salle("Salle 205", CategorieSalle.SALLE_ETUDE, 2, "GAUCHE", true, b2rdc), true);
        Salle s206 = save(new Salle("Salle 206", CategorieSalle.SALLE_ETUDE, 3, "GAUCHE", true, b2rdc), true);
        Salle s211 = save(new Salle("Salle 211", CategorieSalle.SALLE_ETUDE, 1, "DROITE", true, b2et1), true);
        Salle s212 = save(new Salle("Salle 212", CategorieSalle.SALLE_ETUDE, 2, "DROITE", true, b2et1), true);
        Salle s213 = save(new Salle("Salle 213", CategorieSalle.SALLE_ETUDE, 3, "DROITE", true, b2et1), true);
        Salle s214 = save(new Salle("Salle 214", CategorieSalle.SALLE_ETUDE, 4, "DROITE", true, b2et1), true);
        Salle s215 = save(new Salle("Salle 215", CategorieSalle.SALLE_ETUDE, 1, "GAUCHE", true, b2et1), true);
        Salle s216 = save(new Salle("Salle 216", CategorieSalle.SALLE_ETUDE, 2, "GAUCHE", true, b2et1), true);
        Salle s217 = save(new Salle("Salle 217", CategorieSalle.SALLE_ETUDE, 3, "GAUCHE", true, b2et1), true);
        Salle s218 = save(new Salle("Salle 218", CategorieSalle.SALLE_ETUDE, 4, "GAUCHE", true, b2et1), true);

        fp("78:8c:b5:64:34:87", "FSM-WiFi", -59.25, s201);
        fp("78:8c:b5:64:34:87", "FSM-WiFi", -54.25, s202);
        fp("cc:b2:55:91:3c:c0", "FSM-WiFi", -66.25, s203);
        fp("78:8c:b5:64:34:87", "FSM-WiFi", -74.00, s204);
        fp("78:8c:b5:64:34:87", "FSM-WiFi", -51.50, s205);
        fp("78:8c:b5:64:34:87", "FSM-WiFi", -60.25, s206);
        fp("78:8c:b5:64:34:87", "FSM-WiFi", -88.50, s211);
        fp("78:8c:b5:64:34:87", "FSM-WiFi", -79.00, s212);
        fp("78:8c:b5:64:34:87", "FSM-WiFi", -89.00, s213);
        fp("78:8c:b5:64:34:87", "FSM-WiFi", -82.75, s214);
        fp("78:8c:b5:64:34:87", "FSM-WiFi", -82.25, s215);
        fp("78:8c:b5:64:34:87", "FSM-WiFi", -86.00, s216);
        fp("78:8c:b5:64:34:87", "FSM-WiFi", -88.00, s217);
        fp("78:8c:b5:64:34:87", "FSM-WiFi", -84.50, s218);

        // =====================================================
        // BLOC 4
        // =====================================================
        Bloc b4 = blocRepo.save(new Bloc("B4", "Bloc 4", "Salles 401→409", false));
        Etage b4et1 = etageRepo.save(new Etage(0, "1er Etage", false, b4));
        poiRepo.save(new PointLocalisation("Entrée Bloc 4", PointLocalisation.Type.ENTREE, 0f, 0f, false, b4));
        for (int i = 401; i <= 409; i++)
            save(new Salle("Salle " + i, CategorieSalle.SALLE_ETUDE, i - 400, "DROITE", true, b4et1), false);

        // =====================================================
        // BLOCS DÉPARTEMENTS
        // =====================================================
        saveDept("BM",  "Bloc Mathématique", "Département Mathématiques", "Département Mathématiques");
        saveDept("BP1", "Bloc Physique 1",   "Département Physique 1",    "Département Physique 1");
        saveDept("BP2", "Bloc Physique 2",   "Département Physique 2",    "Département Physique 2");
        saveDept("BC1", "Bloc Chimie 1",     "Département Chimie 1",      "Département Chimie 1");
        saveDept("BC2", "Bloc Chimie 2",     "Département Chimie 2",      "Département Chimie 2");

        // =====================================================
        // COUR ROUGE
        // =====================================================
        Bloc cour = blocRepo.save(new Bloc("COUR ROUGE", "Amphi 1→6 • Bibliothèques", "", true));
        Etage courrdc = etageRepo.save(new Etage(0, "Rez-de-chaussée", true, cour));
        poiRepo.save(new PointLocalisation("Entrée Cour Rouge", PointLocalisation.Type.ENTREE, 0f, 0f, true, cour));
        poiRepo.save(new PointLocalisation("Rampe Cour Rouge",  PointLocalisation.Type.RAMPE,  0f, 0f, true, cour));

        save(new Salle("Amphithéâtre 1",        CategorieSalle.SALLE_ETUDE, 1, "DROITE", true, courrdc), false);
        save(new Salle("Amphithéâtre 2",        CategorieSalle.SALLE_ETUDE, 2, "CENTRE", true, courrdc), false);
        save(new Salle("Amphithéâtre 3",        CategorieSalle.SALLE_ETUDE, 3, "GAUCHE", true, courrdc), false);
        save(new Salle("Amphithéâtre 4",        CategorieSalle.SALLE_ETUDE, 4, "GAUCHE", true, courrdc), false);
        save(new Salle("Bibliothèque Centrale", CategorieSalle.SALLE_ETUDE, 1, "CENTRE", true, courrdc), false);
        save(new Salle("Bibliothèque B1",       CategorieSalle.SALLE_ETUDE, 2, "CENTRE", true, courrdc), false);
        save(new Salle("Bibliothèque B2",       CategorieSalle.SALLE_ETUDE, 3, "CENTRE", true, courrdc), false);
        save(new Salle("Salle C1",              CategorieSalle.SALLE_ETUDE, 1, "CENTRE", true, courrdc), false);
        save(new Salle("Salle C2",              CategorieSalle.SALLE_ETUDE, 2, "CENTRE", true, courrdc), false);
        save(new Salle("Salle C3",              CategorieSalle.SALLE_ETUDE, 3, "CENTRE", true, courrdc), false);
        save(new Salle("Salle D1",              CategorieSalle.SALLE_ETUDE, 1, "CENTRE", true, courrdc), false);
        save(new Salle("Salle D2",              CategorieSalle.SALLE_ETUDE, 2, "CENTRE", true, courrdc), false);
        save(new Salle("Salle des thèses",      CategorieSalle.SALLE_ETUDE, 1, "CENTRE", true, courrdc), false);

        System.out.println("✅ Données FSM initialisées avec succès !");
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private Salle save(Salle salle, boolean withPoi) {
        Salle saved = salleRepo.save(salle);
        if (withPoi) {
            PointLocalisation poi = new PointLocalisation(
                    saved.getNom(), 0f, 0f, saved.isAccessiblePmr(), saved);
            poiRepo.save(poi);
        }
        return saved;
    }

    private void fp(String bssid, String ssid, double rssi, Salle salle) {
        java.util.List<PointLocalisation> pois = poiRepo.findBySalle_Id(salle.getId());
        if (pois.isEmpty()) {
            System.err.println("⚠️ Aucun POI pour la salle : " + salle.getNom());
            return;
        }
        fpRepo.save(new Fingerprint(bssid, ssid, rssi, pois.get(0)));
    }

    private void saveDept(String code, String nom, String desc, String salleName) {
        Bloc bloc   = blocRepo.save(new Bloc(code, nom, desc, false));
        Etage etage = etageRepo.save(new Etage(0, "Rez-de-chaussée", true, bloc));
        poiRepo.save(new PointLocalisation("Entrée " + nom, PointLocalisation.Type.ENTREE, 0f, 0f, false, bloc));
        save(new Salle(salleName, CategorieSalle.BUREAU, 1, "DROITE", true, etage), false);
    }
}