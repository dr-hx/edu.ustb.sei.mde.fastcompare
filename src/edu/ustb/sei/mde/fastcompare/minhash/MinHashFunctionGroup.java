package edu.ustb.sei.mde.fastcompare.minhash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class MinHashFunctionGroup {
    private MinHashFunction[] hashFunctions;
    private int[] buffer;
    private final int maxHash;
    private final int prime;

    public MinHashFunctionGroup(int K, int maxHash, long seed) {
        System.out.println("seed "+seed);
        
        Random random = new Random(seed);

        this.hashFunctions = new MinHashFunction[K];
        this.buffer = new int[K];

        this.prime = P_BIGRAM[random.nextInt(P_BIGRAM.length)];
        List<Integer> aList = new ArrayList<>();
        List<Integer> bList = new ArrayList<>();

        for(int i=0;i<K;i++) {
            int a = random.nextInt(this.prime);
            int b = random.nextInt(this.prime);
            if(aList.contains(a) || bList.contains(b)) {
                i --;
                continue;
            }
            aList.add(a);
            bList.add(b);
        }

        for(int i=0;i<K;i++) {
            hashFunctions[i] = new MinHashFunction(aList.get(i), bList.get(i), this.prime);
        }
        if(maxHash <= 1) this.maxHash = this.prime;
        else this.maxHash = maxHash;
    }

    public MinHashFunctionGroup(int K, int maxHash) {
        this(K, maxHash, System.currentTimeMillis());
    }

    static public final int P_BIGRAM[] = {9000907, 9000913, 9000931, 9000961, 9000983, 9000989, 9000997, 9001001, 9001033, 9001039, 9001063, 9001093, 9001121, 9001163, 9001169, 9001171, 9001189};

    public void reset() {
        Arrays.fill(this.buffer, this.prime);
    }

    protected int wrap(int value) {
        return value % this.prime;
    }

    public void update(Iterator<Integer> set) {
        while(set.hasNext()) {
            // int value = wrap(set.next());
            // for(int j=0; j<hashFunctions.length; j++) {
            //     int h = hashFunctions[j].compute(value) % maxHash;
            //     if(buffer[j] > h) buffer[j] = h;
            // }
            update(set.next());
        }
    }

    public void update(Iterable<Integer> set) {
        set.forEach(value->{
            // int v = wrap(value);
            // for(int j=0; j<hashFunctions.length; j++) {
            //     int h = hashFunctions[j].compute(v) % maxHash;
            //     if(buffer[j] > h) buffer[j] = h;
            // }
            update(value);
        });
    }

    public void update(int value) {
        int v = wrap(value);
        for(int j=0; j<hashFunctions.length; j++) {
            int h = hashFunctions[j].compute(v) % maxHash;
            if(buffer[j] > h) buffer[j] = h;
        }
    }

    public int[] getHash() {
        return buffer;
    }
}
