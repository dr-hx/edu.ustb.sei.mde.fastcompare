package edu.ustb.sei.mde.fastcompare.index;

import java.util.Optional;
import java.util.Set;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;

public class NoFilterIndex implements ObjectFilterIndex {
    private Set<EObject> allObjects;

    public NoFilterIndex(MatcherConfigure configure) {
        allObjects = ObjectFilterIndex.createSet(null);
    }

    @Override
    public Iterable<EObject> allCandidates() {
        return allObjects;
    }

    @Override
    public Iterable<EObject> filterCandidates(Comparison inProgress, EObject eObj, Optional<EObject> candidateContainer, double threshold) {
        return allObjects;
    }

    @Override
    public void remove(EObject eObj) {
        allObjects.remove(eObj);
    }

    @Override
    public void index(EObject eObj) {
        allObjects.add(eObj);
    }

    @Override
    public void indexTree(EObject root) {
    }
    
}
