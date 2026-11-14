package com.example;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import com.example.data.local.MultiFloatDatabase;
import com.example.data.preferences.AppPreferences;
import com.example.data.preferences.SecureKeyStore;
import com.example.data.repository.AiChatRepository;
import com.example.data.repository.CalculatorRepository;
import com.example.data.repository.ClipboardRepository;
import com.example.data.repository.InstalledAppsRepository;
import com.example.data.repository.NotesRepository;
import com.example.data.repository.TextFileRepository;
import com.example.data.repository.TodoRepository;
import com.example.data.repository.ToolsRepository;
import com.example.data.repository.TranslatorRepository;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiFloatApp extends Application {

    public static final String CHANNEL_ENGINE = "multifloat_engine_channel";

    private static MultiFloatApp instance;

    private AppPreferences preferences;
    private SecureKeyStore secureKeyStore;
    private MultiFloatDatabase database;
    private ExecutorService ioExecutor;

    private NotesRepository notesRepository;
    private TodoRepository todoRepository;
    private ClipboardRepository clipboardRepository;
    private CalculatorRepository calculatorRepository;
    private AiChatRepository aiChatRepository;
    private TranslatorRepository translatorRepository;
    private TextFileRepository textFileRepository;
    private ToolsRepository toolsRepository;
    private InstalledAppsRepository installedAppsRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        preferences = new AppPreferences(this);
        AppPreferences.applyThemeMode(preferences.getThemeMode());

        secureKeyStore = new SecureKeyStore(this);
        database = MultiFloatDatabase.getInstance(this);
        ioExecutor = Executors.newFixedThreadPool(4);

        notesRepository = new NotesRepository(database.noteDao(), ioExecutor);
        todoRepository = new TodoRepository(database.todoDao(), ioExecutor);
        clipboardRepository = new ClipboardRepository(database.clipboardDao(), ioExecutor);
        calculatorRepository = new CalculatorRepository(database.calcHistoryDao(), ioExecutor);
        aiChatRepository = new AiChatRepository(database.aiMessageDao(), preferences, secureKeyStore, ioExecutor);
        translatorRepository = new TranslatorRepository(preferences, secureKeyStore, ioExecutor);
        textFileRepository = new TextFileRepository(database.savedTextFileDao(), ioExecutor);
        toolsRepository = new ToolsRepository(database.toolUsageDao(), preferences, ioExecutor);
        installedAppsRepository = new InstalledAppsRepository(preferences);
        com.example.miniapps.MiniAppRegistry.getInstance();

        createNotificationChannels();
    }

    public static MultiFloatApp get() {
        return instance;
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel engineChannel = new NotificationChannel(
                    CHANNEL_ENGINE,
                    getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            engineChannel.setDescription(getString(R.string.notif_channel_desc));
            engineChannel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(engineChannel);
            }
        }
    }

    public AppPreferences getPreferences() { return preferences; }
    public SecureKeyStore getSecureKeyStore() { return secureKeyStore; }
    public MultiFloatDatabase getDatabase() { return database; }
    public ExecutorService getIoExecutor() { return ioExecutor; }
    public NotesRepository getNotesRepository() { return notesRepository; }
    public TodoRepository getTodoRepository() { return todoRepository; }
    public ClipboardRepository getClipboardRepository() { return clipboardRepository; }
    public CalculatorRepository getCalculatorRepository() { return calculatorRepository; }
    public AiChatRepository getAiChatRepository() { return aiChatRepository; }
    public TranslatorRepository getTranslatorRepository() { return translatorRepository; }
    public TextFileRepository getTextFileRepository() { return textFileRepository; }
    public ToolsRepository getToolsRepository() { return toolsRepository; }
    public InstalledAppsRepository getInstalledAppsRepository() { return installedAppsRepository; }
    public com.example.miniapps.MiniAppRegistry getMiniAppRegistry() { return com.example.miniapps.MiniAppRegistry.getInstance(); }
}
