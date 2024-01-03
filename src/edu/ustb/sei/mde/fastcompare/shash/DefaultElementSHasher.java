package edu.ustb.sei.mde.fastcompare.shash;

import java.util.Arrays;
import java.util.Map.Entry;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import edu.ustb.sei.mde.fastcompare.config.ClassConfigure;
import edu.ustb.sei.mde.fastcompare.config.FeatureConfigure;
import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.config.WeightTable;

public class DefaultElementSHasher implements ElementSHasher<Hash64> {
    protected MatcherConfigure configure;
    protected int[] bitArray;

    public DefaultElementSHasher(MatcherConfigure configure) {
        this.configure = configure;
        this.bitArray = new int[64];
    }

    protected long enhance(long value) {
        final long low3 = (value & 0x3L) << 61;
        final long shift = value >>> 3;
        return value | shift | low3;
    }

    protected void merge(int[] array, long featureHash, int pos, int neg) {
        for(int i = 0; i < 64; i++) {
            long bit0 = featureHash & 0x1L;
            if(bit0 == 0) {
                array[i] += neg;
            } else {
                array[i] += pos;
            }
            featureHash >>>= 1;
        }
    }

    protected long toHash(int[] array) {
        long hash = 0;
        long bit = 1L;
        for(int i = 0; i < 64; i++) {
            if(array[0] > 0) {
                hash |= bit;
            }
            bit <<= 1;
        }
        return hash;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Hash64 hash(EObject element) {
        Arrays.fill(bitArray, 0, 64, 0);
        final EClass clazz = element.eClass();
        final ClassConfigure classConfigure = configure.getClassConfigure(clazz);
        for(Entry<EStructuralFeature, FeatureConfigure> entry : classConfigure.getConcernedFeatures()) {
            final FeatureConfigure featureConfigure = entry.getValue();
            final SHashFunction<Object> shasher = (SHashFunction<Object>) featureConfigure.getShasher();
            if(!featureConfigure.isNoShashing() && shasher != null) {
                final Object value = element.eGet(entry.getKey());
                if(value != null) {
                    long featureHash = shasher.hash(entry.getKey(), value);
                    if(featureConfigure.getPositiveHashWeight() >= WeightTable.MAJOR) {
                        featureHash = enhance(featureHash);
                    }
                    merge(bitArray, featureHash, featureConfigure.getPositiveHashWeight(), featureConfigure.getNegativeHashWeight());
                }
            }
        }
        return new Hash64(toHash(bitArray));
    }

    @Override
    public Hash64 zeroSHash() {
        return Hash64.ZERO_HASH;
    }
    
}
