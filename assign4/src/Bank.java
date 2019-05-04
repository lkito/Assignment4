import java.util.*;
import java.io.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

public class Bank {
    private static final int Q_SIZE = 10;
    private Map<String, Account> accMap;
    private ArrayBlockingQueue<Transaction> bQue;
    private final Transaction nullTrans = new Transaction("-1", "0", 0);

    private class Worker extends Thread {
        private CountDownLatch signal;

        public Worker(CountDownLatch signal){
            this.signal = signal;
        }

        public void run() {
            while(true){
                Transaction curTrans = null;
                try {
                    curTrans = bQue.take();
                } catch(InterruptedException e) {};
                if(curTrans.equals(nullTrans)){
                    signal.countDown();
                    return;
                }
                int amount = curTrans.getAmount();
                String sender = curTrans.getSenderID();
                String receiver = curTrans.getReceiverID();
                accMap.get(sender).changeBalance(-amount);
                accMap.get(receiver).changeBalance(amount);
            }
        }
    }

    private void printResult(){
        for (Map.Entry<String, Account> entry : accMap.entrySet()) {
            System.out.println(entry.getValue());
        }
    }

    private void readData(String inputFile, int numWorkers){
        Scanner reader = null;
        try {
            reader = new Scanner(new File(inputFile));
        } catch(FileNotFoundException ex) {
            System.out.println("Unable to open file '" + inputFile + "'");
        }

        String from, to;
        int amount;
        while (reader.hasNext()){
            from = "" + reader.nextInt();
            to = "" + reader.nextInt();
            amount = reader.nextInt();
            if(!accMap.containsKey(from)){
                accMap.put(from, new Account(from));
            }
            if(!accMap.containsKey(to)){
                accMap.put(to, new Account(to));
            }
            try {
                bQue.put(new Transaction(from, to, amount));
            } catch(InterruptedException e) {};
        }
        reader.close();
        for(int i = 0; i < numWorkers; i++){
            try {
                bQue.put(new Transaction(nullTrans));
            } catch(InterruptedException e) {};
        }
    }

    private void initWorkers(int numWorkers, CountDownLatch signal){
        for(int i = 0; i < numWorkers; i++){
            Worker wk = new Worker(signal);
            wk.start();
        }
    }

    public Bank(String inputFile, int numWorkers){
        accMap = new HashMap<>();
        bQue = new ArrayBlockingQueue<>(Q_SIZE);
        CountDownLatch signal = new CountDownLatch(numWorkers);
        initWorkers(numWorkers, signal);
        readData(inputFile, numWorkers);
        try {
            signal.await();
        } catch (InterruptedException ex) {};
        printResult();
    }

    public static void main(String[] args){
        Bank b = new Bank(args[0], Integer.parseInt(args[1]));
    }
}



