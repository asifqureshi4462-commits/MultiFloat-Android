package com.example.miniapps;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.data.preferences.AppPreferences;

public class BrowserController extends BaseMiniAppController {

    private WebView webView;
    private EditText etUrl;
    private ProgressBar progressBar;
    private final AppPreferences preferences;

    public BrowserController(Context context, MiniApp.HostCallback hostCallback) {
        super(context, hostCallback);
        this.preferences = MultiFloatApp.get().getPreferences();
    }

    @androidx.annotation.NonNull
    @Override
    public com.example.model.ToolType getToolType() {
        return com.example.model.ToolType.BROWSER;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.miniapp_browser, parent, false);

        etUrl = view.findViewById(R.id.et_browser_url);
        progressBar = view.findViewById(R.id.pb_browser);
        webView = view.findViewById(R.id.webview_mini);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                etUrl.setText(url);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });

        view.findViewById(R.id.btn_browser_back).setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
        });

        view.findViewById(R.id.btn_browser_forward).setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
        });

        view.findViewById(R.id.btn_browser_refresh).setOnClickListener(v -> webView.reload());

        view.findViewById(R.id.btn_browser_home).setOnClickListener(v ->
                loadInputUrl(preferences.getBrowserHomeUrl()));

        etUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                loadInputUrl(etUrl.getText().toString());
                return true;
            }
            return false;
        });

        android.os.Bundle saved = getPendingSavedState();
        String initialUrl = saved != null ? saved.getString("browser_url", "") : "";
        loadInputUrl(initialUrl.isEmpty() ? preferences.getBrowserHomeUrl() : initialUrl);
        return view;
    }

    private void loadInputUrl(String raw) {
        if (raw == null) return;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return;
        String target;
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            target = trimmed;
        } else if (trimmed.contains(".") && !trimmed.contains(" ")) {
            target = "https://" + trimmed;
        } else {
            target = "https://duckduckgo.com/?q=" + android.net.Uri.encode(trimmed);
        }
        etUrl.setText(target);
        webView.loadUrl(target);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    public void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    public void onSaveState(@androidx.annotation.NonNull android.os.Bundle outState) {
        if (webView != null && webView.getUrl() != null) {
            outState.putString("browser_url", webView.getUrl());
        } else if (etUrl != null) {
            outState.putString("browser_url", etUrl.getText().toString());
        }
    }

    @Override
    public void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
