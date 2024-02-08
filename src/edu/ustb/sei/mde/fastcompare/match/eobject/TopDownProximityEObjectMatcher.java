package edu.ustb.sei.mde.fastcompare.match.eobject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.index.ElementIndexAdapter;
import edu.ustb.sei.mde.fastcompare.index.ObjectIndex.Side;
import edu.ustb.sei.mde.fastcompare.utils.MatchUtil;
import edu.ustb.sei.mde.fastcompare.utils.ProfileCounter;
import edu.ustb.sei.mde.fastcompare.utils.Triple;

public class TopDownProximityEObjectMatcher extends ProximityEObjectMatcher {

    public TopDownProximityEObjectMatcher(MatcherConfigure configure) {
        super(configure);
    }

    @Override
    protected void matchIndexedObjects(Comparison comparison,
            Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
        matchList(comparison, roots.left, true, roots);
        matchList(comparison, roots.right, true, roots);
    }

    @Override
    protected Iterable<EObject> matchList(Comparison comparison, Iterable<EObject> todoList, boolean createUnmatches,
            Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
        // todoList is the objects on the same level of the same root
        // for each object, we do subtree match first
        //   if subtree match fully succeeds, remove this node.
        //   otherwise, we append its children to the next round
        // finally, we return the next-round objects

        List<EObject> notFullyMatched = new ArrayList<>(32);
        List<EObject> nextLevel = new ArrayList<>(32);

        while(true) {
            nextLevel.clear();
            notFullyMatched.clear();
    
            for(EObject eObj : todoList) {
                ElementIndexAdapter adapter = ElementIndexAdapter.getAdapter(eObj);
                if(adapter == null) continue;

                Match partialMatch = comparison.getMatch(eObj);
                if (!MatchUtil.isFullMatch(partialMatch)) {
                    partialMatch = trySubtreeMatch(comparison, eObj, partialMatch, createUnmatches, roots);
                    if(!MatchUtil.isFullMatch(partialMatch)) {
                        notFullyMatched.add(eObj);
                    }
                }
            }
    
            for(EObject eObj : notFullyMatched) {
                Match partialMatch = comparison.getMatch(eObj);
                tryFineMatch(comparison, eObj, partialMatch, createUnmatches);
                nextLevel.addAll(eObj.eContents());
            }
    
            if(nextLevel.isEmpty()) break;
            todoList = new ArrayList<>(nextLevel);
        }
        
        return Collections.emptyList();
    }

    private Match trySubtreeMatch(Comparison comparison, EObject a, Match partialMatchOfA, boolean createUnmatches, 
		Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
		Side aSide = eObjectsToSide.get(a);
		assert aSide != null;
		Side bSide = Side.LEFT;
		Side cSide = Side.RIGHT;
		if (aSide == Side.RIGHT) {
			bSide = Side.LEFT;
			cSide = Side.ORIGIN;
		} else if (aSide == Side.LEFT) {
			bSide = Side.RIGHT;
			cSide = Side.ORIGIN;
		} else if (aSide == Side.ORIGIN) {
			bSide = Side.LEFT;
			cSide = Side.RIGHT;
		}
		assert aSide != bSide;
		assert bSide != cSide;
		assert cSide != aSide;

		// sub-tree match
		partialMatchOfA = coarseGainedMatch(comparison, partialMatchOfA, a, aSide, bSide, cSide, roots);
        // if(partialMatchOfA != null) {
        //     counter.hit(a.eClass());
        // }
		assert MatchUtil.isValidPartialMatch(partialMatchOfA);
		return partialMatchOfA;
    }
    // static public ProfileCounter counter = new ProfileCounter();
    
    private boolean tryFineMatch(Comparison comparison, EObject a, Match partialMatchOfA, boolean createUnmatches) {
		Side aSide = eObjectsToSide.get(a);
		assert aSide != null;
		Side bSide = Side.LEFT;
		Side cSide = Side.RIGHT;
		if (aSide == Side.RIGHT) {
			bSide = Side.LEFT;
			cSide = Side.ORIGIN;
		} else if (aSide == Side.LEFT) {
			bSide = Side.RIGHT;
			cSide = Side.ORIGIN;
		} else if (aSide == Side.ORIGIN) {
			bSide = Side.LEFT;
			cSide = Side.RIGHT;
		}
		assert aSide != bSide;
		assert bSide != cSide;
		assert cSide != aSide;

		// sub-tree match
		return fineGainedMatch(comparison, partialMatchOfA, a, aSide, bSide, cSide, createUnmatches);
    }
    
}
