package edu.ustb.sei.mde.fastcompare.match;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.compare.CompareFactory;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.DifferenceSource;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.MatchResource;
import org.eclipse.emf.compare.internal.spec.ComparisonSpec;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.FeatureMap;

import org.eclipse.emf.ecore.util.InternalEList;

import com.google.common.collect.Sets;

import edu.ustb.sei.mde.fastcompare.config.ClassConfigure;
import edu.ustb.sei.mde.fastcompare.config.FeatureConfigure;
import edu.ustb.sei.mde.fastcompare.config.FeatureFilter;
import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.diff.DefaultDiffEngine;
import edu.ustb.sei.mde.fastcompare.diff.IDiffProcessor;
import edu.ustb.sei.mde.fastcompare.utils.AccessBasedLRUCache;

import edu.ustb.sei.mde.fastcompare.utils.DiffUtil;
import edu.ustb.sei.mde.fastcompare.utils.ReferenceUtil;
import edu.ustb.sei.mde.fastcompare.utils.URIComputer;

public class EditionDistance implements DistanceFunction {

	/**
	 * This class is an implementation of a {@link IDiffProcessor} which counts the number of differences to
	 * given an overall distance between two objects.
	 */
	class CountingDiffProcessor implements IDiffProcessor {
		/**
		 * Keeps track of features which have already been detected as changed so that we can apply different
		 * weight in those cases.
		 */
		private Set<EStructuralFeature> alreadyChanged = Sets.newLinkedHashSet();

