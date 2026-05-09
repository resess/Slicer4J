public class Main {
    public static void main(String[] args) {
        double[] grades = { 50.0, 60.0, 60.0, 70.0 };
        double product = 1.0;
        double sum = 0.0;
        for (double g1 : grades) {
            sum += g1;
            product *= 10.0;
        }
        System.out.println(sum);
        System.out.println(product);
    }
}
