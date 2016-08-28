package com.mart.listlock.playactivity.searchactivity;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableRow;

import com.mart.listlock.playactivity.SongInfoRow;
import com.mart.listlock.playactivity.spotifyobjects.SongInfo;

public abstract class SongInfoResultRow extends SongInfoRow {

    public SongInfoResultRow(SongInfo info, Context context, LinearLayout headerRow) {
        super(info, context, headerRow, false, true);

        super.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SongInfoResultRow.this.onClick(v);
            }
        });
    }

    protected abstract void onClick(View v);
}
