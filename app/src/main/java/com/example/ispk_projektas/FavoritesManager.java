package com.example.ispk_projektas;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class FavoritesManager {

    private static final String PREF_NAME = "news_favorites_prefs";
    private static final String KEY_URLS = "favorite_urls";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static Set<String> getFavoriteUrls(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return new HashSet<>(prefs.getStringSet(KEY_URLS, new HashSet<>()));
    }

    public static boolean isFavorite(Context context, String url) {
        return getFavoriteUrls(context).contains(url);
    }

    public static void setFavorite(Context context, String url, boolean favorite) {
        SharedPreferences prefs = getPrefs(context);
        Set<String> urls = new HashSet<>(prefs.getStringSet(KEY_URLS, new HashSet<>()));
        if (favorite) {
            urls.add(url);
        } else {
            urls.remove(url);
        }
        prefs.edit().putStringSet(KEY_URLS, urls).apply();
    }
}
