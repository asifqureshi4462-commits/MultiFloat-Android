package com.example.miniapps;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.data.local.entity.ClipboardEntity;
import com.example.data.repository.ClipboardRepository;
import com.example.util.ClipboardHelper;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ClipboardController extends BaseMiniAppController {

    private final ClipboardRepository repository;
    private final List<ClipboardEntity> items = new ArrayList<>();
    private ClipboardAdapter adapter;
    private TextView tvEmpty;

    public ClipboardController(Context context, MiniApp.HostCallback hostCallback) {
        super(context, hostCallback);
        this.repository = MultiFloatApp.get().getClipboardRepository();
    }

    @NonNull
    @Override
    public com.example.model.ToolType getToolType() {
        return com.example.model.ToolType.CLIPBOARD;
    }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.miniapp_clipboard, parent, false);
        tvEmpty = view.findViewById(R.id.tv_clip_empty);

        RecyclerView rv = view.findViewById(R.id.rv_clipboard);
        rv.setLayoutManager(new LinearLayoutManager(context));
        adapter = new ClipboardAdapter();
        rv.setAdapter(adapter);

        observe(repository.observeHistory(), entries -> {
            items.clear();
            if (entries != null) items.addAll(entries);
            tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            adapter.notifyDataSetChanged();
        });

        view.findViewById(R.id.btn_clip_capture).setOnClickListener(v -> {
            String current = ClipboardHelper.readCurrentClipboard(context);
            if (current.isEmpty()) {
                Toast.makeText(context, "Clipboard is empty or restricted in background", Toast.LENGTH_SHORT).show();
            } else {
                repository.saveEntryIfNew(current);
                Toast.makeText(context, "Captured clipboard text", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btn_clip_clear).setOnClickListener(v -> repository.clearAll());

        // Auto-capture current clipboard when opened in foreground
        String current = ClipboardHelper.readCurrentClipboard(context);
        if (!current.isEmpty()) {
            repository.saveEntryIfNew(current);
        }

        return view;
    }

    private class ClipboardAdapter extends RecyclerView.Adapter<ClipboardAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_simple_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ClipboardEntity entry = items.get(position);
            holder.title.setText(entry.text);
            holder.subtitle.setText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(new Date(entry.copiedAt)));
            holder.actionBtn.setOnClickListener(v ->
                    ClipboardHelper.copyText(context, "MultiFloat Clip", entry.text, true));
            holder.deleteBtn.setOnClickListener(v -> repository.deleteEntry(entry.id));
            holder.itemView.setOnClickListener(v ->
                    ClipboardHelper.copyText(context, "MultiFloat Clip", entry.text, true));
        }

        @Override
        public int getItemCount() { return items.size(); }

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
