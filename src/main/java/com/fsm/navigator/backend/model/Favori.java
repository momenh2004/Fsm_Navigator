package com.fsm.navigator.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "favoris")
public class Favori {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeFavori type;

    @ManyToOne
    @JoinColumn(name = "salle_id", nullable = true)
    private Salle salle;

    @ManyToOne
    @JoinColumn(name = "bloc_id", nullable = true)
    private Bloc bloc;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public enum TypeFavori {
        SALLE, BLOC
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────
    public Long getId()                    { return id; }
    public void setId(Long id)             { this.id = id; }

    public String getUserEmail()           { return userEmail; }
    public void setUserEmail(String e)     { this.userEmail = e; }

    public TypeFavori getType()            { return type; }
    public void setType(TypeFavori t)      { this.type = t; }

    public Salle getSalle()                { return salle; }
    public void setSalle(Salle s)          { this.salle = s; }

    public Bloc getBloc()                  { return bloc; }
    public void setBloc(Bloc b)            { this.bloc = b; }

    public String getNom()                 { return nom; }
    public void setNom(String n)           { this.nom = n; }

    public LocalDateTime getCreatedAt()    { return createdAt; }
    public void setCreatedAt(LocalDateTime d) { this.createdAt = d; }
}