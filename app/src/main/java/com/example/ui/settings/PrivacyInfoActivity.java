package com.example.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.R;
import com.example.ui.PermissionSetupActivity;
import com.example.util.EdgeToEdgeHelper;

public class PrivacyInfoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_info);
        EdgeToEdgeHelper.setup(this, findViewById(R.id.privacy_root));

        findViewById(R.id.btn_priv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_priv_manage_perms).setOnClickListener(v ->
                startActivity(new Intent(this, PermissionSetupActivity.class)));
    }
}
