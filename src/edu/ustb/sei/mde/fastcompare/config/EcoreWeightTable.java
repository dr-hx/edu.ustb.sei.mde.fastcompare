package edu.ustb.sei.mde.fastcompare.config;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.EStringToStringMapEntryImpl;

public class EcoreWeightTable extends WeightTable {
	@Override
	public int getDefaultWeight(EStructuralFeature feature) {

		if (irrelevant(feature)) {
			return 0;
		}

		Integer found = Integer.valueOf(SMALL);
        if (feature == EcorePackage.Literals.ENAMED_ELEMENT__NAME) {
            found = Integer.valueOf(SIGNIFICANT);
        }
        if (feature instanceof EReference) {
            found = Integer.valueOf(referenceChangeCoef * found.intValue());
        } else {
            found = Integer.valueOf(attributeChangeCoef * found.intValue());
        }

		return found.intValue();
	}


	@Override
	public int getDefaultParentWeight(EObject a) {
		final int parentWeight;
		if (a instanceof EStructuralFeature) {
			parentWeight = MASSIVE;
		} else if (a instanceof EAnnotation) {
			parentWeight = UNLIKELY_TO_MATCH;
		} else if (a instanceof EOperation) {
			parentWeight = MAJOR;
		} else if (a instanceof EParameter) {
			parentWeight = UNLIKELY_TO_MATCH;
		} else if (a instanceof EStringToStringMapEntryImpl) {
			parentWeight = UNLIKELY_TO_MATCH;
		} else {
			parentWeight = SIGNIFICANT;
		}
		return parentWeight;
	}

	@Override
	public int getDefaultContainingFeatureWeight(EObject a) {
		if (a instanceof EStructuralFeature || a instanceof EAnnotation || a instanceof EOperation) {
			return MAJOR;
		}
		return SIGNIFICANT;
	}
}
