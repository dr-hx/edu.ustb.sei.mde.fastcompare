package edu.ustb.sei.mde.fastcompare.index;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.Map.Entry;
import java.util.function.Function;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.ecore.EObject;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import edu.ustb.sei.mde.fastcompare.config.ClassConfigure;
import edu.ustb.sei.mde.fastcompare.config.Hasher;
import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.match.eobject.CachingDistanceFunction;
import edu.ustb.sei.mde.fastcompare.match.eobject.CachingDistanceFunctionEx;
import edu.ustb.sei.mde.fastcompare.match.eobject.ScopeQuery;
import edu.ustb.sei.mde.fastcompare.utils.MatchUtil;
import edu.ustb.sei.mde.fastcompare.utils.Triple;

public class ProximityIndex implements ObjectIndex {
	final private ScopeQuery scope;
	final private MatcherConfigure matcherConfigure;
	final CachingDistanceFunction meter;
	final Hasher hasher;

	final ObjectFilterIndex left;
	final ObjectFilterIndex right;
	final ObjectFilterIndex origin;

	public ProximityIndex(ScopeQuery scope, MatcherConfigure matcherConfigure,
			Function<MatcherConfigure, ObjectFilterIndex> creator) {
		this.scope = scope;
		this.matcherConfigure = matcherConfigure;

		if (matcherConfigure.isUsingIdentityHash()) {
			this.meter = new CachingDistanceFunctionEx(matcherConfigure.getDistanceFunction());
		} else {
			this.meter = new CachingDistanceFunction(matcherConfigure.getDistanceFunction());
		}

		this.hasher = matcherConfigure.getElementHasher();

		this.left = creator.apply(matcherConfigure);
		this.right = creator.apply(matcherConfigure);
		this.origin = creator.apply(matcherConfigure);
	}

	@Override
	public Iterable<EObject> getValuesStillThere(Side side) {
		switch (side) {
			case LEFT:
				return left.allCandidates();
			case RIGHT:
				return right.allCandidates();
			case ORIGIN:
				return origin.allCandidates();
			default:
				return Collections.emptyList();
		}
	}

	/**
	 * In this method, we cache the match of fastCheck.eContainer.
	 * Becareful, we must avoid using it when we do not want to cache the result.
	 * 
	 * @param inProgress
	 * @param fastCheck
	 * @return
	 */
	protected boolean readyForThisTest(Comparison inProgress, EObject fastCheck) {
		EObject eContainer = fastCheck.eContainer();
		if (eContainer != null && scope.isInScope(eContainer)) {
			Match match = MatchUtil.getMatch(eContainer, inProgress);
			return match != null;
		}
		return true;
	}

	protected boolean readyForThisTestWithoutCache(Comparison inProgress, EObject fastCheck) {
		EObject eContainer = fastCheck.eContainer();
		if (eContainer != null && scope.isInScope(eContainer)) {
			Match match = inProgress.getMatch(eContainer);
			return match != null;
		}
		return true;
	}

	@Override
	public Match findIdenticalSubtrees(Comparison inProgress, EObject eObj, Side passedObjectSide,
			Match partialMatchOfEObj, Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
		// we cache the match of eObj.eContainer!
		// it is expected to be used in findTheIdenticalSubtree
		if (!readyForThisTest(inProgress, eObj)) {
			return partialMatchOfEObj;
		}

		Match resultMatch = null;

		if (passedObjectSide == Side.LEFT) {
			resultMatch = findTheIdenticalSubtree(inProgress, eObj, Side.LEFT, Side.RIGHT, partialMatchOfEObj, roots.right);
			resultMatch = findTheIdenticalSubtree(inProgress, eObj, Side.LEFT, Side.ORIGIN, resultMatch, roots.origin);
		} else if (passedObjectSide == Side.RIGHT) {
			resultMatch = findTheIdenticalSubtree(inProgress, eObj, Side.RIGHT, Side.LEFT, partialMatchOfEObj, roots.left);
			resultMatch = findTheIdenticalSubtree(inProgress, eObj, Side.RIGHT, Side.ORIGIN, resultMatch, roots.origin);
		} else if (passedObjectSide == Side.ORIGIN) {
			resultMatch = findTheIdenticalSubtree(inProgress, eObj, Side.ORIGIN, Side.LEFT, partialMatchOfEObj, roots.left);
			resultMatch = findTheIdenticalSubtree(inProgress, eObj, Side.ORIGIN, Side.RIGHT, resultMatch, roots.right);
		}

		if(resultMatch!=null) {
			makePseudoMatch(resultMatch);
		}

		return resultMatch;
	}

