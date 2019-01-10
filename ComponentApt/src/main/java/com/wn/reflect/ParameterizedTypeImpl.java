package com.wn.reflect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class ParameterizedTypeImpl implements ParameterizedType {
    private final Type[] actualTypeArguments;
    private final Class<?> rawType;
    private final Type ownerType;

    private ParameterizedTypeImpl(Class<?> var1, Type[] var2, Type var3) {
        this.actualTypeArguments = var2;
        this.rawType = var1;
        this.ownerType = var3 != null ? var3 : var1.getDeclaringClass();
    }


    public static ParameterizedTypeImpl make(Class<?> var0, Type[] var1, Type var2) {
        return new ParameterizedTypeImpl(var0, var1, var2);
    }

    public Type[] getActualTypeArguments() {
        return (Type[]) this.actualTypeArguments.clone();
    }

    public Class<?> getRawType() {
        return this.rawType;
    }

    public Type getOwnerType() {
        return this.ownerType;
    }
}