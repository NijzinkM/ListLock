package com.mart.listlock.common;

import android.app.AlertDialog;
import android.util.Log;

import com.mart.listlock.R;

/**
 * Wraps the standard logger
 */
public class LogW {

    private static boolean on = true;

    public static void d(String tag, String msg) {
        if (on) {
            Log.d(tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (on) {
            Log.e(tag, msg, tr);
        }
    }

    public static void e(String tag, String msg) {
        if (on) {
            Log.e(tag, msg);
        }
    }

    public static void turnOn() {
        on = true;
    }

    public static void turnOff() {
        on = false;
    }
}
