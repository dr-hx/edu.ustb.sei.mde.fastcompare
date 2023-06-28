package edu.ustb.sei.mde.fastcompare.config;

import org.eclipse.emf.ecore.EStructuralFeature;

import edu.ustb.sei.mde.fastcompare.shash.SHashFunction;

public class MutableFeatureConfigure extends FeatureConfigure {
    protected final EStructuralFeature feature;
    protected int weight;
    protected int posWeight;
    protected int negWeight;
    protected boolean noshashing;
    protected SHashFunction<?> shasher;
    
    protected final boolean local;
    
    public boolean isLocal() {
        return local;
    }

    public void setPosWeight(int posWeight) {
        this.posWeight = posWeight;
    }

    public void setNegWeight(int negWeight) {
        this.negWeight = negWeight;
    }
    
    public MutableFeatureConfigure(EStructuralFeature feature, boolean local) {
        this.feature = feature;
        this.local = local;
    }

    public boolean isNoShashing() {
        return noshashing;
    }

    public void setNoShashing(boolean noshashing) {
        this.noshashing = noshashing;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public void setShasher(SHashFunction<?> shasher) {
        this.shasher = shasher;
    }

    public SHashFunction<?> getShasher() {
        return shasher;
    }

    @Override
    public int getPositiveHashWeight() {
        return posWeight;
    }

    @Override
    public int getNegativeHashWeight() {
        return negWeight;
    }
}
