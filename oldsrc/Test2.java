import nonnull.NonNull;
import nonnull.NonNullError;
import java.util.List;

@NonNull
public class Test2<E> {
    public static void main(String[] args) {
        try {
            m(null);
            throw new Error("Should fail because of class @NonNull");
        } catch (NonNullError e) {
            e.printStackTrace();
            System.err.println("as expected");
        }

        try {
            new Test2().n(null);
            throw new Error("Should fail because of class @NonNull");
        } catch (NonNullError e) {
            e.printStackTrace();
            System.err.println("as expected");
        }
        
        try {
            new Test2().longParam(20L, null);
            throw new Error("Should fail because of class @NonNull");
        } catch (NonNullError e) {
            e.printStackTrace();
            System.err.println("as expected");
        }
    }

    public void longParam(long l, Object object) {
    }

    public static void m(String a) {
        System.out.println(a);
    }

    public void n(String a) {
        System.out.println(a);
    }
private void aha(List<? extends String> L, E e) {
}
}
