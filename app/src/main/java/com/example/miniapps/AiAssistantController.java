package com.example.miniapps;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.data.local.entity.AiMessageEntity;
import com.example.data.repository.AiChatRepository;
import com.example.util.ClipboardHelper;
import java.util.ArrayList;
import java.util.List;

public class AiAssistantController extends BaseMiniAppController {

    private final AiChatRepository repository;
    private final List<AiMessageEntity> messages = new ArrayList<>();
    private ChatAdapter adapter;

    private TextView tvStatus;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;

    public AiAssistantController(Context context, MiniApp.HostCallback hostCallback) {
        super(context, hostCallback);
        this.repository = MultiFloatApp.get().getAiChatRepository();
    }

    @NonNull
    @Override
    public com.example.model.ToolType getToolType() {
        return com.example.model.ToolType.AI_ASSISTANT;
    }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.miniapp_ai_assistant, parent, false);

        tvStatus = view.findViewById(R.id.tv_ai_key_status);
        tvEmpty = view.findViewById(R.id.tv_ai_empty);
        progressBar = view.findViewById(R.id.pb_ai_loading);
        recyclerView = view.findViewById(R.id.rv_ai_messages);
        EditText etPrompt = view.findViewById(R.id.et_ai_prompt);

        LinearLayoutManager lm = new LinearLayoutManager(context);
        lm.setStackFromEnd(true);
        recyclerView.setLayoutManager(lm);
        adapter = new ChatAdapter();
        recyclerView.setAdapter(adapter);

        refreshKeyStatus();

        observe(repository.observeMessages(), list -> {
            messages.clear();
            if (list != null) messages.addAll(list);
            tvEmpty.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
            adapter.notifyDataSetChanged();
            if (!messages.isEmpty()) {
                recyclerView.scrollToPosition(messages.size() - 1);
            }
        });

        view.findViewById(R.id.btn_ai_config).setOnClickListener(v -> {
            if (hostCallback != null) hostCallback.onOpenSettingsRequested();
        });

        view.findViewById(R.id.btn_ai_clear).setOnClickListener(v -> repository.clearChat());

        Runnable sendAction = () -> {
            String prompt = etPrompt.getText().toString().trim();
            if (prompt.isEmpty()) return;
            etPrompt.setText("");
            progressBar.setVisibility(View.VISIBLE);
            repository.sendPrompt(prompt, (success, msg) -> {
                progressBar.setVisibility(View.GONE);
                refreshKeyStatus();
            });
        };

        view.findViewById(R.id.btn_ai_send).setOnClickListener(v -> sendAction.run());
        etPrompt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendAction.run();
                return true;
            }
            return false;
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshKeyStatus();
    }

    public void refreshKeyStatus() {
        if (tvStatus == null) return;
        if (repository.hasApiKey()) {
            tvStatus.setText("API Key Configured • Ready");
            tvStatus.setTextColor(context.getColor(R.color.mf_success));
        } else {
            tvStatus.setText("API Key Required • Tap API Key");
            tvStatus.setTextColor(context.getColor(R.color.mf_warning));
        }
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            AiMessageEntity msg = messages.get(position);
            holder.bubble.setText(msg.text);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) holder.bubble.getLayoutParams();
            if ("user".equals(msg.role)) {
                lp.gravity = Gravity.END;
                holder.bubble.setBackgroundResource(R.drawable.bg_chat_user);
                holder.bubble.setTextColor(context.getColor(R.color.mf_on_accent));
            } else {
                lp.gravity = Gravity.START;
                holder.bubble.setBackgroundResource(R.drawable.bg_chat_ai);
                holder.bubble.setTextColor("error".equals(msg.role)
                        ? context.getColor(R.color.mf_error)
                        : context.getColor(R.color.mf_text_primary));
            }
            holder.bubble.setLayoutParams(lp);
            holder.bubble.setOnLongClickListener(v -> {
                ClipboardHelper.copyText(context, "AI Message", msg.text, true);
                return true;
            });
        }

        @Override
        public int getItemCount() { return messages.size(); }

        class VH extends RecyclerView.ViewHolder {
            final TextView bubble;
            VH(View itemView) {
                super(itemView);
                bubble = itemView.findViewById(R.id.tv_chat_bubble);
            }
        }
    }
}
