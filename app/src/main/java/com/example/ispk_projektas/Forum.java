package com.example.ispk_projektas;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Forum extends AppCompatActivity {

    private static final int MENU_GROUP_FILTER = 1;
    private static final int MENU_BASE_ID = 100;

    private FirebaseFirestore db;
    private LinearLayout postsContainer;

    private boolean isAdmin = false;
    private String currentUserId = null;

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

        // Nustatom prisijungusį vartotoją ir ar jis adminas
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            db.collection("users")
                    .document(currentUserId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String role = doc.getString("role");
                        isAdmin = "admin".equals(role);
                        loadPosts();
                    })
                    .addOnFailureListener(e -> {
                        isAdmin = false;
                        loadPosts();
                    });
        } else {
            isAdmin = false;
            loadPosts();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // jei admin statusas jau žinomas – tiesiog atnaujinam sąrašą
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

                    List<DocumentSnapshot> myPosts = new ArrayList<>();
                    List<DocumentSnapshot> otherPosts = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot) {
                        String authorId = doc.getString("autoriusId");
                        if (currentUserId != null && currentUserId.equals(authorId)) {
                            myPosts.add(doc);
                        } else {
                            otherPosts.add(doc);
                        }
                    }

                    // 1) Mano įrašai
                    if (!myPosts.isEmpty()) {
                        // Header "Mano įrašai"
                        View headerMy = getLayoutInflater()
                                .inflate(R.layout.forum_section_header, postsContainer, false);
                        TextView headerMyText = headerMy.findViewById(R.id.sectionTitleTextView);
                        headerMyText.setText("Mano įrašai");
                        postsContainer.addView(headerMy);

                        for (DocumentSnapshot doc : myPosts) {
                            addPostViewFromDoc(doc);
                        }
                    }

                    // 2) Kitų įrašai
                    if (!otherPosts.isEmpty()) {
                        View headerOthers = getLayoutInflater()
                                .inflate(R.layout.forum_section_header, postsContainer, false);
                        TextView headerOthersText = headerOthers.findViewById(R.id.sectionTitleTextView);
                        headerOthersText.setText("Kitų įrašai");
                        postsContainer.addView(headerOthers);

                        for (DocumentSnapshot doc : otherPosts) {
                            addPostViewFromDoc(doc);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Klaida skaitant įrašus: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void addPostViewFromDoc(DocumentSnapshot doc) {
        String postId = doc.getId();
        String title = doc.getString("pavadinimas");
        String category = doc.getString("kategorija");
        String content = doc.getString("turinys");
        String author = doc.getString("autoriusVardas");
        String authorId = doc.getString("autoriusId");

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

        // Atidarymas
        postView.setOnClickListener(v -> {
            Intent intent = new Intent(Forum.this, PostDetailActivity.class);
            intent.putExtra("postId", postId);
            intent.putExtra("title", title);
            intent.putExtra("content", content);
            intent.putExtra("category", category);
            intent.putExtra("author", author);
            startActivity(intent);
        });

        // Trinti – admin ARBA autorius
        boolean canDelete = false;
        if (currentUserId != null && authorId != null) {
            canDelete = isAdmin || currentUserId.equals(authorId);
        } else if (isAdmin) {
            canDelete = true;
        }

        if (canDelete) {
            postView.setOnLongClickListener(v -> {
                new android.app.AlertDialog.Builder(Forum.this)
                        .setTitle("Ištrinti įrašą")
                        .setMessage("Ar tikrai norite ištrinti šį įrašą ir visus jo komentarus?")
                        .setPositiveButton("Taip", (dialog, which) -> {
                            deletePostWithComments(postId);
                        })
                        .setNegativeButton("Ne", null)
                        .show();
                return true;
            });
        }

        postsContainer.addView(postView);
    }


    private void deletePostWithComments(String postId) {
        // Pirmiausia ištrinam komentarus subkolekcijoje
        CollectionReference commentsRef = db.collection("irasai")
                .document(postId)
                .collection("komentarai");

        commentsRef.get()
                .addOnSuccessListener(sn -> {
                    // trinam visus komentarus
                    sn.getDocuments().forEach(doc -> doc.getReference().delete());

                    // tada trinam patį įrašą
                    db.collection("irasai")
                            .document(postId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Įrašas ištrintas", Toast.LENGTH_SHORT).show();
                                loadPosts();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Nepavyko ištrinti įrašo: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Nepavyko gauti komentarų: " + e.getMessage(),
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
                                        continue;
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

                                    // click atidaryti detalę
                                    final String postId = doc.getId();
                                    postView.setOnClickListener(v -> {
                                        Intent intent = new Intent(Forum.this, PostDetailActivity.class);
                                        intent.putExtra("postId", postId);
                                        intent.putExtra("title", title);
                                        intent.putExtra("content", content);
                                        intent.putExtra("category", category);
                                        intent.putExtra("author", author);
                                        startActivity(intent);
                                    });

                                    // long click trynimui – jei admin
                                    if (isAdmin) {
                                        postView.setOnLongClickListener(v -> {
                                            new AlertDialog.Builder(Forum.this)
                                                    .setTitle("Ištrinti įrašą")
                                                    .setMessage("Ar tikrai norite ištrinti šį įrašą ir visus jo komentarus?")
                                                    .setPositiveButton("Taip", (dialog1, which1) -> {
                                                        deletePostWithComments(postId);
                                                    })
                                                    .setNegativeButton("Ne", null)
                                                    .show();
                                            return true;
                                        });
                                    }

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
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Reikia būti prisijungus", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Query q = getBaseQuery()
                .whereEqualTo("autoriusId", uid);

        loadPosts(q, "Neturi savo įrašų");
    }
}
