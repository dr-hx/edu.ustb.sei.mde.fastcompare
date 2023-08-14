package edu.ustb.sei.mde.fastcompare.shash;

import java.util.List;

import org.eclipse.emf.ecore.EStructuralFeature;

public class ListHashFunction implements SHashFunction<List<?>> {
    final private SHashFunction<?> innerHash;

    public ListHashFunction(SHashFunction<?> innerHash) {
        this.innerHash = innerHash;
    }

    @Override
    public long rawHash(List<?> value) {
        throw new UnsupportedOperationException("Unimplemented method 'rawHash'");
    }

    @Override
    @SuppressWarnings("all")
    public long hash(EStructuralFeature feature, List<?> value) {
        long hash = 0L;

        for(Object v : value) {
            long vh = ((SHashFunction<Object>)innerHash).hash(feature, v);
            hash |= vh;
        }

        return hash;
    }
    
}
