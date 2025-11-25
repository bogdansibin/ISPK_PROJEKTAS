package com.example.ispk_projektas;

import com.google.firebase.Timestamp;

public class PostComment {

    private String id;
    private String tekstas;
    private String autoriusId;
    private String autoriusVardas;
    private Timestamp sukurta;

    public PostComment() {
        // Firestore needs empty constructor
    }

    public PostComment(String id, String tekstas, String autoriusId,
                       String autoriusVardas, Timestamp sukurta) {
        this.id = id;
        this.tekstas = tekstas;
        this.autoriusId = autoriusId;
        this.autoriusVardas = autoriusVardas;
        this.sukurta = sukurta;
    }

    public String getId() { return id; }
    public String getTekstas() { return tekstas; }
    public String getAutoriusId() { return autoriusId; }
    public String getAutoriusVardas() { return autoriusVardas; }
    public Timestamp getSukurta() { return sukurta; }

    public void setId(String id) { this.id = id; }
    public void setTekstas(String tekstas) { this.tekstas = tekstas; }
    public void setAutoriusId(String autoriusId) { this.autoriusId = autoriusId; }
    public void setAutoriusVardas(String autoriusVardas) { this.autoriusVardas = autoriusVardas; }
    public void setSukurta(Timestamp sukurta) { this.sukurta = sukurta; }
}
