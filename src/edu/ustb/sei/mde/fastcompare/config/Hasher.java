package edu.ustb.sei.mde.fastcompare.config;

import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.fastcompare.ihash.ElementIHasher;
import edu.ustb.sei.mde.fastcompare.shash.ElementSHasher;
import edu.ustb.sei.mde.fastcompare.shash.Hash64;

public class Hasher {
    private ElementSHasher simhasher;
    private ElementIHasher idhasher;
    

    public Hash64 computeSHash(EObject o) {
        return new Hash64(simhasher.hash(o));
    }

    public long computeIHash(EObject o) {
        return idhasher.hash(o);
    }
}
