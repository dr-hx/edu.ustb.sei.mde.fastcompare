package edu.ustb.sei.mde.fastcompare.match;

import org.eclipse.emf.compare.Comparison;

public interface IComparisonFactory {

	/**
	 * Returns a new {@link Comparison}.
	 * 
	 * @return a new {@link Comparison}.
	 */
	Comparison createComparison();

}
