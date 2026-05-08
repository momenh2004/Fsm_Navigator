package com.fsm.navigator.backend.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

/**
 * PointLocalisation.java – Point de localisation WiFi
 *
 * Type      → Relation obligatoire
 * SALLE     → salle_id  (salle existante)
 * COULOIR   → etage_id
 * ESCALIER  → etage_id
 * ENTREE    → bloc_id
 * SORTIE    → bloc_id
 * RAMPE     → bloc_id
 */
@Entity
@Table(name = "poi")
public class PointLocalisation {

    public enum Type {
        SALLE, COULOIR, ESCALIER, ENTREE, SORTIE, RAMPE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private float x = 0f;

    @Column(nullable = false)
    private float y = 0f;

    @Column(name = "accessible_pmr")
    private boolean accessiblePmr = false;

    // ===== RELATIONS OPTIONNELLES selon le type =====

    @ManyToOne
    @JoinColumn(name = "salle_id", nullable = true)
    @JsonIgnore
    private Salle salle;     // SALLE uniquement

    @ManyToOne
    @JoinColumn(name = "etage_id", nullable = true)
    @JsonIgnore
    private Etage etage;     // COULOIR, ESCALIER

    @ManyToOne
    @JoinColumn(name = "bloc_id", nullable = true)
    @JsonIgnore
    private Bloc bloc;       // ENTREE, SORTIE, RAMPE

    @OneToMany(mappedBy = "poi", cascade = CascadeType.ALL)
    private List<Fingerprint> fingerprints;

    // ===== CONSTRUCTEURS =====
    public PointLocalisation() {}

    // Pour SALLE
    public PointLocalisation(String nom, float x, float y,
                              boolean accessiblePmr, Salle salle) {
        this.nom           = nom;
        this.type          = Type.SALLE;
        this.x             = x;
        this.y             = y;
        this.accessiblePmr = accessiblePmr;
        this.salle         = salle;
    }

    // Pour COULOIR / ESCALIER
    public PointLocalisation(String nom, Type type, float x, float y,
                              boolean accessiblePmr, Etage etage) {
        this.nom           = nom;
        this.type          = type;
        this.x             = x;
        this.y             = y;
        this.accessiblePmr = accessiblePmr;
        this.etage         = etage;
    }

    // Pour ENTREE / SORTIE / RAMPE
    public PointLocalisation(String nom, Type type, float x, float y,
                              boolean accessiblePmr, Bloc bloc) {
        this.nom           = nom;
        this.type          = type;
        this.x             = x;
        this.y             = y;
        this.accessiblePmr = accessiblePmr;
        this.bloc          = bloc;
    }

    // ===== GETTERS =====
    public Long   getId()            { return id; }
    public String getNom()           { return nom; }
    public Type   getType()          { return type; }
    public float  getX()             { return x; }
    public float  getY()             { return y; }
    public boolean isAccessiblePmr() { return accessiblePmr; }
    public Salle  getSalle()         { return salle; }
    public Etage  getEtage()         { return etage; }
    public Bloc   getBloc()          { return bloc; }
    public List<Fingerprint> getFingerprints() { return fingerprints; }

    // ID de référence (utile pour le frontend)
    public Long getSalleId()  { return salle  != null ? salle.getId()  : null; }
    public Long getEtageId()  { return etage  != null ? etage.getId()  : null; }
    public Long getBlocId()   { return bloc   != null ? bloc.getId()   : null; }
    public String getBlocCode() { return bloc != null ? bloc.getCode() : null; }
    public int    getEtageNumero() { return etage != null ? etage.getNumero() : 0; }

    // ===== SETTERS =====
    public void setNom(String nom)              { this.nom = nom; }
    public void setType(Type type)              { this.type = type; }
    public void setX(float x)                  { this.x = x; }
    public void setY(float y)                  { this.y = y; }
    public void setAccessiblePmr(boolean pmr)  { this.accessiblePmr = pmr; }
    public void setSalle(Salle salle)           { this.salle = salle; }
    public void setEtage(Etage etage)           { this.etage = etage; }
    public void setBloc(Bloc bloc)              { this.bloc = bloc; }
}