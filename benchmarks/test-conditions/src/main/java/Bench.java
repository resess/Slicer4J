public class Bench {
    public static void main(String[] args) {
        int [] parsed;
        if (args.length > 0){
            parsed = parse(args);
        } else {
            parsed = null;
        }

        String message = "null";
        if (parsed != null) {
            message = String.valueOf(parsed.length);
        }

        int i = 0;
        while (i < 3) {
            message = message + "-" + i;
            i++;
        }

        while (i < 10) {
            if (i > 6) {
                break;
            }
            message = message + "-" + i;
            i++;
        }

        System.out.println(message + parsed);
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
