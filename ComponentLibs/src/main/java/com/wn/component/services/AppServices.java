package com.wn.component.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.wn.component.Component;
import com.wn.component.util.ClassUtils;
import com.wn.component.util.Consts;
import com.wn.component.util.Logger;
import com.wn.component.util.PackageUtils;

public final class AppServices implements IServices {
    private final List<IServices> mServices = new Vector<>();
    private static volatile AppServices sInstance;
    private boolean mServicesInjection = false;
    private Object mCurrentService = null;
    private String mCurrentServiceId = null;
    private final HashMap<String, Object> mCache = new HashMap<>();

    private AppServices() {
        init();
    }


    private void init() {
        Context context = Component.getInstance().getContext();
        SharedPreferences sp = context.getSharedPreferences(Consts.SP_COMPONENT_CACHE_KEY, Context.MODE_PRIVATE);
        Set<String> set = null;
        //如果不是debug模式并且不是新版本
        if (!Component.debuggable() && !PackageUtils.isNewVersion(context)) {
            set = sp.getStringSet(Consts.COMPONENT_SERVICES_MAP_KEY, null);
        }

        if (set == null) {
            try {
                set = ClassUtils.getFileNameByPackageName(context,
                        Consts.SERVICES_PACKAGE);
                sp.edit().putStringSet(Consts.COMPONENT_SERVICES_MAP_KEY, set)
                        .apply();
            } catch (Throwable e) {
                Logger.e(e.toString());
            }
        }


        if (set != null) {
            try {
                for (String className : set) {
                    Class<?> clazz = Class.forName(className);
                    if (IServices.class.isAssignableFrom(clazz)) {
                        Method method = clazz.getMethod("getInstance");
                        mServices.add((IServices) method.invoke(null));
                    }
                }
            } catch (Throwable e) {
                Logger.e(e.toString());
            }
        }
    }

    public boolean servicesInjection() {
        return mServicesInjection;
    }

    public static AppServices getInstance() {
        if (sInstance == null) {
            synchronized (AppServices.class) {
                if (sInstance == null) {
                    sInstance = new AppServices();
                }
            }
        }


        return sInstance;
    }


    public void addServices(final IServices services) {
        mServices.add(services);
    }

    public void removeServices(final IServices services) {
        mServices.remove(services);
    }


    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T> T getService(@NonNull Class<T> type, @Nullable String id) {

        if (mCurrentService != null && type == mCurrentService.getClass()
                && equals(mCurrentServiceId, id)) {
            return (T) mCurrentService;
        }

        final String key = type.getName() + (id == null ? "" : id);
        T finder = (T) mCache.get(key);

        if (finder == null) {
            for (IServices services : mServices) {
                Object service = services.getService(type, id);
                if (service != null) {
                    mCache.put(key, service);
                    finder = (T) service;
                    break;
                }
            }
        }

        if (finder != null) {
            mCurrentService = finder;
            mCurrentServiceId = id;
        }

        return finder;
    }

    public <T> void withServiceExecute(@NonNull Class<T> type, @Nullable String id, @NonNull Executor<T> executor) {
        final T service = getService(type, id);
        if (service == null) {
            if (executor instanceof ExecutorExpansion) {
                ((ExecutorExpansion<T>) executor).onNotFound(type, id);
            }
        } else {
            executor.onExecute(service);
        }
    }


    public <T> void withServiceExecute(@NonNull Class<T> type, @NonNull Executor<T> executor) {
        withServiceExecute(type, null, executor);
    }

    @Override
    public <T> T getService(Class<T> type) {
        return getService(type, null);
    }

    private static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    public interface Executor<T> {
        void onExecute(@NonNull T service);
    }

    public interface ExecutorExpansion<T> extends com.wn.component.services.AppServices.Executor<T> {
        void onNotFound(@NonNull Class<T> type, @Nullable String id);
    }
}
