package com.fsm.navigator.backend.repository;

import com.fsm.navigator.backend.model.NavigationHistory;
import com.fsm.navigator.backend.model.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NavigationHistoryRepository extends JpaRepository<NavigationHistory, Long> {

    // Top salles les plus naviguées
    @Query("SELECT n.salle.nom, n.salle.etage.bloc.nom, COUNT(n) as cnt " +
           "FROM NavigationHistory n " +
           "WHERE n.type = com.fsm.navigator.backend.model.NavigationHistory.TypeHistorique.NAVIGATION " +
           "GROUP BY n.salle.nom, n.salle.etage.bloc.nom ORDER BY cnt DESC")
    List<Object[]> findTopNavigated();

    // Top salles les plus consultées
    @Query("SELECT n.salle.nom, n.salle.etage.bloc.nom, COUNT(n) as cnt " +
           "FROM NavigationHistory n " +
           "WHERE n.type = com.fsm.navigator.backend.model.NavigationHistory.TypeHistorique.VIEW " +
           "GROUP BY n.salle.nom, n.salle.etage.bloc.nom ORDER BY cnt DESC")
    List<Object[]> findTopViewed();

    // Activité par jour (7 derniers jours)
    @Query(value = "SELECT DATE(created_at) as day, COUNT(*) as cnt " +
                   "FROM navigation_history " +
                   "WHERE created_at >= NOW() - INTERVAL '7 days' " +
                   "GROUP BY DATE(created_at) ORDER BY day", nativeQuery = true)
    List<Object[]> findActivityLast7Days();

    // Répartition par type
    long countByType(NavigationHistory.TypeHistorique type);

    // Stats par étudiant
    @Query("SELECT n.etudiant.email, COUNT(n) as cnt " +
           "FROM NavigationHistory n " +
           "GROUP BY n.etudiant.email ORDER BY cnt DESC")
    List<Object[]> findTopUsers();

    // Historique d'un étudiant
    List<NavigationHistory> findByEtudiantOrderByCreatedAtDesc(Etudiant etudiant);
}