package com.fsm.navigator.backend.repository;
 
import com.fsm.navigator.backend.model.CategorieSalle;
import com.fsm.navigator.backend.model.Salle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalleRepository extends JpaRepository<Salle, Long> {
    List<Salle> findByEtageId(Long etageId);
    List<Salle> findByCategorie(CategorieSalle categorie);
    List<Salle> findByNomContainingIgnoreCase(String nom);
}