package edu.ustb.sei.mde.fastcompare.match.resource;

import java.util.Iterator;

import org.eclipse.emf.compare.MatchResource;
import org.eclipse.emf.ecore.resource.Resource;

public interface IResourceMatcher {
	/**
	 * This will be called by the engine in order to retrieve the mappings created by this matcher.
	 * <p>
	 * The returned mappings should include both "matching" resources and "not matching" resources (i.e.
	 * resources that are in either left or right ... but not in any of the two other lists).
	 * </p>
	 * 
	 * @param leftResources
	 *            An iterator over the resources we found on the left side.
	 * @param rightResources
	 *            An iterator over the resources we found on the right side.
	 * @param originResources
	 *            An iterator over the resources that may be considered as common ancestors of the couples
	 *            detected on the left and right sides.
	 * @return The created resource mappings. Should include both matched and unmatched resources.
	 */
	Iterable<MatchResource> createMappings(Iterator<? extends Resource> leftResources,
			Iterator<? extends Resource> rightResources, Iterator<? extends Resource> originResources);
}