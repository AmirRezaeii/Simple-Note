package org.example

class SettingsManager(
    private val userRepository: UserRepository = UserRepository()
) {
    suspend fun getUserInfo(token: String): Result<UserInfo> {
        return userRepository.getUserInfo(token)
    }

    suspend fun changePassword(
        token: String,
        currentPassword: String,
        newPassword: String,
        retypePassword: String
    ): Result<Unit> {
        if (newPassword != retypePassword) {
            return Result.failure(Exception("New passwords do not match"))
        }
        return userRepository.changePassword(token, currentPassword, newPassword)
    }

    fun logout(): Result<Unit> {
        return Result.success(Unit)
    }
}
