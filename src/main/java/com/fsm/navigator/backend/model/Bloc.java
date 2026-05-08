package com.fsm.navigator.backend.model;

import jakarta.persistence.*;
import java.util.List;

/**
 * Bloc.java – Entité JPA représentant un bloc de la FSM
 */
@Entity
@Table(name = "blocs")
public class Bloc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String nom;

    private String description;

    @Column(name = "accessible_pmr")
    private boolean accessiblePmr = false;

    @OneToMany(mappedBy = "bloc", cascade = CascadeType.ALL)
    private List<Etage> etages;

    // ===== CONSTRUCTEURS =====
    public Bloc() {}

    // Constructeur à 3 paramètres
    public Bloc(String code, String nom, boolean accessiblePmr) {
        this.code         = code;
        this.nom          = nom;
        this.accessiblePmr = accessiblePmr;
    }

    // Constructeur à 4 paramètres
    public Bloc(String code, String nom, String description, boolean accessiblePmr) {
        this.code          = code;
        this.nom           = nom;
        this.description   = description;
        this.accessiblePmr = accessiblePmr;
    }

    // ===== GETTERS =====
    public Long    getId()           { return id; }
    public String  getCode()         { return code; }
    public String  getNom()          { return nom; }
    public String  getDescription()  { return description; }
    public boolean isAccessiblePmr() { return accessiblePmr; }
    public List<Etage> getEtages()   { return etages; }

    // ===== SETTERS =====
    public void setCode(String code)          { this.code = code; }
    public void setNom(String nom)            { this.nom = nom; }
    public void setDescription(String d)      { this.description = d; }
    public void setAccessiblePmr(boolean pmr) { this.accessiblePmr = pmr; }
}