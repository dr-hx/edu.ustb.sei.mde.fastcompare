package edu.ustb.sei.mde.fastcompare.scope;

import java.util.Set;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.URI;


import com.google.common.collect.Sets;

public abstract class AbstractComparisonScope extends AdapterImpl implements IComparisonScope {
   protected Notifier left;
   protected Notifier right;
   protected Notifier origin;
   protected Set<String> nsURIs;
   protected Set<String> resourceURIs;
   protected Set<URI> allInvolvedResourceURIs;

   public AbstractComparisonScope(Notifier left, Notifier right, Notifier origin) {
      this.left = left;
      this.right = right;
      this.origin = origin;
      this.resourceURIs = Sets.newHashSet();
      this.nsURIs = Sets.newHashSet();
      this.allInvolvedResourceURIs = Sets.newHashSet();
    //   this.diagnostic = new BasicDiagnostic(0, "org.eclipse.emf.compare", 0, (String)null, new Object[]{this});
   }

   public Notifier getLeft() {
      return this.left;
   }

   public Notifier getRight() {
      return this.right;
   }

   public Notifier getOrigin() {
      return this.origin;
   }

   public Set<String> getNsURIs() {
      return this.nsURIs;
   }

   public Set<String> getResourceURIs() {
      return this.resourceURIs;
   }

//    public Diagnostic getDiagnostic() {
//       return this.diagnostic;
//    }

//    public void setDiagnostic(Diagnostic diagnostic) {
//       this.diagnostic = diagnostic;
//    }

   public Set<URI> getAllInvolvedResourceURIs() {
      return this.allInvolvedResourceURIs;
   }

   public boolean isAdapterForType(Object type) {
      return type != IComparisonScope.class ? super.isAdapterForType(type) : true;
   }
}
