package com.ajinkya.cleantogether.Fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.common.api.ApiException;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.ajinkya.cleantogether.AuthActivity;
import com.ajinkya.cleantogether.MainActivity;
import com.ajinkya.cleantogether.Models.userModel;
import com.ajinkya.cleantogether.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

public class SignUpFragment extends Fragment {

    private EditText UserName, Email, Phone, Password;
    private AppCompatButton SignUp, GoogleSignUp;
    private TextView SignInTab;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private ProgressDialog progressDialog;
    private GoogleSignInClient signInClient;
    private static final int RC_SIGN_IN = 100;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sign_up, container, false);

        initializeViews(view);
        setupFirebase();
        setupClickListeners();
        configureGoogleSignIn();

        return view;
    }

    private void initializeViews(View view) {
        UserName = view.findViewById(R.id.etUsername);
        Email = view.findViewById(R.id.etEmail);
        Phone = view.findViewById(R.id.etPhone);
        Password = view.findViewById(R.id.etPassword);
        SignUp = view.findViewById(R.id.btnSignUp);
        GoogleSignUp = view.findViewById(R.id.btnGoogleSignUp);
        SignInTab = view.findViewById(R.id.tvSignInTab);
    }

    private void setupFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Creating your account...");
        progressDialog.setCancelable(false);
    }

    private void setupClickListeners() {
        SignUp.setOnClickListener(v -> validateAndSignUp());
        GoogleSignUp.setOnClickListener(v -> startGoogleSignIn());
        SignInTab.setOnClickListener(v -> switchToSignIn());
    }

    private void validateAndSignUp() {
        try {
            // Get input values
            String username = UserName.getText().toString().trim();
            String email = Email.getText().toString().trim();
            String phone = Phone.getText().toString().trim();
            String password = Password.getText().toString().trim();

            // Clear any previous errors
            UserName.setError(null);
            Email.setError(null);
            Phone.setError(null);
            Password.setError(null);

            // Validate username
            if (username.isEmpty()) {
                UserName.setError("Username is required");
                UserName.requestFocus();
                return;
            }

            // Validate email
            if (email.isEmpty()) {
                Email.setError("Email is required");
                Email.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Email.setError("Please enter a valid email");
                Email.requestFocus();
                return;
            }

            // Validate phone
            if (phone.isEmpty()) {
                Phone.setError("Phone number is required");
                Phone.requestFocus();
                return;
            }
            if (phone.length() < 10) {
                Phone.setError("Please enter a valid phone number");
                Phone.requestFocus();
                return;
            }

            // Validate password
            if (password.isEmpty()) {
                Password.setError("Password is required");
                Password.requestFocus();
                return;
            }
            if (password.length() < 6) {
                Password.setError("Password must be at least 6 characters");
                Password.requestFocus();
                return;
            }

            // Show progress dialog
            if (isAdded()) {
                progressDialog.show();
            }

            // Create user with email and password
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (!isAdded()) {
                            return; // Fragment is no longer attached
                        }

                        if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                            String userId = firebaseAuth.getCurrentUser().getUid();
                            userModel user = new userModel(username, email, phone, userId);

                            // Save user data to Firestore
                            firestore.collection("users")
                                    .document(userId)
                                    .set(user)
                                    .addOnSuccessListener(aVoid -> {
                                        if (isAdded()) {
                                            progressDialog.dismiss();
                                            Toast.makeText(requireContext(),
                                                    "Account created successfully",
                                                    Toast.LENGTH_SHORT).show();
                                            navigateToMain();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        if (isAdded()) {
                                            progressDialog.dismiss();
                                            Log.e("SignUpFragment", "Firestore error: " + e.getMessage());
                                            Toast.makeText(requireContext(),
                                                    "Error saving user data: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            progressDialog.dismiss();
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() :
                                    "Failed to create account";
                            Log.e("SignUpFragment", "Auth error: " + errorMessage);
                            Toast.makeText(requireContext(),
                                    errorMessage,
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            progressDialog.dismiss();
                            Log.e("SignUpFragment", "Auth failure: " + e.getMessage());
                            Toast.makeText(requireContext(),
                                    "Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (Exception e) {
            if (isAdded()) {
                progressDialog.dismiss();
                Log.e("SignUpFragment", "Signup error: " + e.getMessage());
                Toast.makeText(requireContext(),
                        "Error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void navigateToMain() {
        if (isAdded()) {
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        }
    }
    private void configureGoogleSignIn() {
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            if (getActivity() != null) {
                signInClient = GoogleSignIn.getClient(getActivity(), gso);
            }
        } catch (Exception e) {
            Log.e("SignUpFragment", "Error configuring Google Sign In: " + e.getMessage());
            if (isAdded()) {
                Toast.makeText(requireContext(), "Error setting up Google Sign In", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startGoogleSignIn() {
        if (signInClient != null && getActivity() != null) {
            try {
                progressDialog.show();
                Intent signInIntent = signInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            } catch (Exception e) {
                progressDialog.dismiss();
                Log.e("SignUpFragment", "Error starting Google Sign In: " + e.getMessage());
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Google Sign In not configured properly", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN && data != null) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                } else {
                    progressDialog.dismiss();
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to get Google account", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (ApiException e) {
                progressDialog.dismiss();
                Log.e("SignUpFragment", "Google sign in failed: " + e.getStatusCode());
                if (isAdded()) {
                    String errorMessage = "Google Sign In Failed: ";
                    switch (e.getStatusCode()) {
                        case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                            errorMessage += "Cancelled";
                            break;
                        case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                            errorMessage += "Failed";
                            break;
                        case GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS:
                            errorMessage += "Already in progress";
                            break;
                        default:
                            errorMessage += "Error " + e.getStatusCode();
                    }
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        progressDialog.show();
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (isAdded()) {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                            String userId = task.getResult().getUser().getUid();
                            String username = account.getDisplayName();
                            String email = account.getEmail();

                            userModel user = new userModel(username, email, "", userId);

                            firestore.collection("users")
                                    .document(userId)
                                    .set(user, SetOptions.merge())
                                    .addOnSuccessListener(unused -> {
                                        if (isAdded()) {
                                            Toast.makeText(requireContext(), "Sign up successful", Toast.LENGTH_SHORT).show();
                                            navigateToMain();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        if (isAdded()) {
                                            Toast.makeText(requireContext(), "Error saving user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void switchToSignIn() {
        if (getActivity() instanceof AuthActivity) {
            ((AuthActivity) getActivity()).switchToSignIn();
        }
    }


}
