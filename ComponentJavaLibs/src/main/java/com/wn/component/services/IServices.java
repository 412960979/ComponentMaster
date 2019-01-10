package com.wn.component.services;

public interface IServices {
    <T> T getService(Class<T> type, String id);

    <T> T getService(Class<T> type);
}
