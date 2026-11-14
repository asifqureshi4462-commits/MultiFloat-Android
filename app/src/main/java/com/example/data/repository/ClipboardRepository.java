package com.example.data.repository;

import androidx.lifecycle.LiveData;
import com.example.data.local.dao.ClipboardDao;
import com.example.data.local.entity.ClipboardEntity;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ClipboardRepository {
    private final ClipboardDao clipboardDao;
    private final ExecutorService ioExecutor;

    public ClipboardRepository(ClipboardDao clipboardDao, ExecutorService ioExecutor) {
        this.clipboardDao = clipboardDao;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<List<ClipboardEntity>> observeHistory() {
        return clipboardDao.observeHistory();
    }

    public void saveEntryIfNew(String text) {
        if (text == null || text.trim().isEmpty()) return;
        final String trimmed = text.trim();
        ioExecutor.execute(() -> {
            String latest = clipboardDao.getLatestTextSync();
            if (!trimmed.equals(latest)) {
                clipboardDao.insert(new ClipboardEntity(trimmed, System.currentTimeMillis()));
            }
        });
    }

    public void deleteEntry(long id) {
        ioExecutor.execute(() -> clipboardDao.deleteById(id));
    }

    public void clearAll() {
        ioExecutor.execute(clipboardDao::clearAll);
    }
}
