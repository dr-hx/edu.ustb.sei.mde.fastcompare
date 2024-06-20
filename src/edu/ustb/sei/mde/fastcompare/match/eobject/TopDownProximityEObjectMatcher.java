package edu.ustb.sei.mde.fastcompare.match.eobject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.index.ElementIndexAdapter;
import edu.ustb.sei.mde.fastcompare.index.ElementIndexAdapterWithStructuralChecksum;
import edu.ustb.sei.mde.fastcompare.index.ObjectIndex;
import edu.ustb.sei.mde.fastcompare.index.ObjectIndex.Side;
import edu.ustb.sei.mde.fastcompare.utils.MatchUtil;
import edu.ustb.sei.mde.fastcompare.utils.Triple;

public class TopDownProximityEObjectMatcher extends ProximityEObjectMatcher {

    public TopDownProximityEObjectMatcher(MatcherConfigure configure) {
        super(configure);
    }

    @Override
    protected void matchIndexedObjects(Comparison comparison,
            Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
        matchList(comparison, roots.left, true, roots);
        matchList(comparison, roots.right, true, roots);
    }

    @Override
    protected Iterable<EObject> matchList(Comparison comparison, Iterable<EObject> todoList, boolean createUnmatches,
            Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
        // todoList is the objects on the same level of the same root
        // for each object, we do subtree match first
        //   if subtree match fully succeeds, remove this node.
        //   otherwise, we append its children to the next round
        // finally, we return the next-round objects

        List<EObject> notFullyMatched = new ArrayList<>(32);
        List<EObject> nextLevel = new ArrayList<>(32);

        while(true) {
            nextLevel.clear();
            notFullyMatched.clear();
    
			// coarse match
            for(EObject eObj : todoList) {
                ElementIndexAdapter adapter = ElementIndexAdapter.getAdapter(eObj);
                if(adapter == null) continue;

                Match partialMatch = comparison.getMatch(eObj);
                if (!MatchUtil.isFullMatch(partialMatch)) {
                    partialMatch = trySubtreeMatch(comparison, eObj, partialMatch, createUnmatches, roots);
                    if(!MatchUtil.isFullMatch(partialMatch)) {
                        notFullyMatched.add(eObj);
                    }
                }
            }

			// structural iso match
			Iterator<EObject> notMatched = notFullyMatched.iterator();
			while(notMatched.hasNext()) {
				EObject eObj = notMatched.next();
                Match partialMatch = comparison.getMatch(eObj);
                partialMatch = trySubtreeStructuralMatch(comparison, eObj, partialMatch, createUnmatches, roots);
                if(MatchUtil.isFullMatch(partialMatch)) {
                    notMatched.remove();
                }
            }
    
			// fine match
            for(EObject eObj : notFullyMatched) {
                Match partialMatch = comparison.getMatch(eObj);
                tryFineMatch(comparison, eObj, partialMatch, createUnmatches);
                nextLevel.addAll(eObj.eContents());
            }
    
            if(nextLevel.isEmpty()) break;
            todoList = new ArrayList<>(nextLevel);
        }
        
        return Collections.emptyList();
    }

    private Match trySubtreeMatch(Comparison comparison, EObject a, Match partialMatchOfA, boolean createUnmatches, 
		Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
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
        // if(partialMatchOfA != null) {
        //     counter.hit(a.eClass());
        // }
		assert MatchUtil.isValidPartialMatch(partialMatchOfA);
		return partialMatchOfA;
    }

