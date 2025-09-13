package org.example

class NoteRepository(
    private val api: NoteApiService = RetrofitClient.apiService
) {
    suspend fun fetchNotesPaged(bearerToken: String, page: Int): NoteListResponse {
        return api.getNotes(bearerToken, page)
    }

    suspend fun searchNotes(bearerToken: String, query: String, page: Int): NoteListResponse {
        return api.searchNotes(bearerToken, query, page)
    }

    suspend fun fetchNoteById(bearerToken: String, noteId: Int): Note {
        return api.getNoteById(bearerToken, noteId)
    }

    suspend fun createNote(bearerToken: String, title: String, description: String): Note {
        val request = NoteRequest(title = title, description = description)
        return api.createNote(bearerToken, request)
    }

    suspend fun updateNote(bearerToken: String, noteId: Int, title: String, description: String): Note {
        val request = NoteRequest(title = title, description = description)
        return api.updateNote(bearerToken, noteId, request)
    }

    suspend fun deleteNote(bearerToken: String, noteId: Int): Boolean {
        val response = api.deleteNote(bearerToken, noteId)
        return response.isSuccessful
    }
}
