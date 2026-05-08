package com.fsm.navigator.backend.repository;

import com.fsm.navigator.backend.model.NavigationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NavigationHistoryRepository extends JpaRepository<NavigationHistory, Long> {

    // Top salles les plus naviguées
    @Query("SELECT n.salleNom, n.blocCode, COUNT(n) as cnt FROM NavigationHistory n " +
           "WHERE n.type = 'NAVIGATION' GROUP BY n.salleNom, n.blocCode ORDER BY cnt DESC")
    List<Object[]> findTopNavigated();

    // Top salles les plus consultées
    @Query("SELECT n.salleNom, n.blocCode, COUNT(n) as cnt FROM NavigationHistory n " +
           "WHERE n.type = 'VIEW' GROUP BY n.salleNom, n.blocCode ORDER BY cnt DESC")
    List<Object[]> findTopViewed();

    // Activité par jour (7 derniers jours)
    @Query(value = "SELECT DATE(created_at) as day, COUNT(*) as cnt " +
                   "FROM navigation_history " +
                   "WHERE created_at >= NOW() - INTERVAL '7 days' " +
                   "GROUP BY DATE(created_at) ORDER BY day", nativeQuery = true)
    List<Object[]> findActivityLast7Days();

    // Répartition par type
    long countByType(NavigationHistory.Type type);

    // Stats par user
    @Query("SELECT n.userEmail, COUNT(n) as cnt FROM NavigationHistory n " +
           "GROUP BY n.userEmail ORDER BY cnt DESC")
    List<Object[]> findTopUsers();
}