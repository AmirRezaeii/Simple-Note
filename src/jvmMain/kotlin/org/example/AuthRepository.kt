//package org.example
//
//class AuthRepository {
//    private val api = RetrofitClient.authApi
//
//    suspend fun register(
//        firstName: String,
//        lastName: String,
//        username: String,
//        email: String,
//        password: String
//    ): Result<Unit> {
//        val response = api.register(RegisterRequest(firstName, lastName, username, email, password))
//        return if (response.isSuccessful) {
//            Result.success(Unit)
//        } else {
//            Result.failure(Exception(response.errorBody()?.string() ?: "Unknown error"))
//        }
//    }
//
//    suspend fun login(username: String, password: String): Result<AuthResponse> {
//        val response = api.login(LoginRequest(username, password))
//        return if (response.isSuccessful) {
//            response.body()?.let { Result.success(it) }
//                ?: Result.failure(Exception("Empty response"))
//        } else {
//            Result.failure(Exception(response.errorBody()?.string() ?: "Unknown error"))
//        }
//    }
//}