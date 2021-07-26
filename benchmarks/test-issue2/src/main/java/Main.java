public class Main {
    
    public static void main(String[] args) {
        if (args.length != 1)
            throw new IllegalArgumentException();
        Integer length = getLength(args[0]);
        String output = "Empty";
        if (length != null) {
            output = "Not " + output;
       }
       System.out.println(output);
   }
   
   private static Integer getLength(String s) {
       int length = s.length();
       if (length == 0) {
           return null;
       }
       return length;
   }
   
}