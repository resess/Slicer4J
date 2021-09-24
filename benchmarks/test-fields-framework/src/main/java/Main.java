import java.util.ArrayList;
import java.util.List;

public class Main {
    List<Long> array = new ArrayList<>();
    public static void main(String[] args) {
        Main main = new Main();
        main.array.add(main.getSomeValue());
        main.printArray();
    }

    private long getSomeValue() {
        return System.nanoTime();
    }
    
    private void printArray() {
        for (Long aLong : array) {
            System.out.println(aLong);
        }
    }

}