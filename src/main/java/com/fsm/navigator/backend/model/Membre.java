package com.fsm.navigator.backend.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@DiscriminatorValue("MEMBRE")
public class Membre extends User {

    @Column
    private String nom;

    @Column
    private String prenom;

    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favori> favoris;

    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NavigationHistory> historique;

    // ===== CONSTRUCTEURS =====
    public Membre() {}

    public Membre(String email, String password) {
        super(email, password);
    }

    public Membre(String email, String password, String nom, String prenom) {
        super(email, password);
        this.nom    = nom;
        this.prenom = prenom;
    }

    @Override
    public String getRoleAsString() { return "MEMBRE"; }

    // ===== GETTERS =====
    public String                  getNom()       { return nom; }
    public String                  getPrenom()    { return prenom; }
    public List<Favori>            getFavoris()   { return favoris; }
    public List<NavigationHistory> getHistorique(){ return historique; }

    // ===== SETTERS =====
    public void setNom(String nom)       { this.nom = nom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
}
