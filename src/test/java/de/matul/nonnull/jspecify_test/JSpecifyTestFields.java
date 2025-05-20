package de.matul.nonnull.jspecify_test;

import de.matul.nonnull.NonNullError;
import org.junit.Test;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class JSpecifyTestFields {
    
    @NonNull Object nonnull;
    @Nullable Object nullable;

    @Test
    public void test1() {
        nullable = null;
        
        nullable = "xxx";
        nonnull = "xxx";
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
}
