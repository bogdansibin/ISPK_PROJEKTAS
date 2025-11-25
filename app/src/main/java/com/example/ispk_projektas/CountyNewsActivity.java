package com.example.ispk_projektas;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private long loadStartTime = 0;
    private long loadEndTime = 0;


    // saugom favorites linkus
    private final List<String> favoriteLinks = new ArrayList<>();

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

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        countyName = getIntent().getStringExtra("countyName");
        if (countyName == null) countyName = "Nežinoma apskritis";

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(countyName);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // RecyclerView
        newsRecyclerView = findViewById(R.id.newsRecyclerView);
        newsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NewsAdapter(
                this,
                new ArrayList<>(),
                item -> {
                    Intent intent = new Intent(CountyNewsActivity.this, ArticleWebActivity.class);
                    intent.putExtra("url", item.link);
                    intent.putExtra("title", item.title);
                    startActivity(intent);
                }
        );
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
                    fetchNews();
                })
                .setNegativeButton("Atstatyti", (dialog, which) -> {
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
        query.append(countyName).append(" naujienos");
        if (categoryFilter != null && !categoryFilter.isEmpty()) {
            query.append(" ").append(categoryFilter);
        }

        try {
            String encodedQuery = java.net.URLEncoder.encode(query.toString(), "UTF-8");
            String url = "https://news.google.com/rss/search?q=" + encodedQuery +
                    "&hl=lt&gl=LT&ceid=LT:lt";
            Log.d(TAG, "Request URL: " + url);
            return url;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** AsyncTask to fetch and parse RSS */
    private class FetchNewsTask extends AsyncTask<String, Void, List<NewsItem>> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadStartTime = System.currentTimeMillis();
            Log.d(TAG, "News load started");
        }

        @Override
        protected List<NewsItem> doInBackground(String... params) {
            String county = params[0];

            String urlString = buildGoogleNewsUrl(county, selectedCategory);
            if (urlString == null) return null;

            List<NewsItem> result = new ArrayList<>();
            InputStream inputStream = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.connect();

                int code = connection.getResponseCode();
                Log.d(TAG, "HTTP code = " + code);

                if (code == HttpURLConnection.HTTP_OK) {
                    inputStream = connection.getInputStream();
                    result = parseRss(inputStream);
                } else {
                    return null;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching news", e);
                return null;
            } finally {
                try {
                    if (inputStream != null) inputStream.close();
                } catch (Exception ignored) {}
                if (connection != null) connection.disconnect();
            }
            return result;
        }

        @Override
        protected void onPostExecute(List<NewsItem> newsItems) {
            if (newsItems == null) {
                Toast.makeText(CountyNewsActivity.this,
                        "Nepavyko įkelti naujienų", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "parseRss returned items: " + newsItems.size());
            allItems = newsItems;

            loadFavoritesAndShow();
        }
    }

    /** Nuskaitom favorites iš Firestore ir pažymime allItems */
    private void loadFavoritesAndShow() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            favoriteLinks.clear();
            applyFavoritesToItems();
            applyFiltersAndShow();
            return;
        }

        String uid = user.getUid();
        db.collection("users")
                .document(uid)
                .collection("favorites")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    favoriteLinks.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String link = doc.getString("link");
                        if (link != null) {
                            favoriteLinks.add(link);
                        }
                    }
                    applyFavoritesToItems();
                    applyFiltersAndShow();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Klaida nuskaitant favorites: ", e);
                    applyFavoritesToItems();
                    applyFiltersAndShow();
                });
    }

    /** pažymim allItems pagal favoriteLinks */
    private void applyFavoritesToItems() {
        for (NewsItem item : allItems) {
            item.isFavorite = favoriteLinks.contains(item.link);
        }
    }

    /** Apply date filters to allItems and show result in adapter */
    private void applyFiltersAndShow() {
        List<NewsItem> filtered = new ArrayList<>();

        for (NewsItem item : allItems) {
            long t = item.pubDateMillis;

            // Filter by date (if pubDate missing (0), drop when filters are set)
            if (fromDateMillis != null && (t == 0 || t < fromDateMillis)) continue;
            if (toDateMillis != null && (t == 0 || t > toDateMillis)) continue;

            filtered.add(item);
        }

        adapter.updateData(filtered);
        Toast.makeText(this,
                "Rasta straipsnių: " + filtered.size(),
                Toast.LENGTH_SHORT).show();

        loadEndTime = System.currentTimeMillis();
        long duration = loadEndTime - loadStartTime;

        Log.d(TAG, "Total news load time = " + duration + " ms");
        Toast.makeText(this,
                "Užkrovimo laikas: " + duration + " ms",
                Toast.LENGTH_LONG).show();

    }

    /** Parse RSS into list of NewsItem (title, link, pubDateMillis) */
    private List<NewsItem> parseRss(InputStream inputStream) throws Exception {
        List<NewsItem> items = new ArrayList<>();

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(false);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(inputStream, "UTF-8");

        boolean insideItem = false;
        String title = null, link = null, description = null, pubDateStr = null;

        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String name;
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    name = xpp.getName();
                    if ("item".equalsIgnoreCase(name)) {
                        insideItem = true;
                        title = link = description = pubDateStr = null;
                    } else if (insideItem) {
                        if ("title".equalsIgnoreCase(name)) {
                            title = xpp.nextText();
                        } else if ("link".equalsIgnoreCase(name)) {
                            link = xpp.nextText();
                        } else if ("description".equalsIgnoreCase(name)) {
                            description = xpp.nextText();
                        } else if ("pubDate".equalsIgnoreCase(name)) {
                            pubDateStr = xpp.nextText();
                            Log.d(TAG, "pubDate raw = " + pubDateStr);
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    name = xpp.getName();
                    if ("item".equalsIgnoreCase(name) && insideItem) {
                        long pubMillis = 0;
                        if (pubDateStr != null) {
                            try {
                                Date d = rssDateFormat.parse(pubDateStr);
                                if (d != null) pubMillis = d.getTime();
                            } catch (ParseException e) {
                                Log.w(TAG, "Failed to parse pubDate: " + pubDateStr, e);
                            }
                        }
                        items.add(new NewsItem(
                                title != null ? title : "",
                                link != null ? link : "",
                                description != null ? description : "",
                                pubMillis
                        ));
                        insideItem = false;
                    }
                    break;
            }
            eventType = xpp.next();
        }

        return items;
    }
}
