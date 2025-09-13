package org.example

class UserRepository(
    private val api: UserApi = RetrofitClient.userApi
) {
    suspend fun getUserInfo(token: String): Result<UserInfo> {
        return try {
            val info = api.getUserInfo(token)
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(token: String, current: String, new: String): Result<Unit> {
        val response = api.changePassword(token, ChangePasswordRequest(current, new))
        return if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(response.errorBody()?.string() ?: "Unknown error"))
        }
    }
}
