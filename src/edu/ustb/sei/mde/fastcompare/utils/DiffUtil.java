package edu.ustb.sei.mde.fastcompare.utils;

import static com.google.common.collect.Lists.newArrayList;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edu.ustb.sei.mde.fastcompare.match.EqualityHelper;
import edu.ustb.sei.mde.fastcompare.match.IEqualityHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.compare.AttributeChange;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.DifferenceSource;
import org.eclipse.emf.compare.DifferenceState;
import org.eclipse.emf.compare.EMFCompareMessages;
import org.eclipse.emf.compare.FeatureMapChange;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.ReferenceChange;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * This utility class will be used to provide similarity implementations.
 * 
 * @author <a href="mailto:laurent.goubet@obeo.fr">Laurent Goubet</a>
 */
public final class DiffUtil {
	/**
	 * There are cases where two strings are found to be equal by the {@link #diceCoefficient(String, String)}
	 * even though they're not "strictly" equal. For example, "pascale pierre" and "pierre pascale" would be
	 * seen as "1.0" similarity. We'll use this constant instead of a plain "1d" in such cases.
	 * <p>
	 * This is the closest double to "1d" that is not equal to 1d (floating point arithmetics, IEEE-754).
	 * We're using this as a java 5 equivalent to java 6 <code>Math.nextAfter(1d, 0d)</code>.
	 * </p>
	 */
	private static final double SIMILAR = Double.longBitsToDouble(0x3fefffffffffffffL);

	/**
	 * {@#computeIgnoredElements(Comparison, IEqualityHelper, List, Diff, boolean) Computing ignored elements}
	 * is an O(n^2) algorithm, so we need to make the operation as efficient as possible to avoid the very bad
	 * impact of scaling. This field keeps a cache of information used while computing the ignored elements.
	 */
	private static WeakReference<IgnoredElementsEntry> ignoredElementsEntry = new WeakReference<DiffUtil.IgnoredElementsEntry>(
			null);

	/** This utility class does not need to be instantiated. */
	private DiffUtil() {
		// Hides default constructor
	}

	/**
	 * The set of all the diffs (atomic or not) that refine the given diff or one of its refining diffs,
	 * recursively.
	 * 
	 * @param diff
	 *            The diff for which all the refining diffs are seeked
	 * @return A set of all the refining diffs (as opposed to getting only the 1st level of refining diffs
	 *         that would be obtained by calling getRefinedBy() on diff.
	 */
	public static Set<Diff> getAllRefiningDiffs(Diff diff) {
		Set<Diff> result = new LinkedHashSet<Diff>();
		if (diff != null) {
			for (Diff refiningDiff : diff.getRefinedBy()) {
				result.add(refiningDiff);
				if (!refiningDiff.getRefinedBy().isEmpty()) {
					result.addAll(getAllRefiningDiffs(refiningDiff));
				}
			}
		}
		return result;
	}

	/**
	 * Determines the root refined diff of a refining diff, i.e. a refined diff that is not refining another
	 * diff.
	 * 
	 * @param diff
	 *            The diff for which the root refined diffs are to be determined
	 * @return The root refined diffs of the provided (refining) diff, as a list. Never <code>null</code>.
	 *         Empty if the provided diff does not refine any diff.
	 */
	public static List<Diff> getRootRefinedDiffs(Diff diff) {
		List<Diff> rootRefinedDiffs = newArrayList();
		for (Diff refinedDiff : diff.getRefines()) {
			if (refinedDiff.getRefines().isEmpty()) {
				rootRefinedDiffs.add(refinedDiff);
			} else {
				rootRefinedDiffs.addAll(getRootRefinedDiffs(refinedDiff));
			}
		}
		return rootRefinedDiffs;
	}

	/**
	 * The set of all the atomic diffs that refine the given diff or one of its refining diffs, recursively.
	 * An atomic diff is a diff that is not refined by any diff.
	 * 
	 * @param diff
	 *            The diff for which all the refining diffs are seeked
	 * @return A set of all the atomic refining diffs (as opposed to getting only the 1st level of refining
	 *         diffs that would be obtained by calling getRefinedBy() on diff.
	 */
	public static Set<Diff> getAllAtomicRefiningDiffs(Diff diff) {
		Set<Diff> result = new LinkedHashSet<Diff>();
		if (diff != null) {
			for (Diff refiningDiff : diff.getRefinedBy()) {
				if (refiningDiff.getRefinedBy().isEmpty()) {
					result.add(refiningDiff);
				} else {
					result.addAll(getAllAtomicRefiningDiffs(refiningDiff));
				}
			}
		}
		return result;
	}

	/**
	 * Computes the dice coefficient between the two given String's bigrams.
	 * <p>
	 * This implementation is case sensitive.
	 * </p>
	 * <p>
	 * <b>Note</b> that this implementation handles two- (or less) character-long strings specifically,
	 * degrading into a "char-by-char" comparison instead of using the bigram unit of the dice coefficient. We
	 * want the similarity between <code>"v1"</code> and <code>"v2"</code> to be <code>0.5</code> and not
	 * <code>0</code>. However, we also want <code>"v1"</code> and <code>"v2"</code> to be "more similar" to
	 * each other than <code>"v"</code> and <code>"v2"</code> and <code>"v1"</code> and <code>"v11"</code> to
	 * be "more similar" than <code>"v"</code> and <code>"v11"</code> while this latter also needs to be "less
	 * similar" than <code>"v1"</code> and <code>"v2"</code>. This requires a slightly different handling for
	 * comparisons with a "single character"-long string than for "two character"-long ones. A set of
	 * invariants we wish to meet can be found in the unit tests.
	 * </p>
	 * 
	 * @param first
	 *            First of the two Strings to compare.
	 * @param second
	 *            Second of the two Strings to compare.
	 * @return The dice coefficient of the two given String's bigrams, ranging from 0d to 1d.
	 */
	public static double diceCoefficient(String first, String second) {
		final char[] str1 = first.toCharArray();
		final char[] str2 = second.toCharArray();

		if (Arrays.equals(str1, str2)) {
			return 1d;
		}

		final double coefficient;
		if (str1.length == 0 || str2.length == 0) {
			coefficient = 0d;
		} else if (str1.length == 1 || str2.length == 1 || (str1.length == 2 && str2.length == 2)) {
			int equalChars = 0;

			for (int i = 0; i < Math.min(str1.length, str2.length); i++) {
				if (str1[i] == str2[i]) {
					equalChars++;
				}
			}

			int union = str1.length + str2.length;
			if (str1.length != str2.length) {
				// one of the two is one (or 0) character long, don't double the matches
				coefficient = (double)equalChars / union;
			} else {
				coefficient = ((double)equalChars * 2) / union;
			}
		} else {
			final int[] s1Bigrams = toBigrams(str1);
			final int[] s2Bigrams = toBigrams(str2);

			// We've converted our bigrams to integers. Note that we do not care about the ordering of these
			// integers, even if "bj" comes after "za" and before "az", this will pose no threat since we only
			// use their ordering to hasten the comparisons thereafter.
			Arrays.sort(s1Bigrams);
			Arrays.sort(s2Bigrams);

			int matchingBigrams = 0;
			int index1 = 0;
			int index2 = 0;
			while (index1 < s1Bigrams.length && index2 < s2Bigrams.length) {
				if (s1Bigrams[index1] == s2Bigrams[index2]) {
					matchingBigrams++;
					index1++;
					index2++;
				} else if (s1Bigrams[index1] < s2Bigrams[index2]) {
					index1++;
				} else {
					index2++;
				}
			}

			coefficient = (2d * matchingBigrams) / (s1Bigrams.length + s2Bigrams.length);
		}

		// If the two Strings were equal, we'd have caught it in the first if of this method. On the contrary,
		// if we're here, we know the two aren't exactly the same. However, since we're comparing through
		// bigrams, we "may" end up with "1.0" similarity, which would make further comparisons hazardous. We
		// might for example say that "Pascale Pierre" matches with "Pierre Pascale" even if there is another
		// "Pascale Pierre" somewhere (since these strings' bigrams are the same). Reduce the end number to a
		// value "close to one, but not equal to one".
		return Math.min(coefficient, SIMILAR);
	}

