public class Main {
    public static void main(String[] args) {
        int a = func(1);
        int b = func(2);
        int c = func(3);
        test(c);
    }
    public static int func(int x) {
        return x + 1;
    }
    public static void test(int x) {
        System.out.println(x);
    }
}
