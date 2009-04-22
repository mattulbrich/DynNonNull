import nonnull.NonNull;


public class TestOnly {
	
	@NonNull Object m1(@NonNull Object non, Object able, @NonNull Object non2) {
		return null;
	}

	public static void main(String[] args) {
        new TestOnly().m1(null, 2, 3);
        System.out.println("argh");
    }
	
}
