package com.soundamplifier.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.soundamplifier.R
import com.soundamplifier.ui.components.GlassCard
import com.soundamplifier.ui.components.SunoXDestructiveTextButton
import com.soundamplifier.ui.theme.SunoXColors
import com.soundamplifier.data.AUDIOGRAM_FREQUENCIES
import com.soundamplifier.data.FirestoreUserRepository
import com.soundamplifier.data.isValidContactPhoneFormat
import com.soundamplifier.data.normalizeFirestoreEarField
import com.soundamplifier.ui.LocaleManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenNavigationDrawer: () -> Unit = {},
    onLogout: () -> Unit,
    onNavigateLogin: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    var firebaseUser by remember { mutableStateOf(auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseUser = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    var displayNameDraft by remember(firebaseUser?.uid, firebaseUser?.displayName) {
        mutableStateOf(firebaseUser?.displayName.orEmpty())
    }

    val firestoreRepo = remember { FirestoreUserRepository() }
    var audiograms by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var audiogramsLoading by remember { mutableStateOf(false) }
    var firestorePhone by remember { mutableStateOf<String?>(null) }
    var phoneDraft by remember { mutableStateOf("") }
    var isEditingProfile by remember { mutableStateOf(false) }
    var nameFieldError by remember { mutableStateOf<String?>(null) }
    var phoneFieldError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(firebaseUser?.uid) {
        val uid = firebaseUser?.uid ?: return@LaunchedEffect
        audiogramsLoading = true
        audiograms = firestoreRepo.getAllAudiograms(uid)
        firestorePhone = firestoreRepo.getStoredPhoneNumber(uid)
        audiogramsLoading = false
    }

    LaunchedEffect(firebaseUser?.uid, firestorePhone, firebaseUser?.phoneNumber) {
        val u = firebaseUser ?: return@LaunchedEffect
        if (u.phoneNumber.isNullOrBlank()) {
            phoneDraft = firestorePhone.orEmpty()
        } else {
            phoneDraft = u.phoneNumber.orEmpty()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenNavigationDrawer) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.open_menu),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = if (firebaseUser == null) Arrangement.Center else Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (firebaseUser == null) {
                Text(
                    text = stringResource(R.string.profile_guest_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(onClick = onNavigateLogin) {
                    Text(stringResource(R.string.profile_login))
                }
            } else {
                val signedInUser = firebaseUser!!
                val authPhoneLinked = !signedInUser.phoneNumber.isNullOrBlank()
                val phoneDisplay = signedInUser.phoneNumber?.takeIf { it.isNotBlank() }
                    ?: firestorePhone?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.profile_not_linked)
                UserInfoCard(
                    isEditing = isEditingProfile,
                    onStartEdit = {
                        nameFieldError = null
                        phoneFieldError = null
                        isEditingProfile = true
                    },
                    onCancelEdit = {
                        displayNameDraft = signedInUser.displayName.orEmpty()
                        phoneDraft = if (signedInUser.phoneNumber.isNullOrBlank()) {
                            firestorePhone.orEmpty()
                        } else {
                            signedInUser.phoneNumber.orEmpty()
                        }
                        nameFieldError = null
                        phoneFieldError = null
                        isEditingProfile = false
                    },
                    onSaveEdit = save@{
                        nameFieldError = null
                        phoneFieldError = null
                        val nameTrim = displayNameDraft.trim()
                        val phoneTrim = phoneDraft.trim()
                        if (nameTrim.isEmpty()) {
                            nameFieldError = context.getString(R.string.profile_error_name_required)
                            return@save
                        }
                        if (!authPhoneLinked) {
                            if (!isValidContactPhoneFormat(phoneTrim)) {
                                phoneFieldError = context.getString(R.string.profile_error_phone_required)
                                return@save
                            }
                        }
                        scope.launch {
                            val user = auth.currentUser ?: return@launch
                            try {
                                val lang = LocaleManager.getSavedLanguageCode(context)
                                if (nameTrim != user.displayName.orEmpty()) {
                                    val request = UserProfileChangeRequest.Builder()
                                        .setDisplayName(nameTrim)
                                        .build()
                                    user.updateProfile(request).await()
                                    user.reload().await()
                                }
                                var updated = auth.currentUser ?: user
                                if (!authPhoneLinked) {
                                    val phoneOk = firestoreRepo.saveUserPhoneNumber(updated.uid, phoneTrim)
                                    if (!phoneOk) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.profile_phone_save_failed),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        return@launch
                                    }
                                }
                                updated = auth.currentUser ?: updated
                                firestoreRepo.createOrUpdateUserProfile(
                                    updated,
                                    lang,
                                    manualPhone = if (!authPhoneLinked) phoneTrim else null,
                                )
                                firestorePhone = firestoreRepo.getStoredPhoneNumber(updated.uid)
                                firebaseUser = updated
                                displayNameDraft = updated.displayName.orEmpty()
                                if (!authPhoneLinked) {
                                    phoneDraft = firestorePhone.orEmpty()
                                }
                                isEditingProfile = false
                                nameFieldError = null
                                phoneFieldError = null
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.profile_name_updated),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    e.message ?: context.getString(R.string.profile_phone_save_failed),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    },
                    displayNameDraft = displayNameDraft,
                    onDisplayNameChange = {
                        displayNameDraft = it
                        nameFieldError = null
                    },
                    nameError = nameFieldError,
                    phoneText = phoneDisplay,
                    phoneEditable = !authPhoneLinked,
                    phoneDraft = phoneDraft,
                    onPhoneDraftChange = {
                        phoneDraft = it
                        phoneFieldError = null
                    },
                    phoneError = phoneFieldError,
                    emailText = signedInUser.email
                        ?: stringResource(R.string.profile_not_linked),
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.profile_hearing_profiles),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                when {
                    audiogramsLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .height(120.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = SunoXColors.Primary)
                        }
                    }
                    audiograms.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.profile_no_audiograms),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f, fill = true),
                        ) {
                            itemsIndexed(
                                audiograms,
                                key = { _, doc ->
                                    "${doc["createdAt"]}_${doc["isActive"]}"
                                },
                            ) { _, doc ->
                                AudiogramSummaryCard(doc = doc)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                SunoXDestructiveTextButton(
                    text = stringResource(R.string.profile_logout),
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun UserInfoCard(
    isEditing: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    displayNameDraft: String,
    onDisplayNameChange: (String) -> Unit,
    nameError: String?,
    phoneText: String,
    phoneEditable: Boolean,
    phoneDraft: String,
    onPhoneDraftChange: (String) -> Unit,
    phoneError: String?,
    emailText: String,
) {
    val shape = RoundedCornerShape(20.dp)
    val mutedReadOnly = MaterialTheme.colorScheme.onSurfaceVariant
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = shape) {
        Column(Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isEditing) {
                TextButton(onClick = onStartEdit) {
                    Text(
                        stringResource(R.string.profile_edit_profile),
                        color = SunoXColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.profile_display_name),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (isEditing) {
            OutlinedTextField(
                value = displayNameDraft,
                onValueChange = onDisplayNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                placeholder = {
                    Text(stringResource(R.string.profile_name_placeholder))
                },
            )
        } else {
            Text(
                text = displayNameDraft.ifBlank { stringResource(R.string.profile_name_placeholder) },
                style = MaterialTheme.typography.bodyLarge,
                color = if (displayNameDraft.isBlank()) mutedReadOnly else MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.profile_phone),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (isEditing && phoneEditable) {
            OutlinedTextField(
                value = phoneDraft,
                onValueChange = onPhoneDraftChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = phoneError != null,
                supportingText = phoneError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                placeholder = {
                    Text(stringResource(R.string.profile_phone_placeholder))
                },
            )
        } else {
            Text(
                text = phoneText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.profile_email),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = emailText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (isEditing) {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onCancelEdit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SunoXColors.Primary),
                ) {
                    Text(stringResource(R.string.profile_cancel))
                }
                Button(
                    onClick = onSaveEdit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SunoXColors.Primary,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(stringResource(R.string.profile_save))
                }
            }
        }
        }
    }
}

@Composable
private fun AudiogramSummaryCard(doc: Map<String, Any>) {
    val createdRaw = doc["createdAt"]
    val dateStr = formatFirestoreDate(createdRaw)
    val leftAvg = earAverageDb(doc["leftEar"])
    val rightAvg = earAverageDb(doc["rightEar"])
    val isActive = doc["isActive"] as? Boolean == true
    val shape = RoundedCornerShape(16.dp)
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = shape, elevationShadow = 6.dp) {
        Column(Modifier.padding(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dateStr,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (isActive) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(SunoXColors.Primary.copy(alpha = 0.18f))
                        .border(1.dp, SunoXColors.Primary.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.profile_active_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = SunoXColors.Primary,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${stringResource(R.string.profile_left_avg)}: ${leftAvg} dB",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${stringResource(R.string.profile_right_avg)}: ${rightAvg} dB",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        }
    }
}

private fun formatFirestoreDate(raw: Any?): String {
    val date: Date? = when (raw) {
        is Timestamp -> raw.toDate()
        is Date -> raw
        else -> null
    }
    if (date == null) return "—"
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(date)
}

private fun earAverageDb(raw: Any?): Int {
    val map = normalizeFirestoreEarField(raw) ?: return 0
    val values = AUDIOGRAM_FREQUENCIES.map { hz -> map[hz.toString()] }.filterNotNull()
    if (values.isEmpty()) return 0
    return values.average().roundToInt()
}
