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

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, goRegisterButton;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        goRegisterButton = findViewById(R.id.registerButton);

        loginButton.setOnClickListener(v -> loginUser());
        goRegisterButton.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_LONG).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(this,
                                    "Login successful but user is null",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Load role & nickname from Firestore
                        db.collection("users")
                                .document(user.getUid())
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    String role = "user";
                                    String nickname = "";

                                    if (documentSnapshot.exists()) {
                                        if (documentSnapshot.getString("role") != null) {
                                            role = documentSnapshot.getString("role");
                                        }
                                        if (documentSnapshot.getString("nickname") != null) {
                                            nickname = documentSnapshot.getString("nickname");
                                        }
                                    }

                                    Toast.makeText(this,
                                            "Login successful (" + role + ")",
                                            Toast.LENGTH_LONG).show();

                                    Intent intent =
                                            new Intent(this, MapActivity.class);
                                    intent.putExtra("role", role);
                                    intent.putExtra("nickname", nickname);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this,
                                            "Login ok, but failed to load profile: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();

                                    // Fallback: still go to map as normal user
                                    Intent intent =
                                            new Intent(this, MapActivity.class);
                                    intent.putExtra("role", "user");
                                    startActivity(intent);
                                    finish();
                                });

                    } else {
                        Toast.makeText(this, "Login failed", Toast.LENGTH_LONG).show();
                    }
                });
    }
}
