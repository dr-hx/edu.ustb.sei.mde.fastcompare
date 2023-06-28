package edu.ustb.sei.mde.fastcompare.shash;

import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import edu.ustb.sei.mde.fastcompare.utils.AccessBasedLRUCache;
import edu.ustb.sei.mde.fastcompare.utils.URIComputer;

public class URIHashFunction implements SHashFunction<EObject> {
    private Map<Iterable<String>, Long> hashCache = new AccessBasedLRUCache<>(1024, 1024, 0.75f);
    private URIComputer uriComputer;

    public URIHashFunction(URIComputer uriComputer) {
        this.uriComputer = uriComputer;
    }
    
    @Override
    public long hash(EStructuralFeature feature, EObject value) {
        Iterable<String> fragments = uriComputer.getOrComputeLocation(value);
		long h = hashCache.computeIfAbsent(fragments, URIHashFunction::hashStringIterable);
		return h;
    }

    static private long hashStringIterable(Iterable<String> strings) {
        long code = 0;
        for (String d : strings) {
            int h = (int) (d.hashCode() % 64);
            code |= (1L << h);
        }
        return code;
    }

	@Override
	public long rawHash(EObject value) {
		throw new UnsupportedOperationException("Unimplemented method 'rawHash'");
	}
    
}
