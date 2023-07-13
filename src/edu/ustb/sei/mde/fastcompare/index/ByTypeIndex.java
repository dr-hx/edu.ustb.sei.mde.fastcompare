package edu.ustb.sei.mde.fastcompare.index;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.match.ScopeQuery;

public class ByTypeIndex implements ObjectIndex {
    final private ScopeQuery scope;
    final private MatcherConfigure matcherConfigure;
    final private Map<EClass, ObjectIndex> allIndexes;

    public ByTypeIndex(ScopeQuery scope, MatcherConfigure matcherConfigure) {
        this.scope = scope;
        this.matcherConfigure = matcherConfigure;
        this.allIndexes = new LinkedHashMap<>(512);
	}

	@Override
	public Iterable<EObject> getValuesStillThere(Side side) {
		List<Iterable<EObject>> allLists = Lists.newArrayList();
		for (ObjectIndex typeSpecificIndex : allIndexes.values()) {
			allLists.add(typeSpecificIndex.getValuesStillThere(side));
		}
		return Iterables.concat(allLists);
	}
	@Override
	public Map<Side, EObject> findClosests(Comparison inProgress, EObject eObj, Side side) {
		EClass clazz = eObj.eClass();
        ObjectIndex subIndex = allIndexes.get(clazz);
        if(subIndex == null) return Collections.emptyMap();
        else return subIndex.findClosests(inProgress, eObj, side);
	}

	@Override
	public void remove(EObject eObj, Side side) {
		EClass clazz = eObj.eClass();
        ObjectIndex subIndex = allIndexes.get(clazz);
        if(subIndex != null) subIndex.remove(eObj, side);
	}

	@Override
	public void index(EObject eObj, Side side) {
		EClass clazz = eObj.eClass();
        ObjectIndex subIndex = allIndexes.computeIfAbsent(clazz, (c)->{
            if(matcherConfigure.shouldDoSimHash(c))
                return new ProximityIndex(scope, matcherConfigure, HashFilterIndex::new);
            else 
                return new ProximityIndex(scope, matcherConfigure, NoFilterIndex::new);
        });
        subIndex.index(eObj, side);
	}
}
