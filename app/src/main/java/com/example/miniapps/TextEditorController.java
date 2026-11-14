package com.example.miniapps;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.data.local.entity.SavedTextFileEntity;
import com.example.data.repository.TextFileRepository;
import com.example.util.ClipboardHelper;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TextEditorController extends BaseMiniAppController {

    private final TextFileRepository repository;
    private final List<SavedTextFileEntity> savedFiles = new ArrayList<>();
    private FilesAdapter adapter;
    private long currentFileId = 0;

    private EditText etFileName;
    private EditText etBody;
    private TextView tvStats;
    private RecyclerView rvSavedFiles;
    private boolean showingFiles = false;

    public TextEditorController(Context context, HostCallback hostCallback) {
        super(context, hostCallback);
        this.repository = MultiFloatApp.get().getTextFileRepository();
    }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.miniapp_text_editor, parent, false);

        etFileName = view.findViewById(R.id.et_editor_filename);
        etBody = view.findViewById(R.id.et_editor_body);
        tvStats = view.findViewById(R.id.tv_editor_stats);
        rvSavedFiles = view.findViewById(R.id.rv_saved_files);

        rvSavedFiles.setLayoutManager(new LinearLayoutManager(context));
        adapter = new FilesAdapter();
        rvSavedFiles.setAdapter(adapter);

        observe(repository.observeAllFiles(), files -> {
            savedFiles.clear();
            if (files != null) savedFiles.addAll(files);
            adapter.notifyDataSetChanged();
        });

        etBody.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();
                int chars = text.length();
                int words = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
                tvStats.setText(words + " words • " + chars + " chars");
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        view.findViewById(R.id.btn_editor_save).setOnClickListener(v -> {
            String name = etFileName.getText().toString().trim();
            if (name.isEmpty()) name = "draft.txt";
            repository.saveFile(currentFileId, name, etBody.getText().toString());
            Toast.makeText(context, "Saved " + name, Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btn_editor_copy).setOnClickListener(v ->
                ClipboardHelper.copyText(context, "Text Editor", etBody.getText().toString(), true));

        view.findViewById(R.id.btn_editor_paste).setOnClickListener(v -> {
            String clip = ClipboardHelper.readCurrentClipboard(context);
            if (!clip.isEmpty()) {
                int pos = Math.max(0, etBody.getSelectionStart());
                etBody.getText().insert(pos, clip);
            }
        });

        view.findViewById(R.id.btn_editor_share).setOnClickListener(v -> {
            String content = etBody.getText().toString();
            if (content.isEmpty()) return;
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, etFileName.getText().toString());
            shareIntent.putExtra(Intent.EXTRA_TEXT, content);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(shareIntent, "Share text"));
        });

        TextView btnFiles = view.findViewById(R.id.btn_editor_files);
        btnFiles.setOnClickListener(v -> {
            showingFiles = !showingFiles;
            rvSavedFiles.setVisibility(showingFiles ? View.VISIBLE : View.GONE);
            etBody.setVisibility(showingFiles ? View.GONE : View.VISIBLE);
            btnFiles.setText(showingFiles ? "Back to Editor" : "Saved Files");
        });

        return view;
    }

    private class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_simple_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            SavedTextFileEntity file = savedFiles.get(position);
            holder.title.setText(file.fileName);
            holder.subtitle.setText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(file.updatedAt)));
            holder.actionBtn.setVisibility(View.GONE);
            holder.deleteBtn.setOnClickListener(v -> repository.deleteFile(file.id));
            holder.itemView.setOnClickListener(v -> {
                currentFileId = file.id;
                etFileName.setText(file.fileName);
                etBody.setText(file.content);
                showingFiles = false;
                rvSavedFiles.setVisibility(View.GONE);
                etBody.setVisibility(View.VISIBLE);
            });
        }

        @Override
        public int getItemCount() { return savedFiles.size(); }

        class VH extends RecyclerView.ViewHolder {
            final TextView title, subtitle;
            final ImageButton actionBtn, deleteBtn;
            VH(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tv_row_title);
                subtitle = itemView.findViewById(R.id.tv_row_subtitle);
                actionBtn = itemView.findViewById(R.id.btn_row_action);
                deleteBtn = itemView.findViewById(R.id.btn_row_delete);
            }
        }
    }
}
