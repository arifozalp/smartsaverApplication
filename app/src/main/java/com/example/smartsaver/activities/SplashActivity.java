package com.example.smartsaver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartsaver.R;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DISPLAY_LENGTH = 2000; // 2 saniye
    private ImageView splashLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        splashLogo = findViewById(R.id.splashLogo);

        // Fade-in animasyonu (isteğe bağlı)
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        splashLogo.startAnimation(fadeIn);

        // Gecikmeli olarak LoginActivity'ye geç
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DISPLAY_LENGTH);
    }
}
