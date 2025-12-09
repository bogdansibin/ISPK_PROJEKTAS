package com.example.ispk_projektas;

public class NewsItem {
    public String title;
    public String link;
    public String description;
    public long pubDateMillis;
    public boolean isFavorite;
    public long views;
    public NewsItem(String title, String link, String description, long pubDateMillis) {
        this.title = title;
        this.link = link;
        this.description = description;
        this.pubDateMillis = pubDateMillis;
        this.isFavorite = false;
    }
}
