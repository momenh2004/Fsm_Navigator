package com.fsm.navigator.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * NavigationHistory.java
 * Enregistre chaque navigation ou consultation de salle.
 * Type : NAVIGATION (lancée) ou VIEW (visualisée dans Search)
 */
@Entity
@Table(name = "navigation_history")
public class NavigationHistory {

    public enum TypeHistorique { NAVIGATION, VIEW }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @ManyToOne
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeHistorique type;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ===== CONSTRUCTEURS =====
    public NavigationHistory() {}

    public NavigationHistory(Etudiant etudiant, Salle salle, TypeHistorique type) {
        this.etudiant = etudiant;
        this.salle    = salle;
        this.type     = type;
    }

    // ===== GETTERS =====
    public Long            getId()        { return id; }
    public Etudiant        getEtudiant()  { return etudiant; }
    public Salle           getSalle()     { return salle; }
    public TypeHistorique  getType()      { return type; }
    public LocalDateTime   getCreatedAt() { return createdAt; }

    // ===== SETTERS =====
    public void setId(Long id)                  { this.id = id; }
    public void setEtudiant(Etudiant e)         { this.etudiant = e; }
    public void setSalle(Salle s)               { this.salle = s; }
    public void setType(TypeHistorique t)       { this.type = t; }
    public void setCreatedAt(LocalDateTime d)   { this.createdAt = d; }
}