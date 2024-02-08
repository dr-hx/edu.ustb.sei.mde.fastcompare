package edu.ustb.sei.mde.fastcompare.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;

import edu.ustb.sei.mde.fastcompare.config.FeatureConfigure.AdaptiveFeatureConfigure;
import edu.ustb.sei.mde.fastcompare.index.DefaultElementIndexAdapterFactory;
import edu.ustb.sei.mde.fastcompare.index.ElementIndexAdapterFactory;
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
    protected IWeightTable defaultWeightTable;

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

        this.indexAdapterFactory = new DefaultElementIndexAdapterFactory();

        initThresholds();
        initIgnoredClasses();
    }

    public ElementIndexAdapterFactory getIndexAdapterFactory() {
        return indexAdapterFactory;
    }

    public void setIndexAdapterFactory(ElementIndexAdapterFactory indexAdapterFactory) {
        this.indexAdapterFactory = indexAdapterFactory;
    }

    protected Set<EClass> ignoredClasses = new HashSet<>();

    protected void initIgnoredClasses() {
        ignoredClasses.add(EcorePackage.eINSTANCE.getEGenericType());
        ignoredClasses.add(EcorePackage.eINSTANCE.getEAnnotation());
        ignoredClasses.add(EcorePackage.eINSTANCE.getEDataType());
        ignoredClasses.add(EcorePackage.eINSTANCE.getEStringToStringMapEntry());
    }

    public boolean initThresholdsForPackage(EPackage ePackage, Consumer<MatcherConfigure> initializer) {
        if(initializer == null) {
            if("http://www.eclipse.org/uml2/5.0.0/UML".equals(ePackage.getNsURI())) {
                this.getClassConfigure((EClass) ePackage.getEClassifier("Abstraction")).setSimThreshold(0.70);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Activity")).setSimThreshold(0.6);
                this.getClassConfigure((EClass) ePackage.getEClassifier("ActivityFinalNode")).setSimThreshold(0.60);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Actor")).setSimThreshold(0.55);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Association")).setSimThreshold(0.68);
                this.getClassConfigure((EClass) ePackage.getEClassifier("BehaviorExecutionSpecification")).setSimThreshold(0.70);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Class")).setSimThreshold(0.60);
                this.getClassConfigure((EClass) ePackage.getEClassifier("ClassifierTemplateParameter")).setSimThreshold(0.70);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Collaboration")).setSimThreshold(0.55);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Component")).setSimThreshold(0.65);
                this.getClassConfigure((EClass) ePackage.getEClassifier("ControlFlow")).setSimThreshold(0.60);
                this.getClassConfigure((EClass) ePackage.getEClassifier("DataType")).setSimThreshold(0.55);
                this.getClassConfigure((EClass) ePackage.getEClassifier("DecisionNode")).setSimThreshold(0.60);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Dependency")).setSimThreshold(0.65);
                this.getClassConfigure((EClass) ePackage.getEClassifier("ExecutionOccurrenceSpecification")).setSimThreshold(0.70);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Extend")).setSimThreshold(0.70);
                this.getClassConfigure((EClass) ePackage.getEClassifier("FlowFinalNode")).setSimThreshold(0.60);
                this.getClassConfigure((EClass) ePackage.getEClassifier("ForkNode")).setSimThreshold(0.62);
                this.getClassConfigure((EClass) ePackage.getEClassifier("GeneralOrdering")).setSimThreshold(0.65);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Generalization")).setSimThreshold(0.60);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Include")).setSimThreshold(0.60);
                this.getClassConfigure((EClass) ePackage.getEClassifier("InitialNode")).setSimThreshold(0.60);
                this.getClassConfigure((EClass) ePackage.getEClassifier("InstanceValue")).setSimThreshold(0.65);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Interaction")).setSimThreshold(0.60);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Interface")).setSimThreshold(0.60);
                this.getClassConfigure((EClass) ePackage.getEClassifier("InterfaceRealization")).setSimThreshold(0.60);
                this.getClassConfigure((EClass) ePackage.getEClassifier("JoinNode")).setSimThreshold(0.70);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Lifeline")).setSimThreshold(0.54);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Message")).setSimThreshold(0.60);
                this.getClassConfigure((EClass) ePackage.getEClassifier("MessageOccurrenceSpecification")).setSimThreshold(0.70);
                this.getClassConfigure((EClass) ePackage.getEClassifier("OccurrenceSpecification")).setSimThreshold(0.65);
                this.getClassConfigure((EClass) ePackage.getEClassifier("OpaqueAction")).setSimThreshold(0.65);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Operation")).setSimThreshold(0.60);
                this.getClassConfigure((EClass) ePackage.getEClassifier("PackageImport")).setSimThreshold(0.60);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Parameter")).setSimThreshold(0.64);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Property")).setSimThreshold(0.70);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Realization")).setSimThreshold(0.65);
                this.getClassConfigure((EClass) ePackage.getEClassifier("TemplateParameter")).setSimThreshold(0.65);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Usage")).setSimThreshold(0.55);
                this.getClassConfigure((EClass) ePackage.getEClassifier("UseCase")).setSimThreshold(0.60);
                this.getClassConfigure((EClass) ePackage.getEClassifier("Package")).setSimThreshold(0.40);

                this.ignoredClasses.add((EClass) ePackage.getEClassifier("Comment"));
                this.ignoredClasses.add((EClass) ePackage.getEClassifier("ExtensionPoint"));
                this.ignoredClasses.add((EClass) ePackage.getEClassifier("InstanceSpecification"));
                this.ignoredClasses.add((EClass) ePackage.getEClassifier("LiteralInteger"));
                this.ignoredClasses.add((EClass) ePackage.getEClassifier("LiteralString"));
                this.ignoredClasses.add((EClass) ePackage.getEClassifier("LiteralUnlimitedNatural"));
                this.ignoredClasses.add((EClass) ePackage.getEClassifier("Model"));

                return true;
            } else {
                return false;
            }
        } else {
            initializer.accept(this);
            return true;
        }
    }

    protected void initThresholds() {
        // init Ecore classes
        this.getClassConfigure(EcorePackage.eINSTANCE.getEClass()).setSimThreshold(0.59);
        this.getClassConfigure(EcorePackage.eINSTANCE.getEAttribute()).setSimThreshold(0.69);
        this.getClassConfigure(EcorePackage.eINSTANCE.getEReference()).setSimThreshold(0.69);
        this.getClassConfigure(EcorePackage.eINSTANCE.getEEnum()).setSimThreshold(0.47);
        this.getClassConfigure(EcorePackage.eINSTANCE.getEEnumLiteral()).setSimThreshold(0.45);
        this.getClassConfigure(EcorePackage.eINSTANCE.getEOperation()).setSimThreshold(0.55);
        this.getClassConfigure(EcorePackage.eINSTANCE.getEPackage()).setSimThreshold(0.45);
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

    public IWeightTable getDefaultWeightTable() {
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
        return !this.ignoredClasses.contains(clazz);
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

    private ElementIndexAdapterFactory indexAdapterFactory;
}
