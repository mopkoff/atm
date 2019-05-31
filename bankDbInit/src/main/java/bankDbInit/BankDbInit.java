package bankDbInit;
import model.*;
import utilities.BankDBDriver;
import utilities.ConfigJsonDriver;

import java.time.LocalDate;


public class BankDbInit {
    private static void init(String DBPath){

        BankDBDriver app = new BankDBDriver(DBPath);
        app.createTables();

        Client client1 = new Client("Petya", Client.Type.human);
        Client client2 = new Client("Vasya", Client.Type.human);
        Client client3 = new Client("ATM-VTB-1234", Client.Type.atm);
        Client client4 = new Client("ATM-SBER-1234", Client.Type.atm);
        Client client5 = new Client("ATM-ALPHA-1234", Client.Type.atm);

        client1 = app.addClient(client1);
        client2 = app.addClient(client2);
        client3 = app.addClient(client3);
        client4 = app.addClient(client4);
        client5 = app.addClient(client5);
        //filling field "id"

        Bank bank1 = new Bank("Sberbank");
        Bank bank2 = new Bank("VTB");
        Bank bank3 = new Bank("ALPHA");
        bank1 = app.addBank(bank1);
        bank2 = app.addBank(bank2);
        bank3 = app.addBank(bank3);


        Account account1 = new Account(client1);
        account1 = app.addAccount(account1);
        Account account2 = app.addAccountForClient(client2);
        Account account3 = app.addAccountForClient(client3);
        Account account4 = app.addAccountForClient(client4);
        Account account5 = app.addAccountForClient(client5);

        Card card1 = new Card("12345", bank1, LocalDate.now().plusYears(2));
        Card card2 = new Card("12345678", bank2, LocalDate.now());

        card1 = app.addCard(account1, card1, 1234);
        card2 = app.addCard(account2, card2, 1234);

        Payment payment1 = new Payment(account1,account2, 100);
        Payment payment2 = new Payment(account1,account2, 500);
        Payment payment3 = new Payment(account2,account1, 1000);
        Payment payment4 = new Payment(account2,account1, 5000);

    }
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: \n" +
                    "1. To fill bank service database: bankService configFile bankName1 bankName2 ... bankNameN\n" +
                    "2. To show available bank names: bankService configFile");
            return;
        }
        String configFile = args[0];
        ConfigJsonDriver configJsonDriver = new ConfigJsonDriver(configFile);

        if (args.length == 1) {
            System.out.println(configJsonDriver.getAvailableBankServiceNames());
            return;
        }

        for (int i = 1; i < args.length; ++i){
            String bankName = args[i];
            ConfigJsonDriver.BankServiceConfig bankServiceConfig = configJsonDriver.getBankServiceConfig(bankName);
            System.out.println("Creating db at " + bankServiceConfig.serviceDBPath);

            init(bankServiceConfig.serviceDBPath);
        }

        //JSON parser object to parse read file

    }
}
