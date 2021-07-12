public class Bench {
    private static String x = "hi";
    public static void main(final String[] args) {
        int a = x.charAt(0)-'0'; // this expression must not be constant!
        int b = args[0].charAt(0)-'0'; // this expression must not be constant!
        int c = getFirst(a, b);
        ++c;
        System.out.println(c);
    }

    private static int getFirst(int first, int second) {
        return first;
    }

}
