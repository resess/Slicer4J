public class Bench {

    public static void main(String[] args) {
        int start = Integer.parseInt(args[0]);
        int length = Integer.parseInt(args[1]);
        int [] arr1 = { 0, 1, 2, 3, 4, 5 };
        int [] arr2 = { 5, 10, 20, 30, 40, 50 };
        System.arraycopy(arr1, 0, arr2, start, length);
        System.out.println(arr2[2]);
   }
}