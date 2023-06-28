package edu.ustb.sei.mde.fastcompare.config;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import edu.ustb.sei.mde.fastcompare.shash.SHashFunction;

/**
 * SHasherTable allows two kinds of mappings.
 * 1. It maps EStructuralFeature onto SHashFunction;
 * 2. It maps types onto SHashFunction.
 */
public class SHasherTable {
    
    protected Map<Class<?>, SHashFunction<?>> byTypeHasherMap;
    protected Map<EStructuralFeature, SHashFunction<?>> featureHashMap;

    

    public SHasherTable() {
        this.byTypeHasherMap = new HashMap<>();
        this.featureHashMap = new HashMap<>();
    }

    protected SHashFunction<?> getFeatureHashFunction(EStructuralFeature feature) {
        return featureHashMap.get(feature);
    }

    protected SHashFunction<?> getValueTypeHashFunction(Class<?> type) {
        return byTypeHasherMap.get(type);
    }

    protected SHashFunction<?> makeListHashFunction(SHashFunction<?> innerHash) {
        return null;
    }

    public void putFeatureHash(EStructuralFeature feature, SHashFunction<?> hash) {
        this.featureHashMap.put(feature, hash);
    }
    
    public SHashFunction<?> getHashFunction(EStructuralFeature feature) {
        SHashFunction<?> hash = getFeatureHashFunction(feature);

        if(hash == null) {
            EClassifier eType = feature.getEType();
            Class<?> jType = null;
            if(eType instanceof EDataType) {
                if(eType instanceof EEnum) {
                    jType = Object.class;
                } else {
                    jType = eType.getInstanceClass();
                    if(jType == null || jType.isPrimitive() == false)
                        jType = null;
                }
            } else {
                jType = EObject.class;
            }

            if(jType != null)
                hash = getValueTypeHashFunction(jType);
            
            if(hash != null && feature.isMany()) {
                hash = makeListHashFunction(hash);
            }

            if(hash != null) {
                featureHashMap.put(feature, hash);
            }
        }

        return hash;
    }
}
