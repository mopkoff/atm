package utilities;

import model.BanknoteMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;

public class ConfigJsonDriver {

    private String configFilePath = "config.json";
    private JSONArray bankServices;
    private JSONArray atms;

    public ConfigJsonDriver() {
        readFile();
    }

    public ConfigJsonDriver(String configFilePath) {
        this.configFilePath = configFilePath;
        readFile();
    }

    private void readFile() {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(configFilePath));
            bankServices = (JSONArray) jsonObject.get("bankServices");
            atms = (JSONArray) jsonObject.get("ATMs");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BankServiceConfig getBankServiceConfig(String bankName) {

        for (JSONObject bankService : (Iterable<JSONObject>) bankServices) {
            if (bankService.get("bankName").equals(bankName)) {
                String serviceName = (String) bankService.get("serviceName");
                int servicePort = (int) (long) bankService.get("servicePort");
                String serverHostname = (String) bankService.get("serviceHostname");
                String serviceRootPath = (String) bankService.get("serviceRootPath");
                String serviceDBPath = (String) bankService.get("serviceDBPath");
                String mode = (String) bankService.get("mode");
                return new BankServiceConfig(bankName, serviceName, servicePort, serverHostname, serviceRootPath, serviceDBPath, mode);
            }
        }
        throw new NullPointerException("Bank service config for such bank name does not exist.");
    }

    public AtmConfig getAtmConfig(String atmName) {

        for (JSONObject atmMachine : (Iterable<JSONObject>) atms) {

            if (atmMachine.get("atmName").equals(atmName)) {
                String bankName = (String) atmMachine.get("bankName");
                JSONObject banknoteMapJSON = (JSONObject) atmMachine.get("banknoteMap");
                String mode = (String) atmMachine.get("mode");
                BanknoteMap banknoteMap = new BanknoteMap(
                        (int) (long) banknoteMapJSON.get("100"),
                        (int) (long) banknoteMapJSON.get("200"),
                        (int) (long) banknoteMapJSON.get("500"),
                        (int) (long) banknoteMapJSON.get("1000"),
                        (int) (long) banknoteMapJSON.get("5000")
                );

                BankServiceConfig bankServiceConfig = getBankServiceConfig(bankName);
                String serviceHostname = bankServiceConfig.serviceHostname;
                String servicePath = bankServiceConfig.serviceRootPath + bankServiceConfig.serviceName;
                return new AtmConfig(atmName, bankName, banknoteMap, serviceHostname, servicePath, mode);
            }

        }
        throw new NullPointerException("Atm config for such atm name does not exist.");
    }

    public String getAvailableAtmNames() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Avaiable ATM names:\n");
        for (JSONObject atm : (Iterable<JSONObject>) atms) {
            stringBuilder.append(atm.get("atmName"));
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    public String getAvailableBankServiceNames() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Avaiable bank names:\n");
        for (JSONObject bankService : (Iterable<JSONObject>) bankServices) {
            stringBuilder.append(bankService.get("bankName"));
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }


    public class BankServiceConfig {
        public final String bankName;
        public final String serviceName;
        public final int servicePort;
        public final String serviceHostname;
        public final String serviceRootPath;
        public final String serviceDBPath;
        public final String mode;

        public BankServiceConfig(String bankName,
                                 String serviceName,
                                 int servicePort,
                                 String serviceHostname,
                                 String serviceRootPath,
                                 String serviceDBPath, String mode) {
            this.bankName = bankName;
            this.serviceName = serviceName;
            this.servicePort = servicePort;
            this.serviceHostname = serviceHostname;
            this.serviceRootPath = serviceRootPath;
            this.serviceDBPath = serviceDBPath;
            this.mode = mode;
        }
    }

    public class AtmConfig {
        public final String atmName;
        public final String bankName;
        public final BanknoteMap banknoteMap;
        public final String serviceHostname;
        public final String servicePath;
        public final String mode;

        public AtmConfig(String atmName, String bankName, BanknoteMap banknoteMap, String serviceHostname, String servicePath, String mode) {
            this.atmName = atmName;
            this.bankName = bankName;
            this.banknoteMap = banknoteMap;
            this.serviceHostname = serviceHostname;
            this.servicePath = servicePath;
            this.mode = mode;
        }
    }


}
