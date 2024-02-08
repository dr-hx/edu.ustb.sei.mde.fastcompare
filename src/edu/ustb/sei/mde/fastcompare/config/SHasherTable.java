package edu.ustb.sei.mde.fastcompare.config;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import edu.ustb.sei.mde.fastcompare.shash.DoubleHashFunction;
import edu.ustb.sei.mde.fastcompare.shash.FloatHashFunction;
import edu.ustb.sei.mde.fastcompare.shash.ListHashFunction;
import edu.ustb.sei.mde.fastcompare.shash.NumberHashFunction;
import edu.ustb.sei.mde.fastcompare.shash.ObjectHashFunction;
import edu.ustb.sei.mde.fastcompare.shash.SHashFunction;
import edu.ustb.sei.mde.fastcompare.shash.StringHashFunction;
import edu.ustb.sei.mde.fastcompare.shash.URIHashFunction;

/**
 * SHasherTable allows two kinds of mappings.
 * 1. It maps EStructuralFeature onto SHashFunction;
 * 2. It maps types onto SHashFunction.
 */
public class SHasherTable {
    
    protected Map<Class<?>, SHashFunction<?>> byTypeHasherMap;
    protected Map<EStructuralFeature, SHashFunction<?>> featureHashMap;

    static public SHasherTable makeDefaultSHasherTable(MatcherConfigure configure) {
        SHasherTable table = new SHasherTable();

        table.byTypeHasherMap.put(Object.class, new ObjectHashFunction());
        
        table.byTypeHasherMap.put(Integer.class, new NumberHashFunction());
        table.byTypeHasherMap.put(int.class, new NumberHashFunction());

        table.byTypeHasherMap.put(Long.class, new NumberHashFunction());
        table.byTypeHasherMap.put(long.class, new NumberHashFunction());

        table.byTypeHasherMap.put(Short.class, new NumberHashFunction());
        table.byTypeHasherMap.put(short.class, new NumberHashFunction());

        table.byTypeHasherMap.put(Byte.class, new NumberHashFunction());
        table.byTypeHasherMap.put(byte.class, new NumberHashFunction());

        table.byTypeHasherMap.put(Character.class, new NumberHashFunction());
        table.byTypeHasherMap.put(char.class, new NumberHashFunction());

        table.byTypeHasherMap.put(Double.class, new DoubleHashFunction());
        table.byTypeHasherMap.put(double.class, new DoubleHashFunction());

        table.byTypeHasherMap.put(Float.class, new FloatHashFunction());
        table.byTypeHasherMap.put(float.class, new FloatHashFunction());

        table.byTypeHasherMap.put(Boolean.class, new ObjectHashFunction());
        table.byTypeHasherMap.put(boolean.class, new ObjectHashFunction());

        table.byTypeHasherMap.put(String.class, new StringHashFunction());
        
        table.byTypeHasherMap.put(EObject.class, new URIHashFunction(configure.getUriComputer()));

        return table;
    }

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
        return new ListHashFunction(innerHash);
    }

    public void putFeatureHash(EStructuralFeature feature, SHashFunction<?> hash) {
        this.featureHashMap.put(feature, hash);
    }

    final private boolean isBasicType(Class<?> cls) {
        return cls.isPrimitive() || 
            cls == String.class || cls == Boolean.class || cls == Character.class ||
            Number.class.isAssignableFrom(cls);
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
                    if(jType == null || !isBasicType(jType))
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
