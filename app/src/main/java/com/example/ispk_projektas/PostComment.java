package com.example.ispk_projektas;

import com.google.firebase.Timestamp;

public class PostComment {

    private String id;
    private String tekstas;
    private String autoriusId;
    private String autoriusVardas;
    private Timestamp sukurta;

    private Boolean flagged;         // ar komentaras pažymėtas kaip netinkamas
    private String flagReason;       // priežastis
    private String flaggedBy;        // kas pažymėjo
    private Timestamp flaggedAt;     // kada pažymėjo

    // 🔹 NAUJA: atsakymų struktūra
    private String parentCommentId;   // null – top-level komentaras, ne null – atsakymas
    private transient int level;      // UI lygiui (įtrauka), nerašomas į DB


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
    public Boolean getFlagged() { return flagged; }
    public void setFlagged(Boolean flagged) { this.flagged = flagged; }

    public String getFlagReason() { return flagReason; }
    public void setFlagReason(String flagReason) { this.flagReason = flagReason; }

    public String getFlaggedBy() { return flaggedBy; }
    public void setFlaggedBy(String flaggedBy) { this.flaggedBy = flaggedBy; }

    public Timestamp getFlaggedAt() { return flaggedAt; }
    public void setFlaggedAt(Timestamp flaggedAt) { this.flaggedAt = flaggedAt; }

    public String getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
