package com.example.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.model.FloatingWindowState;
import com.example.service.FloatingEngineBus;
import com.example.service.FloatingWindowService;
import com.example.util.EdgeToEdgeHelper;
import java.util.ArrayList;
import java.util.List;

public class WindowManagerActivity extends AppCompatActivity {

    private final List<FloatingWindowState> states = new ArrayList<>();
    private WindowAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_window_manager);
        EdgeToEdgeHelper.setup(this, findViewById(R.id.win_mgr_root));

        tvEmpty = findViewById(R.id.tv_wm_empty);
        findViewById(R.id.btn_wm_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_wm_restore_session).setOnClickListener(v -> {
            if (!FloatingEngineBus.isRunning()) {
                if (!FloatingEngineBus.startEngine(this)) {
                    Toast.makeText(this, "Grant overlay permission first", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            FloatingEngineBus.sendWindowAction(this, FloatingWindowService.ACTION_RESTORE_SESSION, null);
        });

        findViewById(R.id.btn_wm_minimize_all).setOnClickListener(v ->
                FloatingEngineBus.sendWindowAction(this, FloatingWindowService.ACTION_MINIMIZE_ALL, null));

        findViewById(R.id.btn_wm_close_all).setOnClickListener(v ->
                FloatingEngineBus.sendWindowAction(this, FloatingWindowService.ACTION_CLOSE_ALL, null));

        RecyclerView rv = findViewById(R.id.rv_window_states);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WindowAdapter();
        rv.setAdapter(adapter);

        FloatingEngineBus.observeWindows().observe(this, list -> {
            states.clear();
            if (list != null) states.addAll(list);
            tvEmpty.setVisibility(states.isEmpty() ? View.VISIBLE : View.GONE);
            adapter.notifyDataSetChanged();
        });
    }

    private class WindowAdapter extends RecyclerView.Adapter<WindowAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_window_state, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            FloatingWindowState st = states.get(position);
            holder.icon.setImageResource(st.toolType.iconRes);
            holder.title.setText(st.title);
            String layerTag = "Layer #" + (st.zIndex + 1);
            holder.status.setText(st.minimized
                    ? "Minimized"
                    : (st.focused ? layerTag + " • Front Active" : layerTag + " • Background"));
            holder.focusBtn.setText(st.minimized ? "Restore" : "Bring Front");

            holder.focusBtn.setOnClickListener(v ->
                    FloatingEngineBus.sendWindowAction(WindowManagerActivity.this,
                            FloatingWindowService.ACTION_RESTORE_WINDOW, st.windowId));

            holder.minBtn.setVisibility(st.minimized ? View.GONE : View.VISIBLE);
            holder.minBtn.setOnClickListener(v ->
                    FloatingEngineBus.sendWindowAction(WindowManagerActivity.this,
                            FloatingWindowService.ACTION_MINIMIZE_WINDOW, st.windowId));

            holder.closeBtn.setOnClickListener(v ->
                    FloatingEngineBus.sendWindowAction(WindowManagerActivity.this,
                            FloatingWindowService.ACTION_CLOSE_WINDOW, st.windowId));
        }

        @Override
        public int getItemCount() { return states.size(); }

        class VH extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView title, status;
            final Button focusBtn;
            final ImageButton minBtn, closeBtn;
            VH(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.iv_ws_icon);
                title = itemView.findViewById(R.id.tv_ws_title);
                status = itemView.findViewById(R.id.tv_ws_state);
                focusBtn = itemView.findViewById(R.id.btn_ws_focus_restore);
                minBtn = itemView.findViewById(R.id.btn_ws_minimize);
                closeBtn = itemView.findViewById(R.id.btn_ws_close);
            }
        }
    }
}