	/**
	 * Converts the array representation of a String into its individual bigrams. Should only be used on
	 * arrays with size greater than or equal to 2.
	 * <p>
	 * Note that we're storing the individual bigrams into ints along the way, the first of a pair in the
	 * least-significant 16 bits. <code>"ab"</code> would thus be converted to <code>6422625</code> or, as
	 * seen bit-wise, <code>0000 0000 0110 0010 0000 0000 0110 0001</code>.
	 * </p>
	 * <p>
	 * We're ignoring the sign of these objects since they do not influence the unicity of the mapping bigram
	 * <-> int.
	 * </p>
	 * 
	 * @param strArray
	 *            The array representation of the string which bigrams we seek.
	 * @return The individual bigrams of strArray, including potential duplicates.
	 */
	private static int[] toBigrams(char[] strArray) {
		final int[] bigrams = new int[strArray.length - 1];
		final int charBitLength = 16;
		for (int i = 0; i < strArray.length - 1; i++) {
			bigrams[i] = strArray[i] | (strArray[i + 1] << charBitLength);
		}
		return bigrams;
	}

	/**
	 * This will compute the longest common subsequence between the two given Lists, ignoring any object that
	 * is included in {@code ignoredElements}. We will use
	 * {@link IEqualityHelper#matchingValues(Object, Object)} in order to try and match the values from both
	 * lists two-by-two. This can thus be used both for reference values or attribute values. If there are two
	 * subsequences of the same "longest" length, the first (according to the second argument) will be
	 * returned.
	 * <p>
	 * Take note that this might be slower than {@link #longestCommonSubsequence(Comparison, List, List)} and
	 * should only be used when elements should be removed from the potential LCS. This is mainly aimed at
	 * merge operations during three-way comparisons as some objects might be in conflict and thus shifting
	 * the computed insertion indices.
	 * </p>
	 * <p>
	 * Please see {@link #longestCommonSubsequence(Comparison, List, List)} for a more complete description.
	 * </p>
	 * 
	 * @param comparison
	 *            This will be used in order to retrieve the Match for EObjects when comparing them.
	 * @param ignoredElements
	 *            Specifies elements that should be excluded from the subsequences.
	 * @param sequence1
	 *            First of the two sequences to consider.
	 * @param sequence2
	 *            Second of the two sequences to consider.
	 * @param <E>
	 *            Type of the sequences content.
	 * @return The LCS of the two given sequences. Will never be the same instance as one of the input
	 *         sequences.
	 * @see #longestCommonSubsequence(Comparison, List, List).
	 */
	public static <E> List<E> longestCommonSubsequence(Comparison comparison, Iterable<E> ignoredElements,
			List<E> sequence1, List<E> sequence2) {
		IEqualityHelper equalityHelper = EqualityHelper.getEqualityHelper(comparison);

		final List<E> copy1 = Lists.newArrayList(sequence1);
		final List<E> copy2 = Lists.newArrayList(sequence2);

		List<Object> ignoredElementsList = new ArrayList<>();
		ignoredElements.forEach(ignoredElementsList::add);

		// Reduce sets
		final List<E> prefix = trimPrefix(comparison, equalityHelper, ignoredElementsList, copy1, copy2);
		final List<E> suffix = trimSuffix(comparison, equalityHelper, ignoredElementsList, copy1, copy2);

		final List<E> subLCS;
		// FIXME extract an interface for the LCS and properly separate these two differently typed
		// implementations.
		if (copy1.size() > Short.MAX_VALUE || copy2.size() > Short.MAX_VALUE) {
			subLCS = intLongestCommonSubsequence(comparison, equalityHelper, ignoredElementsList, copy1,
					copy2);
		} else {
			subLCS = shortLongestCommonSubsequence(comparison, equalityHelper, ignoredElementsList, copy1,
					copy2);
		}

		final List<E> lcs = new ArrayList<E>(prefix.size() + subLCS.size() + suffix.size());
		lcs.addAll(prefix);
		lcs.addAll(subLCS);
		lcs.addAll(suffix);
		return Collections.unmodifiableList(lcs);
	}

	/**
	 * This will compute the longest common subsequence between the two given Lists. We will use
	 * {@link IEqualityHelper#matchingValues(Object, Object)} in order to try and match the values from both
	 * lists two-by-two. This can thus be used both for reference values or attribute values. If there are two
	 * subsequences of the same "longest" length, the first (according to the second argument) will be
	 * returned.
	 * <p>
	 * For example, it the two given sequence are, in this order, <code>{"a", "b", "c", "d", "e"}</code> and
	 * <code>{"c", "z", "d", "a", "b"}</code>, there are two "longest" subsequences : <code>{"a", "b"}</code>
	 * and <code>{"c", "d"}</code>. The first of those two subsequences in the second list is
	 * <code>{"c", "d"}</code>. On the other hand, the LCS of <code>{"a", "b", "c", "d", "e"}</code> and
	 * <code>{"y", "c", "d", "e", "b"}</code> is <code>{"c", "d", "e"}</code>.
	 * </p>
	 * <p>
	 * The following algorithm has been inferred from the wikipedia article on the Longest Common Subsequence,
	 * http://en.wikipedia.org/wiki/Longest_common_subsequence_problem at the time of writing. It is
	 * decomposed in two : we first compute the LCS matrix, then we backtrack through the input to determine
	 * the LCS. Evaluation will be shortcut after the first part if the LCS is one of the two input sequences.
	 * </p>
	 * <p>
	 * Note : we are not using Iterables as input in order to make use of the random access cost of
	 * ArrayLists. This might also be converted to directly use arrays. This implementation will not play well
	 * with LinkedLists or any List which needs to iterate over the values for each call to
	 * {@link List#get(int)}, i.e any list which is not instanceof RandomAccess or does not satisfy its
	 * contract.
	 * </p>
	 * 
	 * @param comparison
	 *            This will be used in order to retrieve the Match for EObjects when comparing them.
	 * @param sequence1
	 *            First of the two sequences to consider.
	 * @param sequence2
	 *            Second of the two sequences to consider.
	 * @param <E>
	 *            Type of the sequences content.
	 * @return The LCS of the two given sequences. Will never be the same instance as one of the input
	 *         sequences.
	 */
	public static <E> List<E> longestCommonSubsequence(Comparison comparison, List<E> sequence1,
			List<E> sequence2) {
		return longestCommonSubsequence(comparison, Collections.<E> emptyList(), sequence1, sequence2);
	}

