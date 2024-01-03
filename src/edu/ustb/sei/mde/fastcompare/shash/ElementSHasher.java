package edu.ustb.sei.mde.fastcompare.shash;

import org.eclipse.emf.ecore.EObject;

public interface ElementSHasher<V extends SimHashValue> {
    V hash(EObject element);
    V zeroSHash();
}
