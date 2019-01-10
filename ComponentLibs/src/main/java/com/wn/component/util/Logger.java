package com.wn.component.util;

import android.util.Log;

import com.wn.component.Component;

public class Logger {
    private final static String TAG = "component";

    private static boolean isLogger = Component.debuggable();

    public static void i(String text) {
        if (isLogger) {
            Log.i(TAG, text);
        }
    }

    public static void w(String text) {
        if (isLogger) {
            Log.w(TAG, text);
        }
    }


    public static void e(String text) {
        if (isLogger) {
            Log.w(TAG, text);
        }
    }


    public static void d(String text) {
        if (isLogger) {
            Log.d(TAG, text);
        }
    }
}
