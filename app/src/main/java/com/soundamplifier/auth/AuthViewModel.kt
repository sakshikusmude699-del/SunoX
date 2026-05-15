package com.soundamplifier.auth

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit

class AuthViewModel : ViewModel() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    private val _state = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    val state: StateFlow<AuthState> = _state

    init {
        val user = firebaseAuth.currentUser
        if (user != null) {
            _state.value = AuthState.LoggedIn(user.phoneNumber ?: user.uid)
        }
    }

    fun startOtp(phoneE164: String, activity: Activity) {
        Log.d("OTP_DEBUG", "startOtp called phone=$phoneE164")
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d("OTP_DEBUG", "onVerificationCompleted")
                firebaseAuth.signInWithCredential(credential)
                    .addOnSuccessListener { result ->
                        val user = result.user
                        _state.value = AuthState.LoggedIn(user?.phoneNumber ?: user?.uid ?: phoneE164)
                    }
                    .addOnFailureListener { e ->
                        _state.value = AuthState.Error(e.message ?: "Auto verification failed")
                    }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e("OTP_DEBUG", "onVerificationFailed", e)
                _state.value = AuthState.Error(e.message ?: "Failed to send OTP")
            }

            override fun onCodeSent(
                newVerificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d("OTP_DEBUG", "onCodeSent verificationId=$newVerificationId")
                verificationId = newVerificationId
                resendToken = token
                _state.value = AuthState.OtpSent(
                    phone = phoneE164,
                    sessionId = newVerificationId,
                    expiresInSec = 60
                )
            }
        }

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneE164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        Log.d("OTP_DEBUG", "about to call verifyPhoneNumber")
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(sessionId: String, phone: String, otp: String) {
        Log.d(
            "OTP_DEBUG",
            "verifyOtp called sessionId=$sessionId otpLength=${otp.length}"
        )
        val id = verificationId ?: sessionId
        val credential = PhoneAuthProvider.getCredential(id, otp)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                _state.value = AuthState.LoggedIn(user?.phoneNumber ?: user?.uid ?: phone)
            }
            .addOnFailureListener {
                _state.value = AuthState.Error("Invalid OTP")
                _state.value = AuthState.OtpSent(phone = phone, sessionId = id, expiresInSec = 60)
            }
    }

    fun logout() {
        firebaseAuth.signOut()
        _state.value = AuthState.LoggedOut
    }
}

