package edu.ustb.sei.mde.fastcompare.index;

import java.util.zip.CRC32;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

import edu.ustb.sei.mde.fastcompare.shash.SimHashValue;
import edu.ustb.sei.mde.fastcompare.utils.CommonUtils;

/**
 * This class is used to store element indices.
 */
public class ElementIndexAdapter extends AdapterImpl {
    public static final long INVALID_IHASH = 0xF000000000000000L;

    public int height = 1;
    public SimHashValue similarityHash = null;
    public long localIdentityHash = INVALID_IHASH;
    private long treeIdentityHash = INVALID_IHASH;

    public void init() {
        localIdentityHash = INVALID_IHASH;
        similarityHash = null;
        treeIdentityHash = INVALID_IHASH;
    }

    public long getSubtreeIdentityHash() {
        if(treeIdentityHash == INVALID_IHASH) {
            if(localIdentityHash == INVALID_IHASH) return INVALID_IHASH;
            CRC32 crc32 = new CRC32();
            CommonUtils.update(crc32, localIdentityHash);
            CommonUtils.update(crc32, "{");
            EObject object = (EObject) this.getTarget();
            // long childHashes = 0L;
            for(EObject child : object.eContents()) {
                ElementIndexAdapter childAdapter = ElementIndexAdapter.getAdapter(child);
                if(childAdapter != null) {
                    long childTreeHash = childAdapter.getSubtreeIdentityHash();
                    if(childTreeHash != INVALID_IHASH) {
                        CommonUtils.update(crc32, childTreeHash);
                        // childHashes ^= childTreeHash;
                    }
                    if(childAdapter.height + 1 > height)
                        height = childAdapter.height + 1;
                }
            }
            // CommonUtils.update(crc32, childHashes);
            CommonUtils.update(crc32, "}");
            treeIdentityHash = crc32.getValue();
        }

        return treeIdentityHash;
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
