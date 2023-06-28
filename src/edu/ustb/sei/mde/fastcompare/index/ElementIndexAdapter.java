package edu.ustb.sei.mde.fastcompare.index;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

import edu.ustb.sei.mde.fastcompare.shash.Hash64;

/**
 * This class is used to store element indices.
 */
public class ElementIndexAdapter extends AdapterImpl {
    public long localIdentityHash;
    public Hash64 similarityHash;
    public long treeIdentityHash;

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

    static public void equip(EObject object) {
        ElementIndexAdapter adapter = (ElementIndexAdapter) EcoreUtil.getExistingAdapter(object, ElementIndexAdapter.class);
        if(adapter == null) {
            adapter = new ElementIndexAdapter();
            object.eAdapters().add(0, adapter);
        } else {
            object.eAdapters().remove(adapter);
            equip(object);
        }
    }

    static public ElementIndexAdapter getAdapter(EObject obj) {
        return (ElementIndexAdapter) EcoreUtil.getExistingAdapter(obj, ElementIndexAdapter.class);
    }
}
