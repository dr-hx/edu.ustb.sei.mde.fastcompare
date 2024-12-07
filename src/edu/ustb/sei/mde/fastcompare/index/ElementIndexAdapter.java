package edu.ustb.sei.mde.fastcompare.index;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import edu.ustb.sei.mde.fastcompare.shash.SimHashValue;
import edu.ustb.sei.mde.fastcompare.shash.StringHashFunction;
import edu.ustb.sei.mde.fastcompare.utils.CRC64;
import edu.ustb.sei.mde.fastcompare.utils.CommonUtils;

/**
 * This class is used to store element indices.
 */
public class ElementIndexAdapter extends AdapterImpl {
    public static final long INVALID_IHASH = 0xF000000000000000L;

    // DUAL HASH
    public SimHashValue similarityHash = null;
    public long localIdentityHash = INVALID_IHASH;

    // TREE HASH
    protected TreeHashValue treeHash = null;

    static protected StringHashFunction stringHash = new StringHashFunction();

    public TreeHashValue getTreeHash() {
    	if(this.treeHash == null) {
    		this.treeHash = computeHash((EObject) this.getTarget());
    	}
    	return this.treeHash;
    }

    public void init() {
        localIdentityHash = INVALID_IHASH;
        similarityHash = null;
        treeHash = null;
    }
	
    protected TreeHashValue computeHash(EObject target) {
		TreeHashValue hash = new TreeHashValue();
		
		CRC64 subtreeChecksum = new CRC64();
		CommonUtils.update(subtreeChecksum, localIdentityHash);
        CommonUtils.update(subtreeChecksum, '{');
		
		for (EObject child : target.eContents()) {
			ElementIndexAdapter childAdapter = ElementIndexAdapter.getAdapter(child);
			if (childAdapter != null) {
				TreeHashValue childTreeHash = childAdapter.getTreeHash();				
				CommonUtils.update(subtreeChecksum, childTreeHash.subtreeChecksum);
				hash.size += childTreeHash.size;
				if(childTreeHash.height + 1 > hash.height)
					hash.height = childTreeHash.height + 1;
			}
		}
		
		CommonUtils.update(subtreeChecksum, '}');
		
        hash.subtreeChecksum = subtreeChecksum.getValue();
		
        return hash;
	}

    public long getSubtreeIdentityHash() {
        return getTreeHash().subtreeChecksum;
    }

    public int position;

    public ElementIndexAdapter(int id) {
        this.position = id;
    }

    @Override
    final public boolean isAdapterForType(Object type) {
        return type == ElementIndexAdapter.class;
    }

    @SuppressWarnings("unchecked")
    static public <T extends ElementIndexAdapter> T getAdapter(EObject obj) {
        return (T) EcoreUtil.getExistingAdapter(obj, ElementIndexAdapter.class);
    }
}