		/**
		 * The current distance.
		 */
		private double distance;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void referenceChange(Match match, EReference reference, FeatureConfigure configure, EObject value, DifferenceKind kind,
				DifferenceSource source) {
			if (!alreadyChanged.contains(reference)) {
				EClassifier eType = reference.getEType();
				if (eType != null) { // Do not update distance in case of untyped reference
					switch (kind) {
						case MOVE:
							distance += configure.getWeight() * orderChangeCoef;
							break;
						case ADD:
						case DELETE:
						case CHANGE:
							distance += configure.getWeight();
							break;
						default:
							break;
					}
				}
				alreadyChanged.add(reference);
			} else {
				distance += 1;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void attributeChange(Match match, EAttribute attribute, FeatureConfigure configure, Object value, DifferenceKind kind,
				DifferenceSource source) {
			if (!alreadyChanged.contains(attribute)) {
				EClassifier eType = attribute.getEType();
				if (eType != null) { // Do not update distance in case of untyped attribute
					Object aValue = ReferenceUtil.safeEGet(match.getLeft(), attribute);
					Object bValue = ReferenceUtil.safeEGet(match.getRight(), attribute);
					switch (kind) {
						case MOVE:
							distance += configure.getWeight() * orderChangeCoef;
							break;
						case ADD:
						case DELETE:
						case CHANGE:
							if (aValue instanceof String && bValue instanceof String) {
								distance += configure.getWeight() * (1 - DiffUtil.diceCoefficient((String)aValue, (String)bValue));
							} else {
								distance += configure.getWeight();
							}
							break;
						default:
							break;
					}
				}
				alreadyChanged.add(attribute);
			} else {
				distance += 1;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void featureMapChange(Match match, EAttribute attribute, FeatureConfigure configure, Object value, DifferenceKind kind,
				DifferenceSource source) {
			if (!alreadyChanged.contains(attribute)) {
				EClassifier eType = attribute.getEType();
				if (eType != null) { // Do not update distance in case of untyped attribute
					Object aValue = ReferenceUtil.safeEGet(match.getLeft(), attribute);
					Object bValue = ReferenceUtil.safeEGet(match.getRight(), attribute);
					switch (kind) {
						case MOVE:
							distance += configure.getWeight() * orderChangeCoef;
							break;
						case ADD:
						case DELETE:
						case CHANGE:
							if (aValue instanceof String && bValue instanceof String) {
								distance += configure.getWeight() * (1 - DiffUtil.diceCoefficient((String)aValue, (String)bValue));
							} else {
								distance += configure.getWeight() ;
							}
							break;
						default:
							break;
					}
				}
				alreadyChanged.add(attribute);
			} else {
				distance += 1;
			}
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diff.IDiffProcessor#resourceAttachmentChange(org.eclipse.emf.compare.Match,
		 *      java.lang.String, org.eclipse.emf.compare.DifferenceKind,
		 *      org.eclipse.emf.compare.DifferenceSource)
		 */
		@Override
		public void resourceAttachmentChange(Match match, String uri, DifferenceKind kind,
				DifferenceSource source) {
			// Not important for the distance computation
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diff.IDiffProcessor#resourceLocationChange(org.eclipse.emf.compare.
		 *      MatchResource, java.lang.String, java.lang.String org.eclipse.emf.compare.DifferenceKind,
		 *      org.eclipse.emf.compare.DifferenceSource)
		 */
		@Override
		public void resourceLocationChange(MatchResource matchResource, String baseLocation,
				String changedLocation, DifferenceKind kind, DifferenceSource source) {
			// Not important for the distance computation
		}

		/**
		 * return the computed distance.
		 * 
		 * @return the computed distance.
		 */
		public double getComputedDistance() {
			return distance;
		}

		/**
		 * Clear the diff processor state so that it's ready for the next computation.
		 */
		public void reset() {
			this.alreadyChanged.clear();
		}

	}

	/**
     * An implementation of a diff engine which count and measure the detected
     * changes.
     */
    class CountingDiffEngine extends DefaultDiffEngine {
        /**
         * The maximum distance until which we just have to stop.
         */
        private double maxDistance;

        /** The comparison for which this engine will detect differences. */
        private final Comparison comparison;

        // final private MatcherConfigure matcherConfigure;

        /**
         * Create the diff engine.
         * 
         * @param maxDistance
         *                       the maximum distance we might reach.
         * @param fakeComparison
         *                       the comparison instance to use while measuring the
         *                       differences between the two objects.
         */
        CountingDiffEngine(double maxDistance, Comparison fakeComparison, MatcherConfigure matcherConfigure) {
            super(new CountingDiffProcessor(), matcherConfigure);
            // this.matcherConfigure = matcherConfigure;
            this.maxDistance = maxDistance;
            // will always return the same instance.

            this.comparison = fakeComparison;

        }

        /**
         * Measure the difference between two objects and return a distance value.
         * 
         * @param comparisonInProgress
         *                             the comparison which is currently being matched.
         * @param a
         *                             first object.
         * @param b
         *                             second object.
         * @return the distance between them computed using the number of changes
         *         required to change a to b.
         */
        public double measureDifferences(Comparison comparisonInProgress, EObject a, EObject b) {
			EClass clazz = a.eClass();
			ClassConfigure configure = matcherConfigure.getClassConfigure(clazz);

            Match fakeMatch = createOrUpdateFakeMatch(a, b);
            getCounter().reset();
            double changes = 0;
            if (!haveSameContainer(comparisonInProgress, a, b)) {
                changes += locationChangeCoef * configure.getParentWeight(a);
            } else {
                int aIndex = getContainmentIndex(a);
                int bIndex = getContainmentIndex(b);
                if (aIndex != bIndex) {
                    /*
                     * we just want to pick the same positioned object if two exactly similar
                     * objects are
                     * candidates in the same container.
                     */
                    changes += 5;
                }

            }
            if (a.eContainingFeature() != b.eContainingFeature()) {
                changes += Math.max(configure.getContainingFeatureWeight(a), configure.getContainingFeatureWeight(b));
            }
            if (changes <= maxDistance) {
                checkForDifferences(fakeMatch);
                changes += getCounter().getComputedDistance();
            }
            return changes;

        }

        @Override
        protected void checkResourceAttachment(Match match) {
            /*
             * we really don't want to check that...
             */
        }

        @Override
        protected void computeDifferences(Match match, EAttribute attribute, FeatureConfigure configure, boolean checkOrdering) {
            if (getCounter().getComputedDistance() <= maxDistance) {
				if (attribute.isMany()) {
					computeMultiValuedFeatureDifferencesTwoWay(match, attribute, configure, checkOrdering);
				} else {
					computeSingleValuedAttributeDifferences(match, attribute, configure);
				}
            }
        }

        @Override
        protected void computeDifferences(Match match, EReference reference, FeatureConfigure configure, boolean checkOrdering) {
            if (getCounter().getComputedDistance() <= maxDistance) {
                if (reference.isContainment()) {
					computeContainmentDifferences(match, reference, configure, checkOrdering);
				} else if (reference.isMany()) {
					computeMultiValuedFeatureDifferences(match, reference, configure, checkOrdering);
				} else {
					computeSingleValuedReferenceDifferencesTwoWay(match, reference, configure);
				}
            }
        }

        protected CountingDiffProcessor getCounter() {
            return (CountingDiffProcessor) getDiffProcessor();
        }

        protected FeatureFilter createFeatureFilter() {
            return new FeatureFilter(matcherConfigure) {
                @Override
                public List<Entry<EStructuralFeature, FeatureConfigure>> getFeaturesToCheck(EClass clazz) {
                    List<Entry<EStructuralFeature, FeatureConfigure>> curList = this.featuresToCheckCache.get(clazz);
                    if (curList == null) {
                        curList = super.getFeaturesToCheck(clazz);
                        int offset = 0;
                        int size = curList.size();
                        for (int i = 0; i < size; i++) {
                            Entry<EStructuralFeature, FeatureConfigure> pair = curList.get(i);
                            if (pair.getValue().getWeight() == 0) {
                                offset++;
                            } else {
                                curList.set(i - offset, pair);
                            }
                        }
                        for (int i = 1; i <= offset; i++) {
                            curList.remove(size - i);
                        }
                    }
                    return super.getFeaturesToCheck(clazz);
                }
            };
        }

        /**
         * return the position in which an Object is contained in its parent list.
         * 
         * @param a
         *          any EObject
         * @return the position in which an Object is contained in its parent list, 0 if
         *         there is no container
         *         or if the reference is single valued.
         */
        private int getContainmentIndex(EObject a) {
            EStructuralFeature feat = a.eContainingFeature();
            EObject container = a.eContainer();
            int position = 0;
            if (container != null) {
                if (feat instanceof EAttribute) {
                    position = indexFromFeatureMap(a, feat, container);
                } else if (feat != null) {
                    if (feat.isMany()) {
                        EList<?> eList = (EList<?>) ReferenceUtil.safeEGet(container, feat);
                        position = eList.indexOf(a);
                    }
                }
            }
            return position;
        }

        /**
         * the position of the {@link EObject} a in its container featureMap.
         * 
         * @param a
         *                  the {@link EObject}.
         * @param feat
         *                  the containing feature.
         * @param container
         *                  the containing EObject.
         * @return the position of the {@link EObject} a in its container featureMap.
         */
        private int indexFromFeatureMap(EObject a, EStructuralFeature feat, EObject container) {
            FeatureMap featureMap = (FeatureMap) ReferenceUtil.safeEGet(container, feat);
            for (int i = 0, size = featureMap.size(); i < size; ++i) {
                if (featureMap.getValue(i) == a) {
                    EStructuralFeature entryFeature = featureMap.getEStructuralFeature(i);
                    if (DiffUtil.isContainmentReference(entryFeature)) {
                        return i;
                    }
                }
            }
            return 0;
        }

        /**
         * Check whether two {@link EObject} have the same containers or not.
         * 
         * @param inProgress
         *                   the comparison currently being matched.
         * @param a
         *                   any {@link EObject}
         * @param b
         *                   any other {@link EObject}
         * @return true if they have the same container. If the containers have been
         *         matched their match will
         *         be used, on the contrary the URI will be indirectly use through the
         *         EqualityHelper.
         */
        private boolean haveSameContainer(Comparison inProgress, EObject a, EObject b) {
            EObject aContainer = a.eContainer();
            EObject bContainer = b.eContainer();
            if ((aContainer == null && bContainer != null) || (aContainer != null && bContainer == null)) {
                return false;
            }
            /*
             * we consider two null containers as being the "same".
             */
            boolean matching = aContainer == null && bContainer == null;
            if (!matching) {
                Match mA = inProgress.getMatch(aContainer);
                Match mB = inProgress.getMatch(bContainer);
                if (mA == null && mB == null) {
                    /*
                     * The Objects have to be out of scope then.
                     */
                    matching = fakeComparison.getEqualityHelper().matchingValues(aContainer, bContainer);
                } else {
                    matching = isReferencedByTheMatch(bContainer, mA)
                            || isReferencedByTheMatch(aContainer, mB);
                }
            }
            return matching;
        }

        /**
         * Return true if the given {@link EObject} is referenced by the left/right or
         * origin match reference.
         * 
         * @param eObj
         *              any {@link EObject}.
         * @param match
         *              any Match.
         * @return true if the given {@link EObject} is referenced by the left/right or
         *         origin match
         *         reference.
         */
        private boolean isReferencedByTheMatch(EObject eObj, Match match) {
            return match != null
                    && (match.getRight() == eObj || match.getLeft() == eObj || match.getOrigin() == eObj);
        }

        /**
         * Create a mock {@link Match} between the two given EObjects so that we can use
         * the exposed
         * {@link #checkForDifferences(Match, org.eclipse.emf.common.util.Monitor)}
         * method to check for
         * differences.
         * 
         * @param a
         *          First of the two EObjects for which we want to force a comparison.
         * @param b
         *          Second of the two EObjects for which we want to force a comparison.
         * @return The created Match.
         */
        private Match createOrUpdateFakeMatch(EObject a, EObject b) {
            Match fakeMatch = comparison.getMatches().get(0);
            fakeMatch.setLeft(a);
            fakeMatch.setRight(b);
            return fakeMatch;
        }

    }

	/**
	 * Weight coefficient of a change of location (uri).
	 */
	private int locationChangeCoef = 1;

    /**
	 * Weight coefficient of a change of order within a reference.
	 */
	private int orderChangeCoef = 1;

	/**
	 * The instance used to compare location of EObjects.
	 */
	final private URIDistance uriDistance;

	/**
	 * a thresholds ratio discrete function per the number of features.
	 */
	// CHECKSTYLE:OFF we know these are magic numbers, so be it, they happens to have the same value but there
	// is no semantic.
	private double[] thresholds = {0d, 0.6d, 0.6d, 0.55d, 0.465d };

    // CHECKSTYLE:ON
	/**
	 * The fake comparison is used to make the diff engine super class happy. We are reusing the same instance
	 * which we are updating because of the cost of adding even a single Match in it (and subsequent growing
	 * of list) which gets very significant considering how much we are calling this during a single
	 * comparison.
	 */
	private Comparison fakeComparison;

	private MatcherConfigure matcherConfigure;

	private AccessBasedLRUCache<EObject, Double> thresholdAmountCache;

    /**
	 * Instantiate a new Edition Distance.
	 * 
	 * @param weightProviderRegistry
	 *            The registry of weight providers to use in this Edition Distance.
	 * @param equalityHelperExtensionProviderRegistry
	 *            The registry of equality helper extension providers to use in this Edition Distance.
	 */
	public EditionDistance(MatcherConfigure matcherConfigure) {
        this.matcherConfigure = matcherConfigure;
		
		URIComputer uriComputer = this.matcherConfigure.getUriComputer();
		this.uriDistance = new URIDistance(uriComputer);

		this.fakeComparison = new ComparisonSpec() {
			/*
			 * We did override this method to avoid the extra cost of maintaining a cross referencer for such
			 * a fake comparison.
			 */
			@Override
			public Match getMatch(EObject element) {
				for (Match m : getMatches()) {
					if (m.getLeft() == element || m.getRight() == element || m.getOrigin() == element) {
						return m;
					}
				}
				return null;
			}
		};

		Match fakeMatch = CompareFactory.eINSTANCE.createMatch();
		((InternalEList<Match>) fakeComparison.getMatches()).addUnique(fakeMatch);

		IEqualityHelper equalityHelper = new EqualityHelper(null, matcherConfigure) { // cache is of no use now
            @Override
            protected boolean matchingURIs(EObject object1, EObject object2) {
                /*
                 * we might trying to compare children of the objects under scrutinity right
                 * now, it
                 * might happen if a containment reference is seen as "relevant" for the
                 * matching
                 * process. In those cases, we don't want to compare the whole uris and instead
                 * want
                 * to compare just the current fragment. This has very important performance
                 * implications.
                 */
                if (object1.eContainer() != null && object2.eContainer() != null
                        && fakeComparison.getMatch(object1.eContainer()) != null) {
                    return uriComputer.retrieveFragment(object1)
                            .equals(uriComputer.retrieveFragment(object2));
                }
                return uriDistance.proximity(object1, object2) == 0;
            }

        };

		fakeComparison.eAdapters().add(equalityHelper);
		equalityHelper.setTarget(fakeComparison);

		this.thresholdAmountCache = new AccessBasedLRUCache<>(4096, 4096, 0.75f);
	}

	public int getLocationChangeCoef() {
        return locationChangeCoef;
    }

	/**
	 * Because we cancel the builder class, we need this method to configure the value
	 * @param locationChangeCoef
	 */
    public void setLocationChangeCoef(int locationChangeCoef) {
        this.locationChangeCoef = locationChangeCoef;
    }

	public int getOrderChangeCoef() {
        return orderChangeCoef;
    }

	/**
	 * Because we cancel the builder class, we need this method to configure the value
	 * @param orderChangeCoef
	 */
    public void setOrderChangeCoef(int orderChangeCoef) {
        this.orderChangeCoef = orderChangeCoef;
    }

    /**
	 * {@inheritDoc}
	 */
	public double distance(Comparison inProgress, EObject a, EObject b) {
		this.uriDistance.setComparison(inProgress);
		double maxDist = Math.max(getThresholdAmount(a), getThresholdAmount(b));
		double measuredDist = new CountingDiffEngine(maxDist, this.fakeComparison, matcherConfigure)
				.measureDifferences(inProgress, a, b);
		if (measuredDist > maxDist) {
			return Double.MAX_VALUE;
		}
		return measuredDist;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean areIdentic(Comparison inProgress, EObject a, EObject b) {
		return new CountingDiffEngine(0, this.fakeComparison, matcherConfigure).measureDifferences(inProgress, a, b) == 0;
	}


	public double getThresholdAmount(EObject eObj) {
		Double result = thresholdAmountCache.get(eObj);
		if(result == null) {
			// When can you safely says these are not the same EObjects *at all* ?
			// lets consider every feature which is set, and add this in the max distance.
			// and then tweak the max value adding half a location change
			// thats very empirical... and might be wrong in the end, but it gives pretty good results with
			// Ecore so I'll try to gather as much as test data I can and add the corresponding test to be able to
			// assess the quality of further changes.
			int max = 0;
			int nbFeatures = 0;
			EClass clazz = eObj.eClass();
			
			ClassConfigure classConfigure = this.matcherConfigure.getClassConfigure(clazz);
			Iterable<Entry<EStructuralFeature, FeatureConfigure>> featureConfigures = classConfigure.getConcernedFeatures();
	
			for(Entry<EStructuralFeature, FeatureConfigure> pair : featureConfigures) {
				EStructuralFeature feat = pair.getKey();
				FeatureConfigure configure = pair.getValue();
				int featureWeight = configure.getWeight();
				if (featureWeight != 0 && eObj.eIsSet(feat)) {
					max += featureWeight;
					nbFeatures++;
				}
			}
	
			// max = max + (locationChangeCoef *
			// weightProviderRegistry.getHighestRankingWeightProvider(eObj.eClass().getEPackage()).getParentWeight(eObj));
			max = max + classConfigure.getParentWeight(eObj);
	
			result = max * getThresholdRatio(nbFeatures);

			thresholdAmountCache.put(eObj, result);
		}

		return result;
	}

	/**
	 * return a ratio to apply on the amount of maximum un-similarity amount depending on the number of
	 * features which are considered.
	 * 
	 * @param nbFeatures
	 *            the nb of features which should be considerd to compute the amount.
	 * @return a ratio to appli on the amount of maximum un-similarity amount depending on the number of
	 *         features which are considered.
	 */
	protected double getThresholdRatio(int nbFeatures) {
		if (nbFeatures >= thresholds.length) {
			// CHECKSTYLE:OFF
			return 0.465d;
			// CHECKSTYLE:ON
		}
		return thresholds[nbFeatures];
	}

}
