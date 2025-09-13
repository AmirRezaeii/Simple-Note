package org.example

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object NotesTable : Table("notes") {
    val localId = integer("local_id").autoIncrement()
    val serverId = integer("server_id").nullable().uniqueIndex()
    val title = varchar("title", 512)
    val content = text("content")
    val createdAt = varchar("created_at", 64).nullable()
    val updatedAt = varchar("updated_at", 64).nullable()
    val isSynced = bool("is_synced").default(false)

    override val primaryKey = PrimaryKey(localId)
}

class LocalNoteDao {

    suspend fun insert(note: LocalNote): Int = newSuspendedTransaction(Dispatchers.IO) {
        val insertStatement = NotesTable.insert {
            it[serverId] = note.serverId
            it[title] = note.title
            it[content] = note.content
            it[createdAt] = note.createdAt
            it[updatedAt] = note.updatedAt
            it[isSynced] = note.isSynced
        }
        insertStatement[NotesTable.localId]
    }

    suspend fun getAll(): List<LocalNote> = newSuspendedTransaction(Dispatchers.IO) {
        NotesTable.selectAll().orderBy(NotesTable.localId, SortOrder.DESC).map {
            LocalNote(
                localId = it[NotesTable.localId],
                serverId = it[NotesTable.serverId],
                title = it[NotesTable.title],
                content = it[NotesTable.content],
                createdAt = it[NotesTable.createdAt],
                updatedAt = it[NotesTable.updatedAt],
                isSynced = it[NotesTable.isSynced]
            )
        }
    }

    suspend fun getByLocalId(id: Int): LocalNote? = newSuspendedTransaction(Dispatchers.IO) {
        NotesTable.select { NotesTable.localId eq id }
            .map {
                LocalNote(
                    localId = it[NotesTable.localId],
                    serverId = it[NotesTable.serverId],
                    title = it[NotesTable.title],
                    content = it[NotesTable.content],
                    createdAt = it[NotesTable.createdAt],
                    updatedAt = it[NotesTable.updatedAt],
                    isSynced = it[NotesTable.isSynced]
                )
            }
            .singleOrNull()
    }

    suspend fun updateByLocalId(id: Int, newTitle: String, newContent: String): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        NotesTable.update({ NotesTable.localId eq id }) {
            it[title] = newTitle
            it[content] = newContent
            it[isSynced] = false // mark unsynced until sync happens
            it[updatedAt] = java.time.Instant.now().toString()
        } > 0
    }

    // Corrected delete: renamed parameter to `id` to avoid shadowing issues
//    suspend fun deleteByLocalId(id: Int): Boolean = newSuspendedTransaction(Dispatchers.IO) {
//        NotesTable.deleteWhere { NotesTable.localId eq id } > 0
//    }

    suspend fun clearAll(): Unit = newSuspendedTransaction(Dispatchers.IO) {
        NotesTable.deleteAll()
    }

    // helper to fetch unsynced notes for SyncManager
    suspend fun getUnsynced(): List<LocalNote> = newSuspendedTransaction(Dispatchers.IO) {
        NotesTable.select { NotesTable.isSynced eq false }.map {
            LocalNote(
                localId = it[NotesTable.localId],
                serverId = it[NotesTable.serverId],
                title = it[NotesTable.title],
                content = it[NotesTable.content],
                createdAt = it[NotesTable.createdAt],
                updatedAt = it[NotesTable.updatedAt],
                isSynced = it[NotesTable.isSynced]
            )
        }
    }

    suspend fun markSynced(localId: Int, serverId: Int, createdAt: String?, updatedAt: String?) = newSuspendedTransaction(Dispatchers.IO) {
        NotesTable.update({ NotesTable.localId eq localId }) {
            it[NotesTable.serverId] = serverId
            it[NotesTable.isSynced] = true
            it[NotesTable.createdAt] = createdAt
            it[NotesTable.updatedAt] = updatedAt
        }
    }
}
