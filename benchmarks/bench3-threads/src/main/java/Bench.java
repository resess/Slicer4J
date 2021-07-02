public class Bench {

    public static void main(String[] args) {
        Message message = new Message();
        Producer producer = new Producer(message);
        Consumer consumer = new Consumer(message);
        Thread t1 = new Thread(producer);
        Thread t2 = new Thread(consumer);
        try {
            t1.run();
            t1.join();
            t2.run();
            t2.join();
            System.out.println(consumer.getProcessed());
        } catch (Exception e) {
            System.out.println("Exception thrown");
        }
   }
}

class Producer implements Runnable {
    Message message;
    Producer(Message message) {
        this.message = message;
    }
    @Override
    public void run() {
        this.message.contents = "Hi there!";
    }
}

class Consumer implements Runnable {
    Message message;
    String processed;
    Consumer(Message message) {
        this.message = message;
    }
    @Override
    public void run() {
        this.processed = "Received: " + this.message.contents;
    }
    public String getProcessed() {
        return processed;
    }
}

class Message {
    public String contents;
}