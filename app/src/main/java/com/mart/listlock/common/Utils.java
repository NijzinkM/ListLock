package com.mart.listlock.common;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.media.MediaBrowserCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.mart.listlock.R;
import com.mart.listlock.listlockactivity.ListLockActivity;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class Utils {

    public static final String KEY_PIN = "pin";
    private static final String LOG_TAG = Utils.class.getName();

    public static void showTextBriefly(final String text, final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(activity, text, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    public static void showTextProlonged(final String text, final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(activity, text, Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    public static void doWhenAuthorized(final Activity activity, final Action onCorrect, final LinearLayout adminModeBanner, final boolean forcePIN) {
        if (ListLockActivity.inAdminMode() && forcePIN == false) {
            LogW.d(LOG_TAG, "in admin mode, executing Action");
            onCorrect.execute();
        } else {
            LogW.d(LOG_TAG, forcePIN ? "forcePIN is true; asking for PIN" : "not in admin mode; asking for PIN");
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(forcePIN ? R.string.pin_forced : R.string.pin_required);

            final EditText input = new EditText(activity);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setTransformationMethod(PasswordTransformationMethod.getInstance());
            input.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    final String inputText = input.getText().toString();
                    if (inputText.length() > 4) {
                        input.setText(inputText.substring(0, inputText.length() - 1));
                        input.setSelection(4);
                    }
                }
            });

            builder.setView(input);

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final String inputText = input.getText().toString();

                    SharedPreferences settings = activity.getSharedPreferences(activity.getString(R.string.app_name), 0);

                    final String savedPin = settings.getString(KEY_PIN, "0000");

                    if (savedPin.equals(inputText)) {
                        setAuthorized(adminModeBanner);
                        onCorrect.execute();
                    } else {
                        Utils.showTextBriefly(activity.getString(R.string.invalid_pin), activity);
                    }
                }
            });

            AlertDialog dialog = builder.create();

            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

            dialog.show();
        }
    }

    public static void setAuthorized(final LinearLayout adminModeBanner) {
        adminModeBanner.setVisibility(View.VISIBLE);
        ListLockActivity.setAdminMode(true);
    }

    public static void setUnauthorized(final LinearLayout adminModeBanner, final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.leave_admin_mode);
        builder.setNegativeButton(context.getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(context.getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                adminModeBanner.setVisibility(View.GONE);
                ListLockActivity.setAdminMode(false);
            }
        });
        builder.create().show();
    }

    public static String decodeSecretKey(String clientID, String clientSecret) {
        String decoded = Base64.encodeToString((clientID + ":" + clientSecret).getBytes(Charset.forName("UTF-8")), Base64.NO_WRAP);
        return decoded;
    }

    public static abstract class Action {
        public abstract void execute();
    }

    public static Typeface createFont(Context context, String fontFile) {
        return Typeface.createFromAsset(context.getAssets(), fontFile);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public static void doWhileLoading(final Action action, final Action onFinished, final Context context) {
        final ProgressDialog dialog = new ProgressDialog(context);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage(context.getString(R.string.loading));
        dialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                action.execute();
                if (onFinished != null) {
                    onFinished.execute();
                }
                dialog.dismiss();
            }
        }).start();
    }

    public static void doWhileLoading(final Action action, final Context context) {
        doWhileLoading(action, null, context);
    }
}
