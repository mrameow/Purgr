package com.purgr.app

import android.content.Context

/**
 * Remembers swipe decisions between app launches, so a half-finished session isn't lost
 * and photos you already kept don't come back next time.
 */
class DecisionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("purgr", Context.MODE_PRIVATE)

    fun kept(): MutableSet<Long> = read(KEPT)
    fun pending(): MutableSet<Long> = read(PENDING)

    fun save(kept: Set<Long>, pending: Set<Long>) {
        prefs.edit()
            .putStringSet(KEPT, kept.mapTo(HashSet()) { it.toString() })
            .putStringSet(PENDING, pending.mapTo(HashSet()) { it.toString() })
            .apply()
    }

    private fun read(key: String): MutableSet<Long> =
        (prefs.getStringSet(key, emptySet()) ?: emptySet()).mapNotNullTo(HashSet()) { it.toLongOrNull() }

    private companion object {
        const val KEPT = "kept"
        const val PENDING = "pending"
    }
}
