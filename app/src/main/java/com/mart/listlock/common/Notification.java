package com.mart.listlock.common;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;

import com.mart.listlock.R;
import com.mart.listlock.playactivity.PlayActivity;
import com.mart.listlock.playactivity.spotifyobjects.SongInfo;

public class Notification {

    private static final int NOTIFICATION_ID = 1903;

    private Context context;
    private SongInfo songInfo;
    private boolean showing;
    private boolean playing;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;

    public Notification(Context context) {
        this.context = context;

        builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_notification)
                .setColor(ContextCompat.getColor(context, R.color.accent))
                .setContentTitle(context.getString(R.string.notification_title_paused))
                .setContentText(context.getString(R.string.notification_now_playing))
                .setShowWhen(false);

        Intent resultIntent = new Intent(context, PlayActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        stackBuilder.addParentStack(PlayActivity.class);

        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        builder.setContentIntent(resultPendingIntent);

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void show() {
        notificationManager.notify(NOTIFICATION_ID, builder.build());
        showing = true;
    }

    public void hide() {
        notificationManager.cancel(NOTIFICATION_ID);
        showing = false;
    }

    public void update() {
        builder.setContentText(
                String.format(
                    context.getString(R.string.notification_now_playing),
                    songInfo.getName(),
                    songInfo.getArtists().get(0).getInfo().getName()
                )
            );

        final String title = playing ?
                context.getString(R.string.notification_title_playing) :
                context.getString(R.string.notification_title_paused);
        builder.setContentTitle(title);

        if (showing) {
            show();
        }
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public SongInfo getSongInfo() {
        return songInfo;
    }

    public void setSongInfo(SongInfo songInfo) {
        this.songInfo = songInfo;
    }

    public boolean isShowing() {
        return showing;
    }
}
