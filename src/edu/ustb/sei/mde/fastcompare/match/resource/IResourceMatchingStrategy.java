package edu.ustb.sei.mde.fastcompare.match.resource;

import java.util.List;

import org.eclipse.emf.compare.MatchResource;
import org.eclipse.emf.ecore.resource.Resource;

public interface IResourceMatchingStrategy {
    	List<MatchResource> matchResources(Iterable<? extends Resource> left, Iterable<? extends Resource> right,
			Iterable<? extends Resource> origin);
}
