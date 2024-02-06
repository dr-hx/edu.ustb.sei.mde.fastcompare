package edu.ustb.sei.mde.fastcompare.index;

import java.util.Collection;
import java.util.Collections;
import java.util.zip.CRC32;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;

import edu.ustb.sei.mde.fastcompare.shash.SimHashValue;
import edu.ustb.sei.mde.fastcompare.utils.CommonUtils;
import edu.ustb.sei.mde.fastcompare.utils.TreesIterator;

/**
 * This class is used to store element indices.
 */
public class ElementIndexAdapter extends AdapterImpl {
    private static final long INVALID_IHASH = 0xF000000000000000L;

    public int depth = 0;
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
            for(EObject child : object.eContents()) {
                ElementIndexAdapter childAdapter = ElementIndexAdapter.getAdapter(child);
                if(childAdapter != null) {
                    long childTreeHash = childAdapter.getSubtreeIdentityHash();
                    if(childTreeHash != INVALID_IHASH) {
                        CommonUtils.update(crc32, childTreeHash);
                    }
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


    static private void clearAdapter(EObject o) {
        ElementIndexAdapter adapter = getAdapter(o);
        if(adapter != null)
            o.eAdapters().remove(adapter);
    }
    static public void removeAdapters(Collection<EObject> roots) {
        for(EObject eObj : roots) {
            clearAdapter(eObj);
            TreeIterator<EObject> iter = eObj.eAllContents();
            while(iter.hasNext()) {
                EObject next = iter.next();
                clearAdapter(next);
            }
        }
    }
    static public void removeAdapters(Notifier notifier) {
        if(notifier == null) return;
        else if(notifier instanceof ResourceSet) {
            for(Resource resource : ((ResourceSet) notifier).getResources()) {
                removeAdapters(resource);
            }
        } else if(notifier instanceof Resource) {
            removeAdapters(((Resource) notifier).getContents());
        } else if(notifier instanceof EObject) {
            removeAdapters(Collections.singleton((EObject) notifier));
        } else {}
    }
}
