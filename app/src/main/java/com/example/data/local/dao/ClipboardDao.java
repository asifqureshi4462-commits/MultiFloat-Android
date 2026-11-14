package com.example.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.data.local.entity.ClipboardEntity;
import java.util.List;

@Dao
public interface ClipboardDao {
    @Query("SELECT * FROM clipboard_entries ORDER BY copiedAt DESC")
    LiveData<List<ClipboardEntity>> observeHistory();

    @Query("SELECT text FROM clipboard_entries ORDER BY copiedAt DESC LIMIT 1")
    String getLatestTextSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ClipboardEntity entry);

    @Query("DELETE FROM clipboard_entries WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM clipboard_entries")
    void clearAll();
}
