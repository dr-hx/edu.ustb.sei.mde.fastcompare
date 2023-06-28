package edu.ustb.sei.mde.fastcompare.shash;


public class StringHashFunction implements SHashFunction<String> {

    @Override
    public long rawHash(String value) {
        return bigramString(value);
    }
    

    static private long bigramHash(char a, char b) {
        return 1L << ((a * 31 + b) % 64);
    }

    static private long bigramString(String string) {
        if(string.length() == 0) return 0;
        else if(string.length() == 1) {
            return 1L << (string.charAt(0) % 64);
        } else {
            long hash = 0;
            char prev = string.charAt(0);
            for(int i = 1; i< string.length(); i++) {
                char cur = string.charAt(i);
                hash |= bigramHash(prev, cur);
                prev = cur;
            }
            return hash;
        }

    }
}
