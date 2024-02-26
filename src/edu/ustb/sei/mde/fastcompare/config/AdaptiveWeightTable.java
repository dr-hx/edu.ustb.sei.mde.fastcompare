package edu.ustb.sei.mde.fastcompare.config;

import org.eclipse.emf.compare.match.eobject.WeightProvider.Descriptor.Registry;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import edu.ustb.sei.mde.fastcompare.utils.SimpleLRUCache;

public class AdaptiveWeightTable implements IWeightTable {
    final private org.eclipse.emf.compare.match.eobject.WeightProvider.Descriptor.Registry registryInstance;

    public AdaptiveWeightTable(Registry registryInstance) {
        this.registryInstance = registryInstance;
    }

    private SimpleLRUCache<EPackage, org.eclipse.emf.compare.match.eobject.WeightProvider> cache = new SimpleLRUCache<>();

    @Override
    public int getDefaultWeight(EStructuralFeature feature) {
        EPackage pack = (EPackage) feature.eContainer().eContainer();
        org.eclipse.emf.compare.match.eobject.WeightProvider provider = cache.get(pack, registryInstance::getHighestRankingWeightProvider);
        return provider.getWeight(feature);
    }

    @Override
    public int getDefaultParentWeight(EObject a) {
        EPackage pack = (EPackage) a.eClass().eContainer();
        org.eclipse.emf.compare.match.eobject.WeightProvider provider = cache.get(pack, registryInstance::getHighestRankingWeightProvider);
        return provider.getParentWeight(a);
    }

    @Override
    public int getDefaultContainingFeatureWeight(EObject a) {
        EPackage pack = (EPackage) a.eClass().eContainer();
        org.eclipse.emf.compare.match.eobject.WeightProvider provider = cache.get(pack, registryInstance::getHighestRankingWeightProvider);
        return provider.getContainingFeatureWeight(a);
    }

    @Override
    public boolean irrelevant(EStructuralFeature feat) {
        if (feat instanceof EAttribute) {
			return feat.isDerived() || feat.isTransient();
		} else {
			EReference ref = (EReference)feat;
			return ref.isDerived() || ref.isTransient() || ref.isContainment() || ref.isContainer();
		}
    }

}