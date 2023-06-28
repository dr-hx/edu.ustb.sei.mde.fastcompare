package edu.ustb.sei.mde.fastcompare.match;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.ecore.EObject;

public interface DistanceFunction {
    /**
     * Return the distance between two EObjects. When the two objects should
     * considered as completely
     * different the implementation is expected to return Double.MAX_VALUE.
     * 
     * @param inProgress
     *                   the comparison being processed right now. This might be
     *                   used for the distance to
     *                   retrieve other matches for instance.
     * @param a
     *                   first object.
     * @param b
     *                   second object.
     * @return the distance between the two EObjects or Double.MAX_VALUE when the
     *         objects are considered
     *         too different to be the same.
     */
    double distance(Comparison inProgress, EObject a, EObject b);
    double getThresholdAmount(EObject eObj);

    /**
     * Check that two objects are equals from the distance function point of view
     * (distance should be 0)
     * You should prefer this method when you just want to check objects are not
     * equals enabling the
     * distance to stop sooner.
     * 
     * @param inProgress
     *                   the comparison being processed right now. This might be
     *                   used for the distance to
     *                   retrieve other matches for instance.
     * @param a
     *                   first object.
     * @param b
     *                   second object.
     * @return true of the two objects are equals, false otherwise.
     */
    boolean areIdentic(Comparison inProgress, EObject a, EObject b);

}