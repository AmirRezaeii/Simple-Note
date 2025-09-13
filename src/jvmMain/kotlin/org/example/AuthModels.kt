package org.example

data class RegisterRequest(
    val first_name: String,
    val last_name: String,
    val username: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class AuthResponse(
    val access: String,
    val refresh: String
)

data class ErrorResponse(
    val detail: String?
)

data class TokenResponse(
    val access: String,
    val refresh: String
)

data class RefreshTokenRequest(val refresh: String)
data class RefreshTokenResponse(val access: String)
