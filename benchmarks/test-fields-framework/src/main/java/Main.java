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
        for (int i = 0; i < array.size(); i++ ) {
            System.out.println(array.get(i));
        }
    }

}