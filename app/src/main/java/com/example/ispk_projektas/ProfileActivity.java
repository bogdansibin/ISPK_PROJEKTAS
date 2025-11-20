package com.example.ispk_projektas;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

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

    // TODO: replace with real adapters later
    private RecyclerView.Adapter<?> forumAdapter;
    private RecyclerView.Adapter<?> savedAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Toolbar with back arrow
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

        // Read data from Intent (sent from MapActivity / Login)
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

        // For now, fake empty data; later you’ll load from Firestore
        showForumItems(new ArrayList<>());        // empty -> show "no items"
        showSavedArticles(new ArrayList<>());     // empty -> show "no saved articles"
    }

    private void setupRecyclerViews() {
        forumRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        savedArticlesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // TODO: create real models & adapters
        forumAdapter = new PlaceholderAdapter("Forum activity item");
        savedAdapter = new PlaceholderAdapter("Saved article item");

        forumRecyclerView.setAdapter(forumAdapter);
        savedArticlesRecyclerView.setAdapter(savedAdapter);
    }

    /**
     * Replace List<Object> with your real ForumPost / ForumActivity model later.
     */
    private void showForumItems(List<Object> items) {
        if (items == null || items.isEmpty()) {
            forumEmptyTextView.setVisibility(android.view.View.VISIBLE);
            forumRecyclerView.setVisibility(android.view.View.GONE);
        } else {
            forumEmptyTextView.setVisibility(android.view.View.GONE);
            forumRecyclerView.setVisibility(android.view.View.VISIBLE);
            // TODO: update adapter with real data
        }
    }

    /**
     * Replace List<Object> with your real SavedArticle / NewsItem model later.
     */
    private void showSavedArticles(List<Object> items) {
        if (items == null || items.isEmpty()) {
            savedEmptyTextView.setVisibility(android.view.View.VISIBLE);
            savedArticlesRecyclerView.setVisibility(android.view.View.GONE);
        } else {
            savedEmptyTextView.setVisibility(android.view.View.GONE);
            savedArticlesRecyclerView.setVisibility(android.view.View.VISIBLE);
            // TODO: update adapter with real data
        }
    }

    /**
     * Super simple placeholder adapter to render dummy rows until you plug in real data.
     */
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
            // 0 by default -> list hidden, empty text visible
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
}
