public class Transaction {
    private String senderID;
    private String receiverID;
    private int amount;

    public Transaction(String senderID, String receiverID, int amount){
        this.senderID = senderID;
        this.receiverID = receiverID;
        this.amount = amount;
    }

    public Transaction(Transaction another){
        this.senderID = another.senderID;
        this.receiverID = another.receiverID;
        this.amount = another.amount;
    }

    public boolean equals(Transaction another){
        return (this.senderID == another.senderID
                && this.receiverID == another.receiverID
                && this.amount == another.amount);
    }

    public int getAmount() {
        return amount;
    }

    public String getReceiverID() {
        return receiverID;
    }

    public String getSenderID() {
        return senderID;
    }
}