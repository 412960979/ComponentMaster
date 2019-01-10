package com.wn.gradle;

import org.gradle.api.Project;
import org.gradle.api.plugins.PluginContainer;

public class DependenciesUtils {
    private DependenciesUtils(){
    }


    public static String adaptationAptConfigurationName(Project project){
        PluginContainer plugins = project.getPlugins();
        boolean hasKotlin = plugins.hasPlugin("kotlin-android");
        if (hasKotlin) {
            if (!plugins.hasPlugin("kotlin-kapt")) {
                plugins.apply("kotlin-kapt");
            }
        }


        return hasKotlin ? "kapt" : "annotationProcessor";
    }
}
