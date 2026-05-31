package com.example.ytshare.ui.screens

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytshare.Constants
import com.example.ytshare.helpers.NSDHelper
import com.example.ytshare.helpers.SharedPrefHelper
import com.example.ytshare.models.HostModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val nsdHelper: NSDHelper,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    val hosts: StateFlow<List<HostModel>> = nsdHelper.hosts

    private val _isTrackingEnabled = MutableStateFlow(
        sharedPreferences.getBoolean(Constants.isTracking, false)
    )
    val isTrackingEnabled: StateFlow<Boolean> = _isTrackingEnabled.asStateFlow()

    private val _selectedIp = MutableStateFlow(
        sharedPreferences.getString(Constants.ip, "0.0.0.0") ?: "0.0.0.0"
    )
    val selectedIp: StateFlow<String> = _selectedIp.asStateFlow()

    fun selectHost(host: HostModel) {
        val hostString = host.toString()
        SharedPrefHelper.saveIp(hostString, sharedPreferences)
        _selectedIp.value = hostString
    }

    fun setTracking(enabled: Boolean) {
        SharedPrefHelper.savePref(enabled, sharedPreferences)
        _isTrackingEnabled.value = enabled
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            nsdHelper.restartDiscovery()
            delay(2000)
            _isRefreshing.value = false
        }
    }
}
