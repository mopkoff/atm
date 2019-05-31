package model;

import java.io.Serializable;

/*
Клиент
Снимает деньги со счета (часть или все сразу), кладет деньги на счет,
* оплачивает платежи.
 */
public class Client extends TableEntity {

    public static String createTableStatement = "CREATE TABLE IF NOT EXISTS clients(\n"
            + "	id TEXT PRIMARY KEY,\n"
            + "	name TEXT NOT NULL,\n"

            + "	type TEXT NOT NULL\n"
            + ");";


    public enum Type {human, atm, company}

    private String id;
    private String Name;
    private Type type;

    public Client(String Name, Type type){
        this.id = "";
        this.Name = Name;
        this.type = type;
    }
    public Client(String id, String Name, Type type){
        this.Name = Name;
        this.id = id;
        this.type = type;
    }
    public String getId(){
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return Name;
    }
    public void setName(String name) {
        Name = name;
    }


    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
    public void withdraw(Card card, int sum){}

    public void deposit(Card card, int sum){}

}
