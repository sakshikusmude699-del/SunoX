package com.soundamplifier.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.soundamplifier.R
import com.soundamplifier.ui.theme.SunoXColors
import kotlinx.coroutines.launch

private val DrawerBackgroundLight = Color.White
private val DrawerBackgroundDark = Color(0xFF0F172A)

@Composable
fun AppDrawerLayout(
    drawerState: DrawerState,
    drawerGesturesEnabled: Boolean = true,
    currentRoute: String?,
    onDrawerNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    content: @Composable (onOpenDrawer: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    ModalNavigationDrawer(
        modifier = Modifier.fillMaxSize(),
        drawerState = drawerState,
        gesturesEnabled = drawerGesturesEnabled,
        scrimColor = Color.Black.copy(alpha = 0.52f),
        drawerContent = {
            val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
            val drawerBg = if (isLight) DrawerBackgroundLight else DrawerBackgroundDark
            val contentColor = if (isLight) SunoXColors.OnLightBackground else Color.White

            ModalDrawerSheet(
                drawerShape = RectangleShape,
                drawerContainerColor = drawerBg,
                drawerContentColor = contentColor,
                drawerTonalElevation = 0.dp,
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                modifier = Modifier
                    .widthIn(max = DrawerDefaults.MaximumDrawerWidth)
                    .fillMaxHeight(),
            ) {
                AppDrawerSheetContent(
                    user = user,
                    currentRoute = currentRoute,
                    onItemClick = { id ->
                        scope.launch { drawerState.close() }
                        when (id) {
                            "logout" -> onLogout()
                            else -> onDrawerNavigate(id)
                        }
                    },
                    sheetBackground = drawerBg,
                )
            }
        },
    ) {
        content { scope.launch { drawerState.open() } }
    }
}

@Composable
private fun AppDrawerSheetContent(
    user: FirebaseUser?,
    currentRoute: String?,
    onItemClick: (String) -> Unit,
    sheetBackground: Color,
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val name = user?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.email?.substringBefore("@").orEmpty().ifBlank { null }
        ?: stringResource(R.string.app_name)
    val email = user?.email.orEmpty()

    val unselectedIconText = if (isLight) {
        Color(0xFF5F5F6B)
    } else {
        Color(0xFFC8C9D4)
    }
    val headerPrimary = if (isLight) SunoXColors.OnLightBackground else Color.White
    val headerMuted = if (isLight) Color(0xFF6B6B76) else Color(0xFF9CA3AF)

    val selectedContainer = SunoXColors.Primary.copy(alpha = if (isLight) 0.14f else 0.22f)
    val accent = SunoXColors.PrimaryDark

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(sheetBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SunoXColors.Primary.copy(alpha = if (isLight) 0.18f else 0.28f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = accent,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = headerPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (email.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = headerMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = if (isLight) Color(0xFFE8E8EE) else Color.White.copy(alpha = 0.12f),
        )

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
        ) {
            DrawerFlatNavRow(
                selected = currentRoute == "home",
                onClick = { onItemClick("home") },
                icon = Icons.Rounded.Home,
                label = stringResource(R.string.nav_home),
                selectedContainer = selectedContainer,
                accent = accent,
                unselectedColor = unselectedIconText,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(12.dp))
            DrawerFlatNavRow(
                selected = currentRoute == "amplifier",
                onClick = { onItemClick("amplifier") },
                icon = Icons.Rounded.Hearing,
                label = stringResource(R.string.nav_amplifier),
                selectedContainer = selectedContainer,
                accent = accent,
                unselectedColor = unselectedIconText,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(12.dp))
            DrawerFlatNavRow(
                selected = currentRoute == "presets",
                onClick = { onItemClick("presets") },
                icon = Icons.Rounded.Tune,
                label = stringResource(R.string.nav_presets),
                selectedContainer = selectedContainer,
                accent = accent,
                unselectedColor = unselectedIconText,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(12.dp))
            DrawerFlatNavRow(
                selected = currentRoute == "audiogram",
                onClick = { onItemClick("audiogram") },
                icon = Icons.AutoMirrored.Rounded.Assignment,
                label = stringResource(R.string.hearing_test_title),
                selectedContainer = selectedContainer,
                accent = accent,
                unselectedColor = unselectedIconText,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(12.dp))
            DrawerFlatNavRow(
                selected = currentRoute == "profile",
                onClick = { onItemClick("profile") },
                icon = Icons.Rounded.Person,
                label = stringResource(R.string.profile_title),
                selectedContainer = selectedContainer,
                accent = accent,
                unselectedColor = unselectedIconText,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(12.dp))
            DrawerFlatNavRow(
                selected = currentRoute == "settings",
                onClick = { onItemClick("settings") },
                icon = Icons.Rounded.Settings,
                label = stringResource(R.string.nav_settings),
                selectedContainer = selectedContainer,
                accent = accent,
                unselectedColor = unselectedIconText,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(12.dp))
            DrawerFlatNavRow(
                selected = currentRoute == "faq",
                onClick = { onItemClick("faq") },
                icon = Icons.AutoMirrored.Rounded.HelpOutline,
                label = stringResource(R.string.nav_faq),
                selectedContainer = selectedContainer,
                accent = accent,
                unselectedColor = unselectedIconText,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(16.dp))
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = if (isLight) Color(0xFFE8E8EE) else Color.White.copy(alpha = 0.12f),
        )

        Spacer(Modifier.height(8.dp))

        DrawerLogoutRow(
            onClick = { onItemClick("logout") },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DrawerFlatNavRow(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    selectedContainer: Color,
    accent: Color,
    unselectedColor: Color,
    shape: RoundedCornerShape,
) {
    val interaction = remember { MutableInteractionSource() }
    val bg = if (selected) selectedContainer else Color.Transparent
    val tint = if (selected) accent else unselectedColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = rememberRipple(bounded = true, color = accent.copy(alpha = 0.25f)),
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = tint,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DrawerLogoutRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interaction,
                indication = rememberRipple(bounded = true, color = SunoXColors.ErrorText.copy(alpha = 0.2f)),
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Logout,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = SunoXColors.ErrorText,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = stringResource(R.string.profile_logout),
            style = MaterialTheme.typography.bodyLarge,
            color = SunoXColors.ErrorText,
        )
    }
}
