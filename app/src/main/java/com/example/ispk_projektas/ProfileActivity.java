package com.example.ispk_projektas;

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
    private RecyclerView.Adapter<?> savedAdapter; // paliekam placeholder

    private final List<ForumPost> myPosts = new ArrayList<>();

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
        loadMyForumPosts();   // 👈 čia realiai nuskaitom
        showSavedArticles(new ArrayList<>()); // dar placeholder
    }

    @Override
    protected void onResume() {
        super.onResume();
        // jei vartotojas sukūrė naują įrašą, grįžus – sąrašas atsinaujins
        loadMyForumPosts();
    }

    private void setupRecyclerViews() {
        forumRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        savedArticlesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        forumAdapter = new ForumPostAdapter(myPosts);
        forumRecyclerView.setAdapter(forumAdapter);

        // kol kas paliekam placeholder adapterį tik „saved“ daliai, jei nori
        savedAdapter = new PlaceholderAdapter("Saved article item");
        savedArticlesRecyclerView.setAdapter(savedAdapter);
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
                //.orderBy("sukurta", Query.Direction.DESCENDING) // jei pridėsi indeksą
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

    // --- palieku tavo PlaceholderAdapter tik „saved“ daliai ---

    private static class PlaceholderAdapter extends RecyclerView.Adapter<PlaceholderViewHolder> {

        private final String label;

        PlaceholderAdapter(String label) {
            this.label = label;
        }

        @Override
        public PlaceholderViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(16, 16, 16, 16);
            tv.setTextSize(14);
            return new PlaceholderViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(PlaceholderViewHolder holder, int position) {
            holder.textView.setText(label + " #" + (position + 1));
        }

        @Override
        public int getItemCount() {
            return 0;
        }
    }

    private static class PlaceholderViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        PlaceholderViewHolder(TextView itemView) {
            super(itemView);
            textView = itemView;
        }
    }

    // savedArticles – kol kas dar neužkraunam iš Firestore
    private void showSavedArticles(List<Object> items) {
        if (items == null || items.isEmpty()) {
            savedEmptyTextView.setVisibility(View.VISIBLE);
            savedArticlesRecyclerView.setVisibility(View.GONE);
        } else {
            savedEmptyTextView.setVisibility(View.GONE);
            savedArticlesRecyclerView.setVisibility(View.VISIBLE);
            // TODO: real data later
        }
    }
}
