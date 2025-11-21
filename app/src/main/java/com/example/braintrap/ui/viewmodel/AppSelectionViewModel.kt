package com.example.braintrap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.braintrap.data.model.AppInfo
import com.example.braintrap.data.repository.TimeLimitRepository
import com.example.braintrap.util.AppInfoProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppSelectionUiState(
    val apps: List<AppInfo> = emptyList(),
    val selectedApps: Set<String> = emptySet(),
    val searchQuery: String = "",
    val selectedCategory: com.example.braintrap.data.model.AppCategory? = null
)

@HiltViewModel
class AppSelectionViewModel @Inject constructor(
    private val appInfoProvider: AppInfoProvider,
    private val timeLimitRepository: TimeLimitRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AppSelectionUiState())
    val uiState: StateFlow<AppSelectionUiState> = _uiState.asStateFlow()
    
    init {
        loadApps()
        loadSelectedApps()
    }
    
    private fun loadApps() {
        viewModelScope.launch {
            val apps = appInfoProvider.getInstalledApps()
            _uiState.value = _uiState.value.copy(apps = apps)
        }
    }
    
    private fun loadSelectedApps() {
        viewModelScope.launch {
            timeLimitRepository.getAllTimeLimits().collect { timeLimits ->
                val selectedPackages = timeLimits.map { it.packageName }.toSet()
                _uiState.value = _uiState.value.copy(
                    selectedApps = selectedPackages,
                    apps = _uiState.value.apps.map { app ->
                        app.copy(isBlocked = selectedPackages.contains(app.packageName))
                    }
                )
            }
        }
    }
    
    fun toggleAppSelection(packageName: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val newSelected = if (currentState.selectedApps.contains(packageName)) {
                currentState.selectedApps - packageName
            } else {
                currentState.selectedApps + packageName
            }
            
            _uiState.value = currentState.copy(selectedApps = newSelected)
        }
    }
    
    fun selectAllSocialMedia() {
        viewModelScope.launch {
            val socialMediaApps = appInfoProvider.getSocialMediaApps()
            val newSelected = _uiState.value.selectedApps + socialMediaApps.map { it.packageName }
            _uiState.value = _uiState.value.copy(selectedApps = newSelected)
        }
    }
    
    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
    
    fun setCategory(category: com.example.braintrap.data.model.AppCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }
    
    fun getFilteredApps(): List<AppInfo> {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()
        
        return state.apps.filter { app ->
            val matchesQuery = query.isEmpty() ||
                    app.appName.lowercase().contains(query) ||
                    app.packageName.lowercase().contains(query) ||
                    app.appName.lowercase().startsWith(query) ||
                    app.packageName.lowercase().startsWith(query)
            
            val matchesCategory = state.selectedCategory == null ||
                    app.category == state.selectedCategory
            
            matchesQuery && matchesCategory
        }.sortedBy { app ->
            // Sort by relevance: exact matches first, then starts with, then contains
            when {
                query.isEmpty() -> 0
                app.appName.lowercase().startsWith(query) -> 1
                app.packageName.lowercase().startsWith(query) -> 2
                app.appName.lowercase().contains(query) -> 3
                else -> 4
            }
        }
    }
    
    fun saveSelections() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val timeLimits = timeLimitRepository.getAllTimeLimits().first()
            val existingPackages = timeLimits.map { it.packageName }.toSet()
            
            // Add new selections
            currentState.selectedApps.forEach { packageName ->
                if (!existingPackages.contains(packageName)) {
                    timeLimitRepository.insertTimeLimit(
                        com.example.braintrap.data.model.TimeLimit(
                            packageName = packageName,
                            dailyLimitMinutes = 30, // Default 30 minutes
                            isEnabled = true
                        )
                    )
                }
            }
            
            // Remove deselected apps
            existingPackages.forEach { packageName ->
                if (!currentState.selectedApps.contains(packageName)) {
                    timeLimitRepository.deleteTimeLimit(packageName)
                }
            }
        }
    }
}

