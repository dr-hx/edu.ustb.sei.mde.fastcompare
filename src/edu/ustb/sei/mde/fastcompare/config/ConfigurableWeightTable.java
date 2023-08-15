package edu.ustb.sei.mde.fastcompare.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;

public class ConfigurableWeightTable extends WeightTable {
    private Map<EStructuralFeature, Integer> featureWeightMap = new HashMap<>();
    private Map<EClass, Integer> parentWeightMap = new HashMap<>();
    private Map<EClass, Integer> containingFeatureWeightMap = new HashMap<>();

    static public final Pattern featurePattern = Pattern.compile("([_a-zA-Z0-9]+)\\.([_a-zA-Z0-9]+)");
    static public final Pattern parentPattern = Pattern.compile("([_a-zA-Z0-9]+)\\.eContainer\\(\\)");
    static public final Pattern containmentFeaturePattern = Pattern.compile("([_a-zA-Z0-9]+)\\.eContainmentFeature\\(\\)");

    public void loadFromFile(List<EPackage> metamodels, File configureFile) {
        Properties properties = new Properties();
        try(FileInputStream reader = new FileInputStream(configureFile)) {
            properties.load(reader);
            for(Entry<Object,Object> entry : properties.entrySet()) {
                String keyString = entry.getKey().toString();
                Matcher parentMatcher = parentPattern.matcher(keyString);
                if(parentMatcher.matches()) {
                    String clazzName = parentMatcher.group(1);
                    EClass clazz = resolveClass(metamodels, clazzName);
                    if(clazz != null) {
                        Integer weight = Integer.parseInt(entry.getValue().toString());
                        parentWeightMap.put(clazz, weight);
                    }
                } else {
                    Matcher containmentMatcher = containmentFeaturePattern.matcher(keyString);
                    if(containmentMatcher.matches()) {
                        String clazzName = containmentMatcher.group(1);
                        EClass clazz = resolveClass(metamodels, clazzName);
                        if(clazz != null) {
                            Integer weight = Integer.parseInt(entry.getValue().toString());
                            containingFeatureWeightMap.put(clazz, weight);
                        }
                    } else {
                        Matcher featureMatcher = featurePattern.matcher(keyString);
                        if(featureMatcher.matches()) {
                            String clazzName = featureMatcher.group(1);
                            String featureName = featureMatcher.group(2);
                            EClass clazz = resolveClass(metamodels, clazzName);
                            EStructuralFeature feature = resolveFeature(clazz, featureName);
                            if(feature != null) {
                                Integer weight = Integer.parseInt(entry.getValue().toString());
                                featureWeightMap.put(feature, weight);
                            }
                        }
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private EClass resolveClass(List<EPackage> metamodels, String clazzName) {
        for(EPackage p : metamodels) {
            EClassifier cls = p.getEClassifier(clazzName);
            if(cls instanceof EClass) return (EClass) cls;
        }
        return null;
    }

    private EStructuralFeature resolveFeature(EClass cls, String featureName) {
        if(cls == null) return null;
        EStructuralFeature f = cls.getEStructuralFeature(featureName);
        if(f != null && f.eContainer() == cls) return f;
        return null;
    }

    @Override
    public int getDefaultContainingFeatureWeight(EObject a) {
        Integer w = this.containingFeatureWeightMap.get(a.eClass());
        if(w == null)
            return super.getDefaultContainingFeatureWeight(a);
        else
            return w;
    }

    @Override
    public int getDefaultParentWeight(EObject a) {
        Integer w = this.parentWeightMap.get(a.eClass());
        if(w == null)
            return super.getDefaultParentWeight(a);
        else
            return w;
    }

    @Override
    public int getDefaultWeight(EStructuralFeature feature) {
        Integer w = this.featureWeightMap.get(feature);
        if(w == null)
            return super.getDefaultWeight(feature);
        else
            return w;
    }
}
