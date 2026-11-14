package com.example.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.data.local.entity.TodoEntity;
import java.util.List;

@Dao
public interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY completed ASC, createdAt DESC")
    LiveData<List<TodoEntity>> observeAllTodos();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TodoEntity todo);

    @Update
    void update(TodoEntity todo);

    @Query("DELETE FROM todos WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM todos WHERE completed = 1")
    void clearCompleted();
}
