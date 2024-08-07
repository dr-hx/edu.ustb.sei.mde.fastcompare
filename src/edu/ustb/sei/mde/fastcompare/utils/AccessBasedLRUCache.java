package edu.ustb.sei.mde.fastcompare.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Map acting like a LRU cache which will evict elements which have not been accessed in a while.
 * 
 * @param <K>
 *            the type of keys maintained by this map
 * @param <V>
 *            the type of mapped values
 */
public class AccessBasedLRUCache<K, V> extends LinkedHashMap<K, V> {

	/**
	 * serialversion uid.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The maximum size of the cache before it starts evicting elements.
	 */
	private int maxSize;
	

	/**
	 * Create a new cache.
	 * 
	 * @param maxSize
	 *            the maximum size of the cache before it starts evicting elements.
	 * @param initialCapacity
	 *            pre-allocated capacity for the cache.
	 * @param loadFactor
	 *            the load factor
	 */
	public AccessBasedLRUCache(int maxSize, int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor, true);
		this.maxSize = maxSize;
	}

	// private boolean flag = false;
	// public AccessBasedLRUCache(int maxSize, int initialCapacity, float loadFactor, boolean dump) {
	// 	super(initialCapacity, loadFactor, true);
	// 	this.maxSize = maxSize;
	// 	this.flag = dump;
	// }
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeEldestEntry(Map.Entry<K, V> eldest) {
		return size() > maxSize;
		// if(flag && ret) {
		// 	System.out.println("Remove oldest");
		// }
		// return ret;
	}
}