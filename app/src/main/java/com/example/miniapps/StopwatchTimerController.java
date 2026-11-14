package com.example.miniapps;

import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.google.android.material.button.MaterialButtonToggleGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StopwatchTimerController extends BaseMiniAppController {

    private boolean isTimerMode = false;
    private boolean isRunning = false;

    private long swStartElapsed = 0L;
    private long swAccumulatedMs = 0L;

    private long timerDurationMs = 5 * 60 * 1000L;
    private long timerRemainingMs = 5 * 60 * 1000L;
    private long timerEndElapsed = 0L;

    private final List<String> laps = new ArrayList<>();
    private LapAdapter lapAdapter;

    private TextView tvDisplay;
    private TextView tvSub;
    private Button btnStartPause;
    private Button btnResetLap;
    private View panelPresets;
    private EditText etCustomMinutes;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;
            if (!isTimerMode) {
                long elapsed = swAccumulatedMs + (SystemClock.elapsedRealtime() - swStartElapsed);
                tvDisplay.setText(formatMillis(elapsed));
                handler.postDelayed(this, 30);
            } else {
                long left = timerEndElapsed - SystemClock.elapsedRealtime();
                if (left <= 0) {
                    timerRemainingMs = 0;
                    isRunning = false;
                    tvDisplay.setText("00:00.00");
                    tvSub.setText("Time Complete!");
                    btnStartPause.setText("Start");
                    playCompletionTone();
                } else {
                    timerRemainingMs = left;
                    tvDisplay.setText(formatMillis(left));
                    handler.postDelayed(this, 50);
                }
            }
        }
    };

    public StopwatchTimerController(Context context, MiniApp.HostCallback hostCallback) {
        super(context, hostCallback);
    }

    @NonNull
    @Override
    public com.example.model.ToolType getToolType() {
        return com.example.model.ToolType.STOPWATCH_TIMER;
    }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.miniapp_stopwatch_timer, parent, false);

        tvDisplay = view.findViewById(R.id.tv_time_display);
        tvSub = view.findViewById(R.id.tv_time_sub);
        btnStartPause = view.findViewById(R.id.btn_time_start_pause);
        btnResetLap = view.findViewById(R.id.btn_time_reset_lap);
        panelPresets = view.findViewById(R.id.panel_timer_presets);
        etCustomMinutes = view.findViewById(R.id.et_custom_minutes);

        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggle_time_mode);
        toggle.check(R.id.btn_mode_stopwatch);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            pauseClock();
            isTimerMode = (checkedId == R.id.btn_mode_timer);
            panelPresets.setVisibility(isTimerMode ? View.VISIBLE : View.GONE);
            if (isTimerMode) {
                tvDisplay.setText(formatMillis(timerRemainingMs));
                tvSub.setText("Countdown Timer");
                btnResetLap.setText("Reset");
            } else {
                tvDisplay.setText(formatMillis(swAccumulatedMs));
                tvSub.setText("Stopwatch");
                btnResetLap.setText("Reset");
            }
        });

        view.findViewById(R.id.btn_preset_1m).setOnClickListener(v -> setTimerMinutes(1));
        view.findViewById(R.id.btn_preset_5m).setOnClickListener(v -> setTimerMinutes(5));
        view.findViewById(R.id.btn_preset_10m).setOnClickListener(v -> setTimerMinutes(10));

        btnStartPause.setOnClickListener(v -> {
            if (isRunning) {
                pauseClock();
            } else {
                startClock();
            }
        });

        btnResetLap.setOnClickListener(v -> {
            if (!isTimerMode && isRunning) {
                long elapsed = swAccumulatedMs + (SystemClock.elapsedRealtime() - swStartElapsed);
                laps.add(0, "Lap " + (laps.size() + 1) + "  •  " + formatMillis(elapsed));
                lapAdapter.notifyDataSetChanged();
            } else {
                resetClock();
            }
        });

        RecyclerView rvLaps = view.findViewById(R.id.rv_laps);
        rvLaps.setLayoutManager(new LinearLayoutManager(context));
        lapAdapter = new LapAdapter();
        rvLaps.setAdapter(lapAdapter);

        return view;
    }

    private void setTimerMinutes(int minutes) {
        pauseClock();
        timerDurationMs = minutes * 60_000L;
        timerRemainingMs = timerDurationMs;
        tvDisplay.setText(formatMillis(timerRemainingMs));
        tvSub.setText(minutes + " min timer ready");
    }

    private void startClock() {
        if (isTimerMode) {
            String customStr = etCustomMinutes.getText().toString().trim();
            if (!customStr.isEmpty() && timerRemainingMs == timerDurationMs) {
                try {
                    int mins = Integer.parseInt(customStr);
                    if (mins > 0) {
                        timerDurationMs = mins * 60_000L;
                        timerRemainingMs = timerDurationMs;
                    }
                } catch (NumberFormatException ignored) {}
            }
            if (timerRemainingMs <= 0) timerRemainingMs = timerDurationMs;
            timerEndElapsed = SystemClock.elapsedRealtime() + timerRemainingMs;
            tvSub.setText("Counting down...");
            btnResetLap.setText("Reset");
        } else {
            swStartElapsed = SystemClock.elapsedRealtime();
            tvSub.setText("Running");
            btnResetLap.setText("Lap");
        }
        isRunning = true;
        btnStartPause.setText("Pause");
        handler.post(ticker);
    }

    private void pauseClock() {
        if (!isRunning) return;
        isRunning = false;
        handler.removeCallbacks(ticker);
        if (!isTimerMode) {
            swAccumulatedMs += (SystemClock.elapsedRealtime() - swStartElapsed);
            tvSub.setText("Paused");
            btnResetLap.setText("Reset");
        } else {
            timerRemainingMs = Math.max(0L, timerEndElapsed - SystemClock.elapsedRealtime());
            tvSub.setText("Paused");
        }
        btnStartPause.setText("Start");
    }

    private void resetClock() {
        pauseClock();
        if (isTimerMode) {
            timerRemainingMs = timerDurationMs;
            tvDisplay.setText(formatMillis(timerRemainingMs));
            tvSub.setText("Ready");
        } else {
            swAccumulatedMs = 0L;
            laps.clear();
            lapAdapter.notifyDataSetChanged();
            tvDisplay.setText("00:00.00");
            tvSub.setText("Ready");
        }
    }

    private void playCompletionTone() {
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            RingtoneManager.getRingtone(context, uri).play();
        } catch (Exception ignored) {}
    }

    private String formatMillis(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long centis = (ms % 1000) / 10;
        return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centis);
    }

    @Override
    public void onSaveState(@NonNull android.os.Bundle outState) {
        outState.putBoolean("sw_is_timer", isTimerMode);
        long curSw = isRunning && !isTimerMode
                ? swAccumulatedMs + (SystemClock.elapsedRealtime() - swStartElapsed)
                : swAccumulatedMs;
        long curTimer = isRunning && isTimerMode
                ? Math.max(0L, timerEndElapsed - SystemClock.elapsedRealtime())
                : timerRemainingMs;
        outState.putLong("sw_accumulated_ms", curSw);
        outState.putLong("timer_duration_ms", timerDurationMs);
        outState.putLong("timer_remaining_ms", curTimer);
    }

    @Override
    protected void onRestoreState(@NonNull android.os.Bundle savedState) {
        swAccumulatedMs = savedState.getLong("sw_accumulated_ms", 0L);
        timerDurationMs = savedState.getLong("timer_duration_ms", 5 * 60_000L);
        timerRemainingMs = savedState.getLong("timer_remaining_ms", timerDurationMs);
        boolean timer = savedState.getBoolean("sw_is_timer", false);
        isTimerMode = timer;
        if (panelPresets != null) panelPresets.setVisibility(timer ? View.VISIBLE : View.GONE);
        if (tvDisplay != null) {
            tvDisplay.setText(formatMillis(timer ? timerRemainingMs : swAccumulatedMs));
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(ticker);
        super.onDestroy();
    }

    private class LapAdapter extends RecyclerView.Adapter<LapAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(16, 12, 16, 12);
            tv.setTextSize(13f);
            tv.setTextColor(context.getColor(R.color.mf_text_primary));
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ((TextView) holder.itemView).setText(laps.get(position));
        }

        @Override
        public int getItemCount() { return laps.size(); }

        class VH extends RecyclerView.ViewHolder {
            VH(View itemView) { super(itemView); }
        }
    }
}
