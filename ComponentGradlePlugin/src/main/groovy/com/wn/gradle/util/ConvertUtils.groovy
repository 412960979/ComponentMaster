package com.wn.gradle.util

import com.android.SdkConstants
import com.android.build.api.transform.TransformInput
import javassist.ClassPool
import javassist.CtClass
import javassist.NotFoundException

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.regex.Matcher

class ConvertUtils {

    static List<String> getClassPath(Collection<TransformInput> inputs) {
        List<String> classPath = new ArrayList<>()
        inputs.each {
            it.directoryInputs.each {
                classPath.add(it.file.absolutePath)
            }
            it.jarInputs.each {
                classPath.add(it.file.absolutePath)
            }
        }

        return classPath
    }

    static List<CtClass> toCtClasses(Collection<TransformInput> inputs, ClassPool classPool) {
        List<String> classNames = new ArrayList<>()
        List<CtClass> allClass = new ArrayList<>()
        inputs.each {
            it.directoryInputs.each {
                try {
                    def dirPath = it.file.absolutePath
                    println "dirPath:$it.file.absolutePath"
                    classPool.insertClassPath(it.file.absolutePath)
                    org.apache.commons.io.FileUtils.listFiles(it.file, null, true).each {
                        if (it.absolutePath.endsWith(SdkConstants.DOT_CLASS)) {
                            def className = it.absolutePath.substring(dirPath.length() + 1, it.absolutePath.length() - SdkConstants.DOT_CLASS.length()).replaceAll(Matcher.quoteReplacement(File.separator), '.')
                            if (classNames.contains(className)) {
                                throw new RuntimeException("You have duplicate classes with the same name : " + className + " please remove duplicate classes ")
                            }
                            classNames.add(className)
                        }
                    }
                } catch (Throwable e) {
                    println e.toString()
                }
            }

            it.jarInputs.each {
                JarFile jarFile = null

                try {
                    classPool.insertClassPath(it.file.absolutePath)
                    jarFile = new JarFile(it.file)
                    Enumeration<JarEntry> classes = jarFile.entries()
                    while (classes.hasMoreElements()) {
                        JarEntry libClass = classes.nextElement()
                        String className = libClass.getName()
                        if (className.endsWith(SdkConstants.DOT_CLASS)) {
                            className = className.substring(0, className.length() - SdkConstants.DOT_CLASS.length()).replaceAll('/', '.')
                            if (classNames.contains(className)) {
                                throw new RuntimeException("You have duplicate classes with the same name : " + className + " please remove duplicate classes ")
                            }
                            classNames.add(className)
                        }
                    }
                    jarFile.close()
                } catch (Throwable e) {
                    println e.toString()
                } finally {
                    if (jarFile != null) {
                        try {
                            jarFile.close()
                        } catch (Throwable ignored) {
                        }

                    }
                }
            }
        }
        classNames.each {
            try {
                allClass.add(classPool.get(it))
            } catch (NotFoundException e) {
                println "class not found exception class name:  $it "
            }
        }
        return allClass
    }


}