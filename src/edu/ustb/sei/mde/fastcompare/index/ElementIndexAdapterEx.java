package edu.ustb.sei.mde.fastcompare.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import edu.ustb.sei.mde.fastcompare.utils.CRC64;
import edu.ustb.sei.mde.fastcompare.utils.CommonUtils;

public class ElementIndexAdapterEx extends ElementIndexAdapter {

    public ElementIndexAdapterEx(int id) {
        super(id);
    }
    
    @Override
    protected TreeHashValueEx computeHash(EObject target) {
		TreeHashValueEx hash = new TreeHashValueEx();
		
		CRC64 subtreeChecksum = new CRC64();
		CRC32 subtreeStructureChecksum = new CRC32();
		
		// Step 1. Build local content checksum and local content simhash
		
		hash.subtreeChecksum = localIdentityHash;
		hash.localContentHash = buildLocalSimhash(target);
		hash.lchBits = TreeHashValueEx.computeBitCounts(hash.localContentHash);
		
		
		// Step 2. Build tree structure checksum, and tree content simhash
		CommonUtils.update(subtreeChecksum, hash.subtreeChecksum);
		CommonUtils.update(subtreeChecksum, '{');
		
		CommonUtils.update(subtreeStructureChecksum, target.eClass().getName());
		CommonUtils.update(subtreeStructureChecksum, '{');
		
		List<TreeHashValueEx> childHashes = new ArrayList<TreeHashValueEx>(target.eContents().size());
		
		for (EObject child : target.eContents()) {
			ElementIndexAdapterEx childAdapter = ElementIndexAdapter.getAdapter(child);
			if (childAdapter != null) {
				TreeHashValueEx childTreeHash = (TreeHashValueEx) childAdapter.getTreeHash();				
				CommonUtils.update(subtreeChecksum, childTreeHash.subtreeChecksum);
				CommonUtils.update(subtreeStructureChecksum, childTreeHash.subtreeStructureChecksum);
				
				hash.size += childTreeHash.size;
				if(childTreeHash.height + 1 > hash.height)
					hash.height = childTreeHash.height + 1;
				
				childHashes.add(childTreeHash);
			}
		}
		
		CommonUtils.update(subtreeChecksum, '}');
		CommonUtils.update(subtreeStructureChecksum, '}');
		
		hash.subtreeChecksum = subtreeChecksum.getValue();
		hash.subtreeStructureChecksum = (int) subtreeStructureChecksum.getValue();
		
		hash.childContentHash = convHashes(hash, childHashes);
		hash.cchBits = TreeHashValueEx.computeBitCounts(hash.childContentHash);
		
		return hash;
	}

    static final float[] bitarray = new float[64];

    protected long convHashes(TreeHashValueEx parent, List<TreeHashValueEx> childHashes) {
    	
    	if(childHashes.size() == 0) {
    		return 0;
    	} else if(childHashes.size() == 1) {
    		TreeHashValueEx c = childHashes.get(0);
    		return c.localContentHash | c.childContentHash;
    	} else {
    		final int totalWeight = (parent.size - 1) * 64;
    		int factoredWeightSum = 0;
    		
    		// factor = \sum_i=1^N(w_i*p_i)/(\sum_i=1^N(w_i*(1-p_i)))
    		// p_i is approximated by #bits_i/64
    		// MAYBE factor_i = p_i/(1-p_i) = #bits / (64 - #bits) \in [0, 64]
    		for(TreeHashValueEx h : childHashes) {
    			final int bitcount = TreeHashValueEx.computeBitCounts(h.localContentHash);
    			final float weight = h.size;
    			factoredWeightSum += weight * bitcount;
    		}
    		
    		final float factor;
    		if(factoredWeightSum == 0) return 0;
    		
    		if(totalWeight == factoredWeightSum) factor = 0;
    		else factor = 1.0f / (((float)totalWeight)/factoredWeightSum - 1.0f);
    		
    		Arrays.fill(bitarray, 0);
    		for(TreeHashValueEx h : childHashes) {
    			final float weight = h.size;
    			final float nWeight = weight * factor;
				fillBitarray(bitarray, h.localContentHash, weight, nWeight);
    			fillBitarray(bitarray, h.childContentHash, weight / 10, nWeight / 10);
    		}
    		return zipBitarray(bitarray);
    	}
    	
	}

	static private void fillBitarray(final float[] bitarray, final long content, final float pWeight, final float nWeight) {
		for(int i=0; i<64; i++) {
			if((content & (1L<<i)) == 0) {
				bitarray[i] -= nWeight;
			} else {
				bitarray[i] += pWeight;
			}
		}
	}
	
	static private long zipBitarray(final float[] bitarray) {
		long value = 0;
		for(int i=0;i<64;i++) {
			if(bitarray[i] >= 0) {
				value |= (1L << i);
			}
		}
		return value;
	}

	private long buildLocalSimhash(EObject o) {
        long hash = 0L;
        for(EStructuralFeature feature : o.eClass().getEAllAttributes()) {
            if(feature.isMany()==false) {
            	Class<?> instanceClass = feature.getEType().getInstanceClass();
				if(instanceClass==String.class) {
					String value = (String) o.eGet(feature);
					if(value != null)
						hash |= stringHash.hash(null, value);
            	}
            }
        }
        return hash;
    }
}
