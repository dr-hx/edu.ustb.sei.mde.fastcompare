package edu.ustb.sei.mde.fastcompare.tests;

import java.security.MessageDigest;
import java.util.zip.CRC32;
import java.util.zip.CRC32C;

import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.CommonUtil;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.match.DefaultEqualityHelperFactory;
import org.eclipse.emf.compare.match.IComparisonFactory;
import org.eclipse.emf.compare.match.eobject.CachingDistance;
import org.eclipse.emf.compare.match.eobject.EqualityHelperExtensionProviderDescriptorRegistryImpl;
import org.eclipse.emf.compare.match.eobject.WeightProviderDescriptorRegistryImpl;
import org.eclipse.emf.compare.match.impl.MatchEngineFactoryImpl;
import org.eclipse.emf.compare.utils.UseIdentifiers;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource.ElementHandler;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.XMI2UMLResource;
import org.junit.Before;
import org.junit.Test;

import edu.ustb.sei.mde.fastcompare.config.AdaptiveWeightTable;
import edu.ustb.sei.mde.fastcompare.config.Hasher;
import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.index.DefaultElementIndexAdapterFactory;
import edu.ustb.sei.mde.fastcompare.index.ElementIndexAdapter;
import edu.ustb.sei.mde.fastcompare.index.ElementIndexAdapterFactory;
import edu.ustb.sei.mde.fastcompare.index.ElementIndexAdapterWithStructuralChecksum;
import edu.ustb.sei.mde.fastcompare.match.DefaultComparisonFactory;
import edu.ustb.sei.mde.fastcompare.match.DefaultMatchEngine;
import edu.ustb.sei.mde.fastcompare.match.IMatchEngine;
import edu.ustb.sei.mde.fastcompare.match.eobject.EditionDistance;
import edu.ustb.sei.mde.fastcompare.match.eobject.IEObjectMatcher;
import edu.ustb.sei.mde.fastcompare.match.eobject.ProximityEObjectMatcher;
import edu.ustb.sei.mde.fastcompare.match.eobject.TopDownProximityEObjectMatcher;
import edu.ustb.sei.mde.fastcompare.scope.DefaultComparisonScope;
import edu.ustb.sei.mde.fastcompare.utils.CRC64;
import edu.ustb.sei.mde.fastcompare.utils.CommonUtils;

public class TestMatchEngine {
    private IMatchEngine engine;
    private ResourceSet resourceSet;
    private MatcherConfigure config;
    
    @Before
    public void initEngine() {
        config = new MatcherConfigure(new AdaptiveWeightTable(WeightProviderDescriptorRegistryImpl.createStandaloneInstance()));
        config.setUseSubtreeHash(true);
        config.setUseIdentityHash(false);
        config.setUseSimHash(false);
        config.setIndexAdapterFactory(new ElementIndexAdapterFactory() {
            @Override
            protected ElementIndexAdapter createAdapter(int id) {
                return new ElementIndexAdapterWithStructuralChecksum(id);
            }
        });
        IEObjectMatcher matcher = new TopDownProximityEObjectMatcher(config);
        engine = new DefaultMatchEngine(config, matcher, new DefaultComparisonFactory(config));
        resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
			Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());

        resourceSet.getPackageRegistry().put(UMLPackage.eINSTANCE.getNsURI(), UMLPackage.eINSTANCE);
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(XMI2UMLResource.FILE_EXTENSION, XMI2UMLResource.Factory.INSTANCE);
		
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

    @Test
    public void test1195Mutant0() {
        URI origin = URI.createFileURI("/source/Java/fastcompare/edu.ustb.sei.mde.fastcompare/testdata/Ecore_1195.ecore");
        URI left = URI.createFileURI("/source/Java/fastcompare/edu.ustb.sei.mde.fastcompare/testdata/Ecore_1195.mutant_4.ecore");

        long start = System.nanoTime();
        Comparison result = engine.match(load(origin, left));
        System.out.println((System.nanoTime() - start) / 1000000.0);
        ((EditionDistance) config.getDistanceFunction()).dump();
        System.out.println(ProximityEObjectMatcher.total);
        // TopDownProximityEObjectMatcher.counter.dump();
        // result.getMatches().forEach(this::printMatch);
    }

