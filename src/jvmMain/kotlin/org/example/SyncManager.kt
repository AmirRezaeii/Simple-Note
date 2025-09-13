//package org.example
//
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//
//class SyncManager(
//    private val api: NoteApiService = RetrofitClient.apiService,
//    private val dao: LocalNoteDao = LocalNoteDao()
//) {
//    /**
//     * Upload unsynced notes. Must be called when online (accessToken available).
//     */
//    suspend fun syncUnsynced(accessToken: String) = withContext(Dispatchers.IO) {
//        val unsynced = dao.getUnsynced()
//        for (note in unsynced) {
//            try {
//                if (note.serverId == null) {
//                    // create on server
//                    val created = api.createNote(accessToken, NoteRequest(note.title, note.description))
//                    dao.markSynced(note.localId, created.id, created.created_at, created.updated_at)
//                } else {
//                    // already has server id — update server
//                    val updated = api.updateNote(accessToken, note.serverId, NoteRequest(note.title, note.description))
//                    dao.markSynced(note.localId, updated.id, updated.created_at, updated.updated_at)
//                }
//            } catch (e: Exception) {
//                // keep unsynced if API fails (network / server)
//            }
//        }
//    }
//}
