package edu.ustb.sei.mde.fastcompare.match;

import java.lang.reflect.Array;
import java.util.function.Function;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.internal.spec.MatchSpec;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.FeatureMap;

import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.match.EqualityHelperExtension.SpecificMatch;
import edu.ustb.sei.mde.fastcompare.match.eobject.IEqualityHelper;
import edu.ustb.sei.mde.fastcompare.utils.AutoLRUCache;

public class EqualityHelper extends AdapterImpl implements IEqualityHelper {
	/** A cache keeping track of the URIs for EObjects. */
	private final AutoLRUCache<EObject, URI> uriCache;

	/** The cached {@link #getTarget() target}. */
	private Comparison comparision;

	/** The record of the most recently used {@link #matchingEObjects(EObject, EObject) match}. */
	private MatchSpec eObjectMatch;

	private MatcherConfigure matcherConfigure;

	/**
	 * Creates a new EqualityHelper with the given cache.
	 * 
	 * @param uriCache
	 *            the cache to be used for {@link EcoreUtil#getURI(EObject)} calls.
	 */
	public EqualityHelper(AutoLRUCache<EObject, URI> uriCache) {
		this.uriCache = uriCache;
	}

	public void setMatcherConfigure(MatcherConfigure matcherConfigure) {
		this.matcherConfigure = matcherConfigure;
	}

