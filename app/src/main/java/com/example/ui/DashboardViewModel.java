package com.example.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.MultiFloatApp;
import com.example.data.local.entity.ToolUsageEntity;
import com.example.data.preferences.AppPreferences;
import com.example.data.repository.ToolsRepository;
import com.example.model.FloatingWindowState;
import com.example.service.FloatingEngineBus;
import java.util.List;

public class DashboardViewModel extends ViewModel {

    private final ToolsRepository toolsRepository;
    private final AppPreferences preferences;

    public DashboardViewModel() {
        MultiFloatApp app = MultiFloatApp.get();
        this.toolsRepository = app.getToolsRepository();
        this.preferences = app.getPreferences();
    }

    public LiveData<Boolean> observeEngineRunning() {
        return FloatingEngineBus.observeEngineRunning();
    }

    public LiveData<List<FloatingWindowState>> observeWindows() {
        return FloatingEngineBus.observeWindows();
    }

    public LiveData<List<ToolUsageEntity>> observeRecentTools() {
        return toolsRepository.observeRecentTools();
    }

    public AppPreferences getPreferences() {
        return preferences;
    }
}
