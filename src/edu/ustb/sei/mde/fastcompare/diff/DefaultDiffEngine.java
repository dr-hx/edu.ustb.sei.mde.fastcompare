package edu.ustb.sei.mde.fastcompare.diff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.DifferenceSource;
import org.eclipse.emf.compare.Match;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.FeatureMapUtil;

import edu.ustb.sei.mde.fastcompare.config.FeatureConfigure;
import edu.ustb.sei.mde.fastcompare.config.FeatureFilter;
import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.match.EqualityHelper;
import edu.ustb.sei.mde.fastcompare.match.IEqualityHelper;
import edu.ustb.sei.mde.fastcompare.utils.DiffUtil;
import edu.ustb.sei.mde.fastcompare.utils.ComparisonUtil;
import static edu.ustb.sei.mde.fastcompare.utils.ReferenceUtil.getAsList;
import static edu.ustb.sei.mde.fastcompare.utils.ReferenceUtil.safeEGet;


public class DefaultDiffEngine implements IDiffEngine {
	/**
	 * We'll use this "placeholder" to differentiate the unmatched elements from the "null" values that
	 * attributes can legitimately use.
	 */
	protected static final Object UNMATCHED_VALUE = new Object();

	/**
	 * The diff processor that will be used by this engine. Should be passed by the constructor and accessed
	 * by {@link #getDiffProcessor()}.
	 */
	private IDiffProcessor diffProcessor;

	protected MatcherConfigure matcherConfigure;

	/**
	 * Create the diff engine.
	 */
	public DefaultDiffEngine(MatcherConfigure matcherConfigure) {
		this(new DiffBuilder(), matcherConfigure);
	}

	/**
	 * Create the diff engine.
	 * 
	 * @param processor
	 *            this instance will be called for each detected difference.
	 */
	public DefaultDiffEngine(IDiffProcessor processor, MatcherConfigure matcherConfigure) {
		this.diffProcessor = processor;
		this.matcherConfigure = matcherConfigure;
	}

