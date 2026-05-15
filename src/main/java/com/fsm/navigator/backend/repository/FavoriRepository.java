package com.fsm.navigator.backend.repository;

import com.fsm.navigator.backend.model.Favori;
import com.fsm.navigator.backend.model.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoriRepository extends JpaRepository<Favori, Long> {

    // Tous les favoris d'un étudiant
    List<Favori> findByEtudiant(Etudiant etudiant);

    // Favori spécifique par étudiant + salle
    Optional<Favori> findByEtudiantAndSalle_Id(Etudiant etudiant, Long salleId);

    // Vérifier existence
    boolean existsByEtudiantAndSalle_Id(Etudiant etudiant, Long salleId);

    // Supprimer un favori
    void deleteByEtudiantAndSalle_Id(Etudiant etudiant, Long salleId);
}