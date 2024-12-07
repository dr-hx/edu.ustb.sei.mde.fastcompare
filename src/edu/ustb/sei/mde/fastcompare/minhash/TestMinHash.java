package edu.ustb.sei.mde.fastcompare.minhash;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.Test;

public class TestMinHash {
    @Test
    public void test() {
        EObjectMinHasher hasher = new EObjectMinHasher(16, 1 << 16);
        var cls = EcorePackage.eINSTANCE.getEClass();
        var dt = EcorePackage.eINSTANCE.getEDataType();
        var clsf = EcorePackage.eINSTANCE.getEClassifier();
        var attr = EcorePackage.eINSTANCE.getEAttribute();
        var ref = EcorePackage.eINSTANCE.getEReference();

        long start = System.nanoTime();
        EObjectMinHashValue h1 = hasher.hash(cls);
        EObjectMinHashValue h2 = hasher.hash(dt);
        EObjectMinHashValue h3 = hasher.hash(clsf);
        EObjectMinHashValue h4 = hasher.hash(attr);
        EObjectMinHashValue h5 = hasher.hash(ref);

        {
            for(int i=0;i<10000;i++) {
                EObjectMinHashValue h11 = hasher.hash(cls);
                EObjectMinHashValue h21 = hasher.hash(dt);
                EObjectMinHashValue h31 = hasher.hash(clsf);
                EObjectMinHashValue h41 = hasher.hash(attr);
                EObjectMinHashValue h51 = hasher.hash(ref);
            }
        }
        long end = System.nanoTime();

        System.out.println("Time "+(end - start) / 1000000.0);

        double s1 = EObjectMinHashValue.similarity(h1, h2);
        System.out.println(s1);

        double s2 = EObjectMinHashValue.similarity(h1, h3);
        System.out.println(s2);

        double s3 = EObjectMinHashValue.similarity(h1, h4);
        System.out.println(s3);

        double s4 = EObjectMinHashValue.similarity(h1, h5);
        System.out.println(s4);

        double s5 = EObjectMinHashValue.similarity(h4, h5);
        System.out.println(s5);
        
    }

    public double sim(int[] l, int[] r) {
        int c = 0;
        for(int i=0;i<l.length;i++) {
            if(l[i] == r[i]) c++;
        }
        return c / (double) l.length;
    }

    @Test
    public void testStringHash() {
        System.out.println((-3) % 2);

        String[] strs = new String[] {"EClass", "EClassifier", "EDataType", "EAttribute"};
        BiGramIterator bigram = new BiGramIterator();
        MinHashFunctionGroup group = new MinHashFunctionGroup(16, 0);
        for(var s1 : strs) {
            bigram.setSource(s1);
            group.reset();
            group.update(bigram);
            var h1p = group.getHash();
            int[] h1 = Arrays.copyOf(h1p, h1p.length);

            for(var s2 : strs) {
                bigram.setSource(s2);
                group.reset();
                group.update(bigram);
                var h2p = group.getHash();
                int[] h2 = Arrays.copyOf(h2p, h2p.length);

                double sim = sim(h1, h2);
                System.out.println(s1+"->"+s2+":"+sim);
            }
        }
    }
}
