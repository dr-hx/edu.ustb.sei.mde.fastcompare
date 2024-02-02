package edu.ustb.sei.mde.fastcompare.config;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

public interface IWeightTable {

    int getDefaultWeight(EStructuralFeature feature);

    int getDefaultParentWeight(EObject a);

    int getDefaultContainingFeatureWeight(EObject a);

    boolean irrelevant(EStructuralFeature feat);

}