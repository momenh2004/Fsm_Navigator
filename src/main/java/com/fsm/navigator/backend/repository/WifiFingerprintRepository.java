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

    // WifiFingerprints d'un bloc (via poi→salle→etage→bloc)
    @Query("SELECT f FROM WifiFingerprint f WHERE " +
           "(f.poi.salle IS NOT NULL AND f.poi.salle.etage.bloc.id = :blocId) OR " +
           "(f.poi.etage IS NOT NULL AND f.poi.etage.bloc.id = :blocId) OR " +
           "(f.poi.bloc IS NOT NULL AND f.poi.bloc.id = :blocId)")
    List<WifiFingerprint> findByBlocId(@Param("blocId") Long blocId);

    // WifiFingerprints d'un étage
    @Query("SELECT f FROM WifiFingerprint f WHERE " +
           "(f.poi.salle IS NOT NULL AND f.poi.salle.etage.id = :etageId) OR " +
           "(f.poi.etage IS NOT NULL AND f.poi.etage.id = :etageId)")
    List<WifiFingerprint> findByEtageId(@Param("etageId") Long etageId);
}
