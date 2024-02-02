package edu.ustb.sei.mde.fastcompare.tests;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.Before;
import org.junit.Test;

import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.match.DefaultComparisonFactory;
import edu.ustb.sei.mde.fastcompare.match.DefaultMatchEngine;
import edu.ustb.sei.mde.fastcompare.match.IMatchEngine;
import edu.ustb.sei.mde.fastcompare.match.eobject.IEObjectMatcher;
import edu.ustb.sei.mde.fastcompare.match.eobject.ProximityEObjectMatcher;
import edu.ustb.sei.mde.fastcompare.scope.DefaultComparisonScope;

public class TestMatchEngine {
    private IMatchEngine engine;
    private ResourceSet resourceSet;
    
    @Before
    public void initEngine() {
        MatcherConfigure config = new MatcherConfigure();
        IEObjectMatcher matcher = new ProximityEObjectMatcher(config);
        engine = new DefaultMatchEngine(matcher, new DefaultComparisonFactory(config));
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
        URI left = URI.createFileURI("/source/Java/fastcompare/edu.ustb.sei.mde.fastcompare/testdata/Ecore_1218.mutant0.ecore");

        Comparison result = engine.match(load(origin, left));

        System.out.println(result);
    }
}
