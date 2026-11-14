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
import com.example.model.ToolType;
import com.example.service.FloatingEngineBus;
import com.example.util.EdgeToEdgeHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SidebarSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sidebar_settings);
        EdgeToEdgeHelper.setup(this, findViewById(R.id.sidebar_settings_root));

        AppPreferences prefs = MultiFloatApp.get().getPreferences();

        findViewById(R.id.btn_sb_set_back).setOnClickListener(v -> finish());

        MaterialSwitch switchEnabled = findViewById(R.id.switch_sb_enabled);
        switchEnabled.setChecked(prefs.isSidebarEnabled());
        switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setSidebarEnabled(isChecked);
            FloatingEngineBus.refreshConfig(this);
        });

        RadioGroup rgSide = findViewById(R.id.rg_sb_side);
        if (AppPreferences.SIDEBAR_LEFT.equals(prefs.getSidebarPosition())) {
            ((RadioButton) findViewById(R.id.rb_sb_left)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.rb_sb_right)).setChecked(true);
        }
        rgSide.setOnCheckedChangeListener((group, checkedId) -> {
            prefs.setSidebarPosition(checkedId == R.id.rb_sb_left
                    ? AppPreferences.SIDEBAR_LEFT : AppPreferences.SIDEBAR_RIGHT);
            FloatingEngineBus.refreshConfig(this);
        });

        RadioGroup rgSize = findViewById(R.id.rg_sb_size);
        int level = prefs.getSidebarSizeLevel();
        if (level == 0) ((RadioButton) findViewById(R.id.rb_sb_compact)).setChecked(true);
        else if (level == 2) ((RadioButton) findViewById(R.id.rb_sb_large)).setChecked(true);
        else ((RadioButton) findViewById(R.id.rb_sb_standard)).setChecked(true);

        rgSize.setOnCheckedChangeListener((group, checkedId) -> {
            int lvl = checkedId == R.id.rb_sb_compact ? 0 : (checkedId == R.id.rb_sb_large ? 2 : 1);
            prefs.setSidebarSizeLevel(lvl);
            FloatingEngineBus.refreshConfig(this);
        });

        TextView tvOpacity = findViewById(R.id.tv_sb_opacity_label);
        SeekBar seekOpacity = findViewById(R.id.seek_sb_opacity);
        int currentAlpha = prefs.getSidebarOpacityPercent();
        tvOpacity.setText("Sidebar Opacity: " + currentAlpha + "%");
        seekOpacity.setProgress(currentAlpha - 40);
        seekOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int pct = 40 + progress;
                tvOpacity.setText("Sidebar Opacity: " + pct + "%");
                if (fromUser) prefs.setSidebarOpacityPercent(pct);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                FloatingEngineBus.refreshConfig(SidebarSettingsActivity.this);
            }
        });

        ChipGroup chipGroup = findViewById(R.id.cg_sb_favorites);
        for (ToolType type : ToolType.values()) {
            Chip chip = new Chip(this);
            chip.setText(type.titleRes);
            chip.setCheckable(true);
            chip.setChecked(prefs.isFavorite(type.id));
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (prefs.isFavorite(type.id) != isChecked) {
                    prefs.toggleFavorite(type.id);
                    FloatingEngineBus.refreshConfig(this);
                }
            });
            chipGroup.addView(chip);
        }
    }
}
