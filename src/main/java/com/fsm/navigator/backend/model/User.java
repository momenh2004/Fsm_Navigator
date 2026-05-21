package com.fsm.navigator.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ===== CONSTRUCTEURS =====
    public User() {}

    public User(String email, String password) {
        this.email    = email;
        this.password = password;
    }

    // ===== MÉTHODE ABSTRAITE =====
    public abstract String getRoleAsString();

    // ===== GETTERS =====
    public Long          getId()        { return id; }
    public String        getEmail()     { return email; }
    @JsonIgnore
    public String        getPassword()  { return password; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ===== SETTERS =====
    public void setId(Long id)         { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String p)  { this.password = p; }
}