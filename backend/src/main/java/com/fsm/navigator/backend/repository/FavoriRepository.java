package com.fsm.navigator.backend.repository;

import com.fsm.navigator.backend.model.Favori;
import com.fsm.navigator.backend.model.Membre;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoriRepository extends JpaRepository<Favori, Long> {

    // Tous les favoris d'un membre
    List<Favori> findByMembre(Membre membre);

    // Favori spécifique par membre + salle
    Optional<Favori> findByMembreAndSalle_Id(Membre membre, Long salleId);

    // Vérifier existence
    boolean existsByMembreAndSalle_Id(Membre membre, Long salleId);

    // Supprimer un favori
    void deleteByMembreAndSalle_Id(Membre membre, Long salleId);
}
