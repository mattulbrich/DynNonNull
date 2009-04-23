/*
 * NonNull Runtime Checking for Methods
 * 
 * 2009 by Mattias Ulbrich
 * 
 * published under GPL.
 */

package nonnull;

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

	/**
	 * The signature of the method which failed its test.
	 * The format is
	 * <pre>
	 *    methodName(Ljava.lang.Object;I)B
	 * </pre>
	 * for a method <code>byte methodName(java.lang.Object, int)</code>
	 */
	private String methodSignature;

	/**
	 * The name of the class in which the failing method exists. 
	 */
	private String className;

	/**
	 * The number of the method parameter that fails, -1 if return failed.
	 */
	private int parameter = -1;

	/**
	 * Instantiates an error for a failed return check.
	 * 
	 * @param className
	 *            the class name
	 * @param methodSignature
	 *            the method signature
	 */
	public NonNullError(String className, String methodSignature) {
		this.className = className;
		this.methodSignature = methodSignature;
	}

	/**
	 * Instantiates an error for a failed parameter check.
	 * 
	 * @param className
	 *            the class name
	 * @param methodSignature
	 *            the method signature
	 * @param parameter
	 *            the parameter number (>=0)
	 */
	public NonNullError(String className, String methodSignature, int parameter) {
		assert parameter >= 0;
		this.className = className;
		this.methodSignature = methodSignature;
		this.parameter = parameter;
	}

	private String getMethod() {

		int sep = methodSignature.indexOf('(');

		return Utility.methodSignatureToString(methodSignature.substring(sep),
				methodSignature.substring(0, sep), "");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Throwable#getMessage()
	 */
	@Override
	public String getMessage() {
		String retval = "Unallowed null reference in class " + className
				+ ", method " + getMethod();
		if (parameter >= 0) {
			retval += " in parameter " + parameter;
		} else {
			retval += " in return statement";
		}

		return retval;
	}

}
