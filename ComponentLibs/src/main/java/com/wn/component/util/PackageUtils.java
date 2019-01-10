package com.wn.component.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import static com.wn.component.util.Consts.LAST_VERSION;
import static com.wn.component.util.Consts.SP_COMPONENT_CACHE_KEY;

/**
 * Created by fxlcy on 18-8-10.
 */

public final class PackageUtils {
    private static String NEW_VERSION_NAME;
    private static int NEW_VERSION_CODE;

    private PackageUtils() {
    }

    public static boolean isNewVersion(Context context) {
        PackageInfo packageInfo = getPackageInfo(context);
        if (null != packageInfo) {
            String versionName = packageInfo.versionName;
            int versionCode = packageInfo.versionCode;

            SharedPreferences sp = context.getSharedPreferences(SP_COMPONENT_CACHE_KEY, Context.MODE_PRIVATE);
            if (!versionName.equals(sp.getString(LAST_VERSION, null)) || versionCode != sp.getInt(LAST_VERSION, -1)) {
                // new version
                NEW_VERSION_NAME = versionName;
                NEW_VERSION_CODE = versionCode;

                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    private static PackageInfo getPackageInfo(Context context) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_CONFIGURATIONS);
        } catch (Exception ignored) {
        }

        return packageInfo;
    }
}
