package bankService;

import java.io.PrintStream;
import java.sql.SQLException;
import java.time.LocalDate;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import utilities.*;
import model.*;

public class BankService extends UnicastRemoteObject
        implements BankServerI {
    private Bank bank;
    private static final long serialVersionUID = 1L;
    private BankDBDriver dbDriver;
    private String bankServiceHostname;
    private String bankServiceName;
    private int port;
    private StreamDispatcher streamDispatcher;
    final private int maxFailedAttempts = 3;

    private enum MessageType {Error, Log}

    public BankService(BankDBDriver dbDriver, String bankServiceHostname, String bankServiceName, int port, String mode) throws RemoteException {
        super(port);

        switch (mode) {
            case "debug":
                this.streamDispatcher = new StreamDispatcher(
                        System.in,
                        System.out,
                        System.out,
                        System.out);
                break;

            case "test":
                this.streamDispatcher = new StreamDispatcher(
                        StreamDispatcher.getInputStreamFromFile("test/"+bankServiceName + ".scenario"),
                        System.out,
                        System.out,
                        System.out);
                break;

            case "release":
                this.streamDispatcher = new StreamDispatcher(
                        System.in,
                        System.out,
                        StreamDispatcher.getPrintStreamForFile("log/" + bankServiceName + ".log"),
                        StreamDispatcher.getPrintStreamForFile("log/" + bankServiceName + ".err"));
                break;
            default:
                this.streamDispatcher = new StreamDispatcher(
                        System.in,
                        System.out,
                        System.out,
                        System.out);
                break;

        }


        streamDispatcher.overrideSystemAll();
        this.bankServiceHostname = bankServiceHostname;
        this.bankServiceName = bankServiceName;
        this.port = port;
        this.dbDriver = dbDriver;
    }

    private void log(MessageType messageType, String message) {
        PrintStream printStream;
        switch (messageType) {
            case Error:
                printStream = streamDispatcher.getErrStream();
                break;
            case Log:
                printStream = streamDispatcher.getLogStream();
                break;
            default:
                printStream = streamDispatcher.getOutStream();
                break;
        }
        printStream.println(this.bankServiceName + " " + messageType.name() + ": " + message);
    }

    public int checkCardPin(Card card, int pin) {
        log(MessageType.Log, "Checking card " + card.getCardNumber() + " pin");
        int result = this.maxFailedAttempts;
        try {
            dbDriver.startTransaction();
            if (dbDriver.checkCardPin(card, pin))
                dbDriver.updateCardFailedAttempts(card, 0);
            else
                dbDriver.increaseCardFailedAttempts(card, 1);
            result = dbDriver.getCardByCardNumber(card.getCardNumber()).getFailedAttempts();
            dbDriver.commitTransaction();
        } catch (SQLException e) {
            try {
                dbDriver.rollbackTransaction();
            } catch (SQLException e1) {
                log(MessageType.Log, "Exception occurred. Watch log for extra information");
                e1.printStackTrace();
            }
            log(MessageType.Log, "Exception occurred. Watch log for extra information");
            e.printStackTrace();
        }

        return result;
    }

    public boolean isCardExpired(Card card) throws NullPointerException {
        log(MessageType.Log, "Validating card " + card.getCardNumber());
        Card resultCard = dbDriver.getCardByCardNumber(card.getCardNumber());
        if (resultCard == null)
            return true;
        return resultCard.getExpirationDate().isBefore(LocalDate.now());
    }

    public boolean isCardBlocked(Card card) {
        log(MessageType.Log, "Checking card " + card.getCardNumber() + " failed attempts");
        return dbDriver.getCardByCardNumber(card.getCardNumber()).getFailedAttempts() >= maxFailedAttempts;
    }

    public double getBalance(Card card) {
        log(MessageType.Log, "Checking card " + card.getCardNumber() + " balance");
        Account account = dbDriver.getAccountByCardNumber(card.getCardNumber());
        return account.getBalance();
    }

    public ArrayList<Payment> getCreatedPayments(Card card) {
        log(MessageType.Log, "Querying created payments for card " + card.getCardNumber());
        Account account = dbDriver.getCardByCardNumber(card.getCardNumber()).getAccount();
        ArrayList<Payment> payments = dbDriver.getPaymentsByFromId(account.getId());

        payments.removeIf(payment -> payment.getPaymentStatus() != Payment.PaymentStatus.Created);
        return payments;
    }

    public ArrayList<Payment> getNotCreatedPayments(Card card) {
        log(MessageType.Log, "Querying not created payments for card " + card.getCardNumber());
        Account account = dbDriver.getCardByCardNumber(card.getCardNumber()).getAccount();
        ArrayList<Payment> payments = dbDriver.getPaymentsByFromId(account.getId());

        payments.removeIf(payment -> payment.getPaymentStatus() == Payment.PaymentStatus.Created);
        return payments;
    }

    public Payment.PaymentStatus payPayment(Card card, Payment payment) {
        log(MessageType.Log, "Performing payment " + payment.getId() + " for card " + card.getCardNumber());
        Payment.PaymentStatus paymentStatus = dbDriver.getPaymentById(payment.getId()).getPaymentStatus();
        Account account = dbDriver.getAccountByCardNumber(card.getCardNumber());
        if (paymentStatus != Payment.PaymentStatus.Created)
            return paymentStatus;
        if (account.getBalance() < payment.getAmount())
            return Payment.PaymentStatus.Failed;

        dbDriver.updatePaymentStatus(payment, Payment.PaymentStatus.Processing);
        try {
            dbDriver.startTransaction();
            Account accountFrom = dbDriver.getAccountById(payment.getAccountFrom().getId());
            Account accountTo = dbDriver.getAccountById(payment.getAccountTo().getId());
            dbDriver.decreaseAccountBalance(accountFrom, payment.getAmount());
            dbDriver.increaseAccountBalance(accountTo, payment.getAmount());
            paymentStatus = dbDriver.updatePaymentStatus(payment, Payment.PaymentStatus.Confirmed);
            dbDriver.commitTransaction();
        } catch (SQLException e) {
            try {
                dbDriver.rollbackTransaction();
                paymentStatus = dbDriver.updatePaymentStatus(payment, Payment.PaymentStatus.Failed);
            } catch (SQLException e1) {
                log(MessageType.Log, "Exception occurred. Watch log for extra information");
                paymentStatus = dbDriver.updatePaymentStatus(payment, Payment.PaymentStatus.Failed);
                e1.printStackTrace();
            }
            log(MessageType.Log, "Exception occurred. Watch log for extra information");
            e.printStackTrace();
        }
        return paymentStatus;
    }

    public ArrayList<Account> getAvailableAccounts(Card card) {
        log(MessageType.Log, "Querying available payment receivers for card " + card.getCardNumber());
        Account account = dbDriver.getAccountByCardNumber(card.getCardNumber());
        return dbDriver.getAvailableAccountsToCreatePayment(account);
    }

    public Payment.PaymentStatus createPayment(Card card, Account reciever, double amount) throws RemoteException {
        log(MessageType.Log, "Creating payment from card " + card.getCardNumber() + " to account " + reciever.getId());
        Account accountFrom = dbDriver.getAccountByCardNumber(card.getCardNumber());
        return dbDriver.addPayments(new Payment(accountFrom, reciever, amount)).getPaymentStatus();
    }


    @Override
    public void deposit(Card card, int sum) throws RemoteException {
        log(MessageType.Log, "Performing deposit for card " + card.getCardNumber());
        try {
            dbDriver.startTransaction();

            Account account = dbDriver.getCardByCardNumber(card.getCardNumber()).getAccount();
            dbDriver.updateAccountBalance(account, account.getBalance() + sum);

            dbDriver.commitTransaction();
        } catch (SQLException e) {
            try {
                dbDriver.rollbackTransaction();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }

    }

    @Override
    public boolean withdraw(Card card, int sum) throws RemoteException {
        log(MessageType.Log, "Performing withdraw from card " + card.getCardNumber());
        try {
            dbDriver.startTransaction();

            Account account = dbDriver.getCardByCardNumber(card.getCardNumber()).getAccount();
            if (account.getBalance() < sum)
                return false;
            dbDriver.updateAccountBalance(account, account.getBalance() - sum);

            dbDriver.commitTransaction();
            return true;
        } catch (SQLException e) {
            try {
                dbDriver.rollbackTransaction();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
        return false;
    }

    public int getMaxFailedAttempts() throws RemoteException {
        return this.maxFailedAttempts;
    }


    public void start() {
        String RMI_HOSTNAME = "java.rmi.server.hostname";
        try {
            System.setProperty(RMI_HOSTNAME, this.bankServiceHostname);

            // Определение имени удаленного RMI объекта
            String serviceName = this.bankServiceName;
            log(MessageType.Log, "Initializing " + serviceName);

            Registry registry = LocateRegistry.createRegistry(this.port);
            registry.rebind(serviceName, this);


            log(MessageType.Log, "Start " + serviceName + " at " + bankServiceHostname + ":" + port);

        } catch (RemoteException e) {
            log(MessageType.Error, "RemoteException : " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            log(MessageType.Error, "RemoteException : " + e.getMessage());
            System.exit(2);
        }
    }

    public static void main(String[] args) throws RemoteException {
        if (args.length == 0) {
            System.out.println("Usage: \n" +
                    "1. To start bank service: bankService configFile bankName\n" +
                    "2. To show available bank names: bankService configFile");
            return;
        }
        String configFile = args[0];
        ConfigJsonDriver configJsonDriver = new ConfigJsonDriver(configFile);

        if (args.length == 1) {
            System.out.println(configJsonDriver.getAvailableBankServiceNames());
            return;
        }

        for (int i = 1; i < 2; ++i) {
            String bankName = args[i];
            ConfigJsonDriver.BankServiceConfig bankServiceConfig = configJsonDriver.getBankServiceConfig(bankName);
            new BankService(new BankDBDriver(
                    bankServiceConfig.serviceDBPath),
                    bankServiceConfig.serviceHostname,
                    bankServiceConfig.serviceName,
                    bankServiceConfig.servicePort,
                    bankServiceConfig.mode
            ).start();
        }


    }


}