public class Bench {

    public static void main(final String[] args) {
        final int a = args[0].charAt(0)-'0'; // this expression must not be constant!
        final int b = 2*a;
        final int c = 2*b;
        final int d = 2*c;
        System.out.println(d);
    }

}
