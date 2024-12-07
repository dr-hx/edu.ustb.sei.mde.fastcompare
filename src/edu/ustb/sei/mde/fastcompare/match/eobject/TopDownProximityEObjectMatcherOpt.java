package edu.ustb.sei.mde.fastcompare.match.eobject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.ecore.EObject;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.index.ElementIndexAdapter;
import edu.ustb.sei.mde.fastcompare.index.ElementIndexAdapterEx;
import edu.ustb.sei.mde.fastcompare.index.ObjectIndex.Side;
import edu.ustb.sei.mde.fastcompare.index.TreeHashValueEx;
import edu.ustb.sei.mde.fastcompare.utils.MatchUtil;
import edu.ustb.sei.mde.fastcompare.utils.Triple;

public class TopDownProximityEObjectMatcherOpt extends TopDownProximityEObjectMatcher {

    public TopDownProximityEObjectMatcherOpt(MatcherConfigure configure) {
        super(configure);
    }
    
    @Override
    protected void makeSubtreeStructuralMatches(Comparison comparison, boolean createUnmatches,
            Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots,
            List<EObject> notFullyMatched) {
        
        Map<EObject, ChangeTrackingMatch> results = trySubtreeStructuralMatches(comparison, notFullyMatched, createUnmatches, roots);
		
		Iterator<EObject> notMatched = notFullyMatched.iterator();
		while(notMatched.hasNext()) {
			EObject eObj = notMatched.next();
			ChangeTrackingMatch partialMatchPair = results.get(eObj);
			if(partialMatchPair != null && MatchUtil.isFullMatch(partialMatchPair.match)) {
				notMatched.remove();
			}
		}
    }

    private Map<EObject, ChangeTrackingMatch> trySubtreeStructuralMatches(Comparison comparison, List<EObject> from, boolean createUnmatches,
			Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
		if (from.isEmpty())
			return Collections.emptyMap();
		else {
			Side aSide = eObjectsToSide.get(from.get(0));
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

			Map<EObject, ChangeTrackingMatch> currentMatches = new HashMap<>();
			from.forEach(e->{
				Match m = comparison.getMatch(e);
				if(m != null) currentMatches.put(e, new ChangeTrackingMatch(m));
			});

			coarseGainedStructuralMatches(comparison, currentMatches, from, aSide, bSide, cSide, roots);

			return currentMatches;
		}
	}

    class ChangeTrackingMatch {
		public Match match = null;
		public Side changedSide = null;

		public ChangeTrackingMatch(Match m) {
			this.match = m;
			this.changedSide = null;
		}

		public ChangeTrackingMatch(Match m, Side changedSide) {
			this.match = m;
			this.changedSide = changedSide;
		}
	}

	class CandidateMatchGroup {
		private static final double TREE_SIM_THREASHOLD = 0.65;
		List<CandidateMatch> bCandidates = new ArrayList<>();
		List<CandidateMatch> cCandidates = new ArrayList<>();

		private void add(EObject from, int iFrom, EObject cand, int iCand, List<CandidateMatch> candidates) {
			double sim = computePartialSimilarity(from, cand);
			if (sim == 0.0)
				return;
			if (sim > 0.96)
				sim = 1.0;

			CandidateMatch cm = new CandidateMatch();
			cm.from = from;
			cm.cand = cand;
			cm.treeSim = sim;
			cm.posDiff = Math.abs(iFrom - iCand);

			candidates.add(cm);
		}

		public void addCandidateB(EObject a, int ia, EObject b, int ib) {
			add(a, ia, b, ib, bCandidates);
		}

		public void addCandidateC(EObject a, int ia, EObject c, int ic) {
			add(a, ia, c, ic, cCandidates);
		}
		
