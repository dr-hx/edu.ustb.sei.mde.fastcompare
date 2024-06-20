package edu.ustb.sei.mde.fastcompare.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import edu.ustb.sei.mde.fastcompare.utils.Pair;

public class ClassConfigure {
    protected final EClass type;
    protected final MatcherConfigure host;
    protected Map<EStructuralFeature, FeatureConfigure> localConfigureMap;
    protected int parentWeight = -1;
    protected int containingFeatureWeight = -1;
    protected List<Entry<EStructuralFeature, FeatureConfigure>> concernedFeatureList = null;
    protected double simThreshold;

    // public abstract IElementHasher getIhasher();
    public ClassConfigure(EClass type, MatcherConfigure host) {
        this.type = type;
        this.host = host;
        this.localConfigureMap = new LinkedHashMap<>(16);
        this.simThreshold = host.getDefaultSimThreshold();
    }

    public double getSimThreshold() {
        return simThreshold;
    }

    public void setSimThreshold(double simThreshold) {
        this.simThreshold = simThreshold;
    }

    public int getContainingFeatureWeight(EObject o) {
        if(containingFeatureWeight == -1) {
            this.containingFeatureWeight = host.getDefaultWeightTable().getDefaultContainingFeatureWeight(o);
        }
        return containingFeatureWeight;
    }

    public void setContainingFeatureWeight(int containingFeatureWeight) {
        this.containingFeatureWeight = containingFeatureWeight;
    }

    public void setParentWeight(int parentWeight) {
        this.parentWeight = parentWeight;
    }

    public int getParentWeight(EObject o) {
        if(parentWeight == -1) {
            parentWeight = host.getDefaultWeightTable().getDefaultParentWeight(o);
        }
        return parentWeight;
    } 

    public FeatureConfigure getFeatureConfigure(EStructuralFeature feature) {
        return this.localConfigureMap.get(feature);
    }

    public void putFeatureConfigure(EStructuralFeature feature, FeatureConfigure configure) {
        this.localConfigureMap.put(feature, configure);
    }

    // public List<EStructuralFeature> getLocalConcernedFeatures() {
    //     return type.getEStructuralFeatures();
    // }

    public Iterable<Entry<EStructuralFeature, FeatureConfigure>> getConcernedFeatures() {
        if(concernedFeatureList == null) {
            EList<EStructuralFeature> eAllStructuralFeatures = type.getEAllStructuralFeatures();
            concernedFeatureList = new ArrayList<>(eAllStructuralFeatures.size());
            eAllStructuralFeatures.stream().filter(f->host.getDefaultWeightTable().irrelevant(f)==false).forEach((feature)->{
                FeatureConfigure configure = localConfigureMap.get(feature);
                if(configure == null) {
                    configure = host.getOrCreateGlobalFeatureConfigure(feature);
                    putFeatureConfigure(feature, configure);
                }
                concernedFeatureList.add(new Pair<EStructuralFeature, FeatureConfigure>(feature, configure));
            });
        }
        return concernedFeatureList;
    }

    @Override
    public String toString() {
        this.getConcernedFeatures();
        StringBuilder builder = new StringBuilder();

        builder.append("ClassConfigure {\n\ttype: ");
        builder.append(type.getName());
        builder.append("\n\tlocalConfigures: [");
        for(Entry<EStructuralFeature, FeatureConfigure> entry : localConfigureMap.entrySet()) {
            builder.append(entry.getKey().getName());
            builder.append("->");
            builder.append(entry.getValue());
            builder.append(", ");
        }
        builder.append("]");
        builder.append("\n\tconcernedFeatures: [");
        for(Entry<EStructuralFeature, FeatureConfigure> entry : concernedFeatureList) {
            builder.append(entry.getKey().getName());
            builder.append("->");
            builder.append(entry.getValue());
            builder.append(", ");
        }
        builder.append("]\n}");

        return builder.toString();
    }

    
}
