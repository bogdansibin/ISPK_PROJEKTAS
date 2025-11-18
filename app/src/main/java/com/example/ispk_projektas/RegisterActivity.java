package com.example.ispk_projektas;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_REGISTER = 9002;

    private EditText nicknameEditText, emailEditText, passwordEditText;
    private Button registerButton;
    private Button googleRegisterButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nicknameEditText = findViewById(R.id.nickname_edittext);
        emailEditText = findViewById(R.id.email_edittext);
        passwordEditText = findViewById(R.id.password_edittext);
        registerButton = findViewById(R.id.register_button);
        googleRegisterButton = findViewById(R.id.googleRegisterButton);

        registerButton.setOnClickListener(v -> registerNewUser());

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleRegisterButton.setOnClickListener(v -> googleRegister());
    }

    private void registerNewUser() {
        String nickname = nicknameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

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

        // Create user in Firebase Auth (email/password)
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser == null) return;

                        String uid = firebaseUser.getUid();

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("nickname", nickname);
                        userData.put("email", email);
                        userData.put("role", "user");
                        userData.put("createdAt", FieldValue.serverTimestamp());

                        db.collection("users").document(uid)
                                .set(userData)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Registered!", Toast.LENGTH_SHORT).show();
                                    // No need for CLEAR_TASK here, it's a natural transition
                                    startActivity(new Intent(this, LoginActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                );
                    } else {
                        Toast.makeText(this, "Auth failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // --- GOOGLE REGISTER FLOW ---

    private void googleRegister() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_GOOGLE_REGISTER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_REGISTER) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseRegisterWithGoogle(account.getIdToken());
                } else {
                    Toast.makeText(this, "Google sign-in failed: null account",
                            Toast.LENGTH_LONG).show();
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseRegisterWithGoogle(String idToken) {

        final String role = "user";

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user = auth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(this, "Registration successful but user is null",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Check if the user already exists in Firestore (optional, but robust)
                        db.collection("users").document(user.getUid()).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        // User already exists (e.g., they logged in once before), redirect directly
                                        Toast.makeText(RegisterActivity.this,
                                                "Account already exists. Logging in.",
                                                Toast.LENGTH_LONG).show();

                                        // Get existing data
                                        String existingRole = documentSnapshot.getString("role");
                                        String existingNickname = documentSnapshot.getString("nickname");

                                        // Redirect based on nickname availability
                                        if (existingNickname == null || existingNickname.isEmpty()) {
                                            Intent intent = new Intent(RegisterActivity.this, NicknameActivity.class);
                                            intent.putExtra("role", existingRole);
                                            startActivity(intent);
                                        } else {
                                            Intent intent = new Intent(RegisterActivity.this, MapActivity.class);
                                            intent.putExtra("role", existingRole);
                                            intent.putExtra("nickname", existingNickname);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                        }
                                        finish();
                                    } else {
                                        // First time Google registration -> Create profile
                                        String email = user.getEmail() != null ? user.getEmail() : "";

                                        Map<String, Object> userData = new HashMap<>();
                                        userData.put("nickname", "");  // force user to choose nickname later
                                        userData.put("email", email);
                                        userData.put("role", role);
                                        userData.put("createdAt", FieldValue.serverTimestamp());

                                        db.collection("users")
                                                .document(user.getUid())
                                                .set(userData)
                                                .addOnSuccessListener(aVoid -> {

                                                    Toast.makeText(RegisterActivity.this,
                                                            "Registration with Google successful! Please set a nickname.",
                                                            Toast.LENGTH_LONG).show();

                                                    // Redirect to Nickname setup
                                                    Intent intent = new Intent(RegisterActivity.this, NicknameActivity.class);
                                                    intent.putExtra("role", role);
                                                    startActivity(intent);
                                                    finish();
                                                })
                                                .addOnFailureListener(e ->
                                                        Toast.makeText(RegisterActivity.this,
                                                                "User registered but saving profile failed: " + e.getMessage(),
                                                                Toast.LENGTH_LONG).show()
                                                );
                                    }
                                });

                    } else {
                        Toast.makeText(this,
                                "Firebase auth with Google failed",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}