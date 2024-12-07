package edu.ustb.sei.mde.fastcompare.minhash;

import java.util.Iterator;
import java.util.function.Function;

public class MapIterator<V, T> implements Iterator<T> {
    private Function<V,T> mapFunction;
    private Iterator<V> source;

    public MapIterator(Function<V, T> mapFunction) {
        this.mapFunction = mapFunction;
    }

    public void setSource(Iterator<V> source) {
        this.source = source;
    }

    @Override
    public boolean hasNext() {
        return this.source.hasNext();
    }

    @Override
    public T next() {
        V next = this.source.next();
        return mapFunction.apply(next);
    }
    
}
