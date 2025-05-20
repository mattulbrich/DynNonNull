package de.matul.nonnull.jspecify_test;

import de.matul.nonnull.NonNullError;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import static org.junit.Assert.fail;

public class JSpecifyTestMethodParams {
    void nullable(@Nullable Object o) { }
    void nonnull(@NonNull Object o) { }
    @NonNull Object getter(@Nullable Object o) { return o; }

    @Test
    public void test1() throws Error {
        nullable(null);
        nullable("xxx");
        nonnull("xxx");
        getter("xxx");
        System.err.println("As expected: not failed");
    }

    @Test
    public void test2() throws Error {
        try {
            nonnull(null);
            fail("Should have failed!");
        } catch (NonNullError e) {
            System.err.println("As expected: failed");
            e.printStackTrace();
        }
    }

    @Test
    public void test3() throws Error {
        try {
            getter(null);
            fail("Should have failed!");
        } catch (NonNullError e) {
            System.err.println("As expected: failed");
            e.printStackTrace();
        }
    }
    
}