	private void makePseudoMatch(Match resultMatch) {
		if(resultMatch.getLeft() == null) resultMatch.setLeft(MatchUtil.PSEUDO_MATCHED_OBJECT);
		if(resultMatch.getRight() == null) resultMatch.setRight(MatchUtil.PSEUDO_MATCHED_OBJECT);
		if(resultMatch.getOrigin() == null) resultMatch.setOrigin(MatchUtil.PSEUDO_MATCHED_OBJECT);
	}

	private Match findTheIdenticalSubtree(final Comparison inProgress, final EObject eObj, final Side eObjSide,
			final Side sideToFind, final Match partialMatch, final Iterable<EObject> candRoots) {
		// skip if the partial match has the match of the sideToFind
		if(partialMatch != null && MatchUtil.isMatched(partialMatch, sideToFind)) 
			return partialMatch;

		final EObject eContainer = eObj.eContainer();
		EObject matchedContainer = null;

		if(eContainer != null) {
			// we expect to hit the cache of the container match
			final Match containerMatch = MatchUtil.getMatch(eContainer, inProgress);
			matchedContainer = MatchUtil.getMatchedObject(containerMatch, sideToFind);
		}
		
		Iterable<EObject> candidates = null;

		if (matchedContainer == null) {
			if (eContainer == null) {
				// try roots
				candidates = candRoots;
			} else {
				return partialMatch;
			}
		} else {
			candidates = matchedContainer.eContents();
		}

		// search in candidates
		return findIdenticalSubtree(inProgress, eObj, eObjSide, sideToFind, partialMatch, candidates);
	}

	private Match findIdenticalSubtree(Comparison inProgress, EObject eObj, Side passedObjectSide, Side sideToFind, Match partialMatch,
			Iterable<EObject> candidates) {
		ElementIndexAdapter adapter = ElementIndexAdapter.getAdapter(eObj);
		if(adapter == null) return null;

		final int height = adapter.height;
		final long subtreeKey = adapter.getSubtreeIdentityHash();

		if (partialMatch == null) {
			for (EObject cand : candidates) {
				ElementIndexAdapter cAdapter = ElementIndexAdapter.getAdapter(cand);
				if(cAdapter != null) {
					if (height == cAdapter.height && cAdapter.getSubtreeIdentityHash() == subtreeKey) {
						Match cMatch = inProgress.getMatch(cand);
						// we probably have to consider the containment position in the future
						if (cMatch == null) {
							partialMatch = MatchUtil.createMatch();
							MatchUtil.setMatch(partialMatch, eObj, passedObjectSide);
							MatchUtil.setMatch(partialMatch, cand, sideToFind);
							return partialMatch;
						} else if (MatchUtil.tryFillMatched(cMatch, eObj, passedObjectSide)) {
							return cMatch;
						}
					}
				}
			}
		} else {
			for (EObject cand : candidates) {
				ElementIndexAdapter cAdapter = ElementIndexAdapter.getAdapter(cand);
				if(cAdapter != null) {
					if (height == cAdapter.height && cAdapter.getSubtreeIdentityHash() == subtreeKey) {
						Match cMatch = inProgress.getMatch(cand);
						if (cMatch == null) {
							// if cand is not considered as matched, we can fill it to partial match
							// we probably have to consider the containment position in the future
							MatchUtil.setMatch(partialMatch, cand, sideToFind);
							return partialMatch;
						}
					}
				}
			}
		}

		return partialMatch;
	}

	private ObjectFilterIndex getStorageToSearchFor(final Side sideToFind) {
		final ObjectFilterIndex storageToSearchFor;
		switch (sideToFind) {
			case RIGHT:
				storageToSearchFor = right;
				break;
			case LEFT:
				storageToSearchFor = left;
				break;
			case ORIGIN:
				storageToSearchFor = origin;
				break;
			default:
				storageToSearchFor = left;
				break;
		}
		return storageToSearchFor;
	}

