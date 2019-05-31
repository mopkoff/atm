package model;


import org.json.simple.JSONObject;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class BanknoteMap {

    private TreeMap<Integer, Integer> banknoteMap;

    public BanknoteMap(int c100, int c200, int c500, int c1000, int c5000) {
        banknoteMap = new TreeMap<>();
        banknoteMap.put(100, c100);
        banknoteMap.put(200, c200);
        banknoteMap.put(500, c500);
        banknoteMap.put(1000, c1000);
        banknoteMap.put(5000, c5000);
    }

    public BanknoteMap(TreeMap<Integer, Integer> banknoteMap) {
        this.banknoteMap = banknoteMap;
    }

    public BanknoteMap() {
        banknoteMap = new TreeMap<>();
        banknoteMap.put(100, 0);
        banknoteMap.put(200, 0);
        banknoteMap.put(500, 0);
        banknoteMap.put(1000, 0);
        banknoteMap.put(5000, 0);
    }

    public boolean isValidBaknoteValue(int key) {
        return banknoteMap.containsKey(key);
    }

    public int putBanknotes(int value, int count) {
        int addedSum=0;
        try {
            banknoteMap.replace(value, banknoteMap.get(value) + count);
            addedSum = value*count;
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Banknote value not valid");
        }
        return addedSum;
    }

    public int addBanknoteMap(BanknoteMap banknoteMapToAdd) {
        int addedSum=0;
        for (Map.Entry<Integer, Integer> banknotes : banknoteMapToAdd.banknoteMap.entrySet()) {
            int banknoteValue = banknotes.getKey();
            int banknoteCount = banknotes.getValue();
            addedSum += this.putBanknotes(banknoteValue, banknoteCount);
        }
        return addedSum;
    }

    public int subBanknoteMap(BanknoteMap banknoteMapToSub) {
        int subtractedSum=0;
        for (Map.Entry<Integer, Integer> banknotes : banknoteMapToSub.banknoteMap.entrySet()) {
            int banknoteValue = banknotes.getKey();
            int banknoteCount = banknotes.getValue();
            subtractedSum += this.putBanknotes(banknoteValue, -banknoteCount);
        }
        return subtractedSum;
    }

    public void getBanknotes(int value, int count) {
        banknoteMap.put(value, banknoteMap.get(value) - count);
    }

    public int getBanknotesSum() {
        int sum = 0;
        for (Map.Entry<Integer, Integer> banknotes : banknoteMap.entrySet()) {
            sum += banknotes.getKey() * banknotes.getValue();
        }
        return sum;
    }

    public int getBanknotesCount() {
        int sum = 0;
        for (Map.Entry<Integer, Integer> banknotes : banknoteMap.entrySet()) {
            sum += banknotes.getValue();
        }
        return sum;
    }

    private void reverseBanknoteMap() {
        TreeMap<Integer, Integer> newBanknoteMap = new TreeMap<>(Collections.reverseOrder());
        newBanknoteMap.putAll(this.banknoteMap);
        this.banknoteMap = newBanknoteMap;
    }

    public BanknoteMap calculate(int sum, boolean dispense_politic_max_value) {
        BanknoteMap resultBanknoteMap = new BanknoteMap();
        if (dispense_politic_max_value)
            this.reverseBanknoteMap();

        for (Map.Entry<Integer, Integer> banknotes : this.banknoteMap.entrySet()) {
            int banknoteValue = banknotes.getKey();
            int banknoteCount = banknotes.getValue();
            while (banknoteCount > 0 && sum >= banknoteValue) {
                sum -= banknoteValue;
                this.getBanknotes(banknoteValue, 1);
                resultBanknoteMap.putBanknotes(banknoteValue, 1);
                banknoteCount = banknotes.getValue();
            }
        }

        if (dispense_politic_max_value)
            this.reverseBanknoteMap();

        return resultBanknoteMap;

    }

    @Override
    public String toString() {
        JSONObject obj = new JSONObject();

        for (Map.Entry<Integer, Integer> banknotes : banknoteMap.entrySet()) {
            obj.put(banknotes.getKey().toString(), banknotes.getValue());
        }

        return obj.toString();
    }
    /*
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{\n\t");
        for (Map.Entry<Integer, Integer> banknotes : banknoteMap.entrySet()) {
                stringBuilder.append("\"" + banknotes.getKey() + "\": " + banknotes.getValue() + "\"\n");
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }*/
}
