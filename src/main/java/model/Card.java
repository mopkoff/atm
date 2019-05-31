package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/*

данные (Счет Клиента, количество купюр по номиналам) хранятся в БД (любая бесплатная БД, например mySQL),

* можно учесть ситуацию с банкоматом одного банка, карточкой другого банка и
  принадлежностью карточки к одной из платежных систем. Описание деталей такого взаимодействия общедоступно.

Карточка клиента
(со своим PIN кодом и номером, привязанным к счету в Банке)
! пинкод не хранится на карточке, следовательно банкомат не может его считать напрямую
* пинкод не хранится в банке в явном (незашифрованном, нехэшированном) виде
*
*
 */
public class Card extends TableEntity   {

    public static String createTableStatement = "CREATE TABLE IF NOT EXISTS cards (\n"
            + "	id TEXT PRIMARY KEY,\n"
            + "	cardNumber TEXT NOT NULL,\n"
            + "	pin TEXT NOT NULL,\n"
            + "	salt BLOB NOT NULL,\n"
            + "	failedAttempts INTEGER default 0,\n"
            + "	expirationDate TEXT NOT NULL,\n"
            + "	accountId TEXT NOT NULL,\n"
            + "	bankId TEXT NOT NULL,\n"
            + " nowUsed INTEGER default 0,\n"

            + " FOREIGN KEY(bankId) REFERENCES banks(id),\n"
            + " FOREIGN KEY(accountId) REFERENCES accounts(id)\n"
            + ");";

    private String id;
    private String cardNumber;
    private int failedAttempts;
    private LocalDate expirationDate;
    private Bank bank;
    private Account account;

    public Card(String cardNumber, Bank bank, LocalDate expirationDate){
        this.id = "";
        this.cardNumber = cardNumber;
        this.bank = bank;
        this.expirationDate = expirationDate;
        this.failedAttempts = 0;
        this.account = null;
    }

    public Card(String id, String cardNumber, int failedAttempts, LocalDate expirationDate, Bank bank, Account account){
        this.id = id;
        this.cardNumber = cardNumber;
        this.failedAttempts = failedAttempts;
        this.expirationDate = expirationDate;
        this.bank = bank;
        this.account = account;
    }


    public Bank getBank() {
        return bank;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getId(){
        return id;
    }
    public String getCardNumber() {
        return cardNumber;
    }
    public LocalDate getExpirationDate() {
        return expirationDate;
    }
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setExpirationDateFromString(String expirationDateString) {
        this.expirationDate = LocalDate.parse(expirationDateString);
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }


    @Override
    public boolean equals(Object obj)
    {
        Card card = (Card)obj;
        if (this.getId().length() > 0) {
            return this.getId().equals(card.getId());
        }
        return this.getCardNumber().equals(card.getCardNumber());
    }
}
