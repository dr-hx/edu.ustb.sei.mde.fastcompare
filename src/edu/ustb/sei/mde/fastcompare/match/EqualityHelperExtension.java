package edu.ustb.sei.mde.fastcompare.match;

import org.eclipse.emf.ecore.EObject;

public interface EqualityHelperExtension {
	/**
	 * Enumeration used to return the result of a specific matching.
	 * 
	 * @author <a href="mailto:stephane.thibaudeau@obeo.fr">Stephane Thibaudeau</a>
	 */
	enum SpecificMatch {
		/** This means that this specific extension provider doesn't know how to handle the given objects. */
		UNKNOWN,

		/** If these objects have been determined to be a match by this extension. */
		MATCH,

		/** If these objects have been determined to <i>not</i> match by this extension. */
		UNMATCH
	}

    SpecificMatch matchingEObjects(EObject object1, EObject object2, IEqualityHelper equalityHelper);
}