package edu.ustb.sei.mde.fastcompare.shash;

public class NumberHashFunction implements SHashFunction<Number> {
    @Override
    public long rawHash(Number value) {
        return value.longValue();
    }
}
