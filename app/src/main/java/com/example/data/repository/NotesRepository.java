package com.example.data.repository;

import androidx.lifecycle.LiveData;
import com.example.data.local.dao.NoteDao;
import com.example.data.local.entity.NoteEntity;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class NotesRepository {
    private final NoteDao noteDao;
    private final ExecutorService ioExecutor;

    public NotesRepository(NoteDao noteDao, ExecutorService ioExecutor) {
        this.noteDao = noteDao;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<List<NoteEntity>> observeAllNotes() {
        return noteDao.observeAllNotes();
    }

    public LiveData<List<NoteEntity>> searchNotes(String query) {
        if (query == null || query.trim().isEmpty()) {
            return noteDao.observeAllNotes();
        }
        return noteDao.searchNotes(query.trim());
    }

    public void saveNote(NoteEntity note, Callback<Long> callback) {
        ioExecutor.execute(() -> {
            note.updatedAt = System.currentTimeMillis();
            long id;
            if (note.id > 0) {
                noteDao.update(note);
                id = note.id;
            } else {
                id = noteDao.insert(note);
                note.id = id;
            }
            if (callback != null) callback.onResult(id);
        });
    }

    public void deleteNote(long id) {
        ioExecutor.execute(() -> noteDao.deleteById(id));
    }

    public interface Callback<T> {
        void onResult(T value);
    }
}
