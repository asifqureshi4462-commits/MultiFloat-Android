package com.example.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.data.local.entity.AiMessageEntity;
import java.util.List;

@Dao
public interface AiMessageDao {
    @Query("SELECT * FROM ai_messages ORDER BY timestamp ASC")
    LiveData<List<AiMessageEntity>> observeMessages();

    @Query("SELECT * FROM ai_messages ORDER BY timestamp ASC")
    List<AiMessageEntity> getAllMessagesSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(AiMessageEntity message);

    @Query("DELETE FROM ai_messages")
    void clearAll();
}
