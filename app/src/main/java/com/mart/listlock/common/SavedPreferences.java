package com.mart.listlock.common;

import android.content.Context;
import android.content.SharedPreferences;

import com.mart.listlock.playactivity.spotifyobjects.SongInfo;
import com.mart.listlock.playactivity.spotifyobjects.SpotifySong;
import com.mart.listlock.request.SpotifyWebRequestException;

import java.util.ArrayList;
import java.util.List;

public class SavedPreferences {

    private static final String PREF_ACCESS_TOKEN = "accesstoken";
    private static final String PREF_REFRESH_TOKEN = "refreshtoken";
    private static final String PREF_SIZE = "size";
    public static final String PREF_SONG = "song";
    public static final String PREF_SONG_LOCKED = "song_locked";
    private static final String PREF_KEYWORD = "keyword";
    private static final String PREF_SEARCHED = "searched";
    private static final String TOKEN_PREFS = "tokens";
    private static final String SONGS_PREFS = "songs";

    public static void setAccessToken(Context context, String accessToken) {
        SharedPreferences.Editor editor = getTokenPrefs(context).edit();
        editor.putString(PREF_ACCESS_TOKEN, accessToken);
        editor.commit();
    }

    public static String getAccessToken(Context context) {
        return getTokenPrefs(context).getString(PREF_ACCESS_TOKEN, null);
    }

    public static void setRefreshToken(Context context, String refreshToken) {
        SharedPreferences.Editor editor = getTokenPrefs(context).edit();
        editor.putString(PREF_REFRESH_TOKEN, refreshToken);
        editor.commit();
    }

    public static String getRefreshToken(Context context) {
        return getTokenPrefs(context).getString(PREF_REFRESH_TOKEN, null);
    }

    public static void setSongs(List<SpotifySong> songs, Context context) {
        SharedPreferences settings = getSongsPrefs(context);
        SharedPreferences.Editor editor = settings.edit();

        final int size = songs.size();

        editor.putInt(PREF_SIZE, size);

        for (int i = 0; i < size; i++) {
            final String uri = songs.get(i).getURI();
            final boolean locked = songs.get(i).isLocked();
            editor.putString(PREF_SONG + i, uri);
            editor.putBoolean(PREF_SONG_LOCKED + i, locked);
        }

        editor.commit();
    }

    public static List<SpotifySong> getSongs(Context context) throws SpotifyWebRequestException {
        final List<SpotifySong> songs = new ArrayList<>();
        final SharedPreferences songsPrefs = getSongsPrefs(context);

        for (int i = 0; i < songsPrefs.getInt(PREF_SIZE, 0); i++) {
            SpotifySong song = new SpotifySong(songsPrefs.getString(PREF_SONG + i, null));
            song.setLocked(songsPrefs.getBoolean(PREF_SONG_LOCKED + i, true));
            songs.add(song);
        }

        return songs;
    }

    public static void setResults(String keyword, boolean searched, Context context) {
        SharedPreferences resultPrefs = getSongsPrefs(context);
        SharedPreferences.Editor editor = resultPrefs.edit();

        editor.putString(PREF_KEYWORD, keyword);
        editor.putBoolean(PREF_SEARCHED, searched);

        editor.commit();
    }

    public static String getKeyword(Context context) {
        return getSongsPrefs(context).getString(PREF_KEYWORD, null);
    }

    public static boolean getSearched(Context context) {
        return getSongsPrefs(context).getBoolean(PREF_SEARCHED, false);
    }

    private static SharedPreferences getTokenPrefs(Context context) {
        return context.getSharedPreferences(TOKEN_PREFS, Context.MODE_PRIVATE);
    }

    private static SharedPreferences getSongsPrefs(Context context) {
        return context.getSharedPreferences(SONGS_PREFS, Context.MODE_PRIVATE);
    }

    public static void clearTokenPrefs(Context context) {
        SharedPreferences.Editor editor = getTokenPrefs(context).edit();
        editor.clear();
        editor.commit();
    }
}
