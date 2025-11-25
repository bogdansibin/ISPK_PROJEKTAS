package com.example.ispk_projektas;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PostDetailActivity extends AppCompatActivity {

    private TextView detailTitleTextView;
    private TextView detailMetaTextView;
    private TextView detailContentTextView;
    private RecyclerView commentsRecyclerView;
    private EditText commentEditText;
    private Button sendCommentButton;

    private FirebaseFirestore db;
    private String postId;
    private String currentUserId;
    private String currentUserName;
    private boolean isAdmin = false;

    private final List<PostComment> comments = new ArrayList<>();
    private CommentsAdapter commentsAdapter;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        detailTitleTextView = findViewById(R.id.detailTitleTextView);
        detailMetaTextView = findViewById(R.id.detailMetaTextView);
        detailContentTextView = findViewById(R.id.detailContentTextView);
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        commentEditText = findViewById(R.id.commentEditText);
        sendCommentButton = findViewById(R.id.sendCommentButton);

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Gauti postId iš intent
        postId = getIntent().getStringExtra("postId");

        // Įdėti basic info iš intent (jei atsiuntei iš Forum)
        String title = getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content");
        String category = getIntent().getStringExtra("category");
        String author = getIntent().getStringExtra("author");

        if (title != null) {
            detailTitleTextView.setText(title);
        }
        String meta = "";
        if (category != null && !category.isEmpty()) meta += category;
        if (author != null && !author.isEmpty()) {
            if (!meta.isEmpty()) meta += " • ";
            meta += author;
        }
        detailMetaTextView.setText(meta);
        if (content != null) {
            detailContentTextView.setText(content);
        }

        // RecyclerView setup
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsAdapter = new CommentsAdapter(
                comments,
                isAdmin,
                this::onCommentLongClick
        );
        commentsRecyclerView.setAdapter(commentsAdapter);

        // pasiimti user role + vardą
        if (currentUserId != null) {
            db.collection("users")
                    .document(currentUserId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String role = doc.getString("role");
                        isAdmin = "admin".equals(role);
                        currentUserName = doc.getString("nickname");
                        commentsAdapter.setAdmin(isAdmin);
                    })
                    .addOnFailureListener(e -> {
                        isAdmin = false;
                        commentsAdapter.setAdmin(false);
                    });
        }

        // tiksliau užkraunam patį įrašą (kad gautume sukurta laiką, jei reikia)
        loadPostDetails();
        loadComments();

        sendCommentButton.setOnClickListener(v -> sendComment());
    }

    private void loadPostDetails() {
        if (postId == null) return;

        db.collection("irasai")
                .document(postId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String title = doc.getString("pavadinimas");
                    String content = doc.getString("turinys");
                    String category = doc.getString("kategorija");
                    String author = doc.getString("autoriusVardas");
                    Timestamp sukurta = doc.getTimestamp("sukurta");

                    if (title != null) {
                        detailTitleTextView.setText(title);
                    }

                    String meta = "";
                    if (category != null && !category.isEmpty()) meta += category;
                    if (author != null && !author.isEmpty()) {
                        if (!meta.isEmpty()) meta += " • ";
                        meta += author;
                    }
                    if (sukurta != null) {
                        if (!meta.isEmpty()) meta += " • ";
                        meta += dateFormat.format(sukurta.toDate());
                    }
                    detailMetaTextView.setText(meta);

                    if (content != null) {
                        detailContentTextView.setText(content);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Klaida skaitant įrašą: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void loadComments() {
        if (postId == null) return;

        db.collection("irasai")
                .document(postId)
                .collection("komentarai")
                .orderBy("sukurta", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(sn -> {
                    List<PostComment> list = new ArrayList<>();
                    for (DocumentSnapshot doc : sn.getDocuments()) {
                        PostComment c = doc.toObject(PostComment.class);
                        if (c != null) {
                            c.setId(doc.getId());
                            list.add(c);
                        }
                    }
                    commentsAdapter.replaceData(list);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Klaida skaitant komentarus: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void sendComment() {
        if (postId == null) return;

        String text = commentEditText.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Komentaras tuščias", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId == null) {
            Toast.makeText(this, "Reikia būti prisijungus", Toast.LENGTH_SHORT).show();
            return;
        }

        String nameToUse = currentUserName;
        if (nameToUse == null || nameToUse.isEmpty()) {
            nameToUse = "Anonimas";
        }

        PostComment comment = new PostComment();
        comment.setTekstas(text);
        comment.setAutoriusId(currentUserId);
        comment.setAutoriusVardas(nameToUse);
        comment.setSukurta(Timestamp.now());

        db.collection("irasai")
                .document(postId)
                .collection("komentarai")
                .add(comment)
                .addOnSuccessListener(ref -> {
                    commentEditText.setText("");
                    loadComments();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Nepavyko išsiųsti komentaro: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void onCommentLongClick(PostComment comment) {
        if (!isAdmin || postId == null || comment.getId() == null) {
            return;
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle("Ištrinti komentarą")
                .setMessage("Ar tikrai norite ištrinti šį komentarą?")
                .setPositiveButton("Taip", (dialog, which) -> {
                    db.collection("irasai")
                            .document(postId)
                            .collection("komentarai")
                            .document(comment.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Komentaras ištrintas", Toast.LENGTH_SHORT).show();
                                loadComments();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Nepavyko ištrinti komentaro: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show()
                            );
                })
                .setNegativeButton("Ne", null)
                .show();
    }
}
