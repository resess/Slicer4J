import java.util.Arrays;
public class Bench {
    
    public static void main(String[] args) {

        int intKey = Integer.parseInt(args[0]);
        int [] intArr = { 10, 20, 15, 22, 35 };
        Arrays.sort(intArr);
        int res = Arrays.binarySearch(intArr, intKey);
        System.out.println(res);
    }
}
