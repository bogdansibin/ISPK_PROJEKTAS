package com.example.ispk_projektas;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText nicknameEditText, emailEditText, passwordEditText;
    private Spinner roleSpinner;
    private Button registerButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nicknameEditText = findViewById(R.id.nickname_edittext);
        emailEditText = findViewById(R.id.email_edittext);
        passwordEditText = findViewById(R.id.password_edittext);
        roleSpinner = findViewById(R.id.role_spinner);
        registerButton = findViewById(R.id.register_button);

        // Simple spinner: "User" / "Admin" (for uni project; not secure in real life)
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.roles_array,              // will define in strings.xml
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        registerButton.setOnClickListener(v -> registerNewUser());
    }

    private void registerNewUser() {
        String nickname = nicknameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String selectedRoleText = roleSpinner.getSelectedItem().toString();

        // Map UI text to role value
        String role;
        if (selectedRoleText.equalsIgnoreCase("Admin")) {
            role = "admin";
        } else {
            role = "user";
        }

        // Basic validation
        if (TextUtils.isEmpty(nickname)) {
            nicknameEditText.setError("Enter nickname");
            nicknameEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Enter email");
            emailEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Enter password");
            passwordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return;
        }

        // Create user in Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user == null) {
                                Toast.makeText(RegisterActivity.this,
                                        "Unexpected error: user is null",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            // Set displayName to nickname
                            UserProfileChangeRequest profileUpdates =
                                    new UserProfileChangeRequest.Builder()
                                            .setDisplayName(nickname)
                                            .build();
                            user.updateProfile(profileUpdates);

                            // Save profile to Firestore
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("nickname", nickname);
                            userData.put("email", email);
                            userData.put("role", role);
                            userData.put("createdAt", FieldValue.serverTimestamp());

                            db.collection("users")
                                    .document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(RegisterActivity.this,
                                                "Registration successful!",
                                                Toast.LENGTH_LONG).show();

                                        // Go to map, passing role & nickname
                                        Intent intent =
                                                new Intent(RegisterActivity.this, MapActivity.class);
                                        intent.putExtra("role", role);
                                        intent.putExtra("nickname", nickname);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(RegisterActivity.this,
                                                "User created but saving profile failed: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    });

                        } else {
                            Toast.makeText(RegisterActivity.this,
                                    "Registration failed: " +
                                            (task.getException() != null
                                                    ? task.getException().getMessage()
                                                    : "Unknown error"),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
