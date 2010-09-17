/*
 * NonNull Runtime Checking for Methods
 * 
 * 2009 by Mattias Ulbrich
 * 
 * published under GPL.
 */

package nonnull;

import java.util.Collection;

import org.apache.bcel.classfile.Utility;

/**
 * This is the error class that is used to report errors by the generated code.
 * 
 * The error message is generated from the information provided to the
 * constructors.
 */
public class NonNullError extends Error {

	/**
	 * serial ID
	 */
	private static final long serialVersionUID = -6980596231380743926L;

	private StackTraceElement invokingContext;

	/**
	 * The number of the method parameter that fails, -1 if return failed.
	 */
	private int failingParameter = -1;
	
	private int failingIndex = -1;

	/**
	 * Instantiates an error for a failed return check.
	 * 
	 * @param className
	 *            the class name
	 * @param methodSignature
	 *            the method signature
	 */
	private NonNullError(int failingParameter, int failingIndex) {
	    super();
	    extractInvokingContext();
	    this.failingParameter = failingParameter;
	    this.failingIndex = failingIndex;
    }

    private void extractInvokingContext() {
        int i = 0;
	    StackTraceElement[] stackTrace = getStackTrace();
        String myClassName = getClass().getName();
        
        do {
	        invokingContext = stackTrace[i];
	        i++;
	    } while(i < stackTrace.length && 
	            myClassName.equals(invokingContext.getClassName()));
        
    }

    public NonNullError(int parameterNumber) {
        this(parameterNumber, -1);
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
		
		if (failingParameter >= 0) {
            retval.append(" in parameter ").append(failingParameter);
        } else {
            retval.append(" in return statement");
        }

        if (failingIndex >= 0) {
            retval.append(", at index ").append(failingIndex).append(
                    " while checking for elementwise nonnull");
        }

		retval.append(".");
		return retval.toString();
	}
	
	public static void checkNonNull(Object ref, int parameterNumber, boolean deepChecking) {
	    
	    if(ref == null) {
	        throw new NonNullError(parameterNumber);
	    }

	    if(!deepChecking)
	        return;

	    // TODO have some kind of plugin? does that make sense?
	    
	    if (ref instanceof Object[]) {
	        Object[] array = (Object[]) ref;
	        for (int i = 0; i < array.length; i++) {
	            if(array[i] == null)
	                throw new NonNullError(parameterNumber, i);
	        }
	    }

	    if (ref instanceof Collection<?>) {
	        Collection<?> coll = (Collection<?>) ref;
	        int i = 0;
	        for (Object object : coll) {
	            if(object == null)
	                throw new NonNullError(parameterNumber, i);
	            i++;
	        }
	    }
	}
	
	public static void checkNonNull(Object ref, boolean deepChecking) {
	   checkNonNull(ref, -1, deepChecking);
	}

}
