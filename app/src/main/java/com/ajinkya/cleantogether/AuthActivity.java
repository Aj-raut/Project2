package com.ajinkya.cleantogether;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.ajinkya.cleantogether.Fragments.SignInFragment;
import com.ajinkya.cleantogether.Fragments.SignUpFragment;
import com.google.firebase.auth.FirebaseAuth;

public class AuthActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        firebaseAuth = FirebaseAuth.getInstance();

        // Check if user is already signed in
        if (firebaseAuth.getCurrentUser() != null) {
            // User is signed in, go to MainActivity
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Show SignInFragment by default
        if (savedInstanceState == null) {
            loadFragment(new SignInFragment());
        }
    }

    public void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.auth_fragment_container, fragment);
        transaction.commit();
    }

    public void switchToSignUp() {
        loadFragment(new SignUpFragment());
    }

    public void switchToSignIn() {
        loadFragment(new SignInFragment());
    }
}