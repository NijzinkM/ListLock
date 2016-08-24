package com.mart.listlock.playactivity.spotifyobjects;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableRow;

import com.mart.listlock.playactivity.PlayActivity;
import com.mart.listlock.playactivity.SongInfoRow;

public class SongInfoRemovableRow extends SongInfoRow {

    private PlayActivity parent;

    public SongInfoRemovableRow(SongInfo songInfo, final PlayActivity parent, LinearLayout headers, boolean playing, boolean locked) {
        super(songInfo, parent, headers, playing, locked);
        this.parent = parent;

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.onClickSong(v);
            }
        });
    }
}
