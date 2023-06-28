package edu.ustb.sei.mde.fastcompare.config;

import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.fastcompare.ihash.IElementHasher;
import edu.ustb.sei.mde.fastcompare.shash.SElementHasher;

public class Hasher {
    private SElementHasher simhasher;
    private IElementHasher idhasher;
    

    public long computeSHash(EObject o) {
        return simhasher.hash(o);
    }

    public long computeIHash(EObject o) {
        return idhasher.hash(o);
    }
}
