package com.example.data.repository;

import androidx.lifecycle.LiveData;
import com.example.data.local.dao.ToolUsageDao;
import com.example.data.local.entity.ToolUsageEntity;
import com.example.data.preferences.AppPreferences;
import com.example.model.ToolType;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ToolsRepository {
    private final ToolUsageDao toolUsageDao;
    private final AppPreferences preferences;
    private final ExecutorService ioExecutor;

    public ToolsRepository(ToolUsageDao toolUsageDao, AppPreferences preferences, ExecutorService ioExecutor) {
        this.toolUsageDao = toolUsageDao;
        this.preferences = preferences;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<List<ToolUsageEntity>> observeRecentTools() {
        return toolUsageDao.observeRecentTools();
    }

    public void recordToolOpened(ToolType toolType) {
        ioExecutor.execute(() -> {
            ToolUsageEntity existing = toolUsageDao.getByIdSync(toolType.id);
            int count = existing != null ? existing.openCount + 1 : 1;
            toolUsageDao.upsert(new ToolUsageEntity(toolType.id, System.currentTimeMillis(), count));
        });
    }

    public List<ToolUsageEntity> getRecentToolsSync() {
        return toolUsageDao.getRecentToolsSync();
    }

    public boolean isFavorite(ToolType toolType) {
        return preferences.isFavorite(toolType.id);
    }

    public void toggleFavorite(ToolType toolType) {
        preferences.toggleFavorite(toolType.id);
    }
}
