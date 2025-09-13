package org.example

import retrofit2.Response
import retrofit2.http.*

interface NoteApiService {
    @GET("notes/")
    suspend fun getNotes(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1
    ): NoteListResponse

    @GET("notes/filter/")
    suspend fun searchNotes(
        @Header("Authorization") token: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): NoteListResponse

    @GET("notes/{id}/")
    suspend fun getNoteById(
        @Header("Authorization") token: String,
        @Path("id") noteId: Int
    ): Note

    @POST("notes/")
    suspend fun createNote(
        @Header("Authorization") token: String,
        @Body note: NoteRequest
    ): Note

    @PUT("notes/{id}/")
    suspend fun updateNote(
        @Header("Authorization") token: String,
        @Path("id") noteId: Int,
        @Body note: NoteRequest
    ): Note

    @DELETE("notes/{id}/")
    suspend fun deleteNote(
        @Header("Authorization") token: String,
        @Path("id") noteId: Int
    ): Response<Unit>
}
