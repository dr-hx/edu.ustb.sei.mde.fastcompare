package edu.ustb.sei.mde.fastcompare.index;

import org.eclipse.emf.ecore.EObject;

import java.util.Map;

import org.eclipse.emf.compare.Comparison;

public interface ObjectIndex {
    enum Side {
		/**
		 * the left side.
		 */
		LEFT,
		/**
		 * The right side.
		 */
		RIGHT,
		/**
		 * The origin side (also known as ancestor).
		 */
		ORIGIN
	}

    /**
	 * return the list of EObjects of a given side still available in the index.
	 * 
	 * @param side
	 *            the side we are looking for.
	 * @return the list of EObjects of a given side still available in the index.
	 */
	Iterable<EObject> getValuesStillThere(Side side);

	/**
	 * Return the closest EObjects found in other sides than the one given.
	 * 
	 * @param inProgress
	 *            the comparison currently being computed. It will not be changed directly but only queried to
	 *            know if some element has already been matched or not.
	 * @param eObj
	 *            the base EObject used to lookup similar ones.
	 * @param side
	 *            the side of the passed EObject.
	 * @return a map of Side, EObjects, returning all the found objects (and the passed one) which are the
	 *         closests.
	 */
	Map<Side, EObject> findClosests(Comparison inProgress, EObject eObj, Side side);

	/**
	 * Remove an object from the index.
	 * 
	 * @param eObj
	 *            object to remove.
	 * @param side
	 *            Side in which this object was.
	 */
	void remove(EObject eObj, Side side);

	/**
	 * Register an Object in the index with the given side.
	 * 
	 * @param eObj
	 *            the {@link EObject} to register.
	 * @param side
	 *            the side in which it should be registered.
	 */
	void index(EObject eObj, Side side);

}
