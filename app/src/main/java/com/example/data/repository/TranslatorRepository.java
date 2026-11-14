package com.example.data.repository;

import android.os.Handler;
import android.os.Looper;
import com.example.data.preferences.AppPreferences;
import com.example.data.preferences.SecureKeyStore;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public class TranslatorRepository {

    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private final AppPreferences preferences;
    private final SecureKeyStore keyStore;
    private final ExecutorService ioExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client;

    public interface TranslateCallback {
        void onResult(boolean success, String textOrError);
    }

    public TranslatorRepository(AppPreferences preferences, SecureKeyStore keyStore, ExecutorService ioExecutor) {
        this.preferences = preferences;
        this.keyStore = keyStore;
        this.ioExecutor = ioExecutor;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .build();
    }

    public boolean hasConfiguration() {
        return keyStore.hasSecret(AppPreferences.KEY_TRANSLATE_API)
                || !preferences.getTranslateEndpoint().contains("googleapis.com");
    }

    public void translate(String text, String sourceCode, String targetCode, TranslateCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            mainHandler.post(() -> callback.onResult(false, "Enter text to translate."));
            return;
        }
        ioExecutor.execute(() -> {
            String apiKey = keyStore.getSecret(AppPreferences.KEY_TRANSLATE_API);
            String endpoint = preferences.getTranslateEndpoint();

            if (endpoint.contains("googleapis.com") && apiKey.isEmpty()) {
                mainHandler.post(() -> callback.onResult(false,
                        "Translation API key not configured. Add a Google Cloud Translation API key or LibreTranslate endpoint in General & API Settings."));
                return;
            }

            try {
                String translated;
                if (endpoint.contains("googleapis.com")) {
                    translated = callGoogleTranslateV2(endpoint, apiKey, text.trim(), sourceCode, targetCode);
                } else {
                    translated = callLibreTranslate(endpoint, apiKey, text.trim(), sourceCode, targetCode);
                }
                mainHandler.post(() -> callback.onResult(true, translated));
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Translation request failed";
                mainHandler.post(() -> callback.onResult(false, msg));
            }
        });
    }

    private String callGoogleTranslateV2(String endpoint, String apiKey, String text,
                                         String source, String target) throws Exception {
        String url = endpoint + (endpoint.contains("?") ? "&" : "?") + "key=" + apiKey;
        JSONObject payload = new JSONObject()
                .put("q", text)
                .put("target", target)
                .put("format", "text");
        if (!"auto".equals(source)) {
            payload.put("source", source);
        }
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(payload.toString(), JSON_MEDIA))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String raw = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Translation API error (" + response.code() + ")");
            }
            JSONObject root = new JSONObject(raw);
            JSONObject data = root.optJSONObject("data");
            if (data != null) {
                JSONArray translations = data.optJSONArray("translations");
                if (translations != null && translations.length() > 0) {
                    return translations.getJSONObject(0).optString("translatedText", "");
                }
            }
            throw new IOException("Invalid translation response.");
        }
    }

    private String callLibreTranslate(String endpoint, String apiKey, String text,
                                      String source, String target) throws Exception {
        JSONObject payload = new JSONObject()
                .put("q", text)
                .put("source", source)
                .put("target", target)
                .put("format", "text");
        if (!apiKey.isEmpty()) {
            payload.put("api_key", apiKey);
        }
        Request request = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(payload.toString(), JSON_MEDIA))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String raw = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Translation endpoint error (" + response.code() + ")");
            }
            JSONObject root = new JSONObject(raw);
            if (root.has("translatedText")) {
                return root.optString("translatedText", "");
            }
            throw new IOException("Unexpected response from translation server.");
        }
    }
}
