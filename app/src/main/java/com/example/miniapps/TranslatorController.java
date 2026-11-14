package com.example.miniapps;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.data.repository.TranslatorRepository;
import com.example.util.ClipboardHelper;

public class TranslatorController extends BaseMiniAppController {

    private static final String[] LANG_NAMES = {
            "English", "Spanish", "French", "German", "Hindi", "Arabic", "Japanese", "Korean", "Chinese"
    };
    private static final String[] LANG_CODES = {
            "en", "es", "fr", "de", "hi", "ar", "ja", "ko", "zh"
    };

    private final TranslatorRepository repository;
    private View bannerConfig;

    public TranslatorController(Context context, HostCallback hostCallback) {
        super(context, hostCallback);
        this.repository = MultiFloatApp.get().getTranslatorRepository();
    }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.miniapp_translator, parent, false);

        bannerConfig = view.findViewById(R.id.banner_translate_config);
        Spinner spSource = view.findViewById(R.id.spinner_source_lang);
        Spinner spTarget = view.findViewById(R.id.spinner_target_lang);
        EditText etInput = view.findViewById(R.id.et_translate_input);
        TextView tvOutput = view.findViewById(R.id.tv_translate_output);
        ProgressBar progress = view.findViewById(R.id.pb_translate);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context, android.R.layout.simple_spinner_dropdown_item, LANG_NAMES);
        spSource.setAdapter(adapter);
        spTarget.setAdapter(adapter);
        spSource.setSelection(0);
        spTarget.setSelection(1);

        refreshConfigBanner();

        view.findViewById(R.id.btn_translate_open_settings).setOnClickListener(v -> {
            if (hostCallback != null) hostCallback.onOpenSettingsRequested();
        });

        view.findViewById(R.id.btn_swap_langs).setOnClickListener(v -> {
            int s = spSource.getSelectedItemPosition();
            int t = spTarget.getSelectedItemPosition();
            spSource.setSelection(t);
            spTarget.setSelection(s);
        });

        view.findViewById(R.id.btn_copy_translation).setOnClickListener(v ->
                ClipboardHelper.copyText(context, "Translation", tvOutput.getText().toString(), true));

        view.findViewById(R.id.btn_do_translate).setOnClickListener(v -> {
            String src = LANG_CODES[spSource.getSelectedItemPosition()];
            String dst = LANG_CODES[spTarget.getSelectedItemPosition()];
            progress.setVisibility(View.VISIBLE);
            repository.translate(etInput.getText().toString(), src, dst, (success, textOrError) -> {
                progress.setVisibility(View.GONE);
                tvOutput.setText(textOrError);
                if (!success) refreshConfigBanner();
            });
        });

        return view;
    }

    public void refreshConfigBanner() {
        if (bannerConfig != null) {
            bannerConfig.setVisibility(repository.hasConfiguration() ? View.GONE : View.VISIBLE);
        }
    }
}
