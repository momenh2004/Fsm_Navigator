package com.fsm.navigator.backend.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "salles")
public class Salle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategorieSalle categorie;

    @Column(name = "ordre_depuis_entree")
    private int ordreDepuisEntree;

    @Column(name = "entree_reference")
    private String entreeReference;

    @Column(name = "accessible_pmr")
    private boolean accessiblePmr = true;

    @ManyToOne
    @JoinColumn(name = "etage_id", nullable = false)
    @JsonIgnore
    private Etage etage;

    // ===== CONSTRUCTEURS =====
    public Salle() {}

    public Salle(String nom, CategorieSalle categorie, int ordreDepuisEntree,
                 String entreeReference, boolean accessiblePmr, Etage etage) {
        this.nom               = nom;
        this.categorie         = categorie;
        this.ordreDepuisEntree = ordreDepuisEntree;
        this.entreeReference   = entreeReference;
        this.accessiblePmr     = accessiblePmr;
        this.etage             = etage;
    }

    // ===== GETTERS =====
    public Long          getId()                { return id; }
    public String        getNom()               { return nom; }
    public CategorieSalle getCategorie()        { return categorie; }
    public int           getOrdreDepuisEntree() { return ordreDepuisEntree; }
    public String        getEntreeReference()   { return entreeReference; }
    public boolean       isAccessiblePmr()      { return accessiblePmr; }
    public Etage         getEtage()             { return etage; }

    // ===== SETTERS =====
    public void setNom(String nom)                   { this.nom = nom; }
    public void setCategorie(CategorieSalle c)       { this.categorie = c; }
    public void setOrdreDepuisEntree(int o)          { this.ordreDepuisEntree = o; }
    public void setEntreeReference(String e)         { this.entreeReference = e; }
    public void setAccessiblePmr(boolean p)          { this.accessiblePmr = p; }
    public void setEtage(Etage e)                    { this.etage = e; }
}