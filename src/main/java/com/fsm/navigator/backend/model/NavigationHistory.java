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

    public enum Type { NAVIGATION, VIEW }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "salle_id")
    private Long salleId;

    @Column(name = "salle_nom")
    private String salleNom;

    @Column(name = "bloc_code")
    private String blocCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public NavigationHistory() {}

    public NavigationHistory(Long salleId, String salleNom, String blocCode,
                              Type type, String userEmail) {
        this.salleId   = salleId;
        this.salleNom  = salleNom;
        this.blocCode  = blocCode;
        this.type      = type;
        this.userEmail = userEmail;
        this.createdAt = LocalDateTime.now();
    }

    public Long          getId()        { return id; }
    public Long          getSalleId()   { return salleId; }
    public String        getSalleNom()  { return salleNom; }
    public String        getBlocCode()  { return blocCode; }
    public Type          getType()      { return type; }
    public String        getUserEmail() { return userEmail; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setSalleId(Long s)    { this.salleId = s; }
    public void setSalleNom(String s) { this.salleNom = s; }
    public void setBlocCode(String s) { this.blocCode = s; }
    public void setType(Type t)       { this.type = t; }
    public void setUserEmail(String e){ this.userEmail = e; }
}