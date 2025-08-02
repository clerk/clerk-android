/**
 * Extension function to create an unmodifiable copy of a map.
 *
 * This function creates a new immutable map containing all the key-value pairs from the original
 * map. The returned map cannot be modified, providing a safe way to share map data without risk of
 * external modification.
 *
 * @param K The type of keys in the map
 * @param V The type of values in the map
 * @return An unmodifiable map containing all entries from the original map
 */
internal fun <K, V> Map<K, V>.toUnmodifiableMap() = buildMap { putAll(this@toUnmodifiableMap) }
