package test;

import nonnull.NonNull;
import de.matul.nonnull.NonNullError;

class Testclass extends TestSuperclass implements TestInterface {

    @NonNull Object field;

    @Override
    public Object method(int x, int[] y, String z) {
        return null;
    }

    public static void main(String args[]) {
        Testclass b = new Testclass();

        System.err.println("Will call method now");
        try {
            b.method(0, null, null);
            System.err.println("Should have failed");
        } catch (NonNullError e) {
            System.err.println("As expected:");
            e.printStackTrace();
        }

        System.err.println("Will call method now, again");
        try {
            b.method(0, null, null);
            System.err.println("Should have failed");
        } catch (NonNullError e) {
            System.err.println("As expected:");
            e.printStackTrace();
        }

        System.err.println("Set field now");
        b.field = new Object();
        try {
            b.field = null;
            System.err.println("Should have failed");
        } catch(NonNullError e) {
            System.err.println("As expected:");
            e.printStackTrace();
        }

        System.err.println("Set superfield now");
        b.fieldInSuper = new Object();
        b.defaultFieldInSuper = null;
        try {
            b.fieldInSuper = null;
            System.err.println("Should have failed");
        } catch(NonNullError e) {
            System.err.println("As expected:");
            e.printStackTrace();
        }

        System.err.println("SUCCESS");
    }
}
