package edu.ustb.sei.mde.fastcompare.utils;

import java.util.Map;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;

import edu.ustb.sei.mde.fastcompare.index.ObjectIndex;

public class MatchUtil {
    static final private SimpleLRUCache<EObject, Match> matchCache = new SimpleLRUCache<>();

    /**
     * The method will not cache the match
     * @param object
     * @param inProgress
     * @return
     */
    static public boolean isFullyMatched(EObject object, Comparison inProgress) {
        Match m = inProgress.getMatch(object);
        return isFullMatch(m);
    }

    public static boolean isFullMatch(Match m) {
        return m != null && m.getLeft() != PSEUDO_MATCHED_OBJECT && m.getRight() != PSEUDO_MATCHED_OBJECT && m.getOrigin() != PSEUDO_MATCHED_OBJECT;
    }

    public static boolean isValidPartialMatch(Match m) {
        return m == null || !(m.getLeft() == m.getRight() || m.getLeft() == m.getOrigin() || m.getRight() == m.getOrigin());
    }

    public static Match simpleGetMatch(EObject object, Comparison inProgress) {
        return inProgress.getMatch(object);
    }

    /**
     * This method will cache the match
     * @param object
     * @param inProgress
     * @return
     */
    public static Match getMatch(EObject object, Comparison inProgress) {
        if(object == null) return null;
        Match m = matchCache.get(object, inProgress::getMatch);
        return m;
    }

    public static EObject getMatchedObject(Match m, ObjectIndex.Side sideToFind) {
        switch (sideToFind) {
			case RIGHT:
				return m.getRight();
			case LEFT:
				return m.getLeft();
			case ORIGIN:
				return m.getOrigin();
			default:
                return null;
		}
    }

    // /**
    //  * This method will cache the match
    //  * @param object
    //  * @param inProgress
    //  * @param side
    //  * @return
    //  */
    // static public boolean isMatched(EObject object, Comparison inProgress, ObjectIndex.Side side) {
    //     Match m = getMatch(object, inProgress);
    //     return isMatched(m, side);
    // }

    public static boolean isMatched(Match m, ObjectIndex.Side side) {
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

    public static void setMatch(Match m, EObject o, ObjectIndex.Side side) {
        switch(side) {
            case LEFT:
                m.setLeft(o);break;
            case RIGHT:
                m.setRight(o);break;
            case ORIGIN:
                m.setOrigin(o);break;
            default:
                break;
        }
    }

    public static boolean tryFillMatched(Match m, EObject o, ObjectIndex.Side side) {
        if(m == null) return false;
        else {
            switch(side) {
                case LEFT:
                    if(m.getLeft() == PSEUDO_MATCHED_OBJECT) {
                        m.setLeft(o);
                        return true;
                    } else return false;
                case RIGHT:
                    if(m.getRight() == PSEUDO_MATCHED_OBJECT) {
                        m.setRight(o);
                        return true;
                    } else return false;
                case ORIGIN:
                    if(m.getOrigin() == PSEUDO_MATCHED_OBJECT) {
                        m.setOrigin(o);
                        return true;
                    } else return false;
                default: return false;
            }
        }
    }

    public static boolean isMatched(Map<ObjectIndex.Side,EObject> m, ObjectIndex.Side side) {
        return m.getOrDefault(side, PSEUDO_MATCHED_OBJECT) != PSEUDO_MATCHED_OBJECT;
    }

    static final public EObject PSEUDO_MATCHED_OBJECT = EcoreFactory.eINSTANCE.createEObject();

    public static boolean ofTheSameType(EObject... arr) {
        EClass clazz = null;
        for(EObject o : arr) {
            if(clazz == null) clazz = o.eClass();
            else if(clazz != o.eClass()) return false;
        }
        return true;
    }
}
