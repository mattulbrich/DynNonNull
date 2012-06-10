package de.matul.nonnull;

public class NonNullError extends Error {

    /**
     *
     */
    public NonNullError() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message
     * @param cause
     */
    public NonNullError(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message
     */
    public NonNullError(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param cause
     */
    public NonNullError(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

}
