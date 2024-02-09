package edu.ustb.sei.mde.fastcompare.index;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;

public abstract class ElementIndexAdapterFactory {
    public ElementIndexAdapter equip(EObject object) {
        ElementIndexAdapter adapter = (ElementIndexAdapter) EcoreUtil.getExistingAdapter(object, ElementIndexAdapter.class);
        if(adapter == null) {
            adapter = createAdapter(totalNumbers ++);
            object.eAdapters().add(0, adapter);
        } else {
            adapter.init();
        }
        return adapter;
    }

    protected abstract ElementIndexAdapter createAdapter(int id);

    protected int totalNumbers = 0;

    public ElementIndexAdapter getAdapter(EObject obj) {
        return ElementIndexAdapter.getAdapter(obj);
    }

    protected void clearAdapter(EObject o) {
        ElementIndexAdapter adapter = getAdapter(o);
        if(adapter != null)
            o.eAdapters().remove(adapter);
    }

    public void removeAdapters(Collection<EObject> roots) {
        for(EObject eObj : roots) {
            clearAdapter(eObj);
            TreeIterator<EObject> iter = eObj.eAllContents();
            while(iter.hasNext()) {
                EObject next = iter.next();
                clearAdapter(next);
            }
        }
    }

    public void removeAdapters(Notifier notifier) {
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
