package model;

import org.json.simple.JSONObject;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;

public class Payment extends TableEntity {

    public static String createTableStatement = "CREATE TABLE IF NOT EXISTS payments(\n"
            + "	id TEXT PRIMARY KEY,\n"
            + "	fromId TEXT NOT NULL,\n"
            + "	toId TEXT NOT NULL,\n"
            + "	amount REAL DEFAULT 0,\n"
            + "	paymentStatus TEXT NOT NULL,\n"

            + " FOREIGN KEY(fromId) REFERENCES accounts(id),\n"
            + " FOREIGN KEY(toId) REFERENCES accounts(id)\n"
            + ");";



    public static enum PaymentStatus {Unknown, Created, Processing, Confirmed, Failed}

    private String id;
    private Account accountFrom;
    private Account accountTo;
    private double amount;
    private PaymentStatus paymentStatus;

    public Payment(Account accountFrom, Account accountTo, double amount){
        this.id = "";
        this.accountFrom = accountFrom;
        this.accountTo = accountTo;
        this.amount = amount;
        this.paymentStatus = PaymentStatus.Created;
    }

    public Payment(String id,Account accountFrom, Account accountTo, double amount, PaymentStatus paymentStatus){
        this.id = id;
        this.accountFrom = accountFrom;
        this.accountTo = accountTo;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Account getAccountFrom() {
        return accountFrom;
    }

    public void setAccountFrom(Account accountFrom) {
        this.accountFrom = accountFrom;
    }

    public Account getAccountTo() {
        return accountTo;
    }

    public void setAccountTo(Account accountTo) {
        this.accountTo = accountTo;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    @Override
    public String toString() {

        JSONObject obj = new JSONObject();
        obj.put("Payment Id", this.id);
        obj.put("From", this.accountFrom.getId());
        obj.put("To", this.accountTo.getId());
        obj.put("Amount", this.amount);
        obj.put("Payment Status", this.paymentStatus.name());


        return obj.toString();
    }
}