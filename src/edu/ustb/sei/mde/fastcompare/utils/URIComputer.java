package edu.ustb.sei.mde.fastcompare.utils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.emf.ecore.EObject;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class URIComputer {
    	/**
	 * A computing cache for the locations.
	 */
	private Map<EObject, Iterable<String>> locationCache;

	/**
	 * A computing cache for the uri fragments.
	 */
	private Map<EObject, String> fragmentsCache;

	/**
	 * The function used to compute the fragment of an {@link EObject}.
	 */
	private Function<EObject, String> fragmentComputation;

    public URIComputer() {
		locationCache = new AccessBasedLRUCache<EObject, Iterable<String>>(1024, 1024, .75F);
		fragmentsCache = new AccessBasedLRUCache<EObject, String>(1024, 1024, .75F);
		fragmentComputation = new URIFragmentComputation();
	}

	private Iterable<String> apply(EObject input) {
		String result = ""; //$NON-NLS-1$
		EObject container = input.eContainer();
		if (container != null) {
			result = retrieveFragment(input);
		} else {
			result = "0"; //$NON-NLS-1$
		}

		final List<String> resultList = Lists.newArrayList(result);
		if (container != null) {
			Iterables.addAll(resultList, getOrComputeLocation(container));
		}
		return resultList;
	}

	/**
	 * The method return the location of an EObject represented as a list of fragments.
	 * 
	 * @param container
	 *            any EObject.
	 * @return a list of fragments.
	 */
	public Iterable<String> getOrComputeLocation(EObject container) {
		Iterable<String> result = locationCache.get(container);
		if (result == null) {
			result = apply(container);
			locationCache.put(container, result);
		}
		return result;
	}

	/**
	 * the containing fragment for a given {@link EObject}.
	 * 
	 * @param input
	 *            an EObject.
	 * @return a String representation of its containing fragment.
	 */
	public String retrieveFragment(EObject input) {
		String result = fragmentsCache.get(input);
		if (result == null) {
			result = fragmentComputation.apply(input);
			fragmentsCache.put(input, result);
		}
		return result;
	}
}
