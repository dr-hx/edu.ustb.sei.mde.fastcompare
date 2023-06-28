package edu.ustb.sei.mde.fastcompare.shash;

public class DoubleHashFunction implements SHashFunction<Double> {
    @Override
    public long rawHash(Double value) {
        return (long) (value * 100);
    }
}
