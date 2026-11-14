package com.example.ui.settings;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.data.preferences.AppPreferences;
import com.example.service.FloatingEngineBus;
import com.example.util.EdgeToEdgeHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;

public class AppearanceSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appearance_settings);
        EdgeToEdgeHelper.setup(this, findViewById(R.id.appearance_root));

        AppPreferences prefs = MultiFloatApp.get().getPreferences();

        findViewById(R.id.btn_app_set_back).setOnClickListener(v -> finish());

        RadioGroup rgTheme = findViewById(R.id.rg_theme_mode);
        int mode = prefs.getThemeMode();
        if (mode == AppPreferences.THEME_LIGHT) ((RadioButton) findViewById(R.id.rb_theme_light)).setChecked(true);
        else if (mode == AppPreferences.THEME_SYSTEM) ((RadioButton) findViewById(R.id.rb_theme_system)).setChecked(true);
        else ((RadioButton) findViewById(R.id.rb_theme_dark)).setChecked(true);

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int newMode = AppPreferences.THEME_DARK;
            if (checkedId == R.id.rb_theme_light) newMode = AppPreferences.THEME_LIGHT;
            else if (checkedId == R.id.rb_theme_system) newMode = AppPreferences.THEME_SYSTEM;
            prefs.setThemeMode(newMode);
            FloatingEngineBus.refreshConfig(this);
        });

        ChipGroup cgAccent = findViewById(R.id.cg_accent_colors);
        String accent = prefs.getAccentColorKey();
        if (AppPreferences.ACCENT_CYAN.equals(accent)) ((Chip) findViewById(R.id.chip_accent_cyan)).setChecked(true);
        else if (AppPreferences.ACCENT_EMERALD.equals(accent)) ((Chip) findViewById(R.id.chip_accent_emerald)).setChecked(true);
        else if (AppPreferences.ACCENT_VIOLET.equals(accent)) ((Chip) findViewById(R.id.chip_accent_violet)).setChecked(true);
        else if (AppPreferences.ACCENT_AMBER.equals(accent)) ((Chip) findViewById(R.id.chip_accent_amber)).setChecked(true);
        else ((Chip) findViewById(R.id.chip_accent_blue)).setChecked(true);

        cgAccent.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            String key = AppPreferences.ACCENT_ELECTRIC;
            if (id == R.id.chip_accent_cyan) key = AppPreferences.ACCENT_CYAN;
            else if (id == R.id.chip_accent_emerald) key = AppPreferences.ACCENT_EMERALD;
            else if (id == R.id.chip_accent_violet) key = AppPreferences.ACCENT_VIOLET;
            else if (id == R.id.chip_accent_amber) key = AppPreferences.ACCENT_AMBER;
            prefs.setAccentColorKey(key);
            FloatingEngineBus.refreshConfig(this);
        });

        setupSeek(R.id.seek_bubble_size, R.id.tv_bubble_size_label,
                prefs.getBubbleSizeDp(), 40, "Floating Bubble Size: ", "dp",
                val -> prefs.setBubbleSizeDp(val));

        setupSeek(R.id.seek_bubble_alpha, R.id.tv_bubble_alpha_label,
                prefs.getBubbleOpacityPercent(), 30, "Floating Bubble Opacity: ", "%",
                val -> prefs.setBubbleOpacityPercent(val));

        setupSeek(R.id.seek_window_alpha, R.id.tv_window_alpha_label,
                prefs.getWindowOpacityPercent(), 40, "Default Window Opacity: ", "%",
                val -> prefs.setWindowOpacityPercent(val));

        setupSeek(R.id.seek_window_radius, R.id.tv_window_radius_label,
                prefs.getWindowCornerRadiusDp(), 0, "Window Corner Radius: ", "dp",
                val -> prefs.setWindowCornerRadiusDp(val));

        setupSeek(R.id.seek_window_scale, R.id.tv_window_scale_label,
                prefs.getDefaultWindowScalePercent(), 75, "Default Window Size: ", "%",
                val -> prefs.setDefaultWindowScalePercent(val));

        MaterialSwitch switchAnim = findViewById(R.id.switch_window_anim);
        switchAnim.setChecked(prefs.isWindowAnimationEnabled());
        switchAnim.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.setWindowAnimationEnabled(isChecked));
    }

    private interface IntConsumer { void accept(int value); }

    private void setupSeek(int seekId, int labelId, int currentValue, int minOffset,
                           String prefix, String suffix, IntConsumer onSave) {
        TextView tv = findViewById(labelId);
        SeekBar sb = findViewById(seekId);
        tv.setText(prefix + currentValue + suffix);
        sb.setProgress(Math.max(0, currentValue - minOffset));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int v = minOffset + progress;
                tv.setText(prefix + v + suffix);
                if (fromUser) onSave.accept(v);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                FloatingEngineBus.refreshConfig(AppearanceSettingsActivity.this);
            }
        });
    }
}
