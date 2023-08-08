package edu.ustb.sei.mde.fastcompare.index;

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

import com.google.common.collect.Maps;

import edu.ustb.sei.mde.fastcompare.config.ClassConfigure;
import edu.ustb.sei.mde.fastcompare.config.Hasher;
import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.match.CachingDistanceFunction;
import edu.ustb.sei.mde.fastcompare.match.CachingDistanceFunctionEx;
import edu.ustb.sei.mde.fastcompare.match.ScopeQuery;
import edu.ustb.sei.mde.fastcompare.utils.MatchUtil;

public class ProximityIndex implements ObjectIndex {
    final private ScopeQuery scope;
    final private MatcherConfigure matcherConfigure;
	final CachingDistanceFunction meter;
	final Hasher hasher;

    final ObjectFilterIndex left;
    final ObjectFilterIndex right;
    final ObjectFilterIndex origin;
	
    public ProximityIndex(ScopeQuery scope, MatcherConfigure matcherConfigure, Function<MatcherConfigure, ObjectFilterIndex> creator) {
		this.scope = scope;
		this.matcherConfigure = matcherConfigure;

		if(matcherConfigure.isUsingIdentityHash()) {
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
		switch(side) {
			case LEFT: return left.allCandidates();
			case RIGHT: return right.allCandidates();
			case ORIGIN: return origin.allCandidates();
			default: return Collections.emptyList();
		}
	}

	protected boolean readyForThisTest(Comparison inProgress, EObject fastCheck) {
		EObject eContainer = fastCheck.eContainer();
		if (eContainer != null && scope.isInScope(eContainer)) {
			Match match =  inProgress.getMatch(eContainer);
			return MatchUtil.isFullMatch(match);
		}
		return true;
	}

	@Override
	public Map<Side, EObject> findClosests(Comparison inProgress, EObject eObj, Side passedObjectSide) {
		// findClosests must be able to handle partial match
		// For partial match, there may exist a match for eObj in which not all the sides are determined.
		if (!readyForThisTest(inProgress, eObj)) {
			return null;
		}

		Match partialMatch = MatchUtil.getMatch(eObj, inProgress);

		Map<Side, EObject> result = new HashMap<Side, EObject>(3);
		result.put(passedObjectSide, eObj);
		if (passedObjectSide == Side.LEFT) {
			if(partialMatch == null) {
				EObject closestRight = findTheClosest(inProgress, eObj, Side.LEFT, Side.RIGHT, true);
				EObject closestOrigin = findTheClosest(inProgress, eObj, Side.LEFT, Side.ORIGIN, true);
				result.put(Side.RIGHT, closestRight);
				result.put(Side.ORIGIN, closestOrigin);
			} else {
				EObject closestRight;
				if(partialMatch.getRight() == MatchUtil.PSEUDO_MATCHED_OBJECT) 
					closestRight = findTheClosest(inProgress, eObj, Side.LEFT, Side.RIGHT, true);
				else closestRight = partialMatch.getRight();
				EObject closestOrigin;
				if(partialMatch.getOrigin() == MatchUtil.PSEUDO_MATCHED_OBJECT) 
					closestOrigin = findTheClosest(inProgress, eObj, Side.LEFT, Side.ORIGIN, true);
				else closestOrigin = partialMatch.getOrigin();
				result.put(Side.RIGHT, closestRight);
				result.put(Side.ORIGIN, closestOrigin);
			}
		} else if (passedObjectSide == Side.RIGHT) {
			if(partialMatch == null) {
				EObject closestLeft = findTheClosest(inProgress, eObj, Side.RIGHT, Side.LEFT, true);
				EObject closestOrigin = findTheClosest(inProgress, eObj, Side.RIGHT, Side.ORIGIN, true);
				result.put(Side.LEFT, closestLeft);
				result.put(Side.ORIGIN, closestOrigin);
			} else {
				EObject closestLeft;
				if(partialMatch.getLeft() == MatchUtil.PSEUDO_MATCHED_OBJECT)
					closestLeft = findTheClosest(inProgress, eObj, Side.RIGHT, Side.LEFT, true);
				else closestLeft = partialMatch.getLeft();
				EObject closestOrigin;
				if(partialMatch.getOrigin() == MatchUtil.PSEUDO_MATCHED_OBJECT)
					closestOrigin = findTheClosest(inProgress, eObj, Side.RIGHT, Side.ORIGIN, true);
				else closestOrigin = partialMatch.getOrigin();
				result.put(Side.LEFT, closestLeft);
				result.put(Side.ORIGIN, closestOrigin);
			}
		} else if (passedObjectSide == Side.ORIGIN) {
			if(partialMatch == null) {
				EObject closestLeft = findTheClosest(inProgress, eObj, Side.ORIGIN, Side.LEFT, true);
				EObject closestRight = findTheClosest(inProgress, eObj, Side.ORIGIN, Side.RIGHT, true);
				result.put(Side.LEFT, closestLeft);
				result.put(Side.RIGHT, closestRight);
			} else {
				EObject closestLeft;
				if(partialMatch.getLeft() == MatchUtil.PSEUDO_MATCHED_OBJECT)
					closestLeft = findTheClosest(inProgress, eObj, Side.ORIGIN, Side.LEFT, true);
				else closestLeft = partialMatch.getLeft();
				EObject closestRight;
				if(partialMatch.getRight() == MatchUtil.PSEUDO_MATCHED_OBJECT) 
					closestRight = findTheClosest(inProgress, eObj, Side.ORIGIN, Side.RIGHT, true);
				else closestRight = partialMatch.getRight();
				result.put(Side.LEFT, closestLeft);
				result.put(Side.RIGHT, closestRight);
			}
		}
		
		return result;
	}

	private EObject findTheClosest(Comparison inProgress, EObject eObj, Side eObjSide, Side sideToFind, boolean shouldDoubleCheck) {
		ObjectFilterIndex storageToSearchFor = left;
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
				break;
		}

		Iterable<EObject> cand = storageToSearchFor.filterCandidates(inProgress, eObj, null, 1.0);
		for (EObject fastCheck : cand) {
			if (!readyForThisTest(inProgress, fastCheck)) {
			} else {
				if (meter.areIdentic(inProgress, eObj, fastCheck)) {
					return fastCheck;
				}
			}
		}

		Match containerMatch = getPreviousMatch(eObj.eContainer(), inProgress);

		SortedMap<Double, EObject> candidates = Maps.newTreeMap();
		double minSim = getMinSim(eObj);
		boolean canCache = true;
		
		EObject matchedContainer = null;
		if(containerMatch!=null) {			
			switch (sideToFind) {
			case RIGHT:
				matchedContainer = containerMatch.getRight();
				break;
			case LEFT:
				matchedContainer = containerMatch.getLeft();
				break;
			case ORIGIN:
				matchedContainer = containerMatch.getOrigin();
				break;
			default:
				break; // never happen
			}
			if(matchedContainer == MatchUtil.PSEUDO_MATCHED_OBJECT) {
				throw new RuntimeException("This should not happen!");
			}
		} else {
			canCache = false;
		}

		Iterable<EObject> cand2 = storageToSearchFor.filterCandidates(inProgress, eObj, Optional.ofNullable(matchedContainer), minSim);

		double bestDistance = Double.MAX_VALUE;
		EObject bestObject = null;

		if(shouldDoubleCheck) {
			for (EObject potentialClosest : cand2) {
				double dist = meter.distance(inProgress, eObj, potentialClosest, matchedContainer == potentialClosest.eContainer(), true);
				if (dist < bestDistance) {
					candidates.compute(Double.valueOf(dist), (key, existingObject)->{
						if(existingObject == null) return potentialClosest;
						ElementIndexAdapter existingObjectAdapter = ElementIndexAdapter.getAdapter(existingObject);
						ElementIndexAdapter potentialObjectAdapter = ElementIndexAdapter.getAdapter(potentialClosest);
						// FIXME: should we use < or >
						if(existingObjectAdapter.position < potentialObjectAdapter.position) 
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
			for (EObject potentialClosest : cand2) {
				double dist;
				dist = meter.distance(inProgress, eObj, potentialClosest, matchedContainer == potentialClosest.eContainer(), canCache);
				
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

	public Match getPreviousMatch(final EObject eObj, Comparison inProgress) {
		if(eObj==null) return null;
		else return inProgress.getMatch(eObj);
	}

	@Override
	public void remove(EObject eObj, Side side) {
		switch(side) {
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
		switch(side) {
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

	@Override
	public void buildTreeIndex(EObject root, Side side) {
		switch(side) {
			case LEFT: {
				left.indexTree(root);
				break;
			}
			case RIGHT: {
				right.indexTree(root);
				break;
			}
			case ORIGIN: {
				origin.indexTree(root);
				break;
			}
		}
	}
}
