import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 Scheduling:

 We maintain a sorted treap of unique priorities, which are in queue.

 When we want to start a process in a thread, we choose random number from [1...SUM_OF_PRIORITIES].
 If we write prefix sums of priorities and X is our number, then we choose the first priority,
 prefix sum of which is greater or equal than X.

 Example:

 priorities = [1, 3, 5]
 prefix sums of priorities [1, 4, 9]

 X = 2, (is generated from [1...9])

 We choose prefix sum of priority 3.

 Why do we use such choosing? Let's look at example.

 X    1  2  3  4  5  6  7  8  9
 Priority    1  3  3  3  5  5  5  5  5

 We can see, that probability of choosing priority 5 is 5 times bigger than probability of choosing priority 1.
 Also, probability of choosing priority 5 is 5/3 times bigger than probability of choosing priority 3.

 Using such technique, we can achieve scheduling of requests processing, described in task.


 */

class TreapThreadPool {

    private TreapQueue workQueue;
    private volatile boolean isRunning = true;

    private LinkedBlockingQueue<MyService.Response> results;


    public TreapThreadPool(int nThreads, MyService service) {
        workQueue = new TreapQueue();
        results = new LinkedBlockingQueue<>();
        for (int i = 0; i < nThreads; i++) {
            new Thread(new TaskWorker(service)).start();
        }
    }

    public void execute(MyService.Request request) {
        if (isRunning) {
            workQueue.offer(request);
        }
    }

    public void executeTasks(ArrayList<MyService.Request> requests) {
        if (isRunning) {
            workQueue.drainTo(requests);
        }
    }

    public MyService.Response getResult() throws InterruptedException{
        return results.take();
    }

    public int numberTaken() {
        return workQueue.numberTaken;
    }

    public void shutdown() {
        isRunning = false;
    }

    private class TreapQueue {
        private Random rand = new Random();
        private HashMap<Integer, MyService.Request> mp = new HashMap<>();
        private Treap treap = new Treap();
        private ReentrantLock lock = new ReentrantLock();
        public volatile int numberTaken = 0;

        void offer(MyService.Request request) {
            lock.lock();



            mp.put(request.priority, request);
            treap.add(request.priority);

            lock.unlock();
        }

        void drainTo(ArrayList<MyService.Request> requests) {
            lock.lock();

            for (MyService.Request req : requests) {
                mp.put(req.priority, req);
                treap.add(req.priority);
            }

            lock.unlock();
        }

        MyService.Request poll() throws InterruptedException {

            MyService.Request req = null;

            lock.lockInterruptibly();
            if (treap.sum() != 0){
                long x = Math.abs(rand.nextLong()) % treap.sum() + 1;
                int priority = treap.lowerBound(x);
                req = mp.get(priority);
                mp.remove(priority);
                treap.remove(priority);
                ++numberTaken;
            }

            lock.unlock();

            return req;
        }
    }

    private class TaskWorker implements Runnable {

        MyService service;
        public TaskWorker(MyService service) {
            this.service = service;
        }

        private void processRequest(MyService.Request request) throws InterruptedException {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
                bb.putLong(request.payload.getLeastSignificantBits());
                bb.putLong(request.payload.getMostSignificantBits());
                byte[] digest = md.digest(bb.array());
                String hash = new String(digest, StandardCharsets.UTF_8);
                results.put(service.new Response(request, hash));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while(isRunning){
                    MyService.Request nextTask = workQueue.poll();
                    if (nextTask != null)
                        processRequest(nextTask);
                    //TimeUnit.SECONDS.sleep(1);
                }
            } catch (InterruptedException e) {

            }
        }
    }
}

class MyService {
    private int numberOfThreads;
    TreapThreadPool executorService;
    private boolean stopped = false;
    public MyService(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        this.executorService = new TreapThreadPool(numberOfThreads, this);
    }
    public class Request {
        public final int priority;
        public final UUID payload;
        public Request(int priority, UUID payload) {
            this.priority = priority;
            this.payload = payload;
        }
    }
    public class Response {
        public final Request request;
        public final String payload;
        public Response(Request request, String payload) {
            this.request = request;
            this.payload = payload;
        }
    }

    public void produce(Request request) throws Exception {
        if (stopped)
            throw new Exception("stopped");
        executorService.execute(request);
    }

    public void produceArray(ArrayList<Request> requests) throws Exception{
        if (stopped)
            throw new Exception("stopped");
        executorService.executeTasks(requests);
    }

    public Response consume() throws InterruptedException{
        return executorService.getResult();
    }

    public int getNumberConsumed() {
        return executorService.numberTaken();
    }

    public void stopThreads() {
        System.out.println("Stopped threads");
        stopped = true;
        executorService.shutdown();
    }


}
