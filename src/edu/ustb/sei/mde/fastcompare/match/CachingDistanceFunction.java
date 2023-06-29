package edu.ustb.sei.mde.fastcompare.match;

import java.util.Map;
import java.util.function.BiPredicate;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.utils.AccessBasedLRUCache;

public class CachingDistanceFunction implements DistanceFunction {
    protected final DistanceFunction meter;
	protected Map<TupleKey<EObject, DistanceFunction>, Double> distanceCache;	

	public CachingDistanceFunction(DistanceFunction meter) {
		this.meter = meter;
		distanceCache = new AccessBasedLRUCache<TupleKey<EObject, DistanceFunction>, Double>(1<<16, 1<<10, .75F);
	}

	@Override
	public double distance(Comparison inProgress, EObject a, EObject b, Boolean haveSameContainer) {
		TupleKey<EObject, DistanceFunction> key = new TupleKey<EObject, DistanceFunction>(a, b, this, TupleKey.otherPred);
		Double previousResult = distanceCache.get(key);
		if (previousResult == null) {
			double dist = meter.distance(inProgress, a, b, haveSameContainer);
			distanceCache.put(key, Double.valueOf(dist));
			return dist;
		}
		return previousResult.doubleValue();
	}

	public double distance(Comparison inProgress, EObject a, EObject b, Boolean haveSameContainer, boolean canCache) {
		if(canCache) {
			return this.distance(inProgress, a, b, haveSameContainer);
		} else 
			return meter.distance(inProgress, a, b, haveSameContainer);
	}

	@Override
	public double getThresholdAmount(EObject eObj) {
		return meter.getThresholdAmount(eObj);
	}

	@Override
	public boolean areIdentic(Comparison inProgress, EObject a, EObject b) {
		return meter.areIdentic(inProgress, a, b);
	}


	protected class TupleKey<V, W> {
		// CHECKSTYLE:OFF
		V a;
		V b;
		W c;
		
		final public static BiPredicate<Long, Long> longPred = (l, r) -> l.equals(r);
		final public static BiPredicate<Object, Object> otherPred = (l, r) -> l == r;
		
		@SuppressWarnings("rawtypes")
		final private BiPredicate pred;

		public TupleKey(V a, V b, W c, BiPredicate<?,?> pred) {
			super();
			this.a = a;
			this.b = b;
			this.c = c;
			this.pred = pred;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + c.hashCode();
			int first = a.hashCode();
			int second = b.hashCode();
			if (first > second) {
				int tmp = first;
				first = second;
				second = tmp;
			}
			result = prime * result + first;
			result = prime * result + second;
			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			TupleKey<?,?> other = (TupleKey<?, ?>)obj;
			if (!c.equals(other.c)) {
				return false;
			}
			return (pred.test(a, other.a) && pred.test(b, other.b)) 
					|| (pred.test(b, other.a) && pred.test(a, other.b));

		}
	}


	@Override
	public MatcherConfigure getMatcherConfigure() {
		return meter.getMatcherConfigure();
	}

	@Override
	public boolean haveSameContainer(Comparison inProgress, EObject a, EObject b) {
		return meter.haveSameContainer(inProgress, a, b);
	}
}
