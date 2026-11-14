package com.example.miniapps;

import android.content.Context;
import android.media.AudioManager;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import com.example.R;
import com.example.service.MediaSessionMonitorService;
import com.example.util.PermissionHelper;

public class MusicControlsController extends BaseMiniAppController {

    private AudioManager audioManager;
    private ImageButton btnPlayPause;
    private TextView tvTitle, tvArtist, btnEnableListener;
    private boolean assumedPlaying = false;

    public MusicControlsController(Context context, HostCallback hostCallback) {
        super(context, hostCallback);
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.miniapp_music_controls, parent, false);

        tvTitle = view.findViewById(R.id.tv_music_title);
        tvArtist = view.findViewById(R.id.tv_music_artist);
        btnPlayPause = view.findViewById(R.id.btn_media_play_pause);
        btnEnableListener = view.findViewById(R.id.btn_enable_media_listener);

        view.findViewById(R.id.btn_media_prev).setOnClickListener(v ->
                sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS));

        view.findViewById(R.id.btn_media_next).setOnClickListener(v ->
                sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT));

        btnPlayPause.setOnClickListener(v -> {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            assumedPlaying = !assumedPlaying;
            btnPlayPause.setImageResource(assumedPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        });

        view.findViewById(R.id.btn_vol_up).setOnClickListener(v -> {
            if (audioManager != null) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            }
        });

        view.findViewById(R.id.btn_vol_down).setOnClickListener(v -> {
            if (audioManager != null) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            }
        });

        btnEnableListener.setOnClickListener(v ->
                context.startActivity(PermissionHelper.buildNotificationListenerSettingsIntent()));

        observe(MediaSessionMonitorService.observeNowPlaying(), info -> {
            if (info != null) {
                tvTitle.setText(info.title);
                tvArtist.setText(info.artist);
                assumedPlaying = info.isPlaying;
                btnPlayPause.setImageResource(info.isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            }
        });

        if (PermissionHelper.isNotificationListenerEnabled(context)) {
            btnEnableListener.setVisibility(View.GONE);
            MediaSessionMonitorService.refreshActiveSession(context);
        }

        return view;
    }

    private void sendMediaKey(int keyCode) {
        if (audioManager == null) return;
        long eventTime = SystemClock.uptimeMillis();
        audioManager.dispatchMediaKeyEvent(new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0));
        audioManager.dispatchMediaKeyEvent(new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0));
        if (PermissionHelper.isNotificationListenerEnabled(context)) {
            MediaSessionMonitorService.refreshActiveSession(context);
        }
    }
}
