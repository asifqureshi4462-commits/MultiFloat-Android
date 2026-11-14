package com.example.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import androidx.appcompat.app.AppCompatActivity;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.util.EdgeToEdgeHelper;

public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable launchNext = () -> {
        boolean onboarded = MultiFloatApp.get().getPreferences().isOnboardingCompleted();
        Intent next = new Intent(this, onboarded ? MainActivity.class : OnboardingActivity.class);
        startActivity(next);
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        EdgeToEdgeHelper.setup(this, findViewById(R.id.splash_root));

        View logo = findViewById(R.id.iv_splash_logo);
        View title = findViewById(R.id.tv_splash_name);
        View tagline = findViewById(R.id.tv_splash_tagline);

        logo.setScaleX(0.6f);
        logo.setScaleY(0.6f);
        logo.setAlpha(0f);
        title.setAlpha(0f);
        tagline.setAlpha(0f);

        logo.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();

        title.animate().alpha(1f).setStartDelay(220).setDuration(450).start();
        tagline.animate().alpha(1f).setStartDelay(380).setDuration(450).start();

        handler.postDelayed(launchNext, 1250);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(launchNext);
        super.onDestroy();
    }
}
