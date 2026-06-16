package com.fsm.navigator.backend.repository;

import com.fsm.navigator.backend.model.PointLocalisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PointLocalisationRepository extends JpaRepository<PointLocalisation, Long> {

    List<PointLocalisation> findByType(PointLocalisation.Type type);

    List<PointLocalisation> findBySalle_Id(Long salleId);   // ← ajouter

    List<PointLocalisation> findByEtage_Id(Long etageId);

    List<PointLocalisation> findByBloc_Id(Long blocId);

    @Query("SELECT DISTINCT p FROM PointLocalisation p " +
           "LEFT JOIN p.salle s LEFT JOIN s.etage se LEFT JOIN se.bloc sb " +
           "LEFT JOIN p.etage e LEFT JOIN e.bloc eb " +
           "LEFT JOIN p.bloc pb " +
           "WHERE sb.id = :blocId OR eb.id = :blocId OR pb.id = :blocId")
    List<PointLocalisation> findByBlocIdAll(@Param("blocId") Long blocId);

    @Query("SELECT p FROM PointLocalisation p WHERE " +
           "(p.salle IS NOT NULL AND p.salle.etage.id = :etageId) OR " +
           "(p.etage IS NOT NULL AND p.etage.id = :etageId)")
    List<PointLocalisation> findByEtageIdAll(@Param("etageId") Long etageId);
}