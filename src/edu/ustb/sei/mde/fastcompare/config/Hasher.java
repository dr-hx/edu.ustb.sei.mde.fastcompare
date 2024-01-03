package edu.ustb.sei.mde.fastcompare.config;

import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.fastcompare.ihash.ElementIHasher;
import edu.ustb.sei.mde.fastcompare.shash.DefaultElementSHasher;
import edu.ustb.sei.mde.fastcompare.shash.ElementSHasher;
import edu.ustb.sei.mde.fastcompare.shash.SimHashValue;

public class Hasher {
    private ElementSHasher<? extends SimHashValue> simhasher;
    private ElementIHasher idhasher;

    public Hasher(MatcherConfigure configure) {
        this.simhasher = new DefaultElementSHasher(configure);
        this.idhasher = new ElementIHasher(configure);
    }
    

    public SimHashValue computeSHash(EObject o) {
        return simhasher.hash(o);
    }

    public long computeIHash(EObject o) {
        return idhasher.hash(o);
    }

    public SimHashValue zeroSHash() {
        return simhasher.zeroSHash();
    }
}
