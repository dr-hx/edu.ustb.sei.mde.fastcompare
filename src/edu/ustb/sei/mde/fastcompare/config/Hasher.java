package edu.ustb.sei.mde.fastcompare.config;

import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.fastcompare.ihash.ElementIHasher;
import edu.ustb.sei.mde.fastcompare.shash.ElementSHasher;

public class Hasher {
    private ElementSHasher simhasher;
    private ElementIHasher idhasher;
    

    public long computeSHash(EObject o) {
        return simhasher.hash(o);
    }

    public long computeIHash(EObject o) {
        return idhasher.hash(o);
    }
}
