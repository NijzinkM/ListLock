package com.mart.listlock.listlockactivity;

import com.mart.listlock.common.LogW;
import com.mart.listlock.common.UserInfo;
import com.mart.listlock.playactivity.MusicService;
import com.mart.listlock.request.SpotifyWebRequest;
import com.mart.listlock.request.SpotifyWebRequestException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AccessTokenUpdater {

    private static final String LOG_TAG = AccessTokenUpdater.class.getName();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final TimeUnit timeUnit = TimeUnit.SECONDS;

    private static boolean started = false;

    private static ScheduledFuture<?> handle;

    public static void start() {
        if (started) {
            throw new IllegalStateException("access token updater already started");
        }

        started = true;
        final int delay = getUpdateIn(UserInfo.getExpiresIn());
        LogW.d(LOG_TAG, "access token updater started with delay: " + delay + " " + timeUnit.name());

        handle = scheduler.scheduleAtFixedRate(updatingRunnable(), delay, delay, timeUnit);
    }

    public static void stop() {
        LogW.d(LOG_TAG, "access token updater stopped");
        handle.cancel(false);
        started = false;
    }

    private static Runnable updatingRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                LogW.d(LOG_TAG, "automatic access token update started");
                try {
                    SpotifyWebRequest.refreshAccessToken(UserInfo.getRefreshToken());
                    if (MusicService.player() != null) {
                        MusicService.player().login(UserInfo.getAccessToken());
                    }
                } catch (SpotifyWebRequestException e) {
                    LogW.e(LOG_TAG, "could not retrieve new access token");
                }
                LogW.d(LOG_TAG, "automatic access token update finished");
            }
        };
    }

    private static int getUpdateIn(int expiresIn) {
        return expiresIn - 100;
    }
}
