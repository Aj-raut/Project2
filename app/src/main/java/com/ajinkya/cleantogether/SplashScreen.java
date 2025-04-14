package com.ajinkya.cleantogether;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashScreen extends AppCompatActivity {

    private ImageView Location, Image;
    private TextView Text1, Text2;
    private Animation Top_anim, Bottom_anim, Left_anim;
    private static final long SPLASH_DURATION = 2000; // Reduced from 3000 to 2000

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);


        initializeViews();


        startAnimations();


        checkAuthAndNavigate();
    }

    private void initializeViews() {
        Location = findViewById(R.id.location);
        Image = findViewById(R.id.image);
        Text1 = findViewById(R.id.text1);
        Text2 = findViewById(R.id.text2);
    }

    private void startAnimations() {
        // Load animations
        Top_anim = AnimationUtils.loadAnimation(this, R.anim.topanim);
        Bottom_anim = AnimationUtils.loadAnimation(this, R.anim.bottomanim);
        Left_anim = AnimationUtils.loadAnimation(this, R.anim.leftanim);

        // Start animations
        Location.startAnimation(Top_anim);
        Image.startAnimation(Left_anim);
        Text1.startAnimation(Bottom_anim);
        Text2.startAnimation(Bottom_anim);
    }

    private void checkAuthAndNavigate() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        new Thread(() -> {
            try {
                Thread.sleep(SPLASH_DURATION);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                Intent intent;
                if (currentUser != null) {
                    intent = new Intent(SplashScreen.this, MainActivity.class);
                } else {
                    intent = new Intent(SplashScreen.this, AuthActivity.class);
                }

                // Create animation for smooth transition
                ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                        this, android.R.anim.fade_in, android.R.anim.fade_out);

                startActivity(intent, options.toBundle());
                finish();
            });
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Clear animations to prevent memory leaks
        if (Location != null) Location.clearAnimation();
        if (Image != null) Image.clearAnimation();
        if (Text1 != null) Text1.clearAnimation();
        if (Text2 != null) Text2.clearAnimation();
    }
}