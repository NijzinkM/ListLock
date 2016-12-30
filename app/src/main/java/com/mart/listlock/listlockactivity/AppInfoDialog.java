package com.mart.listlock.listlockactivity;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;

import com.mart.listlock.R;

public class AppInfoDialog extends AlertDialog {

    protected AppInfoDialog(Context context) {
        super(context);
        setMessage(Html.fromHtml(context.getString(R.string.about_text)));

    }
}
