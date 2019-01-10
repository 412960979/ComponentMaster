package com.wn.component.services;

import java.util.HashMap;

public abstract class BaseModuleServices implements IServices {
    private HashMap<Class, ServiceBinder[]> services;

    public BaseModuleServices() {
        services = new HashMap<>();
        init(services);
    }

    protected abstract void init(HashMap<Class, ServiceBinder[]> services);

    @Override
    public final <T> T getService(Class<T> type) {
        return getService(type, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> T getService(Class<T> type, String id) {
        ServiceBinder[] serviceBinders = services.get(type);

        if (serviceBinders == null || serviceBinders.length == 0) {
            return null;
        }

        if (id == null || "".equals(id)) {
            if (serviceBinders.length == 1) {
                return (T) serviceBinders[0].getObj();
            } else {
                throw new RuntimeException("多个services");
            }
        } else {
            for (ServiceBinder binder : serviceBinders) {
                if (id.equals(binder.getFilterId())) {
                    return (T) binder.getObj();
                }
            }

            return null;
        }
    }
}
