package edu.ustb.sei.mde.fastcompare.utils;

import java.util.function.Function;

public class SimpleLRUCache<K,V> {
    private K prevKey = null;
    private V prevValue = null;

    public V get(K key, Function<K,V> computer) {
        if(!CommonUtils.ObjectEqual(prevKey, key)) {
            prevKey = key;
            prevValue = computer.apply(key);
        }
        return prevValue;
    }
}
