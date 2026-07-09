package com.win9x.shopandpay.data;

public class Currency {

    private final String id;
    private final String name;
    private final String symbol;
    private final boolean isDefault;
    private final String storage;

    public Currency(String id, String name, String symbol, boolean isDefault, String storage) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
        this.isDefault = isDefault;
        this.storage = storage;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public String getStorage() {
        return storage;
    }
}