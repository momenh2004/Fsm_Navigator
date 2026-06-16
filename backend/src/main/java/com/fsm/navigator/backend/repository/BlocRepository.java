package com.fsm.navigator.backend.repository;
 
import com.fsm.navigator.backend.model.Bloc;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
 
public interface BlocRepository extends JpaRepository<Bloc, Long> {
    Optional<Bloc> findByCode(String code);
}