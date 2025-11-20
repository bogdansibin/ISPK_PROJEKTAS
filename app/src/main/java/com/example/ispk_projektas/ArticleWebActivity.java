package com.example.ispk_projektas;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class ArticleWebActivity extends AppCompatActivity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_webview);

        String url = getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");

        // Toolbar back arrow + title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title != null ? title : "Straipsnis");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        webView = findViewById(R.id.articleWebView);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true); // needed for many news sites
        settings.setDomStorageEnabled(true);

        // So links open inside this WebView, not external browser
        webView.setWebViewClient(new WebViewClient());

        if (url != null && !url.isEmpty()) {
            webView.loadUrl(url);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Back arrow in toolbar
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Overri