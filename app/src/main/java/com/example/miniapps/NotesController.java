package com.example.miniapps;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.data.local.entity.NoteEntity;
import com.example.data.repository.NotesRepository;
import com.example.util.ClipboardHelper;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesController extends BaseMiniAppController {

    private final NotesRepository repository;
    private final List<NoteEntity> allNotes = new ArrayList<>();
    private final List<NoteEntity> filteredNotes = new ArrayList<>();
    private NotesAdapter adapter;

    private View panelList;
    private View panelEditor;
    private TextView tvEmpty;
    private TextView tvStatus;
    private EditText etSearch;
    private EditText etTitle;
    private EditText etContent;
    private ImageButton btnPin;

    private NoteEntity currentNote;
    private boolean suppressWatcher = false;
    private final Handler autoSaveHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoSaveRunnable = this::performAutoSave;

    public NotesController(Context context, MiniApp.HostCallback hostCallback) {
        super(context, hostCallback);
        this.repository = MultiFloatApp.get().getNotesRepository();
    }

    @NonNull
    @Override
    public com.example.model.ToolType getToolType() {
        return com.example.model.ToolType.NOTES;
    }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.miniapp_notes, parent, false);

        panelList = view.findViewById(R.id.panel_notes_list);
        panelEditor = view.findViewById(R.id.panel_note_editor);
        tvEmpty = view.findViewById(R.id.tv_notes_empty);
        tvStatus = view.findViewById(R.id.tv_note_status);
        etSearch = view.findViewById(R.id.et_notes_search);
        etTitle = view.findViewById(R.id.et_note_title);
        etContent = view.findViewById(R.id.et_note_content);
        btnPin = view.findViewById(R.id.btn_note_pin);

        RecyclerView rv = view.findViewById(R.id.rv_notes);
        rv.setLayoutManager(new LinearLayoutManager(context));
        adapter = new NotesAdapter();
        rv.setAdapter(adapter);

        observe(repository.observeAllNotes(), notes -> {
            allNotes.clear();
            if (notes != null) allNotes.addAll(notes);
            applyFilter();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        view.findViewById(R.id.btn_new_note).setOnClickListener(v ->
                openNote(new NoteEntity("", "", System.currentTimeMillis(), false)));

        view.findViewById(R.id.btn_note_back).setOnClickListener(v -> {
            performAutoSave();
            panelEditor.setVisibility(View.GONE);
            panelList.setVisibility(View.VISIBLE);
            currentNote = null;
        });

        btnPin.setOnClickListener(v -> {
            if (currentNote != null) {
                currentNote.pinned = !currentNote.pinned;
                updatePinIcon();
                performAutoSave();
            }
        });

        view.findViewById(R.id.btn_note_delete).setOnClickListener(v -> {
            autoSaveHandler.removeCallbacks(autoSaveRunnable);
            if (currentNote != null && currentNote.id > 0) {
                repository.deleteNote(currentNote.id);
            }
            currentNote = null;
            panelEditor.setVisibility(View.GONE);
            panelList.setVisibility(View.VISIBLE);
        });

        TextWatcher editorWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (suppressWatcher || currentNote == null) return;
                tvStatus.setText("Saving...");
                autoSaveHandler.removeCallbacks(autoSaveRunnable);
                autoSaveHandler.postDelayed(autoSaveRunnable, 450);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etTitle.addTextChangedListener(editorWatcher);
        etContent.addTextChangedListener(editorWatcher);

        return view;
    }

    private void openNote(NoteEntity note) {
        currentNote = note;
        suppressWatcher = true;
        etTitle.setText(note.title);
        etContent.setText(note.content);
        suppressWatcher = false;
        updatePinIcon();
        tvStatus.setText(note.id > 0 ? "Auto-saved" : "New note");
        panelList.setVisibility(View.GONE);
        panelEditor.setVisibility(View.VISIBLE);
    }

    private void updatePinIcon() {
        if (currentNote != null && currentNote.pinned) {
            btnPin.setImageResource(R.drawable.ic_star_filled);
        } else {
            btnPin.setImageResource(R.drawable.ic_star_outline);
        }
    }

    private void performAutoSave() {
        if (currentNote == null) return;
        String title = etTitle.getText().toString().trim();
        String body = etContent.getText().toString();
        if (title.isEmpty() && body.trim().isEmpty()) return;
        currentNote.title = title.isEmpty() ? "Untitled Note" : title;
        currentNote.content = body;
        repository.saveNote(currentNote, id -> tvStatus.post(() -> tvStatus.setText("Auto-saved")));
    }

    private void applyFilter() {
        String q = etSearch.getText().toString().trim().toLowerCase(Locale.US);
        filteredNotes.clear();
        for (NoteEntity n : allNotes) {
            if (q.isEmpty()
                    || (n.title != null && n.title.toLowerCase(Locale.US).contains(q))
                    || (n.content != null && n.content.toLowerCase(Locale.US).contains(q))) {
                filteredNotes.add(n);
            }
        }
        tvEmpty.setVisibility(filteredNotes.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        performAutoSave();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        performAutoSave();
        super.onDestroy();
    }

    private class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_simple_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            NoteEntity note = filteredNotes.get(position);
            String prefix = note.pinned ? "★ " : "";
            holder.title.setText(prefix + (note.title == null || note.title.isEmpty() ? "Untitled" : note.title));
            String preview = note.content != null ? note.content.replace("\n", " ") : "";
            String dateStr = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(note.updatedAt));
            holder.subtitle.setText(preview.isEmpty() ? dateStr : preview);
            holder.actionBtn.setOnClickListener(v ->
                    ClipboardHelper.copyText(context, "Note", note.title + "\n" + note.content, true));
            holder.deleteBtn.setOnClickListener(v -> repository.deleteNote(note.id));
            holder.itemView.setOnClickListener(v -> openNote(note));
        }

        @Override
        public int getItemCount() { return filteredNotes.size(); }

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
