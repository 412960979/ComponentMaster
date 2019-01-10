package com.wn.component.services.apt.utils;

import com.google.auto.common.MoreElements;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public final class AnnotationUtils {
    private AnnotationUtils() {
    }

    public static Object getAnnotationValues(Element element, String valueName, Class<? extends Annotation> annotationClass) {
        AnnotationMirror annotationMirror = MoreElements.getAnnotationMirror(element, annotationClass).get();
        Set<? extends Map.Entry<? extends ExecutableElement, ? extends AnnotationValue>> elementSet =
                annotationMirror.getElementValues().entrySet();

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
             elementSet) {
            if (entry.getKey().getSimpleName().toString().endsWith(valueName)){
                return entry.getValue().getValue();
            }
        }

        return null;
    }

    public static boolean checkAnnotation(Element annotationElement, ElementType elementType){
        Target target = annotationElement.getAnnotation(Target.class);
        return target == null || Arrays.asList(target.value()).contains(elementType);
    }
}