	/**
	 * Trims and returns the common prefix of the two given sequences. All ignored elements within or after
	 * this common prefix will also be trimmed.
	 * <p>
	 * Note that the two given sequences will be modified in-place.
	 * </p>
	 * 
	 * @param comparison
	 *            This will be used in order to retrieve the Match for EObjects when comparing them.
	 * @param equalityHelper
	 *            This will be used to check for equality of values in the sequences and the ignored elements.
	 * @param ignoredElements
	 *            Specifies elements that should be excluded from the subsequences.
	 * @param sequence1
	 *            First of the two sequences to consider.
	 * @param sequence2
	 *            Second of the two sequences to consider.
	 * @param <E>
	 *            Type of the sequences content.
	 * @return The common prefix of the two given sequences, less their ignored elements. As a side note, both
	 *         {@code sequence1} and {@code sequence2} will have been trimmed of their prefix when this
	 *         returns.
	 */
	private static <E> List<E> trimPrefix(Comparison comparison, IEqualityHelper equalityHelper,
			List<Object> ignoredElements, List<E> sequence1, List<E> sequence2) {
		final int size1 = sequence1.size();
		final int size2 = sequence2.size();

		List<Object> ignoredElements1 = new ArrayList<>(ignoredElements);
		List<Object> ignoredElements2 = new ArrayList<>(ignoredElements);

		final List<E> prefix = Lists.newArrayList();
		int start1 = 1;
		int start2 = 1;
		boolean matching = true;
		while (start1 <= size1 && start2 <= size2 && matching) {
			final E first = sequence1.get(start1 - 1);
			final E second = sequence2.get(start2 - 1);
			if (equalityHelper.matchingValues(first, second)) {
				prefix.add(first);
				start1++;
				start2++;
			} else {
				int ignore1 = indexOf(equalityHelper, ignoredElements1, first);
				if (ignore1 != -1) {
					ignoredElements1.remove(ignore1);
					start1++;
				}
				int ignore2 = indexOf(equalityHelper, ignoredElements2, second);
				if (ignore2 != -1) {
					ignoredElements2.remove(ignore2);
					start2++;
				}
				if (ignore1 == -1 && ignore2 == -1) {
					matching = false;
				}
			}
		}
		sequence1.subList(0, start1 - 1).clear();
		sequence2.subList(0, start2 - 1).clear();

		return prefix;
	}

	/**
	 * Trims and returns the common suffix of the two given sequences. All ignored elements within or before
	 * this common suffix will also be trimmed.
	 * <p>
	 * Note that the two given sequences will be modified in-place.
	 * </p>
	 * 
	 * @param comparison
	 *            This will be used in order to retrieve the Match for EObjects when comparing them.
	 * @param equalityHelper
	 *            The equality helper to use for this computation.
	 * @param ignoredElements
	 *            Specifies elements that should be excluded from the subsequences.
	 * @param sequence1
	 *            First of the two sequences to consider.
	 * @param sequence2
	 *            Second of the two sequences to consider.
	 * @param <E>
	 *            Type of the sequences content.
	 * @return The common suffix of the two given sequences, less their ignored elements. As a side note, both
	 *         {@code sequence1} and {@code sequence2} will have been trimmed of their suffix when this
	 *         returns.
	 */
	private static <E> List<E> trimSuffix(Comparison comparison, IEqualityHelper equalityHelper,
			List<Object> ignoredElements, List<E> sequence1, List<E> sequence2) {
		final int size1 = sequence1.size();
		final int size2 = sequence2.size();

		List<Object> ignoredElements1 = new ArrayList<>(ignoredElements);
		List<Object> ignoredElements2 = new ArrayList<>(ignoredElements);

		final List<E> suffix = Lists.newArrayList();
		int end1 = size1;
		int end2 = size2;
		boolean matching = true;
		while (end1 > 0 && end2 > 0 && matching) {
			final E first = sequence1.get(end1 - 1);
			final E second = sequence2.get(end2 - 1);
			if (equalityHelper.matchingValues(first, second)) {
				suffix.add(first);
				end1--;
				end2--;
			} else {
				int ignore1 = indexOf(equalityHelper, ignoredElements1, first);
				if (ignore1 != -1) {
					ignoredElements1.remove(ignore1);
					end1--;
				}
				int ignore2 = indexOf(equalityHelper, ignoredElements2, second);
				if (ignore2 != -1) {
					ignoredElements2.remove(ignore2);
					end2--;
				}
				if (ignore1 == -1 && ignore2 == -1) {
					matching = false;
				}
			}
		}
		sequence1.subList(end1, size1).clear();
		sequence2.subList(end2, size2).clear();

		return Lists.reverse(suffix);
	}

