package edu.ustb.sei.mde.fastcompare.scope;

import static edu.ustb.sei.mde.fastcompare.utils.Predicates.*;

import org.eclipse.emf.common.notify.Notifier;

import edu.ustb.sei.mde.fastcompare.utils.ComparePredicates;

public class DefaultComparisonScope extends FilterComparisonScope {
	/**
	 * Simply delegates to the super constructor.
	 * 
	 * @param left
	 *            Left root of this comparison.
	 * @param right
	 *            Right root of this comparison.
	 * @param origin
	 *            Common ancestor of <code>left</code> and <code>right</code>.
	 */
	public DefaultComparisonScope(Notifier left, Notifier right, Notifier origin) {
		super(left, right, origin);
		setEObjectContentFilter(not(ComparePredicates.IS_EGENERIC_TYPE_WITHOUT_PARAMETERS));
		setResourceContentFilter(not(ComparePredicates.IS_EGENERIC_TYPE_WITHOUT_PARAMETERS));
	}
}
