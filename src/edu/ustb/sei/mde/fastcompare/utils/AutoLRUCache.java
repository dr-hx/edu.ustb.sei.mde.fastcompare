package edu.ustb.sei.mde.fastcompare.utils;

import java.util.function.Function;

public class AutoLRUCache<K,V> {

    protected Function<K,V> builder;
    protected AccessBasedLRUCache<K,V> cache;

    public AutoLRUCache(Function<K,V> builder, int maxSize, int initialCapacity, float loadFactor) {
        this.cache = new AccessBasedLRUCache<>(maxSize, initialCapacity, loadFactor);
        this.builder = builder;
    }

    public V get(K key) {
        return cache.computeIfAbsent(key, k->this.builder.apply(k));
    }
}
