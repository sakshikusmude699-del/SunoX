package com.soundamplifier.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.soundamplifier.R
import com.soundamplifier.ui.LocaleManager
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onBack: () -> Unit,
    onOpenNavigationDrawer: () -> Unit = {},
    onOpenFaq: () -> Unit = {},
    lastTestedMillis: Long? = null,
    onRetakeHearingTest: () -> Unit = {},
) {
    val scroll = rememberScrollState()
    val options = remember {
        listOf(
            LocaleManager.LANG_EN to R.string.language_english,
            LocaleManager.LANG_HI to R.string.language_hindi,
            LocaleManager.LANG_MR to R.string.language_marathi,
            LocaleManager.LANG_TA to R.string.language_tamil,
            LocaleManager.LANG_TE to R.string.language_telugu,
            LocaleManager.LANG_GU to R.string.language_gujarati
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenNavigationDrawer) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.open_menu)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_hearing_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (lastTestedMillis != null) {
                Text(
                    text = stringResource(
                        R.string.settings_last_tested,
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(lastTestedMillis))
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Button(onClick = onRetakeHearingTest, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Hearing, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_retake_test))
            }
            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.settings_language_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_language_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            val languageLabelColor = MaterialTheme.colorScheme.onSurface
            options.forEach { (code, labelRes) ->
                SettingsLanguageOptionRow(
                    labelRes = labelRes,
                    selected = code == currentLanguageCode,
                    labelColor = languageLabelColor,
                    onSelect = { onLanguageSelected(code) },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onOpenFaq) {
                Text(
                    text = stringResource(R.string.nav_faq),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SettingsLanguageOptionRow(
    @StringRes labelRes: Int,
    selected: Boolean,
    labelColor: Color,
    onSelect: () -> Unit,
) {
    val rowInteraction = remember(labelRes) { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = rowInteraction,
                indication = rememberRipple(bounded = true, color = labelColor),
                role = Role.RadioButton,
                onClick = { if (!selected) onSelect() },
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = { if (!selected) onSelect() },
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = labelColor.copy(alpha = 0.75f),
            ),
        )
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
