package edu.ustb.sei.mde.fastcompare.match;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyIterator;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.CompareFactory;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.MatchResource;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.index.ElementIndexAdapterFactory;
import edu.ustb.sei.mde.fastcompare.match.eobject.IEObjectMatcher;
import edu.ustb.sei.mde.fastcompare.match.eobject.IdentifierEObjectMatcher;
import edu.ustb.sei.mde.fastcompare.match.eobject.ProximityEObjectMatcher;
import edu.ustb.sei.mde.fastcompare.match.resource.IResourceMatcher;
import edu.ustb.sei.mde.fastcompare.match.resource.IResourceMatchingStrategy;
import edu.ustb.sei.mde.fastcompare.match.resource.StrategyResourceMatcher;
import edu.ustb.sei.mde.fastcompare.scope.IComparisonScope;

public class DefaultMatchEngine implements IMatchEngine {

	/**
	 * Default max size of the EObject's URI loading cache.
	 */
	public static final int DEFAULT_EOBJECT_URI_CACHE_MAX_SIZE = 1024;

	/** The delegate {@link IEObjectMatcher matcher} that will actually pair EObjects together. */
	private final IEObjectMatcher eObjectMatcher;

	/** The strategy that will actually pair Resources together. */
	private final IResourceMatcher resourceMatcher;

	/** The factory that will be use to instantiate Comparison as return by match() methods. */
	private final IComparisonFactory comparisonFactory;

	private final MatcherConfigure matcherConfigure;

	/**
	 * This default engine delegates the pairing of EObjects to an {@link IEObjectMatcher}.
	 * 
	 * @param matcher
	 *            The matcher that will be in charge of pairing EObjects together for this comparison process.
	 * @param comparisonFactory
	 *            factory that will be use to instantiate Comparison as return by match() methods.
	 * @since 3.0
	 */
	public DefaultMatchEngine(MatcherConfigure matcherConfigure, IEObjectMatcher matcher, IComparisonFactory comparisonFactory) {
		this(matcherConfigure, matcher, new StrategyResourceMatcher(), comparisonFactory);
	}

	/**
	 * This default engine delegates the pairing of EObjects to an {@link IEObjectMatcher}.
	 * 
	 * @param eObjectMatcher
	 *            The matcher that will be in charge of pairing EObjects together for this comparison process.
	 * @param resourceMatcher
	 *            The matcher that will be in charge of pairing EObjects together for this comparison process.
	 * @param comparisonFactory
	 *            factory that will be use to instantiate Comparison as return by match() methods.
	 * @since 3.2
	 */
	public DefaultMatchEngine(MatcherConfigure matcherConfigure, IEObjectMatcher eObjectMatcher, IResourceMatcher resourceMatcher,
			IComparisonFactory comparisonFactory) {
		this.eObjectMatcher = checkNotNull(eObjectMatcher);
		this.resourceMatcher = checkNotNull(resourceMatcher);
		this.comparisonFactory = checkNotNull(comparisonFactory);
		this.matcherConfigure = matcherConfigure;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.match.IMatchEngine#match(org.eclipse.emf.compare.scope.IComparisonScope,
	 *      org.eclipse.emf.common.util.Monitor)
	 */
	public Comparison match(IComparisonScope scope) {
		Comparison comparison = comparisonFactory.createComparison();

		final Notifier left = scope.getLeft();
		final Notifier right = scope.getRight();
		final Notifier origin = scope.getOrigin();

		comparison.setThreeWay(origin != null);

		match(comparison, scope, left, right, origin);

		this.matcherConfigure.getIndexAdapterFactory().removeAdapters(left);
		this.matcherConfigure.getIndexAdapterFactory().removeAdapters(right);
		this.matcherConfigure.getIndexAdapterFactory().removeAdapters(origin);

		return comparison;
	}

