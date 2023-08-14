package edu.ustb.sei.mde.fastcompare.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;

import edu.ustb.sei.mde.fastcompare.config.FeatureConfigure.AdaptiveFeatureConfigure;
import edu.ustb.sei.mde.fastcompare.match.EqualityHelper;
import edu.ustb.sei.mde.fastcompare.match.EqualityHelperExtension;
import edu.ustb.sei.mde.fastcompare.match.IEqualityHelper;
import edu.ustb.sei.mde.fastcompare.match.eobject.DistanceFunction;
import edu.ustb.sei.mde.fastcompare.match.eobject.EditionDistance;
import edu.ustb.sei.mde.fastcompare.utils.AutoLRUCache;
import edu.ustb.sei.mde.fastcompare.utils.SimpleLRUCache;
import edu.ustb.sei.mde.fastcompare.utils.URIComputer;

/**
 * This class provides a unified interface to configure the matcher.
 * 
 */
public class MatcherConfigure {
    final static public EStructuralFeature CONTAINER_FEATURE = null;
    // configurable tables
    protected WeightTable defaultWeightTable;

    protected SHasherTable defaultFeatureSHasherTable;

    protected AdaptiveFeatureConfigure adaptiveFeatureConfigure;

    protected EqualityHelperExtension equalityHelperExtension;

    protected IEqualityHelper equalityHelper;

    // configure maps
    protected Map<EClass, ClassConfigure> classConfigureMap = new LinkedHashMap<>(32);

    protected Map<EStructuralFeature, FeatureConfigure> globalFeatureConfigureMap = new LinkedHashMap<>(32);

    private SimpleLRUCache<EClass, ClassConfigure> oneShotClassConfCache = new SimpleLRUCache<>();

    private FeatureFilter featureFilter = null;

    private URIComputer uriComputer;

    private double defaultSimThreshold = 0.5;

    private DistanceFunction distanceFunction;

    private Hasher elementHasher;

    private boolean useIdentityHash = false;
    private boolean useSubtreeHash = false;

    public MatcherConfigure() {
        this.uriComputer = new URIComputer();
        this.defaultWeightTable = new EcoreWeightTable();

        this.equalityHelper = new EqualityHelper(new AutoLRUCache<>(4096, 4096, 0.75f), this);
        this.distanceFunction = new EditionDistance(this);
        
        this.defaultFeatureSHasherTable = SHasherTable.makeDefaultSHasherTable(this);
        this.adaptiveFeatureConfigure = new AdaptiveFeatureConfigure(defaultWeightTable, defaultFeatureSHasherTable);
        this.elementHasher = new Hasher(this);
    }

    public boolean isUsingIdentityHash() {
        return useIdentityHash;
    }

    public boolean isUsingSubtreeHash() {
        return useIdentityHash && useSubtreeHash;
    }

    public void setUseIdentityHash(boolean useIdentityHash) {
        this.useIdentityHash = useIdentityHash;
    }

    public void setUseSubtreeHash(boolean useSubtreeHash) {
        if(useSubtreeHash) {
            this.useIdentityHash = true;
            this.useSubtreeHash = true;
        } else {
            this.useSubtreeHash = false;
        }
    }

    public Hasher getElementHasher() {
        return elementHasher;
    }

    public void setElementHasher(Hasher elementHasher) {
        this.elementHasher = elementHasher;
    }

    public DistanceFunction getDistanceFunction() {
        return distanceFunction;
    }

    public void setDistanceFunction(DistanceFunction distanceFunction) {
        this.distanceFunction = distanceFunction;
    }

    public EqualityHelperExtension getEqualityHelperExtension() {
        return equalityHelperExtension;
    }

    public void setEqualityHelperExtension(EqualityHelperExtension equalityHelperExtension) {
        this.equalityHelperExtension = equalityHelperExtension;
    }

    public IEqualityHelper getEqualityHelper() {
        return equalityHelper;
    }

    public void setEqualityHelper(IEqualityHelper equalityHelper) {
        this.equalityHelper = equalityHelper;
    }

    public FeatureConfigure getOrCreateGlobalFeatureConfigure(EStructuralFeature feature) {
        FeatureConfigure configure = this.globalFeatureConfigureMap.get(feature);
        if (configure == null) {
            this.adaptiveFeatureConfigure.setFeature(feature);
            configure = new MutableFeatureConfigure(feature, false);
            ((MutableFeatureConfigure) configure).setWeight(this.adaptiveFeatureConfigure.getWeight());
            ((MutableFeatureConfigure) configure).setNoShashing(configure.getWeight() == 0);
            if (((MutableFeatureConfigure) configure).isNoShashing()) {
                ((MutableFeatureConfigure) configure).setShasher(null);
                ((MutableFeatureConfigure) configure).setPosWeight(0);
                ((MutableFeatureConfigure) configure).setNegWeight(0);
            } else {
                ((MutableFeatureConfigure) configure).setShasher(this.adaptiveFeatureConfigure.getShasher());
                ((MutableFeatureConfigure) configure)
                        .setPosWeight(this.adaptiveFeatureConfigure.getPositiveHashWeight());
                ((MutableFeatureConfigure) configure)
                        .setNegWeight(this.adaptiveFeatureConfigure.getNegativeHashWeight());
            }
            this.adaptiveFeatureConfigure.setFeature(null);
            this.globalFeatureConfigureMap.put(feature, configure);
        }
        return configure;
    }

    public WeightTable getDefaultWeightTable() {
        return defaultWeightTable;
    }

    public ClassConfigure getClassConfigure(EClass clazz) {
        return oneShotClassConfCache.get(clazz, this::_getClassConfigure);
    }

    public FeatureFilter getFeatureFilter() {
        featureFilter = new FeatureFilter(this);
        return featureFilter;
    }

    public URIComputer getUriComputer() {
        return uriComputer;
    }

    public boolean shouldDoSimHash(EClass clazz) {
        return true;
    }

    public double getDefaultSimThreshold() {
        return defaultSimThreshold;
    }

    public void setDefaultSimThreshold(double v) {
        this.defaultSimThreshold = v;
    }

    protected ClassConfigure createClassConfigure(EClass clazz) {
        return new ClassConfigure(clazz, this);
    }

    private ClassConfigure _getClassConfigure(EClass clazz) {
        ClassConfigure configure = classConfigureMap.computeIfAbsent(clazz, (c) -> {
            return createClassConfigure(clazz);
        });
        return configure;
    }

}
