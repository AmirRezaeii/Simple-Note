package org.example

class AuthManager(private val api: AuthApi = RetrofitClient.authApi) {

    suspend fun login(username: String, password: String): Result<TokenResponse> {
        return try {
            val tokenResp = api.login(LoginRequest(username, password))
            Result.success(tokenResp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        password: String,
        retypePassword: String
    ): Result<Unit> {
        if (password != retypePassword) return Result.failure(Exception("Passwords do not match"))
        return try {
            val resp = api.register(RegisterRequest(firstName, lastName, username, email, password))
            if (resp.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(resp.errorBody()?.string() ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshToken(refreshToken: String): Result<String> {
        return try {
            val resp = api.refreshToken(RefreshTokenRequest(refreshToken))
            Result.success(resp.access)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
