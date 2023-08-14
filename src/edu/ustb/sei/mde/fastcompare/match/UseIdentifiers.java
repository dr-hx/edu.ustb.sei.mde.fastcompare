package edu.ustb.sei.mde.fastcompare.match;

public enum UseIdentifiers {
	/**
	 * Only use id, do not fall back on content based matching if no id is found. This means any element not
	 * having an ID will not be matched.
	 */
	ONLY,

	/**
	 * Use ID when available on an element, if not available use the content matching strategy.
	 */
	WHEN_AVAILABLE,

	/**
	 * Never use IDs, always use the content matching strategy. That's useful for instance when you want to
	 * compare two results of a transformation which have arbitrary IDs.
	 */
	NEVER
}
