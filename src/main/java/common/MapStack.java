package common;

import java.util.*;

public class MapStack<K, V> {
    private final List<Map<K, V>> stack = new ArrayList<>();

    protected MapStack() {
        stack.add(new HashMap<>());
    }

    protected V get(K key) {
        ListIterator<Map<K, V>> iterator = stack.listIterator(stack.size());
        while (iterator.hasPrevious()) {
            Map<K, V> table = iterator.previous();
            if (table.containsKey(key)) {
                return table.get(key);
            }
        }
        throw new RuntimeException("No such element in stack: " + key);
    }

    protected void putFirst(K key, V value) {
        Map<K, V> map = stack.get(0);
        map.put(key, value);
    }

    protected void putLast(K key, V value) {
        Map<K, V> map = stack.get(stack.size() - 1);
        map.put(key, value);
    }

    public void in() {
        stack.add(new HashMap<>());
    }

    public void out() {
        stack.remove(stack.size() - 1);
    }
}
