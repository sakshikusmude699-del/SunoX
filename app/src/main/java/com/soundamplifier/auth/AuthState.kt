package com.soundamplifier.auth

sealed class AuthState {
    data object LoggedOut : AuthState()
    data class LoggedIn(val label: String) : AuthState()
    data class Error(val message: String) : AuthState()
    data class OtpSent(
        val phone: String,
        val sessionId: String,
        val expiresInSec: Int,
    ) : AuthState()
}
