package model;
import java.rmi.*;
import java.util.ArrayList;

/*
Банк
Содержит счета клиентов и счета компаний - получателей платежей.
Содержит информацию о привязке номеров карточек к номерам счетов.
* взаимодействие с Банком производится через удаленный вызов процедур (RMI лидо веб-сервис, либо сокет).
* содержит механизм проверки пинкода
 */
public interface BankServerI extends Remote {

    int checkCardPin(Card card, int pin)  throws RemoteException;

    double getBalance(Card card) throws RemoteException;
    boolean isCardExpired(Card card) throws RemoteException, NullPointerException;
    boolean isCardBlocked(Card card) throws RemoteException;
    public Payment.PaymentStatus payPayment(Card card, Payment payment) throws RemoteException;
    public ArrayList<Payment> getCreatedPayments(Card card) throws RemoteException;
    public ArrayList<Payment> getNotCreatedPayments(Card card) throws RemoteException;
    public Payment.PaymentStatus createPayment(Card card, Account reciever, double amount) throws RemoteException;
    int getMaxFailedAttempts()throws RemoteException;
    public ArrayList<Account> getAvailableAccounts(Card card) throws  RemoteException;
    public boolean withdraw(Card card, int sum) throws RemoteException;
    public void deposit(Card card, int sum) throws RemoteException;
}
