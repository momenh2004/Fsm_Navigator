package com.fsm.navigator.backend.model;

import jakarta.persistence.*;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Etage.java – Représente un étage dans un bloc
 * Ex : Bloc 3, RDC / Bloc 3, 1er étage
 */
@Entity
@Table(name = "etages")
public class Etage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int numero;         // 0 = RDC, 1 = 1er étage, etc.

    @Column(nullable = false)
    private String label;       // Ex: "Rez-de-chaussée", "1er étage"

    @Column(name = "accessible_pmr")
    private boolean accessiblePmr = false; // false si escalier uniquement

    @ManyToOne
    @JoinColumn(name = "bloc_id", nullable = false)
    @JsonIgnore
    private Bloc bloc;

    @OneToMany(mappedBy = "etage", cascade = CascadeType.ALL)
    private List<Salle> salles;

    // ===== CONSTRUCTEURS =====
    public Etage() {}
    public Etage(int numero, String label, boolean accessiblePmr, Bloc bloc) {
        this.numero = numero;
        this.label = label;
        this.accessiblePmr = accessiblePmr;
        this.bloc = bloc;
    }

    // ===== GETTERS / SETTERS =====
    public Long    getId()           { return id; }
    public int     getNumero()       { return numero; }
    public String  getLabel()        { return label; }
    public boolean isAccessiblePmr() { return accessiblePmr; }
    public Bloc    getBloc()         { return bloc; }
    public List<Salle> getSalles()   { return salles; }

    public void setNumero(int n)             { this.numero = n; }
    public void setLabel(String l)           { this.label = l; }
    public void setAccessiblePmr(boolean p)  { this.accessiblePmr = p; }
    public void setBloc(Bloc b)              { this.bloc = b; }
}