		protected double computePartialSimilarity(EObject from, EObject cand) {
			ElementIndexAdapterEx adapter = (ElementIndexAdapterEx) ElementIndexAdapter.getAdapter(from);
			if (adapter == null) return 0.0;
			if (cand.eClass() != from.eClass()) return 0.0;

			final TreeHashValueEx treeHash = (TreeHashValueEx) adapter.getTreeHash();

			final int size = treeHash.size;
			final int height = treeHash.height;

			if (size < 2 && height < 2) return 0.0;

			ElementIndexAdapterEx cAdapter = (ElementIndexAdapterEx) ElementIndexAdapter.getAdapter(cand);
			
			if (cAdapter != null) {
				final TreeHashValueEx childHash = (TreeHashValueEx) cAdapter.getTreeHash();
				if (size == childHash.size && height == childHash.height) {
					final double sim = treeHash.computeSubtreeSimilarity(childHash);
					if (sim > TREE_SIM_THREASHOLD) {
						return sim;
					}
				}
			} 
			
			return 0.0;
		}
	}

	class CandidateMatch implements Comparable<CandidateMatch> {
		EObject from;
		EObject cand;
		double treeSim;
		int posDiff;
		
		
		@Override
		public int compareTo(CandidateMatch right) {
			if(treeSim == right.treeSim) 
				return posDiff - right.posDiff;
			else {
				if(treeSim > right.treeSim) return -1;
				else return 1;
			}
		}
	}

	private void coarseGainedStructuralMatches(Comparison comparison, Map<EObject, ChangeTrackingMatch> currentMatches, 
		List<EObject> from, Side aSide, Side bSide, Side cSide, 
		Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
		
		findUniqueStructureIdenticalSubtrees(comparison, from, aSide, bSide, cSide, currentMatches, roots);
		
		for(int i=0; i<from.size(); i++) {
			EObject a = from.get(i);
			ChangeTrackingMatch result = currentMatches.get(a);

			if(result != null) {
				if(result.match.eContainer() == null) {
					createNewSubtreeMatches(comparison, result.match, roots);
				} else {
                    if(result.changedSide != null)
                        fillSubtreeMatches(comparison, result.match, result.changedSide, roots);
				}
			}
		}
	}

	private List<EObject> getCandidatesWithFilter(Comparison comparison, EObject parent, Side aSide, Side sideToFind, Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
		
		Collection<EObject> unfiltered = getCandidates(comparison, parent, sideToFind, roots);
		List<EObject> filtered = new ArrayList<>(unfiltered.size());
		
		for(EObject cand : unfiltered) {
			if(!MatchUtil.hasMatchFor(cand, comparison, aSide)) {
				filtered.add(cand);
			}
		}

		return filtered;
	}

	private Collection<EObject> getCandidates(Comparison comparison, EObject parent, Side sideToFind, Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
		if(parent != null) {
			final Match containerMatch = MatchUtil.getMatch(parent, comparison);
			final EObject matchedContainer = MatchUtil.getMatchedObject(containerMatch, sideToFind);
	
			if (matchedContainer == null) {
				return Collections.emptyList();
			} else {
				return matchedContainer.eContents();
			}
		} else {
			switch(sideToFind) {
				case LEFT: return roots.left;
				case RIGHT: return roots.right;
				case ORIGIN: return roots.origin;
				default:
					return Collections.emptyList();
			}
		}
	}

