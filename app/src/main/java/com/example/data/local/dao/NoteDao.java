package com.example.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.data.local.entity.NoteEntity;
import java.util.List;

@Dao
public interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    LiveData<List<NoteEntity>> observeAllNotes();

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY pinned DESC, updatedAt DESC")
    LiveData<List<NoteEntity>> searchNotes(String query);

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    NoteEntity getByIdSync(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(NoteEntity note);

    @Update
    void update(NoteEntity note);

    @Delete
    void delete(NoteEntity note);

    @Query("DELETE FROM notes WHERE id = :id")
    void deleteById(long id);
}
