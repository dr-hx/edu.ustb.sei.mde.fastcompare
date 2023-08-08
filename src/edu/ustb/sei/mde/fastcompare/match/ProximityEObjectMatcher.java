package edu.ustb.sei.mde.fastcompare.match;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edu.ustb.sei.mde.fastcompare.config.Hasher;
import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.index.ByTypeIndex;
import edu.ustb.sei.mde.fastcompare.index.ElementIndexAdapter;
import edu.ustb.sei.mde.fastcompare.index.ObjectIndex;
import edu.ustb.sei.mde.fastcompare.index.ObjectIndex.Side;
import edu.ustb.sei.mde.fastcompare.shash.Hash64;
import edu.ustb.sei.mde.fastcompare.utils.MatchUtil;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.compare.CompareFactory;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.ecore.EObject;

/**
 * This matcher is using a distance function to match EObject. It guarantees that elements are matched with
 * the other EObject having the lowest distance. If two elements have the same distance regarding the other
 * EObject it will arbitrary pick one. (You should probably not rely on this and make sure your distance only
 * return 0 if both EObject have the very same content). The matcher will try to use the fact that it is a
 * distance to achieve a suitable scalability. It is also build on the following assumptions :
 * <ul>
 * <li>Most EObjects have no difference and have their corresponding EObject on the other sides of the model
 * (right and origins)</li>
 * <li>Two consecutive calls on the distance function with the same parameters will give the same distance.
 * </li>
 * </ul>
 * The scalability you'll get will highly depend on the complexity of the distance function. The
 * implementation is not caching any distance result from two EObjects.
 * 
 * @author <a href="mailto:cedric.brun@obeo.fr">Cedric Brun</a>
 */
public class ProximityEObjectMatcher implements IEObjectMatcher, ScopeQuery {
	/**
	 * Number of elements to index before a starting a match ahead step.
	 */
	private static final int NB_ELEMENTS_BETWEEN_MATCH_AHEAD = 10000;

	/**
	 * The index which keep the EObjects.
	 */
	private ObjectIndex index;

	/**
	 * Keeps track of which side was the EObject from.
	 */
	private Map<EObject, Side> eObjectsToSide = Maps.newHashMap();

	private MatcherConfigure configure;

	/**
	 * Create the matcher using the given distance function.
	 * 
	 * @param meter
	 *            a function to measure the distance between two {@link EObject}s.
	 */
	public ProximityEObjectMatcher(MatcherConfigure configure) {
		this.index = new ByTypeIndex(this, configure);
		this.configure = configure;
	}

	private Set<EObject> buildIndex(Iterator<? extends EObject> objects, Side side) {
		ElementIndexAdapter.reset();
		Set<EObject> roots = new HashSet<>();
		// roots
		while(objects.hasNext()) {
			EObject next = objects.next();
			doHash(next);
			index.index(next, side);
			eObjectsToSide.put(next, side);
			if(next.eContainer() == null) roots.add(next);
		}
		// build coarse-grained index from roots
		for(EObject root : roots) {
			index.buildTreeIndex(root, side);
		}

		return roots;
	}

	public void createMatches(Comparison comparison, Iterable<? extends EObject> leftRoots, Iterable<? extends EObject> rightRoots, Iterable<? extends EObject> originalRoots) {
		buildIndex(leftRoots, Side.LEFT);
		buildIndex(rightRoots, Side.RIGHT);
		buildIndex(originalRoots, Side.ORIGIN);

		// top-down match

		// unmatch others
	}


	/**
	 * Build index from roots
	 * @param roots
	 * @param side
	 */
	private void buildIndex(Iterable<? extends EObject> roots, Side side) {
		ElementIndexAdapter.reset();
		for(EObject root : roots) {
			buildIndex(root, side);
		}
	}

	private ElementIndexAdapter buildIndex(EObject root, Side side) {
		ElementIndexAdapter adapter = doHash(root);
		index.index(root, side);
		eObjectsToSide.put(root, side);
		int maxDepth = 0;
		for(EObject child : root.eContents()) {
			ElementIndexAdapter cAdapter = buildIndex(child, side);
			if(maxDepth < cAdapter.depth) maxDepth = cAdapter.depth;
		}
		adapter.depth = maxDepth + 1;
		return adapter;
	}

	private ElementIndexAdapter doHash(EObject next) {
		ElementIndexAdapter adapter = ElementIndexAdapter.equip(next);
		Hasher hasher = configure.getElementHasher();
		if(configure.shouldDoSimHash(next.eClass())) {
			adapter.similarityHash = hasher.computeSHash(next);
		} else 
			adapter.similarityHash = Hash64.ZERO_HASH;
		
		if(configure.isUsingIdentityHash())
			adapter.localIdentityHash = hasher.computeIHash(next);

		return adapter;
	}

