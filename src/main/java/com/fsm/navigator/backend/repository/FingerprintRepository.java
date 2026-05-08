package com.fsm.navigator.backend.repository;

import com.fsm.navigator.backend.model.Fingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FingerprintRepository extends JpaRepository<Fingerprint, Long> {

    // Par POI
    List<Fingerprint> findByPoi_Id(Long poiId);

    // Par BSSID
    List<Fingerprint> findByBssid(String bssid);

    // Fingerprints d'un bloc (via poi→salle→etage→bloc)
    @Query("SELECT f FROM Fingerprint f WHERE " +
           "(f.poi.salle IS NOT NULL AND f.poi.salle.etage.bloc.id = :blocId) OR " +
           "(f.poi.etage IS NOT NULL AND f.poi.etage.bloc.id = :blocId) OR " +
           "(f.poi.bloc IS NOT NULL AND f.poi.bloc.id = :blocId)")
    List<Fingerprint> findByBlocId(@Param("blocId") Long blocId);

    // Fingerprints d'un étage
    @Query("SELECT f FROM Fingerprint f WHERE " +
           "(f.poi.salle IS NOT NULL AND f.poi.salle.etage.id = :etageId) OR " +
           "(f.poi.etage IS NOT NULL AND f.poi.etage.id = :etageId)")
    List<Fingerprint> findByEtageId(@Param("etageId") Long etageId);
}