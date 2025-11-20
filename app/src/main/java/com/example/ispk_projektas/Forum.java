package com.example.ispk_projektas;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.ispk_projektas.CreateForum;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

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

        // pakeičiam overflow ikoną į baltą
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

        // Firestore
        db = FirebaseFirestore.getInstance();
        postsContainer = findViewById(R.id.postsContainer);

        FloatingActionButton fabAddPost = findViewById(R.id.fabAddPost);
        fabAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(Forum.this, CreateForum.class);
            startActivity(intent);
        });

    }

    protected void onResume() {
        super.onResume();
        loadPosts();   // 👈 kiekvieną kartą, kai Forum grįžta į ekraną, sąrašas persikrauna
    }

    private void loadPosts() {
        postsContainer.removeAllViews();

        db.collection("irasai")
                .orderBy("sukurta", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "Nėra įrašų", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot) {
                        String postId = doc.getId();
                        String title = doc.getString("pavadinimas");
                        String category = doc.getString("kategorija");
                        String content = doc.getString("turinys");
                        String author = doc.getString("autoriusVardas");

                        // Sukuriam kortelę iš item_forum_post.xml
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

                        postView.setOnClickListener(v -> {
                            Intent intent = new Intent(Forum.this, PostDetailActivity.class);
                            intent.putExtra("postId", postId);
                            intent.putExtra("title", title);
                            intent.putExtra("content", content);
                            intent.putExtra("category", category);
                            intent.putExtra("author", author);
                            startActivity(intent);
                        });

                        postsContainer.addView(postView);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Klaida skaitant įrašus: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Pasiimam string-array iš resources
        String[] items = getResources().getStringArray(R.array.filter_items);

        for (int i = 0; i < items.length; i++) {
            menu.add(
                    MENU_GROUP_FILTER,          // groupId
                    MENU_BASE_ID + i,           // itemId
                    Menu.NONE,                  // order
                    items[i]                    // tekstas
            );
            // showAsAction nenaudojam -> visi punktai bus po trim taškais (overflow menu)
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (item.getGroupId() == MENU_GROUP_FILTER) {
            int which = id - MENU_BASE_ID; // indeksas masyve

            switch (which) {
                case 0:
                    // Filtruoti pagal kategorijas
                    // TODO: pridėk savo logiką
                    break;
                case 1:
                    // Filtruoti pagal raktažodį
                    break;
                case 2:
                    // Filtruoti pagal datą
                    break;
                case 3:
                    // Rodyti tik mano įrašus
                    break;
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}