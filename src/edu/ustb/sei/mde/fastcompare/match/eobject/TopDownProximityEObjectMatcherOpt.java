package edu.ustb.sei.mde.fastcompare.match.eobject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.index.ElementIndexAdapter;
import edu.ustb.sei.mde.fastcompare.index.ElementIndexAdapterWithStructuralChecksum;
import edu.ustb.sei.mde.fastcompare.index.ObjectIndex.Side;
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
			if(MatchUtil.isFullMatch(partialMatchPair.match)) {
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
		List<CandidateMatch> bCandidates = new ArrayList<>();
		List<CandidateMatch> cCandidates = new ArrayList<>();

		private void add(EObject from, int iFrom, EObject cand, int iCand, List<CandidateMatch> candidates) {
			double sim = computePartialSimilarity(from, cand);
			if(sim==0.0) return;
			if(sim>0.96) sim = 1.0;

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

	protected double computePartialSimilarity(EObject from, EObject cand) {
		ElementIndexAdapterWithStructuralChecksum adapter = ElementIndexAdapter.getAdapter(from);
		if (adapter == null) return 0.0;
		if (cand.eClass() != from.eClass()) return 0.0;

		final long subtreeKey = adapter.getTreeStructuralChecksum();

		final int size = adapter.size;
		final int height = adapter.height;

		if (size < 2 && height < 2) return 0.0;

		ElementIndexAdapterWithStructuralChecksum cAdapter = ElementIndexAdapter.getAdapter(cand);
		
		if (cAdapter != null) {
			final long ctreeKey = cAdapter.getTreeStructuralChecksum();
			if (size == cAdapter.size && height == cAdapter.height && ctreeKey == subtreeKey) {
				final double sim = adapter.treeSimHash.similarity(cAdapter.treeSimHash);
				if (sim > 0.85) {
					return sim;
				}
			}
		} 
		
		return 0.0;
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
			final Match matchOfCur = matchOfCurPair.match;

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

	private void pickMatches(Comparison comparison, Side passedObjectSide, Side bSide, Side cSide, 
			Map<EObject, ChangeTrackingMatch> currentMatches, List<CandidateMatch> candidateMatches) {
		Collections.sort(candidateMatches);
		// 1. get match m of from, or create one
		// 2. if m.bSide = empty, then fill bCand
		candidateMatches.forEach(cm -> {
			Match cMatch = comparison.getMatch(cm.cand);

			if(cMatch == null) {
				ChangeTrackingMatch partialMatch = currentMatches.get(cm.from);
				if(partialMatch == null) {
					Match newMatch = MatchUtil.createMatch();
					MatchUtil.setMatch(newMatch, cm.from, passedObjectSide);
					MatchUtil.setMatch(newMatch, cm.cand, bSide);
					currentMatches.put(cm.from, new ChangeTrackingMatch(newMatch, passedObjectSide));
				} else {
					MatchUtil.setMatch(partialMatch.match, cm.cand, bSide);
					if(partialMatch.changedSide != null) throw new RuntimeException();
					partialMatch.changedSide = bSide;
				}
			} else {
				// tryFillMatched will skip matches that have been matched
				MatchUtil.tryFillMatched(cMatch, cm.from, passedObjectSide);
				if(cMatch.eContainer() != null) throw new RuntimeException();
				currentMatches.put(cm.from, new ChangeTrackingMatch(cMatch, passedObjectSide));
			}
		});
	}
}
