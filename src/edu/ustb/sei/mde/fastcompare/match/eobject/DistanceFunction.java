package edu.ustb.sei.mde.fastcompare.match.eobject;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.FeatureMap;

import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.utils.DiffUtil;
import edu.ustb.sei.mde.fastcompare.utils.ReferenceUtil;

public interface DistanceFunction {

	/**
	 * Return the distance between two EObjects. When the two objects should
	 * considered as completely different the implementation is expected to 
	 * return Double.MAX_VALUE.
	 * @param inProgress
	 * 			the comparison being processed right now. This might be
     *          used for the distance to retrieve other matches for instance.
	 * @param a
	 * 			the first object
	 * @param b
	 * 			the second object
	 * @param haveSameContainer
	 * 			a hint that tells whether a and b have the same container.
	 * 			It may be null, which means there is no hint.
	 * @return the distance between the two EObjects or Double.MAX_VALUE when the
     *         objects are considered too different to be the same.
	 */
    double distance(Comparison inProgress, EObject a, EObject b, Boolean haveSameContainer);

	/**
	 * Return the distance threshold of eObj
	 * @param eObj
	 * @return 
	 * 		the threshold
	 */
    double getThresholdAmount(EObject eObj);
	double getContainerThresholdRatio(EObject eObj);
    /**
     * Check that two objects are equals from the distance function point of view
     * (distance should be 0). You should prefer this method when you just want 
	 * to check objects are not equals enabling the distance to stop sooner.
     * 
     * @param inProgress
     *                   the comparison being processed right now. This might be
     *                   used for the distance to retrieve other matches for instance.
     * @param a
     *                   first object.
     * @param b
     *                   second object.
     * @return true of the two objects are equals, false otherwise.
     */
    boolean areIdentic(Comparison inProgress, EObject a, EObject b);

    MatcherConfigure getMatcherConfigure();

	/**
	 * A helper function to compute whether a and b have the same container
	 * by considering the hint same.
	 * @param same
	 * @param inProgress
	 * @param a
	 * @param b
	 * @return
	 */
	default boolean computeHaveSameContainer(Boolean same, Comparison inProgress, EObject a, EObject b) {
		if(same == null) return haveSameContainer(inProgress, a, b);
		else return same;
	}

	/**
	 * The function to compute whether a and b have the same container.
	 * @param inProgress
	 * @param a
	 * @param b
	 * @return
	 */
	boolean haveSameContainer(Comparison inProgress, EObject a, EObject b);

    static public boolean isReferencedByTheMatch(EObject eObj, Match match) {
		return match != null
				&& (match.getRight() == eObj || match.getLeft() == eObj || match.getOrigin() == eObj);
	}

	/**
	 * return the position in which an Object is contained in its parent list.
	 * 
	 * @param a
	 *          any EObject
	 * @return the position in which an Object is contained in its parent list, 0 if
	 *         there is no container
	 *         or if the reference is single valued.
	 */
	static public int getContainmentIndex(EObject a) {
		EStructuralFeature feat = a.eContainingFeature();
		EObject container = a.eContainer();
		int position = 0;
		if (container != null) {
			if (feat instanceof EAttribute) {
				position = indexFromFeatureMap(a, feat, container);
			} else if (feat != null) {
				if (feat.isMany()) {
					EList<?> eList = (EList<?>) ReferenceUtil.safeEGet(container, feat);
					position = eList.indexOf(a);
				}
			}
		}
		return position;
	}

	/**
	 * the position of the {@link EObject} a in its container featureMap.
	 * 
	 * @param a
	 *                  the {@link EObject}.
	 * @param feat
	 *                  the containing feature.
	 * @param container
	 *                  the containing EObject.
	 * @return the position of the {@link EObject} a in its container featureMap.
	 */
	static public int indexFromFeatureMap(EObject a, EStructuralFeature feat, EObject container) {
		FeatureMap featureMap = (FeatureMap) ReferenceUtil.safeEGet(container, feat);
		for (int i = 0, size = featureMap.size(); i < size; ++i) {
			if (featureMap.getValue(i) == a) {
				EStructuralFeature entryFeature = featureMap.getEStructuralFeature(i);
				if (DiffUtil.isContainmentReference(entryFeature)) {
					return i;
				}
			}
		}
		return 0;
	}
}