package de.matul.nonnull;

import java.util.concurrent.atomic.AtomicInteger;

import nonnull.NoNullnessChecks;

@NoNullnessChecks
public final class NonNullChecker {

    private static class Entry {
        private final String classDesc;
        private final String name;
        private final String methodDesc;
        private final int paramNumber;

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

    }

    private static final NonNullResolver resolver = new NonNullResolver();
    private static AtomicInteger number = new AtomicInteger();
    private static DynamicArray<Object> checkerMap =
            new DynamicArray<Object>();

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

        if(value != null) {
            return;
        }

        Object desc = checkerMap.get(index);

        if(desc == null) {
            return;
        }

        if(desc instanceof String) {
            throw new NonNullError((String)desc);
        }

        // this check has not been expanded, yet
        Entry entry = (Entry) desc;
        boolean check = resolver.shouldCheckMethod(entry.classDesc, entry.name,
                entry.methodDesc, entry.paramNumber);
        if(check) {
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

            checkerMap.put(index, errMsg);
            throw new NonNullError(errMsg);
        } else {
            checkerMap.remove(index);
        }
    }


    public static void checkFieldNonNull(Object value, int index) {
        if(value != null) {
            return;
        }

        Object desc = checkerMap.get(index);

        if(desc == null) {
            return;
        }

        if(desc instanceof String) {
            throw new NonNullError((String)desc);
        }

        Entry entry = (Entry) desc;
        boolean check = resolver.shouldCheckField(entry.classDesc, entry.name);
        if(check) {
            String errMsg = "null value for the non-null field " +
                        entry.classDesc.replace('/', '.') + "." + entry.name;
            throw new NonNullError(errMsg);
        } else {
            checkerMap.remove(index);
        }
    }

    private static int getFreshIndex() {
        return number.incrementAndGet();
    }

}
