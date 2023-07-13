package edu.ustb.sei.mde.fastcompare.match;

import java.util.Iterator;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.ecore.EObject;

public interface IEObjectMatcher {
	/**
	 * This will be called by the match engine to determine matches between EObjects.
	 * <p>
	 * The implementation should update the given comparison object by adding the Matches it detect. These
	 * matches should include both matching and unmatchings EObjects (i.e. EObjects that can be matched in all
	 * three lists, EObjects that cna be matched in only two of the three lists, and EObjects that can only be
	 * found in one of the three.
	 * </p>
	 * 
	 * @param comparison
	 *            the comparison to update.
	 * @param leftEObjects
	 *            An iterator over the EObjects that could be found in the left side.
	 * @param rightEObjects
	 *            An iterator over the EObjects that could be found in the right side.
	 * @param originEObjects
	 *            And iterator over the EObject that may be considered ancestors of the couples that can be
	 *            detected in the left and right sides.
	 */
	void createMatches(Comparison comparison, Iterator<? extends EObject> leftEObjects,
			Iterator<? extends EObject> rightEObjects, Iterator<? extends EObject> originEObjects);
}
