package edu.ustb.sei.mde.fastcompare.utils;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;

import edu.ustb.sei.mde.fastcompare.index.ObjectIndex;

public class MatchUtil {
    static final private SimpleLRUCache<EObject, Match> matchCache = new SimpleLRUCache<>();
    static public boolean isFullyMatched(EObject object, Comparison inProgress) {
        Match m = getMatch(object, inProgress);
        return m != null && m.getLeft() != PSEUDO_MATCHED_OBJECT && m.getRight() != PSEUDO_MATCHED_OBJECT && m.getOrigin() != PSEUDO_MATCHED_OBJECT;
    }

    public static Match getMatch(EObject object, Comparison inProgress) {
        Match m = matchCache.get(object, (o)->{
            return inProgress.getMatch(object);
        });
        return m;
    }

    static public boolean isMatched(EObject object, Comparison inProgress, ObjectIndex.Side side) {
        Match m = getMatch(object, inProgress);
        if(m == null) return false;
        else {
            switch(side) {
                case LEFT: return m.getLeft() != PSEUDO_MATCHED_OBJECT;
                case RIGHT: return m.getRight() != PSEUDO_MATCHED_OBJECT;
                case ORIGIN: return m.getOrigin() != PSEUDO_MATCHED_OBJECT;
                default: return false;
            }
        }
    }

    static final public EObject PSEUDO_MATCHED_OBJECT = EcoreFactory.eINSTANCE.createEObject();
}
