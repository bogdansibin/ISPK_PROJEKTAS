package com.example.ispk_projektas;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.ispk_projektas.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateForum extends AppCompatActivity {

    private EditText editTitle;
    private EditText editContent;
    private Spinner spinnerCategory;
    private Button buttonCreate;

    private String currentNick;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_forum);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());


        db = FirebaseFirestore.getInstance();

        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        buttonCreate = findViewById(R.id.button);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            db.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            currentNick = doc.getString("nickname");
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Nepavyko nuskaityti profilio: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        }

        // 2. Button click -> call createPost()
        buttonCreate.setOnClickListener(v -> createPost());
    }

    private void createPost() {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem() != null
                ? spinnerCategory.getSelectedItem().toString()
                : "";

        // Paprasta validacija
        if (title.isEmpty()) {
            editTitle.setError("Įveskite pavadinimą");
            return;
        }
        if (content.isEmpty()) {
            editContent.setError("Įveskite turinį");
            return;
        }


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Reikia būti prisijungus", Toast.LENGTH_SHORT).show();
            return;
        }

        String authorId = user.getUid();

        String authorName;
        if (currentNick != null && !currentNick.isEmpty()) {
            authorName = currentNick;
        } else if (user.getEmail() != null) {
            authorName = user.getEmail();
        } else {
            authorName = "Anonimas";
        }

        Map<String, Object> post = new HashMap<>();
        post.put("pavadinimas", title);
        post.put("kategorija", category);
        post.put("turinys", content);
        post.put("sukurta", FieldValue.serverTimestamp());
        post.put("autoriusId", authorId);
        post.put("autoriusVardas", authorName);

        db.collection("irasai")
                .add(post)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Įrašas išsaugotas", Toast.LENGTH_SHORT).show();
                    editTitle.setText("");
                    editContent.setText("");
                    spinnerCategory.setSelection(0);

                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Klaida saugant: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}