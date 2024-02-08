package edu.ustb.sei.mde.fastcompare.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ProfileCounter {
    private Map<Object, Integer> counter = new HashMap<>();

    public void hit(Object key) {
        counter.compute(key, (o, i) -> {
            if(i == null) return 1;
            else return i + 1;
        });
    }

    public void clear() {
        counter.clear();
    }

    public void dump() {
        for(Entry<Object, Integer> e : counter.entrySet()) {
            System.out.println(e.getKey() + " -> "+ e.getValue());
        }
    }
}
