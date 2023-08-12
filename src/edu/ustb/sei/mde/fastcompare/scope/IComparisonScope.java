package edu.ustb.sei.mde.fastcompare.scope;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

public interface IComparisonScope {
    Notifier getLeft();

    Notifier getRight();

    Notifier getOrigin();

    Iterator<? extends Resource> getCoveredResources(ResourceSet var1);

    Iterator<? extends EObject> getCoveredEObjects(Resource var1);

    Iterator<? extends EObject> getChildren(EObject var1);

    Set<String> getNsURIs();

    Set<String> getResourceURIs();

    Set<URI> getAllInvolvedResourceURIs();
}
