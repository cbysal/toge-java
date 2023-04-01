package client.option;

import java.util.HashMap;

public class OptionPool {
    public static final String CC = "cc";
    private final HashMap<String, String> pool = new HashMap<>();

    public boolean containsKey(String key) {
        return pool.containsKey(key);
    }

    public String get(String key) {
        return pool.get(key);
    }

    public void put(String key, String value) {
        pool.put(key, value);
    }
}
