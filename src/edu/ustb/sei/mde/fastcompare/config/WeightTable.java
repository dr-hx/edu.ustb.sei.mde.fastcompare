package edu.ustb.sei.mde.fastcompare.config;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

public class WeightTable {
    /***
	 * Something not impacting the object identity unless it adds up a lot.
	 */
	public static final int SMALL = 5;

	/**
	 * A normal change in an object.
	 */
	public static final int NORMAL = 10;

	/**
	 * Likely to impact the object identity.
	 */
	public static final int SIGNIFICANT = 20;

	/**
	 * Quite important regarding the Object identity.
	 */
	public static final int MAJOR = 150;

	/**
	 * Very important regarding the Object identity.
	 */
	public static final int MASSIVE = 350;

	/**
	 * It is very unlikely the elements are matching if they have differences of this magnitude.
	 */
	public static final int UNLIKELY_TO_MATCH = 1000;

    /**
	 * Weight coefficient of a change on a reference.
	 */
	protected int referenceChangeCoef = NORMAL;

	/**
	 * Weight coefficient of a change on an attribute.
	 */
	protected int attributeChangeCoef = SIGNIFICANT;

	public int getDefaultWeight(EStructuralFeature feature) {
        if (irrelevant(feature)) {
			return 0;
		}

		Integer found = Integer.valueOf(SMALL);
		/*
         * Copied from EMF Compare
         */
        if ("name".equals(feature.getName()) || "id".equals(feature.getName())) {
            found = Integer.valueOf(SIGNIFICANT);
        }
        if (feature instanceof EReference) {
            found = Integer.valueOf(referenceChangeCoef * found.intValue());
        } else {
            found = Integer.valueOf(attributeChangeCoef * found.intValue());
        }
		
		return found.intValue();
    }

	public int getDefaultParentWeight(EObject a) {
		return SIGNIFICANT;
	}

	public int getDefaultContainingFeatureWeight(EObject a) {
		return NORMAL;
	}

    public boolean irrelevant(EStructuralFeature feat) {
		if (feat instanceof EAttribute) {
			return feat.isDerived() || feat.isTransient();
		} else {
			EReference ref = (EReference)feat;
			return ref.isDerived() || ref.isTransient() || ref.isContainment() || ref.isContainer();
		}
	}
}
