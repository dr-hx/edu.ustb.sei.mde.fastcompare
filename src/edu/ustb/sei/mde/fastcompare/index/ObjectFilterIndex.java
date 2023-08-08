package edu.ustb.sei.mde.fastcompare.index;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.ecore.EObject;


public interface ObjectFilterIndex {
    Iterable<EObject> allCandidates();
    Iterable<EObject> filterCandidates(Comparison inProgress, EObject eObj, Optional<EObject> candidateContainer, double threshold);
    void remove(EObject eObj);
    void index(EObject eObj);
    void indexTree(EObject root);
    
    public static <T> Set<EObject> createSet(T k) {
        return new LinkedHashSet<>(128);
    }
}
