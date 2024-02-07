package edu.ustb.sei.mde.fastcompare.shash;

import org.eclipse.emf.ecore.EStructuralFeature;

public class StringHashFunction implements SHashFunction<String> {

    @Override
    public long rawHash(String value) {
        return bigramString(value);
    }

    @Override
    public long hash(EStructuralFeature feature, String value) {
        return rawHash(value);
    }
    

    static private long bigramHash(char a, char b) {
        return 1L << ((a * 31 + b) % 64);
    }

    static private long bigramString(String string) {
        if(string.length() == 0) return bigramHash('#', '#');
        else if(string.length() == 1) {
            final char ch0 = string.charAt(0);
            return bigramHash('#', ch0) | bigramHash(ch0,'#'); 
        } else {
            long hash = 0;
            char prev = '#';
            for(int i = 0; i< string.length(); i++) {
                char cur = string.charAt(i);
                hash |= bigramHash(prev, cur);
                prev = cur;
            }
            return hash | bigramHash(prev, '#');
        }

    }
}
