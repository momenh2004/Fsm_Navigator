package com.fsm.navigator.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "favoris")
public class Favori {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @ManyToOne
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ===== CONSTRUCTEURS =====
    public Favori() {}

    public Favori(Membre membre, Salle salle) {
        this.membre = membre;
        this.salle  = salle;
    }

    // ===== GETTERS =====
    public Long          getId()        { return id; }
    public Membre        getMembre()    { return membre; }
    public Salle         getSalle()     { return salle; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ===== SETTERS =====
    public void setId(Long id)                { this.id = id; }
    public void setMembre(Membre m)           { this.membre = m; }
    public void setSalle(Salle s)             { this.salle = s; }
    public void setCreatedAt(LocalDateTime d) { this.createdAt = d; }
}
