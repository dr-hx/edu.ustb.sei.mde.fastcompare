package edu.ustb.sei.mde.fastcompare.utils;

import java.util.function.Supplier;

public class SimpleKLRUCache<K,V> {
    class CacheNode<NK, NV> {
        public NK key = null;
        public NV value = null;
        CacheNode<NK, NV> next = null;
    }

    public SimpleKLRUCache(int size) {
        head = new CacheNode<>();
        CacheNode<K,V> cur = head;
        for(int i=0;i<size-1;i++) {
            cur.next = new CacheNode<>();
            cur = cur.next;
        }
        cur.next = null;
    }

    private CacheNode<K,V> head;
    private void moveToFirst(CacheNode<K,V> prev, CacheNode<K,V> cur) {
        if(prev != null) {
            prev.next = cur.next;
            cur.next = head;
            head = cur;
        }
    }

    private CacheNode<K,V> findNode(K key) {
        CacheNode<K,V> prev = null;
        CacheNode<K,V> cur = head;

        while(!CommonUtils.ObjectEqual(cur.key, key)) {
            if(cur.next == null) break;
            prev = cur;
            cur = cur.next;
        }

        moveToFirst(prev, cur);
        return cur;
    }

    public V get(K key, Supplier<V> builder) {
        CacheNode<K,V> cur = findNode(key);
        if(!CommonUtils.ObjectEqual(cur.key, key)) {
            cur.key = key;
            cur.value = builder.get();
        }
        return cur.value;
    }
}
