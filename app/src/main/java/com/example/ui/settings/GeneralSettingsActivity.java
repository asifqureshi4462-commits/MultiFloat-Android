package com.example.ui.settings;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.data.preferences.AppPreferences;
import com.example.data.preferences.SecureKeyStore;
import com.example.util.EdgeToEdgeHelper;
import com.google.android.material.materialswitch.MaterialSwitch;

public class GeneralSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_settings);
        EdgeToEdgeHelper.setup(this, findViewById(R.id.general_root));

        AppPreferences prefs = MultiFloatApp.get().getPreferences();
        SecureKeyStore keyStore = MultiFloatApp.get().getSecureKeyStore();

        findViewById(R.id.btn_gen_set_back).setOnClickListener(v -> finish());

        MaterialSwitch swEdgeSnap = findViewById(R.id.switch_edge_snap);
        MaterialSwitch swRememberPos = findViewById(R.id.switch_remember_pos);
        MaterialSwitch swRestoreSession = findViewById(R.id.switch_restore_session);
        EditText etBrowserHome = findViewById(R.id.et_browser_home);

        RadioGroup rgAiProvider = findViewById(R.id.rg_ai_provider);
        EditText etAiModel = findViewById(R.id.et_ai_model);
        EditText etAiEndpoint = findViewById(R.id.et_ai_endpoint);
        EditText etAiKey = findViewById(R.id.et_ai_api_key);

        EditText etTransEndpoint = findViewById(R.id.et_translate_endpoint);
        EditText etTransKey = findViewById(R.id.et_translate_api_key);

        swEdgeSnap.setChecked(prefs.isEdgeSnapEnabled());
        swRememberPos.setChecked(prefs.isRememberWindowPositions());
        swRestoreSession.setChecked(prefs.isRestoreSessionEnabled());
        etBrowserHome.setText(prefs.getBrowserHomeUrl());

        swEdgeSnap.setOnCheckedChangeListener((b, c) -> prefs.setEdgeSnapEnabled(c));
        swRememberPos.setOnCheckedChangeListener((b, c) -> prefs.setRememberWindowPositions(c));
        swRestoreSession.setOnCheckedChangeListener((b, c) -> prefs.setRestoreSessionEnabled(c));

        if (AppPreferences.AI_PROVIDER_OPENAI.equals(prefs.getAiProvider())) {
            ((RadioButton) findViewById(R.id.rb_ai_openai)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.rb_ai_gemini)).setChecked(true);
        }

        etAiModel.setText(prefs.getAiModel());
        etAiEndpoint.setText(prefs.getAiEndpoint());
        etAiKey.setText(keyStore.getSecret(AppPreferences.KEY_AI_API));

        etTransEndpoint.setText(prefs.getTranslateEndpoint());
        etTransKey.setText(keyStore.getSecret(AppPreferences.KEY_TRANSLATE_API));

        findViewById(R.id.btn_save_general_settings).setOnClickListener(v -> {
            String home = etBrowserHome.getText().toString().trim();
            if (!home.isEmpty()) prefs.setBrowserHomeUrl(home);

            int checkedProvider = rgAiProvider.getCheckedRadioButtonId();
            prefs.setAiProvider(checkedProvider == R.id.rb_ai_openai
                    ? AppPreferences.AI_PROVIDER_OPENAI : AppPreferences.AI_PROVIDER_GEMINI);
            prefs.setAiModel(etAiModel.getText().toString().trim());
            prefs.setAiEndpoint(etAiEndpoint.getText().toString().trim());
            keyStore.saveSecret(AppPreferences.KEY_AI_API, etAiKey.getText().toString().trim());

            String transEp = etTransEndpoint.getText().toString().trim();
            if (!transEp.isEmpty()) prefs.setTranslateEndpoint(transEp);
            keyStore.saveSecret(AppPreferences.KEY_TRANSLATE_API, etTransKey.getText().toString().trim());

            Toast.makeText(this, "Settings & encrypted API keys saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
