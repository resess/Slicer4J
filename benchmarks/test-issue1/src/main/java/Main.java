import java.util.Arrays;

public class Main {
    
    public static void main(String[] args) {
        int[] a = new int[]{ 0, 1 };
        a = addAndReturn(a);
        add(a);
        System.out.println(Arrays.toString(a));
    }
    
    public static void add(int[] a) {
        a[0] += 1;
    }
    
    public static int[] addAndReturn(int[] a) {
        a[0] += 1;
        return a;
    }
}