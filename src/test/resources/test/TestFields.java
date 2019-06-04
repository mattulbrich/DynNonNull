package test;

import java.util.ArrayList;

import nonnull.DeepNonNull;
import nonnull.NonNull;
import nonnull.Nullable;
import de.matul.nonnull.NonNullError;

public class TestFields {
    
    @NonNull Object nonnull;
    @Nullable Object nullable;
    @DeepNonNull Object deepNonNull;
    

    public void test() {
        test1();
        test2();
        test3();
        test4();
    }

    private void test1() {
        nullable = null;
        
        nullable = "xxx";
        nonnull = "xxx";
        deepNonNull = "xxx";
        
        deepNonNull = new Object[0];
        deepNonNull = new Object[] { "xxx" };
    }

    private void test2() {
        try {
            nonnull = null;
            throw new Error("XXX Should have failed!");
        } catch (NonNullError e) {
            System.err.println("As expected: failed");
            e.printStackTrace();
        }
    }

    private void test3() {
        try {
            deepNonNull = null;
            throw new Error("XXX Should have failed!");
        } catch (NonNullError e) {
            System.err.println("As expected: failed");
            e.printStackTrace();
        }
    }

    private void test4() {
        try {
            deepNonNull = new Object[] { "xxx", null };
            throw new Error("XXX Should have failed!");
        } catch (NonNullError e) {
            System.err.println("As expected: failed");
            e.printStackTrace();
        }
    }


    
}
