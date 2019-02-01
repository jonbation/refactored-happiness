package com.orgzly.android.db.entity

import androidx.room.Embedded
import com.orgzly.android.repos.VersionedRook

data class BookView(
        @Embedded
        val book: Book,

        val noteCount: Int,

        val linkedTo: String? = null,

        @Embedded(prefix = "synced_to_")
        val syncedTo: VersionedRook? = null
) {
    fun hasLink(): Boolean {
        return linkedTo != null
    }

    fun hasSync(): Boolean {
        return syncedTo != null
    }

    fun isOutOfSync(): Boolean {
        return syncedTo != null && book.isModified
    }
}