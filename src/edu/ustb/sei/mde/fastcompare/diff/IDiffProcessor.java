package edu.ustb.sei.mde.fastcompare.diff;

import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.DifferenceSource;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.MatchResource;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

import edu.ustb.sei.mde.fastcompare.config.FeatureConfigure;

public interface IDiffProcessor {
	/**
	 * This will be called whenever the Diff engine detected a difference for a
	 * given reference value.
	 * 
	 * @param match
	 *                  The match to which this difference should be attached.
	 * @param reference
	 *                  The reference on which we detected a difference.
	 * @param value
	 *                  The actual value for which we detected a difference.
	 * @param kind
	 *                  Kind of the detected difference.
	 * @param source
	 *                  Source of the detected difference. For two way comparisons,
	 *                  this will always be
	 *                  {@link DifferenceSource#LEFT}. Otherwise, this will indicate
	 *                  the side on which this
	 *                  difference has been detected.
	 */
	void referenceChange(Match match, EReference reference, FeatureConfigure configure, EObject value, DifferenceKind kind,
			DifferenceSource source);

	/**
	 * This will be called whenever the diff engine detected a difference for a
	 * given attribute value.
	 * 
	 * @param match
	 *                  The match to which this difference should be attached.
	 * @param attribute
	 *                  The attribute on which we detected a difference.
	 * @param value
	 *                  The actual value for which we detected a difference.
	 * @param kind
	 *                  Kind of the difference.
	 * @param source
	 *                  Source of the difference. For two way comparisons, this will
	 *                  always be
	 *                  {@link DifferenceSource#LEFT}. Otherwise, this will indicate
	 *                  the side on which this
	 *                  difference has been detected.
	 */
	void attributeChange(Match match, EAttribute attribute, FeatureConfigure configure, Object value, DifferenceKind kind,
			DifferenceSource source);

	/**
	 * This will be called whenever the diff engine detected a difference for a
	 * given attribute value.
	 * 
	 * @param match
	 *                  The match to which this difference should be attached.
	 * @param attribute
	 *                  The EFeatureMapEntry attribute on which we detected a
	 *                  difference.
	 * @param value
	 *                  The actual FeatureMap.Entry value for which we detected a
	 *                  difference.
	 * @param kind
	 *                  Kind of the difference.
	 * @param source
	 *                  Source of the difference. For two way comparisons, this will
	 *                  always be
	 *                  {@link DifferenceSource#LEFT}. Otherwise, this will indicate
	 *                  the side on which this
	 *                  difference has been detected.
	 * @since 3.2
	 */
	void featureMapChange(Match match, EAttribute attribute, FeatureConfigure configure, Object value, DifferenceKind kind,
			DifferenceSource source);

	/**
	 * This will be called whenever the diff engine detected the addition (or
	 * removal) of a root in a given
	 * Resource.
	 * 
	 * @param match
	 *               The match to which this difference should be attached.
	 * @param uri
	 *               The uri of the resource we attached this element to in case of
	 *               an {@link DifferenceKind#ADD}
	 *               , or the uri of the resource it was attached to in case of a
	 *               {@link DifferenceKind#DELETE}
	 *               difference.
	 * @param kind
	 *               Kind of this difference.
	 * @param source
	 *               Source of the difference. For two way comparisons, this will
	 *               always be
	 *               {@link DifferenceSource#LEFT}. Otherwise, this will indicate
	 *               the side on which this
	 *               difference has been detected.
	 */
	void resourceAttachmentChange(Match match, String uri, DifferenceKind kind, DifferenceSource source);

	/**
	 * This will be called whenever the diff engine detects the change of a Resource
	 * location.
	 * 
	 * @param matchResource
	 *                        The matchResource to which this difference should be
	 *                        attached.
	 * @param baseLocation
	 *                        The base location (left side in case of a two way
	 *                        comparison, origin or left side in case of
	 *                        a three way comparison).
	 * @param changedLocation
	 *                        The change location (right side in case of a two way
	 *                        comparison, left or right side in case
	 *                        of a three way comparison).
	 * @param kind
	 *                        Kind of this difference.
	 * @param source
	 *                        Source of the difference. For two way comparisons,
	 *                        this will always be
	 *                        {@link DifferenceSource#LEFT}. Otherwise, this will
	 *                        indicate the side on which this
	 *                        difference has been detected.
	 */
	void resourceLocationChange(MatchResource matchResource, String baseLocation, String changedLocation,
			DifferenceKind kind, DifferenceSource source);
}
