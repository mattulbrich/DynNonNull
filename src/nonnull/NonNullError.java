/*
 * NonNull Runtime Checking for Methods
 * 
 * 2009 by Mattias Ulbrich
 * 
 * published under GPL.
 */

package nonnull;


/**
 * This is the error class used to indicate a violation of non-nullness annotations.
 * 
 * They are usually reported by generated code.
 * 
 * The error message is generated from the information provided to the
 * constructors.
 */
public class NonNullError extends Error {

    private static final long serialVersionUID = -6980596231380743926L;

    private static final String CLASSNAME = NonNullError.class.getName();

    private StackTraceElement invokingContext;

    /**
     * The name of the method parameter that fails, null if return failed.
     */
    private String failingParameter;

    /**
     * The index of a collection that failed, -1 if no collection check happened.
     */
    private int failingIndex = -1;

    /**
     * Instantiates an error for a failed return check.
     */
    private NonNullError(String failingParameter, int failingIndex) {
        super();
        extractInvokingContext();
        this.failingParameter = failingParameter;
        this.failingIndex = failingIndex;
    }

    private NonNullError(String parameterName) {
        this(parameterName, -1);
    }
    
    /**
     * store into {@link #invokingContext} the trace element which really
     * failed.
     */
    private void extractInvokingContext() {
        int i = 0;
        StackTraceElement[] stackTrace = getStackTrace();

        do {
            invokingContext = stackTrace[i];
            i++;
        } while (i < stackTrace.length &&
                CLASSNAME.equals(invokingContext.getClassName()));

    }


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Throwable#getMessage()
     */
    @Override
    public String getMessage() {
        
        StringBuilder retval = new StringBuilder();
        retval.append("Unallowed null reference in class ")
                .append(invokingContext.getClassName())
                .append(", method ")
                .append(invokingContext.getMethodName());

        if (failingParameter != null) {
            retval.append(" in parameter ").append(failingParameter);
        } else {
            retval.append(" in return statement");
        }

        if (failingIndex >= 0) {
            retval.append(", at index ").append(failingIndex).append(
                    " while checking for elementwise NonNull");
        }

        retval.append(".");
        return retval.toString();
    }

    public static void checkNonNull(Object ref, String parameterName,
            boolean deepChecking) {

        if (ref == null) {
            throw new NonNullError(parameterName);
        }

        if (!deepChecking)
            return;

        // TODO have some kind of plugin? does that make sense?

        if (ref instanceof Object[]) {
            Object[] array = (Object[]) ref;
            for (int i = 0; i < array.length; i++) {
                if (array[i] == null)
                    throw new NonNullError(parameterName, i);
            }
        }

        if (ref instanceof Iterable<?>) {
            Iterable<?> coll = (Iterable<?>) ref;
            int i = 0;
            for (Object object : coll) {
                if (object == null)
                    throw new NonNullError(parameterName, i);
                i++;
            }
        }
    }

    public static void checkNonNull(Object ref, boolean deepChecking) {
        checkNonNull(ref, null, deepChecking);
    }

}
