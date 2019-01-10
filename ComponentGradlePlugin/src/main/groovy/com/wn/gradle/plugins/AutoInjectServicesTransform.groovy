package com.wn.gradle.plugins

import com.wn.gradle.ZipUtils
import com.wn.gradle.util.ConvertUtils
import com.wn.gradle.util.ZipUtilsCompat
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.CannotCompileException
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

class AutoInjectServicesTransform extends Transform {
    Project project

    private final
    def sTmpDir = new File("$project.buildDir${File.separator}tmp${File.separator}component-services-classes")

    private final
    def sLogisticsCenterTmpFile = new File("$project.buildDir${File.separator}tmp${File.separator}LogisticsCenter.java")
//            new File(sTmpDir, "LogisticsCenter.java")

    private final static String APPSERVICES_PATH = "com/wn/component/services/AppServices.class"
    private final
    static String LOGISTICSCENTER_PATH = "com/alibaba/android/arouter/core/LogisticsCenter.class"

    AutoInjectServicesTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "componentServicesInject"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }


    @Override
    boolean isIncremental() {
        return false;
    }


    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {


        def classPool = ClassPool.getDefault()

        ArrayList<String> classPath = new ArrayList<>();

        project.android.bootClasspath.each {
            try {
                classPool.appendClassPath((String) it.absolutePath)
                classPath.add(it.absolutePath)
                println "bootClasspath:$it.absolutePath"
            } catch (Throwable e) {
                e.printStackTrace()
            }
        }


        def box = ConvertUtils.toCtClasses(transformInvocation.inputs, classPool)

        //AppServices
        CtClass appServices = null
        List<CtClass> servicesList = new ArrayList<>()
        //aRouter
        CtClass logisticsCenterClass = null
        List<CtClass> aRouterList = new ArrayList<>()

        for (CtClass ctClass : box) {
            try {
                if (ctClass.name == "com.wn.component.services.AppServices") {
                    appServices = ctClass
                }

                if (ctClass.name == "com.alibaba.android.arouter.core.LogisticsCenter") {
                    logisticsCenterClass = ctClass
                }


                if (ctClass.superclass != null && ctClass.superclass.name == "com.wn.component.services.BaseModuleServices") {
                    servicesList.add(ctClass)
                } else if (ctClass.name.startsWith("com.alibaba.android.arouter.routes")) {
                    aRouterList.add(ctClass)
                }
            } catch (Throwable ignored) {
            }
        }

        if (appServices == null) {
            throw new ClassNotFoundException("com.wn.component.services.AppServices")
        }

        CtClass classIRouteRoot = classPool.get("com.alibaba.android.arouter.facade.template.IRouteRoot")
        CtClass classIProviderGroup = classPool.get("com.alibaba.android.arouter.facade.template.IProviderGroup")
        CtClass classIInterceptorGroup = classPool.get("com.alibaba.android.arouter.facade.template.IInterceptorGroup")

        if (logisticsCenterClass != null) {
            try {
                /*   if (!aRouterList.isEmpty()) {
                       CtMethod registerObjMethod = new CtMethod(CtClass.voidType, "register_obj"
                               , new CtClass[1] {
                           classPool.get("java.lang.Object")
                       }, logisticsCenterClass)

                       registerObjMethod.setModifiers(Modifier.PRIVATE | Modifier.STATIC)

                       registerObjMethod.body = "                if(\$1 instanceof IRouteRoot) {\n" +
                               "                    registerRouteRoot((IRouteRoot)\$1);\n" +
                               "                } else if(\$1 instanceof IProviderGroup) {\n" +
                               "                    registerProvider((IProviderGroup)\$1);\n" +
                               "                } else if(\$1 instanceof IInterceptorGroup) {\n" +
                               "                    registerInterceptor((IInterceptorGroup)\$1);\n" +
                               "                } else {\n" +
                               "                    ARouter.logger.info(\"ARouter::\", \"register failed, class name: \" + className + \" should implements one of IRouteRoot/IProviderGroup/IInterceptorGroup.\");\n" +
                               "                }"

                       logisticsCenterClass.addMethod(registerObjMethod)
                   }*/

                CtMethod loadRouterMapMethod = logisticsCenterClass.getDeclaredMethod("loadRouterMap")
                loadRouterMapMethod.body = null


                StringBuilder src = new StringBuilder()

                src.append("{registerByPlugin = true;")

                for (CtClass clazz : aRouterList) {
                    String methodName
                    if (interfaceContains(clazz, classIRouteRoot)) {
                        methodName = "registerRouteRoot"
                    } else if (interfaceContains(clazz, classIProviderGroup)) {
                        methodName = "registerProvider"
                    } else if (interfaceContains(clazz, classIInterceptorGroup)) {
                        methodName = "registerInterceptor"
                    }

                    println "aRouter class name:" + clazz.name

                    String cname = clazz.name

                    if (methodName != null) {
                        src.append('\n')
                                .append("$methodName(new $cname());")
                    }
                }

                src.append("}")

                println "aRouter inject string :${src.toString()}"


                loadRouterMapMethod.setBody(src.toString())
            } catch (Exception e) {
                throw new RuntimeException(e)
            }

        }


        try {
            CtMethod initMethod = appServices.getDeclaredMethod("init")
            initMethod.body = null

            StringBuilder src = new StringBuilder()

            src.append("this.mServicesInjection = true;")

            for (CtClass moduleClass : servicesList) {
                src.append("\nthis.mServices.add(")
                        .append(moduleClass.name)
                        .append(".getInstance());")
            }

            initMethod.insertAfter(src.toString())
        } catch (CannotCompileException e) {
            throw e
        }


        servicesList.forEach({
            System.out.println("find BaseModuleServices: " + it.toString())
        })

        aRouterList.forEach({
            System.out.println("find ARouter: " + it.toString())
        })

        boolean writeAppServicesFile = false

        boolean writeARouterFile = false

        File logisticsCenterClassOut = null

        transformInvocation.inputs.each { TransformInput input ->
            //对类型为jar文件的input进行遍历
            input.jarInputs.each { JarInput jarInput ->
//                classPath.add(jarInput.file.absolutePath)

                //jar文件一般是第三方依赖库jar文件
                // 重命名输出文件（同目录copyFile会冲突）
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }

                //生成输出路径
                def dest = transformInvocation.outputProvider.getContentLocation(jarName + md5Name,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)

                if (ZipUtilsCompat.findEntryName(jarInput.file, APPSERVICES_PATH
                        , LOGISTICSCENTER_PATH)) {
                    sTmpDir.deleteDir()

                    ZipUtils.unzip(jarInput.file.absolutePath, sTmpDir.absolutePath)

                    ZipUtils.zip(sTmpDir.absolutePath, dest.absolutePath, new ZipUtils.FilenameFilter() {

                        @Override
                        boolean accept(String s) {
                            return s != APPSERVICES_PATH && s != LOGISTICSCENTER_PATH
                        }
                    })
                } else {
                    //将输入内容复制到输出
                    FileUtils.copyFile(jarInput.file, dest)
                }

            }



            input.directoryInputs.each { DirectoryInput directoryInput ->
//                classPath.add(directoryInput.file.absolutePath)


                def dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes,
                        directoryInput.scopes, Format.DIRECTORY)

                // 将input的目录复制到output指定目录
                FileUtils.copyDirectory(directoryInput.file, dest)

                if (!writeAppServicesFile) {
                    appServices.writeFile(dest.absolutePath)
                    appServices.defrost()
                    writeAppServicesFile = true
                }

                if (logisticsCenterClass != null && !writeARouterFile) {
                    logisticsCenterClassOut = dest

                    writeARouterFile = true
                }

            }
        }

        if (logisticsCenterClassOut != null) {
            logisticsCenterClass.writeFile(logisticsCenterClassOut.absolutePath)
            logisticsCenterClass.defrost()
        }


        appServices.detach()
        if (logisticsCenterClass != null) {
            logisticsCenterClass.detach()
        }
    }

    private static boolean interfaceContains(CtClass ctClass, CtClass interfaceClass) {
        def iis = ctClass.interfaces

        println "========match interface name:" + interfaceClass.name

        for (def cc in iis) {
            println "========interface name:" + cc.name
            if (cc.name == interfaceClass.name) {
                return true
            }
        }

        return false
    }
}
