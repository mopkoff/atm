package model;

import java.io.Serializable;

public class Bank extends TableEntity {

    public static String createTableStatement = "CREATE TABLE IF NOT EXISTS banks(\n"
            + "	id TEXT PRIMARY KEY,\n"
            + "	name TEXT NOT NULL\n"
            + ");";

    private String id;
    private String name;

    public Bank() {
    }


    public Bank(String bankName) {
        this.name = bankName;
    }

    public Bank(String id, String name) {
        this.id = id;
        this.name = name;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


}
