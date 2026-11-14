package com.example.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.data.local.entity.ToolUsageEntity;
import com.example.data.preferences.AppPreferences;
import com.example.model.ToolType;
import com.example.service.FloatingEngineBus;
import com.example.ui.settings.AboutActivity;
import com.example.ui.settings.AppearanceSettingsActivity;
import com.example.ui.settings.GeneralSettingsActivity;
import com.example.ui.settings.PrivacyInfoActivity;
import com.example.ui.settings.SidebarSettingsActivity;
import com.example.ui.tools.ToolNavigationHelper;
import com.example.util.EdgeToEdgeHelper;
import com.example.util.PermissionHelper;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private DashboardViewModel viewModel;
    private TextView tvEngineStatus, tvWindowCount, chipPermStatus, tvRecentHeader;
    private Button btnLaunchEngine;
    private LinearLayout containerFavorites, containerRecent;
    private MaterialSwitch switchBubble, switchSidebar;
    private boolean updatingSwitches = false;

    private final ToolType[] quickTools = new ToolType[]{
            ToolType.CALCULATOR,
            ToolType.NOTES,
            ToolType.BROWSER,
            ToolType.AI_ASSISTANT,
            ToolType.STOPWATCH_TIMER,
            ToolType.CLIPBOARD,
            ToolType.TODO,
            ToolType.QR_SCANNER
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdgeHelper.setup(this, findViewById(R.id.dashboard_root));

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        tvEngineStatus = findViewById(R.id.tv_engine_status);
        tvWindowCount = findViewById(R.id.tv_active_window_count);
        chipPermStatus = findViewById(R.id.chip_perm_status);
        btnLaunchEngine = findViewById(R.id.btn_launch_engine);
        containerFavorites = findViewById(R.id.container_favorite_tools);
        containerRecent = findViewById(R.id.container_recent_tools);
        tvRecentHeader = findViewById(R.id.tv_recent_header);
        switchBubble = findViewById(R.id.switch_dash_bubble);
        switchSidebar = findViewById(R.id.switch_dash_sidebar);

        RecyclerView rvQuick = findViewById(R.id.rv_quick_tools);
        rvQuick.setLayoutManager(new GridLayoutManager(this, 4));
        rvQuick.setAdapter(new QuickToolsAdapter());

        btnLaunchEngine.setOnClickListener(v -> {
            if (FloatingEngineBus.isRunning()) {
                FloatingEngineBus.stopEngine(this);
            } else {
                if (!FloatingEngineBus.startEngine(this)) {
                    Toast.makeText(this, "Grant Display Over Other Apps permission to launch floating windows.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, PermissionSetupActivity.class));
                }
            }
        });

        chipPermStatus.setOnClickListener(v ->
                startActivity(new Intent(this, PermissionSetupActivity.class)));

        findViewById(R.id.btn_dash_settings).setOnClickListener(v ->
                startActivity(new Intent(this, GeneralSettingsActivity.class)));

        findViewById(R.id.btn_open_tools_menu).setOnClickListener(v ->
                startActivity(new Intent(this, FloatingToolsMenuActivity.class)));

        findViewById(R.id.btn_open_window_manager).setOnClickListener(v ->
                startActivity(new Intent(this, WindowManagerActivity.class)));

        findViewById(R.id.btn_dash_appearance).setOnClickListener(v ->
                startActivity(new Intent(this, AppearanceSettingsActivity.class)));

        findViewById(R.id.btn_dash_sidebar_cfg).setOnClickListener(v ->
                startActivity(new Intent(this, SidebarSettingsActivity.class)));

        findViewById(R.id.btn_dash_privacy).setOnClickListener(v ->
                startActivity(new Intent(this, PrivacyInfoActivity.class)));

        findViewById(R.id.btn_dash_about).setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));

        switchBubble.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingSwitches) return;
            viewModel.getPreferences().setBubbleEnabled(isChecked);
            FloatingEngineBus.refreshConfig(this);
        });

        switchSidebar.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingSwitches) return;
            viewModel.getPreferences().setSidebarEnabled(isChecked);
            FloatingEngineBus.refreshConfig(this);
        });

        findViewById(R.id.btn_dash_open_drawer).setOnClickListener(v -> {
            if (!PermissionHelper.canDrawOverlays(this)) {
                Toast.makeText(this, "Grant Display Over Other Apps permission to use the slide-out drawer.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, PermissionSetupActivity.class));
                return;
            }
            if (!viewModel.getPreferences().isSidebarEnabled()) {
                viewModel.getPreferences().setSidebarEnabled(true);
                refreshQuickSettings();
            }
            FloatingEngineBus.openSidebarDrawer(this);
            Toast.makeText(this, "Slide-out drawer opened — drag any mini-app onto the screen!", Toast.LENGTH_SHORT).show();
        });

        viewModel.observeEngineRunning().observe(this, running -> {
            boolean isRunning = running != null && running;
            tvEngineStatus.setText(isRunning ? R.string.engine_status_running : R.string.engine_status_idle);
            tvEngineStatus.setTextColor(getColor(isRunning ? R.color.mf_success : R.color.electric_cyan));
            btnLaunchEngine.setText(isRunning ? R.string.stop_floating_engine : R.string.launch_floating_engine);
        });

        viewModel.observeWindows().observe(this, windows ->
                tvWindowCount.setText(String.valueOf(windows != null ? windows.size() : 0)));

        viewModel.observeRecentTools().observe(this, this::renderRecentTools);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionBadge();
        refreshQuickSettings();
        renderFavoriteTools();
    }

    private void refreshPermissionBadge() {
        boolean overlay = PermissionHelper.canDrawOverlays(this);
        chipPermStatus.setText(overlay ? "Overlay Ready" : "Grant Overlay");
        chipPermStatus.setTextColor(getColor(overlay ? R.color.mf_success : R.color.mf_warning));
    }

    private void refreshQuickSettings() {
        AppPreferences prefs = viewModel.getPreferences();
        updatingSwitches = true;
        switchBubble.setChecked(prefs.isBubbleEnabled());
        switchSidebar.setChecked(prefs.isSidebarEnabled());
        updatingSwitches = false;
    }

    private void renderFavoriteTools() {
        containerFavorites.removeAllViews();
        Set<String> favs = viewModel.getPreferences().getFavoriteTools();
        if (favs.isEmpty()) {
            addHorizontalToolPill(containerFavorites, ToolType.CALCULATOR);
            return;
        }
        for (String id : favs) {
            addHorizontalToolPill(containerFavorites, ToolType.fromId(id));
        }
    }

    private void renderRecentTools(List<ToolUsageEntity> recents) {
        containerRecent.removeAllViews();
        if (recents == null || recents.isEmpty()) {
            tvRecentHeader.setVisibility(View.GONE);
            return;
        }
        tvRecentHeader.setVisibility(View.VISIBLE);
        for (ToolUsageEntity usage : recents) {
            addHorizontalToolPill(containerRecent, ToolType.fromId(usage.toolId));
        }
    }

    private void addHorizontalToolPill(LinearLayout parent, ToolType type) {
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(android.view.Gravity.CENTER_VERTICAL);
        pill.setBackgroundResource(R.drawable.bg_glass_card);
        int padH = EdgeToEdgeHelper.dpToPx(this, 14);
        int padV = EdgeToEdgeHelper.dpToPx(this, 10);
        pill.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(EdgeToEdgeHelper.dpToPx(this, 8));
        pill.setLayoutParams(lp);

        ImageView icon = new ImageView(this);
        int iconSz = EdgeToEdgeHelper.dpToPx(this, 18);
        icon.setLayoutParams(new LinearLayout.LayoutParams(iconSz, iconSz));
        icon.setImageResource(type.iconRes);
        icon.setColorFilter(getColor(R.color.electric_cyan));
        pill.addView(icon);

        TextView label = new TextView(this);
        label.setText(type.titleRes);
        label.setTextColor(getColor(R.color.mf_text_primary));
        label.setTextSize(12f);
        label.setPadding(EdgeToEdgeHelper.dpToPx(this, 6), 0, 0, 0);
        pill.addView(label);

        pill.setOnClickListener(v -> launchToolSmart(type));
        parent.addView(pill);
    }

    private void launchToolSmart(ToolType type) {
        if (PermissionHelper.canDrawOverlays(this)) {
            FloatingEngineBus.openFloatingTool(this, type);
            Toast.makeText(this, "Launched " + getString(type.titleRes) + " floating window", Toast.LENGTH_SHORT).show();
        } else {
            ToolNavigationHelper.openFullScreen(this, type);
        }
    }

    private class QuickToolsAdapter extends RecyclerView.Adapter<QuickToolsAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_tool_chip, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ToolType type = quickTools[position];
            holder.icon.setImageResource(type.iconRes);
            holder.name.setText(type.titleRes);
            holder.itemView.setOnClickListener(v -> launchToolSmart(type));
            holder.itemView.setOnLongClickListener(v -> {
                ToolNavigationHelper.openFullScreen(MainActivity.this, type);
                return true;
            });
        }

        @Override
        public int getItemCount() { return quickTools.length; }

        class VH extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView name;
            VH(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.iv_tool_icon);
                name = itemView.findViewById(R.id.tv_tool_name);
            }
        }
    }
}
