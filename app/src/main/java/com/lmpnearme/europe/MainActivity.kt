package com.lmpnearme.europe

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.lmpnearme.europe.ui.screens.ApiKeyScreen
import com.lmpnearme.europe.ui.screens.MainScreen
import com.lmpnearme.europe.ui.theme.LmpNearMeTheme
import com.lmpnearme.europe.viewmodel.MainViewModel
import com.lmpnearme.europe.viewmodel.UiState

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) viewModel.onLocationPermissionGranted()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        requestLocationPermissionsIfNeeded()

        setContent {
            LmpNearMeTheme {
                val uiState by viewModel.uiState.collectAsState()
                val currentZone by viewModel.currentZone.collectAsState()

                if (uiState is UiState.NeedsApiKey) {
                    ApiKeyScreen(
                        onApiKeySaved = { key ->
                            viewModel.saveApiKeyAndStart(key)
                            requestLocationPermissionsIfNeeded()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MainScreen(
                        uiState = uiState,
                        currentZone = currentZone,
                        availableZones = viewModel.availableZones,
                        onRefresh = { viewModel.refresh() },
                        onClearApiKey = { viewModel.clearApiKey() },
                        onSelectZone = { viewModel.selectZone(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    private fun requestLocationPermissionsIfNeeded() {
        if (!viewModel.hasLocationPermission.value) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}
