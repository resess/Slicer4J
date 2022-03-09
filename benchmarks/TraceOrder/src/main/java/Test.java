public class Test {
	public static void main (String[] args) {
		int a = 1; 
        int b = 2; 
        int c = func(a, b);
        System.out.println(func(a, b));

	}
    public static int func(int a, int b) {
        return (a+b);
    }
}

