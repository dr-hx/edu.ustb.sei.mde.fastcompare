package edu.ustb.sei.mde.fastcompare.index;

import java.util.zip.CRC32;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import edu.ustb.sei.mde.fastcompare.shash.HashLoHi;
import edu.ustb.sei.mde.fastcompare.shash.StringHashFunction;
import edu.ustb.sei.mde.fastcompare.utils.CommonUtils;

public class ElementIndexAdapterWithStructuralChecksum extends ElementIndexAdapter {

    public ElementIndexAdapterWithStructuralChecksum(int id) {
        super(id);
    }

    private long treeStructuralChecksum = ElementIndexAdapter.INVALID_IHASH;
    public int size = 1;
    public HashLoHi treeSimHash = null;

    static private StringHashFunction stringHash = new StringHashFunction();

    private long getChildrenSimhash() {
        return treeSimHash.high;
    }

    public long getTreeStructuralChecksum() {
        if(treeStructuralChecksum == ElementIndexAdapter.INVALID_IHASH) {
            EObject object = (EObject) this.getTarget();
            long high = buildLocalSimhash(object);
            long low = 0L;
            CRC32 crc32 = new CRC32();
            CommonUtils.update(crc32, object.eClass().getName());
            CommonUtils.update(crc32, "{");
            for(EObject child : object.eContents()) {
                ElementIndexAdapterWithStructuralChecksum childAdapter = (ElementIndexAdapterWithStructuralChecksum) ElementIndexAdapter.getAdapter(child);
                if(childAdapter != null) {
                    long childTreeHash = childAdapter.getTreeStructuralChecksum();
                    if(childTreeHash != INVALID_IHASH) {
                        CommonUtils.update(crc32, childTreeHash);
                    }
                    size += childAdapter.size;
                    low |= childAdapter.getChildrenSimhash();
                }
            }
            CommonUtils.update(crc32, "}");
            treeStructuralChecksum = crc32.getValue();
            treeSimHash = new HashLoHi(high, low);
        }

        return treeStructuralChecksum;
    }

    private long buildLocalSimhash(EObject o) {
        long hash = 0L;
        for(EStructuralFeature feature : o.eClass().getEAllAttributes()) {
            if(feature.isMany()==false && feature.getEType().getInstanceClass()==String.class) {
                String value = (String) o.eGet(feature);
                if(value != null)
                    hash |= stringHash.hash(null, value);
            }
        }
        return hash;
    }
    
}
