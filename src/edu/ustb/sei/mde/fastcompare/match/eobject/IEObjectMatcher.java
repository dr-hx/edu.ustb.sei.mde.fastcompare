package edu.ustb.sei.mde.fastcompare.match.eobject;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;

public interface IEObjectMatcher {
	/**
	 * This will be called by the match engine to determine matches between EObjects.
	 * <p>
	 * The implementation should update the given comparison object by adding the Matches it detect. These
	 * matches should include both matching and unmatchings EObjects (i.e. EObjects that can be matched in all
	 * three lists, EObjects that can be matched in only two of the three lists, and EObjects that can only be
	 * found in one of the three.
	 * </p>
	 * 
	 * <p>
	 * Note that this interface is different from EMF Compare because it accepts iterables of root objects,
	 * rather than iterators of all objects. We made this change to intend to get better efficiency but losing
	 * flexiblity.
	 * </p>
	 * 
	 * @param comparison
	 *            the comparison to update.
	 * @param leftRoots
	 *            An iterable over the root EObjects that could be found in the left side.
	 * @param rightRoots
	 *            An iterable over the root EObjects that could be found in the right side.
	 * @param originalRoots
	 *            And iterable over the root EObject that may be considered ancestors of the couples that can be
	 *            detected in the left and right sides.
	 */
	void createMatches(Comparison comparison, Collection<EObject> leftRoots,
		Collection<EObject> rightRoots, Collection<EObject> originRoots);
	
	void createMatches(Comparison comparison, Iterator<? extends EObject> leftEObjects,
		Iterator<? extends EObject> rightEObjects, Iterator<? extends EObject> originEObjects);

	MatcherConfigure getMatcherConfigure();
}
