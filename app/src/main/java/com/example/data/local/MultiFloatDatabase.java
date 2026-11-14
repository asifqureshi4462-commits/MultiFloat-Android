package com.example.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.data.local.dao.AiMessageDao;
import com.example.data.local.dao.CalcHistoryDao;
import com.example.data.local.dao.ClipboardDao;
import com.example.data.local.dao.NoteDao;
import com.example.data.local.dao.SavedTextFileDao;
import com.example.data.local.dao.TodoDao;
import com.example.data.local.dao.ToolUsageDao;
import com.example.data.local.entity.AiMessageEntity;
import com.example.data.local.entity.CalcHistoryEntity;
import com.example.data.local.entity.ClipboardEntity;
import com.example.data.local.entity.NoteEntity;
import com.example.data.local.entity.SavedTextFileEntity;
import com.example.data.local.entity.TodoEntity;
import com.example.data.local.entity.ToolUsageEntity;

@Database(
        entities = {
                NoteEntity.class,
                TodoEntity.class,
                ClipboardEntity.class,
                CalcHistoryEntity.class,
                AiMessageEntity.class,
                SavedTextFileEntity.class,
                ToolUsageEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class MultiFloatDatabase extends RoomDatabase {

    private static volatile MultiFloatDatabase instance;

    public abstract NoteDao noteDao();
    public abstract TodoDao todoDao();
    public abstract ClipboardDao clipboardDao();
    public abstract CalcHistoryDao calcHistoryDao();
    public abstract AiMessageDao aiMessageDao();
    public abstract SavedTextFileDao savedTextFileDao();
    public abstract ToolUsageDao toolUsageDao();

    public static MultiFloatDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (MultiFloatDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            MultiFloatDatabase.class,
                            "multifloat_db"
                    ).build();
                }
            }
        }
        return instance;
    }
}
