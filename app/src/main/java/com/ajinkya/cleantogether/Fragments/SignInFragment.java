package com.ajinkya.cleantogether.Fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.ajinkya.cleantogether.AdminActivity;
import com.ajinkya.cleantogether.AuthActivity;
import com.ajinkya.cleantogether.DriverActivity;
import com.ajinkya.cleantogether.MainActivity;
import com.ajinkya.cleantogether.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class SignInFragment extends Fragment {
    private TextInputEditText Email, Password;
    private AppCompatButton SignIn, btnGoogleSignIn;
    private TextView SignUpTab, ForgotPassword;
    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient signInClient;
    private static final int RC_SIGN_IN = 100;

    public SignInFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sign_in, container, false);

        // Initialize views with exact IDs from XML
        Email = view.findViewById(R.id.etEmail);
        Password = view.findViewById(R.id.etPassword);
        SignIn = view.findViewById(R.id.btnSignIn);
        SignUpTab = view.findViewById(R.id.tvSignUpTab);
        btnGoogleSignIn = view.findViewById(R.id.btnGoogleSignIn);
        ForgotPassword = view.findViewById(R.id.tvForgotPassword);

        // Initialize Firebase Auth and Progress Dialog
        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        // Configure Google Sign In
        configureGoogleSignIn();

        // Set click listeners
        btnGoogleSignIn.setOnClickListener(v -> startGoogleSignIn());
        SignUpTab.setOnClickListener(v -> navigateToSignUp());
        SignIn.setOnClickListener(v -> signInUser());
        ForgotPassword.setOnClickListener(v -> handleForgotPassword());

        return view;
    }

    private void configureGoogleSignIn() {
        try {
            // Configure Google Sign In
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            // Initialize Google Sign In Client
            if (getActivity() != null) {
                signInClient = GoogleSignIn.getClient(getActivity(), gso);
            }
        } catch (Exception e) {
            Log.e("SignInFragment", "Error configuring Google Sign In: " + e.getMessage());
            if (isAdded()) {
                Toast.makeText(requireContext(), "Error setting up Google Sign In", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startGoogleSignIn() {
        if (signInClient != null && getActivity() != null) {
            try {
                Intent signInIntent = signInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            } catch (Exception e) {
                Log.e("SignInFragment", "Error starting Google Sign In: " + e.getMessage());
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            try {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                hideProgress();
                Log.e("SignInFragment", "Google sign in failed: " + e.getStatusCode() + " " + e.getMessage());
                Toast.makeText(getContext(), "Google sign in failed: " + e.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        showProgress();
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    hideProgress();
                    if (getActivity() == null) return; // Check if fragment is attached

                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null && isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                if (isAdded()) {
                                    Toast.makeText(requireContext(),
                                            "Welcome " + user.getDisplayName(),
                                            Toast.LENGTH_SHORT).show();
                                    navigateToMain();
                                }
                            });
                        }
                    } else {
                        Log.e("SignInFragment", "signInWithCredential:failure", task.getException());
                        if (isAdded()) {
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() :
                                    "Authentication Failed";
                            requireActivity().runOnUiThread(() -> {
                                if (isAdded()) {
                                    Toast.makeText(requireContext(),
                                            "Authentication Failed: " + errorMessage,
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
    }

    private void navigateToSignUp() {
        if (getActivity() instanceof AuthActivity) {
            ((AuthActivity) getActivity()).switchToSignUp();
        }
    }

    private void handleForgotPassword() {
        String email = Email.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getContext(), "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress();
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    hideProgress();
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Password reset email sent", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    hideProgress();
                    Toast.makeText(getContext(), "Failed to send reset email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void signInUser() {
        String email = Email.getText().toString().trim();
        String password = Password.getText().toString().trim();

        // Clear any previous errors
        Email.setError(null);
        Password.setError(null);

        // Validate inputs
        if (email.isEmpty()) {
            Email.setError("Email is required");
            Email.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            Password.setError("Password is required");
            Password.requestFocus();
            return;
        }

        // Check for admin login
        if (email.equals("admin") || email.equals("Admin") || email.equals("@Admin")) {
            if (password.equals("12345678")) {
                // Admin login successful
                if (isAdded()) {
                    Intent intent = new Intent(requireContext(), AdminActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                }
                return;
            } else {
                Password.setError("Invalid admin password");
                Password.requestFocus();
                return;
            }
        }

        if (email.equals("driver") || email.equals("Driver") || email.equals("@Driver")) {
            if (password.equals("12345678")) {
                // Admin login successful
                if (isAdded()) {
                    Intent intent = new Intent(requireContext(), DriverActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                }
                return;
            } else {
                Password.setError("Invalid admin password");
                Password.requestFocus();
                return;
            }
        }

        // Regular user login - validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Email.setError("Please enter a valid email address");
            Email.requestFocus();
            return;
        }

        showProgress();

        // Regular user authentication with Firebase
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    hideProgress();
                    if (!isAdded()) return;

                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Sign in successful", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() :
                                "Authentication failed";
                        Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showProgress() {
        if (progressDialog != null && !progressDialog.isShowing() && isAdded()) {
            progressDialog.show();
        }
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
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

    private void navigateToDriverActivity() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), DriverActivity.class);
            intent.putExtra("DRIVER_ID", "DRIVER_001"); // Pass the default driver ID
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }

    

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}