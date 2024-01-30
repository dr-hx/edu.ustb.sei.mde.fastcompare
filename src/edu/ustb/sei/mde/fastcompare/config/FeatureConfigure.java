package edu.ustb.sei.mde.fastcompare.config;

import org.eclipse.emf.ecore.EStructuralFeature;

import edu.ustb.sei.mde.fastcompare.shash.SHashFunction;

public abstract class FeatureConfigure {
    public abstract int getWeight();
    public abstract SHashFunction<?> getShasher();
    public abstract boolean isNoShashing();
    public abstract int getPositiveHashWeight();
    public abstract int getNegativeHashWeight();

    @Override
    public String toString() {
        return getClass().getSimpleName()+"{"
        + "weight: "+getWeight()
        + ", isNoSimHashing: "+isNoShashing()
        + ", simHasher:"+getShasher()
        + ", positiveHashWeight: "+getPositiveHashWeight()
        + ", negativeHashWeight: "+getNegativeHashWeight()
        + "}";
    }

    static public class AdaptiveFeatureConfigure extends FeatureConfigure {
        private EStructuralFeature currentFeature;
        protected WeightTable defaultWeight;
        protected SHasherTable defaultFeatureSHasher; 

        public AdaptiveFeatureConfigure(WeightTable weightTable, SHasherTable shasherTable) {
            this.defaultWeight = weightTable;
            this.defaultFeatureSHasher = shasherTable;
        }

        @Override
        public int getWeight() {
            return this.defaultWeight.getDefaultWeight(currentFeature);
        }

        @Override
        public SHashFunction<?> getShasher() {
            return this.defaultFeatureSHasher.getHashFunction(currentFeature);
        }

        @Override
        public boolean isNoShashing() {
           return getWeight() == 0;
        }

        public void setFeature(EStructuralFeature feature) {
            this.currentFeature = feature;
        }

        @Override
        public int getPositiveHashWeight() {
            return getWeight();
        }

        @Override
        public int getNegativeHashWeight() {
            return -1;
        }
    }
}
