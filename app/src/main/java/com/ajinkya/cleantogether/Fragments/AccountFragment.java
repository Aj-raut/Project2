package com.ajinkya.cleantogether.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.ajinkya.cleantogether.AuthActivity;
import com.ajinkya.cleantogether.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class AccountFragment extends Fragment {

    private TextView tvUsername, tvEmail, tvPhone;
    private Button btnLogout;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private ListenerRegistration userListener;
    private boolean dataLoaded = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        tvUsername = view.findViewById(R.id.tvUsername);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Set logout click listener
        btnLogout.setOnClickListener(v -> logout());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!dataLoaded) {
            loadUserData();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (userListener != null) {
            userListener.remove();
        }
    }

    private void loadUserData() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // Set email from FirebaseUser
            tvEmail.setText(currentUser.getEmail());

            // Get additional user data from Firestore with real-time updates
            userListener = firestore.collection("users")
                    .document(currentUser.getUid())
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                            Toast.makeText(getContext(), "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("userName");
                            String phone = documentSnapshot.getString("userNumber");

                            tvUsername.setText(username);
                            tvPhone.setText(phone);
                            dataLoaded = true;
                        }
                    });
        }
    }

    private void logout() {
        if (userListener != null) {
            userListener.remove();
        }

        // Sign out from Firebase
        firebaseAuth.signOut();

        // Show success message
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate to AuthActivity
        Intent intent = new Intent(getActivity(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) {
            userListener.remove();
        }
    }
}