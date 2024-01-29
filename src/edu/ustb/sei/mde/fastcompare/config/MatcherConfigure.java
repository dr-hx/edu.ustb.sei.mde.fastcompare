package edu.ustb.sei.mde.fastcompare.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.function.Function;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
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
    
    /**
     * Configurable weight tables.
     * By default, we import from the weight tables from EMF compare.
     * It is independed from matcher instances.
     */
    protected WeightTable defaultWeightTable;

    /**
     * It tells the default feature hash functions.
     * By default, it is created by the engine.
     * Users may provide their owned table.
     * It is independed from matcher instances.
     */
    protected SHasherTable defaultFeatureSHasherTable;

    /**
     * It is used to generate default feature configure. 
     * It is not intended to be replaced by developers.
     * It is independed from matcher instances.
     */
    private AdaptiveFeatureConfigure adaptiveFeatureConfigure;

    /**
     * It is used by equality helper to compute whether two eObjects are matched.
     * It may be null.
     * It is independed from matcher instances.
     */
    protected EqualityHelperExtension equalityHelperExtension;

    /**
     * A provider to create equality helper that is used to compute whether two values are matched.
     * It is independed from matcher instances.
     */
    protected Function<MatcherConfigure, IEqualityHelper> equalityHelperProvider;

    /**
     * The class map tells the class configures.
     * Each class configure can tell local feature configures.
     * It is independed from matcher instances.
     */
    protected Map<EClass, ClassConfigure> classConfigureMap = new LinkedHashMap<>(32);

    /**
     * The map tells the global feature configures.
     * Because a feature may be inherited by many classes, the gloabl map give this feature a common configure.
     * It is independed from matcher instances.
     */
    protected Map<EStructuralFeature, FeatureConfigure> globalFeatureConfigureMap = new LinkedHashMap<>(32);

    private SimpleLRUCache<EClass, ClassConfigure> oneShotClassConfCache = new SimpleLRUCache<>();

    private URIComputer uriComputer;

    private double defaultSimThreshold = 0.5;

    /**
     * The function depends on matcher instances.
     * It should be created/reset for each matcher instance.
     */
    protected DistanceFunction distanceFunction;

    /**
     * The hasher used to compute element hashes (s-hash and i-hash).
     * It is independent from matcher instances.
     */
    private Hasher elementHasher;

    private boolean useIdentityHash = false;
    private boolean useSubtreeHash = false;
    private boolean useSimHash = true;

    public MatcherConfigure() {
        this.uriComputer = new URIComputer();
        this.defaultWeightTable = new EcoreWeightTable();

        this.equalityHelperProvider = (c) -> new EqualityHelper(new AutoLRUCache<>(4096, 4096, 0.75f), c);
        this.distanceFunction = new EditionDistance(this);
        
        this.defaultFeatureSHasherTable = SHasherTable.makeDefaultSHasherTable(this);
        this.adaptiveFeatureConfigure = new AdaptiveFeatureConfigure(defaultWeightTable, defaultFeatureSHasherTable);
        this.elementHasher = new Hasher(this);
    }

    public boolean isUsingIdentityHash() {
        return useIdentityHash;
    }

    public boolean isUsingSubtreeHash() {
        return useSubtreeHash;
    }

    public void setUseIdentityHash(boolean useIdentityHash) {
        this.useIdentityHash = useIdentityHash;
    }

    public void setUseSubtreeHash(boolean useSubtreeHash) {
        this.useSubtreeHash = useSubtreeHash;
    }

    public void setUseSimHash(boolean useSimHash) {
        this.useSimHash = useSimHash;
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
        return equalityHelperProvider.apply(this);
    }

    public void setEqualityHelperProvider(Function<MatcherConfigure, IEqualityHelper> equalityHelperProvider) {
        this.equalityHelperProvider = equalityHelperProvider;
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
        FeatureFilter featureFilter;
        featureFilter = new FeatureFilter(this);
        return featureFilter;
    }

    public URIComputer getUriComputer() {
        return uriComputer;
    }

    final public boolean shouldDoSimHash(EClass clazz) {
        return useSimHash && shouldDoSimHashForEClass(clazz);
    }

    protected boolean shouldDoSimHashForEClass(EClass clazz) {
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

    public void loadThresholds(List<EPackage> metamodels, File thresholdFile) {
        try(FileInputStream input = new FileInputStream(thresholdFile)) {
            Properties properties = new Properties();
            properties.load(input);
            for(Entry<Object,Object> entry : properties.entrySet()) {
                EClass clazz = resolveClass(metamodels, entry.getKey().toString());
                if(clazz != null) {
                    Double t = Double.parseDouble(entry.getValue().toString());
                    getClassConfigure(clazz).setSimThreshold(t);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private EClass resolveClass(List<EPackage> metamodels, String name) {
        for(EPackage p : metamodels) {
            EClassifier c = p.getEClassifier(name);
            if(c instanceof EClass) return (EClass) c;
        }
        return null;
    } 
}
