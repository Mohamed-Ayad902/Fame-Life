package org.mayad.fame_life.core.platform

/** Small key-value text storage for local records (profile, history, notifications). */
interface TextStore {
    fun read(key: String): String?
    fun write(key: String, value: String)
    fun remove(key: String)
}

class InMemoryTextStore : TextStore {
    private val values = mutableMapOf<String, String>()
    override fun read(key: String): String? = values[key]
    override fun write(key: String, value: String) {
        values[key] = value
    }
    override fun remove(key: String) {
        values.remove(key)
    }
}
