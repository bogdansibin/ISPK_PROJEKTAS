package com.example.ispk_projektas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private TextView nicknameTextView;
    private TextView roleTextView;
    private TextView emailTextView;

    private RecyclerView forumRecyclerView;
    private RecyclerView savedArticlesRecyclerView;

    private TextView forumEmptyTextView;
    private TextView savedEmptyTextView;

    private ForumPostAdapter forumAdapter;
    private final List<ForumPost> myPosts = new ArrayList<>();

    private NewsAdapter savedArticlesAdapter;
    private final List<NewsItem> savedArticles = new ArrayList<>();

    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.profileToolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        nicknameTextView = findViewById(R.id.profileNicknameTextView);
        roleTextView = findViewById(R.id.profileRoleTextView);
        emailTextView = findViewById(R.id.profileEmailTextView);

        forumRecyclerView = findViewById(R.id.forumRecyclerView);
        savedArticlesRecyclerView = findViewById(R.id.savedArticlesRecyclerView);

        forumEmptyTextView = findViewById(R.id.forumEmptyTextView);
        savedEmptyTextView = findViewById(R.id.savedEmptyTextView);

        // Intent data
        String nickname = getIntent().getStringExtra("nickname");
        String role = getIntent().getStringExtra("role");
        String email = getIntent().getStringExtra("email");

        if (nickname == null) nickname = "";
        if (role == null) role = "user";
        if (email == null) email = "";

        nicknameTextView.setText(nickname.isEmpty() ? "Vartotojas" : nickname);
        roleTextView.setText("Vaidmuo: " + role);
        emailTextView.setText(email);

        setupRecyclerViews();
        loadMyForumPosts();
        loadSavedArticles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyForumPosts();
        loadSavedArticles();
    }

    private void setupRecyclerViews() {
        forumRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        savedArticlesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        forumAdapter = new ForumPostAdapter(myPosts);
        forumRecyclerView.setAdapter(forumAdapter);

        savedArticlesAdapter = new NewsAdapter(
                this,
                new ArrayList<>(),   // NE perduodam savedArticles
                item -> {
                    Intent intent = new Intent(ProfileActivity.this, ArticleWebActivity.class);
                    intent.putExtra("url", item.link);
                    intent.putExtra("title", item.title);
                    startActivity(intent);
                }
        );
        savedArticlesRecyclerView.setAdapter(savedArticlesAdapter);

        savedArticlesRecyclerView.setAdapter(savedArticlesAdapter);
    }

    private void loadMyForumPosts() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showForumItems(new ArrayList<>());
            Toast.makeText(this, "Reikia būti prisijungus", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();

        db.collection("irasai")
                .whereEqualTo("autoriusId", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ForumPost> list = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        ForumPost post = new ForumPost();
                        post.setId(doc.getId());
                        post.setPavadinimas(doc.getString("pavadinimas"));
                        post.setKategorija(doc.getString("kategorija"));
                        post.setTurinys(doc.getString("turinys"));
                        post.setAutoriusId(doc.getString("autoriusId"));
                        post.setAutoriusVardas(doc.getString("autoriusVardas"));
                        post.setSukurta(doc.getTimestamp("sukurta"));

                        list.add(post);
                    }

                    showForumItems(list);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Klaida skaitant įrašus: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    showForumItems(new ArrayList<>());
                });
    }

    private void showForumItems(List<ForumPost> items) {
        if (items == null || items.isEmpty()) {
            forumEmptyTextView.setVisibility(View.VISIBLE);
            forumRecyclerView.setVisibility(View.GONE);
        } else {
            forumEmptyTextView.setVisibility(View.GONE);
            forumRecyclerView.setVisibility(View.VISIBLE);
            myPosts.clear();
            myPosts.addAll(items);
            forumAdapter.notifyDataSetChanged();
        }
    }

    private void loadSavedArticles() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showSavedArticles(new ArrayList<>());
            return;
        }

        String uid = user.getUid();

        db.collection("users")
                .document(uid)
                .collection("favorites")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<NewsItem> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String title = doc.getString("title");
                        String link = doc.getString("link");
                        String description = doc.getString("description");
                        Long pubDateMillis = doc.getLong("pubDateMillis");

                        if (title == null) title = "";
                        if (link == null) link = "";
                        if (description == null) description = "";
                        long pubMillis = (pubDateMillis != null) ? pubDateMillis : 0L;

                        NewsItem item = new NewsItem(title, link, description, pubMillis);
                        item.isFavorite = true;
                        list.add(item);
                    }
                    showSavedArticles(list);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Klaida skaitant išsaugotus straipsnius: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    showSavedArticles(new ArrayList<>());
                });
    }

    private void showSavedArticles(List<NewsItem> items) {
        if (items == null || items.isEmpty()) {
            savedEmptyTextView.setVisibility(View.VISIBLE);
            savedArticlesRecyclerView.setVisibility(View.GONE);
        } else {
            savedEmptyTextView.setVisibility(View.GONE);
            savedArticlesRecyclerView.setVisibility(View.VISIBLE);
        }

        // jei tau dar reikia savedArticles lauko – laikom kopiją
        savedArticles.clear();
        if (items != null) {
            savedArticles.addAll(items);
        }

        // į adapterį paduodam NAUJĄ list'ą, ne tą patį savedArticles
        savedArticlesAdapter.updateData(new ArrayList<>(savedArticles));
    }

}
