package edu.ustb.sei.mde.fastcompare.utils;
import java.util.Iterator;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;

public class TreesIterator implements Iterator<EObject> {
    private Iterator<? extends EObject> rootIterator;
    private Iterator<EObject> subtreeIterator;

    class TreeIteratorWithRoot implements Iterator<EObject> {
        private EObject root;
        private TreeIterator<? extends EObject> iterator;

        TreeIteratorWithRoot(EObject root) {
            this.root = root;
            this.iterator = null;
        }
        @Override
        public boolean hasNext() {
           if(iterator != null) return iterator.hasNext();
           else return root != null;
        }

        @Override
        public EObject next() {
           if(iterator != null) return iterator.next();
           else {
              if(root != null) {
                  iterator = root.eAllContents();
                  return root;
              } else {
                  return null;
              }
           }
        }
        
    }

    public TreesIterator(Iterable<? extends EObject> roots) {
        rootIterator = roots.iterator();
        subtreeIterator = null;
    }

    @Override
    public boolean hasNext() {
        while(true) {
            if(subtreeIterator == null) {
                if(rootIterator.hasNext()) {
                    EObject root = rootIterator.next();
                    subtreeIterator = new TreeIteratorWithRoot(root);
                } else return false;
            }
            if(subtreeIterator.hasNext()) return true;
            else subtreeIterator = null;
        }
    }

    @Override
    public EObject next() {
        return subtreeIterator.next();
    }
    
}
