public class Account {
    private String ID;
    private int balance;
    private int numTransactions;
    private static final int STARTING_BALACE = 1000;

    public Account(String ID, int balance, int numTransactions){
        this.balance = balance;
        this.ID = ID;
        this.numTransactions = numTransactions;
    }

    public Account(String ID){
        this(ID, STARTING_BALACE, 0);
    }

    public synchronized int getBalance() {
        return balance;
    }

    public synchronized int getNumTransactions() {
        return numTransactions;
    }

    public String getID() {
        return ID;
    }

    public synchronized void changeBalance(int balance) {
        this.balance += balance;
        numTransactions++;
    }

    @Override
    public String toString(){
        String result = "";
        result += "Acct: " + getID();
        result += ";  Bal: " + getBalance();
        result += ";  Trans: " + getNumTransactions() + ";";
        return result;
    }

    private static class AccWorker extends Thread {
        private Account acc;

        public AccWorker(Account acc) {
            this.acc = acc;
        }

        public void run() {
            for (int i = 0; i < 1_000_000; i++) {
                acc.changeBalance(3);
            }
        }
    }

    public static void main(String[] args){
        Account acc = new Account("100");
        AccWorker AW1 = new AccWorker(acc);
        AccWorker AW2 = new AccWorker(acc);
        AccWorker AW3 = new AccWorker(acc);
        AccWorker AW4 = new AccWorker(acc);
        AW1.start();
        AW2.start();
        AW3.start();
        AW4.start();
        try {
            AW1.join();
            AW2.join();
            AW3.join();
            AW4.join();
        }
        catch (InterruptedException ignored) {}
        System.out.println(acc);
    }
}