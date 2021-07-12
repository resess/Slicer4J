public class PThread implements Runnable {
    String[] args; int index;
    int[] arr = null;
    PThread(String[] args, int index){
        this.args = args; 
        this.index = index;
    }
    public void run() {
        if (index < args.length){
            this.arr = parse(args[index]);
        }
    }
    private int[] parse(String str) {
        int [] arr = new int[str.length()];
        for (int i = 0; i< str.length(); i++) {
            arr[i] = str.charAt(i)-'0';
        }
        return arr;
    }
}