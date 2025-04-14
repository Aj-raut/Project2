package com.ajinkya.cleantogether;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.ajinkya.cleantogether.Fragments.AccountFragment;
import com.ajinkya.cleantogether.Fragments.CameraFragment;
import com.ajinkya.cleantogether.Fragments.HomeFragment;

import me.ibrahimsn.lib.OnItemSelectedListener;
import me.ibrahimsn.lib.SmoothBottomBar;

public class MainActivity extends AppCompatActivity {
    private SmoothBottomBar bottomBar;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize bottom bar
        bottomBar = findViewById(R.id.bottomBar);

        // Set default fragment if needed
        if (savedInstanceState == null) {
            currentFragment = new HomeFragment();
            loadFragment(currentFragment);
        }

        // Setup bottom bar item selection listener
        bottomBar.setOnItemSelectedListener((OnItemSelectedListener) i -> {
            Fragment fragment;
            switch (i) {
                case 0:
                    if (!(currentFragment instanceof HomeFragment)) {
                        fragment = new HomeFragment();
                        loadFragment(fragment);
                    }
                    break;
                case 1:
                    if (!(currentFragment instanceof CameraFragment)) {
                        fragment = new CameraFragment();
                        loadFragment(fragment);
                    }
                    break;
                case 2:
                    if (!(currentFragment instanceof AccountFragment)) {
                        fragment = new AccountFragment();
                        loadFragment(fragment);
                    }
                    break;
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        if (fragment != null) {
            currentFragment = fragment;
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
            );
            transaction.replace(R.id.fragmentContainer, fragment);
            // Don't add to back stack for bottom navigation
            transaction.commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (!(currentFragment instanceof HomeFragment)) {
            // If not on home fragment, switch to it
            bottomBar.setItemActiveIndex(0);
            loadFragment(new HomeFragment());
        } else {
            super.onBackPressed();
        }
    }
}