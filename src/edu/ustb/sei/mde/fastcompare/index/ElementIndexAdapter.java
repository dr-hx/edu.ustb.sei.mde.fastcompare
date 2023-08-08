package edu.ustb.sei.mde.fastcompare.index;

import java.util.zip.CRC32;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

import edu.ustb.sei.mde.fastcompare.shash.Hash64;
import edu.ustb.sei.mde.fastcompare.utils.CommonUtils;

/**
 * This class is used to store element indices.
 */
public class ElementIndexAdapter extends AdapterImpl {
    private static final long INVALID_IHASH = 0xF000000000000000L;

    public int depth = 0;
    public Hash64 similarityHash = null;
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
            for(EObject child : object.eContents()) {
                ElementIndexAdapter childAdapter = ElementIndexAdapter.getAdapter(child);
                long childTreeHash = childAdapter.getSubtreeIdentityHash();
                if(childTreeHash != INVALID_IHASH) {
                    CommonUtils.update(crc32, childTreeHash);
                }
            }
            CommonUtils.update(crc32, "}");
            treeIdentityHash = crc32.getValue();
        }

        return treeIdentityHash;
    }

    public int position;

    public ElementIndexAdapter() {
        this.position = totalNumbers ++;
    }

    @Override
    public boolean isAdapterForType(Object type) {
        return type == ElementIndexAdapter.class;
    }

    static private int totalNumbers = 0;

    static public void reset() {
        totalNumbers = 0;
    }

    static public ElementIndexAdapter equip(EObject object) {
        ElementIndexAdapter adapter = (ElementIndexAdapter) EcoreUtil.getExistingAdapter(object, ElementIndexAdapter.class);
        if(adapter == null) {
            adapter = new ElementIndexAdapter();
            object.eAdapters().add(0, adapter);
        } else {
            adapter.init();
        }
        return adapter;
    }

    static public ElementIndexAdapter getAdapter(EObject obj) {
        return (ElementIndexAdapter) EcoreUtil.getExistingAdapter(obj, ElementIndexAdapter.class);
    }
}