	private void findUniqueStructureIdenticalSubtrees(Comparison comparison, List<EObject> from, 
		Side passedObjectSide, Side bSide, Side cSide,
			Map<EObject, ChangeTrackingMatch> currentMatches,
			Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {

		EObject parent = null;

		// make candiate matches
		List<CandidateMatchGroup> groups = new ArrayList<>();

		CandidateMatchGroup group = null;
		List<EObject> bCandidates = null;
		List<EObject> cCandidates = null;

		int size = from.size();
		for (int i = 0; i < size; i++) {
			final EObject cur = from.get(i);
			final EObject curPar = cur.eContainer();

			final ChangeTrackingMatch matchOfCurPair = currentMatches.get(cur);
			final Match matchOfCur = matchOfCurPair == null ? null : matchOfCurPair.match;

			if (readyForThisTest(comparison, cur) == false)
				continue;

			if (parent != curPar || (curPar == null && parent == null)) {
				parent = curPar;

				bCandidates = getCandidatesWithFilter(comparison, parent, passedObjectSide, bSide, roots);
				cCandidates = getCandidatesWithFilter(comparison, parent, passedObjectSide, cSide, roots);

				if (bCandidates.isEmpty() && cCandidates.isEmpty()) {
					group = null;
				} else {
					// add group
					group = new CandidateMatchGroup();
					groups.add(group);
				}
			}

			if (group == null)
				continue;

			if (!MatchUtil.isMatched(matchOfCur, bSide)) {
				final int bCandSize = bCandidates.size();
				for (int j = 0; j < bCandSize; j++) {
					EObject b = bCandidates.get(j);
					group.addCandidateB(cur, i, b, j);
				}
			}

			if (!MatchUtil.isMatched(matchOfCur, cSide)) {
				final int cCandSize = cCandidates.size();
				for (int j = 0; j < cCandSize; j++) {
					EObject c = cCandidates.get(j);
					group.addCandidateC(cur, i, c, j);
				}
			}
		}

		// pick matches
		groups.forEach(g -> {
			pickMatches(comparison, passedObjectSide, bSide, cSide, currentMatches, g.bCandidates);
			pickMatches(comparison, passedObjectSide, cSide, bSide, currentMatches, g.cCandidates);
		});
	}

	private boolean hasMatchFor(ChangeTrackingMatch ctm, Side sideToCheck) {
		if(ctm == null) return false;
		else return MatchUtil.isMatched(ctm.match, sideToCheck);
	}

	private void pickMatches(Comparison comparison, Side passedObjectSide, Side bSide, Side cSide, 
			Map<EObject, ChangeTrackingMatch> currentMatches, List<CandidateMatch> candidateMatches) {
		Collections.sort(candidateMatches);

		candidateMatches.forEach(cm -> {
			//FIXME
			// we establish a new mapping iff. cm.from does not have a bSide match and cm.cand does not have a aSide match
			// cm.cand is selected as a candidate because it does hot have a aSide match initially

			// currentMatches contains all the matches of aSide objects, but the matches of bSide objects are not actively indexed
			// if currentMatches contains a match of cm.cand, then this match must be one stored in this method
			// hence, before we establish/store a match for cm.cand, we should check if comparison has a match of cm.cand
			ChangeTrackingMatch aMatch = currentMatches.get(cm.from);
			ChangeTrackingMatch bMatch = currentMatches.get(cm.cand);

			if(hasMatchFor(aMatch, bSide) || hasMatchFor(bMatch, passedObjectSide)) return;
			
			if(aMatch == null && bMatch == null) {
				// check if cm.cand has a match in comparison. 
				// if so, reuse the match
				Match oldBMatch = comparison.getMatch(cm.cand);
				ChangeTrackingMatch ctm;
				if(oldBMatch == null) {
					Match newMatch = MatchUtil.createMatch();
					MatchUtil.setMatch(newMatch, cm.from, passedObjectSide);
					MatchUtil.setMatch(newMatch, cm.cand, bSide);
					ctm = new ChangeTrackingMatch(newMatch, passedObjectSide);
				} else {
					MatchUtil.setMatch(oldBMatch, cm.from, passedObjectSide);
					ctm = new ChangeTrackingMatch(oldBMatch, passedObjectSide);
				}
				currentMatches.put(cm.from, ctm);
				currentMatches.put(cm.cand, ctm);
			} else if(bMatch == null) {// aMatch != null
				Match oldBMatch = comparison.getMatch(cm.cand);
				if(oldBMatch != null) {
					return; // skip now, but we may further try to merge the two matches
				}

				if(aMatch.changedSide != null) {
					if(aMatch.changedSide != passedObjectSide) 
					throw new RuntimeException();
				}
				aMatch.changedSide = bSide;
				MatchUtil.setMatch(aMatch.match, cm.cand, bSide);
				currentMatches.put(cm.cand, aMatch);
			} else if(aMatch == null) { // bMatch != null
				MatchUtil.setMatch(bMatch.match, cm.from, passedObjectSide);
				bMatch.changedSide = passedObjectSide;
				currentMatches.put(cm.from, bMatch);
			} else {
				throw new RuntimeException();
			}


			// Match cMatch = comparison.getMatch(cm.cand);

			// if(cMatch == null) {
			// 	ChangeTrackingMatch partialMatch = currentMatches.get(cm.from);
			// 	if(partialMatch == null) {
			// 		Match newMatch = MatchUtil.createMatch();
			// 		MatchUtil.setMatch(newMatch, cm.from, passedObjectSide);
			// 		MatchUtil.setMatch(newMatch, cm.cand, bSide);
			// 		currentMatches.put(cm.from, new ChangeTrackingMatch(newMatch, passedObjectSide));
			// 	} else {
			// 		MatchUtil.setMatch(partialMatch.match, cm.cand, bSide);
			// 		if(partialMatch.changedSide != null && partialMatch.changedSide != passedObjectSide) 
			// 			throw new RuntimeException();
			// 		partialMatch.changedSide = bSide;
			// 	}
			// } else {
			// 	// tryFillMatched will skip matches that have been matched
			// 	MatchUtil.tryFillMatched(cMatch, cm.from, passedObjectSide);
			// 	if(cMatch.eContainer() != null) throw new RuntimeException();
			// 	currentMatches.put(cm.from, new ChangeTrackingMatch(cMatch, passedObjectSide));
			// }
		});
	}


	protected void createNewSubtreeSimMatches(Comparison comparison, Match match,
			Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {
		if (roots.origin.isEmpty() && match.getOrigin() == MatchUtil.PSEUDO_MATCHED_OBJECT) {
			match.setOrigin(null); // finalize this match
		}

		((BasicEList<Match>) comparison.getMatches()).addUnique(match);

		// if match is a full match, remove objects
		if (MatchUtil.isFullMatch(match)) {
			index.remove(match.getLeft(), Side.LEFT);
			index.remove(match.getRight(), Side.RIGHT);
			if (match.getOrigin() != null) {
				index.remove(match.getOrigin(), Side.ORIGIN);
			}
		}

		List<EObject> lefts = getChildren(match.getLeft());
		List<EObject> rights = getChildren(match.getRight());
		List<EObject> origins = getChildren(match.getOrigin());
		
		Map<EObject, EObject> matchesLR = createSimMatches(lefts, rights);
		Map<EObject, EObject> matchesLO = createSimMatches(lefts, origins);
		
		for(EObject l : lefts) {
			EObject r = matchesLR.get(l);
			EObject o = matchesLO.get(l);
			
			if(r==null && o==null) continue;
			
			Match childMatch = MatchUtil.createMatch();
			
			childMatch.setLeft(l);
			childMatch.setRight(r == null ? MatchUtil.PSEUDO_MATCHED_OBJECT : r);
			childMatch.setOrigin(o == null ? MatchUtil.PSEUDO_MATCHED_OBJECT : o);
			
			createNewSubtreeSimMatches(comparison, childMatch, roots);
		}
	}
	
	protected Map<EObject, EObject> createSimMatches(List<EObject> alist, List<EObject> blist) {
		if(alist.isEmpty() || blist.isEmpty()) return Collections.emptyMap();
		else {
			alist = getCopy(alist);
			blist = getCopy(blist);
			
			Multimap<Long, EObject> checksumMap = LinkedHashMultimap.create();
			blist.forEach(e->{
				ElementIndexAdapter adapter = ElementIndexAdapter.getAdapter(e);
				if(adapter!=null)
					checksumMap.put(adapter.getTreeHash().subtreeChecksum, e);
			});
			
			Map<EObject, EObject> matches = new HashMap<EObject, EObject>();
			
			Iterator<EObject> aItr = alist.iterator();
			while(aItr.hasNext()) {
				EObject e = aItr.next();
				ElementIndexAdapter adapter = ElementIndexAdapter.getAdapter(e);
				if(adapter != null) {
					long checksum = adapter.getTreeHash().subtreeChecksum;
					Collection<EObject> cand = checksumMap.get(checksum);
					if(cand==null || cand.isEmpty()) {
						// NO Match
					} else {
						Iterator<EObject> candItr = cand.iterator();
						
						while(candItr.hasNext()) {
							EObject match = candItr.next();
							if(match.eClass() == e.eClass()) {							
								aItr.remove();
								candItr.remove();
								matches.put(e, match);
								break;
							}
						}
						
					}
				}
			}
			
			return matches;
		}
	}
	
	
	protected List<EObject> getCopy(List<EObject> list) {
		if(list.isEmpty()) return list;
		else return new ArrayList<>(list);
	}
	protected List<EObject> getUnmatchedChildren(Comparison comparison, List<EObject> children, Side sideToFill) {
		if(children.isEmpty()) return Collections.emptyList();
		else {
			return children.stream().filter(e->{
				Match m = comparison.getMatch(e);
				return !(MatchUtil.isMatched(m, sideToFill));
			}).toList();
		}
	}
	
	protected void fillSubtreeSimMatches(Comparison comparison, Match match, Side sideToFill,
			Triple<Collection<EObject>, Collection<EObject>, Collection<EObject>> roots) {

		if (MatchUtil.isFullMatch(match)) {
			index.remove(match.getLeft(), Side.LEFT);
			index.remove(match.getRight(), Side.RIGHT);
			if (match.getOrigin() != null) {
				index.remove(match.getOrigin(), Side.ORIGIN);
			}
		}

		List<EObject> lefts = getUnmatchedChildren(match.getLeft(), comparison, sideToFill);
		List<EObject> rights = getUnmatchedChildren(match.getRight(), comparison, sideToFill);
		List<EObject> origins = getUnmatchedChildren(match.getOrigin(), comparison, sideToFill);

		List<EObject> colA = null;
		List<EObject> colB = null;
		List<EObject> colToFill = null;

		switch (sideToFill) {
		case LEFT:
			colA = rights;
			colB = origins;
			colToFill = lefts;
			break;
		case RIGHT:
			colA = lefts;
			colB = origins;
			colToFill = rights;
			break;
		case ORIGIN:
			colA = lefts;
			colB = rights;
			colToFill = origins;
			break;
		}
		
		Map<EObject, EObject> matchesLR = createSimMatches(colToFill, colA);
		Map<EObject, EObject> matchesLO = createSimMatches(colToFill, colB);
		
		for(EObject e : colToFill) {
			EObject m = matchesLR.get(e);
			if(m!=null && e.eClass() == m.eClass()) {
				Match childMatch = comparison.getMatch(m);
				if (childMatch != null) {
					if (MatchUtil.tryFillMatched(childMatch, e, sideToFill)) {
						fillSubtreeSimMatches(comparison, childMatch, sideToFill, roots);
						continue;
					} else {
						System.err.println(childMatch);
					}
				} else {
					System.err.println("null child match [A]!");
				}
			} else {
				m = matchesLO.get(e);
				if(m!=null && e.eClass() == m.eClass()) {
					Match childMatch = comparison.getMatch(m);
					if (childMatch != null) {
						if (MatchUtil.tryFillMatched(childMatch, e, sideToFill)) {
							fillSubtreeSimMatches(comparison, childMatch, sideToFill, roots);
							continue;
						} else {
							System.err.println(childMatch);
						}
					} else {
						System.err.println("null child match [B]!");
					}
				}
			}
		}
	}
	
	private List<EObject> getUnmatchedChildren(EObject obj, Comparison c, Side side) {
		return getChildren(obj).stream().filter(e->!MatchUtil.hasMatchFor(e, c, side)).toList();
	}
}
