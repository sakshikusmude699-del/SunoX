package com.soundamplifier.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.soundamplifier.R
import com.soundamplifier.data.FirestoreUserRepository
import com.soundamplifier.data.isValidContactPhoneFormat
import com.soundamplifier.ui.LocaleManager
import com.soundamplifier.ui.components.GlassCard
import com.soundamplifier.ui.components.SunoXPrimaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PhoneRequiredScreen(
    firestoreUserRepository: FirestoreUserRepository,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
    var phoneText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_phone_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.onboarding_phone_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = phoneText,
                    onValueChange = {
                        phoneText = it
                        errorText = null
                    },
                    label = { Text(stringResource(R.string.onboarding_phone_label)) },
                    placeholder = { Text(stringResource(R.string.onboarding_phone_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = errorText != null,
                    supportingText = if (errorText != null) {
                        { Text(errorText!!, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(modifier = Modifier.height(24.dp))
                SunoXPrimaryButton(
                    text = stringResource(R.string.onboarding_phone_continue),
                    onClick = phoneSubmit@{
                        errorText = null
                        val trimmed = phoneText.trim()
                        if (!isValidContactPhoneFormat(trimmed)) {
                            errorText = context.getString(R.string.onboarding_phone_error_invalid)
                            return@phoneSubmit
                        }
                        val user = auth.currentUser
                        if (user == null) {
                            errorText = context.getString(R.string.onboarding_phone_error_save)
                            return@phoneSubmit
                        }
                        busy = true
                        scope.launch {
                            val lang = LocaleManager.getSavedLanguageCode(context)
                            val saved = withContext(Dispatchers.IO) {
                                val writeOk = firestoreUserRepository.saveUserPhoneNumber(user.uid, trimmed)
                                if (!writeOk) return@withContext false
                                firestoreUserRepository.createOrUpdateUserProfile(
                                    user,
                                    lang,
                                    manualPhone = trimmed,
                                )
                                true
                            }
                            busy = false
                            if (saved) {
                                onSuccess()
                            } else {
                                errorText = context.getString(R.string.onboarding_phone_error_save)
                            }
                        }
                    },
                    enabled = !busy,
                    height = 52.dp,
                )
            }
        }
    }
}
