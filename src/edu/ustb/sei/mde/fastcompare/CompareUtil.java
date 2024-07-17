package edu.ustb.sei.mde.fastcompare;

import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.diff.DefaultDiffEngine;
import org.eclipse.emf.compare.match.eobject.WeightProviderDescriptorRegistryImpl;
import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.fastcompare.config.AdaptiveWeightTable;
import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.index.ElementIndexAdapter;
import edu.ustb.sei.mde.fastcompare.index.ElementIndexAdapterFactory;
import edu.ustb.sei.mde.fastcompare.index.ElementIndexAdapterWithStructuralChecksum;
import edu.ustb.sei.mde.fastcompare.match.DefaultComparisonFactory;
import edu.ustb.sei.mde.fastcompare.match.DefaultMatchEngine;
import edu.ustb.sei.mde.fastcompare.match.IMatchEngine;
import edu.ustb.sei.mde.fastcompare.match.eobject.IEObjectMatcher;
import edu.ustb.sei.mde.fastcompare.match.eobject.ProximityEObjectMatcher;
import edu.ustb.sei.mde.fastcompare.match.eobject.TopDownProximityEObjectMatcherOpt;
import edu.ustb.sei.mde.fastcompare.utils.ComparePredicates;
import static edu.ustb.sei.mde.fastcompare.utils.Predicates.not;

import java.util.function.Predicate;

public class CompareUtil {
    static public IMatchEngine createClassicalProximityMatchEngine() {
        MatcherConfigure config = new MatcherConfigure(new AdaptiveWeightTable(WeightProviderDescriptorRegistryImpl.createStandaloneInstance()));
        return createClassicalProximityMatchEngine(config);
    }

    public static IMatchEngine createClassicalProximityMatchEngine(MatcherConfigure config) {
        config.setUseSubtreeHash(false);
        config.setUseIdentityHash(false);
        config.setUseSimHash(false);

        IEObjectMatcher matcher = new ProximityEObjectMatcher(config);
        IMatchEngine engine = new DefaultMatchEngine(config, matcher, new DefaultComparisonFactory(config));

        return engine;
    }

    static public IMatchEngine createTopDownProximityMatchEngine() {
        MatcherConfigure config = new MatcherConfigure(new AdaptiveWeightTable(WeightProviderDescriptorRegistryImpl.createStandaloneInstance()));
        return createTopDownProximityMatchEngine(config);
    }

    public static IMatchEngine createTopDownProximityMatchEngine(MatcherConfigure config) {
        config.setUseSubtreeHash(true);
        config.setUseIdentityHash(false);
        config.setUseSimHash(false);
        config.setIndexAdapterFactory(new ElementIndexAdapterFactory() {
            @Override
            protected ElementIndexAdapter createAdapter(int id) {
                return new ElementIndexAdapterWithStructuralChecksum(id);
            }
        });
        IEObjectMatcher matcher = new TopDownProximityEObjectMatcherOpt(config);
        IMatchEngine engine = new DefaultMatchEngine(config, matcher, new DefaultComparisonFactory(config));
        return engine;
    }

    static public Predicate<? super EObject> defaultFilter = not(ComparePredicates.IS_EGENERIC_TYPE_WITHOUT_PARAMETERS);
    static public int[] iterateResult(Comparison result) {
        org.eclipse.emf.compare.diff.DefaultDiffEngine diff = new DefaultDiffEngine();
        diff.diff(result, new BasicMonitor());

        int totalMatches = 0;
        int totalDiffs = 0;
        var itr = result.eAllContents();
        while(itr.hasNext()) {
            EObject m = itr.next();
            if(m instanceof Match) {
                EObject left = ((Match)m).getLeft();
                EObject right = ((Match)m).getRight();
                if(left == null) {
                    if(defaultFilter.test(right)) {
                        totalDiffs += ((Match)m).getDifferences().size();
                    } else {
                        itr.prune();
                    }
                } else if(right == null) {
                    if(defaultFilter.test(left)) {
                        totalDiffs += ((Match)m).getDifferences().size();
                    } else {
                        itr.prune();
                    }
                } else {
                    if(defaultFilter.test(left) && defaultFilter.test(right)) {
                        totalMatches ++;
                        totalDiffs += ((Match)m).getDifferences().size();
                    } else {
                        itr.prune();
                    }
                }
            }
        }

        return new int[] {totalMatches, totalDiffs};
    }
}
