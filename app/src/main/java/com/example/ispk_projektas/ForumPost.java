package com.example.ispk_projektas;

import com.google.firebase.Timestamp;

public class ForumPost {
    private String id;
    private String pavadinimas;
    private String kategorija;
    private String turinys;
    private String autoriusId;
    private String autoriusVardas;
    private Timestamp sukurta;

    public ForumPost() {
        // Firestore needs empty constructor
    }

    public ForumPost(String id, String pavadinimas, String kategorija,
                     String turinys, String autoriusId, String autoriusVardas,
                     Timestamp sukurta) {
        this.id = id;
        this.pavadinimas = pavadinimas;
        this.kategorija = kategorija;
        this.turinys = turinys;
        this.autoriusId = autoriusId;
        this.autoriusVardas = autoriusVardas;
        this.sukurta = sukurta;
    }

    public String getId() { return id; }
    public String getPavadinimas() { return pavadinimas; }
    public String getKategorija() { return kategorija; }
    public String getTurinys() { return turinys; }
    public String getAutoriusId() { return autoriusId; }
    public String getAutoriusVardas() { return autoriusVardas; }
    public Timestamp getSukurta() { return sukurta; }

    public void setId(String id) { this.id = id; }
    public void setPavadinimas(String pavadinimas) { this.pavadinimas = pavadinimas; }
    public void setKategorija(String kategorija) { this.kategorija = kategorija; }
    public void setTurinys(String turinys) { this.turinys = turinys; }
    public void setAutoriusId(String autoriusId) { this.autoriusId = autoriusId; }
    public void setAutoriusVardas(String autoriusVardas) { this.autoriusVardas = autoriusVardas; }
    public void setSukurta(Timestamp sukurta) { this.sukurta = sukurta; }
}