	/**
	 * This methods will delegate to the proper "match(T, T, T)" implementation according to the types of
	 * {@code left}, {@code right} and {@code origin}.
	 * 
	 * @param comparison
	 *            The comparison to which will be added detected matches.
	 * @param scope
	 *            The comparison scope that should be used by this engine to determine the objects to match.
	 * @param left
	 *            The left {@link Notifier}.
	 * @param right
	 *            The right {@link Notifier}.
	 * @param origin
	 *            The common ancestor of <code>left</code> and <code>right</code>. Can be <code>null</code>.
	 * @param monitor
	 *            The monitor to report progress or to check for cancellation
	 */
	protected void match(Comparison comparison, IComparisonScope scope, final Notifier left,
			final Notifier right, final Notifier origin) {
		// FIXME side-effect coding
		if (left instanceof ResourceSet || right instanceof ResourceSet) {
			match(comparison, scope, (ResourceSet)left, (ResourceSet)right, (ResourceSet)origin);
		} else if (left instanceof Resource || right instanceof Resource) {
			match(comparison, scope, (Resource)left, (Resource)right, (Resource)origin);
		} else if (left instanceof EObject || right instanceof EObject) {
			match(comparison, scope, (EObject)left, (EObject)right, (EObject)origin);
		} else {
			// TODO Cannot happen ... for now. Should we log an exception?
		}
	}

	/**
	 * This will be used to match the given {@link ResourceSet}s. This default implementation will query the
	 * comparison scope for these resource sets children, then delegate to an {@link IResourceMatcher} to
	 * determine the resource mappings.
	 * 
	 * @param comparison
	 *            The comparison to which will be added detected matches.
	 * @param scope
	 *            The comparison scope that should be used by this engine to determine the objects to match.
	 * @param left
	 *            The left {@link ResourceSet}.
	 * @param right
	 *            The right {@link ResourceSet}.
	 * @param origin
	 *            The common ancestor of <code>left</code> and <code>right</code>. Can be <code>null</code>.
	 * @param monitor
	 *            The monitor to report progress or to check for cancellation
	 */
	protected void match(Comparison comparison, IComparisonScope scope, ResourceSet left, ResourceSet right,
			ResourceSet origin) {
		final Iterator<? extends Resource> leftChildren = scope.getCoveredResources(left);
		final Iterator<? extends Resource> rightChildren = scope.getCoveredResources(right);
		final Iterator<? extends Resource> originChildren;
		if (origin != null) {
			originChildren = scope.getCoveredResources(origin);
		} else {
			originChildren = emptyIterator();
		}

		// TODO Change API to pass the monitor to createMappings()
		final Iterable<MatchResource> mappings = this.resourceMatcher.createMappings(leftChildren,
				rightChildren, originChildren);

		final List<Iterator<? extends EObject>> leftIterators = Lists.newLinkedList();
		final List<Iterator<? extends EObject>> rightIterators = Lists.newLinkedList();
		final List<Iterator<? extends EObject>> originIterators = Lists.newLinkedList();

		for (MatchResource mapping : mappings) {
			// if (monitor.isCanceled()) {
			// 	throw new ComparisonCanceledException();
			// }
			comparison.getMatchedResources().add(mapping);

			final Resource leftRes = mapping.getLeft();
			final Resource rightRes = mapping.getRight();
			final Resource originRes = mapping.getOrigin();

			if (leftRes != null) {
				leftIterators.add(scope.getCoveredEObjects(leftRes));
			}

			if (rightRes != null) {
				rightIterators.add(scope.getCoveredEObjects(rightRes));
			}

			if (originRes != null) {
				originIterators.add(scope.getCoveredEObjects(originRes));
			}
		}

		final Iterator<? extends EObject> leftEObjects = Iterators.concat(leftIterators.iterator());
		final Iterator<? extends EObject> rightEObjects = Iterators.concat(rightIterators.iterator());
		final Iterator<? extends EObject> originEObjects = Iterators.concat(originIterators.iterator());

		getEObjectMatcher().createMatches(comparison, leftEObjects, rightEObjects, originEObjects);
	}

