package com.example.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.data.preferences.AppPreferences;
import com.example.data.repository.InstalledAppsRepository;
import com.example.model.InstalledAppInfo;
import com.example.service.FloatingEngineBus;
import com.example.util.EdgeToEdgeHelper;
import com.example.util.ExternalAppLauncher;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;

public class InstalledAppsActivity extends AppCompatActivity {

    private AppPreferences preferences;
    private InstalledAppsRepository repository;

    private final List<InstalledAppInfo> allApps = new ArrayList<>();
    private final List<InstalledAppInfo> displayedApps = new ArrayList<>();

    private AppsAdapter adapter;
    private InstalledAppsRepository.FilterMode currentFilter = InstalledAppsRepository.FilterMode.ALL;
    private String currentQuery = "";

    private TextView tvCount, tvEmpty, tvFreeformStatus;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private boolean freeformSupported;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_installed_apps);
        EdgeToEdgeHelper.setup(this, findViewById(R.id.installed_apps_root));

        preferences = MultiFloatApp.get().getPreferences();
        repository = MultiFloatApp.get().getInstalledAppsRepository();
        freeformSupported = ExternalAppLauncher.isFreeformSupported(this);

        tvCount = findViewById(R.id.tv_apps_count);
        tvEmpty = findViewById(R.id.tv_apps_empty);
        tvFreeformStatus = findViewById(R.id.tv_freeform_status);
        progressBar = findViewById(R.id.pb_apps_loading);
        recyclerView = findViewById(R.id.rv_installed_apps);

        findViewById(R.id.btn_apps_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_apps_refresh).setOnClickListener(v -> loadAppsAsync());

        tvFreeformStatus.setText(freeformSupported
                ? R.string.freeform_supported_badge
                : R.string.freeform_unsupported_badge);
        tvFreeformStatus.setTextColor(getColor(freeformSupported ? R.color.mf_success : R.color.mf_warning));

        findViewById(R.id.btn_freeform_details).setOnClickListener(v -> showFreeformExplanationDialog(null));

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppsAdapter();
        recyclerView.setAdapter(adapter);

        EditText etSearch = findViewById(R.id.et_apps_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString() : "";
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        ChipGroup cgFilter = findViewById(R.id.cg_apps_filter);
        cgFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_apps_recent) {
                currentFilter = InstalledAppsRepository.FilterMode.RECENT;
            } else if (id == R.id.chip_apps_favorites) {
                currentFilter = InstalledAppsRepository.FilterMode.FAVORITES;
            } else if (id == R.id.chip_apps_sidebar) {
                currentFilter = InstalledAppsRepository.FilterMode.SIDEBAR;
            } else {
                currentFilter = InstalledAppsRepository.FilterMode.ALL;
            }
            applyFilters();
        });

        loadAppsAsync();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!allApps.isEmpty()) {
            loadAppsAsync();
        }
    }

    private void loadAppsAsync() {
        progressBar.setVisibility(View.VISIBLE);
        MultiFloatApp.get().getIoExecutor().execute(() -> {
            List<InstalledAppInfo> loaded = repository.loadLaunchableApps(getApplicationContext());
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                progressBar.setVisibility(View.GONE);
                allApps.clear();
                allApps.addAll(loaded);
                applyFilters();
            });
        });
    }

    private void applyFilters() {
        displayedApps.clear();
        displayedApps.addAll(InstalledAppsRepository.filterAndSort(allApps, currentQuery, currentFilter));

        int sidebarCount = preferences.getSelectedSidebarApps().size();
        tvCount.setText(allApps.size() + " launchable apps • " + sidebarCount + " in sidebar");
        tvEmpty.setVisibility(displayedApps.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(displayedApps.isEmpty() ? View.GONE : View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    private void handleLaunchClick(InstalledAppInfo app) {
        ExternalAppLauncher.LaunchStatus status = ExternalAppLauncher.launchExternalApp(
                this,
                app.packageName,
                ExternalAppLauncher.computeDefaultFreeformBounds(this, null),
                false
        );
        switch (status) {
            case LAUNCHED_FREEFORM:
                app.lastUsedTimeMs = System.currentTimeMillis();
                Toast.makeText(this, "Launched " + app.appName + " in freeform window", Toast.LENGTH_SHORT).show();
                break;
            case FREEFORM_UNSUPPORTED:
                showFreeformExplanationDialog(app);
                break;
            case APP_UNINSTALLED:
                Toast.makeText(this, getString(R.string.app_uninstalled_removed, app.appName), Toast.LENGTH_LONG).show();
                FloatingEngineBus.refreshConfig(this);
                loadAppsAsync();
                break;
            case LAUNCHED_NORMAL:
                app.lastUsedTimeMs = System.currentTimeMillis();
                break;
            default:
                Toast.makeText(this, "Unable to launch " + app.appName, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void showFreeformExplanationDialog(InstalledAppInfo pendingApp) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.freeform_info_title)
                .setMessage(freeformSupported
                        ? "This device declares Android Freeform Window Management. External apps launched via MultiFloat request freeform bounds using Android's ActivityOptions API."
                        : getString(R.string.freeform_unsupported_explanation))
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.action_open_dev_options, (d, w) -> {
                    try {
                        startActivity(ExternalAppLauncher.buildDeveloperOptionsIntent());
                    } catch (Exception e) {
                        Toast.makeText(this, "Enable Developer Options in Android Settings first.", Toast.LENGTH_SHORT).show();
                    }
                });

        if (pendingApp != null) {
            builder.setPositiveButton(R.string.action_launch_normally, (d, w) -> {
                ExternalAppLauncher.LaunchStatus st = ExternalAppLauncher.launchExternalApp(
                        this, pendingApp.packageName, null, true);
                if (st == ExternalAppLauncher.LaunchStatus.APP_UNINSTALLED) {
                    Toast.makeText(this, getString(R.string.app_uninstalled_removed, pendingApp.appName), Toast.LENGTH_LONG).show();
                    loadAppsAsync();
                } else {
                    pendingApp.lastUsedTimeMs = System.currentTimeMillis();
                }
            });
        }
        builder.show();
    }

    private class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_installed_app, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            InstalledAppInfo app = displayedApps.get(position);
            holder.name.setText(app.appName);
            holder.pkg.setText(app.packageName);
            if (app.icon != null) {
                holder.icon.setImageDrawable(app.icon);
            } else {
                holder.icon.setImageResource(R.drawable.ic_apps);
            }

            holder.favBtn.setImageResource(app.isFavorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            holder.favBtn.setOnClickListener(v -> {
                preferences.toggleFavoriteApp(app.packageName);
                app.isFavorite = preferences.isFavoriteApp(app.packageName);
                if (currentFilter == InstalledAppsRepository.FilterMode.FAVORITES && !app.isFavorite) {
                    applyFilters();
                } else {
                    notifyItemChanged(holder.getBindingAdapterPosition());
                }
            });

            holder.sidebarBtn.setText(app.isSidebarPinned ? "In Sidebar ✓" : "Add to Sidebar");
            holder.sidebarBtn.setOnClickListener(v -> {
                if (!repository.isAppInstalled(InstalledAppsActivity.this, app.packageName)) {
                    Toast.makeText(InstalledAppsActivity.this,
                            getString(R.string.app_uninstalled_removed, app.appName), Toast.LENGTH_LONG).show();
                    loadAppsAsync();
                    return;
                }
                boolean pinned = preferences.toggleSidebarApp(app.packageName);
                app.isSidebarPinned = pinned;
                FloatingEngineBus.refreshConfig(InstalledAppsActivity.this);
                Toast.makeText(InstalledAppsActivity.this,
                        pinned ? app.appName + " added to sidebar" : app.appName + " removed from sidebar",
                        Toast.LENGTH_SHORT).show();
                applyFilters();
            });

            holder.launchBtn.setText(freeformSupported ? "Float App" : "Open App");
            holder.launchBtn.setOnClickListener(v -> handleLaunchClick(app));
        }

        @Override
        public int getItemCount() {
            return displayedApps.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView name, pkg;
            final ImageButton favBtn;
            final Button sidebarBtn, launchBtn;

            VH(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.iv_installed_app_icon);
                name = itemView.findViewById(R.id.tv_installed_app_name);
                pkg = itemView.findViewById(R.id.tv_installed_app_pkg);
                favBtn = itemView.findViewById(R.id.btn_installed_app_fav);
                sidebarBtn = itemView.findViewById(R.id.btn_installed_app_sidebar);
                launchBtn = itemView.findViewById(R.id.btn_installed_app_launch);
            }
        }
    }
}
