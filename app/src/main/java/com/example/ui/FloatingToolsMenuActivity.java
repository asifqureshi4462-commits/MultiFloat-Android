package com.example.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.data.preferences.AppPreferences;
import com.example.model.ToolType;
import com.example.service.FloatingEngineBus;
import com.example.ui.tools.ToolNavigationHelper;
import com.example.util.EdgeToEdgeHelper;
import com.example.util.PermissionHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FloatingToolsMenuActivity extends AppCompatActivity {

    private final List<ToolType> filtered = new ArrayList<>();
    private ToolsAdapter adapter;
    private AppPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tools_menu);
        EdgeToEdgeHelper.setup(this, findViewById(R.id.tools_menu_root));

        preferences = MultiFloatApp.get().getPreferences();
        filtered.addAll(Arrays.asList(ToolType.values()));

        findViewById(R.id.btn_tools_back).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_all_tools);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ToolsAdapter();
        rv.setAdapter(adapter);

        EditText etSearch = findViewById(R.id.et_tools_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().trim().toLowerCase(Locale.US);
                filtered.clear();
                for (ToolType t : ToolType.values()) {
                    String title = getString(t.titleRes).toLowerCase(Locale.US);
                    if (q.isEmpty() || title.contains(q) || t.category.toLowerCase(Locale.US).contains(q)) {
                        filtered.add(t);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private class ToolsAdapter extends RecyclerView.Adapter<ToolsAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tool_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ToolType type = filtered.get(position);
            holder.icon.setImageResource(type.iconRes);
            holder.title.setText(type.titleRes);
            holder.category.setText(type.category + " • Internal Mini-App");

            boolean fav = preferences.isFavorite(type.id);
            holder.favBtn.setImageResource(fav ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);

            holder.favBtn.setOnClickListener(v -> {
                preferences.toggleFavorite(type.id);
                notifyItemChanged(holder.getBindingAdapterPosition());
                FloatingEngineBus.refreshConfig(FloatingToolsMenuActivity.this);
            });

            holder.openBtn.setOnClickListener(v ->
                    ToolNavigationHelper.openFullScreen(FloatingToolsMenuActivity.this, type));

            holder.floatBtn.setOnClickListener(v -> {
                if (PermissionHelper.canDrawOverlays(FloatingToolsMenuActivity.this)) {
                    FloatingEngineBus.openFloatingTool(FloatingToolsMenuActivity.this, type);
                    Toast.makeText(FloatingToolsMenuActivity.this,
                            "Floating " + getString(type.titleRes) + " opened", Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(new Intent(FloatingToolsMenuActivity.this, PermissionSetupActivity.class));
                }
            });
        }

        @Override
        public int getItemCount() { return filtered.size(); }

        class VH extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView title, category;
            final ImageButton favBtn;
            final Button openBtn, floatBtn;
            VH(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.iv_menu_tool_icon);
                title = itemView.findViewById(R.id.tv_menu_tool_title);
                category = itemView.findViewById(R.id.tv_menu_tool_cat);
                favBtn = itemView.findViewById(R.id.btn_menu_fav);
                openBtn = itemView.findViewById(R.id.btn_menu_full);
                floatBtn = itemView.findViewById(R.id.btn_menu_float);
            }
        }
    }
}
