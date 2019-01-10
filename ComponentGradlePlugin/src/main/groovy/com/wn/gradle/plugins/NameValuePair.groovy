package com.wn.gradle.plugins

import org.gradle.api.artifacts.dsl.DependencyHandler;

class NameValuePair {
    String name;
    Object value;
    Closure closure;

    NameValuePair() {
    }

    NameValuePair(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    @Override
    String toString() {
        return "name:$name value:$value"
    }

    void addToDep(DependencyHandler depHandler) {
        if (closure == null) {
            depHandler.add(name, value)
        } else {
            depHandler.add(name, value, closure)
        }
    }

}