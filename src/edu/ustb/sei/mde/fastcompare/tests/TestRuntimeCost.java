package edu.ustb.sei.mde.fastcompare.tests;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.ustb.sei.mde.fastcompare.utils.AutoLRUCache;
import edu.ustb.sei.mde.fastcompare.utils.SimpleKLRUCache;
import edu.ustb.sei.mde.fastcompare.utils.SimpleLRUCache;

public class TestRuntimeCost {
    private Timer timer;
    private Random random;

    public void initGlobal() {
        random = new Random();
    }

    public void init() {
        initGlobal();
        timer = new Timer();
    }

    public void output() {
        timer.printTimerInfo(System.out);
    }

    @Test
    public void testLRUCache() {
        init();
        Map<Integer, Integer> data = new HashMap<>();
        for(int i = 0; i < 100000; i++) {
            int r = random.nextInt();
            data.put(r, r);
        }

        SimpleLRUCache<Integer, Integer> simpleCache = new SimpleLRUCache<>();
        SimpleKLRUCache<Integer, Integer> simpleKCache = new SimpleKLRUCache<>(4);
        AutoLRUCache<Integer, Integer> autoCache = new AutoLRUCache<>(128, 128, 0.75f);

        //warm up
        
        profilingSimpleMap(data);
        profilingSimpleLRU(data, simpleCache);
        profilingSimpleKLRU(data, simpleKCache);
        profilingAutoLRU(data, autoCache);
        
        profilingSimpleLRU(data, simpleCache);
        profilingSimpleKLRU(data, simpleKCache);
        profilingAutoLRU(data, autoCache);
        profilingSimpleMap(data);
        // profiling

        profilingSimpleKLRU(data, simpleKCache);
        profilingAutoLRU(data, autoCache);
        profilingSimpleMap(data);
        profilingSimpleLRU(data, simpleCache);

        profilingAutoLRU(data, autoCache);
        profilingSimpleMap(data);
        profilingSimpleLRU(data, simpleCache);
        profilingSimpleKLRU(data, simpleKCache);



        timer.printTimerInfo(System.out);
    }

    private void profilingSimpleMap(Map<Integer, Integer> data) {
        timer.tic("simpleMap");
        for(int i = 0; i < 10000000; i++) {
            int k = random.nextInt();
            data.get(k);
        }
        timer.toc("simpleMap");
    }

    private void profilingSimpleKLRU(Map<Integer, Integer> data, SimpleKLRUCache<Integer, Integer> simpleKCache) {
        timer.tic("simpleKLRUCache");
        for(int i = 0; i < 10000000; i++) {
            int k = random.nextInt();
            simpleKCache.get(k, ()->data.get(k));
        }
        timer.toc("simpleKLRUCache");
    }

    private void profilingSimpleLRU(Map<Integer, Integer> data, SimpleLRUCache<Integer, Integer> simpleCache) {
        timer.tic("simpleLRUCache");
        for(int i = 0; i < 10000000; i++) {
            int k = random.nextInt();
            simpleCache.get(k, (l)->data.get(k));
        }
        timer.toc("simpleLRUCache");
    }

    private void profilingAutoLRU(Map<Integer, Integer> data, AutoLRUCache<Integer, Integer> simpleCache) {
        timer.tic("autoCache");
        for(int i = 0; i < 10000000; i++) {
            int k = random.nextInt();
            simpleCache.get(k, ()->data.get(k));
        }
        timer.toc("autoCache");
    }
}
