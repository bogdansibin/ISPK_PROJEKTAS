package com.example.ispk_projektas;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CountyNewsActivity extends AppCompatActivity {

    private static final String TAG = "CountyNews";

    private RecyclerView newsRecyclerView;
    private String countyName;

    private NewsAdapter adapter;
    private List<NewsItem> allItems = new ArrayList<>();

    // Google News RSS date format
    private final SimpleDateFormat rssDateFormat =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

    // UI date format
    private final SimpleDateFormat dateUiFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // Filters
    private Long fromDateMillis = null;
    private Long toDateMillis = null;
    private String selectedCategory = ""; // "" = Visos

    // Category list
    private final String[] CATEGORIES = new String[]{
            "Visos",
            "Politika",
            "Sportas",
            "Ekonomika",
            "Kriminalai",
            "Kultūra",
            "Technologijos"
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_county);

        countyName = getIntent().getStringExtra("countyName");
        if (countyName == null) countyName = "Nežinoma apskritis";

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(countyName);
        }

        // RecyclerView
        newsRecyclerView = findViewById(R.id.newsRecyclerView);
        newsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NewsAdapter(new ArrayList<>(), item -> {
            Intent intent = new Intent(CountyNewsActivity.this, ArticleWebActivity.class);
            intent.putExtra("url", item.link);
            intent.putExtra("title", item.title);
            startActivity(intent);
        });
        newsRecyclerView.setAdapter(adapter);

        // Load news with default filters (no category, no date)
        fetchNews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_news_filter, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_filter) {
            showFilterDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Open dialog with category + date range filters */
    private void showFilterDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_news_filter, null);

        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        TextView fromDateText = dialogView.findViewById(R.id.fromDateText);
        TextView toDateText = dialogView.findViewById(R.id.toDateText);

        // Setup categories spinner
        ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item,
                        CATEGORIES);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(spinnerAdapter);

        // Pre-select current category
        int selectedIndex = 0;
        if (selectedCategory != null && !selectedCategory.isEmpty()) {
            for (int i = 0; i < CATEGORIES.length; i++) {
                if (CATEGORIES[i].equals(selectedCategory)) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        categorySpinner.setSelection(selectedIndex);

        // Pre-fill dates
        if (fromDateMillis != null) {
            fromDateText.setText(dateUiFormat.format(new Date(fromDateMillis)));
        } else {
            fromDateText.setText("Pasirinkti");
        }

        if (toDateMillis != null) {
            toDateText.setText(dateUiFormat.format(new Date(toDateMillis)));
        } else {
            toDateText.setText("Pasirinkti");
        }

        // Date pickers
        fromDateText.setOnClickListener(v -> openDatePicker(true, fromDateText));
        toDateText.setOnClickListener(v -> openDatePicker(false, toDateText));

        new AlertDialog.Builder(this)
                .setTitle("Filtruoti naujienas")
                .setView(dialogView)
                .setPositiveButton("Taikyti", (dialog, which) -> {
                    String chosenCategory = (String) categorySpinner.getSelectedItem();
                    selectedCategory = chosenCategory.equals("Visos") ? "" : chosenCategory;

                    // Re-load feed and then apply filters
                    fetchNews();
                })
                .setNegativeButton("Atstatyti", (dialog, which) -> {
                    // Reset filters
                    selectedCategory = "";
                    fromDateMillis = null;
                    toDateMillis = null;
                    fetchNews();
                })
                .setNeutralButton("Atšaukti", null)
                .show();
    }

    /** Open DatePicker dialog for from/to date */
    private void openDatePicker(boolean isFrom, TextView labelView) {
        final Calendar c = Calendar.getInstance();

        if (isFrom && fromDateMillis != null) {
            c.setTimeInMillis(fromDateMillis);
        } else if (!isFrom && toDateMillis != null) {
            c.setTimeInMillis(toDateMillis);
        }

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(this,
                (DatePicker view, int y, int m, int d) -> {
                    Calendar chosen = Calendar.getInstance();
                    if (isFrom) {
                        chosen.set(y, m, d, 0, 0, 0);
                        fromDateMillis = chosen.getTimeInMillis();
                        labelView.setText(dateUiFormat.format(chosen.getTime()));
                    } else {
                        chosen.set(y, m, d, 23, 59, 59);
                        toDateMillis = chosen.getTimeInMillis();
                        labelView.setText(dateUiFormat.format(chosen.getTime()));
                    }
                }, year, month, day);
        dpd.show();
    }

    private void fetchNews() {
        new FetchNewsTask().execute(countyName);
    }

    /** Build Google News RSS URL from county + category */
    private String buildGoogleNewsUrl(String countyName, String categoryFilter) {
        StringBuilder query = new StringBuilder();
  