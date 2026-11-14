package com.example.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.data.local.entity.CalcHistoryEntity;
import java.util.List;

@Dao
public interface CalcHistoryDao {
    @Query("SELECT * FROM calc_history ORDER BY createdAt DESC LIMIT 50")
    LiveData<List<CalcHistoryEntity>> observeHistory();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CalcHistoryEntity entity);

    @Query("DELETE FROM calc_history")
    void clearAll();
}
