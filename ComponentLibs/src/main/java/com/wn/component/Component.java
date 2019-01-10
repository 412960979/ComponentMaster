package com.wn.component;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import com.alibaba.android.arouter.launcher.ARouter;
import com.wn.component.services.AppServices;

public final class Component {
    private static boolean mIsDebug = false;

    @SuppressLint("StaticFieldLeak")
    private volatile static Component sInstance;

    @SuppressLint("StaticFieldLeak")
    private static Application mContext;

    private static Boolean mHasARouter;


    public Context getContext() {
        return mContext;
    }

    private Component() {
    }

    public static Component getInstance() {
        if (sInstance == null) {
            synchronized (Component.class) {
                if (sInstance == null) {
                    sInstance = new Component();
                    if (mContext == null) {
                        throw new RuntimeException("请先初始化");
                    }
                }
            }
        }

        return sInstance;
    }

    public boolean servicesInjection() {
        return AppServices.getInstance().servicesInjection();
    }

    public synchronized static void init(Context context, boolean debug) {
        mIsDebug = debug;
        if (context instanceof Application) {
            mContext = (Application) context;
        } else {
            mContext = (Application) context.getApplicationContext();
        }


        AppServices.getInstance();

        if (hasARouter()) {
            if (debug) {
                ARouter.openDebug();
                ARouter.openLog();
            }
            ARouter.init(mContext);
        }
    }

    public static boolean debuggable() {
        return mIsDebug;
    }

    private static boolean hasARouter() {
        if (mHasARouter == null) {
            try {
                Class.forName("com.alibaba.android.arouter.launcher.ARouter", false, Thread.currentThread().getContextClassLoader());
                mHasARouter = true;
            } catch (ClassNotFoundException e) {
                mHasARouter = false;
            }
        }

        return mHasARouter;
    }

}