    @Test
    public void test11051Mutant() {
        URI origin = URI.createFileURI("/source/Java/fastcompare/edu.ustb.sei.mde.fastcompare/testdata/UML_10051.xmi");
        URI left = URI.createFileURI("/source/Java/fastcompare/edu.ustb.sei.mde.fastcompare/testdata/UML_10051.mutant_0.xmi");

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

    @Test
    public void testCRC32() {
        System.out.println("eClass".hashCode());
        System.out.println("eIsProxy".hashCode());

        CRC32 crc = new CRC32();
        // 1. 4132794411 { }
        crc.reset();
        CommonUtils.update(crc, 4132794411L);
        System.out.println(crc.getValue());
        CommonUtils.update(crc, "{");
        System.out.println(crc.getValue());
        CommonUtils.update(crc, "}");
        System.out.println(crc.getValue());
        // 2. 2836312605 { }
        // crc.reset();
        // CommonUtils.update(crc, 2836312605L);
        // System.out.println(crc.getValue());
        // CommonUtils.update(crc, "{");
        // System.out.println(crc.getValue());
        // CommonUtils.update(crc, "}");
        // System.out.println(crc.getValue());
        // 3. 3428475947 { }
        crc.reset();
        CommonUtils.update(crc, 3428475947L);
        System.out.println(crc.getValue());
        CommonUtils.update(crc, "{");
        System.out.println(crc.getValue());
        CommonUtils.update(crc, "}");
        System.out.println(crc.getValue());

        crc.reset();
        crc.update((int)4132794411L);
        System.out.println(crc.getValue());

        crc.reset();
        crc.update((int)3428475947L);
        System.out.println(crc.getValue());

    }


    @Test
    public void testEMFC() {
        org.eclipse.emf.compare.match.eobject.WeightProvider.Descriptor.Registry weightInstance = WeightProviderDescriptorRegistryImpl.createStandaloneInstance();
		final MatchEngineFactoryImpl matchEngineFactory = new MatchEngineFactoryImpl(UseIdentifiers.NEVER, weightInstance) {
			@Override
			public org.eclipse.emf.compare.match.IMatchEngine getMatchEngine() {
				if (matchEngine == null) {
					final IComparisonFactory comparisonFactory = new org.eclipse.emf.compare.match.DefaultComparisonFactory(new DefaultEqualityHelperFactory());
					final org.eclipse.emf.compare.match.eobject.ProximityEObjectMatcher matcher;
					final org.eclipse.emf.compare.match.eobject.EditionDistance editionDistance = new org.eclipse.emf.compare.match.eobject.EditionDistance(weightInstance,
							EqualityHelperExtensionProviderDescriptorRegistryImpl.createStandaloneInstance());
					final CachingDistance cachedDistance = new CachingDistance(editionDistance);
					matcher = new org.eclipse.emf.compare.match.eobject.ProximityEObjectMatcher(cachedDistance);
					matchEngine = new org.eclipse.emf.compare.match.DefaultMatchEngine(matcher, comparisonFactory);
				}
				return matchEngine;
			}
		};

        URI origin = URI.createFileURI("/source/Java/fastcompare/edu.ustb.sei.mde.fastcompare/testdata/UML_10051.xmi");
        URI left = URI.createFileURI("/source/Java/fastcompare/edu.ustb.sei.mde.fastcompare/testdata/UML_10051.mutant_0.xmi");
        Comparison result = matchEngineFactory.getMatchEngine().match(new org.eclipse.emf.compare.scope.DefaultComparisonScope(resourceSet.getResource(origin, true), resourceSet.getResource(left, true), null), new BasicMonitor());
        System.out.println(result);
    }
}