	/**
	 * Checks whether the given {@code list} contains the given {@code element} according to the semantics of
	 * {@link IEqualityHelper#matchingValues(Comparison, Object, Object)} and returns the index at which it is
	 * positioned if applicable.
	 * 
	 * @param comparison
	 *            This will be used in order to retrieve the Match for EObjects when comparing them.
	 * @param list
	 *            List which content we are to check.
	 * @param element
	 *            The element we expect to be contained in {@code list}.
	 * @param <E>
	 *            Type of the input iterable's content.
	 * @return The index at which the given {@code list} contains {@code element}, {@code -1} otherwise.
	 */
	protected <E> int indexOf(Comparison comparison, List<E> list, E element) {
		final IEqualityHelper equality = EqualityHelper.getEqualityHelper(comparison);
		for (int i = 0; i < list.size(); i++) {
			E candidate = list.get(i);
			if (equality.matchingValues(candidate, element)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.diff.IDiffEngine#diff(org.eclipse.emf.compare.Comparison,
	 *      org.eclipse.emf.common.util.Monitor)
	 */
	public void diff(Comparison comparison) {
		for (Match rootMatch : comparison.getMatches()) {
			checkForDifferences(rootMatch);
		}
	}

	/**
	 * Checks the given {@link Match}'s sides for potential differences. Will recursively check for
	 * differences on submatches.
	 * 
	 * @param match
	 *            The match that is to be checked.
	 * @param monitor
	 *            The monitor to report progress or to check for cancellation.
	 */
	protected void checkForDifferences(Match match) {
		internalCheckForDifferences(match, this.matcherConfigure.getFeatureFilter());
	}

	/**
	 * This will be called recursively in order to check matches for potential differences, then their
	 * sub-matches.
	 * 
	 * @param match
	 *            The match that is to be checked.
	 * @param monitor
	 *            The monitor to report progress or to check for cancellation.
	 * @param featureFilter
	 *            The {@link FeatureFilter} for this comparison.
	 */
	private void internalCheckForDifferences(Match match, FeatureFilter featureFilter) {
		checkResourceAttachment(match);

		final Iterator<Entry<EStructuralFeature, FeatureConfigure>> features = featureFilter.getFeaturesToCheckByInstance(match);
		while(features.hasNext()) {
			Entry<EStructuralFeature, FeatureConfigure> pair = features.next();
			EStructuralFeature feature = pair.getKey();
			
			if(feature instanceof EReference) {
				final EReference reference = (EReference) feature;
				final boolean considerOrdering = featureFilter.checkForOrderingChanges(reference);
				computeDifferences(match, reference, pair.getValue(), considerOrdering);
			} else {
				final EAttribute attribute = (EAttribute) feature;
				final boolean considerOrdering = featureFilter.checkForOrderingChanges(attribute);
				computeDifferences(match, attribute, pair.getValue(), considerOrdering);
			}
		}

		for (Match submatch : match.getSubmatches()) {
			internalCheckForDifferences(submatch, featureFilter);
		}
	}

	/**
	 * Checks whether the given {@link Match}'s sides have changed resources. This will only be called for
	 * {@link Match} elements referencing the root(s) of an EMF Resource.
	 * 
	 * @param match
	 *            The match that is to be checked.
	 * @param monitor
	 *            The monitor to report progress or to check for cancellation.
	 */
	protected void checkResourceAttachment(Match match) {
		final Comparison comparison = match.getComparison();

		if (comparison.getMatchedResources().isEmpty()) {
			// This is a comparison of EObjects, do not go up to the resources
			return;
		}

		final EObject left = match.getLeft();
		final EObject right = match.getRight();
		final EObject origin = match.getOrigin();

		final boolean originIsRoot = isRoot(origin);

		boolean threeWay = comparison.isThreeWay();
		if (threeWay) {
			if (originIsRoot) {
				// Uncontrol or delete, the "resource attachment" is a deletion
				if (!isRoot(left)) {
					final String uri = origin.eResource().getURI().toString();
					getDiffProcessor().resourceAttachmentChange(match, uri, DifferenceKind.DELETE,
							DifferenceSource.LEFT);
				}
				if (!isRoot(right)) {
					final String uri = origin.eResource().getURI().toString();
					getDiffProcessor().resourceAttachmentChange(match, uri, DifferenceKind.DELETE,
							DifferenceSource.RIGHT);
				}
				// Cases where isRoot(left) == true or isRoot(right) == true
				// are handled in org.eclipse.emf.compare.egit by EGitPostProcessor#postDiff
			} else {
				// Control or add, the "resource attachment" is an addition
				if (isRoot(left)) {
					final String uri = left.eResource().getURI().toString();
					getDiffProcessor().resourceAttachmentChange(match, uri, DifferenceKind.ADD,
							DifferenceSource.LEFT);
				}
				if (isRoot(right)) {
					final String uri = right.eResource().getURI().toString();
					getDiffProcessor().resourceAttachmentChange(match, uri, DifferenceKind.ADD,
							DifferenceSource.RIGHT);
				}
			}
		} else {
			final boolean leftIsRoot = isRoot(left);
			final boolean rightIsRoot = isRoot(right);
			if (leftIsRoot && !rightIsRoot) {
				final String uri = left.eResource().getURI().toString();
				getDiffProcessor().resourceAttachmentChange(match, uri, DifferenceKind.ADD,
						DifferenceSource.LEFT);
			} else if (!leftIsRoot && rightIsRoot) {
				final String uri = right.eResource().getURI().toString();
				getDiffProcessor().resourceAttachmentChange(match, uri, DifferenceKind.DELETE,
						DifferenceSource.LEFT);
			}
		}
	}

	/**
	 * Checks whether the given EObject is a root of its resource or not.
	 * 
	 * @param eObj
	 *            The EObject to check.
	 * @return <code>true</code> if this object is a root of its containing resource, <code>false</code>
	 *         otherwise.
	 */
	protected static boolean isRoot(EObject eObj) {
		if (eObj instanceof InternalEObject) {
			return ((InternalEObject)eObj).eDirectResource() != null;
		}

		boolean isRoot = false;
		if (eObj != null) {
			final Resource res = eObj.eResource();
			final EObject container = eObj.eContainer();
			// <root of containment tree> || <root of fragment>
			isRoot = (container == null && res != null)
					|| (container != null && container.eResource() != res);
		}
		return isRoot;
	}

	/**
	 * Delegates the computation of Differences for a given containment reference according to the type of
	 * comparison (two- or three-way), and whether we need to take ordering changes into account.
	 * 
	 * @param match
	 *            The match which sides we need to check for potential differences.
	 * @param reference
	 *            The reference whose values are currently being checked for differences.
	 * @param checkOrdering
	 *            Whether we need to detect ordering changes or ignore them.
	 */
	protected void computeContainmentDifferences(Match match, EReference reference, FeatureConfigure configure, boolean checkOrdering) {
		Comparison comparison = match.getComparison();

		if (checkOrdering) {
			if (comparison.isThreeWay()) {
				computeContainmentDifferencesThreeWay(match, reference, configure, checkOrdering);
			} else {
				computeContainmentDifferencesTwoWay(match, reference, configure, checkOrdering);
			}
		} else {
			List<Object> leftValues = getAsList(match.getLeft(), reference);
			List<Object> rightValues = getAsList(match.getRight(), reference);
			if (comparison.isThreeWay()) {
				createContainmentDifferencesNoOrdering(match, reference, configure, leftValues, DifferenceSource.LEFT);
				createContainmentDifferencesNoOrdering(match, reference, configure, rightValues, DifferenceSource.RIGHT);

				List<Object> originValues = getAsList(match.getOrigin(), reference);
				for (Object value : originValues) {
					Match valueMatch = comparison.getMatch((EObject)value);
					if (valueMatch == null) {
						// out of scope
					} else {
						if (valueMatch.getLeft() == null) {
							featureChange(match, reference, configure, value, DifferenceKind.DELETE,
									DifferenceSource.LEFT);
						}
						if (valueMatch.getRight() == null) {
							featureChange(match, reference, configure, value, DifferenceKind.DELETE,
									DifferenceSource.RIGHT);
						}
					}
				}
			} else {
				createContainmentDifferencesNoOrdering(match, reference, configure, leftValues, DifferenceSource.LEFT);

				for (Object value : rightValues) {
					Match valueMatch = comparison.getMatch((EObject)value);
					if (valueMatch == null) {
						// out of scope
					} else {
						if (valueMatch.getLeft() == null) {
							featureChange(match, reference, configure, value, DifferenceKind.DELETE,
									DifferenceSource.LEFT);
						}
					}
				}
			}
		}
	}

	/**
	 * Delegates the computation of Differences for a given muti-valued feature according to the type of
	 * comparison (two- or three-way), and whether we need to take ordering changes into account.
	 * 
	 * @param match
	 *            The match which sides we need to check for potential differences.
	 * @param feature
	 *            The feature whose values are currently being checked for differences.
	 * @param checkOrdering
	 *            Whether we need to detect ordering changes or ignore them.
	 */
	protected void computeMultiValuedFeatureDifferences(Match match, EStructuralFeature feature, FeatureConfigure configure,
			boolean checkOrdering) {
		Comparison comparison = match.getComparison();

		if (checkOrdering) {
			if (comparison.isThreeWay()) {
				computeMultiValuedFeatureDifferencesThreeWay(match, feature, configure, checkOrdering);
			} else {
				computeMultiValuedFeatureDifferencesTwoWay(match, feature, configure, checkOrdering);
			}
		} else {
			List<Object> leftValues = getAsList(match.getLeft(), feature);
			List<Object> rightValues = getAsList(match.getRight(), feature);
			if (comparison.isThreeWay()) {
				List<Object> originValues = getAsList(match.getOrigin(), feature);
				createMultiValuedFeatureDifferencesNoOrdering(match, feature, configure, leftValues, originValues,
						DifferenceSource.LEFT);
				createMultiValuedFeatureDifferencesNoOrdering(match, feature, configure, rightValues, originValues,
						DifferenceSource.RIGHT);
			} else {
				createMultiValuedFeatureDifferencesNoOrdering(match, feature, configure, leftValues, rightValues,
						DifferenceSource.LEFT);
			}
		}
	}

	/**
	 * Computes the difference between the sides of the given {@code match} for the given containment
	 * {@code reference}.
	 * <p>
	 * This is only meant for three-way comparisons.
	 * </p>
	 * <p>
	 * <b>Note</b> that this is no longer used for references which ordering is not considered, so
	 * {@code checkOrdering} will always be <code>true</code> .
	 * </p>
	 * 
	 * @param match
	 *            The match which sides we need to check for potential differences.
	 * @param reference
	 *            The containment reference which values are to be checked.
	 * @param checkOrdering
	 *            {@code true} if we should consider ordering changes on this reference, {@code false}
	 *            otherwise.
	 */
	protected void computeContainmentDifferencesThreeWay(Match match, EReference reference, FeatureConfigure configure,
			boolean checkOrdering) {
		final Comparison comparison = match.getComparison();

		// We won't use iterables here since we need random access collections for fast LCS.
		final List<Object> leftValues = getAsList(match.getLeft(), reference);
		final List<Object> rightValues = getAsList(match.getRight(), reference);
		final List<Object> originValues = getAsList(match.getOrigin(), reference);

		final List<Object> lcsOriginLeft = DiffUtil.longestCommonSubsequence(comparison, originValues,
				leftValues);
		final List<Object> lcsOriginRight = DiffUtil.longestCommonSubsequence(comparison, originValues,
				rightValues);

		createContainmentDifferences(match, reference, configure, checkOrdering, leftValues, lcsOriginLeft,
				DifferenceSource.LEFT);
		createContainmentDifferences(match, reference, configure, checkOrdering, rightValues, lcsOriginRight,
				DifferenceSource.RIGHT);

		// deleted from either side
		for (Object diffCandidate : originValues) {
			/*
			 * A value that is in the origin but not in either left or right has been deleted or is a moved
			 * element which previously was in this reference. If the latter, we'll detect it later on when
			 * checking its new reference.
			 */
			final Match candidateMatch = comparison.getMatch((EObject)diffCandidate);
			if (candidateMatch == null) {
				// out of scope
			} else {
				if (candidateMatch.getLeft() == null) {
					featureChange(match, reference, configure, diffCandidate, DifferenceKind.DELETE,
							DifferenceSource.LEFT);
				}
				if (candidateMatch.getRight() == null) {
					featureChange(match, reference, configure, diffCandidate, DifferenceKind.DELETE,
							DifferenceSource.RIGHT);
				}
			}
		}
	}

	/**
	 * This will iterate over the given list of values from a containment reference and create the differences
	 * that can be detected from it.
	 * <p>
	 * Ordering changes will not be considered at all from this method. Values that exist in both the given
	 * list of elements and the origin (either ancestor or right side for two-way comparisons), will only have
	 * a Diff if their container or containing reference has changed. If they are still in the same container
	 * and reference, then even if they are not in the same position, we will not try and detect an ordering
	 * change.
	 * </p>
	 * 
	 * @param match
	 *            The match which sides we need to check for potential differences.
	 * @param reference
	 *            The containment reference which values are to be checked.
	 * @param sideValues
	 *            Value of that <code>reference</code> on the given <code>side</code>.
	 * @param side
	 *            The side currently being compared.
	 */
	protected void createContainmentDifferencesNoOrdering(Match match, EReference reference, FeatureConfigure configure, 
			List<Object> sideValues, DifferenceSource side) {
		Comparison comparison = match.getComparison();
		IEqualityHelper equalityHelper = EqualityHelper.getEqualityHelper(comparison);

		int sizeSide = sideValues.size();

		int currentSide = 0;
		while (currentSide < sizeSide) {
			EObject sideValue = (EObject)sideValues.get(currentSide++);
			Match candidateMatch = comparison.getMatch(sideValue);

			if (candidateMatch == null) {
				// out of scope
			} else {
				EObject originValue;
				if (comparison.isThreeWay()) {
					originValue = candidateMatch.getOrigin();
				} else {
					originValue = candidateMatch.getRight();
				}

				if (matchingContainment(equalityHelper, sideValue, originValue)) {
					// Object present in both sides, in the same container and same containment feature.
					// We don't care about ordering so there is no change here.
				} else {
					// Object has either changed container or isn't present in the origin
					if (originValue != null) {
						featureChange(match, reference, configure, sideValue, DifferenceKind.MOVE, side);
					} else {
						featureChange(match, reference, configure, sideValue, DifferenceKind.ADD, side);
					}
				}
			}
		}
	}

	/**
	 * Called from {@link #computeContainmentDifferencesThreeWay(Match, EReference, boolean)} once our LCS
	 * have been computed and we know what really changed. It will be used for both the left and right side.
	 * 
	 * @param match
	 *            The match which sides we need to check for potential differences.
	 * @param reference
	 *            The containment reference which values are to be checked.
	 * @param checkOrdering
	 *            {@code true} if we should consider ordering changes on this reference, {@code false}
	 *            otherwise.
	 * @param values
	 *            Value of that <code>reference</code> on the given <code>side</code>.
	 * @param lcsWithOrigin
	 *            LCS between the reference values on the given <code>side</code> and the values in origin.
	 * @param side
	 *            The side currently being compared.
	 */
	protected void createContainmentDifferences(Match match, EReference reference, FeatureConfigure configure, boolean checkOrdering,
			List<Object> values, List<Object> lcsWithOrigin, DifferenceSource side) {
		final Comparison comparison = match.getComparison();
		final IEqualityHelper equality = EqualityHelper.getEqualityHelper(comparison);

		int lcsCursor = 0;
		Optional<Match> lcsCurrent = getMatchIfPresent(comparison, lcsWithOrigin, lcsCursor);
		for (Object diffCandidate : values) {
			final Match candidateMatch = comparison.getMatch((EObject)diffCandidate);
			// See bug 405000 for this strange iteration on the LCS
			if (candidateMatch == null || lcsCurrent.orElse(null) == candidateMatch) {
				lcsCursor++;
				lcsCurrent = getMatchIfPresent(comparison, lcsWithOrigin, lcsCursor);
				continue;
			}

			final EObject value;
			if (side == DifferenceSource.LEFT) {
				value = candidateMatch.getLeft();
			} else {
				value = candidateMatch.getRight();
			}
			final EObject originValue;
			if (comparison.isThreeWay()) {
				originValue = candidateMatch.getOrigin();
			} else {
				originValue = candidateMatch.getRight();
			}

			if (matchingContainment(equality, value, originValue)) {
				/*
				 * Contained in both compared side and the origin, and not part of the LCS. It has moved
				 * within its (containment) reference.
				 */
				if (checkOrdering) {
					featureChange(match, reference, configure, diffCandidate, DifferenceKind.MOVE, side);
				}
			} else {
				/*
				 * This element is in different containers in the compared side and origin (if it is in origin
				 * at all).
				 */
				if (originValue != null) {
					featureChange(match, reference, configure, diffCandidate, DifferenceKind.MOVE, side);
				} else {
					featureChange(match, reference, configure, diffCandidate, DifferenceKind.ADD, side);
				}
			}
		}
	}

	/**
	 * Checks whether the two given EObjects are contained within the same object, under the same reference.
	 * 
	 * @param equalityHelper
	 *            Our current equality helper.
	 * @param o1
	 *            First of the two EObjects to compare.
	 * @param o2
	 *            Second of the two EObjects to compare.
	 * @return <code>true</code> if these two objects are contained within the same container, false
	 *         otherwise.
	 */
	protected boolean matchingContainment(IEqualityHelper equalityHelper, EObject o1, EObject o2) {
		if (o1 == null || o2 == null) {
			return false;
		}

		boolean matchingContainment = false;

		final EObject container1 = o1.eContainer();
		final EObject container2 = o2.eContainer();
		if (container1 != null && container2 != null) {
			final EReference containing1 = o1.eContainmentFeature();
			final EReference containing2 = o2.eContainmentFeature();
			matchingContainment = (containing1 == containing2
					|| containing1.getName().equals(containing2.getName()))
					&& equalityHelper.matchingValues(o1.eContainer(), o2.eContainer());
		}
		return matchingContainment;
	}

	/**
	 * This will be used in order to read the LCS synchronously with the iteration on its target lists'
	 * values. This should be used cautiously since it will work on empty lists, null values and out-of-scope
	 * objects.
	 * 
	 * @param comparison
	 *            The current comparison.
	 * @param list
	 *            A list of EObjects. May be empty or contain out-of-scope values.
	 * @param index
	 *            Index of the object we seek within this list.
	 * @return An optional containing the match of the object at the given index... or
	 *         {@link Optional#absent()}.
	 */
	protected static Optional<Match> getMatchIfPresent(final Comparison comparison, List<Object> list,
			int index) {
		Optional<Match> value = Optional.empty();
		if (list.size() > index) {
			EObject current = (EObject)list.get(index);
			if (current != null) {
				value = Optional.ofNullable(comparison.getMatch(current));
			}
		}
		return value;
	}

	/**
	 * This will be used in order to read the LCS synchronously with the iteration on its target lists'
	 * values. This should be used cautiously since it will work on empty lists, and null values contained in
	 * the list are treated the same as an empty list and considered to be Optional.absent().
	 * 
	 * @param list
	 *            A list of EObjects. May be empty or contain null or out-of-scope values.
	 * @param index
	 *            Index of the object we seek within this list.
	 * @return An optional containing the object at the given index... or {@link Optional#absent()}.
	 */
	protected static Optional<Object> getIfPresent(List<Object> list, int index) {
		Optional<Object> value = Optional.empty();
		if (list.size() > index) {
			value = Optional.ofNullable(list.get(index));
		}
		return value;
	}

	/**
	 * Computes the difference between the sides of the given {@code match} for the given containment
	 * {@code reference}.
	 * <p>
	 * This is only meant for two-way comparisons.
	 * </p>
	 * <p>
	 * <b>Note</b> that this is no longer used for references which ordering is not considered, so
	 * {@code checkOrdering} will always be <code>true</code> .
	 * </p>
	 * 
	 * @param match
	 *            The match which sides we need to check for potential differences.
	 * @param reference
	 *            The containment reference which values are to be checked.
	 * @param checkOrdering
	 *            {@code true} if we should consider ordering changes on this reference, {@code false}
	 *            otherwise.
	 */
	protected void computeContainmentDifferencesTwoWay(Match match, EReference reference, FeatureConfigure configure,
			boolean checkOrdering) {
		final Comparison comparison = match.getComparison();

		final List<Object> leftValues = getAsList(match.getLeft(), reference);
		final List<Object> rightValues = getAsList(match.getRight(), reference);

		final List<Object> lcs = DiffUtil.longestCommonSubsequence(comparison, rightValues, leftValues);

		createContainmentDifferences(match, reference, configure, checkOrdering, leftValues, lcs, DifferenceSource.LEFT);

		// deleted
		for (Object diffCandidate : rightValues) {
			/*
			 * A value that is in the right but not in the left either has been deleted or is a moved element
			 * which previously was in this reference. We'll detect the move on its new reference.
			 */
			final Match candidateMatch = comparison.getMatch((EObject)diffCandidate);
			if (candidateMatch == null) {
				// out of scope
			} else if (candidateMatch.getLeft() == null) {
				featureChange(match, reference, configure, diffCandidate, DifferenceKind.DELETE, DifferenceSource.LEFT);
			}
		}
	}

	/**
	 * Computes the difference between the sides of the given <code>match</code> for the given
	 * <code>attribute</code>.
	 * 
	 * @param match
	 *            The match which sides we need to check for potential differences.
	 * @param attribute
	 *            The attribute which values are to be checked.
	 * @param checkOrdering
	 *            <code>true</code> if we should consider ordering changes on this attribute,
	 *            <code>false</code> otherwise.
	 */
	protected void computeDifferences(Match match, EAttribute attribute, FeatureConfigure configure, boolean checkOrdering) {
		final Comparison comparison = match.getComparison();

		// This default implementation does not care about "attribute" changes on added/removed elements
		boolean shortcut = false;
		if (comparison.isThreeWay()) {
			shortcut = match.getOrigin() == null;
		} else {
			shortcut = match.getLeft() == null || match.getRight() == null;
		}

		// Do not shortcut when multi-valued FeatureMaps are affected to keep their ordering intact
		if (shortcut && FeatureMapUtil.isFeatureMap(attribute)) {
			final EObject owner = getOwner(match);
			if (owner != null && FeatureMapUtil.isMany(owner, attribute)) {
				shortcut = false;
			}
		}

		if (shortcut) {
			return;
		}

		if (attribute.isMany()) {
			if (comparison.isThreeWay()) {
				computeMultiValuedFeatureDifferencesThreeWay(match, attribute, configure, checkOrdering);
			} else {
				computeMultiValuedFeatureDifferencesTwoWay(match, attribute, configure, checkOrdering);
			}
		} else {
			computeSingleValuedAttributeDifferences(match, attribute, configure);
		}
	}

	/**
	 * Returns one side of the match if any exist. The order of the checked sides is Origin, Left and Right.
	 *
	 * @param match
	 *            The match whose sides are checked.
	 * @return Either Origin, Left or Right if one of them exists, {@code null} otherwise.
	 */
	protected EObject getOwner(Match match) {
		EObject owner = match.getOrigin();
		if (owner == null) {
			owner = match.getLeft();
			if (owner == null) {
				owner = match.getRight();
			}
		}
		return owner;
	}

	/**
	 * Computes the difference between the sides of the given <code>match</code> for the given
	 * <code>reference</code>.
	 * 
	 * @param match
	 *            The match which sides we need to check for potential differences.
	 * @param reference
	 *            The reference which values are to be checked.
	 * @param checkOrdering
	 *            <code>true</code> if we should consider ordering changes on this reference,
	 *            <code>false</code> otherwise.
	 */
	protected void computeDifferences(Match match, EReference reference, FeatureConfigure configure, boolean checkOrdering) {
		final Comparison comparison = match.getComparison();

		if (reference.isContainment()) {
			computeContainmentDifferences(match, reference, configure, checkOrdering);
		} else if (reference.isMany()) {
			computeMultiValuedFeatureDifferences(match, reference, configure, checkOrdering);
		} else {
			if (comparison.isThreeWay()) {
				computeSingleValuedReferenceDifferencesThreeWay(match, reference, configure);
			} else {
				computeSingleValuedReferenceDifferencesTwoWay(match, reference, configure);
			}
		}
	}

	/**
	 * Computes the difference between the sides of the given {@code match} for the given multi-valued
	 * {@code feature}.
	 * <p>
	 * The given {@code feature} cannot be a containment reference.
	 * </p>
	 * <p>
	 * This is only meant for three-way comparisons.
	 * </p>
	 * <p>
	 * <b>Note</b> that this is no longer used for features which ordering is not considered, so
	 * {@code checkOrdering} will always be <code>true</code> .
	 * </p>
	 * 
	 * @param match
	 *            The match which sides we need to check for potential differences.
	 * @param feature
	 *            The feature which values are to be checked.
	 * @param checkOrdering
	 *            {@code true} if we should consider ordering changes on this feature, {@code false}
	 *            otherwise.
	 */
	protected void computeMultiValuedFeatureDifferencesThreeWay(Match match, EStructuralFeature feature, FeatureConfigure configure, 
			boolean checkOrdering) {
		final Comparison comparison = match.getComparison();
		final IEqualityHelper equality = EqualityHelper.getEqualityHelper(comparison);

		// We won't use iterables here since we need random access collections for fast LCS.
		final List<Object> leftValues = getAsList(match.getLeft(), feature);
		final List<Object> rightValues = getAsList(match.getRight(), feature);
		final List<Object> originValues = getAsList(match.getOrigin(), feature);

		final List<Object> lcsOriginLeft = DiffUtil.longestCommonSubsequence(comparison, originValues,
				leftValues);
		final List<Object> lcsOriginRight = DiffUtil.longestCommonSubsequence(comparison, originValues,
				rightValues);

		// Any value that is _not_ in the LCS has changed.

		List<Object> originWithNoLCS = trimLCS(originValues, lcsOriginLeft, equality);
		int lcsCursor = 0;
		Optional<Object> lcsCurrent = getIfPresent(lcsOriginLeft, lcsCursor);
		for (Object diffCandidate : leftValues) {
			// See bug 405000 for this strange iteration on the LCS
			if (equality.matchingValues(diffCandidate, lcsCurrent.orElse(null))) {
				lcsCursor++;
				lcsCurrent = getIfPresent(lcsOriginLeft, lcsCursor);
				continue;
			}

			int index = indexOf(comparison, originWithNoLCS, diffCandidate);
			if (index >= 0) {
				originWithNoLCS.remove(index);
				if (checkOrdering) {
					featureChange(match, feature, configure, diffCandidate, DifferenceKind.MOVE, DifferenceSource.LEFT);
				}
			} else if (FeatureMapUtil.isFeatureMap(feature) && diffCandidate instanceof FeatureMap.Entry) {
				// A value of a FeatureMap changed its key
				// TODO Could feature map have duplicate entries and require the same "!isUnique" treatment?
				if (isFeatureMapEntryKeyChange(equality, (FeatureMap.Entry)diffCandidate, originValues)) {
					featureChange(match, feature, configure, diffCandidate, DifferenceKind.CHANGE,
							DifferenceSource.LEFT);
				} else if (isFeatureMapValueMove(comparison, (FeatureMap.Entry)diffCandidate,
						DifferenceSource.LEFT)) {
					featureChange(match, feature, configure, diffCandidate, DifferenceKind.MOVE, DifferenceSource.LEFT);
				} else {
					featureChange(match, feature, configure, diffCandidate, DifferenceKind.ADD, DifferenceSource.LEFT);
				}
			} else {
				featureChange(match, feature, configure, diffCandidate, DifferenceKind.ADD, DifferenceSource.LEFT);
			}
		}

		// A Value that is not in the left but present in the origin has been deleted
		List<Object> leftWithNoLCS = trimLCS(leftValues, lcsOriginLeft, equality);
		for (Object diffCandidate : originWithNoLCS) {
			int indexLeft = indexOf(comparison, leftWithNoLCS, diffCandidate);
			if (indexLeft == -1) {
				if ((feature instanceof EReference || match.getLeft() != null)
						&& !isFeatureMapChangeOrMove(comparison, feature, diffCandidate, leftValues,
								DifferenceSource.LEFT)) {
					featureChange(match, feature, configure, diffCandidate, DifferenceKind.DELETE,
							DifferenceSource.LEFT);
				}
			} else if (!feature.isUnique()) {
				leftWithNoLCS.remove(indexLeft);
			}
		}

		originWithNoLCS = trimLCS(originValues, lcsOriginRight, equality);
		lcsCursor = 0;
		lcsCurrent = getIfPresent(lcsOriginRight, lcsCursor);
		for (Object diffCandidate : rightValues) {
			// See bug 405000 for this strange iteration on the LCS
			if (equality.matchingValues(diffCandidate, lcsCurrent.orElse(null))) {
				lcsCursor++;
				lcsCurrent = getIfPresent(lcsOriginRight, lcsCursor);
				continue;
			}

			int index = indexOf(comparison, originWithNoLCS, diffCandidate);
			if (index >= 0) {
				originWithNoLCS.remove(index);
				if (checkOrdering) {
					featureChange(match, feature, configure, diffCandidate, DifferenceKind.MOVE, DifferenceSource.RIGHT);
				}
			} else if (FeatureMapUtil.isFeatureMap(feature) && diffCandidate instanceof FeatureMap.Entry) {
				// A value of a FeatureMap changed its key
				if (isFeatureMapEntryKeyChange(equality, (FeatureMap.Entry)diffCandidate, originValues)) {
					featureChange(match, feature, configure, diffCandidate, DifferenceKind.CHANGE,
							DifferenceSource.RIGHT);
				} else if (isFeatureMapValueMove(comparison, (FeatureMap.Entry)diffCandidate,
						DifferenceSource.RIGHT)) {
					featureChange(match, feature, configure, diffCandidate, DifferenceKind.MOVE, DifferenceSource.RIGHT);
				} else {
					featureChange(match, feature, configure, diffCandidate, DifferenceKind.ADD, DifferenceSource.RIGHT);
				}
			} else {
				featureChange(match, feature, configure, diffCandidate, DifferenceKind.ADD, DifferenceSource.RIGHT);
			}
		}

		// A Value that is not in the right but present in the origin has been deleted
		List<Object> rightWithNoLCS = trimLCS(rightValues, lcsOriginRight, equality);
		for (Object diffCandidate : originWithNoLCS) {
			int indexRight = indexOf(comparison, rightWithNoLCS, diffCandidate);
			if (indexRight == -1) {
				if ((feature instanceof EReference || match.getRight() != null)
						&& !isFeatureMapChangeOrMove(comparison, feature, diffCandidate, rightValues,
								DifferenceSource.RIGHT)) {
					featureChange(match, feature, configure, diffCandidate, DifferenceKind.DELETE,
							DifferenceSource.RIGHT);
				}
			} else if (!feature.isUnique()) {
				rightWithNoLCS.remove(indexRight);
			}
		}
	}

	/**
	 * This will iterate over the given list of values from a multi-valued feature and create the differences
	 * that can be detected from it.
	 * <p>
	 * Ordering changes will not be considered at all from this method. Values that exist in both the given
	 * list of elements and the origin (either ancestor or right side for two-way comparisons), will never
	 * have a Diff even if they are not at the same index in the list of their respective side.
	 * </p>
	 * 
	 * @param match
	 *            The match which sides we need to check for potential differences.
	 * @param feature
	 *            The feature which values are to be checked. Cannot be a containment reference.
	 * @param sideValues
	 *            Value of that <code>reference</code> on the given <code>side</code>.
	 * @param originValues
	 *            Value of that <code>reference</code> on the origin side. Could be the common ancestor or the
	 *            right side in case of two-way comparisons.
	 * @param side
	 *            The side currently being compared.
	 */
	protected void createMultiValuedFeatureDifferencesNoOrdering(Match match, EStructuralFeature feature, FeatureConfigure configure, 
			List<Object> sideValues, List<Object> originValues, DifferenceSource side) {
		Comparison comparison = match.getComparison();
		IEqualityHelper equalityHelper = EqualityHelper.getEqualityHelper(comparison);

		List<Object> originCopy = new ArrayList<>(originValues);

		int sizeSide = sideValues.size();
		int currentSide = 0;
		while (currentSide < sizeSide) {
			Object sideValue = sideValues.get(currentSide++);

			int currentOrigin = 0;
			int sizeOrigin = originCopy.size();

			boolean matching = false;
			while (currentOrigin < sizeOrigin && !matching) {
				Object originValue = originCopy.get(currentOrigin);
				if (equalityHelper.matchingValues(sideValue, originValue)) {
					originCopy.remove(currentOrigin);
					matching = true;
				}
				currentOrigin++;
			}

			if (!matching) {
				// This object is in the side checked, but not in the origin
				if (FeatureMapUtil.isFeatureMap(feature) && sideValue instanceof FeatureMap.Entry) {
					// A value of a FeatureMap changed its key
					if (isFeatureMapEntryKeyChange(equalityHelper, (FeatureMap.Entry)sideValue,
							originValues)) {
						featureChange(match, feature, configure, sideValue, DifferenceKind.CHANGE, side);
					} else if (isFeatureMapValueMove(comparison, (FeatureMap.Entry)sideValue, side)) {
						featureChange(match, feature, configure, sideValue, DifferenceKind.MOVE, side);
					} else {
						featureChange(match, feature, configure, sideValue, DifferenceKind.ADD, side);
					}
				} else {
					featureChange(match, feature, configure, sideValue, DifferenceKind.ADD, side);
				}
			}
		}

		// Objects that are still present in "originCopy" now have been deleted from the current side.
		// We want all reference changes, but if the current side has been deleted, we do not want to have
		// attribute changes under this deletion.
		boolean sideDeleted;
		if (side == DifferenceSource.LEFT) {
			sideDeleted = match.getLeft() == null;
		} else {
			sideDeleted = match.getRight() == null;
		}
		for (Object originValue : originCopy) {
			if ((feature instanceof EReference || !sideDeleted)
					&& !isFeatureMapChangeOrMove(comparison, feature, originValue, sideValues, side)) {
				featureChange(match, feature, configure, originValue, DifferenceKind.DELETE, side);
			}
		}
	}

	/**
	 * Computes the difference between the sides of the given {@code match} for the given multi-valued
	 * {@code feature}.
	 * <p>
	 * The given {@code feature} cannot be a containment reference.
	 * </p>
	 * <p>
	 * This is only meant for two-way comparisons.
	 * </p>
	 * <p>
	 * <b>Note</b> that this is no longer used for features which ordering is not considered, so
	 * {@code checkOrdering} will always be <code>true</code> .
	 * </p>
	 * 
	 * @param match
	 *            The match which sides we need to check for potential differences.
	 * @param feature
	 *            The feature which values are to be checked.
	 * @param checkOrdering
	 *            {@code true} if we should consider ordering changes on this feature, {@code false}
	 *            otherwise.
	 */
	protected void computeMultiValuedFeatureDifferencesTwoWay(Match match, EStructuralFeature feature, FeatureConfigure configure,
			boolean checkOrdering) {
		final Comparison comparison = match.getComparison();
		final IEqualityHelper equality = EqualityHelper.getEqualityHelper(comparison);

		// We won't use iterables here since we need random access collections for fast LCS.
		final List<Object> leftValues = getAsList(match.getLeft(), feature);
		final List<Object> rightValues = getAsList(match.getRight(), feature);

		final List<Object> lcs = DiffUtil.longestCommonSubsequence(comparison, rightValues, leftValues);

		List<Object> rightWithNoLCS = trimLCS(rightValues, lcs, equality);
		int lcsCursor = 0;
		Optional<Object> lcsCurrent = getIfPresent(lcs, lcsCursor);
		for (Object diffCandidate : leftValues) {
			// See bug 405000 for this strange iteration on the LCS
			if (equality.matchingValues(diffCandidate, lcsCurrent.orElse(null))) {
				lcsCursor++;
				lcsCurrent = getIfPresent(lcs, lcsCursor);
				continue;
			}

			int index = indexOf(comparison, rightWithNoLCS, diffCandidate);
			if (index >= 0) {
				rightWithNoLCS.remove(index);
				if (checkOrdering) {
					featureChange(match, feature, configure, diffCandidate, DifferenceKind.MOVE, DifferenceSource.LEFT);
				}
			} else if (FeatureMapUtil.isFeatureMap(feature) && diffCandidate instanceof FeatureMap.Entry) {
				// A value of a FeatureMap changed his key
				if (isFeatureMapEntryKeyChange(equality, (FeatureMap.Entry)diffCandidate, rightValues)) {
					featureChange(match, feature, configure, diffCandidate, DifferenceKind.CHANGE,
							DifferenceSource.LEFT);
				} else if (isFeatureMapValueMove(comparison, (FeatureMap.Entry)diffCandidate,
						DifferenceSource.LEFT)) {
					featureChange(match, feature, configure, diffCandidate, DifferenceKind.MOVE, DifferenceSource.LEFT);
				} else {
					featureChange(match, feature, configure, diffCandidate, DifferenceKind.ADD, DifferenceSource.LEFT);
				}
			} else {
				featureChange(match, feature, configure, diffCandidate, DifferenceKind.ADD, DifferenceSource.LEFT);
			}
		}

		List<Object> leftWithNoLCS = trimLCS(leftValues, lcs, equality);
		for (Object diffCandidate : rightWithNoLCS) {
			int index = indexOf(comparison, leftWithNoLCS, diffCandidate);
			if (index == -1) {
				// A value that is in the right but not in the left has been deleted or moved.
				if (isFeatureMapMoveFromNonFeatureMapContainment(comparison, feature, diffCandidate,
						leftValues, DifferenceSource.LEFT)) {
					// add move change if the move originates from a non-feature-map containment.
					featureChange(match, feature, configure, diffCandidate, DifferenceKind.MOVE, DifferenceSource.LEFT);
				} else if (!isFeatureMapChangeOrMove(comparison, feature, diffCandidate, leftValues,
						DifferenceSource.LEFT)) {
					featureChange(match, feature, configure, diffCandidate, DifferenceKind.DELETE,
							DifferenceSource.LEFT);
				}
			} else if (!feature.isUnique()) {
				leftWithNoLCS.remove(index);
			}
		}
	}

	/**
	 * Removes all elements from the given lcs from the given source list.
	 * 
	 * @param source
	 *            The list we need to trim of its LCS-contained elements.
	 * @param lcs
	 *            The LCS between this list and another.
	 * @param equalityHelper
	 *            The equality helper used to determine which elements of <code>source</code> are contained in
	 *            <code>lcs</code>.
	 * @return The sub-list of <code>source</code> without any of its lcs elements.
	 */
	private List<Object> trimLCS(List<Object> source, List<Object> lcs, IEqualityHelper equalityHelper) {
		List<Object> result = new ArrayList<>();
		Iterator<Object> sourceIterator = source.iterator();
		int lcsCursor = 0;
		Optional<Object> lcsCurrent = getIfPresent(lcs, lcsCursor);
		while (sourceIterator.hasNext() && lcsCurrent.isPresent()) {
			Object current = sourceIterator.next();
			if (equalityHelper.matchingValues(current, lcsCurrent.get())) {
				lcsCursor++;
				lcsCurrent = getIfPresent(lcs, lcsCursor);
			} else {
				result.add(current);
			}
		}
		while (sourceIterator.hasNext()) {
			result.add(sourceIterator.next());
		}
		return result;
	}

	/**
	 * Checks if the given candidate is a FeatureMap change of type DifferenceKind.CHANGE.
	 *
	 * @param comparison
	 *            The comparison object.
	 * @param feature
	 *            The feature which values are to be checked.
	 * @param diffCandidate
	 *            The given candidate for which we search an equivalent value.
	 * @param values
	 *            The entries in which we search.
	 * @param source
	 *            The given DifferenceSource of the entry.
	 * @return true if the given candidate is a FeatureMap change of type DifferenceKind.CHANGE, false
	 *         otherwise.
	 */
	private boolean isFeatureMapChange(final Comparison comparison, final EStructuralFeature feature,
			final Object diffCandidate, final List<Object> values, final DifferenceSource source) {
		return FeatureMapUtil.isFeatureMap(feature)
				&& (isFeatureMapEntryKeyChange(EqualityHelper.getEqualityHelper(comparison),
						(FeatureMap.Entry)diffCandidate, values));
	}

	/**
	 * Checks if the given candidate is a FeatureMap change of type DifferenceKind.MOVE.
	 *
	 * @param comparison
	 *            The comparison object.
	 * @param feature
	 *            The feature which values are to be checked.
	 * @param diffCandidate
	 *            The given candidate for which we search an equivalent value.
	 * @param values
	 *            The entries in which we search.
	 * @param source
	 *            The given DifferenceSource of the entry.
	 * @return true if the given candidate is a FeatureMap change of type DifferenceKind.MOVE, false
	 *         otherwise.
	 */
	private boolean isFeatureMapMove(final Comparison comparison, final EStructuralFeature feature,
			final Object diffCandidate, final List<Object> values, final DifferenceSource source) {
		return FeatureMapUtil.isFeatureMap(feature)
				&& isFeatureMapValueMove(comparison, (FeatureMap.Entry)diffCandidate, source);
	}

	/**
	 * Checks if the given candidate is a FeatureMap change of type DifferenceKind.MOVE which originates from
	 * a Non-FeatureMap-Containment.
	 *
	 * @param comparison
	 *            The comparison object.
	 * @param feature
	 *            The feature which values are to be checked.
	 * @param diffCandidate
	 *            The given candidate for which we search an equivalent value.
	 * @param values
	 *            The entries in which we search.
	 * @param source
	 *            The given DifferenceSource of the entry.
	 * @return true if the given candidate is a FeatureMap change of type DifferenceKind.MOVE, false
	 *         otherwise.
	 */
	private boolean isFeatureMapMoveFromNonFeatureMapContainment(final Comparison comparison,
			final EStructuralFeature feature, final Object diffCandidate, final List<Object> values,
			final DifferenceSource source) {
		if (isFeatureMapMove(comparison, feature, diffCandidate, values, source)) {
			final Object entryValue = ((FeatureMap.Entry)diffCandidate).getValue();
			if (entryValue instanceof EObject) {
				Match match = comparison.getMatch((EObject)entryValue);
				if (match == null) {
					// out of scope
				} else {
					final EObject leftObject = match.getLeft();
					return !ComparisonUtil.isContainedInFeatureMap(leftObject);
				}
			}
		}
		return false;
	}

	/**
	 * Checks if the given candidate is a FeatureMap change of type DifferenceKind.CHANGE or
	 * DifferenceKind.MOVE.
	 * 
	 * @param comparison
	 *            The comparison object.
	 * @param feature
	 *            The feature which values are to be checked.
	 * @param diffCandidate
	 *            The given candidate for which we search an equivalent value.
	 * @param values
	 *            The entries in which we search.
	 * @param source
	 *            The given DifferenceSource of the entry.
	 * @return true if the given candidate is a FeatureMap change of type DifferenceKind.CHANGE or
	 *         DifferenceKind.MOVE, false otherwise.
	 */
	private boolean isFeatureMapChangeOrMove(final Comparison comparison, final EStructuralFeature feature,
			final Object diffCandidate, final List<Object> values, final DifferenceSource source) {
		return isFeatureMapChange(comparison, feature, diffCandidate, values, source)
				|| isFeatureMapMove(comparison, feature, diffCandidate, values, source);
	}

	/**
	 * Checks if the entry has its value equivalent in the list of entries, with a different key.
	 * 
	 * @param equality
	 *            Use to compare objects by the org.eclipse.emf.compare.match.IMatchEngine.
	 * @param entry
	 *            The given FeatureMap.Entry for which we search an equivalent value.
	 * @param entries
	 *            The entries in which we search.
	 * @return true if the entry has its value equivalent in the list of entries, with a different key, false
	 *         otherwise.
	 */
	private boolean isFeatureMapEntryKeyChange(final IEqualityHelper equality, final FeatureMap.Entry entry,
			final List<Object> entries) {
		final Object entryValue = entry.getValue();
		final EStructuralFeature entryKey = entry.getEStructuralFeature();
		if (DiffUtil.isContainmentReference(entryKey)) {
			for (Object object : entries) {
				if (object instanceof FeatureMap.Entry) {
					final FeatureMap.Entry featureMapEntry = (FeatureMap.Entry)object;
					if (equality.matchingValues(entryValue, featureMapEntry.getValue())) {
						return !entryKey.equals(featureMapEntry.getEStructuralFeature());
					}
				}
			}
		}
		return false;
	}

	/**
	 * Checks if the entry's value has its equivalent in the opposite side, and thus is a DifferenceKind.MOVE
	 * difference. If the FeatureMapEntry is non-contained the method will return {@code false}.
	 * 
	 * @param comparison
	 *            The comparison object.
	 * @param entry
	 *            The FeatureMap.Entry which contains the value for which we try to find its equivalent.
	 * @param side
	 *            The given DifferenceSource of the entry.
	 * @return {@code true} if the entry's value has its equivalent in the opposite side and is contained
	 *         within the feature map, {@code false} otherwise.
	 */
	private boolean isFeatureMapValueMove(final Comparison comparison, FeatureMap.Entry entry,
			DifferenceSource side) {
		final boolean move;
		final Object entryValue = entry.getValue();
		final EStructuralFeature structuralFeature = entry.getEStructuralFeature();

		if (entryValue instanceof EObject && DiffUtil.isContainmentReference(structuralFeature)) {
			final Match candidateMatch = comparison.getMatch((EObject)entryValue);

			if (candidateMatch == null) {
				move = false;
			} else {
				final EObject value;
				if (side == DifferenceSource.LEFT) {
					value = candidateMatch.getLeft();
				} else {
					value = candidateMatch.getRight();
				}
				final EObject oppositeValue;
				if (comparison.isThreeWay()) {
					oppositeValue = candidateMatch.getOrigin();
				} else {
					oppositeValue = candidateMatch.getRight();
				}

				if (value != null && oppositeValue != null) {
					move = !EqualityHelper.getEqualityHelper(comparison).matchingValues(value.eContainer(),
							oppositeValue.eContainer());
				} else {
					move = false;
				}
			}
		} else {
			move = false;
		}
		return move;

	}

	/**
	 * Computes the difference between the sides of the given <code>match</code> for the given single-valued
	 * <code>attribute</code>.
	 * 
	 * @param match
	 *            The match which sides we need to check for potential differences.
	 * @param attribute
	 *            The attribute which values are to be checked.
	 */
	protected void computeSingleValuedAttributeDifferences(Match match, EAttribute attribute, FeatureConfigure configure) {
		final Comparison comparison = match.getComparison();
		final EObject left = match.getLeft();
		Object leftValue = UNMATCHED_VALUE;
		if (left != null) {
			leftValue = safeEGet(left, attribute);
		}
		final EObject right = match.getRight();
		Object rightValue = UNMATCHED_VALUE;
		if (right != null) {
			rightValue = safeEGet(right, attribute);
		}

		final IEqualityHelper helper = EqualityHelper.getEqualityHelper(comparison);
		final EObject origin = match.getOrigin();
		if (helper.matchingValues(leftValue, rightValue)) {
			// Identical values in left and right. The only problematic case is if they do not match the
			// origin (and left and right are defined, i.e don't detect attribute change on unmatched)
			if (leftValue != UNMATCHED_VALUE && comparison.isThreeWay()) {
				final Object originValue;
				if (origin == null) {
					originValue = null;
				} else {
					originValue = safeEGet(origin, attribute);
				}
				final boolean matchingLO = helper.matchingValues(leftValue, originValue);

				// if !matchingLO, the same change has been made on both side. This is actually a
				// pseudo-conflict.
				if (!matchingLO && !isNullOrEmptyString(leftValue)) {
					// The same value has been SET or CHANGED on both sides
					// (depending on whether origin is null or not)
					getDiffProcessor().attributeChange(match, attribute, configure, leftValue, DifferenceKind.CHANGE,
							DifferenceSource.LEFT);
					getDiffProcessor().attributeChange(match, attribute, configure, rightValue, DifferenceKind.CHANGE,
							DifferenceSource.RIGHT);
				} else if (!matchingLO) {
					// The same value has been UNSET from both sides
					getDiffProcessor().attributeChange(match, attribute, configure, originValue, DifferenceKind.CHANGE,
							DifferenceSource.LEFT);
					getDiffProcessor().attributeChange(match, attribute, configure, originValue, DifferenceKind.CHANGE,
							DifferenceSource.RIGHT);
				}
			}
		} else if (origin != null) {
			final Object originValue = safeEGet(origin, attribute);

			if (helper.matchingValues(leftValue, originValue)) {
				Object changedValue = rightValue;
				if (isNullOrEmptyString(rightValue)) {
					changedValue = originValue;
				}

				if (rightValue != UNMATCHED_VALUE) {
					// Value is in left and origin, but not in the right
					getDiffProcessor().attributeChange(match, attribute, configure, changedValue, DifferenceKind.CHANGE,
							DifferenceSource.RIGHT);
				} else {
					// Right is unmatched, left is the same as in the origin. No diff here : the diff is on
					// the match itself, not on one of its attributes.
				}
			} else if (helper.matchingValues(rightValue, originValue)) {
				Object changedValue = leftValue;
				if (isNullOrEmptyString(leftValue)) {
					changedValue = originValue;
				}

				if (leftValue != UNMATCHED_VALUE) {
					// Value is in right and origin, but not in left
					getDiffProcessor().attributeChange(match, attribute, configure, changedValue, DifferenceKind.CHANGE,
							DifferenceSource.LEFT);
				} else {
					// Left is unmatched, right is the same as in the origin. No diff here : the diff is on
					// the match itself, not on one of its attributes.
				}
			} else {
				/*
				 * Left and right are different. None match what's in the origin. Those of the two that are
				 * not unmatched are thus a "change" difference, with a possible conflict.
				 */
				Object leftChange = leftValue;
				if (isNullOrEmptyString(leftValue)) {
					leftChange = originValue;
				}
				Object rightChange = rightValue;
				if (isNullOrEmptyString(rightValue)) {
					rightChange = originValue;
				}

				if (leftValue != UNMATCHED_VALUE) {
					getDiffProcessor().attributeChange(match, attribute, configure, leftChange, DifferenceKind.CHANGE,
							DifferenceSource.LEFT);
				}
				if (rightValue != UNMATCHED_VALUE) {
					getDiffProcessor().attributeChange(match, attribute, configure, rightChange, DifferenceKind.CHANGE,
							DifferenceSource.RIGHT);
				}
			}
		} else {
			// Left and right values are different, and we have no origin.
			if (leftValue != UNMATCHED_VALUE) {
				if (isNullOrEmptyString(leftValue)) {
					getDiffProcessor().attributeChange(match, attribute, configure, rightValue, DifferenceKind.CHANGE,
							DifferenceSource.LEFT);
				} else {
					getDiffProcessor().attributeChange(match, attribute, configure, leftValue, DifferenceKind.CHANGE,
							DifferenceSource.LEFT);
				}
			}
			if (comparison.isThreeWay() && rightValue != UNMATCHED_VALUE) {
				if (isNullOrEmptyString(rightValue)) {
					getDiffProcessor().attributeChange(match, attribute, configure, leftValue, DifferenceKind.CHANGE,
							DifferenceSource.RIGHT);
				} else {
					getDiffProcessor().attributeChange(match, attribute, configure, rightValue, DifferenceKind.CHANGE,
							DifferenceSource.RIGHT);
				}
			}
		}
	}

	/**
	 * Returns {@code true} if the given {@code object} is {@code null} or the empty String.
	 * 
	 * @param object
	 *            The object we need to test.
	 * @return {@code true} if the given {@code object} is {@code null} or the empty String.
	 */
	private boolean isNullOrEmptyString(Object object) {
		return object == null || "".equals(object); //$NON-NLS-1$
	}

	/**
	 * Returns {@code true} if the given {@code object} is {@code null} or the {@link #UNMATCHED_VALUE}.
	 * 
	 * @param object
	 *            The object we need to test.
	 * @return {@code true} if the given {@code object} is {@code null} or the {@link #UNMATCHED_VALUE}.
	 */
	private boolean isNullOrUnmatched(Object object) {
		return object == null || object == UNMATCHED_VALUE;
	}

	/**
	 * Computes the difference between the sides of the given <code>match</code> for the given single-valued
	 * <code>reference</code>.
	 * <p>
	 * The given {@code reference} cannot be a containment reference.
	 * </p>
	 * <p>
	 * This is only meant for three-way comparisons.
	 * </p>
	 * 
	 * @param match
	 *            The match which sides we need to check for potential differences.
	 * @param reference
	 *            The reference which values are to be checked.
	 */
	protected void computeSingleValuedReferenceDifferencesThreeWay(Match match, EReference reference, FeatureConfigure configure) {
		final Comparison comparison = match.getComparison();

		final EObject left = match.getLeft();
		Object leftValue = UNMATCHED_VALUE;
		if (left != null) {
			leftValue = safeEGet(left, reference);
		}
		final EObject right = match.getRight();
		Object rightValue = UNMATCHED_VALUE;
		if (right != null) {
			rightValue = safeEGet(right, reference);
		}
		final EObject origin = match.getOrigin();
		Object originValue = UNMATCHED_VALUE;
		if (origin != null) {
			originValue = safeEGet(origin, reference);
		}

		boolean distinctValueLO = !EqualityHelper.getEqualityHelper(comparison).matchingValues(leftValue, originValue);
		// consider null and unmatched as the same
		distinctValueLO = distinctValueLO
				&& !(isNullOrUnmatched(leftValue) && isNullOrUnmatched(originValue));

		if (distinctValueLO) {
			// Left and origin are distinct
			if (leftValue == null || leftValue == UNMATCHED_VALUE) {
				// Left has been removed
				getDiffProcessor().referenceChange(match, reference, configure, (EObject)originValue,
						DifferenceKind.CHANGE, DifferenceSource.LEFT);
			} else {
				// Left has been set to a new value, or left has been added altogether
				getDiffProcessor().referenceChange(match, reference, configure, (EObject)leftValue,
						DifferenceKind.CHANGE, DifferenceSource.LEFT);
			}
		}

		boolean distinctValueRO = !EqualityHelper.getEqualityHelper(comparison).matchingValues(rightValue, originValue);
		// consider null and unmatched as the same
		distinctValueRO = distinctValueRO
				&& !(isNullOrUnmatched(rightValue) && isNullOrUnmatched(originValue));

		if (distinctValueRO) {
			// Right and origin are distinct
			if (rightValue == null || rightValue == UNMATCHED_VALUE) {
				// right value is unset, or right has been removed
				getDiffProcessor().referenceChange(match, reference, configure, (EObject)originValue,
						DifferenceKind.CHANGE, DifferenceSource.RIGHT);
			} else {
				// Right has been set to a new value, or right has been added altogether
				getDiffProcessor().referenceChange(match, reference, configure, (EObject)rightValue,
						DifferenceKind.CHANGE, DifferenceSource.RIGHT);
			}
		}
	}

	/**
	 * Computes the difference between the sides of the given <code>match</code> for the given single-valued
	 * <code>reference</code>.
	 * <p>
	 * The given {@code reference} cannot be a containment reference.
	 * </p>
	 * <p>
	 * This is only meant for two-way comparisons.
	 * </p>
	 * 
	 * @param match
	 *            The match which sides we need to check for potential differences.
	 * @param reference
	 *            The reference which values are to be checked.
	 */
	protected void computeSingleValuedReferenceDifferencesTwoWay(Match match, EReference reference, FeatureConfigure configure) {
		final Comparison comparison = match.getComparison();

		final EObject left = match.getLeft();
		Object leftValue = UNMATCHED_VALUE;
		if (left != null) {
			leftValue = safeEGet(left, reference);
		}
		final EObject right = match.getRight();
		Object rightValue = UNMATCHED_VALUE;
		if (right != null) {
			rightValue = safeEGet(right, reference);
		}

		boolean distinctValue = !EqualityHelper.getEqualityHelper(comparison).matchingValues(leftValue, rightValue);
		// consider null and unmatched as the same
		distinctValue = distinctValue && !(isNullOrUnmatched(leftValue) && isNullOrUnmatched(rightValue));

		if (distinctValue) {
			if (leftValue == null || leftValue == UNMATCHED_VALUE) {
				// left value is unset, or left has been removed
				getDiffProcessor().referenceChange(match, reference, configure, (EObject)rightValue,
						DifferenceKind.CHANGE, DifferenceSource.LEFT);
			} else {
				// Left has been set to a new value, or left has been added altogether
				getDiffProcessor().referenceChange(match, reference, configure, (EObject)leftValue,
						DifferenceKind.CHANGE, DifferenceSource.LEFT);
			}
		}
	}

	/**
	 * Delegates to the diff processor to create the specified feature change.
	 * 
	 * @param match
	 *            The match on which values we detected a diff.
	 * @param feature
	 *            The exact feature on which a diff was detected.
	 * @param value
	 *            The value for which we detected a changed.
	 * @param kind
	 *            The kind of difference to create.
	 * @param source
	 *            The source from which originates that diff.
	 */
	protected void featureChange(Match match, EStructuralFeature feature, FeatureConfigure configure, Object value, DifferenceKind kind,
			DifferenceSource source) {
		if (FeatureMapUtil.isFeatureMap(feature)) {
			getDiffProcessor().featureMapChange(match, (EAttribute)feature, configure, value, kind, source);
		} else if (feature instanceof EAttribute) {
			getDiffProcessor().attributeChange(match, (EAttribute)feature, configure, value, kind, source);
		} else if (value instanceof EObject) {
			getDiffProcessor().referenceChange(match, (EReference)feature, configure, (EObject)value, kind, source);
		}
	}

	/**
	 * This will return the diff processor that has been created through {@link #createDiffProcessor()} for
	 * this differencing process.
	 * 
	 * @return The diff processor to notify of difference detections.
	 */
	protected final IDiffProcessor getDiffProcessor() {
		return diffProcessor;
	}
}
