package edu.ustb.sei.mde.fastcompare.utils;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.internal.spec.MatchSpec;

public class FastMatchSpec extends MatchSpec {
    private Comparison comparison = null;

    @Override
    public Comparison getComparison() {
        if(comparison == null) {
            comparison = super.getComparison();
        }
        return comparison;
    }
}
