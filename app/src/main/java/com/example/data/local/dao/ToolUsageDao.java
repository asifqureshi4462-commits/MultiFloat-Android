package com.example.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.data.local.entity.ToolUsageEntity;
import java.util.List;

@Dao
public interface ToolUsageDao {
    @Query("SELECT * FROM tool_usage ORDER BY lastOpenedAt DESC LIMIT 12")
    LiveData<List<ToolUsageEntity>> observeRecentTools();

    @Query("SELECT * FROM tool_usage ORDER BY lastOpenedAt DESC LIMIT 6")
    List<ToolUsageEntity> getRecentToolsSync();

    @Query("SELECT * FROM tool_usage WHERE toolId = :toolId LIMIT 1")
    ToolUsageEntity getByIdSync(String toolId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ToolUsageEntity entity);
}
