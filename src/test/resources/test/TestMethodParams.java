package test;

import java.util.Arrays;

import nonnull.DeepNonNull;
import nonnull.NonNull;
import nonnull.Nullable;
import de.matul.nonnull.NonNullError;

public class TestMethodParams {
    void nullable(@Nullable Object o) { }
    void nonnull(@NonNull Object o) { }
    void deepnonnull(@DeepNonNull Object o) { }
    
    void test() {
        test1();
        test2();
        test3();
        test4();
        test5();

    }
    
    private void test1() throws Error {
        try {
            nullable(null);
            
            nullable("xxx");
            nonnull("xxx");
            deepnonnull("xxx");
            deepnonnull(new Object[0]);
            System.err.println("As expected: not failed");
        } catch (NonNullError e) {
            e.printStackTrace();
            throw new Error("XXX Should not have failed!");
        }
    }
    
    private void test2() throws Error {
        try {
            nonnull(null);
            throw new Error("XXX Should have failed!");
        } catch (NonNullError e) {
            System.err.println("As expected: failed");
            e.printStackTrace();
        }
    }
    
    private void test3() throws Error {
        try {
            deepnonnull(null);
            throw new Error("XXX Should have failed!");
        } catch (NonNullError e) {
            System.err.println("As expected: failed");
            e.printStackTrace();
        }
    }
    
    private void test4() throws Error {
        try {
            deepnonnull(new Object[] { "xxx", null  } );
            throw new Error("XXX Should have failed!");
        } catch (NonNullError e) {
            System.err.println("As expected: failed");
            e.printStackTrace();
        }
    }
    
    private void test5() throws Error {
        try {
            deepnonnull(Arrays.asList(new Object[] { "xxx", null  } ));
            throw new Error("XXX Should have failed!");
        } catch (NonNullError e) {
            System.err.println("As expected: failed");
            e.printStackTrace();
        }
    }
}
