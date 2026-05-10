package com.fsm.navigator.backend.repository;

import com.fsm.navigator.backend.model.Favori;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoriRepository extends JpaRepository<Favori, Long> {

    List<Favori> findByUserEmail(String userEmail);

    Optional<Favori> findByUserEmailAndSalle_Id(String userEmail, Long salleId);

    Optional<Favori> findByUserEmailAndBloc_Id(String userEmail, Long blocId);

    boolean existsByUserEmailAndSalle_Id(String userEmail, Long salleId);

    boolean existsByUserEmailAndBloc_Id(String userEmail, Long blocId);

    void deleteByUserEmailAndSalle_Id(String userEmail, Long salleId);

    void deleteByUserEmailAndBloc_Id(String userEmail, Long blocId);
}