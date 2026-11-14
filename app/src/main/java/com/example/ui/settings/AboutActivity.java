package com.example.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.BuildConfig;
import com.example.R;
import com.example.util.EdgeToEdgeHelper;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        EdgeToEdgeHelper.setup(this, findViewById(R.id.about_root));

        TextView tvVersion = findViewById(R.id.tv_about_version);
        tvVersion.setText("Version " + BuildConfig.VERSION_NAME);

        findViewById(R.id.btn_about_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_about_privacy).setOnClickListener(v ->
                startActivity(new Intent(this, PrivacyInfoActivity.class)));
    }
}
