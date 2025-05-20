package de.matul.nonnull.test;

import de.matul.nonnull.NonNullError;
import nonnull.DeepNonNull;
import nonnull.NonNull;
import nonnull.Nullable;
import org.junit.Test;

public class TestFields {
    
    @NonNull Object nonnull;
    @Nullable Object nullable;
    @DeepNonNull Object deepNonNull;

    @Test
    public void test1() {
        nullable = null;
        
        nullable = "xxx";
        nonnull = "xxx";
        deepNonNull = "xxx";
        
        deepNonNull = new Object[0];
        deepNonNull = new Object[] { "xxx" };
    }

    @Test
    public void test2() {
        try {
            nonnull = "something";
            nonnull = null;
            throw new Error("XXX Should have failed!");
        } catch (NonNullError e) {
            System.err.println("As expected: failed");
            e.printStackTrace();
        }
    }

    @Test
    public void test3() {
        try {
            deepNonNull = null;
            throw new Error("XXX Should have failed!");
        } catch (NonNullError e) {
            System.err.println("As expected: failed");
            e.printStackTrace();
        }
    }

    @Test
    public void test4() {
        try {
            deepNonNull = new Object[] { "xxx", null };
            throw new Error("XXX Should have failed!");
        } catch (NonNullError e) {
            System.err.println("As expected: failed");
            e.printStackTrace();
        }
    }
}
