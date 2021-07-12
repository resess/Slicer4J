public class SliceMe {
    public static void main(String[] args) {
        int [] parsed;
        if (args.length > 0){
            parsed = parse(args);
        } else {
            parsed = null;
        }
        System.out.println(parsed.length);
    }
    private static int[] parse(String[] str) {
        String fullString = String.join(", ", str);
        int [] arr = new int[fullString.length()];
        for (int i = 0; i< fullString.length(); i++) {
            arr[i] = fullString.charAt(i)-'0';
        }
        return arr;
    }
}