	/**
	 * {@inheritDoc}
	 */

	public void createMatches(Comparison comparison, Iterator<? extends EObject> leftEObjects,
			Iterator<? extends EObject> rightEObjects, Iterator<? extends EObject> originEObjects) {
		if (!leftEObjects.hasNext() && !rightEObjects.hasNext() && !originEObjects.hasNext()) {
			return;
		}
		
		Set<EObject> leftRoots = buildIndex(leftEObjects, Side.LEFT);
		Set<EObject> rightRotos = buildIndex(rightEObjects, Side.RIGHT);
		Set<EObject> originalRoots = buildIndex(originEObjects, Side.ORIGIN);

		// int nbElements = 0;
		// int lastSegment = 0;
		/*
		 * We are iterating through the three sides of the scope at the same time so that index might apply
		 * pre-matching strategies elements if they wish.
		 */
		// while (leftEObjects.hasNext() || rightEObjects.hasNext() || originEObjects.hasNext()) {
		// 	if (leftEObjects.hasNext()) {
		// 		EObject next = leftEObjects.next();
		// 		index.index(next, Side.LEFT);
		// 		eObjectsToSide.put(next, Side.LEFT);
		// 	}

		// 	if (rightEObjects.hasNext()) {
		// 		EObject next = rightEObjects.next();
		// 		index.index(next, Side.RIGHT);
		// 		eObjectsToSide.put(next, Side.RIGHT);
		// 	}

		// 	if (originEObjects.hasNext()) {
		// 		EObject next = originEObjects.next();
		// 		index.index(next, Side.ORIGIN);
		// 		eObjectsToSide.put(next, Side.ORIGIN);
		// 	}
		// 	// if (nbElements / NB_ELEMENTS_BETWEEN_MATCH_AHEAD > lastSegment) {
		// 	// 	matchAheadOfTime(comparison);
		// 	// 	lastSegment++;
		// 	// }
		// }

		matchIndexedObjects(comparison);

		createUnmatchesForRemainingObjects(comparison);
		restructureMatchModel(comparison);

	}

	// /**
	//  * If the index supports it, match element ahead of time, in case of failure the elements are kept and
	//  * will be processed again later on.
	//  * 
	//  * @param comparison
	//  *            the current comparison.
	//  * @param monitor
	//  *            monitor to track progress.
	//  */
	// private void matchAheadOfTime(Comparison comparison) {
	// 	if (index instanceof MatchAheadOfTime) {
	// 		matchList(comparison, ((MatchAheadOfTime)index).getValuesToMatchAhead(Side.LEFT), false);
	// 		matchList(comparison, ((MatchAheadOfTime)index).getValuesToMatchAhead(Side.RIGHT), false);
	// 	}
	// }

	/**
	 * Match elements for real, if no match is found for an element, an object will be created to represent
	 * this unmatch and the element will not be processed again.
	 * 
	 * @param comparison
	 *            the current comparison.
	 * @param monitor
	 *            monitor to track progress.
	 */
	private void matchIndexedObjects(Comparison comparison) {
		Iterable<EObject> todo = index.getValuesStillThere(Side.LEFT);
		while (todo.iterator().hasNext()) {
			todo = matchList(comparison, todo, true);
		}
		todo = index.getValuesStillThere(Side.RIGHT);
		while (todo.iterator().hasNext()) {
			todo = matchList(comparison, todo, true);
		}

	}

	/**
	 * Create all the Match objects for the remaining EObjects.
	 * 
	 * @param comparison
	 *            the current comparison.
	 * @param monitor
	 *            a monitor to track progress.
	 */
	private void createUnmatchesForRemainingObjects(Comparison comparison) {
		// FIXME: we should check whether remained objects do not have partial matches
		for (EObject notFound : index.getValuesStillThere(Side.RIGHT)) {
			areMatching(comparison, null, notFound, null, null);
		}
		for (EObject notFound : index.getValuesStillThere(Side.LEFT)) {
			areMatching(comparison, notFound, null, null, null);
		}
		for (EObject notFound : index.getValuesStillThere(Side.ORIGIN)) {
			areMatching(comparison, null, null, notFound, null);
		}
	}

