package edu.ustb.sei.mde.fastcompare.shash;

public class Hash64 {
    final public long hash;
    final public int bitCounts;
    
    public Hash64(long h) {
        this.hash = h;
        this.bitCounts = Hash64.computeBitCounts(h);
    }

    static final public Hash64 ZERO_HASH = new Hash64(0);

    

    @Override
    public int hashCode() {
        return (int) (hash ^ (hash >>> 32));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Hash64 other = (Hash64) obj;
        return (hash == other.hash);
    }

    static public final double similarity(Hash64 l, Hash64 r) {
        if(l.hash == 0 && r.hash == 0) return 1.0;
        int uc = computeBitCounts(l.hash & r.hash);
        if(uc == 0) return 0.0;
        double sqrt =Math.sqrt(l.bitCounts * r.bitCounts);
        return uc / sqrt;
    }

    static final long m1 = 0x5555555555555555L; // binary: 0101...
    static final long m2 = 0x3333333333333333L; // binary: 00110011..
    static final long m4 = 0x0F0F0F0F0F0F0F0FL; // binary: 4 zeros, 4 ones ...
    static final long m8 = 0x00FF00FF00FF00FFL; // binary: 8 zeros, 8 ones ...
    static final long m16 = 0x0000FFFF0000FFFFL; // binary: 16 zeros, 16 ones ...
    static final long m32 = 0x00000000FFFFFFFFL; // binary: 32 zeros, 32 ones
    static final long hff = 0xFFFFFFFFFFFFFFFFL; // binary: all ones
    static final long h01 = 0x0101010101010101L;

    static public int computeBitCounts(long x) {
        x = (x & m1) + ((x >> 1) & m1); // put count of each 2 bits into those 2 bits
        x = (x & m2) + ((x >> 2) & m2); // put count of each 4 bits into those 4 bits
        x = (x & m4) + ((x >> 4) & m4); // put count of each 8 bits into those 8 bits
        x = (x & m8) + ((x >> 8) & m8); // put count of each 16 bits into those 16 bits
        x = (x & m16) + ((x >> 16) & m16); // put count of each 32 bits into those 32 bits
        x = (x & m32) + ((x >> 32) & m32); // put count of each 64 bits into those 64 bits
        return (int) x;
    }
}
