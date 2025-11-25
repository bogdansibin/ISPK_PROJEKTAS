package com.example.ispk_projektas;

import android.app.Activity;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;

public class AuthManager {
    public static void logout(Activity activity) {
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);

        activity.finish();
    }
}
