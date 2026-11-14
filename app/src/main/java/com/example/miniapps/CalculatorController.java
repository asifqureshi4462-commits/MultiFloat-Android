package com.example.miniapps;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.data.local.entity.CalcHistoryEntity;
import com.example.data.repository.CalculatorRepository;
import com.example.util.ClipboardHelper;
import com.example.util.MathEvaluator;
import com.google.android.material.chip.Chip;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CalculatorController extends BaseMiniAppController {

    private final CalculatorRepository repository;
    private final StringBuilder expression = new StringBuilder();
    private TextView tvExpression;
    private TextView tvResult;
    private String lastResult = "0";
    private final List<CalcHistoryEntity> historyItems = new ArrayList<>();
    private HistoryAdapter historyAdapter;

    public CalculatorController(Context context, HostCallback hostCallback) {
        super(context, hostCallback);
        this.repository = MultiFloatApp.get().getCalculatorRepository();
    }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.miniapp_calculator, parent, false);

        tvExpression = view.findViewById(R.id.tv_calc_expression);
        tvResult = view.findViewById(R.id.tv_calc_result);

        View panelSci = view.findViewById(R.id.panel_sci_keys);
        View panelHistory = view.findViewById(R.id.panel_calc_history);
        View panelKeypad = view.findViewById(R.id.panel_calc_keypad);

        Chip chipSci = view.findViewById(R.id.chip_sci_toggle);
        Chip chipHistory = view.findViewById(R.id.chip_history_toggle);

        chipSci.setOnCheckedChangeListener((buttonView, isChecked) ->
                panelSci.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        chipHistory.setOnCheckedChangeListener((buttonView, isChecked) -> {
            panelHistory.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            panelKeypad.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });

        view.findViewById(R.id.btn_copy_calc).setOnClickListener(v ->
                ClipboardHelper.copyText(context, "Calculator Result", lastResult, true));

        view.findViewById(R.id.btn_clear_calc_history).setOnClickListener(v ->
                repository.clearHistory());

        RecyclerView rvHistory = view.findViewById(R.id.rv_calc_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(context));
        historyAdapter = new HistoryAdapter();
        rvHistory.setAdapter(historyAdapter);

        observe(repository.observeHistory(), items -> {
            historyItems.clear();
            if (items != null) historyItems.addAll(items);
            historyAdapter.notifyDataSetChanged();
        });

        bindAppendButton(view, R.id.btn_0, "0");
        bindAppendButton(view, R.id.btn_1, "1");
        bindAppendButton(view, R.id.btn_2, "2");
        bindAppendButton(view, R.id.btn_3, "3");
        bindAppendButton(view, R.id.btn_4, "4");
        bindAppendButton(view, R.id.btn_5, "5");
        bindAppendButton(view, R.id.btn_6, "6");
        bindAppendButton(view, R.id.btn_7, "7");
        bindAppendButton(view, R.id.btn_8, "8");
        bindAppendButton(view, R.id.btn_9, "9");
        bindAppendButton(view, R.id.btn_dot, ".");
        bindAppendButton(view, R.id.btn_add, "+");
        bindAppendButton(view, R.id.btn_sub, "−");
        bindAppendButton(view, R.id.btn_mul, "×");
        bindAppendButton(view, R.id.btn_div, "÷");
        bindAppendButton(view, R.id.btn_mod, "%");
        bindAppendButton(view, R.id.btn_pow, "^");
        bindAppendButton(view, R.id.btn_lparen, "(");
        bindAppendButton(view, R.id.btn_rparen, ")");
        bindAppendButton(view, R.id.btn_pi, "π");
        bindAppendButton(view, R.id.btn_sin, "sin(");
        bindAppendButton(view, R.id.btn_cos, "cos(");
        bindAppendButton(view, R.id.btn_tan, "tan(");
        bindAppendButton(view, R.id.btn_sqrt, "sqrt(");
        bindAppendButton(view, R.id.btn_log, "log(");
        bindAppendButton(view, R.id.btn_ln, "ln(");

        view.findViewById(R.id.btn_clear).setOnClickListener(v -> {
            expression.setLength(0);
            lastResult = "0";
            tvExpression.setText("0");
            tvResult.setText("0");
        });

        view.findViewById(R.id.btn_del).setOnClickListener(v -> {
            if (expression.length() > 0) {
                expression.deleteCharAt(expression.length() - 1);
                updatePreview();
            }
        });

        view.findViewById(R.id.btn_eq).setOnClickListener(v -> evaluateFinal());

        return view;
    }

    private void bindAppendButton(View root, int id, String token) {
        root.findViewById(id).setOnClickListener(v -> {
            expression.append(token);
            updatePreview();
        });
    }

    private void updatePreview() {
        String expr = expression.toString();
        tvExpression.setText(expr.isEmpty() ? "0" : expr);
        if (expr.isEmpty()) {
            lastResult = "0";
            tvResult.setText("0");
            return;
        }
        try {
            String res = MathEvaluator.evaluateFormatted(expr);
            lastResult = res;
            tvResult.setText(res);
        } catch (Exception ignored) {
            // Incomplete expression while typing
        }
    }

    private void evaluateFinal() {
        String expr = expression.toString();
        if (expr.isEmpty()) return;
        try {
            String res = MathEvaluator.evaluateFormatted(expr);
            repository.addCalculation(expr, res);
            lastResult = res;
            tvResult.setText(res);
            expression.setLength(0);
            expression.append(res);
            tvExpression.setText(expr + " =");
        } catch (Exception e) {
            tvResult.setText("Error");
        }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_simple_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            CalcHistoryEntity item = historyItems.get(position);
            holder.title.setText(item.expression + " = " + item.result);
            holder.subtitle.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(item.createdAt)));
            holder.deleteBtn.setVisibility(View.GONE);
            holder.actionBtn.setOnClickListener(v ->
                    ClipboardHelper.copyText(context, "Calculation", item.result, true));
            holder.itemView.setOnClickListener(v -> {
                expression.setLength(0);
                expression.append(item.result);
                updatePreview();
            });
        }

        @Override
        public int getItemCount() { return historyItems.size(); }

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
