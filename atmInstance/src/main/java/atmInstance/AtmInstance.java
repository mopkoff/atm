package atmInstance;

import utilities.*;
import model.*;

import java.io.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Scanner;

/*
Банкомат
Выдает Клиенту деньги по Карточке, списывая их со Счета Клиента в Банке
* Позволяет оплачивать зарегистрированные в Банке платежи, с точностью до копейки.
* Реализует конечный автомат с таблицей переходов
  Следит за расходом купюр, соблюдая пропорции.
! Содержит и выдает купюры, кратные 100 рублям.
! За раз выдаёт не более 40 купюр.
! Не выдает клиенту денег больше, чем есть на его счете.
! Принимает у Клиента деньги на счет.
! Информирует пользователя о результате каждой операции.
Проверяет, валидность срока действия карточки.
Запрашивает ПИН код у Клиента при начале сеанса.
Реализует другие очевидные операции.
* Рассмотреть выделение внутренних сущностей банкомата (диспенсер, принтер, купюроприменик) в отдельные классы
* Если в приложении используются транзакции, предлагается рассмотреть
   вовлечение вышеупомянутых сущностей в эти транзацкии

   Задание выполняется на Java SE (без использования механизмов Java EE)),
данные (Счет Клиента, количество купюр по номиналам) хранятся в БД (любая бесплатная БД, например mySQL),
вариант - банкомат может хранить свою информацию о купюрах в текстовом (* вариант в JSON формате) файле.
 */
public class AtmInstance {

    private String atmName;
    private String bankName;
    private Dispencer dispencer;
    private BillAcceptor billAcceptor;
    private BanknoteMap banknoteMap;
    private BankServerI bankService;

    private enum ATMState {Idle, CardCheck, CodeEntry, OperationChoice, BalanceCheck, Withdraw, Deposit, CreatePayment, PayPayment, ReturnCard, Error, TurningOff, PaymentHistory}

    private enum MessageType {Error, Log, Info, InputRequest}

    private ATMState currentState = ATMState.Idle;
    private StreamDispatcher streamDispatcher;
    private Card currentCard;


