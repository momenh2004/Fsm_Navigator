package com.fsm.navigator.model;

/**
 * PointInteret.java – Modèle d'un point d'intérêt (salle, amphi, etc.)
 * Alimenté par le backend via GET /api/blocs
 */
public class PointInteret {

    private int     id;
    private String  nom;
    private String  categorie;
    private String  batiment;    // nom du bloc
    private String  etage;       // "Rez-de-chaussée", "1er étage"...
    private String  blocId;      // code du bloc (ex: "B3", "B1")
    private boolean accessiblePmr;

    public PointInteret(int id, String nom, String categorie,
                        String batiment, String etage) {
        this.id        = id;
        this.nom       = nom;
        this.categorie = categorie;
        this.batiment  = batiment;
        this.etage     = etage;
    }

    // ===== GETTERS =====
    public int     getId()            { return id; }
    public String  getNom()           { return nom; }
    public String  getCategorie()     { return categorie; }
    public String  getBatiment()      { return batiment; }
    public String  getEtage()         { return etage; }
    public String  getBlocId()        { return blocId; }
    public boolean isAccessiblePmr()  { return accessiblePmr; }

    // ===== SETTERS =====
    public void setBlocId(String blocId)          { this.blocId = blocId; }
    public void setAccessiblePmr(boolean pmr)     { this.accessiblePmr = pmr; }
}