import nonnull.DeepNonNull;
import nonnull.NonNull;
import nonnull.NonNullError;


public class Test {
    
    private Object result;
	
	@NonNull Object nonnull(@NonNull Object non, Object able, @NonNull Object non2) {
		return result;
	}
	
	@DeepNonNull Object[] deepNonnull(@DeepNonNull Object[] deepNon, @NonNull Object[] non) {
	    return new Object[] { result };
	}

	public static void main(String[] args) {
	    Test t = new Test();
	    t.testNonNull();
	    t.testDeepNonNull();
    }
	
	private void testDeepNonNull() {
	    
	    Object[] arr = { "v" };
	    Object[] nullArr = { "v", "v", null };
	    
	    try {
            result = "v";
            deepNonnull(null, arr);
            throw new AssertionError("should have failed");
        } catch (NonNullError ex) {
            ex.printStackTrace();
        }

        try {
            result = "v";
            deepNonnull(nullArr, arr);
            throw new AssertionError("should have failed");
        } catch (NonNullError ex) {
            ex.printStackTrace();
        }
        
        try {
            result = "v";
            deepNonnull(nullArr, null);
            throw new AssertionError("should have failed");
        } catch (NonNullError ex) {
            ex.printStackTrace();
        }

        try {
            result = null;
            deepNonnull(arr, arr);
            throw new AssertionError("should have failed");
        } catch (NonNullError ex) {
            ex.printStackTrace();
        }

        result = "v";
        deepNonnull(arr, nullArr);

    }

    private void testNonNull() {
        try {
            result = "v";
            nonnull(null, "v", "v");
            throw new AssertionError("should have failed");
        } catch (NonNullError ex) {
            ex.printStackTrace();
        }

        try {
            result = "v";
            nonnull("v", "v", null);
            throw new AssertionError("should have failed");
        } catch (NonNullError ex) {
            ex.printStackTrace();
        }

        try {
            result = null;
            nonnull("v", "v", "v");
            throw new AssertionError("should have failed");
        } catch (NonNullError ex) {
            ex.printStackTrace();
        }

        result = "v";
        nonnull("v", "v", "v");

        result = "v";
        nonnull("v", null, "v");

    }

    private void toLookAtByteCodeToUnderstandWhatHappens(Object arg) {
	    NonNullError.checkNonNull(arg, "arg0", false);
	    NonNullError.checkNonNull(arg, false);
	}
	
}
