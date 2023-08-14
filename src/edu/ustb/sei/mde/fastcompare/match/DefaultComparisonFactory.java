package edu.ustb.sei.mde.fastcompare.match;

import static com.google.common.base.Preconditions.checkNotNull;

import org.eclipse.emf.compare.CompareFactory;
import org.eclipse.emf.compare.Comparison;

import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;

public class DefaultComparisonFactory implements IComparisonFactory {

	/** The factory used to instantiate IEqualityHelper to associate with Comparison. */
	private final MatcherConfigure matcherConfigure;

	/**
	 * Creates a new DefaultComparisonFactory.
	 * 
	 * @param equalityHelperFactory
	 *            The factory used to instantiate IEqualityHelper to associate with Comparison.
	 */
	public DefaultComparisonFactory(MatcherConfigure matcherConfigure) {
		this.matcherConfigure = checkNotNull(matcherConfigure);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.match.IComparisonFactory#createComparison()
	 */
	public Comparison createComparison() {
		Comparison comparison = CompareFactory.eINSTANCE.createComparison();

		IEqualityHelper equalityHelper = matcherConfigure.getEqualityHelper();

		comparison.eAdapters().add(equalityHelper);
		equalityHelper.setTarget(comparison);

		return comparison;
	}

}
