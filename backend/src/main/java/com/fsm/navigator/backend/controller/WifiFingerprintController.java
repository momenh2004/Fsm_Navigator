package com.fsm.navigator.backend.controller;

import com.fsm.navigator.backend.model.*;
import com.fsm.navigator.backend.repository.WifiFingerprintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/fingerprints")
@CrossOrigin(origins = "*")
public class WifiFingerprintController {

    @Autowired private WifiFingerprintRepository fpRepo;

    @GetMapping
    public List<Map<String, Object>> getAll() {
        List<WifiFingerprint> fps = fpRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (WifiFingerprint fp : fps) {
            PointLocalisation poi = fp.getPoi();
            if (poi == null) continue;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id",        fp.getId());
            map.put("bssid",     fp.getBssid());
            map.put("ssid",      fp.getSsid());
            map.put("rssiMoyen", fp.getRssiMoyen());
            map.put("poiId",     poi.getId());
            map.put("poiNom",    poi.getNom());
            map.put("poiType",   poi.getType().name());
            map.put("x",         poi.getX());
            map.put("y",         poi.getY());

            if (poi.getSalle() != null) {
                Salle s = poi.getSalle();
                map.put("salleId",     s.getId());
                map.put("salleNom",    s.getNom());
                map.put("etageNumero", s.getEtage().getNumero());
                map.put("blocCode",    s.getEtage().getBloc().getCode());
            } else if (poi.getEtage() != null) {
                map.put("salleNom",    poi.getNom());
                map.put("etageNumero", poi.getEtage().getNumero());
                map.put("blocCode",    poi.getEtage().getBloc().getCode());
            } else if (poi.getBloc() != null) {
                map.put("salleNom", poi.getNom());
                map.put("blocCode", poi.getBloc().getCode());
            }

            result.add(map);
        }
        return result;
    }
}
