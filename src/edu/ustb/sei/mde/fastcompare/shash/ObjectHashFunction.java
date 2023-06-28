package edu.ustb.sei.mde.fastcompare.shash;

public class ObjectHashFunction implements SHashFunction<Object> {

    @Override
    public long rawHash(Object value) {
        if(value == null) return 0L;
        return value.hashCode();
    }
    
}
