public class Bench {

    static int compute(int start, int end, int seed) {
        int gap = end - start; // line 4
        int offset = start + 1; // line 5
        int base = gap * 2; // line 6
        int result = base + offset + seed; // line 7
        return result; // line 8
    }

    public static void main(String[] args) {
        int x = 5; // line 11
        int y = 15; // line 12
        int z = compute(x, y, 3); // line 13
        System.out.println(z); // line 14
    }
}
