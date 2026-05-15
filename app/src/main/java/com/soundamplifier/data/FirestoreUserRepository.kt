package com.soundamplifier.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreUserRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun createOrUpdateUserProfile(
        user: FirebaseUser,
        language: String,
        /** Collected in onboarding when Auth is Google/email-only (Auth [phoneNumber] stays null). */
        manualPhone: String? = null,
    ) {
        try {
            val uid = user.uid
            val docRef = db.collection(COL_USERS).document(uid)
            val snapshot = docRef.get().await()
            val manual = manualPhone?.trim()?.takeIf { it.isNotEmpty() }
            val data = mutableMapOf<String, Any?>(
                KEY_EMAIL to user.email,
                KEY_DISPLAY_NAME to user.displayName,
                KEY_LAST_LOGIN to FieldValue.serverTimestamp(),
                KEY_UPDATED_AT to FieldValue.serverTimestamp(),
                KEY_LANGUAGE to language,
            )
            user.photoUrl?.toString()?.takeIf { it.isNotEmpty() }?.let { data[KEY_PHOTO_URL] = it }
            when {
                !user.phoneNumber.isNullOrBlank() -> {
                    data[KEY_PHONE] = user.phoneNumber
                    data[KEY_PHONE_NUMBER] = user.phoneNumber
                }
                manual != null -> {
                    data[KEY_PHONE] = manual
                    data[KEY_PHONE_NUMBER] = manual
                }
                snapshot.exists() -> {
                    val existing =
                        readPhoneField(snapshot.get(KEY_PHONE))
                            ?: readPhoneField(snapshot.get(KEY_PHONE_NUMBER))
                    if (!existing.isNullOrBlank()) {
                        data[KEY_PHONE] = existing
                        data[KEY_PHONE_NUMBER] = existing
                    }
                }
            }
            if (!snapshot.exists()) {
                data[KEY_CREATED_AT] = FieldValue.serverTimestamp()
            }
            docRef.set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "createOrUpdateUserProfile failed", e)
        }
    }

    suspend fun saveAudiogram(
        uid: String,
        leftEar: Map<String, Int>,
        rightEar: Map<String, Int>,
    ) {
        try {
            val col = db.collection(COL_USERS).document(uid).collection(SUB_AUDIOGRAMS)
            val activeSnap = col.whereEqualTo(KEY_IS_ACTIVE, true).get().await()
            val newRef = col.document()
            val batch = db.batch()
            for (doc in activeSnap.documents) {
                batch.update(doc.reference, KEY_IS_ACTIVE, false)
            }
            batch.set(
                newRef,
                mapOf(
                    KEY_CREATED_AT to FieldValue.serverTimestamp(),
                    KEY_LEFT_EAR to leftEar,
                    KEY_RIGHT_EAR to rightEar,
                    KEY_IS_ACTIVE to true,
                ),
            )
            batch.commit().await()
        } catch (e: Exception) {
            Log.e(TAG, "saveAudiogram failed", e)
        }
    }

    suspend fun getAllAudiograms(uid: String): List<Map<String, Any>> {
        return try {
            val col = db.collection(COL_USERS).document(uid).collection(SUB_AUDIOGRAMS)
            val snap = col.orderBy(KEY_CREATED_AT, Query.Direction.DESCENDING).get().await()
            snap.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e(TAG, "getAllAudiograms failed", e)
            emptyList()
        }
    }

    /** True if `users/{uid}/audiograms` has at least one document. On failure, returns true (fail-open). */
    suspend fun hasAnyAudiogram(uid: String): Boolean {
        return try {
            val col = db.collection(COL_USERS).document(uid).collection(SUB_AUDIOGRAMS)
            val snap = col.limit(1).get().await()
            snap.documents.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "hasAnyAudiogram failed", e)
            true
        }
    }

    /** Phone saved on the user doc (e.g. after Google sign-in onboarding); Auth may still be null. */
    suspend fun getStoredPhoneNumber(uid: String): String? {
        return try {
            val doc = db.collection(COL_USERS).document(uid).get().await()
            if (!doc.exists()) return null
            readPhoneField(doc.get(KEY_PHONE))
                ?: readPhoneField(doc.get(KEY_PHONE_NUMBER))
        } catch (e: Exception) {
            Log.e(TAG, "getStoredPhoneNumber failed", e)
            null
        }
    }

    /**
     * Google-only accounts must have a contact number in Auth **or** in Firestore (`phone` / `phoneNumber`).
     * At least 10 digits; `+` and spacing allowed.
     */
    suspend fun userHasValidContactPhone(user: FirebaseUser): Boolean {
        if (!user.phoneNumber.isNullOrBlank() &&
            user.phoneNumber!!.filter { it.isDigit() }.length >= 10
        ) {
            return true
        }
        val stored = getStoredPhoneNumber(user.uid) ?: return false
        return isValidContactPhoneFormat(stored)
    }

    /**
     * Persists [phone] on `users/{uid}` for email/Google accounts where [FirebaseUser.getPhoneNumber] is null.
     * Call from profile or onboarding after the user enters their number.
     */
    suspend fun saveUserPhoneNumber(uid: String, phone: String): Boolean {
        val trimmed = phone.trim()
        if (trimmed.isEmpty()) return false
        return try {
            db.collection(COL_USERS).document(uid)
                .set(
                    mapOf(
                        KEY_PHONE to trimmed,
                        KEY_PHONE_NUMBER to trimmed,
                        KEY_LAST_LOGIN to FieldValue.serverTimestamp(),
                        KEY_UPDATED_AT to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "saveUserPhoneNumber failed", e)
            false
        }
    }

    suspend fun saveCustomPreset(uid: String, preset: CustomPreset) {
        try {
            val docId = preset.id.toString()
            val ref = db.collection(COL_USERS).document(uid).collection(SUB_CUSTOM_PRESETS).document(docId)
            ref.set(
                mapOf(
                    KEY_CP_NAME to preset.name,
                    KEY_CP_BOOST to preset.boostQuietSounds,
                    KEY_CP_MASTER to preset.masterGain,
                    KEY_CP_LOW to preset.lowBoostDb,
                    KEY_CP_HIGH to preset.highBoostDb,
                    KEY_CP_CREATED_AT to preset.createdAt,
                    KEY_CP_ICON to preset.iconKey,
                    KEY_CP_BUILT_IN to (preset.builtInPresetId ?: ""),
                ),
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "saveCustomPreset failed", e)
        }
    }

    suspend fun deleteCustomPreset(uid: String, presetId: Int) {
        try {
            db.collection(COL_USERS).document(uid).collection(SUB_CUSTOM_PRESETS)
                .document(presetId.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "deleteCustomPreset failed", e)
        }
    }

    /**
     * Creates a new login session token and stores it on `users/{uid}` (atomic transaction when possible).
     * Any other device still holding the previous token becomes invalid on the next server read or snapshot.
     */
    suspend fun createSessionToken(uid: String): String {
        val newToken = UUID.randomUUID().toString()
        val ref = db.collection(COL_USERS).document(uid)
        return try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(ref)
                val version = (snapshot.getLong(KEY_SESSION_VERSION) ?: 0L) + 1L
                transaction.set(
                    ref,
                    mapOf(
                        KEY_SESSION_TOKEN to newToken,
                        KEY_SESSION_VERSION to version,
                        KEY_SESSION_UPDATED_AT to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
                newToken
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "createSessionToken transaction failed, using direct write", e)
            try {
                ref.update(
                    mapOf(
                        KEY_SESSION_TOKEN to newToken,
                        KEY_SESSION_UPDATED_AT to FieldValue.serverTimestamp(),
                    ),
                ).await()
            } catch (e2: Exception) {
                ref.set(
                    mapOf(
                        KEY_SESSION_TOKEN to newToken,
                        KEY_SESSION_UPDATED_AT to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                ).await()
            }
            newToken
        }
    }

    /** True if [expectedToken] is already stored on the server (use after login to avoid races). */
    suspend fun confirmSessionTokenOnServer(uid: String, expectedToken: String): Boolean {
        return try {
            val snap = db.collection(COL_USERS).document(uid).get(Source.SERVER).await()
            snap.getString(KEY_SESSION_TOKEN) == expectedToken
        } catch (e: Exception) {
            Log.e(TAG, "confirmSessionTokenOnServer failed", e)
            false
        }
    }

    /**
     * Server-backed check (not cache-only). Returns `null` if the read failed (caller should not sign out).
     * Returns `false` if the server token does not match [localToken] (confirmed other session / stale).
     */
    suspend fun isSessionValid(uid: String, localToken: String): Boolean? {
        if (localToken.isEmpty()) return true
        return try {
            val snap = db.collection(COL_USERS).document(uid).get(Source.SERVER).await()
            if (!snap.exists()) return false
            val remote = snap.getString(KEY_SESSION_TOKEN) ?: return false
            remote == localToken
        } catch (e: Exception) {
            Log.e(TAG, "isSessionValid failed", e)
            null
        }
    }

    private fun readPhoneField(raw: Any?): String? =
        when (raw) {
            is String -> raw.trim().takeIf { it.isNotEmpty() }
            is Number -> raw.toString().trim().takeIf { it.isNotEmpty() }
            else -> null
        }

    private companion object {
        const val TAG = "FirestoreRepo"
        const val COL_USERS = "users"
        const val SUB_AUDIOGRAMS = "audiograms"
        const val SUB_CUSTOM_PRESETS = "custom_presets"

        const val KEY_PHONE = "phone"
        const val KEY_PHONE_NUMBER = "phoneNumber"
        const val KEY_EMAIL = "email"
        const val KEY_DISPLAY_NAME = "displayName"
        const val KEY_PHOTO_URL = "photoURL"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_UPDATED_AT = "updatedAt"
        const val KEY_LAST_LOGIN = "lastLoginAt"
        const val KEY_LANGUAGE = "language"
        const val KEY_LEFT_EAR = "leftEar"
        const val KEY_RIGHT_EAR = "rightEar"
        const val KEY_IS_ACTIVE = "isActive"

        const val KEY_CP_NAME = "name"
        const val KEY_CP_BOOST = "boostQuietSounds"
        const val KEY_CP_MASTER = "masterGain"
        const val KEY_CP_LOW = "lowBoostDb"
        const val KEY_CP_HIGH = "highBoostDb"
        const val KEY_CP_CREATED_AT = "createdAt"
        const val KEY_CP_ICON = "iconKey"
        const val KEY_CP_BUILT_IN = "builtInPresetId"

        const val KEY_SESSION_TOKEN = "sessionToken"
        const val KEY_SESSION_VERSION = "sessionVersion"
        const val KEY_SESSION_UPDATED_AT = "sessionUpdatedAt"
    }
}

/** At least 10 digits; allows leading `+`, spaces, and punctuation in [raw]. */
fun isValidContactPhoneFormat(raw: String): Boolean =
    raw.trim().filter { it.isDigit() }.length >= 10

/** Build [CustomPreset] from Firestore row (optional use for future sync). */
fun Map<String, Any>.customPresetFromFirestoreMap(): CustomPreset? {
    val id = (this["id"] as? Number)?.toInt() ?: return null
    val name = this["name"] as? String ?: return null
    val boost = (this["boostQuietSounds"] as? Number)?.toFloat() ?: return null
    val master = (this["masterGain"] as? Number)?.toFloat() ?: return null
    val low = (this["lowBoostDb"] as? Number)?.toFloat() ?: return null
    val high = (this["highBoostDb"] as? Number)?.toFloat() ?: return null
    val created = when (val raw = this["createdAt"]) {
        is Timestamp -> raw.toDate().time
        is Number -> raw.toLong()
        else -> System.currentTimeMillis()
    }
    val icon = (this["iconKey"] as? String)?.takeIf { it.isNotEmpty() } ?: "tune"
    val builtIn = (this["builtInPresetId"] as? String)?.takeIf { it.isNotEmpty() }
    return CustomPreset(
        id = id,
        accountId = AccountLocalIds.GUEST,
        name = name,
        boostQuietSounds = boost,
        masterGain = master,
        lowBoostDb = low,
        highBoostDb = high,
        createdAt = created,
        iconKey = icon,
        builtInPresetId = builtIn,
    )
}