	/**
	 * Process the list of objects matching them. This method might not be able to process all the EObjects if
	 * - for instance, their container has not been matched already. Every object which could not be matched
	 * is returned in the list.
	 * 
	 * @param comparison
	 *            the comparison being built.
	 * @param todoList
	 *            the list of objects to process.
	 * @param createUnmatches
	 *            whether elements which have no match should trigger the creation of a Match object (meaning
	 *            we won't try to match them afterwards) or not.
	 * @param monitor
	 *            a monitor to track progress.
	 * @return the list of EObjects which could not be processed for some reason.
	 */
	private Iterable<EObject> matchList(Comparison comparison, Iterable<EObject> todoList, boolean createUnmatches) {
		Set<EObject> remainingResult = Sets.newLinkedHashSet();
		List<EObject> requiredContainers = Lists.newArrayList();

		Iterator<EObject> todo = todoList.iterator();
		while (todo.hasNext()) {
			EObject next = todo.next();
			/*
			 * Let's first add every container which is in scope
			 */
			EObject container = next.eContainer();
			while (container != null && isInScope(container)) {
				if (!MatchUtil.isFullyMatched(container, comparison)) {
					requiredContainers.add(0, container);
				}
				container = container.eContainer();
			}
		}

		Iterator<EObject> containersAndTodo = Iterators.concat(requiredContainers.iterator(), todoList.iterator());
		while (containersAndTodo.hasNext()) {
			EObject next = containersAndTodo.next();
			/*
			 * At this point you need to be sure the element has not been matched in any other way before.
			 */
			Match match = MatchUtil.getMatch(next, comparison);
			if (!MatchUtil.isFullMatch(match)) {
				if (!tryToMatch(comparison, next, match, createUnmatches)) {
					remainingResult.add(next);
				}
			}
		}
		return remainingResult;
	}

	/**
	 * Try to create a Match. If the match got created, register it (having actual left/right/origin matches
	 * or not), if not, then return false. Cases where it might not create the match : if some required data
	 * has not been computed yet (for instance if the container of an object has not been matched and if the
	 * distance need to know if it's match to find the children matches).
	 * 
	 * @param comparison
	 *            the comparison under construction, it will be updated with the new match.
	 * @param a
	 *            object to match.
	 * @param partialMatch
	 * @param createUnmatches
	 *            whether elements which have no match should trigger the creation of a Match object (meaning
	 *            we won't try to match them afterwards) or not.
	 * @return false if the conditions are not fulfilled to create the match, true otherwhise.
	 */
	private boolean tryToMatch(Comparison comparison, EObject a, Match partialMatch, boolean createUnmatches) {
		boolean okToMatch;
		Side aSide = eObjectsToSide.get(a);
		assert aSide != null;
		Side bSide = Side.LEFT;
		Side cSide = Side.RIGHT;
		if (aSide == Side.RIGHT) {
			bSide = Side.LEFT;
			cSide = Side.ORIGIN;
		} else if (aSide == Side.LEFT) {
			bSide = Side.RIGHT;
			cSide = Side.ORIGIN;
		} else if (aSide == Side.ORIGIN) {
			bSide = Side.LEFT;
			cSide = Side.RIGHT;
		}
		assert aSide != bSide;
		assert bSide != cSide;
		assert cSide != aSide;

		// sub-tree match
		coarseGainedMatch(comparison, partialMatch, a, aSide, bSide, cSide);

		// fine-gained matching
		okToMatch = fineGainedMatch(comparison, partialMatch, a, aSide, bSide, cSide, createUnmatches);
		
		return okToMatch;
	}

	private void coarseGainedMatch(Comparison comparison, Match partialMatch, EObject a, Side aSide, Side bSide, Side cSide) {
		// find subtrees in bSide and cSide
		// if bSubtree == null && cSubtree == null, then do nothing
		// if bSubtree != null && cSubtree == null, then Match.a = aSubtree, Match.b = bSubtree, Match.c = PSEUDO
		// if bSubtree == null && cSubtree != null, then Match.a = aSubtree, Match.b = PSEUDO, Match.c = cSubtree
		// if bSubtree != null && cSubtree != null, then Match.a = aSubtree, Match.b = bSubtree, Match.c = cSubtree, remove all from index
	}

	private boolean fineGainedMatch(Comparison comparison, Match partialMatch, EObject a, Side aSide, Side bSide, Side cSide, boolean createUnmatches) {
		boolean okToMatch = false;
		Map<Side, EObject> closests = index.findClosests(comparison, a, aSide);
		if (closests != null) {
			EObject lObj = closests.get(bSide);
			EObject aObj = closests.get(cSide);
			if (lObj != null || aObj != null) {
				// we have at least one other match
				areMatching(comparison, closests.get(Side.LEFT), closests.get(Side.RIGHT), closests.get(Side.ORIGIN), partialMatch);
				okToMatch = true;
			} else if (createUnmatches) {
				areMatching(comparison, closests.get(Side.LEFT), closests.get(Side.RIGHT), closests.get(Side.ORIGIN), partialMatch);
				okToMatch = true;
			}
		}
		return okToMatch;
	}

