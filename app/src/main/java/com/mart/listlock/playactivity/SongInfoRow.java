package com.mart.listlock.playactivity;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.mart.listlock.R;
import com.mart.listlock.playactivity.spotifyobjects.SongInfo;

public class SongInfoRow extends LinearLayout {

    private static final int UNLOCKED_LINE_WIDTH = 2;
    private static final int UNLOCKED_LINE_PADDING = 8;

    private SongInfo songInfo;

    public SongInfoRow(SongInfo songInfo, final Context context, LinearLayout headers, boolean playing, boolean locked) {
        super(context);

        this.songInfo = songInfo;

        final TextView titleText = new TextView(context);
        final TextView artistText = new TextView(context);
        final TextView durationText = new TextView(context);

        String artistString = songInfo.getArtists().get(0).getInfo().getName();
        for (int i = 1; i < songInfo.getArtists().size(); i++) {
            artistString += ", " + songInfo.getArtists().get(i).getInfo().getName();
        }

        titleText.setText(songInfo.getName());
        artistText.setText(artistString);
        durationText.setText(toTimeString(songInfo.getLength()));

        titleText.setTextAppearance(context, android.R.style.TextAppearance_Medium);
        artistText.setTextAppearance(context, android.R.style.TextAppearance_Medium);
        durationText.setTextAppearance(context, android.R.style.TextAppearance_Medium);

        titleText.setWidth(headers.findViewById(R.id.header_title).getWidth());
        artistText.setWidth(headers.findViewById(R.id.header_artist).getWidth());
        durationText.setWidth(headers.findViewById(R.id.header_duration).getWidth());

        titleText.setEllipsize(TextUtils.TruncateAt.END);
        artistText.setEllipsize(TextUtils.TruncateAt.END);
        durationText.setEllipsize(TextUtils.TruncateAt.END);

        titleText.setMaxLines(1);
        artistText.setMaxLines(1);
        durationText.setMaxLines(1);

        durationText.setGravity(Gravity.END);



        if (locked) {
            addView(titleText);
        } else {
            View line = new View(context);
            line.setLayoutParams(new LayoutParams(UNLOCKED_LINE_WIDTH, headers.getHeight())); // get height off already drawn view
            line.setBackgroundColor(getResources().getColor(R.color.accent));

            LinearLayout firstColumn = new LinearLayout(context);
            firstColumn.setOrientation(LinearLayout.HORIZONTAL);
            firstColumn.addView(line);

            titleText.setWidth(headers.findViewById(R.id.header_title).getWidth() - UNLOCKED_LINE_WIDTH - UNLOCKED_LINE_PADDING);
            titleText.setPadding(UNLOCKED_LINE_PADDING, 0, 0, 0);
            firstColumn.addView(titleText);

            addView(firstColumn);
        }

        addView(artistText);
        addView(durationText);

        int paddingBottom = playing ? 40 : 20;
        setPadding(0, 20, 0, paddingBottom);
    }

    private static String toTimeString(long millis) {
        long second = (millis / 1000) % 60;
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;

        String time = String.format("%02d:%02d", minute, second);

        if (hour > 0)
            time = String.format("%02d:%02d:%02d", hour, minute, second);

        if (hour > 99)
            time = "very long";

        return time;
    }

    public SongInfo getSongInfo() {
        return songInfo;
    }
}
