package com.lagradost.cloudstream3.ui.protube;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;

import com.lagradost.cloudstream3.MainActivity;
import com.lagradost.cloudstream3.R;
import com.lagradost.cloudstream3.ui.protube.receivers.NotificationActionReceiver;

import java.net.HttpURLConnection;
import java.net.URL;

public class ForegroundService extends Service {

    private static final String CHANNEL_ID = "YTPRO_CHANNEL";
    private MediaSessionCompat mediaSession;
    private NotificationManager notificationManager;

    private String currentIcon = "";
    private String currentTitle = "";
    private String currentSubtitle = "";
    private long currentDuration = 0;
    private long currentPosition = 0;
    private String currentAction = "play";

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentIcon = intent.getStringExtra("icon");
            currentTitle = intent.getStringExtra("title");
            currentSubtitle = intent.getStringExtra("subtitle");
            currentDuration = intent.getLongExtra("duration", 0);
            currentPosition = intent.getLongExtra("currentPosition", 0);
            currentAction = intent.getStringExtra("action");
            updateNotification();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
        mediaSession = new MediaSessionCompat(this, "YTPRO_SESSION");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateReceiver, new IntentFilter("UPDATE_NOTIFICATION"), Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(updateReceiver, new IntentFilter("UPDATE_NOTIFICATION"));
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "YTPRO Player", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            currentIcon = intent.getStringExtra("icon");
            currentTitle = intent.getStringExtra("title");
            currentSubtitle = intent.getStringExtra("subtitle");
            currentDuration = intent.getLongExtra("duration", 0);
            currentPosition = intent.getLongExtra("currentPosition", 0);
            currentAction = intent.getStringExtra("action");
            updateNotification();
        }
        return START_NOT_STICKY;
    }

    private void updateNotification() {
        new Thread(() -> {
            Bitmap art = null;
            try {
                if (currentIcon != null && !currentIcon.isEmpty()) {
                    URL url = new URL(currentIcon);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    art = BitmapFactory.decodeStream(connection.getInputStream());
                }
            } catch (Exception ignored) {}

            if (art == null) {
                art = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            }

            mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentSubtitle)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentDuration)
                    .build());

            int state = currentAction.equals("play") ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
            if (currentAction.equals("buffer")) state = PlaybackStateCompat.STATE_BUFFERING;

            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(state, currentPosition, 1.0f)
                    .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                            PlaybackStateCompat.ACTION_SEEK_TO)
                    .build());

            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_baseline_play_arrow_24)
                    .setContentTitle(currentTitle)
                    .setContentText(currentSubtitle)
                    .setLargeIcon(art)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSession.getSessionToken())
                            .setShowActionsInCompactView(0, 1, 2))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            builder.addAction(R.drawable.ic_baseline_skip_previous_24, "Previous", getPendingIntent("PREV_ACTION"));
            if (currentAction.equals("play") || currentAction.equals("buffer")) {
                builder.addAction(R.drawable.ic_baseline_pause_24, "Pause", getPendingIntent("PAUSE_ACTION"));
            } else {
                builder.addAction(R.drawable.ic_baseline_play_arrow_24, "Play", getPendingIntent("PLAY_ACTION"));
            }
            builder.addAction(R.drawable.ic_baseline_skip_next_24, "Next", getPendingIntent("NEXT_ACTION"));

            startForeground(1, builder.build());
        }).start();
    }

    private PendingIntent getPendingIntent(String action) {
        Intent intent = new Intent(this, NotificationActionReceiver.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy() ;
        if (mediaSession != null) mediaSession.release();
        try {
            unregisterReceiver(updateReceiver);
        } catch (Exception ignored) {}
    }
}
