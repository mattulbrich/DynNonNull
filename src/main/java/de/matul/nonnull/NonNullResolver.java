package de.matul.nonnull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Set;

import nonnull.NonNull;

import org.objectweb.asm.Type;

import de.matul.nonnull.NonNullResolver.AnnotationType;


public class NonNullResolver {

    public enum AnnotationType {
        DEEP_NON_NULL, NON_NULL, NULLABLE, NONE
    }

    private static final Set<String> NON_NULL_ANNOTATIONS;
    private static final Set<String> NULLABLE_ANNOTATIONS;
    private static final Set<String> DEEP_NON_NULL_ANNOTATIONS;

    static {
        try {
            URL url = NonNullResolver.class.getResource("annotationNonNull.txt");
            if(url == null ) {
                throw new FileNotFoundException("annotationNonNull.txt is missing");
            }
            NON_NULL_ANNOTATIONS = Util.readStringSet(url.openStream());

            url = NonNullResolver.class.getResource("annotationNullable.txt");
            if(url == null ) {
                throw new FileNotFoundException("annotationNullable.txt is missing");
            }
            NULLABLE_ANNOTATIONS = Util.readStringSet(url.openStream());

            url = NonNullResolver.class.getResource("annotationDeepNonNull.txt");
            if(url == null ) {
                throw new FileNotFoundException("annotationDeepNonNull.txt is missing");
            }
            DEEP_NON_NULL_ANNOTATIONS = Util.readStringSet(url.openStream());

            String prop = System.getProperty("de.matul.nonnull.annotation.nonnull");
            if(prop != null) {
                NON_NULL_ANNOTATIONS.add(prop);
            }

            prop = System.getProperty("de.matul.nonnull.annotation.nullable");
            if(prop != null) {
                NULLABLE_ANNOTATIONS.add(prop);
            }

            prop = System.getProperty("de.matul.nonnull.annotation.deepnonnull");
            if(prop != null) {
                DEEP_NON_NULL_ANNOTATIONS.add(prop);
            }

        } catch (IOException e) {
            throw new NonNullError("Error while initialising NonNullResolver", e);
        }
    }

    public AnnotationType shouldCheckMethod(String classDesc, String methodName, String methodDesc, int param) {
        try {
            String className = classDesc.replace('/', '.');
            Class<?> clazz = Class.forName(className);
            return shouldCheckMethodResult(clazz, methodName, methodDesc, param);
        } catch (Exception e) {
            throw new NonNullError("Internal error in non-null checking" , e);
        }
    }

    public AnnotationType shouldCheckField(String classDesc, String fieldName) {
        try {
            String className = classDesc.replace('/', '.');
            Class<?> clazz = Class.forName(className);
            
            AnnotationType classAnn = getAnnotation(clazz);

            Field field = null;
            while(field == null && clazz != null) {
                try {
                    field = clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    // just go on
                }

                if(field == null) {
                    clazz = clazz.getSuperclass();
                }
            }

            if(field == null) {
                throw new NonNullError("Unknown field: " + fieldName);
            }

            AnnotationType ann = getAnnotation(field);

            if(ann == AnnotationType.NONE) {
                ann = classAnn;
            }

            return ann;

        } catch (Throwable e) {
            throw new NonNullError("Internal error in non-null checking for " +
                    classDesc + "." + fieldName , e);
        }
    }

    private AnnotationType shouldCheckMethodResult(Class<?> clazz, String methodName,
            String methodDesc, int param) throws ClassNotFoundException {
        
        Method method = findMethod(methodName, methodDesc, clazz);

        if(method == null) {
            return AnnotationType.NONE;
        }

        NonNullAgent.debug("methodName: %s", methodName);
        NonNullAgent.debug("Method: %s", method);
        
        AnnotationType methAnn = getMethodAnnotation(method, param);
        if(methAnn != AnnotationType.NONE) {
            return methAnn;
        }

        NonNullAgent.debug("methAnn: " + methAnn);

        AnnotationType classAnn = getAnnotation(clazz);
        if(classAnn != AnnotationType.NONE) {
            return classAnn;
        }

        Class<?> superclass = clazz.getSuperclass();
        if(superclass != null) {
            AnnotationType superAnn = 
                    shouldCheckMethodResult(superclass, methodName, methodDesc, param);
            if(superAnn != AnnotationType.NONE) {
                return superAnn;
            }
        }

        for (Class<?> intf : clazz.getInterfaces()) {
            AnnotationType superAnn = 
                    shouldCheckMethodResult(intf, methodName, methodDesc, param);
            if(superAnn != AnnotationType.NONE) {
                return superAnn;
            }
        }

        return AnnotationType.NONE;
    }

    private @NonNull AnnotationType getMethodAnnotation(Method method, int param) {
        if(param == -1) {
            return getAnnotation(method);
        } else {
            Annotation[] annotations = method.getParameterAnnotations()[param];
            if(annotations != null) {
                for (Annotation ann : annotations) {
                    NonNullAgent.debug("Ann: %s", ann);
                    if(NON_NULL_ANNOTATIONS.contains(ann.annotationType().getName())) {
                        return AnnotationType.NON_NULL;
                    }
                    if(NULLABLE_ANNOTATIONS.contains(ann.annotationType().getName())) {
                        return AnnotationType.NULLABLE;
                    }
                    if(DEEP_NON_NULL_ANNOTATIONS.contains(ann.annotationType().getName())) {
                        return AnnotationType.DEEP_NON_NULL;
                    }
                }
            }
            return AnnotationType.NONE;
        }
    }

    private @NonNull AnnotationType getAnnotation(AnnotatedElement annElem) {
        Annotation[] annotations = annElem.getAnnotations();
        NonNullAgent.debug("Annotations for %s: %s", annElem, Arrays.asList(annotations));
        if(annotations != null) {
            for (Annotation ann : annotations) {
                NonNullAgent.debug("Ann: " + ann);
                if(NON_NULL_ANNOTATIONS.contains(ann.annotationType().getName())) {
                    return AnnotationType.NON_NULL;
                }
                if(NULLABLE_ANNOTATIONS.contains(ann.annotationType().getName())) {
                    return AnnotationType.NULLABLE;
                }
                if(DEEP_NON_NULL_ANNOTATIONS.contains(ann.annotationType().getName())) {
                    return AnnotationType.DEEP_NON_NULL;
                }
            }
        }
        return AnnotationType.NONE;
    }

    private Method findMethod(String methodName, String methodDesc,
            Class<?> clazz) {
        Method result = null;
        for (Method m : clazz.getDeclaredMethods()) {
            if(methodName.equals(m.getName())) {
                if(Type.getMethodDescriptor(m).equals(methodDesc)) {
                    result  = m;
                }
            }
        }
        return result;
    }

}
