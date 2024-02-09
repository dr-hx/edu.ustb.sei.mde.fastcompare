package edu.ustb.sei.mde.fastcompare.shash;

public class HashLoHi implements SimHashValue {
    final public long high;
    final public long low;
    final private int highBitCount;
    final private int lowBitCount;

    public HashLoHi(long h, long l) {
        high = h;
        low = l;
        highBitCount = Hash64.computeBitCounts(h);
        lowBitCount = Hash64.computeBitCounts(l);
    }

    private double simHi(HashLoHi v) {
        int dc = Hash64.computeBitCounts(high ^ ((HashLoHi) v).high);
        if(dc == 0) return 1.0;
        double bc = (highBitCount + ((HashLoHi) v).highBitCount);
        return 1.0 - dc / bc;
    }

    private double simLo(HashLoHi v) {
        int dc = Hash64.computeBitCounts(low ^ ((HashLoHi) v).low);
        if(dc == 0) return 1.0;
        double bc = (lowBitCount + ((HashLoHi) v).lowBitCount);
        return 1.0 - dc / bc;
    }

    @Override
    public double similarity(SimHashValue value) {
        // TODO: we need new sim fun: 1 - |x xor y| / (|x| + |y|)
        if(value instanceof HashLoHi) {
            if(isZero() && ((HashLoHi) value).isZero()) {
                return 1.0;
            } else {
                double hs = simHi((HashLoHi) value);
                double ls = simLo((HashLoHi) value);
                return (8 * hs + ls) / 9.0;
            }
        } else {
            return 0;
        }
    }
    
    private boolean isZero() {
        return high == 0 && low == 0;
    }
}
