package com.example.ispk_projektas;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Calendar;

public class Forum extends AppCompatActivity {

    private static final int MENU_GROUP_FILTER = 1;
    private static final int MENU_BASE_ID = 100;

    private FirebaseFirestore db;
    private LinearLayout postsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Drawable overflow = toolbar.getOverflowIcon();
        if (overflow != null) {
            overflow = DrawableCompat.wrap(overflow);
            DrawableCompat.setTint(
                    overflow,
                    ContextCompat.getColor(this, android.R.color.white)
            );
            toolbar.setOverflowIcon(overflow);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        db = FirebaseFirestore.getInstance();
        postsContainer = findViewById(R.id.postsContainer);

        FloatingActionButton fabAddPost = findViewById(R.id.fabAddPost);
        fabAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(Forum.this, CreateForum.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPosts();
    }

    private Query getBaseQuery() {
        return db.collection("irasai");
    }
    private void loadPosts() {
        loadPosts(getBaseQuery(), "Nėra įrašų");
    }

    private void loadPosts(Query query, @Nullable String emptyMessage) {
        postsContainer.removeAllViews();

        query.get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        if (emptyMessage != null) {
                            Toast.makeText(this, emptyMessage, Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot) {
                        String title = doc.getString("pavadinimas");
                        String category = doc.getString("kategorija");
                        String content = doc.getString("turinys");
                        String author = doc.getString("autoriusVardas");

                        View postView = getLayoutInflater()
                                .inflate(R.layout.item_forum_post, postsContainer, false);

                        TextView textTitle = postView.findViewById(R.id.textTitle);
                        TextView textMeta = postView.findViewById(R.id.textMeta);
                        TextView textContent = postView.findViewById(R.id.textContent);

                        textTitle.setText(title != null ? title : "(be pavadinimo)");

                        String meta = "";
                        if (category != null && !category.isEmpty()) {
                            meta += category;
                        }
                        if (author != null && !author.isEmpty()) {
                            if (!meta.isEmpty()) meta += " • ";
                            meta += author;
                        }
                        textMeta.setText(meta);

                        textContent.setText(content != null ? content : "");

                        postsContainer.addView(postView);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Klaida skaitant įrašus: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        String[] items = getResources().getStringArray(R.array.filter_items);

        for (int i = 0; i < items.length; i++) {
            menu.add(
                    MENU_GROUP_FILTER,
                    MENU_BASE_ID + i,
                    Menu.NONE,
                    items[i]
            );
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getGroupId() == MENU_GROUP_FILTER) {
            int which = item.getItemId() - MENU_BASE_ID;

            switch (which) {
                case 0:
                    showCategoryFilterDialog();
                    break;
                case 1:
                    showKeywordFilterDialog();
                    break;
                case 2:
                    showDateFilterDialog();
                    break;
                case 3:
                    filterMyPosts();
                    break;
                case 4:
                    loadPosts();
                    break;

            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showCategoryFilterDialog() {
        String[] kategorijos = getResources().getStringArray(R.array.kategorijos);

        new AlertDialog.Builder(this)
                .setTitle("Pasirinkite kategoriją")
                .setItems(kategorijos, (dialog, which) -> {
                    String selected = kategorijos[which];

                    Query q = getBaseQuery()
                            .whereEqualTo("kategorija", selected);

                    loadPosts(q, "Nėra įrašų šioje kategorijoje");
                })
                .setNegativeButton("Atšaukti", null)
                .show();
    }

    private void showKeywordFilterDialog() {
        final EditText input = new EditText(this);
        input.setHint("Įveskite raktažodį");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle("Filtruoti pagal raktažodį")
                .setView(input)
                .setPositiveButton("Filtruoti", (dialog, which) -> {
                    String keyword = input.getText().toString().trim().toLowerCase();
                    if (keyword.isEmpty()) {
                        Toast.makeText(this, "Raktažodis tuščias", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    postsContainer.removeAllViews();

                    getBaseQuery()
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (querySnapshot.isEmpty()) {
                                    Toast.makeText(this, "Nėra įrašų", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                int count = 0;

                                for (DocumentSnapshot doc : querySnapshot) {
                                    String title = doc.getString("pavadinimas");
                                    String content = doc.getString("turinys");
                                    String category = doc.getString("kategorija");
                                    String author = doc.getString("autoriusVardas");

                                    String t = title != null ? title.toLowerCase() : "";
                                    String c = content != null ? content.toLowerCase() : "";

                                    if (!t.contains(keyword) && !c.contains(keyword)) {
                                        continue; // neatitinka
                                    }

                                    count++;

                                    View postView = getLayoutInflater()
                                            .inflate(R.layout.item_forum_post, postsContainer, false);

                                    TextView textTitle = postView.findViewById(R.id.textTitle);
                                    TextView textMeta = postView.findViewById(R.id.textMeta);
                                    TextView textContent = postView.findViewById(R.id.textContent);

                                    textTitle.setText(title != null ? title : "(be pavadinimo)");

                                    String meta = "";
                                    if (category != null && !category.isEmpty()) {
                                        meta += category;
                                    }
                                    if (author != null && !author.isEmpty()) {
                                        if (!meta.isEmpty()) meta += " • ";
                                        meta += author;
                                    }
                                    textMeta.setText(meta);

                                    textContent.setText(content != null ? content : "");

                                    postsContainer.addView(postView);
                                }

                                if (count == 0) {
                                    Toast.makeText(this, "Pagal šį raktažodį įrašų nėra", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Klaida skaitant įrašus: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show()
                            );
                })
                .setNegativeButton("Atšaukti", null)
                .show();
    }

    private void showDateFilterDialog() {
        final Calendar cal = Calendar.getInstance();

        DatePickerDialog dpd = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar start = Calendar.getInstance();
                    start.set(year, month, dayOfMonth, 0, 0, 0);
                    start.set(Calendar.MILLISECOND, 0);

                    Calendar end = Calendar.getInstance();
                    end.set(year, month, dayOfMonth, 23, 59, 59);
                    end.set(Calendar.MILLISECOND, 999);

                    Query q = getBaseQuery()
                            .whereGreaterThanOrEqualTo("sukurta", start.getTime())
                            .whereLessThanOrEqualTo("sukurta", end.getTime());

                    loadPosts(q, "Tą dieną įrašų nėra");
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        dpd.show();
    }

    private void filterMyPosts() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Query q = getBaseQuery()
                .whereEqualTo("autoriusId", uid);

        loadPosts(q, "Neturi savo įrašų");
    }
}
