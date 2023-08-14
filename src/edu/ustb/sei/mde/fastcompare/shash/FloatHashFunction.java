package edu.ustb.sei.mde.fastcompare.shash;

public class FloatHashFunction implements SHashFunction<Float> {
    @Override
    public long rawHash(Float value) {
        return (long) (value * 100);
    }
}
