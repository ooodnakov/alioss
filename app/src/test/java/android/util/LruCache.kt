package android.util

/**
 * Minimal implementation of [android.util.LruCache] for JVM unit tests.
 */
open class LruCache<K, V>(private val maxSize: Int) {
    private val entries = LinkedHashMap<K, V>(0, 0.75f, true)
    private var currentSize = 0

    init {
        require(maxSize > 0) { "maxSize must be positive" }
    }

    @Synchronized
    open fun get(key: K): V? = entries[key]

    @Synchronized
    open fun put(key: K, value: V): V? {
        val previous = entries.put(key, value)
        if (previous != null) {
            currentSize -= safeSizeOf(key, previous)
        }
        currentSize += safeSizeOf(key, value)
        trimToSize(maxSize)
        return previous
    }

    @Synchronized
    open fun evictAll() {
        entries.clear()
        currentSize = 0
    }

    protected open fun sizeOf(key: K, value: V): Int = 1

    private fun trimToSize(maxSize: Int) {
        val iterator = entries.entries.iterator()
        while (currentSize > maxSize && iterator.hasNext()) {
            val entry = iterator.next()
            currentSize -= safeSizeOf(entry.key, entry.value)
            iterator.remove()
        }
    }

    private fun safeSizeOf(key: K, value: V): Int {
        val size = sizeOf(key, value)
        require(size >= 0) { "Negative size: key=$key value=$value" }
        return size
    }
}
