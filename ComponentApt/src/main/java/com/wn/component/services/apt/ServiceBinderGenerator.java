package com.wn.component.services.apt;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import com.wn.component.services.ServiceBinder;
import com.wn.component.services.annotation.ServicesImpl;
import com.wn.component.services.apt.utils.AnnotationUtils;
import com.wn.component.util.Supplier;

public class ServiceBinderGenerator {
    final String type;
    final String id;
    final String implType;

//        private static final String serverBinderName = ServiceBinder.class.getName();
    private static final String supplierName = Supplier.class.getName();


    public ServiceBinderGenerator(Element element) {
        ServicesImpl servicesImpl = element.getAnnotation(ServicesImpl.class);


        if (servicesImpl != null) {
            Object value = AnnotationUtils.getAnnotationValues(element, "value"
                    , ServicesImpl.class);

            if (value == null) {
                throw new RuntimeException("ServicesImpl value is null");
            }


            TypeElement typeElement = (TypeElement) ((DeclaredType) value).asElement();

            id = servicesImpl.filterId();
            type = typeElement.getQualifiedName().toString();
            implType = ((TypeElement) element).getQualifiedName().toString();
        } else {
            throw new RuntimeException("init generator fail");
        }
    }


    public String generate() {
        String idStr;
        if (id == null || "".equals(id)) {
            idStr = "null";
        } else {
            idStr = "\"" + id + "\"";
        }

        return "\n                new ServiceBinder(" + idStr + ", new " + supplierName + "() {\n" +
                "                    @Override\n" +
                "                    public Object get() {\n" +
                "                        return new " + implType + "();\n" +
                "                    }\n" +
                "                })";
    }
}