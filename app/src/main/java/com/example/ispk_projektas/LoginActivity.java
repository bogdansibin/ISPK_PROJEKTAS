package com.example.ispk_projektas;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;

    private EditText emailEditText, passwordEditText;
    private Button loginButton, goRegisterButton;
    private Button googleSignInButton;

    private TextView forgotPasswordText; // 👈 PRIDĖTA

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🌟 SESSION CHECK: If user is already logged in, skip login screen
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            checkProfileAndRedirect(currentUser.getUid());
            return;
        }

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        goRegisterButton = findViewById(R.id.registerButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);

        // Email/password login
        loginButton.setOnClickListener(v -> loginUser());
        goRegisterButton.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        // Google sign-in config
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        forgotPasswordText.setOnClickListener(v -> sendPasswordReset());

    }

    private void sendPasswordReset() {
        String email = emailEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError("Įveskite el. paštą");
            emailEditText.requestFocus();
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Slaptažodžio atstatymo nuoroda išsiųsta į " + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Nepavyko išsiųsti el. laiško";
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    //prisijungimu patirkinimas

    /**
     * Helper function to fetch user profile and redirect to MapActivity or NicknameActivity.
     */
    private void checkProfileAndRedirect(String uid) {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String nickname = doc.getString("nickname");
                    String role = doc.getString("role");

                    if (nickname == null) nickname = "";
                    if (role == null) role = "user";

                    if (nickname.isEmpty()) {
                        // Go to nickname screen if missing (e.g., first Google login)
                        Intent i = new Intent(LoginActivity.this, NicknameActivity.class);
                        i.putExtra("role", role);
                        startActivity(i);
                        finish();
                    } else {
                        // Go straight to MapActivity
                        Intent intent = new Intent(LoginActivity.this, MapActivity.class);
                        intent.putExtra("nickname", nickname);
                        intent.putExtra("role", role);
                        // 🌟 IMPORTANT: Clear the back stack
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user profile. Please log in again.", Toast.LENGTH_LONG).show();
                    auth.signOut(); // Force sign out if profile retrieval fails critically
                });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Įveskite el. paštą");
            emailEditText.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            emailEditText.setError("Įveskite slaptažodį");
            emailEditText.requestFocus();
            return;
        }


        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser == null) return;

                        // 🌟 Use the helper method after successful authentication
                        checkProfileAndRedirect(firebaseUser.getUid());

                    } else {
                        Toast.makeText(this,
                                "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }


    // --- GOOGLE LOGIN FLOW ---

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    Toast.makeText(this,
                            "Google sign-in failed: null account",
                            Toast.LENGTH_LONG).show();
                }
            } catch (ApiException e) {
                Toast.makeText(this,
                        "Google sign-in failed: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(this,
                                    "Login successful but user is null",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        db.collection("users")
                                .document(user.getUid())
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    String role = "user";
                                    String nickname = "";
                                    String email = user.getEmail() != null ? user.getEmail() : "";

                                    if (documentSnapshot.exists()) {
                                        // User exists in Firestore -> Load profile
                                        DocumentSnapshot doc = documentSnapshot;
                                        role = doc.getString("role") != null ? doc.getString("role") : "user";
                                        nickname = doc.getString("nickname") != null ? doc.getString("nickname") : "";
                                    } else {
                                        // First time Google login -> create profile with empty nickname
                                        Map<String, Object> userData = new HashMap<>();
                                        userData.put("nickname", "");
                                        userData.put("email", email);
                                        userData.put("role", role);
                                        userData.put("createdAt", FieldValue.serverTimestamp());

                                        db.collection("users")
                                                .document(user.getUid())
                                                .set(userData);
                                    }

                                    // 🌟 Use the helper method to handle redirection
                                    checkProfileAndRedirect(user.getUid());
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this,
                                            "Google login ok, but failed to load profile: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();

                                    // fallback: go to nickname screen, role=user
                                    Intent i = new Intent(this, NicknameActivity.class);
                                    i.putExtra("role", "user");
                                    startActivity(i);
                                    finish();
                                });

                    } else {
                        Toast.makeText(this,
                                "Firebase auth with Google failed",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}