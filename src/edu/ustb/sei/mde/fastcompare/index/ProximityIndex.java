package edu.ustb.sei.mde.fastcompare.index;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.match.eobject.ScopeQuery;
import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.fastcompare.config.Hasher;
import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.match.DistanceFunction;

public class ProximityIndex implements ObjectIndex {
    final private ScopeQuery scope;
    final private MatcherConfigure matcherConfigure;
	final DistanceFunction meter;
	final Hasher hasher;

    final ObjectFilterIndex left;
    final ObjectFilterIndex right;
    final ObjectFilterIndex origin;
	
    public ProximityIndex(ScopeQuery scope, MatcherConfigure matcherConfigure, Function<MatcherConfigure, ObjectFilterIndex> creator) {
		this.scope = scope;
		this.matcherConfigure = matcherConfigure;
		this.meter = matcherConfigure.getDistanceFunction();
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

	@Override
	public Map<Side, EObject> findClosests(Comparison inProgress, EObject eObj, Side side) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'findClosests'");
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
}
