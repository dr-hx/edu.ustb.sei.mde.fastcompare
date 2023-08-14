package edu.ustb.sei.mde.fastcompare.match.resource;

import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.CompareFactory;
import org.eclipse.emf.compare.MatchResource;
import org.eclipse.emf.ecore.resource.Resource;

import com.google.common.collect.Lists;

public class NameMatchingStrategy implements IResourceMatchingStrategy {
	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.match.resource.IResourceMatchingStrategy#matchResources(java.lang.Iterable,
	 *      java.lang.Iterable, java.lang.Iterable)
	 */
	public List<MatchResource> matchResources(Iterable<? extends Resource> left,
			Iterable<? extends Resource> right, Iterable<? extends Resource> origin) {
		final List<MatchResource> mappings = Lists.newArrayList();

		final List<Resource> rightCopy = Lists.newArrayList(right);
		final List<Resource> originCopy = Lists.newArrayList(origin);

		// Can we find matches for the left resource in either left or origin?
		for (Resource leftResource : left) {
			final Resource matchingRight = findMatch(leftResource, rightCopy);
			final Resource matchingOrigin = findMatch(leftResource, originCopy);

			if (matchingRight != null || matchingOrigin != null) {
				rightCopy.remove(matchingRight);
				originCopy.remove(matchingOrigin);
				mappings.add(createMatchResource(leftResource, matchingRight, matchingOrigin));
			}
		}

		// We no longer have to check in the left, but we may have matches of the right resources in the
		// origin list
		for (Resource rightResource : rightCopy) {
			final Resource matchingOrigin = findMatch(rightResource, originCopy);
			originCopy.remove(matchingOrigin);

			if (matchingOrigin != null) {
				mappings.add(createMatchResource(null, rightResource, matchingOrigin));
			}
		}

		return mappings;
	}

	/**
	 * Returns the first match of <code>reference</code> in <code>candidates</code>. This implementation will
	 * consider two Resources to be "matches" if they have the same name.
	 * 
	 * @param reference
	 *            The reference resource.
	 * @param candidates
	 *            The list of potential candidates that may match <code>reference</code>.
	 * @return The first match of <code>reference</code> in <code>candidates</code>. <code>null</code> if
	 *         none.
	 */
	protected Resource findMatch(Resource reference, Iterable<Resource> candidates) {
		final URI referenceURI = reference.getURI();
		for (Resource candidate : candidates) {
			if (urisLastSegmentMatch(referenceURI, candidate.getURI())) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * Indicates whether the given URIs are equal or have the same last segment.
	 * 
	 * @param referenceURI
	 *            Reference URI
	 * @param otherURI
	 *            Candidate URI
	 * @return <code>true</code> if both URIs are null, or if they are equal, or if they have the same
	 *         non-null last segment.
	 */
	private boolean urisLastSegmentMatch(URI referenceURI, URI otherURI) {
		if (referenceURI == otherURI) {
			return true;
		}
		if (referenceURI != null && otherURI != null) {
			if (referenceURI.equals(otherURI)) {
				return true;
			}
			String lastSegment = referenceURI.lastSegment();
			return lastSegment != null && lastSegment.equals(otherURI.lastSegment());
		}
		return false;
	}

	/**
	 * Creates a {@link MatchResource} instance and sets all three resources of the mapping on it.
	 * 
	 * @param left
	 *            The left resource of this mapping.
	 * @param right
	 *            The right resource of this mapping.
	 * @param origin
	 *            The origin resource of this mapping.
	 * @return The create mapping.
	 */
	protected static MatchResource createMatchResource(Resource left, Resource right, Resource origin) {
		final MatchResource match = CompareFactory.eINSTANCE.createMatchResource();

		match.setLeft(left);
		match.setRight(right);
		match.setOrigin(origin);

		if (left != null && left.getURI() != null) {
			match.setLeftURI(left.getURI().toString());
		}
		if (right != null && right.getURI() != null) {
			match.setRightURI(right.getURI().toString());
		}
		if (origin != null && origin.getURI() != null) {
			match.setOriginURI(origin.getURI().toString());
		}

		return match;
	}
}
