package com.example.ispk_projektas;

public class NewsItem {
    public String title;
    public String link;
    public String description;
    public long pubDateMillis; // for filtering

    public NewsItem(String title, String link, String description, long pubDateMillis) {
        this.title = title;
        this.link = link;
        this.description = description;
        this.pubDateMillis = pubDateMillis;
    }
}
