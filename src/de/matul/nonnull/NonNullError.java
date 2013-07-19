/*
 * NonNull Runtime Checking for Methods
 * 
 * 2009 by Mattias Ulbrich
 * 
 * published under GPL.
 */
package de.matul.nonnull;

/**
 * This error class is used to indicate non-null errors which occur at runtime.
 */
public class NonNullError extends Error {

    private static final long serialVersionUID = 4814928304649188871L;

    public NonNullError() {
        super();
    }

    public NonNullError(String message, Throwable cause) {
        super(message, cause);
    }

    public NonNullError(String message) {
        super(message);
    }

    public NonNullError(Throwable cause) {
        super(cause);
    }

}
