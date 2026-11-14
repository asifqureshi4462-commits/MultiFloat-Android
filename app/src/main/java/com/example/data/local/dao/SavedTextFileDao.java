package com.example.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.data.local.entity.SavedTextFileEntity;
import java.util.List;

@Dao
public interface SavedTextFileDao {
    @Query("SELECT * FROM saved_text_files ORDER BY updatedAt DESC")
    LiveData<List<SavedTextFileEntity>> observeAllFiles();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SavedTextFileEntity file);

    @Query("DELETE FROM saved_text_files WHERE id = :id")
    void deleteById(long id);
}
