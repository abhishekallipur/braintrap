package com.example.braintrap.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.braintrap.data.model.TimeLimit
import com.example.braintrap.data.repository.TimeLimitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val timeLimitRepository: TimeLimitRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _timeLimits = MutableStateFlow<List<TimeLimit>>(emptyList())
    val timeLimits: StateFlow<List<TimeLimit>> = _timeLimits.asStateFlow()
    
    private val prefs = context.getSharedPreferences("braintrap_prefs", Context.MODE_PRIVATE)
    
    private val _bonusTimeMinutes = MutableStateFlow(getBonusTimeMinutes())
    val bonusTimeMinutes: StateFlow<Int> = _bonusTimeMinutes.asStateFlow()
    
    private val _unlockMethod = MutableStateFlow(getUnlockMethod())
    val unlockMethod: StateFlow<String> = _unlockMethod.asStateFlow()
    
    private val _registeredNfcTag = MutableStateFlow(getRegisteredNfcTag())
    val registeredNfcTag: StateFlow<String?> = _registeredNfcTag.asStateFlow()
    
    init {
        loadTimeLimits()
    }
    
    private fun loadTimeLimits() {
        viewModelScope.launch {
            timeLimitRepository.getAllTimeLimits().collect { limits ->
                _timeLimits.value = limits
            }
        }
    }
    
    fun updateTimeLimit(packageName: String, minutes: Int) {
        viewModelScope.launch {
            val existing = timeLimitRepository.getTimeLimit(packageName)
            if (existing != null) {
                timeLimitRepository.updateTimeLimit(
                    existing.copy(dailyLimitMinutes = minutes)
                )
            } else {
                timeLimitRepository.insertTimeLimit(
                    TimeLimit(
                        packageName = packageName,
                        dailyLimitMinutes = minutes,
                        isEnabled = true
                    )
                )
            }
        }
    }
    
    fun toggleTimeLimit(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            val existing = timeLimitRepository.getTimeLimit(packageName)
            if (existing != null) {
                timeLimitRepository.updateTimeLimit(
                    existing.copy(isEnabled = enabled)
                )
            }
        }
    }
    
    fun getBonusTimeMinutes(): Int {
        return prefs.getInt("bonus_time_minutes", 45) // Default 45 minutes
    }
    
    fun setBonusTimeMinutes(minutes: Int) {
        prefs.edit().putInt("bonus_time_minutes", minutes).apply()
        _bonusTimeMinutes.value = minutes
    }
    
    fun getUnlockMethod(): String {
        return prefs.getString("unlock_method", "math") ?: "math" // Default to math challenges
    }
    
    fun setUnlockMethod(method: String) {
        prefs.edit().putString("unlock_method", method).apply()
        _unlockMethod.value = method
    }
    
    fun getRegisteredNfcTag(): String? {
        return prefs.getString("registered_nfc_tag", null)
    }
    
    fun setRegisteredNfcTag(tagId: String) {
        prefs.edit().putString("registered_nfc_tag", tagId).apply()
        _registeredNfcTag.value = tagId
    }
    
    fun clearRegisteredNfcTag() {
        prefs.edit().remove("registered_nfc_tag").apply()
        _registeredNfcTag.value = null
    }
}

