/*
 * NonNull Runtime Checking for Methods
 * 
 * 2009 by Mattias Ulbrich
 * 
 * published under GPL.
 */
package de.matul.nonnull;

import java.util.concurrent.atomic.AtomicInteger;

import de.matul.nonnull.NonNullResolver.AnnotationType;

import nonnull.NoNullnessChecks;
import nonnull.Nullable;

@NoNullnessChecks
public final class NonNullChecker {

    private static class Entry {
        private final String classDesc;
        private final String name;
        private final String methodDesc;
        private final int paramNumber;
        private @Nullable AnnotationType toCheck;

        private Entry(String classDesc, String name, String methodDesc,
                int paramNumber) {
            super();
            this.classDesc = classDesc;
            this.name = name;
            this.methodDesc = methodDesc;
            this.paramNumber = paramNumber;
        }

        private Entry(String classDesc, String name) {
            super();
            this.classDesc = classDesc;
            this.name = name;
            this.methodDesc = null;
            this.paramNumber = -2;
        }

        @Override public String toString() {
            return classDesc + "." + name + methodDesc + "/" + paramNumber;
        }
    }

    /**
     * This resolves a method/field access
     */
    private static final NonNullResolver resolver = new NonNullResolver();
    
    /**
     * The number of the next unsed index in checkerMap. It is atomic such that
     * more threads can accesss the checker at once.
     */
    private static AtomicInteger number = new AtomicInteger();
    
    /**
     * This map stores information about method calls/field accesses in an
     * index-based manner.
     * 
     * The {@code register...} methods add {@link Entry} objects to the map and
     * the check method eventually adds the check information or removes the
     * entry.
     */
    private static DynamicArray<Entry> checkerMap =
            new DynamicArray<Entry>();

    static int registerArgumentCheck(String className, String methodName, String methDesc,
            int paramNumber) {
        int result = getFreshIndex();
        checkerMap.put(result, new Entry(className, methodName, methDesc, paramNumber));
        NonNullAgent.debug("Registering entry no. %d", result);
        return result;
    }

    static int registerGetFieldCheck(String className, String fieldName) {
        int result = getFreshIndex();
        checkerMap.put(result, new Entry(className, fieldName));
        return result;
    }

    static int registerPutFieldCheck(String className, String fieldName) {
        int result = getFreshIndex();
        checkerMap.put(result, new Entry(className, fieldName));
        return result;
    }

    static int registerReturnCheck(String className, String methodName, String methDesc) {
        int result = getFreshIndex();
        checkerMap.put(result, new Entry(className, methodName, methDesc, -1));
        return result;
    }

    public static void checkMethodParameterNonNull(Object value, int index) {

        Entry entry = checkerMap.get(index);

        if(entry == null) {
            // no check is needed ... fine
            return;
        }

        AnnotationType check = entry.toCheck;
        if(check == null) {
            // no check has been determined yet.
            check = resolver.shouldCheckMethod(entry.classDesc, entry.name, 
                    entry.methodDesc, entry.paramNumber);
            if(check != AnnotationType.NON_NULL && check != AnnotationType.DEEP_NON_NULL) {
                checkerMap.remove(index);
                return;
            }
            entry.toCheck = check;
        }


        if(value == null) {
            String errMsg;
            if(entry.paramNumber == -1) {
                errMsg = "null result value in non-null method " +
                        entry.classDesc.replace('/', '.') + "." + entry.name +
                        Util.getMethodSignature(entry.methodDesc);
            } else {
                errMsg = "null value in non-null parameter number "
                        + (entry.paramNumber+1) +
                        " in method " + entry.classDesc.replace('/', '.') + "."
                        + entry.name + Util.getMethodSignature(entry.methodDesc);
            }
            throw new NonNullError(errMsg);
        }

        if(check == AnnotationType.DEEP_NON_NULL) {
            String result = checkDeepNonNull(value);
            if(result != null) {
                String errMsg;
                if(entry.paramNumber == -1) {
                    errMsg = "embedded null result value in non-null method " +
                            entry.classDesc.replace('/', '.') + "." + entry.name +
                            Util.getMethodSignature(entry.methodDesc) + ": " + result;
                } else {
                    errMsg = "null value in non-null parameter number "
                            + (entry.paramNumber+1) +
                            " in method " + entry.classDesc.replace('/', '.') + "."
                            + entry.name + Util.getMethodSignature(entry.methodDesc) +
                            ": " + result;
                }
                throw new NonNullError(errMsg);
            }
        }
    }


    public static void checkFieldNonNull(Object value, int index) {
        Entry entry = checkerMap.get(index);

        if(entry == null) {
            // no check is needed ... fine
            return;
        }

        AnnotationType check = entry.toCheck;
        if(check == null) {
            // no check has been determined yet.
            check = resolver.shouldCheckField(entry.classDesc, entry.name);
            if(check != AnnotationType.NON_NULL && check != AnnotationType.DEEP_NON_NULL) {
                checkerMap.remove(index);
                return;
            }
            entry.toCheck = check;
        }

        if(value == null) {
            String errMsg = "null value for the non-null field " +
                    entry.classDesc.replace('/', '.') + "." + entry.name;
            throw new NonNullError(errMsg);
        }

        if(check == AnnotationType.DEEP_NON_NULL) {
            String result = checkDeepNonNull(value);
            if(result != null) {
                String errMsg = "embedded null value for the non-null field " +
                        entry.classDesc.replace('/', '.') + "." + entry.name + ": " + result;
                throw new NonNullError(errMsg);
            }
        }

    }

    // TODO have a more sophisticated plugin mechanism where people can add their implementations
    private static @Nullable String checkDeepNonNull(Object value) {
        
        if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            for (int i = 0; i < array.length; i++) {
                if(array[i] == null) {
                    return "array index [" + i + "]";
                }
            }
        }
        
        if (value instanceof Iterable<?>) {
            Iterable<?> iterable = (Iterable<?>) value;
            int pos = 0;
            for (Object object : iterable) {
                if(object == null) {
                    return "iteration index " + pos;
                }
                pos ++;
            }
        }
        
        return null;
    }

    private static int getFreshIndex() {
        return number.incrementAndGet();
    }

}
