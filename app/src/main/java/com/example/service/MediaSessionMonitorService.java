package com.example.service;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.service.notification.NotificationListenerService;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;

/**
 * Optional NotificationListenerService used solely to read active media session metadata
 * (track title, artist, playback state) when the user explicitly grants Notification Access.
 */
public class MediaSessionMonitorService extends NotificationListenerService {

    public static class NowPlayingInfo {
        public final String title;
        public final String artist;
        public final boolean isPlaying;

        public NowPlayingInfo(String title, String artist, boolean isPlaying) {
            this.title = title;
            this.artist = artist;
            this.isPlaying = isPlaying;
        }
    }

    private static final MutableLiveData<NowPlayingInfo> nowPlayingLive = new MutableLiveData<>();

    public static LiveData<NowPlayingInfo> observeNowPlaying() {
        return nowPlayingLive;
    }

    public static void refreshActiveSession(Context context) {
        try {
            MediaSessionManager msm = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (msm == null) return;
            ComponentName cn = new ComponentName(context, MediaSessionMonitorService.class);
            List<MediaController> controllers = msm.getActiveSessions(cn);
            if (controllers != null && !controllers.isEmpty()) {
                MediaController active = controllers.get(0);
                MediaMetadata meta = active.getMetadata();
                PlaybackState state = active.getPlaybackState();
                String title = meta != null ? meta.getString(MediaMetadata.METADATA_KEY_TITLE) : null;
                String artist = meta != null ? meta.getString(MediaMetadata.METADATA_KEY_ARTIST) : null;
                boolean playing = state != null && state.getState() == PlaybackState.STATE_PLAYING;
                nowPlayingLive.postValue(new NowPlayingInfo(
                        title != null && !title.isEmpty() ? title : "Active Media Session",
                        artist != null ? artist : active.getPackageName(),
                        playing
                ));
            }
        } catch (SecurityException ignored) {
            // Notification access not granted by user; handled gracefully in MusicControlsController
        }
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        refreshActiveSession(this);
    }
}
