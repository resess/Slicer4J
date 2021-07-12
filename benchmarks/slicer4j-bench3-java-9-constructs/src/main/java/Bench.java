

public class Bench {
    public static void main(String[] args) {
        int firstParam = Integer.parseInt(args[0]);
        int secondParam = Integer.parseInt(args[1]);
        TwoArgIntOperator addTwoInts = (a, b) -> a + b;
        int result = Bench.method(firstParam, secondParam, addTwoInts);
        System.out.println("Result: " + result);
    }

    static int method(int firstParam, int secondParam, TwoArgIntOperator operator) {
        return operator.op(firstParam, secondParam);
    }
}

interface TwoArgIntOperator {
    public int op(int a, int b);
}