package org.farmanager.plugins.jdbc;

public class IdValuePair {
    private final int id;
    private final String value;


    public IdValuePair(int id, String value) {
        this.id = id;
        this.value = value;
    }


    public int getId() {
        return id;
    }

    public String getValue() {
        return value;
    }
}
