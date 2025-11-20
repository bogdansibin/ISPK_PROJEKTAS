package com.example.ispk_projektas;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostDetailActivity extends AppCompatActivity {

    private TextView detailTitleTextView;
    private TextView detailMetaTextView;
    private TextView detailContentTextView;
    private RecyclerView commentsRecyclerView;
    private EditText commentEditText;
    private Button sendCommentButton;

    private CommentsAdapter commentsAdapter;
    private final List<PostComment> comments = new ArrayList<>();

    private FirebaseFirestore db;
    private String postId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        db = FirebaseFirestore.getInstance();

        detailTitleTextView = findViewById(R.id.detailTitleTextView);
        detailMetaTextView = findViewById(R.id.detailMetaTextView);
        detailContentTextView = findViewById(R.id.detailContentTextView);
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        commentEditText = findViewById(R.id.commentEditText);
        sendCommentButton = findViewById(R.id.sendCommentButton);

        // gautas iš Intent
        postId = getIntent().getStringExtra("postId");
        String title = getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content");
        String category = getIntent().getStringExtra("category");
        String author = getIntent().getStringExtra("author");

        detailTitleTextView.setText(title != null ? title : "(be pavadinimo)");

        String meta = "";
        if (category != null && !category.isEmpty()) meta += category;
        if (author != null && !author.isEmpty()) {
            if (!meta.isEmpty()) meta += " • ";
            meta += author;
        }
        detailMetaTextView.setText(meta);
        detailContentTextView.setText(content != null ? content : "");

        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsAdapter = new CommentsAdapter(comments);
        commentsRecyclerView.setAdapter(commentsAdapter);

        sendCommentButton.setOnClickListener(v -> sendComment());

        loadComments();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadComments();
    }

    private void loadComments() {
        if (postId == null) return;

        db.collection("irasai")
                .document(postId)
                .collection("komentarai")
                .orderBy("sukurta", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<PostComment> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        PostComment c = new PostComment();
                        c.setId(doc.getId());
                        c.setTekstas(doc.getString("tekstas"));
                        c.setAutoriusId(doc.getString("autoriusId"));
                        c.setAutoriusVardas(doc.getString("autoriusVardas"));
                        c.setSukurta(doc.getTimestamp("sukurta"));
                        list.add(c);
                    }
                    commentsAdapter.replaceData(list);
                    commentsRecyclerView.setVisibility(
                            list.isEmpty() ? View.GONE : View.VISIBLE
                    );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Klaida skaitant komentarus: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void sendComment() {
        String text = commentEditText.getText().toString().trim();
        if (text.isEmpty()) {
            commentEditText.setError("Įveskite komentarą");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Reikia būti prisijungus", Toast.LENGTH_SHORT).show();
            return;
        }

        String authorId = user.getUid();

        db.collection("users")
                .document(authorId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String authorName = userDoc.getString("nickname");
                    if (authorName == null || authorName.isEmpty()) {
                        authorName = user.getEmail() != null ? user.getEmail() : "Anonimas";
                    }

                    sendCommentToFirestore(authorId, authorName, text);
                })
                .addOnFailureListener(e -> {
                    String fallbackName = user.getEmail() != null ? user.getEmail() : "Anonimas";
                    sendCommentToFirestore(authorId, fallbackName, text);
                });
    }


    private void sendCommentToFirestore(String authorId, String authorName, String text) {
        Map<String, Object> comment = new HashMap<>();
        comment.put("tekstas", text);
        comment.put("autoriusId", authorId);
        comment.put("autoriusVardas", authorName);
        comment.put("sukurta", FieldValue.serverTimestamp());

        db.collection("irasai")
                .document(postId)
                .collection("komentarai")
                .add(comment)
                .addOnSuccessListener(docRef -> {
                    commentEditText.setText("");
                    loadComments();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Klaida siunčiant komentarą: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

}
