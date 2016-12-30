package com.mart.listlock.playactivity.playlistactivity;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.mart.listlock.common.LogW;
import com.mart.listlock.playactivity.PlayActivity;
import com.mart.listlock.R;
import com.mart.listlock.common.Utils;
import com.mart.listlock.common.UserInfo;
import com.mart.listlock.playactivity.spotifyobjects.PlaylistInfo;
import com.mart.listlock.request.SpotifyWebRequest;
import com.mart.listlock.request.SpotifyWebRequestException;

import java.util.ArrayList;
import java.util.List;

public class PlaylistActivity extends AppCompatActivity {

    private static final String LOG_TAG = PlaylistActivity.class.getName();

    private TextView title;
    private TableLayout table;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);
        setFinishOnTouchOutside(true);

        title = (TextView) findViewById(R.id.playlist_activity_title);
        table = (TableLayout) findViewById(R.id.playlist_results_table);

        title.setText(getString(R.string.playlists_for_name, UserInfo.getDisplayName()));

        List<PlaylistInfo> playLists = new ArrayList<>();

        try {
            playLists.addAll(SpotifyWebRequest.requestPlaylists(UserInfo.getId(), UserInfo.getAccessToken()));

            if (playLists.isEmpty()) {
                LogW.d(LOG_TAG, "no playlists found");
                Utils.showTextBriefly(getString(R.string.no_playlists_found), this);
                finish();
            }
            for (PlaylistInfo playlist : playLists) {
                LogW.d(LOG_TAG, "playlist found: " + playlist.getName());
                TableRow tableRow = new PlaylistTableRow(this, playlist);
                table.addView(tableRow);
            }
        } catch (SpotifyWebRequestException e) {
            LogW.e(LOG_TAG, "failed to request playlists", e);
            Utils.showTextBriefly(getString(R.string.retrieve_playlists_failed), this);
            finish();
        }

    }

    private class PlaylistTableRow extends TableRow {

        public PlaylistTableRow(Context context, final PlaylistInfo playlistInfo) {
            super(context);

            TextView name = new TextView(PlaylistActivity.this);
            name.setText(playlistInfo.getName());
            name.setTextAppearance(context, android.R.style.TextAppearance_Medium);
            name.setEllipsize(TextUtils.TruncateAt.END);
            name.setMaxLines(1);

            addView(name);

            setPadding(0, 20, 0 ,20);

            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    LogW.d(LOG_TAG, "playlist '" + playlistInfo.getName() + "' clicked");
                    Intent data = new Intent();
                    data.putExtra(PlayActivity.KEY_PLAYLIST_ID, playlistInfo.getId());
                    setResult(PlayActivity.PLAYLIST_REQUEST_CODE, data);
                    finish();
                }
            });
        }
    }

}
