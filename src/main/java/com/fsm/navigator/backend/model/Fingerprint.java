package com.fsm.navigator.backend.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Fingerprint.java – Empreinte WiFi liée à un POI
 *
 * Relation : Fingerprint → PointLocalisation (poi_id)
 * Plus de relation directe avec Salle
 */
@Entity
@Table(name = "fingerprints")
public class Fingerprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String bssid;

    @Column
    private String ssid;

    @Column(name = "rssi_moyen")
    private double rssiMoyen;

    @ManyToOne
    @JoinColumn(name = "poi_id", nullable = false)
    @JsonIgnore
    private PointLocalisation poi;

    // ===== CONSTRUCTEURS =====
    public Fingerprint() {}

    public Fingerprint(String bssid, String ssid, double rssiMoyen, PointLocalisation poi) {
        this.bssid     = bssid;
        this.ssid      = ssid;
        this.rssiMoyen = rssiMoyen;
        this.poi       = poi;
    }

    // ===== GETTERS =====
    public Long   getId()       { return id; }
    public String getBssid()    { return bssid; }
    public String getSsid()     { return ssid; }
    public double getRssiMoyen(){ return rssiMoyen; }
    public PointLocalisation getPoi() { return poi; }

    // Utile pour le frontend — infos du POI
    public String getPoiNom()    { return poi != null ? poi.getNom()  : null; }
    public Long   getPoiId()     { return poi != null ? poi.getId()   : null; }
    public String getPoiType()   { return poi != null ? poi.getType().name() : null; }
    public Long   getSalleId()   { return poi != null ? poi.getSalleId() : null; }
    public String getBlocCode()  { return poi != null ? poi.getBlocCode() : null; }

    // ===== SETTERS =====
    public void setBssid(String bssid)         { this.bssid = bssid; }
    public void setSsid(String ssid)           { this.ssid = ssid; }
    public void setRssiMoyen(double rssiMoyen) { this.rssiMoyen = rssiMoyen; }
    public void setPoi(PointLocalisation poi)  { this.poi = poi; }
}