    public static AtmInstance getAtmInstance(String atmName, String bankName, BanknoteMap initBanknoteMap, String bankServiceHostname, String bankServicePath, String mode) {

        try {
            return new AtmInstance(atmName, bankName, initBanknoteMap, bankServiceHostname, bankServicePath, mode);
        } catch (NotBoundException e) {
            System.out.println("Server lookup failed. Maybe the server " + bankServiceHostname + " at " + bankServicePath + " is not turned on.");
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private AtmInstance(String atmName, String bankName, BanknoteMap initBanknoteMap, String bankServiceHostname, String bankServicePath, String mode) throws IOException, NotBoundException {

        String RMI_HOSTNAME = "java.rmi.server.hostname";
        this.atmName = atmName;
        this.bankName = bankName;
        this.banknoteMap = initBanknoteMap;
        this.dispencer = new Dispencer();
        this.billAcceptor = new BillAcceptor();

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
                        StreamDispatcher.getInputStreamFromFile("test/" + atmName + ".scenario"),
                        System.out,
                        System.out,
                        System.out);
                System.out.println("using test/" + atmName + ".scenario as input");
                break;

            case "release":
                this.streamDispatcher = new StreamDispatcher(
                        System.in,
                        System.out,
                        StreamDispatcher.getPrintStreamForFile("log/" + atmName + ".log"),
                        StreamDispatcher.getPrintStreamForFile("log/" + atmName + ".err"));
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
        System.setProperty(RMI_HOSTNAME, bankServiceHostname);
        // URL удаленного объекта

        bankService = (BankServerI) Naming.lookup(bankServicePath);

    }

    private void setState(ATMState state) {
        printATM(MessageType.Log, "State switched from " + currentState.name() + " to " + state.name());
        currentState = state;
    }

    private void chooseOperation() {
        String choiceRequest = "\nChoose operation:" +
                "\n1. Balance check." +
                "\n2. Withdraw." +
                "\n3. Deposit." +
                "\n4. Create payment." +
                "\n5. Pay payment." +
                "\n6. Return card." +
                "\n0. Turn off ATM.";
        String input = inputString(choiceRequest);

        switch (input) {
            case "1":
                setState(ATMState.BalanceCheck);
                break;
            case "2":
                setState(ATMState.Withdraw);
                break;
            case "3":
                setState(ATMState.Deposit);
                break;
            case "4":
                setState(ATMState.CreatePayment);
                break;
            case "5":
                setState(ATMState.PayPayment);
                break;
            case "6":
                setState(ATMState.ReturnCard);
                break;
            case "0":
                setState(ATMState.TurningOff);
                break;
            default:
                printATM(MessageType.Error, "Unsupported operation");
                break;

        }
    }

    private String inputString(String str) {
        printATM(MessageType.InputRequest, str);
        Scanner s = new Scanner(this.streamDispatcher.getInStream());
        String input = s.next();
        printATM(MessageType.Info, "RECIEVED: " + input + "\n");
        return input;
    }

    private int inputUnsignedInt(String str) {
        printATM(MessageType.InputRequest, str);
        Scanner s = new Scanner(this.streamDispatcher.getInStream());
        String input = s.next();
        printATM(MessageType.Info, "RECIEVED: " + input + "\n");
        return Integer.parseUnsignedInt(input);
    }

    private double inputPositiveDouble(String str) {
        printATM(MessageType.InputRequest, str);
        Scanner s = new Scanner(this.streamDispatcher.getInStream());
        String input = s.next();
        double result = Double.parseDouble(input);
        if (result <= 0)
            throw new IllegalArgumentException("Value should be positive");
        return result;
    }

    private void printATM(MessageType messageType, String message) {
        PrintStream printStream;
        switch (messageType) {
            case Error:
                printStream = streamDispatcher.getOutStream();
                break;
            case Log:
                printStream = streamDispatcher.getLogStream();
                break;
            case Info:
                printStream = streamDispatcher.getOutStream();
                break;
            case InputRequest:
                printStream = streamDispatcher.getOutStream();
                break;
            default:
                printStream = streamDispatcher.getOutStream();
                break;
        }
        printStream.println(this.atmName + " " + messageType.name() + ": " + message);
    }

    private void performIdle() {
        currentCard = new Card(inputString("Card cardNumber"), new Bank(), LocalDate.now().plusYears(1));
        setState(ATMState.CardCheck);
    }

    private void performCardCheck() {
        try {
            if (bankService.isCardExpired(currentCard)) {
                printATM(MessageType.Info, "Card " + currentCard.getCardNumber() + " is invalid");
                setState(ATMState.Idle);
            } else {
                printATM(MessageType.Info, "Card " + currentCard.getCardNumber() + " successfully checked");
                setState(ATMState.CodeEntry);
            }
        } catch (RemoteException e) {
            printATM(MessageType.Error, "Card with such number does not exist");
            setState(ATMState.Idle);
        }
    }

    private void performCodeEntry() throws RemoteException {
        if (bankService.isCardBlocked(currentCard)) {
            printATM(MessageType.Info, "Card " + currentCard.getCardNumber() + " is blocked");
            setState(ATMState.Idle);
        }
        int pin = inputUnsignedInt("Pin");
        int checkingCardResult = bankService.checkCardPin(currentCard, pin);
        if (checkingCardResult == 0) {
            setState(ATMState.OperationChoice);
        } else
            printATM(MessageType.Info, "Bad pin for card " + currentCard.getCardNumber() + ". Attempts left: " + (bankService.getMaxFailedAttempts() - checkingCardResult));
    }

    private void performBalanceCheck() throws RemoteException {
        double balance = bankService.getBalance(currentCard);
        printATM(MessageType.Log, this.banknoteMap.toString());
        printATM(MessageType.Info, "Balance: " + balance + " for card " + currentCard.getCardNumber());
        setState(ATMState.OperationChoice);
    }

    private void performWithdraw() {
        int sum_to_withdraw = inputUnsignedInt("Sum to withdraw. If input value not multiplied by banknote value, the lowest possible will be withdrawn");
        BanknoteMap result = this.dispencer.withdraw(sum_to_withdraw);
        if (result != null)
            printATM(MessageType.Info, "Successfully withdrawn " + result.toString() + " from card " + currentCard.getCardNumber());
        setState(ATMState.OperationChoice);
    }

    private void performDeposit() {
        String depositRequest = "Sum to deposit. Put 0 to banknote value to finish deposit";
        printATM(MessageType.InputRequest, depositRequest);

        int sumToDeposit = billAcceptor.deposit();

        try {
            bankService.deposit(currentCard, sumToDeposit);
            this.banknoteMap.addBanknoteMap(billAcceptor.banknoteMapToDeposit);
        } catch (RemoteException e) {
            printATM(MessageType.Log, e.getMessage());
            printATM(MessageType.Error, "Deposit " + sumToDeposit + " to card " + currentCard.getCardNumber() + " failed");
            setState(ATMState.OperationChoice);
        }

        printATM(MessageType.Info, "Successfully deposited " + sumToDeposit + " to card " + currentCard.getCardNumber());
        setState(ATMState.OperationChoice);
    }

    private class BillAcceptor {
        BanknoteMap banknoteMapToDeposit = new BanknoteMap();

        private int deposit() {
            while (true) {
                int banknoteValue = inputUnsignedInt("Banknote value");
                if (banknoteValue == 0)
                    return banknoteMapToDeposit.getBanknotesSum();

                if (banknoteMap.isValidBaknoteValue(banknoteValue)) {
                    int banknoteCount = inputUnsignedInt("Banknote count");
                    banknoteMapToDeposit.putBanknotes(banknoteValue, banknoteCount);
                } else
                    printATM(MessageType.Error, "Invalid banknote value");

            }
        }
    }

    private class Dispencer {

        BanknoteMap banknoteMapToWithdraw = new BanknoteMap();

        private BanknoteMap withdraw(int sum) {
            if (sum > banknoteMap.getBanknotesSum()) {
                printATM(MessageType.Error, "ATM does not contains enough banknotes");
                return null;
            }
            BanknoteMap banknoteMapToWithdraw = banknoteMap.calculate(sum, true);

            if (banknoteMapToWithdraw.getBanknotesCount() > 40) {
                printATM(MessageType.Error, "Banknote count to give reached 40. Try to reduce required sum or change dispense politic");
                banknoteMap.addBanknoteMap(banknoteMapToWithdraw);
                return null;
            }
            try {
                bankService.withdraw(currentCard, banknoteMapToWithdraw.getBanknotesSum());

            } catch (RemoteException e) {
                e.printStackTrace();
                return null;
            }
            return banknoteMapToWithdraw;
        }

    }

    private void performPayPayment() throws RemoteException {
        ArrayList<Payment> payments = bankService.getCreatedPayments(this.currentCard);
        if (payments.isEmpty()) {
            printATM(MessageType.Info, "No available payments");
            setState(ATMState.OperationChoice);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder("\n");
        int i = 0;
        for (Payment payment : payments) {
            stringBuilder.append(++i).append(". ").append(payment).append("\n");
        }
        stringBuilder.append(0).append(". ").append("Abort pay").append("\n");
        printATM(MessageType.Info, stringBuilder.toString());

        int paymentIndexToPay = inputUnsignedInt("Payment index to pay for");
        while (paymentIndexToPay > i) {
            printATM(MessageType.Info, "Incorrect index");
            paymentIndexToPay = inputUnsignedInt("Payment index to pay for");
        }
        if (paymentIndexToPay == 0) {
            setState(ATMState.OperationChoice);
            return;
        }

        bankService.payPayment(this.currentCard, payments.get(paymentIndexToPay - 1));
    }

    private void performPaymentHistory() throws RemoteException {
        ArrayList<Payment> payments = bankService.getNotCreatedPayments(this.currentCard);
        if (payments.isEmpty()) {
            printATM(MessageType.Info, "No available payments");
            setState(ATMState.OperationChoice);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder("\n");
        int i = 0;
        for (Payment payment : payments) {
            stringBuilder.append(++i).append(". ").append(payment).append("\n");
        }
        printATM(MessageType.Info, stringBuilder.toString());

        int paymentIndexToPay = inputUnsignedInt("Press 0 to return back to menu");
        while (paymentIndexToPay != 0) {
            printATM(MessageType.Info, "Incorrect choice");
            paymentIndexToPay = inputUnsignedInt("Press 0 to return back to menu");
        }
        setState(ATMState.OperationChoice);

    }

    private void performCreatePayment() throws RemoteException {
        ArrayList<Account> accounts = bankService.getAvailableAccounts(this.currentCard);
        if (accounts.isEmpty()) {
            printATM(MessageType.Info, "No available accounts");
            setState(ATMState.OperationChoice);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder("\n");
        int i = 0;
        for (Account account : accounts) {
            stringBuilder.append(++i).append(". ").append(account.getId()).append("\n");
        }

        stringBuilder.append(0).append(". ").append("Abort payment creation").append("\n");
        printATM(MessageType.Info, stringBuilder.toString());

        int accountIndexRecieverReciever = inputUnsignedInt("Account index reciever");
        while (accountIndexRecieverReciever > i) {
            printATM(MessageType.Info, "Incorrect index");
            accountIndexRecieverReciever = inputUnsignedInt("Account index reciever");
        }
        if (accountIndexRecieverReciever == 0) {
            setState(ATMState.OperationChoice);
            return;
        }

        double amount = inputPositiveDouble("Input amount");

        bankService.createPayment(this.currentCard, accounts.get(accountIndexRecieverReciever - 1), amount);
    }

    private void performError() {
        printATM(MessageType.Error, "Unhandled exception");
        setState(ATMState.OperationChoice);
    }

    private void performReturnCard() {

        printATM(MessageType.Info, "Card " + currentCard.getCardNumber() + " returned");
        currentCard = null;
        setState(ATMState.Idle);
    }

    private void performDefault() {
        printATM(MessageType.Error, "Unhandled state");
        setState(ATMState.OperationChoice);
    }

    public void start() {
        boolean working = true;
        while (working) {
            try {
                switch (currentState) {
                    case Idle:
                        performIdle();
                        break;
                    case CardCheck:
                        performCardCheck();
                        break;
                    case CodeEntry:
                        performCodeEntry();
                        break;
                    case OperationChoice:
                        chooseOperation();
                        break;
                    case BalanceCheck:
                        performBalanceCheck();
                        break;
                    case Withdraw:
                        performWithdraw();
                        break;
                    case Deposit:
                        performDeposit();
                        break;
                    case PayPayment:
                        performPayPayment();
                        break;
                    case PaymentHistory:
                        performPaymentHistory();
                        break;
                    case CreatePayment:
                        performCreatePayment();
                        break;
                    case ReturnCard:
                        performReturnCard();
                        break;

                    case Error:
                        performError();
                        break;

                    case TurningOff:
                        working = false;
                        printATM(MessageType.Info, "Turning off");
                        break;
                    default:
                        performDefault();
                        break;
                }


            } catch (RemoteException | NumberFormatException e) {
                printATM(MessageType.Error, e.getMessage());
            }
            catch (Exception e){
                e.printStackTrace();
                setState(ATMState.Error);
            }
        }
    }


    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Usage: \n" +
                    "1. To start atmInstance: atmInstance configFile atmName\n" +
                    "2. To show available atmInstance names: bankService configFile");
            return;
        }

        String configFile = args[0];
        ConfigJsonDriver configJsonDriver = new ConfigJsonDriver(configFile);

        if (args.length == 1) {
            System.out.println(configJsonDriver.getAvailableAtmNames());
            return;
        }
        String atmName = args[1];

        ConfigJsonDriver.AtmConfig atmConfig = configJsonDriver.getAtmConfig(atmName);
        AtmInstance atmInstance = AtmInstance.getAtmInstance(
                atmConfig.atmName,
                atmConfig.bankName,
                atmConfig.banknoteMap,
                atmConfig.serviceHostname,
                atmConfig.servicePath,
                atmConfig.mode);
        if (atmInstance != null)
            atmInstance.start();

    }

}









































