package edu.ustb.sei.mde.fastcompare.match;

import java.util.Iterator;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.ecore.EObject;

import com.google.common.collect.Iterators;

import edu.ustb.sei.mde.fastcompare.utils.URIComputer;

public class URIDistance {
	/**
	 * The upper bound distance we can get using this function.
	 */
	private static final int MAX_DISTANCE = 10;

	private URIComputer uriComputer;

	/**
	 * Create a new {@link URIDistance}.
	 */
	public URIDistance(URIComputer u) {
		uriComputer = u;
	}

	/**
	 * Set an optional comparison used to retrieve matches already computed. This will impact the way the uri
	 * is computed by making sure two matching objects will have the same URI.
	 * 
	 * @param comparison
	 *            the comparison to use to retrieve the matches.
	 */
	public void setComparison(Comparison comparison) {
		// Unused by the default implementation
	}

	/**
	 * Return a metric result URI similarities. It compares 2 strings splitting those by "/" and return an int
	 * representing the level of similarity. 0 - they are exactly the same to 10 - they are completely
	 * different. "adding a fragment", "removing a fragment".
	 * 
	 * @param a
	 *            First of the two {@link EObject}s to compare.
	 * @param b
	 *            Second of the two {@link EObject}s to compare.
	 * @return The number of changes to transform one uri to another one.
	 */
	public int proximity(EObject a, EObject b) {
		Iterable<String> aPath = getOrComputeLocation(a);
		Iterable<String> bPath = getOrComputeLocation(b);
		return proximity(aPath, bPath);
	}

	/**
	 * Return a metric result URI similarities. It compares 2 lists of fragments and return an int
	 * representing the level of similarity. 0 - they are exactly the same to 10 - they are completely
	 * different. "adding a fragment", "removing a fragment".
	 * 
	 * @param aPath
	 *            First of the two list of {@link String}s to compare.
	 * @param bPath
	 *            Second of the two list of {@link String}s to compare.
	 * @return The number of changes to transform one uri to another one.
	 */
	public int proximity(Iterable<String> aPath, Iterable<String> bPath) {
		int aSize = 0;
		int bSize = 0;
		Iterator<String> itA = aPath.iterator();
		Iterator<String> itB = bPath.iterator();
		boolean areSame = true;
		int commonSegments = 0;
		int remainingASegments = 0;
		int remainingBSegments = 0;
		while (itA.hasNext() && itB.hasNext() && areSame) {
			String a = itA.next();
			String b = itB.next();
			if (a.equals(b)) {
				commonSegments++;
			} else {
				areSame = false;
			}
			aSize++;
			bSize++;

		}
		if (commonSegments == 0) {
			return MAX_DISTANCE;
		}
		remainingASegments = aSize + Iterators.size(itA) - commonSegments;
		remainingBSegments = bSize + Iterators.size(itB) - commonSegments;

		int nbSegmentsToGoFromAToB = remainingASegments + remainingBSegments;
		return (nbSegmentsToGoFromAToB * 10) / (commonSegments * 2 + nbSegmentsToGoFromAToB);
	}

	
	private Iterable<String> getOrComputeLocation(EObject container) {
		return uriComputer.getOrComputeLocation(container);
	}

	/**
	 * return the maximum value we can get for this distance.
	 * 
	 * @return the maximum value we can get for this distance.
	 */
	public int getUpperBoundDistance() {
		return MAX_DISTANCE;
	}

}