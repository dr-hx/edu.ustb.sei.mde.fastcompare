package edu.ustb.sei.mde.fastcompare.match;

import org.eclipse.emf.compare.Comparison;

import edu.ustb.sei.mde.fastcompare.scope.IComparisonScope;

public interface IMatchEngine {
    Comparison match(IComparisonScope scope);
}
