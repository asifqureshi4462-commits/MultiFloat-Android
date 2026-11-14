package com.example.data.repository;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import com.example.data.local.dao.AiMessageDao;
import com.example.data.local.entity.AiMessageEntity;
import com.example.data.preferences.AppPreferences;
import com.example.data.preferences.SecureKeyStore;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public class AiChatRepository {

    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private final AiMessageDao messageDao;
    private final AppPreferences preferences;
    private final SecureKeyStore keyStore;
    private final ExecutorService ioExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client;

    public interface AiCallback {
        void onComplete(boolean success, String message);
    }

    public AiChatRepository(AiMessageDao messageDao, AppPreferences preferences,
                            SecureKeyStore keyStore, ExecutorService ioExecutor) {
        this.messageDao = messageDao;
        this.preferences = preferences;
        this.keyStore = keyStore;
        this.ioExecutor = ioExecutor;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .build();
    }

    public LiveData<List<AiMessageEntity>> observeMessages() {
        return messageDao.observeMessages();
    }

    public boolean hasApiKey() {
        return keyStore.hasSecret(AppPreferences.KEY_AI_API);
    }

    public void clearChat() {
        ioExecutor.execute(messageDao::clearAll);
    }

    public void sendPrompt(String prompt, AiCallback callback) {
        if (prompt == null || prompt.trim().isEmpty()) {
            mainHandler.post(() -> callback.onComplete(false, "Please enter a message."));
            return;
        }
        String trimmed = prompt.trim();
        ioExecutor.execute(() -> {
            messageDao.insert(new AiMessageEntity("user", trimmed, System.currentTimeMillis()));

            String apiKey = keyStore.getSecret(AppPreferences.KEY_AI_API);
            if (apiKey.isEmpty()) {
                String msg = "No AI API key configured. Open General & API Settings to add your Gemini or OpenAI-compatible API key securely.";
                messageDao.insert(new AiMessageEntity("error", msg, System.currentTimeMillis()));
                mainHandler.post(() -> callback.onComplete(false, msg));
                return;
            }

            try {
                String reply = AppPreferences.AI_PROVIDER_OPENAI.equals(preferences.getAiProvider())
                        ? callOpenAiCompatible(apiKey, trimmed)
                        : callGeminiRest(apiKey, trimmed);
                messageDao.insert(new AiMessageEntity("assistant", reply, System.currentTimeMillis()));
                mainHandler.post(() -> callback.onComplete(true, reply));
            } catch (Exception e) {
                String err = "AI request failed: " + (e.getMessage() != null ? e.getMessage() : "Network error");
                messageDao.insert(new AiMessageEntity("error", err, System.currentTimeMillis()));
                mainHandler.post(() -> callback.onComplete(false, err));
            }
        });
    }

    private String callGeminiRest(String apiKey, String prompt) throws Exception {
        String model = preferences.getAiModel();
        if (model.isEmpty()) model = "gemini-2.5-flash";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        JSONArray contents = new JSONArray();
        List<AiMessageEntity> history = messageDao.getAllMessagesSync();
        int start = Math.max(0, history.size() - 10);
        for (int i = start; i < history.size(); i++) {
            AiMessageEntity item = history.get(i);
            if ("error".equals(item.role)) continue;
            JSONObject part = new JSONObject().put("text", item.text);
            JSONObject content = new JSONObject()
                    .put("role", "user".equals(item.role) ? "user" : "model")
                    .put("parts", new JSONArray().put(part));
            contents.put(content);
        }
        if (contents.length() == 0) {
            contents.put(new JSONObject()
                    .put("role", "user")
                    .put("parts", new JSONArray().put(new JSONObject().put("text", prompt))));
        }

        JSONObject bodyJson = new JSONObject().put("contents", contents);
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(bodyJson.toString(), JSON_MEDIA))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String respStr = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + extractApiError(respStr));
            }
            JSONObject root = new JSONObject(respStr);
            JSONArray candidates = root.optJSONArray("candidates");
            if (candidates != null && candidates.length() > 0) {
                JSONObject first = candidates.getJSONObject(0);
                JSONObject contentObj = first.optJSONObject("content");
                if (contentObj != null) {
                    JSONArray parts = contentObj.optJSONArray("parts");
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text", "").trim();
                    }
                }
            }
            throw new IOException("Empty response from Gemini API.");
        }
    }

    private String callOpenAiCompatible(String apiKey, String prompt) throws Exception {
        String endpoint = preferences.getAiEndpoint();
        String model = preferences.getAiModel();
        if (model.isEmpty() || model.startsWith("gemini")) model = "gpt-4o-mini";

        JSONArray messages = new JSONArray();
        List<AiMessageEntity> history = messageDao.getAllMessagesSync();
        int start = Math.max(0, history.size() - 10);
        for (int i = start; i < history.size(); i++) {
            AiMessageEntity item = history.get(i);
            if ("error".equals(item.role)) continue;
            messages.put(new JSONObject()
                    .put("role", "user".equals(item.role) ? "user" : "assistant")
                    .put("content", item.text));
        }

        JSONObject bodyJson = new JSONObject()
                .put("model", model)
                .put("messages", messages);

        Request request = new Request.Builder()
                .url(endpoint)
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(bodyJson.toString(), JSON_MEDIA))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String respStr = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + extractApiError(respStr));
            }
            JSONObject root = new JSONObject(respStr);
            JSONArray choices = root.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject message = choices.getJSONObject(0).optJSONObject("message");
                if (message != null) {
                    return message.optString("content", "").trim();
                }
            }
            throw new IOException("Empty response from AI endpoint.");
        }
    }

    private String extractApiError(String raw) {
        try {
            JSONObject obj = new JSONObject(raw);
            JSONObject err = obj.optJSONObject("error");
            if (err != null) return err.optString("message", raw);
        } catch (Exception ignored) {
        }
        return raw.length() > 140 ? raw.substring(0, 140) : raw;
    }
}
