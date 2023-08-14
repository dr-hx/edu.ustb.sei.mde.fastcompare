package edu.ustb.sei.mde.fastcompare.match.eobject;

import org.eclipse.emf.ecore.EObject;

public interface ScopeQuery {
	/**
	 * Check whether the object is in the scope or not.
	 * 
	 * @param any
	 *            any EObject.
	 * @return true if the Object is in scope. False otherwise.
	 */
	boolean isInScope(EObject any);
}
