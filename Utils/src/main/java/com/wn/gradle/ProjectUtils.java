package com.wn.gradle;

import org.gradle.api.Project;

public class ProjectUtils {
    private ProjectUtils() {
    }

    public static <T> T getExtension(Project project, Class<T> type, String name) {
        T extension = null;
        try {
            extension = type.cast(project.getExtensions().findByName(name));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (extension == null) {
            return null;
        }

        return extension;
    }

    public static <T> T checkExtension(Project project, Class<T> type, String name) {
        T extension = type.cast(project.getExtensions().findByName(name));
        if (extension == null) {
            throw new NullPointerException(name);
        }

        return extension;
    }
}
