package edu.ustb.sei.mde.fastcompare.minhash;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import edu.ustb.sei.mde.fastcompare.utils.URIComputer;

public class EObjectMinHasher {
    protected URIComputer uriComputer = new URIComputer();
    protected BiGramIterator bigrams = new BiGramIterator();
    private MapIterator<String, Integer> string2int = new MapIterator<>(s->s.hashCode());
    
    protected int maxHashBits = 16;
    protected int blockSize = 32;

    protected MinHashFunctionGroup nameMinHashGroup;
    protected MinHashFunctionGroup attributeMinHashGroup;
    protected MinHashFunctionGroup referenceMinHashGroup;


    public EObjectMinHasher(int blockSize, int maxHashBits) {
        this.blockSize = blockSize;
        this.maxHashBits = maxHashBits;

        this.nameMinHashGroup = new MinHashFunctionGroup(blockSize, 1 << maxHashBits, 1721992074092L);
        this.attributeMinHashGroup = new MinHashFunctionGroup(blockSize/2, 1 << maxHashBits);
        this.referenceMinHashGroup = new MinHashFunctionGroup(blockSize/2, 1 << maxHashBits);
    }

    public void reset() {
        nameMinHashGroup.reset();
        attributeMinHashGroup.reset();
        referenceMinHashGroup.reset();
    }

    public EObjectMinHashValue hash(EObject eObj) {
        update(eObj);
        return getHash();
    }

    public void update(EObject eObj) {
        reset();
        List<EStructuralFeature> allFeatures = eObj.eClass().getEAllStructuralFeatures();
        for(EStructuralFeature f : allFeatures) {
            if(f.isDerived() || f.isTransient() || f.isVolatile() 
                || (f instanceof EReference && ((EReference)f).isContainer())) 
                continue;
            update(eObj, f);
        }
    }

    static final public int[] getCopy(int[] list) {
        return Arrays.copyOf(list, list.length);
    }

    public EObjectMinHashValue getHash() {
        return new EObjectMinHashValue(
            getCopy(nameMinHashGroup.getHash()), 
            getCopy(attributeMinHashGroup.getHash()), 
            getCopy(referenceMinHashGroup.getHash()));
    }

    @SuppressWarnings("unchecked")
    public void update(EObject eObj, EStructuralFeature feature) {
        Object rawValue = eObj.eGet(feature);
        if(rawValue != null) {
            if(feature instanceof EAttribute 
                && "name".equals(feature.getName()) 
                    && "java.lang.String".equals(feature.getEType().getInstanceTypeName())) {
                String name = (String) rawValue;
                bigrams.setSource(name);
                nameMinHashGroup.update(bigrams);
            } else if(feature instanceof EReference){
                if(((EReference)feature).isContainment() == false) {
                    if(feature.isMany()) {
                        List<EObject> objects = (List<EObject>) rawValue;
                        for(EObject object : objects) {
                            Iterable<String> uri = uriComputer.getOrComputeLocation(object);
                            string2int.setSource(uri.iterator());
                            referenceMinHashGroup.update(string2int);
                        }
                    } else {
                        Iterable<String> uri = uriComputer.getOrComputeLocation((EObject) rawValue);
                        string2int.setSource(uri.iterator());
                        referenceMinHashGroup.update(string2int);
                    }
                }
            } else {
                if(feature.isMany()) {
                    attributeMinHashGroup.update(rawFeatureHash(feature.getName(), (List<Object>) rawValue));
                } else {
                    attributeMinHashGroup.update(rawFeatureHash(feature.getName(), rawValue));
                }
            }
        }
        
    }

    protected int rawFeatureHash(String featureName, Object value) {
        int featureNameHash = featureName.hashCode();
        int valueHash = value.hashCode();
        return featureNameHash * 31 + valueHash;
    }

    protected int rawFeatureHash(String featureName, List<Object> values) {
        int featureNameHash = featureName.hashCode();
        int valueHash = 0;
        for(Object value : values) {
            valueHash = valueHash * 31 + value.hashCode();
        }

        return featureNameHash * 31 + valueHash;
    }
}
