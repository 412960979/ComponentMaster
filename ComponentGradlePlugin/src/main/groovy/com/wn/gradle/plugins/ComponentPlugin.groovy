package com.wn.gradle.plugins


import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.wn.gradle.util.DependenciesUtils
import java.util.function.BiConsumer
import java.util.function.Consumer

class ComponentPlugin implements Plugin<Project> {
    //主模块名
    private String mainModule = "app"

    private final static String PROP_IS_RUN_ALONE = "isRunAlone"
    private final static String PROP_GLOBLE_IS_RUN_ALONE = "globalIsRunAlone"

    private final static String PROP_MAIN_MODULE = "mainModule"

    private final static String PROP_IS_MAIN_MODULE = "isMainModule"


    private final static String PROP_RUN_ALONE_DIR = "runAloneDir"

    private final static String PROP_KOTLIN_SUPPORT = "kotlinSupport"

    //是否注入字节码
    private final static String INJECTION = "injection"

    private final static String LIB_VERSION = "1.0.0"

    @Override
    void apply(Project project) {


        project.extensions.create("component", ComponentExtension, project)

        final props = project.properties

        final String moduleName = project.name

        boolean isMainModule

        if (project.hasProperty(PROP_MAIN_MODULE)) {
            mainModule = Boolean.parseBoolean(props.get(PROP_MAIN_MODULE))
        }

        if (project.hasProperty(PROP_IS_MAIN_MODULE)) {
            isMainModule = Boolean.parseBoolean(project.properties.get(PROP_IS_MAIN_MODULE))
        } else {
            isMainModule = mainModule == moduleName
        }

        //是否可以独立运行
        boolean isRunAlone

        //如果是主模块不需要配置isRunAlone
        if (!isMainModule) {
            if (!project.hasProperty(PROP_IS_RUN_ALONE)) {
                throw new RuntimeException("you should set isRunAlone in " + moduleName + "'s gradle.properties")
            }

            String globalIsRunAloneStr = props.get(PROP_GLOBLE_IS_RUN_ALONE)
            if (globalIsRunAloneStr == null || globalIsRunAloneStr.isEmpty()) {
                isRunAlone = Boolean.parseBoolean(props.get(PROP_IS_RUN_ALONE))
            } else {
                isRunAlone = Boolean.parseBoolean(globalIsRunAloneStr)
            }
        } else {
            isRunAlone = true
        }


        AssembleTask assembleTask = getTaskInfo(project)

        if (assembleTask.isAssemble) {
            project.afterEvaluate {
                ComponentExtension ext = ext(project)

/*                if (ext.componentServicesName != null
                        && ext.componentServicesName.trim() != "") {
                    (project.android as BaseExtension)
                            .defaultConfig.javaCompileOptions
                            .annotationProcessorOptions.argument("__component_services_class_name__", ext.componentServicesName)
                }
                if (ext.componentServicesPackage != null
                        && ext.componentServicesPackage.trim() != "") {
                    (project.android as BaseExtension)
                            .defaultConfig.javaCompileOptions
                            .annotationProcessorOptions.argument("__component_services_package__", ext.componentServicesName)
                }*/


                addDep(ext.dependencies, project)
            }
        }




        props.forEach(new BiConsumer<String, Object>() {
            @Override
            void accept(String s, Object o) {
                println "props---------" + s + "========" + o.toString()
            }
        })

        final boolean kotlinSupport = getBoolean(props, PROP_KOTLIN_SUPPORT, true)
        final boolean injection = getBoolean(props, INJECTION, false)

        println "injection:$injection =================="
        println "kotlinSupport:$kotlinSupport =================="

        if (isRunAlone) {
            project.apply plugin: 'com.android.application'

            BaseExtension appExt = project.android as BaseExtension
            if (injection) {
                println "apply AutoInjectServicesTransform"
                appExt.registerTransform(new AutoInjectServicesTransform(project))
/*                try {
                    project.apply plugin: 'com.alibaba.arouter'
                    println "apply 'com.alibaba.arouter'"
                } catch (Throwable e) {
                    e.printStackTrace()
                }*/
            }


            project.afterEvaluate {
                addDep(ext(project).runAloneDependencies, project)
            }
        } else {
            project.apply plugin: 'com.android.library'
        }

        if (kotlinSupport) {
            project.apply plugin: 'kotlin-android'
        }



        ext(project).compilerEffective()

        project.dependencies.add("implementation", 'com.wn.component:ComponentLibs:' + LIB_VERSION)
        project.dependencies.add(DependenciesUtils.adaptationAptConfigurationName(project)
                , 'com.wn.component:ComponentApt:' + LIB_VERSION)


        BaseExtension appExt = project.android as BaseExtension
        String runAloneDir = props.get(PROP_RUN_ALONE_DIR, "runAlone")


        DomainObjectCollection<BaseVariant> variants = null

        if (appExt instanceof AppExtension) {
            variants = appExt.applicationVariants
        } else if (appExt instanceof LibraryExtension) {
            variants = appExt.libraryVariants
        }

        List<String> variantsStr = new ArrayList<>()
        if (variants != null) {
            for (BaseVariant variant in variants) {
                variantsStr.add(variant.name)
            }
        }

        //是否要设置runAlone文件夹
        def setAlone = isRunAlone && !isMainModule

        appExt.sourceSets.forEach(new Consumer<AndroidSourceSet>() {
            @Override
            void accept(AndroidSourceSet sourceSet) {
                final String flavorName = sourceSet.name

                if (!variantsStr.contains(sourceSet.name) && flavorName != "main") {
                    return
                }

                settingSourceSet(sourceSet, runAloneDir, flavorName, kotlinSupport
                        , setAlone)
            }
        })


        if (variants != null) {
            variants.all {
                final String name = it.flavorName
                final String flavorName = (name == null || name.isEmpty()) ? "main" : name

                settingSourceSet(appExt.sourceSets.getByName(flavorName), runAloneDir, flavorName, kotlinSupport
                        , setAlone)
            }
        }


        appExt.defaultConfig {
            javaCompileOptions {
                annotationProcessorOptions {
                    arguments = [AROUTER_MODULE_NAME: project.getName(), moduleName: project.getName()]
                }
            }
        }

        if (!injection) {
            project.afterEvaluate {
                appExt.buildTypes.all {
                    if (it.minifyEnabled) {
                        def proDir = project.file("build/tmp/proguardFiles/${it.name}")
                        if (!proDir.exists()) {
                            proDir.mkdirs()
                        }
                        def proFile = new File(proDir, "proguard-rules-component.pro")
                        if (!proFile.exists()) {
                            proFile.createNewFile()
                        }
                        proFile.write("-keep class com.wn.component.services.gen.**{\n" +
                                "    *;\n" +
                                "}\n" +
                                "-keep class com.alibaba.android.arouter.routes.**{\n" +
                                "    *;\n" +
                                "}")
                        it.proguardFile(proFile)
                    }
                }
            }
        }
    }

