package com.example.miniapps;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.data.local.entity.TodoEntity;
import com.example.data.repository.TodoRepository;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

public class TodoController extends BaseMiniAppController {

    private final TodoRepository repository;
    private final List<TodoEntity> todos = new ArrayList<>();
    private TodoAdapter adapter;
    private TextView tvEmpty;
    private String selectedPriority = "Normal";

    public TodoController(Context context, MiniApp.HostCallback hostCallback) {
        super(context, hostCallback);
        this.repository = MultiFloatApp.get().getTodoRepository();
    }

    @NonNull
    @Override
    public com.example.model.ToolType getToolType() {
        return com.example.model.ToolType.TODO;
    }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.miniapp_todo, parent, false);

        EditText etInput = view.findViewById(R.id.et_todo_input);
        Chip chipPriority = view.findViewById(R.id.chip_todo_priority);
        tvEmpty = view.findViewById(R.id.tv_todo_empty);

        chipPriority.setOnClickListener(v -> {
            if ("Normal".equals(selectedPriority)) selectedPriority = "High";
            else if ("High".equals(selectedPriority)) selectedPriority = "Low";
            else selectedPriority = "Normal";
            chipPriority.setText("Priority: " + selectedPriority);
        });

        Runnable addTaskAction = () -> {
            String title = etInput.getText().toString().trim();
            if (!title.isEmpty()) {
                repository.addTodo(title, selectedPriority);
                etInput.setText("");
            }
        };

        view.findViewById(R.id.btn_todo_add).setOnClickListener(v -> addTaskAction.run());
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTaskAction.run();
                return true;
            }
            return false;
        });

        view.findViewById(R.id.btn_todo_clear_done).setOnClickListener(v ->
                repository.clearCompleted());

        RecyclerView rv = view.findViewById(R.id.rv_todos);
        rv.setLayoutManager(new LinearLayoutManager(context));
        adapter = new TodoAdapter();
        rv.setAdapter(adapter);

        observe(repository.observeAllTodos(), list -> {
            todos.clear();
            if (list != null) todos.addAll(list);
            tvEmpty.setVisibility(todos.isEmpty() ? View.VISIBLE : View.GONE);
            adapter.notifyDataSetChanged();
        });

        return view;
    }

    private class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_todo, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            TodoEntity item = todos.get(position);
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(item.completed);
            holder.title.setText(item.title);
            holder.priority.setText(item.priority);
            if (item.completed) {
                holder.title.setPaintFlags(holder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.title.setAlpha(0.55f);
            } else {
                holder.title.setPaintFlags(holder.title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                holder.title.setAlpha(1.0f);
            }
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                    repository.toggleTodo(item));
            holder.deleteBtn.setOnClickListener(v -> repository.deleteTodo(item.id));
        }

        @Override
        public int getItemCount() { return todos.size(); }

        class VH extends RecyclerView.ViewHolder {
            final CheckBox checkBox;
            final TextView title, priority;
            final ImageButton deleteBtn;
            VH(View itemView) {
                super(itemView);
                checkBox = itemView.findViewById(R.id.cb_todo_done);
                title = itemView.findViewById(R.id.tv_todo_title);
                priority = itemView.findViewById(R.id.tv_todo_priority);
                deleteBtn = itemView.findViewById(R.id.btn_todo_delete);
            }
        }
    }
}
