package com.wn.gradle.plugins

import com.android.build.gradle.AppExtension
import org.gradle.api.Action
import org.gradle.api.Project
import com.wn.gradle.util.DependenciesUtils

class ComponentExtension {


    private final Dependencies dependencies = new Dependencies()

    private final Dependencies runAloneDependencies = new Dependencies()

    private final Project project

    private final static String RUNALONE_DEP_PREFIX = "runAlone"


    final ARouterVersions versions = new ARouterVersions()


    void setApplicationId(String appId) {
        def android = project.android
        if (android instanceof AppExtension) {
            android.defaultConfig.setApplicationId(appId)
        }
    }


    private String aptConfigurationName

    private String getAptConfigurationName() {
        if (aptConfigurationName == null) {
            aptConfigurationName = DependenciesUtils.adaptationAptConfigurationName(project)
        }

        return aptConfigurationName
    }

    void aRouterVersions(Action<ARouterVersions> versionsAction) {
        final String compilerVersion = versions.compilerVersion
        final String apiVersion = versions.apiVersion

        versionsAction.execute(versions)

        if (compilerVersion != versions.compilerVersion) {
            project.dependencies.add(getAptConfigurationName(), "com.alibaba:arouter-compiler:" + versions.compilerVersion)
        }

        if (apiVersion != versions.apiVersion) {
            project.dependencies.add("implementation", 'com.alibaba:arouter-api:' + versions.apiVersion)
        }
    }


    ComponentExtension(Project project) {
        this.project = project
    }

    protected final compilerEffective() {
        project.dependencies.add(getAptConfigurationName(), "com.alibaba:arouter-compiler:" + versions.compilerVersion)
        project.dependencies.add("implementation", 'com.alibaba:arouter-api:' + versions.apiVersion)
    }

    void dependencies(Closure configureClosure) {
        configureClosure.delegate = project
        configureClosure()
    }

    protected Dependencies getRunAloneDependencies() {
        return runAloneDependencies
    }

    protected Dependencies getDependencies() {
        return dependencies
    }


    def methodMissing(String name, args) {
        if (args == null || args.length < 1 || args.length > 2) {
            throw UnsupportedOperationException(name)
        }

        if (name.startsWith(RUNALONE_DEP_PREFIX)) {
            def n = name.substring(RUNALONE_DEP_PREFIX.length())
            if (n.isEmpty()) {
                throw UnsupportedOperationException(name)
            }

            n = String.valueOf(n.charAt(0).toLowerCase()) + n.substring(1)
            addDep(true, n, args)
        } else {
            addDep(false, name, args)
        }
    }


    def addDep(boolean isRunAlone, String name, args) {
        if (name == null || name.isEmpty()) {
            throw UnsupportedOperationException(name)
        }

        def nvp = new NameValuePair(name, args[0])
        if (args.length == 2 && args[1] instanceof Closure) {
            nvp.closure = args[1] as Closure
        }

        if (isRunAlone) {
            runAloneDependencies.add(nvp)
        } else {
            dependencies.add(nvp)
        }
    }
}