	/**
	 * Creates a new EqualityHelper with the given cache and registry.
	 * 
	 * @param uriCache
	 *            the cache to be used for {@link EcoreUtil#getURI(EObject)} calls.
	 * @param equalityHelperExtensionProviderRegistry
	 *            Registry ofequality helper extension provider
	 */
	public EqualityHelper(AutoLRUCache<EObject, URI> uriCache,
			MatcherConfigure matcherConfigure) {
		this.uriCache = uriCache;
		this.matcherConfigure = matcherConfigure;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.utils.IEqualityHelper#matchingValues(java.lang.Object, java.lang.Object)
	 */
	public boolean matchingValues(Object object1, Object object2) {
		final boolean equal;
		// This method is generally called O(n^2) times so for large samples it accounts for as much of 15% of
		// the time spent merging. Anything we can do to make this method faster will have a significant
		// performance impact.
		if (object1 == object2) {
			equal = true;
		} else if (object1 == null) {
			// Special case, consider that the empty String is equal to null (unset attributes)
			equal = "".equals(object2); //$NON-NLS-1$
		} else if (object2 == null) {
			// Special case, consider that the empty String is equal to null (unset attributes)
			equal = "".equals(object1); //$NON-NLS-1$
		} else {
			// Here we use cached information about the most recently used Match for matching two EObjects.
			// We can use that information cheaply (only == tests are involved) at the very start of
			// the matching, to avoid the cost instanceof checking and casting, which can account for as much
			// as 1/3 of the overall cost.
			MatchSpec currentEObjectMatch = eObjectMatch;
			if (currentEObjectMatch != null && currentEObjectMatch.matches(object1)) {
				equal = currentEObjectMatch.matches(object2);
			} else if (object1 instanceof EObject) {
				if (object2 instanceof EObject) {
					equal = matchingEObjects((EObject)object1, (EObject)object2);
				} else {
					equal = false;
				}
			} else if (object1 instanceof String || object1 instanceof Integer
					|| object1 instanceof Boolean) {
				// primitives and String are much more common than arrays... and isArray() is expensive.
				equal = object1.equals(object2);
			} else if (object1.getClass().isArray() && object2.getClass().isArray()) {
				// [299641] compare arrays by their content instead of instance equality
				equal = matchingArrays(object1, object2);
			} else if (object1 instanceof FeatureMap.Entry && object2 instanceof FeatureMap.Entry) {
				FeatureMap.Entry featureMapEntry1 = (FeatureMap.Entry)object1;
				EStructuralFeature key1 = featureMapEntry1.getEStructuralFeature();
				FeatureMap.Entry featureMapEntry2 = (FeatureMap.Entry)object2;
				EStructuralFeature key2 = featureMapEntry2.getEStructuralFeature();
				if (key1.equals(key2)) {
					Object value1 = featureMapEntry1.getValue();
					Object value2 = featureMapEntry2.getValue();
					equal = matchingValues(value1, value2);
				} else {
					equal = false;
				}
			} else {
				equal = object1.equals(object2);
			}
		}
		return equal;
	}

	/**
	 * Compares two values as EObjects, using their Match if it can be found, comparing through their URIs
	 * otherwise.
	 * 
	 * @param object1
	 *            First of the two objects to compare here.
	 * @param object2
	 *            Second of the two objects to compare here.
	 * @return <code>true</code> if these two EObjects are to be considered equal, <code>false</code>
	 *         otherwise.
	 */
	protected boolean matchingEObjects(EObject object1, EObject object2) {
		final boolean matching;
		MatchSpec match = (MatchSpec)getMatch(object1);

		if (match != null) {
			if (match.getLeft() == object2 || match.getRight() == object2 || match.getOrigin() == object2) {
				return true;
			}
		}

		// Call to specific matcher if one was provided
		if (matcherConfigure != null && object1 != null && object2 != null) {
			EqualityHelperExtension equalityHelperExtensionProvider = matcherConfigure.getEqualityHelperExtension();

			if (equalityHelperExtensionProvider != null) {
				SpecificMatch specificMatch = equalityHelperExtensionProvider.matchingEObjects(object1,
						object2, this);
				if (specificMatch != null) {
					switch (specificMatch) {
						case MATCH:
							return true;
						case UNMATCH:
							return false;
						case UNKNOWN:
							// Fall through
						default:
							break;
					}
				}
			}
		}

		// Match could be null if the value is out of the scope
		if (match != null) {
			eObjectMatch = match;
			matching = match.matches(object2);
		} else if (getTarget().getMatch(object2) != null || object1.eClass() != object2.eClass()) {
			matching = false;
		} else {
			matching = matchingURIs(object1, object2);
		}
		return matching;
	}

	/**
	 * Returns the match of this EObject if any, <code>null</code> otherwise.
	 * 
	 * @param o
	 *            The object for which we need the associated Match.
	 * @return Match of this EObject if any, <code>null</code> otherwise.
	 */
	protected Match getMatch(EObject o) {
		return getTarget().getMatch(o);
	}

	/**
	 * Compare the URIs (of similar concept) of EObjects.
	 * 
	 * @param object1
	 *            First of the two objects to compare here.
	 * @param object2
	 *            Second of the two objects to compare here.
	 * @return <code>true</code> if these two EObjects have the same URIs, <code>false</code> otherwise.
	 */
	protected boolean matchingURIs(EObject object1, EObject object2) {
		// An object that is uncontained and is not a proxy has no URI. bypass them.
		if (!object1.eIsProxy() && isUncontained(object1) || !object2.eIsProxy() && isUncontained(object2)) {
			return false;
		}

		final boolean equal;
		final URI uri1 = uriCache.get(object1);
		final URI uri2 = uriCache.get(object2);
		if (uri1.hasFragment() && uri2.hasFragment()) {
			final String uri1Fragment = removeURIAttachment(uri1.fragment());
			final String uri2Fragment = removeURIAttachment(uri2.fragment());
			equal = uri1Fragment.equals(uri2Fragment);
		} else {
			equal = uri1.equals(uri2);
		}
		return equal;
	}

	/**
	 * To some {@link URI}s a human friendly description is attached describing the type the {@link URI} is
	 * pointing to. The description is marked by a "?" at the beginning and end. This method returns the
	 * fragment without the attached type description.
	 * 
	 * @param fragment
	 *            The {@link URI} fragment to check for a type description attachment
	 * @return The fragment of the {@link URI} stripped from type description if it has one, otherwise the
	 *         original fragment is returned.
	 */
	private String removeURIAttachment(String fragment) {
		// check if fragment contains at least two question marks
		final int questionMark1 = fragment.indexOf('?');
		final boolean hasTwoQuestionMarks = questionMark1 != -1
				&& fragment.indexOf('?', questionMark1 + 1) != -1;
		if (hasTwoQuestionMarks) {
			return fragment.substring(0, questionMark1);
		}
		return fragment;
	}

	/**
	 * Checks whether the given object is contained anywhere.
	 * 
	 * @param object
	 *            The object whose container we are to check.
	 * @return <code>true</code> if the object has no reachable container, <code>false</code> otherwise.
	 */
	private boolean isUncontained(EObject object) {
		return object.eContainer() == null && object.eResource() == null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.utils.IEqualityHelper#matchingAttributeValues(java.lang.Object,
	 *      java.lang.Object)
	 */

	public boolean matchingAttributeValues(Object object1, Object object2) {
		// The default equality helper handles attributes and references the same.
		return matchingValues(object1, object2);
	}

	/**
	 * Compares two values as arrays, checking that their length and content match each other.
	 * 
	 * @param object1
	 *            First of the two objects to compare here.
	 * @param object2
	 *            Second of the two objects to compare here.
	 * @return <code>true</code> if these two arrays are to be considered equal, <code>false</code> otherwise.
	 */
	private boolean matchingArrays(Object object1, Object object2) {
		boolean equal = true;
		final int length1 = Array.getLength(object1);
		if (length1 != Array.getLength(object2)) {
			equal = false;
		} else {
			for (int i = 0; i < length1 && equal; i++) {
				final Object element1 = Array.get(object1, i);
				final Object element2 = Array.get(object2, i);
				equal = matchingValues(element1, element2);
			}
		}
		return equal;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.common.notify.impl.AdapterImpl#isAdapterForType(java.lang.Object)
	 */
	@Override
	public boolean isAdapterForType(Object type) {
		return type == IEqualityHelper.class;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.common.notify.impl.AdapterImpl#getTarget()
	 */
	@Override
	public Comparison getTarget() {
		return comparision;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.common.notify.impl.AdapterImpl#setTarget(Notifier)
	 */
	@Override
	public void setTarget(Notifier newTarget) {
		comparision = (Comparison) newTarget;
		super.setTarget(newTarget);
	}

	// /**
	//  * The EqualityHelper often needs to get an EObject uri. As such it has an internal cache that clients
	//  * might leverage through this method.
	//  * 
	//  * @param object
	//  *            any EObject.
	//  * @return the URI of the given EObject, or {@code null} if we somehow could not compute it.
	//  */
	// @Deprecated
	// public URI getURI(EObject object) {
	// 	try {
	// 		return uriCache.get(object);
	// 	} catch (ExecutionException e) {
	// 		return null;
	// 	}
	// }

	// /**
	//  * Returns the cache used by this object.
	//  * 
	//  * @return the cache used by this object.
	//  */
	// @Deprecated
	// public Cache<EObject, URI> getCache() {
	// 	return uriCache;
	// }

	/**
	 * Create a cache as required by EqualityHelper.
	 * 
	 * @param cacheBuilder
	 *            The builder to use to instantiate the cache.
	 * @return the new cache.
	 */
	public static AutoLRUCache<EObject, URI> createDefaultCache(int minSize) {
		return new AutoLRUCache<EObject, URI>(new URICacheFunction(), minSize * 8, minSize, 0.75f);
	}

	/**
	 * This is the function that will be used by our {@link #uriCache} to compute its values.
	 * 
	 * @author <a href="mailto:laurent.goubet@obeo.fr">Laurent Goubet</a>
	 */
	private static class URICacheFunction implements Function<EObject, URI> {
		/**
		 * {@inheritDoc}
		 * 
		 * @see com.google.common.base.Function#apply(java.lang.Object)
		 */
		public URI apply(EObject input) {
			if (input == null) {
				return null;
			}
			return EcoreUtil.getURI(input);
		}
	}

	static public final int DEFAULT_EOBJECT_URI_CACHE_MAX_SIZE = 1024;

	static public IEqualityHelper getEqualityHelper(Comparison comparison) {
		IEqualityHelper ret = (IEqualityHelper)EcoreUtil.getExistingAdapter(comparison, IEqualityHelper.class);
		if (ret == null) {
			ret = new EqualityHelper(EqualityHelper.createDefaultCache(DEFAULT_EOBJECT_URI_CACHE_MAX_SIZE));
			comparison.eAdapters().add(ret);
			ret.setTarget(comparison);
		}
		return ret;
	}
}