    @Override
    protected Match coarseGainedMatch(Comparison comparison, Match partialMatchOfA, EObject a, Side aSide, Side bSide, Side cSide, 
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
						total ++;
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

	private Match trySubtreeStructuralMatch(Comparison comparison, EObject a, Match partialMatchOfA, boolean createUnmatches, 
		Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
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
		partialMatchOfA = coarseGainedStructuralMatch(comparison, partialMatchOfA, a, aSide, bSide, cSide, roots);
        // if(partialMatchOfA != null) {
        //     counter.hit(a.eClass());
        // }
		assert MatchUtil.isValidPartialMatch(partialMatchOfA);
		return partialMatchOfA;
    }

	protected Match coarseGainedStructuralMatch(Comparison comparison, Match partialMatchOfA, EObject a, Side aSide, Side bSide, Side cSide, 
		Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
		// find subtrees in bSide and cSide
		// if bSubtree == null && cSubtree == null, then do nothing
		// if bSubtree != null && cSubtree == null, then Match.a = aSubtree, Match.b = bSubtree, Match.c = PSEUDO
		// if bSubtree == null && cSubtree != null, then Match.a = aSubtree, Match.b = PSEUDO, Match.c = cSubtree
		// if bSubtree != null && cSubtree != null, then Match.a = aSubtree, Match.b = bSubtree, Match.c = cSubtree, remove all from index
		if(configure.isUsingSubtreeHash()) {
			boolean unmatchedB = !MatchUtil.isMatched(partialMatchOfA, bSide);
			
			Match result = findUniqueStructureIdenticalSubtrees(comparison, a, aSide, partialMatchOfA, roots);

			if(partialMatchOfA == null) {
				if(result == null) {
					// no subtree match is found
					return null;
				} else {
					if(result.eContainer() == null) {
						// this is a new match
						total ++;
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

    private boolean readyForThisTest(Comparison inProgress, EObject fastCheck) {
		EObject eContainer = fastCheck.eContainer();
		if (eContainer != null && isInScope(eContainer)) {
			Match match = MatchUtil.getMatch(eContainer, inProgress);
			return match != null;
		}
		return true;
	}

    private Match findUniqueStructureIdenticalSubtrees(Comparison inProgress, EObject eObj, Side passedObjectSide,
			Match partialMatchOfEObj, Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
		// we cache the match of eObj.eContainer!
		// it is expected to be used in findTheIdenticalSubtree
		if (!readyForThisTest(inProgress, eObj)) {
			return partialMatchOfEObj;
		}

		Match resultMatch = null;

		if (passedObjectSide == Side.LEFT) {
            if(!MatchUtil.isMatched(partialMatchOfEObj, Side.RIGHT))
                resultMatch = findUniqueStructureIdenticalSubtree(inProgress, eObj, Side.LEFT, Side.RIGHT, partialMatchOfEObj, roots.right);
            else resultMatch = partialMatchOfEObj;
            if(!MatchUtil.isMatched(partialMatchOfEObj, Side.ORIGIN))
                resultMatch = findUniqueStructureIdenticalSubtree(inProgress, eObj, Side.LEFT, Side.ORIGIN, resultMatch, roots.origin);
		} else if (passedObjectSide == Side.RIGHT) {
            if(!MatchUtil.isMatched(partialMatchOfEObj, Side.LEFT))
                resultMatch = findUniqueStructureIdenticalSubtree(inProgress, eObj, Side.RIGHT, Side.LEFT, partialMatchOfEObj, roots.left);
            else resultMatch = partialMatchOfEObj;
            if(!MatchUtil.isMatched(partialMatchOfEObj, Side.ORIGIN))
                resultMatch = findUniqueStructureIdenticalSubtree(inProgress, eObj, Side.RIGHT, Side.ORIGIN, resultMatch, roots.origin);
		} else if (passedObjectSide == Side.ORIGIN) {
            if(!MatchUtil.isMatched(partialMatchOfEObj, Side.LEFT))
                resultMatch = findUniqueStructureIdenticalSubtree(inProgress, eObj, Side.ORIGIN, Side.LEFT, partialMatchOfEObj, roots.left);
            else resultMatch = partialMatchOfEObj;
            if(!MatchUtil.isMatched(partialMatchOfEObj, Side.RIGHT))
                resultMatch = findUniqueStructureIdenticalSubtree(inProgress, eObj, Side.ORIGIN, Side.RIGHT, resultMatch, roots.right);
		}

		return resultMatch;
	}

    private Match findUniqueStructureIdenticalSubtree(Comparison inProgress, EObject eObj, Side passedObjectSide, Side sideToFind, Match partialMatch,
			Iterable<EObject> candidates) {
		ElementIndexAdapterWithStructuralChecksum adapter = ElementIndexAdapter.getAdapter(eObj);
		if(adapter == null) return null;

        final EObject eContainer = eObj.eContainer();
		EObject matchedContainer = null;

		if(eContainer != null) {
			final Match containerMatch = MatchUtil.getMatch(eContainer, inProgress);
			matchedContainer = MatchUtil.getMatchedObject(containerMatch, sideToFind);
		}

		if (matchedContainer == null) {
			if (eContainer != null) {
				return partialMatch;
			}
		} else {
			candidates = matchedContainer.eContents();
		}

		final long subtreeKey = adapter.getTreeStructuralChecksum();
		
        final int size = adapter.size;
        final int height = adapter.height;
        if(size < 2 && height < 2) return partialMatch;

		EObject found = null;
		double bestSim = 0;

		for (EObject cand : candidates) {
			if(cand.eClass() != eObj.eClass()) continue;
			ElementIndexAdapterWithStructuralChecksum cAdapter = ElementIndexAdapter.getAdapter(cand);
			if(cAdapter != null) {
				if(MatchUtil.hasMatchFor(cand, inProgress, passedObjectSide)) continue;
				final long ctreeKey = cAdapter.getTreeStructuralChecksum();
				if (size == cAdapter.size && height == cAdapter.height && ctreeKey == subtreeKey) {
					final double sim = adapter.treeSimHash.similarity(cAdapter.treeSimHash);
					if(sim > 0.85) {
						if(found == null || (sim > 0.96 && bestSim < 0.93)) {
							found = cand;
							bestSim = sim;
						} else {
							return partialMatch;
						}
					}
				}
			}
		}
		
		if(found != null) {
			Match cMatch = inProgress.getMatch(found);
			// we probably have to consider the containment position in the future
			if (cMatch == null) {
				if(partialMatch == null) {
					partialMatch = MatchUtil.createMatch();
					MatchUtil.setMatch(partialMatch, eObj, passedObjectSide);
				}
				MatchUtil.setMatch(partialMatch, found, sideToFind);
				return partialMatch;
			} else if (MatchUtil.tryFillMatched(cMatch, eObj, passedObjectSide)) {
				return cMatch;
			}
		}

		return partialMatch;
	}
    // static public ProfileCounter counter = new ProfileCounter();
    
    private boolean tryFineMatch(Comparison comparison, EObject a, Match partialMatchOfA, boolean createUnmatches) {
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
		return fineGainedMatch(comparison, partialMatchOfA, a, aSide, bSide, cSide, createUnmatches);
    }
    
}