	@Override
	public Map<Side, EObject> findClosests(Comparison inProgress, EObject eObj, Side passedObjectSide,
			Match partialMatchOfEObj) {
		// findClosests must be able to handle partial match
		// For partial match, there may exist a match for eObj in which not all the
		// sides are determined.
		// we cache the match of eObj.eContainer!
		if (!readyForThisTest(inProgress, eObj)) {
			return null;
		}

		// At this point, eObj may have a partial match. We must complete the matching
		// process.

		Map<Side, EObject> result = new HashMap<Side, EObject>(3);
		result.put(passedObjectSide, eObj);

		// compete partial match
		if (passedObjectSide == Side.LEFT) {
			if (partialMatchOfEObj == null) {
				EObject closestRight = findTheClosest(inProgress, eObj, Side.LEFT, Side.RIGHT, true);
				EObject closestOrigin = findTheClosest(inProgress, eObj, Side.LEFT, Side.ORIGIN, true);
				result.put(Side.RIGHT, closestRight);
				result.put(Side.ORIGIN, closestOrigin);
			} else {
				EObject closestRight;
				if (partialMatchOfEObj.getRight() == MatchUtil.PSEUDO_MATCHED_OBJECT)
					closestRight = findTheClosest(inProgress, eObj, Side.LEFT, Side.RIGHT, true);
				else
					closestRight = partialMatchOfEObj.getRight();
				EObject closestOrigin;
				if (partialMatchOfEObj.getOrigin() == MatchUtil.PSEUDO_MATCHED_OBJECT)
					closestOrigin = findTheClosest(inProgress, eObj, Side.LEFT, Side.ORIGIN, true);
				else
					closestOrigin = partialMatchOfEObj.getOrigin();
				result.put(Side.RIGHT, closestRight);
				result.put(Side.ORIGIN, closestOrigin);
			}
		} else if (passedObjectSide == Side.RIGHT) {
			if (partialMatchOfEObj == null) {
				EObject closestLeft = findTheClosest(inProgress, eObj, Side.RIGHT, Side.LEFT, true);
				EObject closestOrigin = findTheClosest(inProgress, eObj, Side.RIGHT, Side.ORIGIN, true);
				result.put(Side.LEFT, closestLeft);
				result.put(Side.ORIGIN, closestOrigin);
			} else {
				EObject closestLeft;
				if (partialMatchOfEObj.getLeft() == MatchUtil.PSEUDO_MATCHED_OBJECT)
					closestLeft = findTheClosest(inProgress, eObj, Side.RIGHT, Side.LEFT, true);
				else
					closestLeft = partialMatchOfEObj.getLeft();
				EObject closestOrigin;
				if (partialMatchOfEObj.getOrigin() == MatchUtil.PSEUDO_MATCHED_OBJECT)
					closestOrigin = findTheClosest(inProgress, eObj, Side.RIGHT, Side.ORIGIN, true);
				else
					closestOrigin = partialMatchOfEObj.getOrigin();
				result.put(Side.LEFT, closestLeft);
				result.put(Side.ORIGIN, closestOrigin);
			}
		} else if (passedObjectSide == Side.ORIGIN) {
			if (partialMatchOfEObj == null) {
				EObject closestLeft = findTheClosest(inProgress, eObj, Side.ORIGIN, Side.LEFT, true);
				EObject closestRight = findTheClosest(inProgress, eObj, Side.ORIGIN, Side.RIGHT, true);
				result.put(Side.LEFT, closestLeft);
				result.put(Side.RIGHT, closestRight);
			} else {
				EObject closestLeft;
				if (partialMatchOfEObj.getLeft() == MatchUtil.PSEUDO_MATCHED_OBJECT)
					closestLeft = findTheClosest(inProgress, eObj, Side.ORIGIN, Side.LEFT, true);
				else
					closestLeft = partialMatchOfEObj.getLeft();
				EObject closestRight;
				if (partialMatchOfEObj.getRight() == MatchUtil.PSEUDO_MATCHED_OBJECT)
					closestRight = findTheClosest(inProgress, eObj, Side.ORIGIN, Side.RIGHT, true);
				else
					closestRight = partialMatchOfEObj.getRight();
				result.put(Side.LEFT, closestLeft);
				result.put(Side.RIGHT, closestRight);
			}
		}
		// At this point, result should not contain PSEUDO MATCH
		return result;
	}