	/**
	 * Process all the matches of the given comparison and re-attach them to their parent if one is found.
	 * 
	 * @param comparison
	 *            the comparison to restructure.
	 * @param monitor
	 *            a monitor to track progress.
	 */
	private void restructureMatchModel(Comparison comparison) {
		Iterator<Match> it = ImmutableList.copyOf(Iterators.filter(comparison.eAllContents(), Match.class))
				.iterator();

		while (it.hasNext()) {
			Match cur = it.next();
			EObject possibleContainer = null;
			if (cur.getLeft() != null) {
				possibleContainer = cur.getLeft().eContainer();
			}
			if (possibleContainer == null && cur.getRight() != null) {
				possibleContainer = cur.getRight().eContainer();
			}
			if (possibleContainer == null && cur.getOrigin() != null) {
				possibleContainer = cur.getOrigin().eContainer();
			}
			Match possibleContainerMatch = comparison.getMatch(possibleContainer);
			if (possibleContainerMatch != null) {
				((BasicEList<Match>)possibleContainerMatch.getSubmatches()).addUnique(cur);
			}
		}
	}

	/**
	 * Register the given object as a match and add it in the comparison.
	 * 
	 * @param comparison
	 *            container for the Match.
	 * @param left
	 *            left element.
	 * @param right
	 *            right element
	 * @param origin
	 *            origin element.
	 * @return the created match.
	 */
	private Match areMatching(Comparison comparison, EObject left, EObject right, EObject origin, Match partialMatch) {
		Match result;
		if(partialMatch == null) {
			result = CompareFactory.eINSTANCE.createMatch();
			result.setLeft(left);
			result.setRight(right);
			result.setOrigin(origin);
			((BasicEList<Match>)comparison.getMatches()).addUnique(result);
		} else {
			result = partialMatch;
			if(result.getLeft() == MatchUtil.PSEUDO_MATCHED_OBJECT) result.setLeft(left);
			if(result.getRight() == MatchUtil.PSEUDO_MATCHED_OBJECT) result.setRight(right);
			if(result.getOrigin() == MatchUtil.PSEUDO_MATCHED_OBJECT) result.setOrigin(origin);
		}

		if (left != null) {
			index.remove(left, Side.LEFT);
		}
		if (right != null) {
			index.remove(right, Side.RIGHT);
		}
		if (origin != null) {
			index.remove(origin, Side.ORIGIN);
		}
		return result;
	}

	/**
	 * This represent a distance function used by the {@link ProximityEObjectMatcher} to compare EObjects and
	 * retrieve the closest EObject from one side to another. Axioms of the distance are supposed to be
	 * respected more especially :
	 * <ul>
	 * <li>symetry : dist(a,b) == dist(b,a)</li>
	 * <li>separation :dist(a,a) == 0</li>
	 * </ul>
	 * Triangular inequality is not leveraged with the current implementation but might be at some point to
	 * speed up the indexing. <br/>
	 * computing the distance between two EObjects should be a <b> fast operation</b> or the scalability of
	 * the whole matching phase will be poor.
	 * 
	 * @author cedric brun <cedric.brun@obeo.fr>
	 */
	public interface DistanceFunction {
		/**
		 * Return the distance between two EObjects. When the two objects should considered as completely
		 * different the implementation is expected to return Double.MAX_VALUE.
		 * 
		 * @param inProgress
		 *            the comparison being processed right now. This might be used for the distance to
		 *            retrieve other matches for instance.
		 * @param a
		 *            first object.
		 * @param b
		 *            second object.
		 * @return the distance between the two EObjects or Double.MAX_VALUE when the objects are considered
		 *         too different to be the same.
		 */
		double distance(Comparison inProgress, EObject a, EObject b);

		/**
		 * Check that two objects are equals from the distance function point of view (distance should be 0)
		 * You should prefer this method when you just want to check objects are not equals enabling the
		 * distance to stop sooner.
		 * 
		 * @param inProgress
		 *            the comparison being processed right now. This might be used for the distance to
		 *            retrieve other matches for instance.
		 * @param a
		 *            first object.
		 * @param b
		 *            second object.
		 * @return true of the two objects are equals, false otherwise.
		 */
		boolean areIdentic(Comparison inProgress, EObject a, EObject b);

	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isInScope(EObject eContainer) {
		return eObjectsToSide.get(eContainer) != null;
	}
}
