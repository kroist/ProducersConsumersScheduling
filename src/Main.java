import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;


/**

    runtime for:

    2 producers, 2 consumers, 10^6 requests per consumer, 1 threads:     12 seconds
    2 producers, 2 consumers, 10^6 requests per consumer, 4 threads:     6 seconds
 */

public class Main {

    Random rand = new Random();

    private class Producer implements Runnable {
        int numRequest;
        MyService service;
        public Producer(int numRequest, MyService service) {
            this.numRequest = numRequest;
            this.service = service;
        }

        @Override
        public void run() {
            int priority = rand.nextInt(Integer.MAX_VALUE) + 1;
            for (int i = 0; i < numRequest; i++) {
                try {
                    service.produce(service.new Request(priority, new UUID(rand.nextLong(), rand.nextLong())));
                } catch (Exception e) {

                }
            }
            System.out.println("finished sending requests");
        }
    }

    private class Consumer implements Runnable {
        MyService service;
        boolean stopped = false;
        public Consumer(MyService service) {
            this.service = service;
        }


        public void run() {
            while(!stopped) {
                try {
                    MyService.Response response = service.consume();
                } catch(InterruptedException e) {
                    stopped = true;
                }
            }
        }

    }
    public static void main(String[] args) {

        if (args.length < 4) {
            System.out.println("specify args!");
            System.exit(0);
        }



        //first arg: number of producers
        int numProducers = Integer.parseInt(args[0]);
        //second arg: number of consumers (consumers and producers are distinguished)
        int numConsumers = Integer.parseInt(args[1]);
        //third arg: number of requests per producer
        int numRequests = Integer.parseInt(args[2]);
        //fourth arg: number of threads for request processor
        int numThreads = Integer.parseInt(args[3]);

        MyService service = new MyService(numThreads);

        Main main = new Main();


        long startTime = System.currentTimeMillis();

        ArrayList<Thread> producers = new ArrayList<>();
        for (int i = 0; i < numProducers; i++) {
            Thread thread = new Thread(main.new Producer(numRequests, service));
            thread.start();
            producers.add(thread);
        }
        ArrayList<Thread> consumers = new ArrayList<>();
        for (int i = 0; i < numConsumers; i++) {
            Thread thread = new Thread(main.new Consumer(service));
            thread.start();
            consumers.add(thread);
        }

        for (Thread thread : producers) {
            try {
                thread.join();
            } catch(InterruptedException e) {

            }
        }


        while(service.getNumberConsumed() != numProducers*numRequests) {
            //System.out.println(service.getNumberConsumed());
        }

        long endTime = System.currentTimeMillis();

        System.out.println("taken everything");

        long totalTime = endTime-startTime;
        NumberFormat formatter = new DecimalFormat("#0.00000");
        System.out.println("runtime: " + formatter.format(totalTime/1000d));


        service.stopThreads();
        for (Thread thread : consumers)
            thread.interrupt();

    }
}
