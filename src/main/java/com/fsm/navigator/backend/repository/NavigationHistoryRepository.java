package com.fsm.navigator.backend.repository;

import com.fsm.navigator.backend.model.NavigationHistory;
import com.fsm.navigator.backend.model.Membre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    // Activité par jour — période paramétrable
    @Query(value = "SELECT DATE(created_at) as day, COUNT(*) as cnt " +
                   "FROM navigation_history " +
                   "WHERE created_at >= :since " +
                   "GROUP BY DATE(created_at) ORDER BY day", nativeQuery = true)
    List<Object[]> findActivitySince(@Param("since") LocalDateTime since);

    // Top naviguées — période paramétrable
    @Query("SELECT n.salle.nom, n.salle.etage.bloc.nom, COUNT(n) as cnt " +
           "FROM NavigationHistory n " +
           "WHERE n.type = com.fsm.navigator.backend.model.NavigationHistory.TypeHistorique.NAVIGATION " +
           "AND n.createdAt >= :since " +
           "GROUP BY n.salle.nom, n.salle.etage.bloc.nom ORDER BY cnt DESC")
    List<Object[]> findTopNavigatedSince(@Param("since") LocalDateTime since);

    // Top consultées — période paramétrable
    @Query("SELECT n.salle.nom, n.salle.etage.bloc.nom, COUNT(n) as cnt " +
           "FROM NavigationHistory n " +
           "WHERE n.type = com.fsm.navigator.backend.model.NavigationHistory.TypeHistorique.VIEW " +
           "AND n.createdAt >= :since " +
           "GROUP BY n.salle.nom, n.salle.etage.bloc.nom ORDER BY cnt DESC")
    List<Object[]> findTopViewedSince(@Param("since") LocalDateTime since);

    // Top utilisateurs — période paramétrable
    @Query("SELECT n.membre.email, COUNT(n) as cnt " +
           "FROM NavigationHistory n " +
           "WHERE n.createdAt >= :since " +
           "GROUP BY n.membre.email ORDER BY cnt DESC")
    List<Object[]> findTopUsersSince(@Param("since") LocalDateTime since);

    // Répartition par type
    long countByType(NavigationHistory.TypeHistorique type);

    // Stats par membre
    @Query("SELECT n.membre.email, COUNT(n) as cnt " +
           "FROM NavigationHistory n " +
           "GROUP BY n.membre.email ORDER BY cnt DESC")
    List<Object[]> findTopUsers();

    // Historique d'un membre
    List<NavigationHistory> findByMembreOrderByCreatedAtDesc(Membre membre);
}
