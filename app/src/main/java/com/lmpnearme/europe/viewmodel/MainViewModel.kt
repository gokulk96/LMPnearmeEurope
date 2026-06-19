package com.lmpnearme.europe.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lmpnearme.europe.data.models.ElectricitySnapshot
import com.lmpnearme.europe.data.repository.ElectricityRepository
import com.lmpnearme.europe.utils.BiddingZone
import com.lmpnearme.europe.utils.BiddingZoneMapper
import com.lmpnearme.europe.utils.LocationHelper
import com.lmpnearme.europe.utils.SecureKeyStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    object NeedsApiKey : UiState()
    object Loading : UiState()
    data class Success(val snapshot: ElectricitySnapshot) : UiState()
    data class Error(val message: String) : UiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorage = SecureKeyStorage(application)
    private val repository = ElectricityRepository()
    private val locationHelper = LocationHelper(application)

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _currentZone = MutableStateFlow<BiddingZone?>(null)
    val currentZone: StateFlow<BiddingZone?> = _currentZone.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    val availableZones: List<BiddingZone> = BiddingZoneMapper.all()

    init {
        checkLocationPermission()
        if (!secureStorage.hasApiKey()) {
            _uiState.value = UiState.NeedsApiKey
        } else {
            val savedZoneCode = secureStorage.getPreferredZone()
            if (savedZoneCode != null) {
                _currentZone.value = BiddingZoneMapper.all().find { it.eicCode == savedZoneCode }
            }
            refresh()
        }
    }

    fun checkLocationPermission() {
        val ctx = getApplication<Application>()
        _hasLocationPermission.value = ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onLocationPermissionGranted() {
        _hasLocationPermission.value = true
        detectZoneAndRefresh()
    }

    fun detectZoneAndRefresh() {
        viewModelScope.launch {
            val detectedZone = try { locationHelper.detectZone() } catch (_: Exception) { null }
            val zone = detectedZone ?: _currentZone.value ?: BiddingZoneMapper.default()
            _currentZone.value = zone
            secureStorage.savePreferredZone(zone.eicCode)
            loadData(zone)
        }
    }

    fun saveApiKeyAndStart(key: String) {
        secureStorage.saveApiKey(key)
        refresh()
    }

    fun clearApiKey() {
        secureStorage.clearApiKey()
        _uiState.value = UiState.NeedsApiKey
    }

    fun selectZone(zone: BiddingZone) {
        _currentZone.value = zone
        secureStorage.savePreferredZone(zone.eicCode)
        refresh()
    }

    fun refresh() {
        val apiKey = secureStorage.getApiKey() ?: run {
            _uiState.value = UiState.NeedsApiKey
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val zone = _currentZone.value ?: run {
                if (_hasLocationPermission.value) {
                    val detected = try { locationHelper.detectZone() } catch (_: Exception) { null }
                    detected ?: BiddingZoneMapper.default()
                } else {
                    BiddingZoneMapper.default()
                }
            }
            _currentZone.value = zone
            repository.fetchSnapshot(apiKey, zone).fold(
                onSuccess = { _uiState.value = UiState.Success(it) },
                onFailure = { _uiState.value = UiState.Error(it.message ?: "Failed to load data") }
            )
        }
    }
}
