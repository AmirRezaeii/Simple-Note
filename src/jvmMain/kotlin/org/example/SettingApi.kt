package org.example

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class ChangePasswordRequest(
    val old_password: String,
    val new_password: String
)

data class UserInfo(
    val id: Int,
    val username: String,
    val email: String,
    val first_name: String,
    val last_name: String
)

interface UserApi {
    @POST("auth/change-password/")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<Unit>

    @GET("auth/userinfo/")
    suspend fun getUserInfo(
        @Header("Authorization") token: String
    ): UserInfo
}