    private static void addDep(Dependencies dependencies, Project project) {
        println "dep add pro size " + dependencies.size()

        dependencies.forEach { dep ->
            println "dep class " + dep.value.toString()

            if (dep.value instanceof Project) {

                def p = (dep.value as Project)

                def isApp = isApplication(p)

                println project.name + " isApp " + isApp

                if (!isApp) {
                    dep.addToDep(project.dependencies)
                }

                return
            }

            dep.addToDep(project.dependencies)
        }
    }

    private static boolean getBoolean(Map<String, ?> props, String key, boolean defValue) {
        Object obj = props.get(key)
        if (obj == null) {
            return defValue
        } else {
            return Boolean.parseBoolean(obj.toString())
        }
    }

    private static boolean isApplication(Project project) {
/*        BaseExtension ext
        try {
            ext = project.android as BaseExtension
        } catch (MissingPropertyException e) {
            e.printStackTrace()

            return false
        }


        if (ext == null) {
            return false
        } else {
            if (ext instanceof AppExtension) {
                return true
            }

            return false
        }*/

//        def gradleFile = project.file("gradle.properties")

        def isRunAlone
        def props = project.properties
        String globalIsRunAloneStr = props.get(PROP_GLOBLE_IS_RUN_ALONE)
        if (globalIsRunAloneStr == null || globalIsRunAloneStr.isEmpty()) {
            isRunAlone = Boolean.parseBoolean(props.get(PROP_IS_RUN_ALONE))
        } else {
            isRunAlone = Boolean.parseBoolean(globalIsRunAloneStr)
        }

        return isRunAlone
    }

    private static ComponentExtension ext(Project project) {
        return project.component as ComponentExtension
    }


    private static void settingSourceSet(AndroidSourceSet sourceSet
                                         , String runAloneDir, String flavorName
                                         , boolean kotlinSupport, boolean setAlone) {

        if (setAlone) {
            String runAloneAbsDir = "src/$flavorName/$runAloneDir"

            sourceSet.manifest.srcFile(new File(runAloneAbsDir + "/AndroidManifest.xml"))
            sourceSet.java.srcDirs += runAloneAbsDir + "/java"
            sourceSet.res.srcDirs += runAloneAbsDir + "/res"
            sourceSet.assets.srcDirs += runAloneAbsDir + "/assets"
            sourceSet.jniLibs.srcDirs += runAloneAbsDir + "/jniLibs"

            if (kotlinSupport) {
                sourceSet.java.srcDirs += runAloneAbsDir + "/kotlin"
            }
        }

        if (kotlinSupport) {
            sourceSet.java.srcDirs += "src/$flavorName/kotlin"
        }
    }


    private static AssembleTask getTaskInfo(Project project) {
        def taskNames = project.gradle.startParameter.taskNames

        AssembleTask assembleTask = new AssembleTask()
        for (String task : taskNames) {

            if (task.toUpperCase().contains("ASSEMBLE")
                    || task.contains("aR")
                    || task.contains("asR")
                    || task.contains("asD")
                    || task.toUpperCase().contains("TINKER")
                    || task.toUpperCase().contains("INSTALL")
                    || task.toUpperCase().contains("RESGUARD")) {
                if (task.toUpperCase().contains("DEBUG")) {
                    assembleTask.isDebug = true
                }
                assembleTask.isAssemble = true
                if (!assembleTask.modules.contains(project.name)) {
                    assembleTask.modules.add(project.name)
                }
                break
            }
        }

        return assembleTask
    }

    private static class AssembleTask {
        boolean isAssemble = false
        boolean isDebug = false
        List<String> modules = new ArrayList<>()
    }
}