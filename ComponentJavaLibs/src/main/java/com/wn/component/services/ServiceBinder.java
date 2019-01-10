package com.wn.component.services;

import com.wn.component.util.Supplier;

public final class ServiceBinder {

    private String id;
    private Supplier supplier;
    private Object obj;

    public ServiceBinder(String id, Supplier supplier) {
        this.id = id;
        this.supplier = supplier;
    }


    public String getFilterId() {
        return id;
    }


    public Object getObj() {
        if (obj == null) {
            obj = supplier.get();
        }

        return obj;
    }
}
