public class Bench {
    public static void main(String[] args) {
        int MAGIC = 42; // line 3
        int FACTOR = 3; // line 4
        int dynamic = args.length; // line 5
        int mixed = MAGIC + dynamic; // line 6
        int step1 = mixed * FACTOR; // line 7
        int step2 = step1 + MAGIC; // line 8
        int step3 = step2 - dynamic; // line 9
        String msg = "Result=" + step3; // line 10
        System.out.println(msg); // line 11
    }
}
