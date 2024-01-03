package edu.ustb.sei.mde.fastcompare.index;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.shash.SimHashValue;
import edu.ustb.sei.mde.fastcompare.utils.CommonUtils;

public class HashFilterIndex implements ObjectFilterIndex {
    private Map<Long, Set<EObject>> integrityMap;
    // private Map<Long, Set<EObject>> subtreeIntegrityMap;
    private Map<SimHashValue, Set<EObject>> similarityMap;
    private Set<EObject> allObjects;
    private final Function<EObject, Double> computeThresholdAmount;
    private final MatcherConfigure matcherConfigure;

    public HashFilterIndex(MatcherConfigure configure) {
        if(configure.isUsingIdentityHash()) {
            integrityMap = new HashMap<>();
        } else {
            integrityMap = null;
        }
        // if(configure.isUsingSubtreeHash()) {
        //     subtreeIntegrityMap = new HashMap<>();
        // } else {
        //     subtreeIntegrityMap = null;
        // }
        similarityMap = new HashMap<>();
        allObjects = ObjectFilterIndex.createSet(null);
        this.matcherConfigure = configure;
        this.computeThresholdAmount = configure.getDistanceFunction()::getThresholdAmount;
    }


    public Iterable<EObject> allCandidates() {
        return new LinkedHashSet<>(allObjects);
    }

    public double getContainerSimilarityRatio(EObject object) {
        EClass clazz = object.eClass();
        double total = computeThresholdAmount.apply(object);
        int containerWeight = this.matcherConfigure.getClassConfigure(clazz).getParentWeight(object);
        double ratio = containerWeight / total;
		return ratio;
	}

    public Iterable<EObject> filterCandidates(Comparison inProgress, EObject eObj, Optional<EObject> candidateContainer, double threshold) {
        ElementIndexAdapter eObjAdapter = ElementIndexAdapter.getAdapter(eObj);
        CommonUtils.checkNotNull(eObjAdapter);

        if(threshold == 0) {
            if(this.integrityMap!=null) {
                long idhash = eObjAdapter.localIdentityHash;
                Set<EObject> candidates = integrityMap.getOrDefault(idhash, Collections.emptySet());
                return candidates;
            } else {
                SimHashValue shash = eObjAdapter.similarityHash;
                Set<EObject> candidates = integrityMap.getOrDefault(shash, Collections.emptySet());
                return candidates;
            }
        } else {
            double minSim = this.matcherConfigure.getClassConfigure(eObj.eClass()).getSimThreshold();
            double containerDiff = getContainerSimilarityRatio(eObj);
            SimHashValue shash = eObjAdapter.similarityHash;
            LinkedList<EObject> result = new LinkedList<>();
            for (Entry<SimHashValue, Set<EObject>> bucket : similarityMap.entrySet()) {
                SimHashValue bhash = bucket.getKey();
                double sim = SimHashValue.similarity(shash, bhash);
                if(sim >= minSim) {
                    if(candidateContainer == null) { // unknown
                        result.addAll(bucket.getValue());
                    } else {
                        EObject actualCandidateContainer = candidateContainer.orElse(null);
                        for(EObject cand : bucket.getValue()) {
                            if (actualCandidateContainer == cand.eContainer()) {
                                result.add(cand);
                            } else {
                                if(sim >= (minSim + containerDiff)) {
                                    result.add(cand);
                                }
                            }
                        }
                    }
                }
            }
            return result;
        }
    }

    public void remove(EObject eObj) {
        ElementIndexAdapter adapter = ElementIndexAdapter.getAdapter(eObj);
        allObjects.remove(eObj);
        Set<EObject> list;
        if (integrityMap != null) {
            list = integrityMap.get(adapter.localIdentityHash);
            if (list != null)
                list.remove(eObj);
        }
        // TODO: we may not have to remove it
        // TODO: if we have to do so, we must remove recursively
        // if (subtreeIntegrityMap != null) {
        //     // we also have to remove the tree rooting at eObj from the index
        //     list = subtreeIntegrityMap.get(adapter.getSubtreeIdentityHash());
        //     if (list != null)
        //         list.remove(eObj);
        // }
        list = similarityMap.get(adapter.similarityHash);
        if (list != null)
            list.remove(eObj);
    }

    public void index(EObject eObj) {
        ElementIndexAdapter adapter = ElementIndexAdapter.getAdapter(eObj);
        
        allObjects.add(eObj);
        Set<EObject> list;
        if(integrityMap != null) {
            list = integrityMap.computeIfAbsent(adapter.localIdentityHash, ObjectFilterIndex::createSet);
            list.add(eObj);
        }
        list = similarityMap.computeIfAbsent(adapter.similarityHash, ObjectFilterIndex::createSet);
        list.add(eObj);
    }
}