	/**
	 * This will only query the scope for the given Resources' children, then delegate to an
	 * {@link IEObjectMatcher} to determine the EObject matches.
	 * <p>
	 * We expect at least two of the given resources not to be <code>null</code>.
	 * </p>
	 * 
	 * @param comparison
	 *            The comparison to which will be added detected matches.
	 * @param scope
	 *            The comparison scope that should be used by this engine to determine the objects to match.
	 * @param left
	 *            The left {@link Resource}. Can be <code>null</code>.
	 * @param right
	 *            The right {@link Resource}. Can be <code>null</code>.
	 * @param origin
	 *            The common ancestor of <code>left</code> and <code>right</code>. Can be <code>null</code>.
	 * @param monitor
	 *            The monitor to report progress or to check for cancellation
	 */
	protected void match(Comparison comparison, IComparisonScope scope, Resource left, Resource right,
			Resource origin) {
		// monitor.subTask(EMFCompareMessages.getString("DefaultMatchEngine.monitor.match.resource")); //$NON-NLS-1$
		// Our "roots" are Resources. Consider them matched
		final MatchResource match = CompareFactory.eINSTANCE.createMatchResource();

		match.setLeft(left);
		match.setRight(right);
		match.setOrigin(origin);

		if (left != null) {
			URI uri = left.getURI();
			if (uri != null) {
				match.setLeftURI(uri.toString());
			}
		}

		if (right != null) {
			URI uri = right.getURI();
			if (uri != null) {
				match.setRightURI(uri.toString());
			}
		}

		if (origin != null) {
			URI uri = origin.getURI();
			if (uri != null) {
				match.setOriginURI(uri.toString());
			}
		}

		comparison.getMatchedResources().add(match);

		// We need at least two resources to match them
		if (atLeastTwo(left == null, right == null, origin == null)) {
			/*
			 * TODO But if we have only one resource, which is then unmatched, should we not still do
			 * something with it?
			 */
			return;
		}

		final Iterator<? extends EObject> leftEObjects;
		if (left != null) {
			leftEObjects = scope.getCoveredEObjects(left);
		} else {
			leftEObjects = emptyIterator();
		}
		final Iterator<? extends EObject> rightEObjects;
		if (right != null) {
			rightEObjects = scope.getCoveredEObjects(right);
		} else {
			rightEObjects = emptyIterator();
		}
		final Iterator<? extends EObject> originEObjects;
		if (origin != null) {
			originEObjects = scope.getCoveredEObjects(origin);
		} else {
			originEObjects = emptyIterator();
		}

		getEObjectMatcher().createMatches(comparison, leftEObjects, rightEObjects, originEObjects);

	}

	/**
	 * This will query the scope for the given {@link EObject}s' children, then delegate to an
	 * {@link IEObjectMatcher} to compute the Matches.
	 * <p>
	 * We expect at least the <code>left</code> and <code>right</code> EObjects not to be <code>null</code>.
	 * </p>
	 * 
	 * @param comparison
	 *            The comparison to which will be added detected matches.
	 * @param scope
	 *            The comparison scope that should be used by this engine to determine the objects to match.
	 * @param left
	 *            The left {@link EObject}.
	 * @param right
	 *            The right {@link EObject}.
	 * @param origin
	 *            The common ancestor of <code>left</code> and <code>right</code>.
	 * @param monitor
	 *            The monitor to report progress or to check for cancellation.
	 */
	protected void match(Comparison comparison, IComparisonScope scope, EObject left, EObject right,
			EObject origin) {
		// monitor.subTask(EMFCompareMessages.getString("DefaultMatchEngine.monitor.match.eobject")); //$NON-NLS-1$
		if (left == null || right == null) {
			throw new IllegalArgumentException();
		}

		final Iterator<? extends EObject> leftEObjects = Iterators.concat(Iterators.singletonIterator(left),
				scope.getChildren(left));
		final Iterator<? extends EObject> rightEObjects = Iterators.concat(Iterators.singletonIterator(right),
				scope.getChildren(right));
		final Iterator<? extends EObject> originEObjects;
		if (origin != null) {
			originEObjects = Iterators.concat(Iterators.singletonIterator(origin), scope.getChildren(origin));
		} else {
			originEObjects = emptyIterator();
		}

		getEObjectMatcher().createMatches(comparison, leftEObjects, rightEObjects, originEObjects);
	}

