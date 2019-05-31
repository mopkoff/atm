package utilities;

import model.*;

import java.sql.*;


import java.time.LocalDate;
import java.util.*;

import model.Payment.PaymentStatus;


public class BankDBDriver {

    private String url;
    private Connection connection = null;

    public BankDBDriver(String fileName) {
        this.url = "jdbc:sqlite:" + fileName;
        //createNewDatabase(fileName);
    }

    private Connection connect() {
        // SQLite connection string
        Connection conn = null;
        try {

            if (this.connection == null || this.connection.isClosed())
                conn = DriverManager.getConnection(url);
            else
                conn = this.connection;
        } catch (SQLException e) {
            System.out.println("Connection failed. " + e.getMessage());
        }
        return conn;
    }

    public void startTransaction() throws SQLException {
        this.connection = this.connect();
        this.connection.setAutoCommit(false);
    }

    public void rollbackTransaction() throws SQLException {
        if (this.connection != null) {
            this.connection.rollback();
        }
    }

    public void commitTransaction() throws SQLException {
        this.connection.commit();
        this.connection.close();
        this.connection = null;
    }

    public void createTables() {
        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement()) {
            // order of creating is important due to references
            stmt.execute(Client.createTableStatement);
            stmt.execute(Bank.createTableStatement);
            stmt.execute(Account.createTableStatement);
            stmt.execute(Payment.createTableStatement);
            stmt.execute(Card.createTableStatement);
        } catch (SQLException e) {
            System.out.println(e.getMessage());

        }
    }

    public Bank addBank(Bank bank) {
        String sql = "INSERT INTO banks(id,name) VALUES(?,?)";
        String name = bank.getName();
        String uuid = UUID.randomUUID().toString();
        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setString(2, name);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating bank failed, no rows affected.");
            }

            bank.setId(uuid);
        } catch (SQLException e) {
            System.out.println("Creating bank failed. " + e.getMessage());
        }
        return bank;
    }

    public Bank getBankById(String id) {
        String sql = "SELECT id, name "
                + "FROM banks WHERE id = ?";
        Bank resultBank = null;
        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            ResultSet queryResult = pstmt.executeQuery();
            while (queryResult.next()) {
                resultBank = new Bank(queryResult.getString("id"), queryResult.getString("name"));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return resultBank;
    }

    public List<Bank> getBanksByName(String name) {
        String sql = "SELECT id, name "
                + "FROM banks WHERE name IN ?";
        ArrayList<Bank> resultBanksArrayList = new ArrayList<Bank>();

        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // set the value
            pstmt.setString(1, name);
            //
            ResultSet queryResult = pstmt.executeQuery();
            // loop through the result set
            while (queryResult.next()) {
                resultBanksArrayList.add(new Bank(queryResult.getString("id"), queryResult.getString("name")));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return resultBanksArrayList;
    }

    public Client addClient(Client client) {
        String sql = "INSERT INTO clients(id,name,type) VALUES(?,?,?)";
        String name = client.getName();
        String uuid = UUID.randomUUID().toString();
        String type = client.getType().name();
        Connection conn = this.connect();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setString(2, name);
            pstmt.setString(3, type);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating client failed, no rows affected.");
            }
            client.setId(uuid);

        } catch (SQLException e) {
            System.out.println("Creating client failed. " + e.getMessage());
        }
        return client;
    }

    public List<Client> getClientsByName(String name) {
        String sql = "SELECT id, name, type"
                + "FROM clients WHERE name IN ?";
        ArrayList<Client> resultClientsArrayList = new ArrayList<Client>();

        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // set the value
            pstmt.setString(1, name);
            //
            ResultSet queryResult = pstmt.executeQuery();
            // loop through the result set
            while (queryResult.next()) {
                resultClientsArrayList.add(new Client(queryResult.getString("id"),
                        queryResult.getString("name"),
                        Client.Type.valueOf( queryResult.getString("type"))));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return resultClientsArrayList;
    }

    public Client getClientById(String id) {
        String sql = "SELECT id, name, type "
                + "FROM clients WHERE id = ?";
        Client resultClient = null;
        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            ResultSet queryResult = pstmt.executeQuery();
            // loop through the result set
            while (queryResult.next()) {
                resultClient = new Client(queryResult.getString("id"),
                        queryResult.getString("name"),
                        Client.Type.valueOf( queryResult.getString("type")));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return resultClient;
    }

    public Account addAccountForClient(Client client) {
        String sql = "INSERT INTO accounts(id, clientId) VALUES(?,?)";
        String client_id = client.getId();
        String uuid = UUID.randomUUID().toString();
        Connection conn = this.connect();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setString(2, client_id);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating account failed, no rows affected.");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return getAccountById(uuid);
    }

    public Account addAccount(Account account) {
        String sql = "INSERT INTO accounts(id, clientId) VALUES(?,?)";
        String client_id = account.getClientId();
        String uuid = UUID.randomUUID().toString();
        Connection conn = this.connect();

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, uuid);
            pstmt.setString(2, client_id);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating account failed, no rows affected.");
            }


            account.setId(uuid);
        } catch (SQLException e) {
            System.out.println("Creating account failed. " + e.getMessage());
        }
        return account;
    }

    public Account getAccountById(String id) {
        String sql = "SELECT id, clientId, balance "
                + "FROM accounts WHERE id = ?";
        Account resultAccount = null;
        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // set the value
            pstmt.setString(1, id);
            //
            ResultSet queryResult = pstmt.executeQuery();
            // loop through the result set
            while (queryResult.next()) {
                resultAccount = new Account(queryResult.getString("id"),
                        queryResult.getString("clientId"),
                        queryResult.getDouble("balance"));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return resultAccount;
    }

    public Account getAccountByClientName(String name) {
        String sql = "SELECT id, clientId, balance " +
                "FROM accounts WHERE clientId = " +
                "(SELECT clientId FROM clients WHERE name = ?)";
        Account resultAccount = null;
        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // set the value
            pstmt.setString(1, name);
            //
            ResultSet queryResult = pstmt.executeQuery();
            // loop through the result set
            while (queryResult.next()) {
                resultAccount = new Account(queryResult.getString("id"),
                        queryResult.getString("clientId"),
                        queryResult.getDouble("balance"));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return resultAccount;
    }

    public ArrayList<Account> getAvailableAccountsToCreatePayment(Account account) {
        String sql = "SELECT id, clientId, balance " +
                "FROM accounts WHERE id != ?";
        ArrayList<Account>accounts = new ArrayList<>();
        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // set the value
            pstmt.setString(1, account.getId());
            //
            ResultSet queryResult = pstmt.executeQuery();
            // loop through the result set
            while (queryResult.next()) {
                accounts.add( new Account(queryResult.getString("id"),
                        queryResult.getString("clientId"),
                        queryResult.getDouble("balance")));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return accounts;
    }

    public Account getAccountByCardNumber(String cardNumber) {
        String sql = "SELECT id, clientId, balance " +
                "FROM accounts WHERE id = " +
                "(SELECT accountId FROM cards WHERE cardNumber = ?)";
        Account resultAccount = null;
        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // set the value
            pstmt.setString(1, cardNumber);
            //
            ResultSet queryResult = pstmt.executeQuery();
            // loop through the result set
            while (queryResult.next()) {
                resultAccount = new Account(queryResult.getString("id"),
                        queryResult.getString("clientId"),
                        queryResult.getDouble("balance"));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return resultAccount;
    }

    public boolean decreaseAccountBalance(Account account, double amount){

        return increaseAccountBalance(account,-amount);
    }

    public boolean increaseAccountBalance(Account account, double amount){
        String sql = "UPDATE accounts SET balance = balance + ? WHERE id = ?";
        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setString(2, account.getId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating account balance failed attempts failed, no rows affected.");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return true;
    }

    public boolean updateAccountBalance(Account account, double amount){
        String sql = "UPDATE accounts SET balance = ? WHERE id = ?";
        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setString(2, account.getId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating account balance failed attempts failed, no rows affected.");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return true;
    }


    public Card addCard(Account account, Card card, int pin) {
        String sql = "INSERT INTO cards(id, cardNumber, pin, salt, expirationDate, bankId, accountId) VALUES(?,?,?,?,?,?,?)";
        String uuid = UUID.randomUUID().toString();
        PinHasher pinHasher = new PinHasher(pin);
        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setString(2, card.getCardNumber());
            pstmt.setString(3, pinHasher.getHash());
            pstmt.setBytes(4, pinHasher.getSalt());
            pstmt.setString(5, card.getExpirationDate().toString());
            pstmt.setString(6, card.getBank().getId());
            pstmt.setString(7, account.getId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating card failed, no rows affected.");
            }

            card.setId(uuid);
        } catch (SQLException e) {
            System.out.println("Creating card failed. " + e.getMessage());
        }
        return card;
    }

    public Card getCardByCardNumber(String cardNumber) {
        String sql = "SELECT id, cardNumber, failedAttempts, expirationDate, bankId, accountId "
                + "FROM cards WHERE cardNumber = ?";
        Card resultCard = null;

        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // set the value
            pstmt.setString(1, cardNumber);
            //
            ResultSet queryResult = pstmt.executeQuery();
            // loop through the result set
            while (queryResult.next()) {
                //long id, String cardNumber, int failedAttempts, Date expirationDate, Bank bank
                resultCard = new Card(queryResult.getString("id"),
                        queryResult.getString("cardNumber"),
                        queryResult.getInt("failedAttempts"),
                        LocalDate.parse(queryResult.getString("expirationDate")),
                        //inner query
                        getBankById(queryResult.getString("bankId")),
                        //inner query
                        getAccountById(queryResult.getString("accountId")));
            }

        } catch (SQLException e1) {
            System.out.println("Querying card failed. " + e1.getMessage());
            return null;
        }
        //commit transaction


        return resultCard;
    }

    public boolean checkCardPin(Card card, int pin) {
        String sql = "SELECT pin, salt FROM cards WHERE cardNumber = ?";
        boolean result = false;
        PinHasher pinHasher = null;
        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, card.getCardNumber());
            ResultSet queryResult = pstmt.executeQuery();
            while (queryResult.next()) {
                pinHasher = new PinHasher(pin, queryResult.getBytes("salt"));
                result = pinHasher.getHash().equals(queryResult.getString("pin"));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return result;
    }

    public void increaseCardFailedAttempts(Card card, int incFailedAttemptsBy) {
        String sql = "UPDATE cards SET failedAttempts = failedAttempts + ? WHERE cardNumber = ?";
        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, incFailedAttemptsBy);
            pstmt.setString(2, card.getCardNumber());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Increasing card failed attempts failed, no rows affected.");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void updateCardFailedAttempts(Card card, int newFailedAttempts) {
        String sql = "UPDATE cards SET failedAttempts = ? WHERE cardNumber = ?";
        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newFailedAttempts);
            pstmt.setString(2, card.getCardNumber());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Resetting card failed attempts failed, no rows affected.");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public Payment addPayments(Payment payment) {
        String sql = "INSERT INTO payments(id, fromId, toId, amount, paymentStatus) VALUES(?,?,?,?,?)";
        String uuid = UUID.randomUUID().toString();

        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setString(2, payment.getAccountFrom().getId());
            pstmt.setString(3, payment.getAccountTo().getId());
            pstmt.setDouble(4, payment.getAmount());
            pstmt.setString(5, payment.getPaymentStatus().name());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating payment failed, no rows affected.");
            }

            payment.setId(uuid);
        } catch (SQLException e) {
            System.out.println("Creating payment failed. " + e.getMessage());
        }
        return payment;
    }

    public ArrayList<Payment> getPaymentsByFromId(String fromId) {
        String sql = "SELECT id, fromId, toId, amount, paymentStatus "
                + "FROM payments WHERE fromId = ?";
        ArrayList<Payment> payments= new ArrayList<>();
        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // set the value
            pstmt.setString(1, fromId);
            //
            ResultSet queryResult = pstmt.executeQuery();
            // loop through the result set
            while (queryResult.next()) {
                //long id, String cardNumber, int failedAttempts, Date expirationDate, Bank bank
                payments.add(new Payment(queryResult.getString("id"),
                        getAccountById(queryResult.getString("fromId")),
                        getAccountById(queryResult.getString("toId")),
                        queryResult.getDouble("amount"),
                        PaymentStatus.valueOf( queryResult.getString("paymentStatus"))));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return payments;
    }

    public PaymentStatus updatePaymentStatus(Payment payment, PaymentStatus paymentStatus) {
        String sql = "UPDATE payments SET paymentStatus = ? WHERE id = ?";

        Connection conn = this.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // set the value
            pstmt.setString(1, paymentStatus.name());
            pstmt.setString(2, payment.getId());
            //
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating payment status failed, no rows affected.");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return PaymentStatus.Unknown;
        }
        return paymentStatus;
    }

    public Payment getPaymentById(String id) {
        String sql = "SELECT id, fromId, toId, amount, paymentStatus "
                + "FROM payments WHERE id = ?";
        Payment resultPayment = null;
        Connection conn = this.connect();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // set the value
            pstmt.setString(1, id);
            //
            ResultSet queryResult = pstmt.executeQuery();
            // loop through the result set
            while (queryResult.next()) {
                //long id, String cardNumber, int failedAttempts, Date expirationDate, Bank bank
                resultPayment = new Payment(queryResult.getString("id"),
                        getAccountById(queryResult.getString("fromId")),
                        getAccountById(queryResult.getString("toId")),
                        queryResult.getDouble("amount"),
                        PaymentStatus.valueOf( queryResult.getString("paymentStatus")));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return resultPayment;
    }


}