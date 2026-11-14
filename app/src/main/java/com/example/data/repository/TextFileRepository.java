package com.example.data.repository;

import androidx.lifecycle.LiveData;
import com.example.data.local.dao.SavedTextFileDao;
import com.example.data.local.entity.SavedTextFileEntity;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class TextFileRepository {
    private final SavedTextFileDao dao;
    private final ExecutorService ioExecutor;

    public TextFileRepository(SavedTextFileDao dao, ExecutorService ioExecutor) {
        this.dao = dao;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<List<SavedTextFileEntity>> observeAllFiles() {
        return dao.observeAllFiles();
    }

    public void saveFile(long existingId, String fileName, String content) {
        ioExecutor.execute(() -> {
            SavedTextFileEntity entity = new SavedTextFileEntity(fileName, content, System.currentTimeMillis());
            if (existingId > 0) entity.id = existingId;
            dao.insert(entity);
        });
    }

    public void deleteFile(long id) {
        ioExecutor.execute(() -> dao.deleteById(id));
    }
}