	/**
	 * Checks whether the given {@code sequence} contains the given {@code element} according to the semantics
	 * of the given {@code equalityHelper}.
	 * 
	 * @param equalityHelper
	 *            The {@link IEqualityHelper} gives us the necessary semantics for Object matching.
	 * @param sequence
	 *            The sequence which elements we need to compare with {@code element}.
	 * @param element
	 *            The element we are seeking in {@code sequence}.
	 * @return index of the given {@code element} in {@code sequence} if any, {@code -1} otherwise.
	 * @see IEqualityHelper#matchingValues(Comparison, Object, Object)
	 */
	private static int indexOf(IEqualityHelper equalityHelper, List<Object> sequence, Object element) {
		for (int i = 0; i < sequence.size(); i++) {
			if (equalityHelper.matchingValues(element, sequence.get(i))) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * This is a classic, single-threaded implementation. We use shorts for the score matrix so as to limit
	 * the memory cost (we know the max LCS length is not greater than Short#MAX_VALUE).
	 * 
	 * @param comparison
	 *            This will be used in order to retrieve the Match for EObjects when comparing them.
	 * @param equalityHelper
	 *            The equality helper to use for this computation.
	 * @param ignoredElements
	 *            Specifies elements that should be excluded from the subsequences.
	 * @param sequence1
	 *            First of the two sequences to consider.
	 * @param sequence2
	 *            Second of the two sequences to consider.
	 * @param <E>
	 *            Type of the sequences content.
	 * @return The LCS of the two given sequences. Will never be the same instance as one of the input
	 *         sequences.
	 */
	private static <E> List<E> shortLongestCommonSubsequence(Comparison comparison,
			IEqualityHelper equalityHelper, List<Object> ignoredElements, List<E> sequence1,
			List<E> sequence2) {
		final int size1 = sequence1.size();
		final int size2 = sequence2.size();

		final short[][] matrix = new short[size1 + 1][size2 + 1];

		// Compute the LCS matrix
		for (int i = 1; i <= size1; i++) {
			final E first = sequence1.get(i - 1);
			for (int j = 1; j <= size2; j++) {
				// assume array dereferencing and arithmetics faster than equals
				final short current = matrix[i - 1][j - 1];
				final short nextIfNoMatch = (short)Math.max(matrix[i - 1][j], matrix[i][j - 1]);

				if (nextIfNoMatch > current) {
					matrix[i][j] = nextIfNoMatch;
				} else {
					final E second = sequence2.get(j - 1);
					if (equalityHelper.matchingValues(first, second)
							&& indexOf(equalityHelper, ignoredElements, second) == -1) {
						matrix[i][j] = (short)(1 + current);
					} else {
						matrix[i][j] = nextIfNoMatch;
					}
				}
			}
		}

		// Traceback the matrix to create the final LCS
		int current1 = size1;
		int current2 = size2;
		final List<E> result = Lists.newArrayList();

		while (current1 > 0 && current2 > 0) {
			final short currentLength = matrix[current1][current2];
			final short nextLeft = matrix[current1][current2 - 1];
			final short nextUp = matrix[current1 - 1][current2];
			if (currentLength > nextLeft && currentLength > nextUp) {
				result.add(sequence1.get(current1 - 1));
				current1--;
				current2--;
			} else if (nextLeft >= nextUp) {
				current2--;
			} else {
				current1--;
			}
		}

		return Lists.reverse(result);
	}

	/**
	 * This is a classic, single-threaded implementation. We know the max LCS length is greater than
	 * Short#MAX_VALUE... the score matrix will thus be int-typed, resulting in a huge memory cost.
	 * 
	 * @param comparison
	 *            This will be used in order to retrieve the Match for EObjects when comparing them.
	 * @param equalityHelper
	 *            The equality helper to use for this computation.
	 * @param ignoredElements
	 *            Specifies elements that should be excluded from the subsequences.
	 * @param sequence1
	 *            First of the two sequences to consider.
	 * @param sequence2
	 *            Second of the two sequences to consider.
	 * @param <E>
	 *            Type of the sequences content.
	 * @return The LCS of the two given sequences. Will never be the same instance as one of the input
	 *         sequences.
	 */
	private static <E> List<E> intLongestCommonSubsequence(Comparison comparison,
			IEqualityHelper equalityHelper, List<Object> ignoredElements, List<E> sequence1,
			List<E> sequence2) {
		final int size1 = sequence1.size();
		final int size2 = sequence2.size();

		final int[][] matrix = new int[size1 + 1][size2 + 1];

		// Compute the LCS matrix
		for (int i = 1; i <= size1; i++) {
			final E first = sequence1.get(i - 1);
			for (int j = 1; j <= size2; j++) {
				// assume array dereferencing and arithmetics faster than equals
				final int current = matrix[i - 1][j - 1];
				final int nextIfNoMatch = Math.max(matrix[i - 1][j], matrix[i][j - 1]);

				if (nextIfNoMatch > current) {
					matrix[i][j] = nextIfNoMatch;
				} else {
					final E second = sequence2.get(j - 1);
					if (equalityHelper.matchingValues(first, second)
							&& indexOf(equalityHelper, ignoredElements, second) == -1) {
						matrix[i][j] = 1 + current;
					} else {
						matrix[i][j] = nextIfNoMatch;
					}
				}
			}
		}

		// Traceback the matrix to create the final LCS
		int current1 = size1;
		int current2 = size2;
		final List<E> result = Lists.newArrayList();

		while (current1 > 0 && current2 > 0) {
			final int currentLength = matrix[current1][current2];
			final int nextLeft = matrix[current1][current2 - 1];
			final int nextUp = matrix[current1 - 1][current2];
			if (currentLength > nextLeft && currentLength > nextUp) {
				result.add(sequence1.get(current1 - 1));
				current1--;
				current2--;
			} else if (nextLeft >= nextUp) {
				current2--;
			} else {
				current1--;
			}
		}

		return Lists.reverse(result);
	}

	/*
	 * TODO perf : all "lookups" in source and target could be rewritten by using the lcs elements' matches.
	 * This may or may not help, should be profiled.
	 */
	/**
	 * This will try and determine the index at which a given element from the {@code source} list should be
	 * inserted in the {@code target} list. We expect {@code newElement} to be an element from the
	 * {@code source} or to have a Match that allows us to map it to one of the {@code source} list's
	 * elements.
	 * <p>
	 * The expected insertion index will always be relative to the Longest Common Subsequence (LCS) between
	 * the two given lists, ignoring all elements from that LCS that have changed between the target list and
	 * the common origin of the two. If there are more than one "longest" subsequence between the two lists,
	 * the insertion index will be relative to the first that comes in the {@code target} list.
	 * </p>
	 * <p>
	 * Note : we are not using Iterables as input in order to make use of the random access cost of
	 * ArrayLists. This might also be converted to directly use arrays. This implementation will not play well
	 * with LinkedLists or any List which needs to iterate over the values for each call to
	 * {@link List#get(int)}, i.e any list which is not instanceof RandomAccess or does not satisfy its
	 * contract.
	 * </p>
	 * 
	 * @param comparison
	 *            This will be used in order to retrieve the Match for EObjects when comparing them.
	 * @param ignoredElements
	 *            If there are elements from {@code target} that should be ignored when searching for an
	 *            insertion index, set them here. Can be {@code null} or an empty list.
	 * @param source
	 *            The List from which one element has to be added to the {@code target} list.
	 * @param target
	 *            The List into which one element from {@code source} has to be added.
	 * @param newElement
	 *            The element from {@code source} that needs to be added into {@code target}.
	 * @param <E>
	 *            Type of the sequences content.
	 * @return The index at which {@code newElement} should be inserted in {@code target}.
	 * @see #longestCommonSubsequence(Comparison, List, List)
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public static <E> int findInsertionIndex(Comparison comparison, Iterable<E> ignoredElements,
			List<E> source, List<E> target, E newElement) {
		final IEqualityHelper equalityHelper = EqualityHelper.getEqualityHelper(comparison);

		final List<E> lcs;
		if (ignoredElements != null) {
			lcs = longestCommonSubsequence(comparison, ignoredElements, source, target);
		} else {
			lcs = longestCommonSubsequence(comparison, source, target);
		}

		E firstLCS = null;
		E lastLCS = null;
		int lcsSize = lcs.size();
		if (lcsSize > 0) {
			firstLCS = lcs.get(0);
			lastLCS = lcs.get(lcsSize - 1);
		}

		final int noLCS = -2;
		int currentIndex = -1;
		int firstLCSIndex = -1;
		int lastLCSIndex = -1;
		if (firstLCS == null) {
			// We have no LCS
			firstLCSIndex = noLCS;
			lastLCSIndex = noLCS;
		}

		ListIterator<E> sourceIterator = source.listIterator();
		Iterator<E> lcsIterator = lcs.iterator();
		E currentLCS = null;
		if (lcsIterator.hasNext()) {
			currentLCS = lcsIterator.next();
		}
		for (int i = 0; sourceIterator.hasNext() && (currentIndex == -1 || firstLCSIndex == -1); i++) {
			final E sourceElement = sourceIterator.next();
			if (currentLCS != null && equalityHelper.matchingValues(sourceElement, currentLCS)) {
				if (firstLCSIndex == -1) {
					firstLCSIndex = i;
				}
				if (lcsIterator.hasNext()) {
					currentLCS = lcsIterator.next();
				} else {
					currentLCS = null;
				}
				// If this is a part of the LCS, it cannot be the current element (might be duplicates, so we
				// have to <continue> here)
				continue;
			}
			if (equalityHelper.matchingValues(sourceElement, newElement)) {
				currentIndex = i;
			}
		}
		// The list may contain duplicates, use a reverse iteration to find the last from LCS.
		final int sourceSize = source.size();
		sourceIterator = source.listIterator(sourceSize);
		for (int i = sourceSize - 1; sourceIterator.hasPrevious() && lastLCSIndex == -1; i--) {
			final E sourceElement = sourceIterator.previous();
			if (lastLCSIndex == -1 && equalityHelper.matchingValues(lastLCS, sourceElement)) {
				lastLCSIndex = i;
			}
		}

		int insertionIndex = -1;
		if (firstLCSIndex == noLCS) {
			// We have no LCS. The two lists have no element in common. Insert at the very end of the target.
			insertionIndex = target.size();
		} else if (currentIndex < firstLCSIndex) {
			// The object we are to insert is before the LCS in source.
			insertionIndex = insertBeforeLCS(target, equalityHelper, firstLCS);
		} else if (currentIndex > lastLCSIndex) {
			// The object we are to insert is after the LCS in source.
			insertionIndex = findInsertionIndexAfterLCS(target, equalityHelper, lastLCS);
		} else {
			// Our object is in-between two elements A and B of the LCS in source
			insertionIndex = findInsertionIndexWithinLCS(source, target, equalityHelper, lcs, currentIndex);
		}

		// We somehow failed to determine the insertion index. Insert at the very end.
		if (insertionIndex == -1) {
			insertionIndex = target.size();
		}

		return insertionIndex;
	}

	/**
	 * This will try and determine the index at which a given element from the {@code source} list should be
	 * inserted in the {@code target} list.
	 * <p>
	 * The expected insertion index will always be relative to the Longest Common Subsequence (LCS) between
	 * the two given lists.
	 * </p>
	 * 
	 * @param comparison
	 *            This will be used in order to retrieve the Match for EObjects when comparing them.
	 * @param source
	 *            The List from which one element has to be added to the {@code target} list.
	 * @param target
	 *            The List into which one element from {@code source} has to be added.
	 * @param lcs
	 *            The precomputed LCS between these two lists.
	 * @param currentIndexInSource
	 *            The current index (in source) of the element we want to insert in target.
	 * @param <E>
	 *            Type of the sequences content.
	 * @return The index at which {@code newElement} should be inserted in {@code target}.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public static <E> int findInsertionIndexForElementAt(Comparison comparison, List<E> source,
			List<E> target, List<E> lcs, int currentIndexInSource) {
		final IEqualityHelper equalityHelper = EqualityHelper.getEqualityHelper(comparison);

		E firstLCS = null;
		E lastLCS = null;
		int lcsSize = lcs.size();
		if (lcsSize > 0) {
			firstLCS = lcs.get(0);
			lastLCS = lcs.get(lcsSize - 1);
		}

		final int noLCS = -2;
		int firstLCSIndex = -1;
		int lastLCSIndex = -1;
		if (firstLCS == null) {
			// We have no LCS
			firstLCSIndex = noLCS;
			lastLCSIndex = noLCS;
		}

		ListIterator<E> sourceIterator = source.listIterator();
		for (int i = 0; sourceIterator.hasNext() && firstLCSIndex == -1; i++) {
			final E sourceElement = sourceIterator.next();
			if (firstLCSIndex == -1 && equalityHelper.matchingValues(sourceElement, firstLCS)) {
				firstLCSIndex = i;
			}
		}
		// The list may contain duplicates, use a reverse iteration to find the last from LCS.
		final int sourceSize = source.size();
		sourceIterator = source.listIterator(sourceSize);
		for (int i = sourceSize - 1; sourceIterator.hasPrevious() && lastLCSIndex == -1; i--) {
			final E sourceElement = sourceIterator.previous();
			if (lastLCSIndex == -1 && equalityHelper.matchingValues(lastLCS, sourceElement)) {
				lastLCSIndex = i;
			}
		}

		int insertionIndex = -1;
		if (firstLCSIndex == noLCS) {
			// We have no LCS. The two lists have no element in common. Insert at the very end of the target.
			insertionIndex = target.size();
		} else if (currentIndexInSource < firstLCSIndex) {
			// The object we are to insert is before the LCS in source.
			insertionIndex = insertBeforeLCS(target, equalityHelper, firstLCS);
		} else if (currentIndexInSource > lastLCSIndex) {
			// The object we are to insert is after the LCS in source.
			insertionIndex = findInsertionIndexAfterLCS(target, equalityHelper, lastLCS);
		} else {
			// Our object is in-between two elements A and B of the LCS in source
			insertionIndex = findInsertionIndexWithinLCS(source, target, equalityHelper, lcs,
					currentIndexInSource);
		}

		// We somehow failed to determine the insertion index. Insert at the very end.
		if (insertionIndex == -1) {
			insertionIndex = target.size();
		}

		return insertionIndex;
	}

	/**
	 * This will be called to try and find the insertion index for an element that is located in-between two
	 * elements A and B of the LCS between {@code source} and {@code target}.
	 * 
	 * @param source
	 *            The List from which one element has to be added to the {@code target} list.
	 * @param target
	 *            The List into which one element from {@code source} has to be added.
	 * @param equalityHelper
	 *            The equality helper to use for this computation.
	 * @param lcs
	 *            The lcs between {@code source} and {@code target}.
	 * @param currentIndex
	 *            Current index (in {@code source} of the element we are to insert into {@code target}.
	 * @param <E>
	 *            Type of the sequences content.
	 * @return The index in the target list in which should be inserted that element.
	 */
	private static <E> int findInsertionIndexWithinLCS(List<E> source, List<E> target,
			final IEqualityHelper equalityHelper, final List<E> lcs, int currentIndex) {
		int insertionIndex = -1;
		/*
		 * If any element of the subsequence {<index of A>, <index of B>} from source had been in the same
		 * subsequence in target, it would have been part of the LCS. We thus know none is.
		 */
		// The insertion index will be just after A in target

		// First, find which element of the LCS is "A"
		int lcsIndexOfSubsequenceStart = -1;
		for (int i = 0; i < currentIndex; i++) {
			final E sourceElement = source.get(i);

			boolean isInLCS = false;
			for (int j = lcsIndexOfSubsequenceStart + 1; j < lcs.size() && !isInLCS; j++) {
				final E lcsElement = lcs.get(j);

				if (equalityHelper.matchingValues(sourceElement, lcsElement)) {
					isInLCS = true;
					lcsIndexOfSubsequenceStart++;
				}
			}
		}

		if (lcsIndexOfSubsequenceStart > -1) {
			// Now browse our target list to find the index of that lcs element in it
			int targetCursor = 0;
			for (int i = 0; i <= lcsIndexOfSubsequenceStart; i++) {
				E lcsElement = lcs.get(i);

				boolean isInLCS = false;
				for (int j = targetCursor; j < target.size() && !isInLCS; j++) {
					E targetElement = target.get(j);

					if (equalityHelper.matchingValues(lcsElement, targetElement)) {
						isInLCS = true;
						targetCursor = j + 1;
					}
				}
			}
			insertionIndex = targetCursor;
		}

		return insertionIndex;
	}

	/**
	 * This will be called when we are to insert an element after the LCS in the {@code target} list.
	 * 
	 * @param target
	 *            The List into which one element has to be added.
	 * @param equalityHelper
	 *            The equality helper to use for this computation.
	 * @param lastLCS
	 *            The last element of the LCS.
	 * @param <E>
	 *            Type of the sequences content.
	 * @return The index to use for insertion into {@code target} in order to add an element just after the
	 *         LCS.
	 */
	private static <E> int findInsertionIndexAfterLCS(List<E> target, IEqualityHelper equalityHelper,
			E lastLCS) {
		int insertionIndex = -1;
		// The insertion index will be inside the subsequence {<LCS end>, <list.size()>} in target.
		/*
		 * We'll insert it just after the LCS end : there cannot be any common element between the two lists
		 * "after" the LCS since it would be part of the LCS itself.
		 */
		for (int i = target.size() - 1; i >= 0 && insertionIndex == -1; i--) {
			final E targetElement = target.get(i);
			if (equalityHelper.matchingValues(lastLCS, targetElement)) {
				// We've reached the last element of the LCS in target. insert after it.
				insertionIndex = i + 1;
			}
		}
		return insertionIndex;
	}

	/**
	 * This will be called when we are to insert an element before the LCS in the {@code target} list.
	 * 
	 * @param target
	 *            The List into which one element has to be added.
	 * @param equalityHelper
	 *            The equality helper to use for this computation.
	 * @param firstLCS
	 *            The first element of the LCS.
	 * @param <E>
	 *            Type of the sequences content.
	 * @return The index to use for insertion into {@code target} in order to add an element just before the
	 *         LCS.
	 */
	private static <E> int insertBeforeLCS(List<E> target, IEqualityHelper equalityHelper, E firstLCS) {
		int insertionIndex = -1;
		// The insertion index will be inside the subsequence {0, <LCS start>} in target
		/*
		 * We'll insert it just before the LCS start : there cannot be any common element between the two
		 * lists "before" the LCS since it would be part of the LCS itself.
		 */
		for (int i = 0; i < target.size() && insertionIndex == -1; i++) {
			final E targetElement = target.get(i);

			if (equalityHelper.matchingValues(firstLCS, targetElement)) {
				// We've reached the first element from the LCS in target. Insert here
				insertionIndex = i;
			}
		}
		return insertionIndex;
	}

	/**
	 * This will try and determine the index at which a given element from the {@code source} list should be
	 * inserted in the {@code target} list. We expect {@code newElement} to be an element from the
	 * {@code source} or to have a Match that allows us to map it to one of the {@code source} list's
	 * elements.
	 * <p>
	 * The expected insertion index will always be relative to the Longest Common Subsequence (LCS) between
	 * the two given lists. If there are more than one "longest" subsequence between the two lists, the
	 * insertion index will be relative to the first that comes in the {@code target} list.
	 * </p>
	 * <p>
	 * For example, assume {@code source} is <code>{"1", "2", "4", "6", "8", "3", "0", "7", "5"}</code> and
	 * {@code target} is <code>{"8", "1", "2", "9", "3", "4", "7"}</code>; I try to merge the addition of
	 * {@code "0"} in the right list. The returned "insertion index" will be {@code 5} : just after
	 * {@code "3"}. There are two subsequence of the same "longest" length 4 :
	 * <code>{"1", "2", "3", "7"}</code> and <code>{"1", "2", "4", "7"}</code>. However, the first of those
	 * two in {@code target} is <code>{"1", "2", "3", "7"}</code>. The closest element before {@code "0"} in
	 * this LCS in {@code source} is {@code "3"}.
	 * </p>
	 * <p>
	 * Note : we are not using Iterables as input in order to make use of the random access cost of
	 * ArrayLists. This might also be converted to directly use arrays. This implementation will not play well
	 * with LinkedLists or any List which needs to iterate over the values for each call to
	 * {@link List#get(int)}, i.e any list which is not instanceof RandomAccess or does not satisfy its
	 * contract.
	 * </p>
	 * 
	 * @param comparison
	 *            This will be used in order to retrieve the Match for EObjects when comparing them.
	 * @param source
	 *            The List from which one element has to be added to the {@code target} list.
	 * @param target
	 *            The List into which one element from {@code source} has to be added.
	 * @param newElement
	 *            The element from {@code source} that needs to be added into {@code target}.
	 * @param <E>
	 *            Type of the sequences content.
	 * @return The index at which {@code newElement} should be inserted in {@code target}.
	 * @see #longestCommonSubsequence(Comparison, List, List)
	 */
	public static <E> int findInsertionIndex(Comparison comparison, List<E> source, List<E> target,
			E newElement) {
		return findInsertionIndex(comparison, null, source, target, newElement);
	}

	/**
	 * This is the main entry point for {@link #findInsertionIndex(Comparison, Iterable, List, List, Object)}.
	 * It will use default algorithms to determine the source and target lists as well as the list of elements
	 * that should be ignored when computing the insertion index.
	 * 
	 * @param comparison
	 *            This will be used in order to retrieve the Match for EObjects when comparing them.
	 * @param diff
	 *            The diff which merging will trigger the need for an insertion index in its target list.
	 * @param rightToLeft
	 *            {@code true} if the merging will be done into the left list, so that we should consider the
	 *            right model as the source and the left as the target.
	 * @return The index at which this {@code diff}'s value should be inserted into the 'target' list, as
	 *         inferred from {@code rightToLeft}.
	 * @see #findInsertionIndex(Comparison, Iterable, List, List, Object)
	 */
	public static int findInsertionIndex(Comparison comparison, Diff diff, boolean rightToLeft) {
		final EStructuralFeature targetFeature = getTargetFeature(comparison, diff, rightToLeft);
		if (!targetFeature.isMany()) {
			throw new IllegalArgumentException(EMFCompareMessages.getString("DiffUtil.IllegalFeature", //$NON-NLS-1$
					targetFeature.getName()));
		}

		final List<Object> sourceList = getSourceList(comparison, diff, rightToLeft);
		final List<Object> targetList = getTargetList(comparison, diff, rightToLeft);
		final Object changedValue = getChangedValue(diff);

		// We rely on the implementation details, i.e., that this computes a modifiable set, except for the
		// empty set.
		Set<Object> ignoredElements = computeIgnoredElements(comparison, EqualityHelper.getEqualityHelper(comparison),
				targetList, diff, rightToLeft);
		if (diff.getKind() == DifferenceKind.MOVE) {
			if (ignoredElements.isEmpty()) {
				ignoredElements = Collections.singleton(changedValue);
			} else {
				ignoredElements.add(changedValue);
			}
		}

		return DiffUtil.findInsertionIndex(comparison, ignoredElements, sourceList, targetList, changedValue);
	}

	/**
	 * Get the changed feature of the given difference.
	 * 
	 * @param diff
	 *            the given difference.
	 * @return the feature of the difference.
	 */
	private static EStructuralFeature getChangedFeature(Diff diff) {
		final EStructuralFeature feature;
		if (diff instanceof AttributeChange) {
			feature = ((AttributeChange)diff).getAttribute();
		} else if (diff instanceof ReferenceChange) {
			feature = ((ReferenceChange)diff).getReference();
		} else if (diff instanceof FeatureMapChange) {
			feature = ((FeatureMapChange)diff).getAttribute();
		} else {
			throw new IllegalArgumentException(EMFCompareMessages.getString("DiffUtil.IllegalDiff", diff //$NON-NLS-1$
					.eClass().getName()));
		}
		return feature;
	}

	/**
	 * Get the value of the given difference.
	 * 
	 * @param diff
	 *            the given difference.
	 * @return the value of the difference.
	 */
	private static Object getChangedValue(Diff diff) {
		final Object value;
		if (diff instanceof AttributeChange) {
			value = ((AttributeChange)diff).getValue();
		} else if (diff instanceof ReferenceChange) {
			value = ((ReferenceChange)diff).getValue();
		} else if (diff instanceof FeatureMapChange) {
			value = ((FeatureMapChange)diff).getValue();
		} else {
			throw new IllegalArgumentException(EMFCompareMessages.getString("DiffUtil.IllegalDiff", diff //$NON-NLS-1$
					.eClass().getName()));
		}
		return value;
	}

	/**
	 * Retrieves the "source" list of the given {@code diff}. This will be different according to the kind of
	 * change and the direction of the merging. Note that, e.g., in case of a move, the source list is not
	 * necessarily the list of values which originally contained the moved value; it is the target list of the
	 * move on the source side, which is retrieved through the comparison's match.
	 * 
	 * @param comparison
	 *            This will be used in order to retrieve the Match for EObjects when comparing them.
	 * @param diff
	 *            The diff for which merging we need a 'source'.
	 * @param rightToLeft
	 *            Direction of the merging. {@code true} if the merge is to be done on the left side, making
	 *            'source' the right side, {@code false} otherwise.
	 * @return The list that should be used as a source for this merge. May be empty, but never
	 *         <code>null</code>.
	 */
	private static List<Object> getSourceList(Comparison comparison, Diff diff, boolean rightToLeft) {
		final EObject expectedContainer;
		final Match match = diff.getMatch();

		if (diff.getKind() == DifferenceKind.MOVE) {
			final EObject targetContainer = getTargetContainer(comparison, diff, rightToLeft);
			final Match targetMatch = comparison.getMatch(targetContainer);

			expectedContainer = ComparisonUtil.getExpectedSide(targetMatch, diff.getSource(), rightToLeft);
		} else {
			if (diff.getKind() == DifferenceKind.DELETE && match.getOrigin() != null
					&& rightToLeft == (diff.getSource() == DifferenceSource.LEFT)) {
				expectedContainer = match.getOrigin();
			} else if (rightToLeft) {
				expectedContainer = match.getRight();
			} else {
				expectedContainer = match.getLeft();
			}
		}

		final EStructuralFeature feature = getTargetFeature(comparison, diff, rightToLeft);
		return ReferenceUtil.getAsList(expectedContainer, feature);
	}

	/**
	 * Retrieves the "target" list of the given {@code diff}. This will be different according to the kind of
	 * change and the direction of the merging.
	 * 
	 * @param comparison
	 *            This will be used in order to retrieve the Match for EObjects when comparing them.
	 * @param diff
	 *            The diff for which merging we need a 'target'.
	 * @param rightToLeft
	 *            Direction of the merging. {@code true} if the merge is to be done on the left side, making
	 *            'target' the right side, {@code false} otherwise.
	 * @return The list that should be used as a target for this merge. May be empty, but never
	 *         <code>null</code>.
	 */
	private static List<Object> getTargetList(Comparison comparison, Diff diff, boolean rightToLeft) {
		final EStructuralFeature targetFeature = getTargetFeature(comparison, diff, rightToLeft);
		final EObject expectedContainer = getTargetContainer(comparison, diff, rightToLeft);
		return ReferenceUtil.getAsList(expectedContainer, targetFeature);
	}

	/**
	 * Retrieves the "target" container, that is the object containing the value to be changed by the given
	 * {@code diff}. This container will be different according to the kind of change and the direction of the
	 * merging.
	 * 
	 * @param comparison
	 *            This will be used in order to retrieve the Match for EObjects when comparing them.
	 * @param diff
	 *            The diff for which we need the target container in the current merging.
	 * @param rightToLeft
	 *            Direction of the merging. {@code true} if the merge is to be done on the left side, making
	 *            'target' the right side, {@code false} otherwise.
	 * @return The container that should be used as a target for this merge.
	 */
	private static EObject getTargetContainer(Comparison comparison, Diff diff, boolean rightToLeft) {
		final EObject targetContainer;

		final Match match = diff.getMatch();

		if (diff.getKind() == DifferenceKind.MOVE && ComparisonUtil.isFeatureMapContainment(diff)) {
			targetContainer = ComparisonUtil.moveElementGetExpectedContainer(comparison,
					(FeatureMapChange)diff, rightToLeft);
		} else if (isContainmentReferenceMove(diff)) {

			// The value can only be an EObject, and its match cannot be null.
			// If any of these two assumptions is wrong, something went horribly awry beforehand.
			final Object value = getChangedValue(diff);
			final Match valueMatch = comparison.getMatch((EObject)value);

			// If it exists, use the source side's container as reference
			EObject target;
			if (rightToLeft) {
				target = valueMatch.getRight();
			} else {
				target = valueMatch.getLeft();
			}
			if (target == null) {
				target = valueMatch.getOrigin();
			}

			final Match targetContainerMatch = comparison.getMatch(target.eContainer());

			if (rightToLeft) {
				targetContainer = targetContainerMatch.getLeft();
			} else {
				targetContainer = targetContainerMatch.getRight();
			}
		} else if (rightToLeft) {
			targetContainer = match.getLeft();
		} else {
			targetContainer = match.getRight();
		}

		return targetContainer;
	}

	/**
	 * Retrieves the "target" feature, that is the feature at which the value to be changed by the given
	 * {@code diff} is contained by the {@link #getTargetContainer(Comparison, Diff, boolean) target
	 * container}. This feature might be different according to the kind of change and the direction of the
	 * merging.
	 * 
	 * @param comparison
	 *            This will be used in order to retrieve the Match for EObjects when comparing them.
	 * @param diff
	 *            The diff for which we need the target feature in the current merging.
	 * @param rightToLeft
	 *            Direction of the merging. {@code true} if the merge is to be done on the left side, making
	 *            'target' the right side, {@code false} otherwise.
	 * @return The feature that should be used as a target for this merge.
	 */
	private static EStructuralFeature getTargetFeature(Comparison comparison, Diff diff,
			boolean rightToLeft) {
		final EStructuralFeature targetFeature;

		EStructuralFeature changedFeature = getChangedFeature(diff);
		if (diff.getKind() == DifferenceKind.MOVE && isTargetOnTheRight(diff, rightToLeft)
				&& isContainmentReference(changedFeature)) {
			final Object diffValue = getChangedValue(diff);
			final Match valueMatch = comparison.getMatch((EObject)diffValue);
			final EObject expectedValue = ComparisonUtil.getExpectedSide(valueMatch, diff.getSource(),
					rightToLeft);
			targetFeature = expectedValue.eContainingFeature();
		} else {
			targetFeature = changedFeature;
		}

		return targetFeature;
	}

	/**
	 * Specifies whether the given {@code diff} represents a move of an {@link EObject}; that is, whether the
	 * changed feature of the given {@code diff} is a containment reference.
	 * 
	 * @param diff
	 *            The diff to check.
	 * @return <code>true</code> if {@code diff} represents a move of an {@link EObject}, <code>false</code>
	 *         otherwise.
	 */
	private static boolean isContainmentReferenceMove(Diff diff) {
		final EStructuralFeature diffFeature = getChangedFeature(diff);
		return diff.getKind() == DifferenceKind.MOVE && isContainmentReference(diffFeature);
	}

	/**
	 * Specifies whether the given <code>feature</code> is a {@link EReference#isContainment() containment}
	 * {@link EReference reference}.
	 * 
	 * @param feature
	 *            The feature to check.
	 * @return <code>true</code> if the <code>feature</code> is a containment reference, <code>false</code>
	 *         otherwise.
	 */
	public static boolean isContainmentReference(EStructuralFeature feature) {
		return feature != null && ((EStructuralFeature.Internal)feature).isContainment();
	}

	/**
	 * Specifies whether the target container, target feature, and target value of the current merging is the
	 * right-hand side. This depends on the source of the {@code diff} and the direction of merging as
	 * specified in {@code rightToLeft}.
	 * 
	 * @param diff
	 *            The diff to check.
	 * @param rightToLeft
	 *            Direction of the merging. {@code true} if the merge is to be done on the left side, making
	 *            'target' the right side, {@code false} otherwise.
	 * @return <code>true</code> if the target is on the right side, <code>false</code> otherwise.
	 */
	private static boolean isTargetOnTheRight(Diff diff, boolean rightToLeft) {
		DifferenceSource source = diff.getSource();
		if (rightToLeft) {
			return source == DifferenceSource.LEFT;
		} else {
			return source == DifferenceSource.RIGHT;

		}
	}

	/**
	 * When computing the insertion index of an element in a list, we need to ignore all elements present in
	 * that list that feature unresolved Diffs on the same feature. This algorithm is expensive and because
	 * this method is generally called O{n^2} times we must take steps to optimize it. The caller assumes that
	 * a non-empty returned set is modifiable.
	 * 
	 * @param comparison
	 *            This will be used in order to retrieve the Match for EObjects when comparing them.
	 * @param equalityHelper
	 *            This will be used to check for equality of values.
	 * @param candidates
	 *            The list in which we need to compute an insertion index.
	 * @param diff
	 *            The diff we are computing an insertion index for.
	 * @param <E>
	 *            Type of the list's content.
	 * @param rightToLeft
	 *            Direction of the merging. {@code true} if the merge is to be done on the left side, making
	 *            'target' the right side, {@code false} otherwise.
	 * @return The list of elements that should be ignored when computing the insertion index for a new
	 *         element in {@code candidates}.
	 */
	public static <E> Set<E> computeIgnoredElements(Comparison comparison, IEqualityHelper equalityHelper,
			List<E> candidates, final Diff diff, boolean rightToLeft) {
		// There is no point doing any computations if the candidates list is empty.
		if (!candidates.isEmpty()) {
			return Collections.emptySet();
		}

		// We only use this if the candidates list contains EObjects.
		IgnoredElementsEntry currentIgnoredElementsEntry = null;

		// We only use this if the candidates list does not contain EObjects.
		Iterable<? extends Diff> filteredCandidates = null;

		final Set<E> ignored = Sets.newLinkedHashSet();
		UnresolvedDiffMatching predicate = new UnresolvedDiffMatching(comparison, equalityHelper, diff,
				rightToLeft);
		for (E candidate : candidates) {
			predicate.setReferenceValue(candidate);
			if (candidate instanceof EObject) {
				if (currentIgnoredElementsEntry == null) {
					currentIgnoredElementsEntry = ignoredElementsEntry.get();
					if (currentIgnoredElementsEntry == null
							|| !currentIgnoredElementsEntry.appliesTo(candidates)) {
						currentIgnoredElementsEntry = new IgnoredElementsEntry(comparison, candidates);
						ignoredElementsEntry = new WeakReference<IgnoredElementsEntry>(
								currentIgnoredElementsEntry);
					}
				}

				List<Diff> differences = currentIgnoredElementsEntry.getDifferences(candidate);
				if (Predicates.exists(differences, predicate)) {
					ignored.add(candidate);
				}
			} else {
				if (filteredCandidates == null) {
					Match match = diff.getMatch();
					filteredCandidates = Lists.newArrayList(match.getDifferences());
				}
				if (Predicates.exists(filteredCandidates, predicate)) {
					ignored.add(candidate);
				}
			}
		}
		return ignored;
	}

	/**
	 * This can be used to check whether a given Diff affects a value for which we can find another,
	 * unresolved Diff on a given Feature.
	 * 
	 * @author <a href="mailto:laurent.goubet@obeo.fr">Laurent Goubet</a>
	 */
	private static class UnresolvedDiffMatching implements Predicate<Diff> {

		/** The value changed by {@link #referenceDiff} to compare against. */
		private Object referenceValue;

		/** The target feature of the referenceDiff. */
		private final EStructuralFeature referenceFeature;

		/** The target container of the reference diff. */
		private final EObject referenceContainer;

		/** The comparison of the {@link #referenceDiff diff's} {@link Diff#getMatch() match}. */
		private final Comparison comparison;

		/** The equality helper of the {@link #referenceDiff diff's} {@link Diff#getMatch() match}. */
		private final IEqualityHelper equalityHelper;

		/** The direction of the merge. */
		private final boolean rightToLeft;

		/**
		 * Constructs a predicate that can be used to retrieve all unresolved diffs that apply to the target
		 * feature and container of the given {@code diff} and that affect the same value as the given
		 * {@code value}.
		 * 
		 * @param comparison
		 *            This will be used in order to retrieve the Match for EObjects when comparing them.
		 * @param equalityHelper
		 *            This will be used to check for equality of values.
		 * @param diff
		 *            to compare against this diff's target feature and container.
		 * @param rightToLeft
		 *            the direction of the merging, which is used to obtain the diff's target feature and
		 *            container.
		 */
		UnresolvedDiffMatching(Comparison comparison, IEqualityHelper equalityHelper, Diff diff,
				boolean rightToLeft) {
			this.comparison = comparison;
			this.equalityHelper = equalityHelper;
			this.rightToLeft = rightToLeft;
			this.referenceFeature = getTargetFeature(comparison, diff, rightToLeft);
			this.referenceContainer = getTargetContainer(comparison, diff, rightToLeft);
		}

		/**
		 * Updates that predicate based on the given reference value.
		 * 
		 * @param referenceValue
		 *            the new current reference value.
		 */
		public void setReferenceValue(Object referenceValue) {
			this.referenceValue = referenceValue;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see com.google.common.base.Predicate#apply(java.lang.Object)
		 */
		public boolean test(Diff input) {
			// Probably ADDs in conflict with an ADD at a different index should also be ignored
			return input.getKind() == DifferenceKind.MOVE && isUnresolved(input) && matchesTarget(input)
					&& matchesValue(input);
		}

		/**
		 * Specifies whether the given {@code diff} is unresolved (i.e., has not been merged yet).
		 * 
		 * @param input
		 *            diff to check.
		 * @return <code>true</code> if it is unresolved, otherwise <code>false</code>.
		 */
		private boolean isUnresolved(Diff input) {
			return input.getState() == DifferenceState.UNRESOLVED;
		}

		/**
		 * Specifies whether the given {@code diff} matches the target container and feature of the
		 * {@link #referenceDiff reference diff} of this predicate.
		 * 
		 * @param input
		 *            diff to check.
		 * @return <code>true</code> if it matches the target container and feature, <code>false</code>
		 *         otherwise.
		 */
		private boolean matchesTarget(Diff input) {
			final boolean isSupportedDiff = input instanceof AttributeChange
					|| input instanceof ReferenceChange || input instanceof FeatureMapChange;
			return isSupportedDiff && matchesTargetFeature(input) && matchesTargetContainer(input);
		}

		/**
		 * Specifies whether the given {@code diff} matches the target feature of the {@link #referenceDiff
		 * reference diff} of this predicate.
		 * 
		 * @param input
		 *            diff to check.
		 * @return <code>true</code> if it matches the target feature, <code>false</code> otherwise.
		 */
		private boolean matchesTargetFeature(Diff input) {
			final EStructuralFeature inputFeature = getTargetFeature(comparison, input, rightToLeft);
			return referenceFeature == inputFeature;
		}

		/**
		 * Specifies whether the given {@code diff} matches the target container of the {@link #referenceDiff
		 * reference diff} of this predicate.
		 * 
		 * @param input
		 *            diff to check.
		 * @return <code>true</code> if it matches the target container, <code>false</code> otherwise.
		 */
		private boolean matchesTargetContainer(Diff input) {
			final EObject inputContainer = getTargetContainer(comparison, input, rightToLeft);
			return referenceContainer == inputContainer;
		}

		/**
		 * Specifies whether the affected value of the given {@code diff} matches the {@link #referenceValue
		 * reference value} of this predicate.
		 * 
		 * @param input
		 *            diff to check.
		 * @return <code>true</code> if the affected value matches the reference value, <code>false</code>
		 *         otherwise.
		 */
		private boolean matchesValue(Diff input) {
			final boolean matchesValue;
			final Object inputValue = getChangedValue(input);
			if (inputValue == referenceValue) {
				matchesValue = true;
			} else if (input instanceof AttributeChange || input instanceof FeatureMapChange) {
				matchesValue = equalityHelper.matchingAttributeValues(inputValue, referenceValue);
			} else if (input instanceof ReferenceChange) {
				matchesValue = equalityHelper.matchingValues(inputValue, referenceValue);
			} else {
				matchesValue = false;
			}
			return matchesValue;
		}
	}

	/**
	 * A cache for mapping objects to their differences. The method
	 * {@link DiffUtil#computeIgnoredElements(Comparison, IEqualityHelper, List, Diff, boolean)
	 * computeIgnoredElements}, is often called repeatedly for the same list of candidates. Without some
	 * careful optimization, that method consumes 30% of the overall time to merge a large number of
	 * differences.
	 */
	private static class IgnoredElementsEntry {
		/**
		 * The comparison.
		 */
		private final Comparison comparison;

		/**
		 * The underlying {@link BasicEList#data() data} array of the candidates list.
		 */
		private final Object[] data;

		/**
		 * A map from object to its associated differences. The keys are generally EObject, but we make it
		 * Object so that we don't need to do a cast to do a lookup.
		 */
		private final Map<Object, EList<Diff>> allDifferences;

		/**
		 * Creates instance given the comparison and the list of candidates. Only if the candidates list is a
		 * {@link BasicEList} will we cache any information about the differences of the candidates.
		 * 
		 * @param comparison
		 *            the comparison.
		 * @param candidates
		 *            the list of candidates.
		 */
		protected IgnoredElementsEntry(Comparison comparison, List<?> candidates) {
			this.comparison = comparison;
			if (candidates instanceof BasicEList<?>) {
				data = ((BasicEList<?>)candidates).data();
				allDifferences = Maps.newHashMap();
			} else {
				data = null;
				allDifferences = null;
			}
		}

		/**
		 * Returns whether this entry is an entry for the given list of candidates. In particular, it returns
		 * {@code true} if the the candidates list is a {@link BasicEList} with {@link BasicEList#data() data}
		 * that is {@code ==} to this entry's {@link #data}.
		 * 
		 * @param candidates
		 *            the list in question.
		 * @return whether this entry is an entry for the given list of candidates.
		 */
		public boolean appliesTo(List<?> candidates) {
			if (candidates instanceof BasicEList<?>) {
				return this.data == ((BasicEList<?>)candidates).data();
			} else {
				return false;
			}
		}

		/**
		 * Returns the {@link Comparison#getDifferences(EObject) differences} for the given object. This
		 * implementation will use {@link #allDifferences the cache}, if available. The caller must ensure
		 * that the cache is {@link #appliesTo(List) applicable} before calling this method.
		 * 
		 * @param object
		 *            the object, which must be an {@link EObject}.
		 * @return the differences for the given object.
		 */
		public List<Diff> getDifferences(Object object) {
			if (allDifferences == null) {
				return comparison.getDifferences((EObject)object);
			} else {
				EList<Diff> differences = allDifferences.get(object);
				if (differences == null) {
					differences = comparison.getDifferences((EObject)object);
					allDifferences.put(object, differences);
				} else {
					differences.size();
				}
				return differences;
			}
		}
	}
}
