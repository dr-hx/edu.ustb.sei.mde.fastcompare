package edu.ustb.sei.mde.fastcompare.match.eobject;

import java.util.Collection;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.utils.Triple;

public class TopDownProximityEObjectMatcher extends ProximityEObjectMatcher {

    public TopDownProximityEObjectMatcher(MatcherConfigure configure) {
        super(configure);
    }

    

    @Override
    protected void matchIndexedObjects(Comparison comparison,
            Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
        // TODO Auto-generated method stub
        super.matchIndexedObjects(comparison, roots);
    }

    @Override
    protected Iterable<EObject> matchList(Comparison comparison, Iterable<EObject> todoList, boolean createUnmatches,
            Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
        // TODO Auto-generated method stub
        return super.matchList(comparison, todoList, createUnmatches, roots);
    }

    
    
}