	/**
	 * Returns the Resource matcher associated with this match engine.
	 * 
	 * @return The Resource matcher associated with this match engine.
	 */
	protected final IResourceMatcher getResourceMatcher() {
		return this.resourceMatcher;
	}

	/**
	 * Returns the EObject matcher associated with this match engine.
	 * 
	 * @return The EObject matcher associated with this match engine.
	 */
	protected final IEObjectMatcher getEObjectMatcher() {
		return eObjectMatcher;
	}

	/**
	 * This will check that at least two of the three given booleans are <code>true</code>.
	 * 
	 * @param condition1
	 *            First of the three booleans.
	 * @param condition2
	 *            Second of the three booleans.
	 * @param condition3
	 *            Third of the three booleans.
	 * @return <code>true</code> if at least two of the three given booleans are <code>true</code>,
	 *         <code>false</code> otherwise.
	 */
	private static boolean atLeastTwo(boolean condition1, boolean condition2, boolean condition3) {
		// CHECKSTYLE:OFF This expression is alone in its method, and documented.
		return condition1 && (condition2 || condition3) || (condition2 && condition3);
		// CHECKSTYLE:ON
	}

	/**
	 * Helper creator method that instantiate a {@link DefaultMatchEngine} that will use identifiers as
	 * specified by the given {@code useIDs} enumeration.
	 * 
	 * @param useIDs
	 *            the kinds of matcher to use.
	 * @param weightProviderRegistry
	 *            the match engine needs a WeightProvider in case of this match engine do not use identifiers.
	 * @param equalityHelperExtensionProviderRegistry
	 *            the match engine may need a Equality Helper Extension
	 * @param strategies
	 *            the matching strategies you want to use for the match step.
	 * @return a new {@link DefaultMatchEngine} instance.
	 */
	public static IMatchEngine create(UseIdentifiers useIDs, MatcherConfigure configure,
			Collection<IResourceMatchingStrategy> strategies) {
		final IComparisonFactory comparisonFactory = new DefaultComparisonFactory(configure);
		final IEObjectMatcher eObjectMatcher = createDefaultEObjectMatcher(useIDs, configure);

		final IResourceMatcher resourceMatcher;
		if (strategies == null || strategies.isEmpty()) {
			resourceMatcher = new StrategyResourceMatcher();
		} else {
			resourceMatcher = new StrategyResourceMatcher(strategies);
		}

		final IMatchEngine matchEngine = new DefaultMatchEngine(configure, eObjectMatcher, resourceMatcher,
				comparisonFactory);
		return matchEngine;
	}

	/**
	 * Creates and configures an {@link IEObjectMatcher} with the strategy given by {@code useIDs}. The
	 * {@code cache} will be used to cache some expensive computation (should better a LoadingCache).
	 * 
	 * @param useIDs
	 *            which strategy the return IEObjectMatcher must follow.
	 * @param weightProviderRegistry
	 *            the match engine needs a WeightProvider in case of this match engine do not use identifiers.
	 * @param equalityHelperExtensionProviderRegistry
	 *            the match engine may need a Equality helper extension.
	 * @return a new IEObjectMatcher.
	 */
	public static IEObjectMatcher createDefaultEObjectMatcher(UseIdentifiers useIDs, MatcherConfigure configure) {
		final IEObjectMatcher matcher;

		switch (useIDs) {
			case NEVER:
				matcher = new ProximityEObjectMatcher(configure);
				break;
			case ONLY:
				matcher = new IdentifierEObjectMatcher();
				break;
			case WHEN_AVAILABLE:
				// fall through to default
			default:
				// Use an ID matcher, delegating to proximity when no ID is available
				final IEObjectMatcher contentMatcher = new ProximityEObjectMatcher(configure);
				matcher = new IdentifierEObjectMatcher(contentMatcher);
				break;

		}

		return matcher;
	}
}
