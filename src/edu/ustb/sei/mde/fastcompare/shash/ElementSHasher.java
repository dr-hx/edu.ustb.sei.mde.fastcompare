package edu.ustb.sei.mde.fastcompare.shash;

import org.eclipse.emf.ecore.EObject;

public interface ElementSHasher {
    long hash(EObject element);
}
