import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Issue {

    public static void main(String[] args) {
        PairSet<Integer, String> pairSet = new PairSet<>(Arrays.asList(new Pair<>(1, "A"), new Pair<>(2, "B")));
        System.out.println(pairSet);
   }
}

class PairSet<T, G> {
 
   Set<Pair<T, G>> set;

   public PairSet() {
       set = new HashSet<>();
   }

   public PairSet(Collection<Pair<T, G>> collection) {
       this();
       set.addAll(collection);
   }

   @Override
   public String toString() {
       StringBuilder sb = new StringBuilder();
       for (Pair<T, G> pair : set) {
           sb.append(pair).append('\n');
       }
       return sb.toString();
   }
}

class Pair<T, G> {

   T first;
   G second;

   public Pair(T first, G second) {
       this.first = first;
       this.second = second;
   }

   @Override
   public String toString() {
       return String.format("%s %s %s", first, (char) 8212, second);
   }
}
