package edu.ustb.sei.mde.fastcompare.match.eobject;

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
import edu.ustb.sei.mde.fastcompare.utils.MatchUtil;
import edu.ustb.sei.mde.fastcompare.utils.Triple;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
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

	/**
	 * Legency code
	 * @param objects
	 * @param side
	 * @return
	 */
	private Set<EObject> buildIndex(Iterator<? extends EObject> objects, Side side) {
		ElementIndexAdapter.reset();
		Set<EObject> roots = new LinkedHashSet<>();
		// roots
		while(objects.hasNext()) {
			EObject next = objects.next();
			doHash(next);
			index.index(next, side);
			eObjectsToSide.put(next, side);
			if(next.eContainer() == null) roots.add(next);
		}

		// build coarse-grained index from roots
		if(configure.isUsingSubtreeHash()) {
			for(EObject root : roots) {
				buildSubtreeKey(root);
			}
		}

		return roots;
	}

	private int buildSubtreeKey(EObject root) {
		int maxChildDepth = 0;
		for(EObject child : root.eContents()) {
			ElementIndexAdapter cAdapter = ElementIndexAdapter.getAdapter(child);
			if(maxChildDepth < cAdapter.depth) maxChildDepth = cAdapter.depth;
		}
		ElementIndexAdapter adapter = ElementIndexAdapter.getAdapter(root);
		adapter.depth = maxChildDepth + 1;
		return adapter.depth;
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
		int maxDepth = 0;
		index.index(root, side);
		eObjectsToSide.put(root, side);
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
			adapter.similarityHash = hasher.zeroSHash();
		
		if(configure.isUsingIdentityHash() || configure.isUsingSubtreeHash())
			adapter.localIdentityHash = hasher.computeIHash(next);

		return adapter;
	}

	public void createMatches(Comparison comparison, Iterator<? extends EObject> leftEObjects,
		Iterator<? extends EObject> rightEObjects, Iterator<? extends EObject> originEObjects) {
		Set<EObject> leftRoots = buildIndex(leftEObjects, Side.LEFT);
		Set<EObject> rightRoots = buildIndex(rightEObjects, Side.RIGHT);
		Set<EObject> originRoots = buildIndex(originEObjects, Side.ORIGIN);
		
		Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots = Triple.make(leftRoots, rightRoots, originRoots);

		matchIndexedObjects(comparison, roots);
		createUnmatchesForRemainingObjects(comparison);
		restructureMatchModel(comparison);
	}

	/**
	 * {@inheritDoc}
	 */
	public void createMatches(Comparison comparison, Collection<EObject> leftRoots,
		Collection<EObject> rightRoots, Collection<EObject> originRoots) {
		buildIndex(leftRoots, Side.LEFT);
		buildIndex(rightRoots, Side.RIGHT);
		buildIndex(originRoots, Side.ORIGIN);

		Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots = Triple.make(leftRoots, rightRoots, originRoots);

		matchIndexedObjects(comparison, roots);
		createUnmatchesForRemainingObjects(comparison);
		restructureMatchModel(comparison);

	}

	/**
	 * Match elements for real, if no match is found for an element, an object will be created to represent
	 * this unmatch and the element will not be processed again.
	 * 
	 * @param comparison
	 *            the current comparison.
	 * @param monitor
	 *            monitor to track progress.
	 */
	protected void matchIndexedObjects(Comparison comparison, Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
		Iterable<EObject> todo = index.getValuesStillThere(Side.LEFT);
		while (todo.iterator().hasNext()) {
			todo = matchList(comparison, todo, true, roots);
		}
		todo = index.getValuesStillThere(Side.RIGHT);
		while (todo.iterator().hasNext()) {
			todo = matchList(comparison, todo, true, roots);
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
	protected Iterable<EObject> matchList(Comparison comparison, Iterable<EObject> todoList, boolean createUnmatches,
		Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
			// we may adopt a top-down algorithm here to match by subtree id from roots
			// the algorithm should be Breadth-first
			// for unmatched children, we adopt fine-gained algorithm

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
			Match partialMatch = comparison.getMatch(next);
			if (!MatchUtil.isFullMatch(partialMatch)) {
				if (!tryToMatch(comparison, next, partialMatch, createUnmatches, roots)) {
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
	 * @param partialMatchOfA
	 * 			  the partial match (cached) of a
	 * @param createUnmatches
	 *            whether elements which have no match should trigger the creation of a Match object (meaning
	 *            we won't try to match them afterwards) or not.
	 * @return false if the conditions are not fulfilled to create the match, true otherwhise.
	 */
	private boolean tryToMatch(Comparison comparison, EObject a, Match partialMatchOfA, boolean createUnmatches, 
		Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
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
		partialMatchOfA = coarseGainedMatch(comparison, partialMatchOfA, a, aSide, bSide, cSide, roots);
		assert MatchUtil.isValidPartialMatch(partialMatchOfA);
		if(MatchUtil.isFullMatch(partialMatchOfA)) 
			return true;

		// fine-gained matching
		okToMatch = fineGainedMatch(comparison, partialMatchOfA, a, aSide, bSide, cSide, createUnmatches);
		
		return okToMatch;
	}

	private Match coarseGainedMatch(Comparison comparison, Match partialMatchOfA, EObject a, Side aSide, Side bSide, Side cSide, 
		Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
		// find subtrees in bSide and cSide
		// if bSubtree == null && cSubtree == null, then do nothing
		// if bSubtree != null && cSubtree == null, then Match.a = aSubtree, Match.b = bSubtree, Match.c = PSEUDO
		// if bSubtree == null && cSubtree != null, then Match.a = aSubtree, Match.b = PSEUDO, Match.c = cSubtree
		// if bSubtree != null && cSubtree != null, then Match.a = aSubtree, Match.b = bSubtree, Match.c = cSubtree, remove all from index
		if(configure.isUsingSubtreeHash()) {
			// when partialMatchOfA != null, we should match objects with partial matches
			// otherwise, we can match objects with partial matches
			// if findIdenticalSubtrees returns a match
			//    if partialMatchOfA == null && result !=null
			//    if result == partialMatchOfA, fill from aSide
			//    if partialMatchOfA == null && result != partialMatchOfA, it is a new match or a partial match from other sides
			//      for a new match, createNewMatch
			//      otherwise, fill from the side of result (either bSide or cSide)
			boolean unmatchedB = !MatchUtil.isMatched(partialMatchOfA, bSide);
			
			Match result = index.findIdenticalSubtrees(comparison, a, aSide, partialMatchOfA, roots);
			
			if(partialMatchOfA == null) {
				if(result == null) {
					// no subtree match is found
					return null;
				} else {
					if(result.eContainer() == null) {
						// this is a new match
						createNewSubtreeMatches(comparison, result, roots);
					} else {
						// this is a match from other sides
						// fill a
						// fillSubtreeMatch(result, aSide)
						fillSubtreeMatches(comparison, result, aSide, roots);
					}
					return result;
				}
			} else {
				// in this case, result will not be null
				// we must check if the partial match is changed
				// if changed, fill b or c
				// fillSubtreeMatch(result, bSide) or fillSubtreeMatch(result, cSide)
				assert result != null;
				ObjectIndex.Side sideToFill = null;
				if(unmatchedB) {
					if(MatchUtil.isMatched(result, bSide)) sideToFill = bSide;
				} else {
					if(MatchUtil.isMatched(result, cSide)) sideToFill = cSide;
				}

				if(sideToFill != null) {
					assert MatchUtil.isFullMatch(result);
					fillSubtreeMatches(comparison, result, sideToFill, roots);
					return result;
				} else 
					return result;
			}
		} else return partialMatchOfA;
	}

	// at this point, we know that sideToFile is sub-tree-equal to one of the other two sides.
	// 	since this match is filled from a partial match and 
	// 	we create partial match only when the filled sides are sub-tree-equal
	private void fillSubtreeMatches(Comparison comparison, Match match, ObjectIndex.Side sideToFill,
		Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
		
		if(MatchUtil.isFullMatch(match)) {
			index.remove(match.getLeft(), ObjectIndex.Side.LEFT);
			index.remove(match.getRight(), ObjectIndex.Side.RIGHT);
			if(match.getOrigin() != null) {
				index.remove(match.getOrigin(), ObjectIndex.Side.ORIGIN);
			}
		}

		List<EObject> lefts = getChildren(match.getLeft());
		List<EObject> rights = getChildren(match.getRight());
		List<EObject> origins = getChildren(match.getOrigin());

		List<EObject> colA = null;
		List<EObject> colB = null;
		List<EObject> colToFill = null;
		ObjectIndex.Side sideA = null;
		ObjectIndex.Side sideB = null;

		switch(sideToFill) {
			case LEFT:
			colA = rights;
			colB = origins;
			colToFill = lefts;
			sideA = ObjectIndex.Side.RIGHT;
			sideB = ObjectIndex.Side.ORIGIN;
			break;
			case RIGHT:
			colA = lefts;
			colB = origins;
			colToFill = rights;
			sideA = ObjectIndex.Side.LEFT;
			sideB = ObjectIndex.Side.ORIGIN;
			break;
			case ORIGIN:
			colA = lefts;
			colB = rights;
			colToFill = origins;
			sideA = ObjectIndex.Side.LEFT;
			sideB = ObjectIndex.Side.RIGHT;
			break;
		}


		if(lefts.size() == rights.size() && lefts.size() == origins.size()) {
			int size = lefts.size();
			for(int i=0;i<size;i++) {
				EObject ac = colA.get(i);
				EObject bc = colB.get(i);
				EObject fc = colToFill.get(i);

				if(ac.eClass() == fc.eClass()) {
					Match childMatch = comparison.getMatch(ac);
					if(childMatch != null) {
						if(MatchUtil.tryFillMatched(childMatch, fc, sideToFill)) {
							fillSubtreeMatches(comparison, childMatch, sideToFill, roots);
							continue;
						} else {
							System.err.println(childMatch);
						}
						
						if(MatchUtil.getMatchedObject(childMatch, sideB) == bc) 
							continue;
					} else {
						System.err.println("null child match [A]!");
					}
				}
				
				if(bc.eClass() == fc.eClass()) {
					Match childMatch = comparison.getMatch(bc);
					if(childMatch != null) {
						if(MatchUtil.tryFillMatched(childMatch, fc, sideToFill)) {
							fillSubtreeMatches(comparison, childMatch, sideToFill, roots);
						} else {
							System.err.println(childMatch);
						}
					} else {
						System.err.println("null child match [B]!");
					}
				}
			}

		} else {
			// we stop here because we cannot ensure subtree equal
			// and this branch should rarely be triggered
			throw new RuntimeException("An unexpected branch is triggered. We hope the partial match should be sub-tree-equal. Please check if this case is reasonable.");
		}


	}

	private void createNewSubtreeMatches(Comparison comparison, Match match,
		Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {

		if(roots.origin.isEmpty() && match.getOrigin() == MatchUtil.PSEUDO_MATCHED_OBJECT) {
			match.setOrigin(null); // finalize this match
		}

		((BasicEList<Match>)comparison.getMatches()).addUnique(match);

		// if match is a full match, remove objects
		if(MatchUtil.isFullMatch(match)) {
			index.remove(match.getLeft(), ObjectIndex.Side.LEFT);
			index.remove(match.getRight(), ObjectIndex.Side.RIGHT);
			if(match.getOrigin() != null) {
				index.remove(match.getOrigin(), ObjectIndex.Side.ORIGIN);
			}
		}
		
		List<EObject> lefts = getChildren(match.getLeft());
		List<EObject> rights = getChildren(match.getRight());
		List<EObject> origins = getChildren(match.getOrigin());

		boolean lrEqualSize = lefts.size() == rights.size();
		boolean loEqualSize = lefts.size() == origins.size();

		if(lrEqualSize && loEqualSize) {
			int size = lefts.size();
			for(int i=0; i< size; i++) {
				EObject lc = lefts.get(i);
				EObject rc = rights.get(i);
				EObject oc = origins.get(i);

				boolean lrEqType = lc.eClass() == rc.eClass();
				boolean loEqType = lc.eClass() == oc.eClass();

				if(lrEqType && loEqType) {
					Match childMatch = CompareFactory.eINSTANCE.createMatch();
					childMatch.setLeft(lc);
					childMatch.setRight(rc);
					childMatch.setOrigin(oc);
					createNewSubtreeMatches(comparison, childMatch, roots);
				} else if(lrEqType) {
					Match childMatch = CompareFactory.eINSTANCE.createMatch();
					childMatch.setLeft(lc);
					childMatch.setRight(rc);
					childMatch.setOrigin(MatchUtil.PSEUDO_MATCHED_OBJECT);
					createNewSubtreeMatches(comparison, childMatch, roots);
				} else if(loEqType) {
					Match childMatch = CompareFactory.eINSTANCE.createMatch();
					childMatch.setLeft(lc);
					childMatch.setRight(MatchUtil.PSEUDO_MATCHED_OBJECT);
					childMatch.setOrigin(oc);
					createNewSubtreeMatches(comparison, childMatch, roots);
				} else if(rc.eClass() == oc.eClass()) {
					Match childMatch = CompareFactory.eINSTANCE.createMatch();
					childMatch.setLeft(MatchUtil.PSEUDO_MATCHED_OBJECT);
					childMatch.setRight(rc);
					childMatch.setOrigin(oc);
					createNewSubtreeMatches(comparison, childMatch, roots);
				}
			}
		} else if(lrEqualSize) {
			int size = lefts.size();
			for(int i=0; i< size; i++) {
				EObject lc = lefts.get(i);
				EObject rc = rights.get(i);
				if(lc.eClass() == rc.eClass()) {
					Match childMatch = CompareFactory.eINSTANCE.createMatch();
					childMatch.setLeft(lc);
					childMatch.setRight(rc);
					childMatch.setOrigin(MatchUtil.PSEUDO_MATCHED_OBJECT);
					createNewSubtreeMatches(comparison, childMatch, roots);
				}
			}
		} else if(loEqualSize) {
			int size = lefts.size();
			for(int i=0; i< size; i++) {
				EObject lc = lefts.get(i);
				EObject oc = origins.get(i);
				if(lc.eClass() == oc.eClass()) {
					Match childMatch = CompareFactory.eINSTANCE.createMatch();
					childMatch.setLeft(lc);
					childMatch.setRight(MatchUtil.PSEUDO_MATCHED_OBJECT);
					childMatch.setOrigin(oc);
					createNewSubtreeMatches(comparison, childMatch, roots);
				}
			}
		} else if(rights.size() == origins.size()) {
			int size = rights.size();
			for(int i=0; i< size; i++) {
				EObject rc = rights.get(i);
				EObject oc = origins.get(i);
				if(rc.eClass() == oc.eClass()) {
					Match childMatch = CompareFactory.eINSTANCE.createMatch();
					childMatch.setLeft(MatchUtil.PSEUDO_MATCHED_OBJECT);
					childMatch.setRight(rc);
					childMatch.setOrigin(oc);
					createNewSubtreeMatches(comparison, childMatch, roots);
				}
			}
		}

	}


	private List<EObject> getChildren(EObject obj) {
		if(obj == null) return Collections.emptyList();
		else return obj.eContents();
	}

	private boolean fineGainedMatch(Comparison comparison, Match partialMatch, EObject a, Side aSide, Side bSide, Side cSide, boolean createUnmatches) {
		boolean okToMatch = false;
		Map<Side, EObject> closests = index.findClosests(comparison, a, aSide, partialMatch);
		if (closests != null) {
			EObject bObj = closests.get(bSide);
			EObject cObj = closests.get(cSide);
			if (bObj != null || cObj != null) {
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
	 * If the partialMatch is null, this method will register the given object as a match and add it in the comparison.
	 * If the partialMatch is not null, this method will fill the blanks (i.e., PSEUDO_MATCHED_OBJECT) with the actual match (or null).
	 * Note that after this method is called, partialMatch becomes a full match.
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
