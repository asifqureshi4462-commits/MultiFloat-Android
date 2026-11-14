package com.example.miniapps;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.data.preferences.AppPreferences;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ClockController extends BaseMiniAppController {

    private final AppPreferences preferences;
    private TextView tvTime, tvAmPm, tvDate, tvTz;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable clockTicker = new Runnable() {
        @Override
        public void run() {
            updateClock();
            handler.postDelayed(this, 1000);
        }
    };

    public ClockController(Context context, MiniApp.HostCallback hostCallback) {
        super(context, hostCallback);
        this.preferences = MultiFloatApp.get().getPreferences();
    }

    @androidx.annotation.NonNull
    @Override
    public com.example.model.ToolType getToolType() {
        return com.example.model.ToolType.CLOCK;
    }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.miniapp_clock, parent, false);

        tvTime = view.findViewById(R.id.tv_clock_time);
        tvAmPm = view.findViewById(R.id.tv_clock_ampm);
        tvDate = view.findViewById(R.id.tv_clock_date);
        tvTz = view.findViewById(R.id.tv_clock_tz);
        MaterialSwitch switch24h = view.findViewById(R.id.switch_clock_24h);

        switch24h.setChecked(preferences.isClock24Hour());
        switch24h.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.setClock24Hour(isChecked);
            updateClock();
        });

        updateClock();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.removeCallbacks(clockTicker);
        handler.post(clockTicker);
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(clockTicker);
        super.onPause();
    }

    private void updateClock() {
        Date now = new Date();
        boolean is24 = preferences.isClock24Hour();
        SimpleDateFormat timeFmt = new SimpleDateFormat(is24 ? "HH:mm:ss" : "hh:mm:ss", Locale.getDefault());
        tvTime.setText(timeFmt.format(now));
        if (is24) {
            tvAmPm.setVisibility(View.GONE);
        } else {
            tvAmPm.setVisibility(View.VISIBLE);
            tvAmPm.setText(new SimpleDateFormat("a", Locale.getDefault()).format(now));
        }
        tvDate.setText(new SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(now));
        tvTz.setText(TimeZone.getDefault().getDisplayName());
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(clockTicker);
        super.onDestroy();
    }
}
