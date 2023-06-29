package edu.ustb.sei.mde.fastcompare.match;

import java.util.Map;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import edu.ustb.sei.mde.fastcompare.index.ElementIndexAdapter;
import edu.ustb.sei.mde.fastcompare.utils.AccessBasedLRUCache;

public class CachingDistanceFunctionEx extends CachingDistanceFunction {

	public CachingDistanceFunctionEx(EditionDistance wrapped) {
		super(wrapped);
	}
	
	private Map<TupleKey<Long, EClass>, HeuristicCache> heuristicCache = new AccessBasedLRUCache<TupleKey<Long, EClass>, HeuristicCache>(10000, 1000, .75F);
	
	private long getDigest(EObject object) {
		ElementIndexAdapter indexAdapter = ElementIndexAdapter.getAdapter(object);
        return indexAdapter.localIdentityHash;
	}
	
	protected double containerOrderDiff(EObject a, EObject b, boolean sameContainer) {
		double changes = 0;
		
		if(sameContainer) {
			int aIndex = DistanceFunction.getContainmentIndex(a);
			int bIndex = DistanceFunction.getContainmentIndex(b);
			
			if (aIndex != bIndex) {
				changes = 5;
			} else changes = 0;
		}
		
		if (a.eContainingFeature() != b.eContainingFeature()) {
			changes += Math.max(
					getMatcherConfigure().getClassConfigure(a.eClass()).getContainingFeatureWeight(a),
					getMatcherConfigure().getClassConfigure(b.eClass()).getContainingFeatureWeight(b));
		}
		
		return changes;
	}
	
	class HeuristicCache {
		public double[] distances;
		public EObject[] belief;
		public double maxDist;
	}
	
	public double distance(Comparison inProgress, EObject a, EObject b) {
		TupleKey<EObject, DistanceFunction> key = new TupleKey<EObject, DistanceFunction>(a, b, this, TupleKey.otherPred);
		Double previousResult = distanceCache.get(key);
		if (previousResult == null) {
			TupleKey<Long, EClass> tuple = new TupleKey<>(getDigest(a), getDigest(b), a.eClass(), TupleKey.longPred);
			
			HeuristicCache hPreviousResults = heuristicCache.computeIfAbsent(tuple, k -> {
                HeuristicCache cache = new HeuristicCache();
                cache.distances = new double[] { -1, -1 };
                cache.belief = new EObject[] { a, b };
                cache.maxDist = Math.max(meter.getThresholdAmount(a), meter.getThresholdAmount(b));
                return cache;
            });

            boolean sameContainer = this.haveSameContainer(inProgress, a, b);
            final int pos = sameContainer ? 0 : 1;
            double containerDiff = this.containerOrderDiff(a, b, sameContainer);
			
			double hCachedDist = hPreviousResults.distances[pos];
			if (hCachedDist != -1) {
                double retDist = 0;
                if (hCachedDist == Double.MAX_VALUE)
                    retDist = hCachedDist;
                else {
                    retDist = hCachedDist + containerDiff;
                    if (retDist > hPreviousResults.maxDist)
                        retDist = Double.MAX_VALUE;
                }
                return retDist;
            }
			
			double dist = meter.distance(inProgress, a, b);				
			distanceCache.put(key, Double.valueOf(dist));
			
			double cachedDist = dist;
			if(dist > 0 && dist < Double.MAX_VALUE) {
				cachedDist -= containerDiff;
				assert cachedDist >= 0;
			}
			hPreviousResults.distances[pos] = cachedDist;
			
			return dist;
		}
		return previousResult.doubleValue();
	}
	

}