package com.soundamplifier.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.soundamplifier.R
import com.soundamplifier.data.AccountLocalIds
import com.soundamplifier.data.AppDatabase
import com.soundamplifier.data.HearingTestPreferences
import com.soundamplifier.data.MIGRATION_2_3
import com.soundamplifier.data.MIGRATION_3_4
import com.soundamplifier.data.MIGRATION_4_5
import com.soundamplifier.data.FirstLoginHomePreferences
import com.soundamplifier.data.FirestoreUserRepository
import com.soundamplifier.data.ProfileRepository
import com.soundamplifier.ui.screens.AmplifierScreen
import com.soundamplifier.ui.screens.AudiogramScreen
import com.soundamplifier.ui.components.SunoXAmbientBackground
import com.soundamplifier.ui.navigation.AppDrawerLayout
import com.soundamplifier.ui.navigation.sunoxEnter
import com.soundamplifier.ui.navigation.sunoxExit
import com.soundamplifier.ui.navigation.sunoxPopEnter
import com.soundamplifier.ui.navigation.sunoxPopExit
import com.soundamplifier.ui.theme.SunoXColors
import com.soundamplifier.ui.theme.SunoXTheme
import com.soundamplifier.ui.screens.FaqScreen
import com.soundamplifier.ui.screens.GoogleLoginScreen
import com.soundamplifier.ui.screens.HomeScreen
import com.soundamplifier.ui.screens.PhoneRequiredScreen
import com.soundamplifier.ui.screens.PresetManagerScreen
import com.soundamplifier.ui.screens.ProfileScreen
import com.soundamplifier.ui.screens.SettingsScreen
import com.soundamplifier.viewmodel.AmplifierViewModel
import com.soundamplifier.viewmodel.AudiogramViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object ThemeManager {
    private const val PREFS_NAME = "smarthear_prefs"
    private const val KEY_DARK_MODE = "dark_mode"

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun init(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        _isDarkMode.value = prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun toggle(activity: android.app.Activity) {
        val next = !_isDarkMode.value
        _isDarkMode.value = next
        activity.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE, next)
            .apply()
        activity.recreate()
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var amplifierViewModel: AmplifierViewModel
    private lateinit var audiogramViewModel: AudiogramViewModel
    private lateinit var profileRepository: ProfileRepository
    private lateinit var googleSignInClient: GoogleSignInClient

    private val firestoreUserRepository = FirestoreUserRepository()

    private var sessionDocListener: ListenerRegistration? = null
    private var sessionNavHandler: (() -> Unit)? = null

    @Volatile
    private var handlingSessionInvalidation: Boolean = false

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* handled silently */ }

    companion object {
        private const val PREFS_SESSION = "session"
        private const val PREFS_KEY_SESSION_TOKEN = "sessionToken"
        private const val FIRESTORE_FIELD_SESSION_TOKEN = "sessionToken"
        private const val TAG_SESSION = "SessionEnforcement"
    }

    private fun sessionPrefs() = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE)

    private fun clearSessionListener() {
        sessionDocListener?.remove()
        sessionDocListener = null
    }

    private fun attachSessionListener(uid: String) {
        clearSessionListener()
        val prefs = sessionPrefs()
        sessionDocListener = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, e ->
                if (e != null) return@addSnapshotListener
                if (snap == null || !snap.exists()) return@addSnapshotListener
                val remote = snap.getString(FIRESTORE_FIELD_SESSION_TOKEN) ?: return@addSnapshotListener
                val local = prefs.getString(PREFS_KEY_SESSION_TOKEN, null) ?: ""
                if (remote != local) {
                    runOnUiThread {
                        handleSessionInvalidatedFromServer(showToast = true)
                    }
                }
            }
    }

    private fun handleSessionInvalidatedFromServer(showToast: Boolean) {
        if (handlingSessionInvalidation) return
        handlingSessionInvalidation = true
        clearSessionListener()
        sessionPrefs().edit().clear().apply()
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            FirstLoginHomePreferences.clearPending(this, uid)
        }
        FirebaseAuth.getInstance().signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            if (::amplifierViewModel.isInitialized) {
                amplifierViewModel.refreshAccountScope(this@MainActivity)
            }
            sessionNavHandler?.invoke()
            if (showToast) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.session_logged_out_other_device),
                    Toast.LENGTH_LONG,
                ).show()
            }
            handlingSessionInvalidation = false
        }
    }

    private fun performVoluntarySignOut(onComplete: () -> Unit) {
        clearSessionListener()
        sessionPrefs().edit().clear().apply()
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            FirstLoginHomePreferences.clearPending(this, uid)
        }
        FirebaseAuth.getInstance().signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            if (::amplifierViewModel.isInitialized) {
                amplifierViewModel.refreshAccountScope(this@MainActivity)
            }
            onComplete()
        }
    }

    override fun onResume() {
        super.onResume()
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            clearSessionListener()
            return
        }
        if (!::amplifierViewModel.isInitialized) return
        val prefs = sessionPrefs()
        val local = prefs.getString(PREFS_KEY_SESSION_TOKEN, null) ?: ""
        lifecycleScope.launch {
            if (local.isEmpty()) {
                try {
                    val token = withContext(Dispatchers.IO) {
                        firestoreUserRepository.createSessionToken(user.uid)
                    }
                    prefs.edit().putString(PREFS_KEY_SESSION_TOKEN, token).apply()
                    runOnUiThread { attachSessionListener(user.uid) }
                } catch (e: Exception) {
                    Log.e(TAG_SESSION, "session bootstrap failed", e)
                }
                return@launch
            }
            val valid = withContext(Dispatchers.IO) {
                firestoreUserRepository.isSessionValid(user.uid, local)
            }
            when (valid) {
                false -> runOnUiThread { handleSessionInvalidatedFromServer(showToast = true) }
                true -> {
                    runOnUiThread { attachSessionListener(user.uid) }
                    lifecycleScope.launch {
                        delay(450)
                        val recheck = withContext(Dispatchers.IO) {
                            firestoreUserRepository.isSessionValid(user.uid, local)
                        }
                        if (recheck == false) {
                            runOnUiThread { handleSessionInvalidatedFromServer(showToast = true) }
                        }
                    }
                }
                null -> { /* network / error — do not sign out */ }
            }
        }
    }

    override fun onDestroy() {
        clearSessionListener()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager.init(applicationContext)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "sound-amplifier-db")
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
            val repository = ProfileRepository(db.audiogramDao())

            withContext(Dispatchers.Main) {
                profileRepository = repository
                amplifierViewModel = ViewModelProvider(this@MainActivity, object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return AmplifierViewModel(
                            repository,
                            db.customPresetDao(),
                            applicationContext,
                        ) as T
                    }
                })[AmplifierViewModel::class.java]

                audiogramViewModel = ViewModelProvider(this@MainActivity, object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return AudiogramViewModel(repository, applicationContext) as T
                    }
                })[AudiogramViewModel::class.java]

                setContent {
                    val isDarkMode by ThemeManager.isDarkMode.collectAsState(initial = false)
                    SideEffect {
                        val bg = if (isDarkMode) SunoXColors.DeepBackground else SunoXColors.LightBackground
                        window.setBackgroundDrawable(ColorDrawable(bg.toArgb()))
                    }
                    val navController = rememberNavController()
                    var accountScopeNonce by remember { mutableStateOf(0) }
                    var lastTestedMillis by remember(accountScopeNonce) {
                        mutableStateOf<Long?>(null)
                    }

                    val signInLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult(),
                    ) { result ->
                        if (result.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
                        val data = result.data ?: return@rememberLauncherForActivityResult
                        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                        try {
                            val account = task.getResult(ApiException::class.java)
                                ?: return@rememberLauncherForActivityResult
                            val idToken = account.idToken
                            if (idToken.isNullOrEmpty()) {
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.google_sign_in_not_configured),
                                    Toast.LENGTH_LONG,
                                ).show()
                                return@rememberLauncherForActivityResult
                            }
                            val credential = GoogleAuthProvider.getCredential(idToken, null)
                            FirebaseAuth.getInstance()
                                .signInWithCredential(credential)
                                .addOnSuccessListener { authResult ->
                                    val user = authResult.user ?: return@addOnSuccessListener
                                    lifecycleScope.launch {
                                        val lang = LocaleManager.getSavedLanguageCode(this@MainActivity)
                                        withContext(Dispatchers.IO) {
                                            firestoreUserRepository.createOrUpdateUserProfile(user, lang)
                                        }
                                        var token = withContext(Dispatchers.IO) {
                                            firestoreUserRepository.createSessionToken(user.uid)
                                        }
                                        var confirmed = withContext(Dispatchers.IO) {
                                            firestoreUserRepository.confirmSessionTokenOnServer(
                                                user.uid,
                                                token,
                                            )
                                        }
                                        if (!confirmed) {
                                            Log.w(TAG_SESSION, "session token not visible on server yet, retrying")
                                            token = withContext(Dispatchers.IO) {
                                                firestoreUserRepository.createSessionToken(user.uid)
                                            }
                                            confirmed = withContext(Dispatchers.IO) {
                                                firestoreUserRepository.confirmSessionTokenOnServer(
                                                    user.uid,
                                                    token,
                                                )
                                            }
                                        }
                                        if (!confirmed) {
                                            Log.e(TAG_SESSION, "session token still not confirmed after retry")
                                        }
                                        sessionPrefs().edit()
                                            .putString(PREFS_KEY_SESSION_TOKEN, token)
                                            .apply()
                                        FirstLoginHomePreferences.markPendingAfterSignIn(
                                            this@MainActivity,
                                            user.uid,
                                        )
                                        attachSessionListener(user.uid)
                                        accountScopeNonce++
                                        amplifierViewModel.refreshAccountScope(this@MainActivity)
                                        Toast.makeText(
                                            this@MainActivity,
                                            getString(R.string.google_sign_in_success),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        if (navController.currentDestination?.route == "login") {
                                            navController.navigate("bootstrap") {
                                                popUpTo("login") { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this@MainActivity,
                                        e.message ?: getString(R.string.google_sign_in_not_configured),
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                        } catch (e: ApiException) {
                            Toast.makeText(
                                this@MainActivity,
                                e.message ?: getString(R.string.google_sign_in_not_configured),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }

                    DisposableEffect(navController) {
                        val act = this@MainActivity
                        act.sessionNavHandler = {
                            accountScopeNonce++
                            navController.navigate("login") {
                                popUpTo(navController.graph.id) {
                                    inclusive = true
                                }
                            }
                        }
                        onDispose {
                            act.sessionNavHandler = null
                        }
                    }

                    LaunchedEffect(accountScopeNonce) {
                        lastTestedMillis = withContext(Dispatchers.IO) {
                            val aid = AccountLocalIds.localKey(this@MainActivity)
                            profileRepository.getLatestForAccount(aid)?.createdAt
                        }
                    }

                    SunoXTheme(darkTheme = isDarkMode) {
                        Box(Modifier.fillMaxSize()) {
                            SunoXAmbientBackground(isDark = isDarkMode)
                            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route
                            val guestRoutes = setOf("login", "bootstrap", "collect_phone")
                            val drawerGesturesEnabled = currentRoute != null && currentRoute !in guestRoutes
                            var firstLoginHomeGateRevision by remember { mutableIntStateOf(0) }
                            var previousNavRoute by remember { mutableStateOf<String?>(null) }

                            LaunchedEffect(currentRoute) {
                                if (!drawerGesturesEnabled) {
                                    drawerState.close()
                                }
                                val uid = FirebaseAuth.getInstance().currentUser?.uid
                                val prev = previousNavRoute
                                if (prev == "home" && currentRoute != null && currentRoute != "home" && uid != null) {
                                    FirstLoginHomePreferences.clearPending(this@MainActivity, uid)
                                    firstLoginHomeGateRevision++
                                }
                                previousNavRoute = currentRoute
                            }

                            val logoutAndGoToLogin: () -> Unit = {
                                performVoluntarySignOut {
                                    accountScopeNonce++
                                    amplifierViewModel.refreshAccountScope(this@MainActivity)
                                    navController.navigate("login") {
                                        popUpTo(navController.graph.id) {
                                            inclusive = true
                                        }
                                    }
                                }
                            }

                            val navigateFromDrawer: (String) -> Unit = { route ->
                                when (route) {
                                    "home" -> navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                    else -> navController.navigate(route) {
                                        popUpTo("home") { inclusive = false }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }

                            val onDrawerItemNavigate: (String) -> Unit = { route ->
                                if (route == "audiogram") {
                                    audiogramViewModel.prepareRetake()
                                }
                                navigateFromDrawer(route)
                            }

                            AppDrawerLayout(
                                drawerState = drawerState,
                                drawerGesturesEnabled = drawerGesturesEnabled,
                                currentRoute = currentRoute,
                                onDrawerNavigate = onDrawerItemNavigate,
                                onLogout = logoutAndGoToLogin,
                            ) { openDrawer ->
                                NavHost(
                                    navController = navController,
                                    startDestination = "bootstrap",
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    composable(
                                        route = "bootstrap",
                                        enterTransition = sunoxEnter(),
                                        exitTransition = sunoxExit(),
                                        popEnterTransition = sunoxPopEnter(),
                                        popExitTransition = sunoxPopExit(),
                                    ) {
                                        Box(
                                            Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {}
                                        LaunchedEffect(Unit) {
                                            val user = awaitFirstAuthUser()
                                            if (navController.currentDestination?.route != "bootstrap") return@LaunchedEffect
                                            if (user == null) {
                                                navController.navigate("login") {
                                                    popUpTo("bootstrap") { inclusive = true }
                                                }
                                                return@LaunchedEffect
                                            }
                                            val hasPhone = withContext(Dispatchers.IO) {
                                                firestoreUserRepository.userHasValidContactPhone(user)
                                            }
                                            if (!hasPhone) {
                                                navController.navigate("collect_phone") {
                                                    popUpTo("bootstrap") { inclusive = true }
                                                }
                                            } else {
                                                navController.navigate("home") {
                                                    popUpTo("bootstrap") { inclusive = true }
                                                }
                                            }
                                        }
                                    }

                                    composable(
                                        route = "collect_phone",
                                        enterTransition = sunoxEnter(),
                                        exitTransition = sunoxExit(),
                                        popEnterTransition = sunoxPopEnter(),
                                        popExitTransition = sunoxPopExit(),
                                    ) {
                                        BackHandler { }
                                        RequireAuth(navController = navController) {
                                            PhoneRequiredScreen(
                                                firestoreUserRepository = firestoreUserRepository,
                                                onSuccess = {
                                                    accountScopeNonce++
                                                    amplifierViewModel.refreshAccountScope(this@MainActivity)
                                                    navController.navigate("home") {
                                                        popUpTo("collect_phone") { inclusive = true }
                                                        launchSingleTop = true
                                                    }
                                                },
                                            )
                                        }
                                    }

                                    composable(
                                        route = "login",
                                        enterTransition = sunoxEnter(),
                                        exitTransition = sunoxExit(),
                                        popEnterTransition = sunoxPopEnter(),
                                        popExitTransition = sunoxPopExit(),
                                        ) {
                                        BackHandler { finish() }
                                        GoogleLoginScreen(
                                            onGoogleSignIn = {
                                                signInLauncher.launch(googleSignInClient.signInIntent)
                                            },
                                            isDarkMode = isDarkMode,
                                            onThemeToggle = { ThemeManager.toggle(this@MainActivity) },
                                        )
                                    }

                                    composable(
                                        route = "home",
                                        enterTransition = sunoxEnter(),
                                        exitTransition = sunoxExit(),
                                        popEnterTransition = sunoxPopEnter(),
                                        popExitTransition = sunoxPopExit(),
                                    ) {
                                    RequireAuth(navController = navController) {
                                        HomeScreen(
                                            onOpenNavigationDrawer = openDrawer,
                                            onStartAudiogramTest = {
                                                audiogramViewModel.prepareRetake()
                                                navController.navigate("audiogram") {
                                                    launchSingleTop = true
                                                }
                                            },
                                            onOpenAmplifier = {
                                                navController.navigate("amplifier") {
                                                    popUpTo("home") { inclusive = false }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            onLogout = logoutAndGoToLogin,
                                            isDarkMode = isDarkMode,
                                            onThemeToggle = { ThemeManager.toggle(this@MainActivity) },
                                            firestoreUserRepository = firestoreUserRepository,
                                            homeFirstLoginGateRevision = firstLoginHomeGateRevision,
                                        )
                                    }
                                }

                                composable("audiogram") {
                                    RequireAuth(navController = navController) {
                                        val state by audiogramViewModel.uiState.collectAsState()
                                        AudiogramScreen(
                                            uiState = state,
                                            onStart = { audiogramViewModel.startTest() },
                                            onHeard = { audiogramViewModel.userHeard() },
                                            onCannotHear = { audiogramViewModel.userCannotHear() },
                                            onDismissInstruction = { audiogramViewModel.dismissInstruction() },
                                            onContinueTransition = { audiogramViewModel.continueAfterLeftEar() },
                                            onSaveAndApply = {
                                                lifecycleScope.launch {
                                                    val s = audiogramViewModel.uiState.value
                                                    val left = s.leftThresholds.map { it.toFloat() }
                                                    val right = s.rightThresholds.map { it.toFloat() }
                                                    withContext(Dispatchers.IO) {
                                                        audiogramViewModel.persistAudiogram(left, right)
                                                    }
                                                    val preset =
                                                        amplifierViewModel.buildMyHearingProfilePreset(left, right)
                                                    amplifierViewModel.replaceMyHearingProfileAndApplyNow(preset)
                                                    amplifierViewModel.loadLatestProfilePublic()
                                                    HearingTestPreferences.setCompleted(
                                                        this@MainActivity,
                                                        AccountLocalIds.localKey(this@MainActivity),
                                                    )
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        getString(R.string.hearing_test_profile_applied),
                                                        Toast.LENGTH_LONG,
                                                    ).show()
                                                    navController.navigate("amplifier") {
                                                        popUpTo("home")
                                                    }
                                                }
                                            },
                                            onRetake = { audiogramViewModel.prepareRetake() },
                                        )
                                    }
                                }

                                composable("amplifier") {
                                    RequireAuth(navController = navController) {
                                        val state by amplifierViewModel.uiState.collectAsState()
                                        val builtInPresets by amplifierViewModel.builtInPresetsDisplay.collectAsState()
                                        val userCustomPresets by amplifierViewModel.userOnlyCustomPresets.collectAsState()
                                        LaunchedEffect(Unit) {
                                            amplifierViewModel.refreshAccountScope(this@MainActivity)
                                        }
                                        AmplifierScreen(
                                            uiState = state,
                                            onToggle = { amplifierViewModel.toggleAmplifier() },
                                            onBoostQuietSoundsChange = {
                                                amplifierViewModel.setBoostQuietSounds(it)
                                            },
                                            onMasterGainChange = { amplifierViewModel.setMasterGain(it) },
                                            onLowBoostChange = { amplifierViewModel.setLowBoost(it) },
                                            onHighBoostChange = { amplifierViewModel.setHighBoost(it) },
                                            onApplyPreset = { amplifierViewModel.applyPreset(it) },
                                            builtInPresets = builtInPresets,
                                            customPresets = userCustomPresets,
                                            onApplyCustomPreset = { amplifierViewModel.applyCustomPreset(it) },
                                            onDeleteCustomPreset = { amplifierViewModel.deleteCustomPreset(it) },
                                            onSaveCurrentAsPreset = {
                                                amplifierViewModel.saveCurrentAsPreset(it)
                                            },
                                            toastMessages = amplifierViewModel.toasts,
                                            isDarkMode = isDarkMode,
                                            onThemeToggle = { ThemeManager.toggle(this@MainActivity) },
                                            onOpenNavigationDrawer = openDrawer,
                                            onNavigatePresets = {
                                                navController.navigate("presets") {
                                                    launchSingleTop = true
                                                }
                                            },
                                            onResumeMicLabels = { amplifierViewModel.refreshMicDeviceLabels() },
                                        )
                                    }
                                }

                                composable("presets") {
                                    RequireAuth(navController = navController) {
                                        LaunchedEffect(Unit) {
                                            amplifierViewModel.refreshAccountScope(this@MainActivity)
                                        }
                                        PresetManagerScreen(
                                            viewModel = amplifierViewModel,
                                            onBack = { navController.popBackStack() },
                                            onOpenNavigationDrawer = openDrawer,
                                        )
                                    }
                                }

                                composable("faq") {
                                    RequireAuth(navController = navController) {
                                        FaqScreen(
                                            onBack = { navController.popBackStack() },
                                            onOpenNavigationDrawer = openDrawer,
                                        )
                                    }
                                }

                                composable("settings") {
                                    RequireAuth(navController = navController) {
                                        SettingsScreen(
                                            currentLanguageCode = LocaleManager.getSavedLanguageCode(
                                                this@MainActivity,
                                            ),
                                            onLanguageSelected = { code ->
                                                LocaleManager.setLanguage(this@MainActivity, code)
                                            },
                                            onBack = { navController.popBackStack() },
                                            onOpenNavigationDrawer = openDrawer,
                                            onOpenFaq = {
                                                navController.navigate("faq") {
                                                    popUpTo("home") { inclusive = false }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            lastTestedMillis = lastTestedMillis,
                                            onRetakeHearingTest = {
                                                audiogramViewModel.prepareRetake()
                                                navController.navigate("audiogram") {
                                                    launchSingleTop = true
                                                }
                                            },
                                        )
                                    }
                                }

                                composable("profile") {
                                    RequireAuth(navController = navController) {
                                        ProfileScreen(
                                            onBack = { navController.popBackStack() },
                                            onOpenNavigationDrawer = openDrawer,
                                            onLogout = logoutAndGoToLogin,
                                            onNavigateLogin = {
                                                navController.navigate("login") {
                                                    popUpTo(navController.graph.id) { inclusive = true }
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

private suspend fun awaitFirstAuthUser(): FirebaseUser? =
    suspendCancellableCoroutine { cont ->
        val auth = FirebaseAuth.getInstance()
        val listener = object : FirebaseAuth.AuthStateListener {
            override fun onAuthStateChanged(a: FirebaseAuth) {
                a.removeAuthStateListener(this)
                if (cont.isActive) cont.resume(a.currentUser)
            }
        }
        auth.addAuthStateListener(listener)
        cont.invokeOnCancellation { auth.removeAuthStateListener(listener) }
    }

@Composable
private fun RequireAuth(
    navController: NavHostController,
    content: @Composable () -> Unit,
) {
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    LaunchedEffect(user?.uid) {
        if (user == null) {
            navController.navigate("login") {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
            }
        }
    }
    if (user != null) {
        content()
    }
}

