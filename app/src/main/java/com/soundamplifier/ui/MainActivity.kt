package com.soundamplifier.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.soundamplifier.data.AppDatabase
import com.soundamplifier.data.ProfileRepository
import com.soundamplifier.ui.screens.AmplifierScreen
import com.soundamplifier.ui.screens.AudiogramScreen
import com.soundamplifier.ui.screens.HomeScreen
import com.soundamplifier.viewmodel.AmplifierViewModel
import com.soundamplifier.viewmodel.AudiogramViewModel

class MainActivity : ComponentActivity() {

    private lateinit var amplifierViewModel: AmplifierViewModel
    private lateinit var audiogramViewModel: AudiogramViewModel

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled silently; UI will show error if mic unavailable */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request mic permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        // Setup DB and repository
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "sound-amplifier-db")
            .fallbackToDestructiveMigration()
            .build()
        val repository = ProfileRepository(db.audiogramDao())

        // ViewModels
        amplifierViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AmplifierViewModel(repository) as T
            }
        })[AmplifierViewModel::class.java]

        audiogramViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AudiogramViewModel(repository) as T
            }
        })[AudiogramViewModel::class.java]

        setContent {
            MaterialTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "home") {

                    composable("home") {
                        HomeScreen(
                            onStartAmplifier = { navController.navigate("amplifier") },
                            onStartAudiogramTest = { navController.navigate("audiogram") }
                        )
                    }

                    composable("audiogram") {
                        val state by audiogramViewModel.uiState.collectAsState()
                        AudiogramScreen(
                            uiState = state,
                            onStart = { audiogramViewModel.startTest() },
                            onHeard = { audiogramViewModel.userHeard() },
                            onCannotHear = { audiogramViewModel.userCannotHear() },
                            onDone = {
                                amplifierViewModel.loadLatestProfilePublic()
                                navController.navigate("amplifier") {
                                    popUpTo("home")
                                }
                            }
                        )
                    }

                    composable("amplifier") {
                        val state by amplifierViewModel.uiState.collectAsState()
                        AmplifierScreen(
                            uiState = state,
                            onToggle = { amplifierViewModel.toggleAmplifier() },
                            onNoiseReductionChange = { amplifierViewModel.setNoiseReduction(it) },
                            onMasterGainChange = { amplifierViewModel.setMasterGain(it) },
                            onLowBoostChange = { amplifierViewModel.setLowBoost(it) },
                            onHighBoostChange = { amplifierViewModel.setHighBoost(it) }
                        )
                    }
                }
            }
        }
    }
}
