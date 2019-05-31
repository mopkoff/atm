package model;

import java.io.Serializable;

public class Account extends TableEntity {

    public static String createTableStatement = "CREATE TABLE IF NOT EXISTS accounts(\n"
            + "	id TEXT PRIMARY KEY,\n"
            + "	clientId TEXT NOT NULL,\n"
            + "	balance REAL default 0,\n"

            + " FOREIGN KEY(clientId) REFERENCES clients(id)\n"
            + ");";

    private String id;
    private String clientId;
    private double balance;

    public Account(String id, String clientId, double balance){
        this.id = id;
        this.clientId = clientId;
        this.balance = balance;
    }

    public Account(String clientId){
        this.clientId = clientId;
    }

    public Account(Client client){
        this.clientId = client.getId();
    }

    public String getClientId() {
        return clientId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
