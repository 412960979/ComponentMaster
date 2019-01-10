package com.wn.component.services.apt;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.wn.component.services.BaseModuleServices;
import com.wn.component.services.ServiceBinder;
import com.wn.component.services.annotation.Module;
import com.wn.component.services.annotation.ServicesImpl;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.*;

public class ComponentServicesProcessor extends AbstractProcessor {
    private HashMap<String, List<ServiceBinderGenerator>> ServiceBinderGenerators = new HashMap();
    private String servicesClassName = "ModuleServices";

    private boolean moduleSetting = false;

    private Type hasMapType = ParameterizedTypeImpl.make(
            HashMap.class, new Type[]{Class.class, new GenericArrayType() {
                @Override
                public Type getGenericComponentType() {
                    return ServiceBinder.class;
                }
            }}, null
    );

    private boolean checkId(List<ServiceBinderGenerator> generators, ServiceBinderGenerator g) {
        if (generators.size() > 0 && "".equals(g.id)) {
            return false;
        }

        for (ServiceBinderGenerator generator : generators) {
            if ("".equals(generator.id)) {
                return false;
            } else if (generator.id.equals(g.id)) {
                return false;
            }
        }

        generators.add(g);

        return true;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Messager messager = processingEnv.getMessager();
        Map<String, String> options = processingEnv.getOptions();

        /*for (Map.Entry<String, String> entry : options.entrySet()){
            System.out.println(entry.getKey() + "================" + entry.getValue());
        }*/

        if (!options.isEmpty()) {
            try {
                servicesClassName = getVariateName(processingEnv.getOptions().get("moduleName"), servicesClassName)
                        + "ModuleServices";
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        for (TypeElement typeElement : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(typeElement)) {
                if (element.getAnnotation(ServicesImpl.class) != null) {
                    ElementKind kind = element.getKind();
                    if (!kind.isClass()) {
                        messager.printMessage(Diagnostic.Kind.ERROR, ServicesImpl.class.getName() + " only class use");
                    } else {
                        try {
                            ServiceBinderGenerator generator = new ServiceBinderGenerator(element);
                            List<ServiceBinderGenerator> generators;
                            if (ServiceBinderGenerators.containsKey(generator.type)) {
                                generators = ServiceBinderGenerators.get(generator.type);

                                if (!checkId(generators, generator)) {
                                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "services id duplicate or no id");
                                }
                            } else {
                                generators = new ArrayList();
                                ServiceBinderGenerators.put(generator.type, generators);

                                generators.add(generator);
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "注解" + Module.class.getName());
                        }
                    }
                } else if (element.getAnnotation(Module.class) != null) {
                    if (moduleSetting) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "注解" + Module.class.getName() + "只能设置一次");
                    }

                    Module module = element.getAnnotation(Module.class);

                    if (!"".equals(module.servicesName().trim())) {
                        servicesClassName = module.servicesName();
                    }

                    moduleSetting = true;
                }
            }
        }

        TypeSpec.Builder moduleServicesTypeBuilder = TypeSpec.classBuilder(servicesClassName)
                .superclass(BaseModuleServices.class)
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC);

        MethodSpec.Builder initMethodBuild = MethodSpec.methodBuilder("init")
                .addAnnotation(Override.class)
                .addParameter(hasMapType, "binders")
                .addModifiers(Modifier.PROTECTED);

        moduleServicesTypeBuilder
                .addField(BaseModuleServices.class, "sInstance", Modifier.STATIC, Modifier.VOLATILE, Modifier.PRIVATE)
                .addMethod(MethodSpec.methodBuilder("getInstance")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addCode("   if(sInstance == null) {\n" +
                                "                synchronized (" + servicesClassName + ".class) {\n" +
                                "                    if (sInstance == null) {\n" +
                                "                        sInstance = new " + servicesClassName + "();\n" +
                                "                    }\n" +
                                "                }\n" +
                                "             }\n" +
                                "\n" +
                                "           return sInstance;\n")
                        .returns(BaseModuleServices.class)
                        .build());

        moduleServicesTypeBuilder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

        for (Map.Entry<String, List<ServiceBinderGenerator>> entry : ServiceBinderGenerators.entrySet()){
            StringBuilder content = new StringBuilder("binders.put(");
            content.append(entry.getKey())
                    .append(".class, new ServiceBinder[]{");

            int i = 0;
            for (ServiceBinderGenerator generator : entry.getValue()) {
                if (i > 0){
                    content.append(",");
                }

                content.append(generator.generate());

                i++;
            }

            content.append("});\n");

            initMethodBuild.addCode(content.toString());
        }

        try{
            String servicesPackage = "com.wn.component.services.gen";
            JavaFile.builder(servicesPackage, moduleServicesTypeBuilder.addMethod(initMethodBuild.build()).build())
                    .build().writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(ServicesImpl.class.getName());
        types.add(Module.class.getName());

        return types;
    }

    private static String getVariateName(String name, String defaultName){
        String variateName = name.replaceAll("[ -]", "_")
                .replaceAll("[^0-9a-zA-Z_$]", "");
        if (variateName.equals("")){
            return defaultName;
        }

        variateName = variateName.toLowerCase();
        char start = variateName.charAt(0);

        // 首字母大写
        if (start >= 'a' && start <= 'z'){
            variateName = String.valueOf(start).toUpperCase() + variateName.substring(1);
        } else if (start != '_') {
            variateName = "_" + variateName;
        }

        return variateName;
    }
}























