package com.example.braintrap.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.braintrap.data.model.TimeLimit
import com.example.braintrap.service.AppBlockingService
import com.example.braintrap.ui.viewmodel.SettingsViewModel
import com.example.braintrap.util.PermissionManager

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToAchievements: () -> Unit = {}
) {
    val timeLimits by viewModel.timeLimits.collectAsState()
    val bonusTimeMinutes by viewModel.bonusTimeMinutes.collectAsState()
    val unlockMethod by viewModel.unlockMethod.collectAsState()
    val registeredNfcTag by viewModel.registeredNfcTag.collectAsState()
    val context = LocalContext.current
    
    var showNfcRegistration by remember { mutableStateOf(false) }
    
    if (showNfcRegistration) {
        LaunchedEffect(Unit) {
            val intent = android.content.Intent(context, com.example.braintrap.ui.NfcRegistrationActivity::class.java)
            context.startActivity(intent)
            showNfcRegistration = false
        }
    }
    
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Service Status Card
                ServiceStatusCard(context = context)
            }
            
            item {
                // Achievements Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    onClick = onNavigateToAchievements
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🏆",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "View Achievements",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    text = "Track your progress and milestones",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "View Achievements"
                        )
                    }
                }
            }
            
            item {
                // Unlock Method Card
                UnlockMethodCard(
                    selectedMethod = unlockMethod,
                    onMethodSelected = { method ->
                        viewModel.setUnlockMethod(method)
                    }
                )
            }
            
            // Show NFC tag management if NFC is selected
            if (unlockMethod == "nfc" || unlockMethod == "both") {
                item {
                    NfcTagManagementCard(
                        registeredTag = registeredNfcTag,
                        onRegisterTag = { showNfcRegistration = true },
                        onClearTag = { viewModel.clearRegisteredNfcTag() }
                    )
                }
            }
            
            item {
                // Bonus Time Card
                BonusTimeCard(
                    bonusTimeMinutes = bonusTimeMinutes,
                    onBonusTimeChanged = { minutes ->
                        viewModel.setBonusTimeMinutes(minutes)
                    }
                )
            }
            
            item {
                Text(
                    text = "Time Limits",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            
            items(timeLimits) { timeLimit ->
                TimeLimitItem(
                    timeLimit = timeLimit,
                    onTimeLimitChanged = { minutes ->
                        viewModel.updateTimeLimit(timeLimit.packageName, minutes)
                    },
                    onToggleEnabled = { enabled ->
                        viewModel.toggleTimeLimit(timeLimit.packageName, enabled)
                    }
                )
            }
        }
    }
}

@Composable
fun ServiceStatusCard(context: Context) {
    val hasAccessibility = PermissionManager.isAccessibilityServiceEnabled(
        context,
        AppBlockingService::class.java
    )
    val hasUsageStats = PermissionManager.isUsageStatsPermissionGranted(context)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasAccessibility && hasUsageStats) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Service Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (hasAccessibility) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (hasAccessibility) 
                        MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasAccessibility) "Accessibility: Enabled" else "Accessibility: Disabled",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (hasUsageStats) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (hasUsageStats) 
                        MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasUsageStats) "Usage Stats: Enabled" else "Usage Stats: Disabled",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (!hasAccessibility || !hasUsageStats) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Blocking will not work without these permissions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun BonusTimeCard(
    bonusTimeMinutes: Int,
    onBonusTimeChanged: (Int) -> Unit
) {
    var bonusTime by remember { mutableStateOf(bonusTimeMinutes.toString()) }
    var showSaveButton by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }
    
    LaunchedEffect(bonusTimeMinutes) {
        bonusTime = bonusTimeMinutes.toString()
        showSaveButton = false
    }
    
    // Auto-hide confirmation message
    LaunchedEffect(showConfirmation) {
        if (showConfirmation) {
            kotlinx.coroutines.delay(2000)
            showConfirmation = false
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Challenge Reward Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Minutes earned after solving math challenge",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = bonusTime,
                onValueChange = { 
                    if (it.all { char -> char.isDigit() } && it.length <= 3) {
                        bonusTime = it
                        showSaveButton = it != bonusTimeMinutes.toString() && it.isNotEmpty()
                    }
                },
                label = { Text("Bonus Time (minutes)") },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                singleLine = true,
                suffix = { Text("min") }
            )
            
            if (showSaveButton) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        bonusTime.toIntOrNull()?.let { minutes ->
                            if (minutes > 0) {
                                onBonusTimeChanged(minutes)
                                showSaveButton = false
                                showConfirmation = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Bonus Time")
                }
            }
            
            if (showConfirmation) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bonus time updated successfully!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeLimitItem(
    timeLimit: TimeLimit,
    onTimeLimitChanged: (Int) -> Unit,
    onToggleEnabled: (Boolean) -> Unit
) {
    var timeLimitMinutes by remember { mutableStateOf(timeLimit.dailyLimitMinutes.toString()) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeLimit.packageName,
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(
                    checked = timeLimit.isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = timeLimitMinutes,
                onValueChange = { 
                    if (it.all { char -> char.isDigit() }) {
                        timeLimitMinutes = it
                        it.toIntOrNull()?.let { minutes ->
                            onTimeLimitChanged(minutes)
                        }
                    }
                },
                label = { Text("Daily Limit (minutes)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = timeLimit.isEnabled,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun UnlockMethodCard(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "🔓",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Unlock Method",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
            
            Text(
                text = "Choose how you want to unlock blocked apps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Math Challenge Option
            UnlockMethodOption(
                icon = "🧮",
                title = "Math Challenge",
                description = "Solve puzzles to unlock (Default)",
                isSelected = selectedMethod == "math",
                onClick = { onMethodSelected("math") }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // NFC Option
            UnlockMethodOption(
                icon = "📱",
                title = "NFC Tag",
                description = "Tap NFC tag to unlock instantly",
                isSelected = selectedMethod == "nfc",
                onClick = { onMethodSelected("nfc") }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Both Option
            UnlockMethodOption(
                icon = "🔀",
                title = "Both Methods",
                description = "Use either math or NFC to unlock",
                isSelected = selectedMethod == "both",
                onClick = { onMethodSelected("both") }
            )
        }
    }
}

@Composable
fun UnlockMethodOption(
    icon: String,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) 
                        androidx.compose.ui.text.font.FontWeight.Bold 
                    else androidx.compose.ui.text.font.FontWeight.Normal
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun NfcTagManagementCard(
    registeredTag: String?,
    onRegisterTag: () -> Unit,
    onClearTag: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (registeredTag != null)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = if (registeredTag != null) "✅" else "⚠️",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (registeredTag != null) "NFC Tag Registered" else "No NFC Tag Registered",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
            
            if (registeredTag != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Tag ID:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = registeredTag,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onClearTag,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Clear Tag")
                    }
                    
                    Button(
                        onClick = onRegisterTag,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Change Tag")
                    }
                }
            } else {
                Text(
                    text = "You must register an NFC tag to use NFC unlock. Only the registered tag will be able to unlock your apps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Button(
                    onClick = onRegisterTag,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Register NFC Tag Now")
                }
            }
        }
    }
}
