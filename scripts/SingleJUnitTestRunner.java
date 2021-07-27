import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/* renamed from: SingleJUnitTestRunner */
public class SingleJUnitTestRunner {
    public static void main(String [] strArr) throws ClassNotFoundException {
        int i = 0;
        String[] split = strArr[0].split("#");
        Result run = new JUnitCore().run(Request.method(Class.forName(split[0]), split[1]));
        if (run.wasSuccessful()) {
            System.out.println("Test pass");
        } else {
            System.out.println("Test fail");
            for (Failure failure: run.getFailures()) {
                System.out.println(failure.getTrace());
            }
        }
        if (!run.wasSuccessful()) {
            i = 1;
        }
        System.exit(i);
    }
}
