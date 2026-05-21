package com.fsm.navigator.backend.repository;

import com.fsm.navigator.backend.model.WifiFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WifiFingerprintRepository extends JpaRepository<WifiFingerprint, Long> {

    // Par POI
    List<WifiFingerprint> findByPoi_Id(Long poiId);

    // Par BSSID
    List<WifiFingerprint> findByBssid(String bssid);

    // WifiFingerprints d'un bloc (via poi→salle→etage→bloc, poi→etage→bloc, ou poi→bloc)
    @Query("SELECT DISTINCT f FROM WifiFingerprint f " +
           "JOIN f.poi p " +
           "LEFT JOIN p.salle s LEFT JOIN s.etage se LEFT JOIN se.bloc sb " +
           "LEFT JOIN p.etage pe LEFT JOIN pe.bloc peb " +
           "LEFT JOIN p.bloc pb " +
           "WHERE sb.id = :blocId OR peb.id = :blocId OR pb.id = :blocId")
    List<WifiFingerprint> findByBlocId(@Param("blocId") Long blocId);

    // WifiFingerprints d'un étage (via poi→salle→etage ou poi→etage)
    @Query("SELECT DISTINCT f FROM WifiFingerprint f " +
           "JOIN f.poi p " +
           "LEFT JOIN p.salle s LEFT JOIN s.etage se " +
           "LEFT JOIN p.etage pe " +
           "WHERE se.id = :etageId OR pe.id = :etageId")
    List<WifiFingerprint> findByEtageId(@Param("etageId") Long etageId);
}
