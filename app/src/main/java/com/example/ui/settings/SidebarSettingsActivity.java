package com.example.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.data.preferences.AppPreferences;
import com.example.data.repository.InstalledAppsRepository;
import com.example.model.InstalledAppInfo;
import com.example.model.SidebarShortcut;
import com.example.model.ToolType;
import com.example.service.FloatingEngineBus;
import com.example.ui.InstalledAppsActivity;
import com.example.util.EdgeToEdgeHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SidebarSettingsActivity extends AppCompatActivity {

    private AppPreferences prefs;
    private InstalledAppsRepository appsRepo;
    private final List<SidebarShortcut> orderedShortcuts = new ArrayList<>();
    private final Map<String, InstalledAppInfo> appCache = new HashMap<>();
    private ShortcutReorderAdapter reorderAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sidebar_settings);
        EdgeToEdgeHelper.setup(this, findViewById(R.id.sidebar_settings_root));

        prefs = MultiFloatApp.get().getPreferences();
        appsRepo = MultiFloatApp.get().getInstalledAppsRepository();

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

        RadioGroup rgIconStyle = findViewById(R.id.rg_sb_icon_style);
        int style = prefs.getSidebarIconStyle();
        if (style == 1) ((RadioButton) findViewById(R.id.rb_sb_style_minimal)).setChecked(true);
        else if (style == 2) ((RadioButton) findViewById(R.id.rb_sb_style_ring)).setChecked(true);
        else ((RadioButton) findViewById(R.id.rb_sb_style_rounded)).setChecked(true);

        rgIconStyle.setOnCheckedChangeListener((group, checkedId) -> {
            int s = checkedId == R.id.rb_sb_style_minimal ? 1
                    : (checkedId == R.id.rb_sb_style_ring ? 2 : 0);
            prefs.setSidebarIconStyle(s);
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

        findViewById(R.id.btn_sb_manage_apps).setOnClickListener(v ->
                startActivity(new Intent(this, InstalledAppsActivity.class)));

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

        RecyclerView rvReorder = findViewById(R.id.rv_sb_reorder);
        rvReorder.setLayoutManager(new LinearLayoutManager(this));
        reorderAdapter = new ShortcutReorderAdapter();
        rvReorder.setAdapter(reorderAdapter);

        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                if (from < 0 || to < 0 || from >= orderedShortcuts.size() || to >= orderedShortcuts.size()) {
                    return false;
                }
                Collections.swap(orderedShortcuts, from, to);
                reorderAdapter.notifyItemMoved(from, to);
                persistCustomOrder();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
        });
        touchHelper.attachToRecyclerView(rvReorder);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadShortcutOrder();
    }

    private void reloadShortcutOrder() {
        appCache.clear();
        List<String> selectedApps = prefs.getSelectedSidebarApps();
        for (String pkg : selectedApps) {
            InstalledAppInfo info = appsRepo.loadAppByPackage(this, pkg);
            if (info != null) {
                appCache.put(pkg, info);
            }
        }

        orderedShortcuts.clear();
        for (String key : prefs.getSidebarCustomOrder()) {
            SidebarShortcut sc = SidebarShortcut.fromKey(key);
            if (sc == null) continue;
            if (sc.isExternalApp) {
                if (appCache.containsKey(sc.packageName)) {
                    orderedShortcuts.add(sc);
                }
            } else {
                orderedShortcuts.add(sc);
            }
        }
        reorderAdapter.notifyDataSetChanged();
    }

    private void persistCustomOrder() {
        List<String> keys = new ArrayList<>(orderedShortcuts.size());
        for (SidebarShortcut sc : orderedShortcuts) {
            keys.add(sc.key);
        }
        prefs.setSidebarCustomOrder(keys);
        FloatingEngineBus.refreshConfig(this);
    }

    private class ShortcutReorderAdapter extends RecyclerView.Adapter<ShortcutReorderAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sidebar_reorder, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            SidebarShortcut sc = orderedShortcuts.get(position);
            if (sc.isExternalApp) {
                InstalledAppInfo info = appCache.get(sc.packageName);
                holder.title.setText(info != null ? info.appName : sc.packageName);
                holder.type.setText("Installed App • " + sc.packageName);
                holder.icon.clearColorFilter();
                if (info != null && info.icon != null) {
                    holder.icon.setImageDrawable(info.icon);
                } else {
                    holder.icon.setImageResource(R.drawable.ic_apps);
                }
                holder.btnRemove.setVisibility(View.VISIBLE);
                holder.btnRemove.setOnClickListener(v -> {
                    int idx = holder.getBindingAdapterPosition();
                    if (idx < 0 || idx >= orderedShortcuts.size()) return;
                    SidebarShortcut target = orderedShortcuts.get(idx);
                    if (target.packageName != null) {
                        prefs.toggleSidebarApp(target.packageName);
                        orderedShortcuts.remove(idx);
                        notifyItemRemoved(idx);
                        FloatingEngineBus.refreshConfig(SidebarSettingsActivity.this);
                    }
                });
            } else if (sc.toolType != null) {
                holder.title.setText(sc.toolType.titleRes);
                holder.type.setText("Mini-App • " + sc.toolType.category);
                holder.icon.setImageResource(sc.toolType.iconRes);
                holder.icon.setColorFilter(getColor(R.color.mf_cyan_text));
                holder.btnRemove.setVisibility(View.GONE);
                holder.btnRemove.setOnClickListener(null);
            }

            holder.btnUp.setEnabled(position > 0);
            holder.btnUp.setAlpha(position > 0 ? 1f : 0.35f);
            holder.btnUp.setOnClickListener(v -> {
                int idx = holder.getBindingAdapterPosition();
                if (idx > 0 && idx < orderedShortcuts.size()) {
                    Collections.swap(orderedShortcuts, idx, idx - 1);
                    notifyItemMoved(idx, idx - 1);
                    notifyItemChanged(idx);
                    notifyItemChanged(idx - 1);
                    persistCustomOrder();
                }
            });

            holder.btnDown.setEnabled(position < orderedShortcuts.size() - 1);
            holder.btnDown.setAlpha(position < orderedShortcuts.size() - 1 ? 1f : 0.35f);
            holder.btnDown.setOnClickListener(v -> {
                int idx = holder.getBindingAdapterPosition();
                if (idx >= 0 && idx < orderedShortcuts.size() - 1) {
                    Collections.swap(orderedShortcuts, idx, idx + 1);
                    notifyItemMoved(idx, idx + 1);
                    notifyItemChanged(idx);
                    notifyItemChanged(idx + 1);
                    persistCustomOrder();
                }
            });
        }

        @Override
        public int getItemCount() {
            return orderedShortcuts.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView title, type;
            final ImageButton btnUp, btnDown, btnRemove;

            VH(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.iv_reorder_icon);
                title = itemView.findViewById(R.id.tv_reorder_title);
                type = itemView.findViewById(R.id.tv_reorder_type);
                btnUp = itemView.findViewById(R.id.btn_reorder_up);
                btnDown = itemView.findViewById(R.id.btn_reorder_down);
                btnRemove = itemView.findViewById(R.id.btn_reorder_remove);
            }
        }
    }
}
