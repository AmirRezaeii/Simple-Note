package org.example

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/token/")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @POST("auth/register/")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("auth/token/refresh/")
    suspend fun refreshToken(@Body body: RefreshTokenRequest): RefreshTokenResponse
}
