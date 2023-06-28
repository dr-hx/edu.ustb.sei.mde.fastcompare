package edu.ustb.sei.mde.fastcompare.diff;

import org.eclipse.emf.compare.Comparison;

public interface IDiffEngine {

	/**
	 * This is the entry point of the differencing process.
	 * <p>
	 * It will complete the input <code>comparison</code> by iterating over the
	 * {@link org.eclipse.emf.compare.Match matches} it contain, filling in the differences it can detect for
	 * each distinct Match.
	 * </p>
	 * 
	 * @param comparison
	 *            The comparison this engine is expected to complete.
	 */
	void diff(Comparison comparison);
}