	private EObject findTheClosest(Comparison inProgress, final EObject eObj, final Side eObjSide,
			final Side sideToFind, final boolean shouldDoubleCheck) {
		final ObjectFilterIndex storageToSearchFor = getStorageToSearchFor(sideToFind);
		
		Predicate<EObject> filter = (o) -> {
			return !MatchUtil.hasMatchFor(o, inProgress, eObjSide);
			// Match m = inProgress.getMatch(o);
			// if(m != null) {
			// 	return !MatchUtil.isMatched(m, eObjSide); // since we want to find the match of eObjSide, m.eObjSide must be empty
			// }
			// return true;
		};
		
		Iterable<EObject> idenCands = storageToSearchFor.filterCandidates(inProgress, eObj, null, 1.0);
		idenCands = Iterables.filter(idenCands, filter);

		for (EObject fastCheck : idenCands) {
			if (!readyForThisTestWithoutCache(inProgress, fastCheck)) {
			} else {
				if (meter.areIdentic(inProgress, eObj, fastCheck)) {
					return fastCheck;
				}
			}
		}

		// we expect to hit the cache of the container match
		// but it will not happen during double check, so we decide to the use a plain
		// get here
		Match containerMatch = getContainerMatch(eObj, inProgress);

		SortedMap<Double, EObject> candidates = Maps.newTreeMap();
		double minSim = getMinSim(eObj);
		boolean canCache = true;

		EObject matchedContainer = null;
		if (containerMatch != null) {
			matchedContainer = MatchUtil.getMatchedObject(containerMatch, sideToFind);
			if (matchedContainer == MatchUtil.PSEUDO_MATCHED_OBJECT) {
				throw new RuntimeException("This should not happen!");
			}
		} else {
			canCache = false;
		}

		Iterable<EObject> simCands = storageToSearchFor.filterCandidates(inProgress, eObj,
				Optional.ofNullable(matchedContainer), minSim);
		simCands = Iterables.filter(simCands, filter);

		double bestDistance = Double.MAX_VALUE;
		EObject bestObject = null;

		if (shouldDoubleCheck) {
			for (EObject potentialClosest : simCands) {
				double dist = meter.distance(inProgress, eObj, potentialClosest,
						matchedContainer == potentialClosest.eContainer(), true);
				if (dist < bestDistance) {
					candidates.compute(Double.valueOf(dist), (key, existingObject) -> {
						if (existingObject == null)
							return potentialClosest;
						ElementIndexAdapter existingObjectAdapter = ElementIndexAdapter.getAdapter(existingObject);
						ElementIndexAdapter potentialObjectAdapter = ElementIndexAdapter.getAdapter(potentialClosest);
						// FIXME: should we use < or >
						if (existingObjectAdapter.position < potentialObjectAdapter.position)
							return existingObject;
						else
							return potentialClosest;
					});
				}
				// FIXME: the following code should not be executed if we want to be consistent
				// with EMF Compare
				// However, the following code may actually improve the result of the match
				// else if(dist<Double.MAX_VALUE && dist != bestDistance && candidates.size() <
				// 3) {
				// candidates.put(Double.valueOf(dist), potentialClosest);
				// }
			}
			// double check
			for (Entry<Double, EObject> entry : candidates.entrySet()) {
				EObject doubleCheck = this.findTheClosest(inProgress, entry.getValue(), sideToFind, eObjSide, false);
				if (doubleCheck == eObj) {
					return entry.getValue();
				}
			}
		} else {
			for (EObject potentialClosest : simCands) {
				double dist;
				dist = meter.distance(inProgress, eObj, potentialClosest,
						matchedContainer == potentialClosest.eContainer(), canCache);

				if (dist < bestDistance) {
					bestDistance = dist;
					bestObject = potentialClosest;
				}
			}
		}

		return bestObject;
	}

	private double getMinSim(EObject eObj) {
		ClassConfigure configure = matcherConfigure.getClassConfigure(eObj.eClass());
		return configure.getSimThreshold();
	}

	/**
	 * We do not cache the container match because this method will also be called
	 * during double check
	 * in which the cache won't help
	 * 
	 * @param eObj
	 * @param inProgress
	 * @return
	 */
	protected Match getContainerMatch(final EObject eObj, Comparison inProgress) {
		EObject container = eObj.eContainer();
		if (container == null)
			return null;
		else
			return inProgress.getMatch(container);
	}

	@Override
	public void remove(EObject eObj, Side side) {
		switch (side) {
			case LEFT: {
				left.remove(eObj);
				break;
			}
			case RIGHT: {
				right.remove(eObj);
				break;
			}
			case ORIGIN: {
				origin.remove(eObj);
				break;
			}
		}
	}

	@Override
	public void index(EObject eObj, Side side) {
		switch (side) {
			case LEFT: {
				left.index(eObj);
				break;
			}
			case RIGHT: {
				right.index(eObj);
				break;
			}
			case ORIGIN: {
				origin.index(eObj);
				break;
			}
		}
	}

	// @Override
	// public void indexingSubtrees(EObject root, Side side) {
	// switch(side) {
	// case LEFT: {
	// left.indexingSubtrees(root);
	// break;
	// }
	// case RIGHT: {
	// right.indexingSubtrees(root);
	// break;
	// }
	// case ORIGIN: {
	// origin.indexingSubtrees(root);
	// break;
	// }
	// }
	// }
}
