package com.fsm.navigator.backend.repository;
 
import com.fsm.navigator.backend.model.Etage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
 
public interface EtageRepository extends JpaRepository<Etage, Long> {
    List<Etage> findByBlocId(Long blocId);
}