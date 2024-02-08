package edu.ustb.sei.mde.fastcompare.tests;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource.ElementHandler;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.Before;
import org.junit.Test;

import edu.ustb.sei.mde.fastcompare.config.Hasher;
import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.match.DefaultComparisonFactory;
import edu.ustb.sei.mde.fastcompare.match.DefaultMatchEngine;
import edu.ustb.sei.mde.fastcompare.match.IMatchEngine;
import edu.ustb.sei.mde.fastcompare.match.eobject.EditionDistance;
import edu.ustb.sei.mde.fastcompare.match.eobject.IEObjectMatcher;
import edu.ustb.sei.mde.fastcompare.match.eobject.ProximityEObjectMatcher;
import edu.ustb.sei.mde.fastcompare.match.eobject.TopDownProximityEObjectMatcher;
import edu.ustb.sei.mde.fastcompare.scope.DefaultComparisonScope;

public class TestMatchEngine {
    private IMatchEngine engine;
    private ResourceSet resourceSet;
    private MatcherConfigure config;
    
    @Before
    public void initEngine() {
        config = new MatcherConfigure();
        config.setUseSubtreeHash(true);
        config.setUseIdentityHash(false);
        config.setUseSimHash(false);
        IEObjectMatcher matcher = new TopDownProximityEObjectMatcher(config);
        engine = new DefaultMatchEngine(config, matcher, new DefaultComparisonFactory(config));
        resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
			Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		
    }


    private DefaultComparisonScope load(URI left, URI right) {
        Resource l = resourceSet.getResource(left, true);
        Resource r = resourceSet.getResource(right, true);

        return new DefaultComparisonScope(l, r, null);
    }

    @Test
    public void testMutant0() {
        URI origin = URI.createFileURI("/source/Java/fastcompare/edu.ustb.sei.mde.fastcompare/testdata/Ecore_1218.ecore");
        URI left = URI.createFileURI("/source/Java/fastcompare/edu.ustb.sei.mde.fastcompare/testdata/Ecore_1218.mutant3.ecore");

        long start = System.nanoTime();
        Comparison result = engine.match(load(origin, left));
        System.out.println((System.nanoTime() - start) / 1000000.0);
        ((EditionDistance) config.getDistanceFunction()).dump();
        System.out.println(ProximityEObjectMatcher.total);
        // TopDownProximityEObjectMatcher.counter.dump();
        // result.getMatches().forEach(this::printMatch);
    }

    private void printMatch(Match m) {
        System.out.println(m.getLeft() + "=>" + m.getRight());
        m.getSubmatches().forEach(this::printMatch);
    }
}
