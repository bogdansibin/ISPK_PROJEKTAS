package com.example.ispk_projektas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class NicknameActivity extends AppCompatActivity {

    private EditText nicknameEditText;
    private Button saveNicknameButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nicknames);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nicknameEditText = findViewById(R.id.nicknameEditText);
        saveNicknameButton = findViewById(R.id.saveNicknameButton);

        saveNicknameButton.setOnClickListener(v -> saveNickname());
    }

    private void saveNickname() {
        String nickname = nicknameEditText.getText().toString().trim();

        if (nickname.isEmpty()) {
            nicknameEditText.setError("Įveskite vartotojo vardą");
            nicknameEditText.requestFocus();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Naudotojas nerastas", Toast.LENGTH_LONG).show();
            return;
        }

        String uid = user.getUid();

        Map<String, Object> update = new HashMap<>();
        update.put("nickname", nickname);

        db.collection("users").document(uid)
                .set(update, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    // optionally get role (default user)
                    String role = getIntent().getStringExtra("role");
                    if (role == null) role = "user";

                    Intent intent = new Intent(NicknameActivity.this, MapActivity.class);
                    intent.putExtra("nickname", nickname);
                    intent.putExtra("role", role);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Nepavyko išsaugoti vardo: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}
