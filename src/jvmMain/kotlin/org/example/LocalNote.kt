package org.example

data class LocalNote(
    val localId: Int,
    val serverId: Int?,      // remote ID from API (nullable if not yet synced)
    val title: String,
    val content: String,
    val createdAt: String?,  // optional ISO timestamps from server
    val updatedAt: String?,
    val isSynced: Boolean
)