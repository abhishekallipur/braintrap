package com.example.braintrap.ui.screen

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.braintrap.ui.viewmodel.AppSelectionViewModel
import com.example.braintrap.util.PermissionManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    context: Context,
    viewModel: AppSelectionViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var selectedAppsCount by remember { mutableStateOf(0) }
    
    val steps = remember {
        listOf(
        OnboardingStep(
            icon = Icons.Default.Star,
            title = "Welcome to BrainTrap",
            description = "Take control of your digital habits with intelligent app blocking and time management.",
            actionText = "Get Started",
            onAction = { currentStep++ }
        ),
        OnboardingStep(
            icon = Icons.Default.Settings,
            title = "Enable Accessibility Service",
            description = "We need accessibility permission to monitor and block apps when you reach your limits.",
            actionText = "Enable",
            onAction = {
                PermissionManager.requestAccessibilityService(context)
            },
            checkPermission = { PermissionManager.isAccessibilityServiceEnabled(context, com.example.braintrap.service.AppBlockingService::class.java) }
        ),
        OnboardingStep(
            icon = Icons.Default.Info,
            title = "Grant Usage Stats Permission",
            description = "Allow us to track your app usage time so we can enforce your daily limits.",
            actionText = "Grant Permission",
            onAction = {
                PermissionManager.requestUsageStatsPermission(context)
            },
            checkPermission = { PermissionManager.isUsageStatsPermissionGranted(context) }
        ),
        OnboardingStep(
            icon = Icons.Default.CheckCircle,
            title = "Select Apps to Monitor",
            description = "Choose which apps you want to set time limits for. You can always change this later.",
            actionText = if (selectedAppsCount > 0) "Continue ($selectedAppsCount selected)" else "Skip for Now",
            onAction = { currentStep++ },
            isAppSelection = true
        ),
        OnboardingStep(
            icon = Icons.Default.CheckCircle,
            title = "You're All Set!",
            description = "Start your journey towards mindful app usage. You can add more apps and adjust limits anytime.",
            actionText = "Start Using BrainTrap",
            onAction = onComplete
        )
        )
    }
    
    // Check permissions periodically and auto-advance
    LaunchedEffect(currentStep) {
        while (currentStep < steps.size - 1) {
            delay(500)
            val step = steps.getOrNull(currentStep)
            if (step?.checkPermission?.invoke() == true) {
                delay(800) // Small delay before advancing
                currentStep++
            }
        }
    }
    
    val currentStepData = steps.getOrNull(currentStep)
    
    // Animated background gradient
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                    )
                )
            )
    ) {
        if (currentStepData != null) {
            var scale by remember { mutableStateOf(0.8f) }
            var alpha by remember { mutableStateOf(0f) }
            
            LaunchedEffect(currentStep) {
                scale = 0.8f
                alpha = 0f
                delay(100)
                animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) { value, _ ->
                    scale = 0.8f + (0.2f * value)
                    alpha = value
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .scale(scale)
                    .alpha(alpha),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animated icon with pulse
                val iconScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "icon"
                )
                
                Surface(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(iconScale),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = currentStepData.icon,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Title with gradient
                Text(
                    text = currentStepData.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Description
                Text(
                    text = currentStepData.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // App Selection UI (only for app selection step)
                if (currentStepData.isAppSelection) {
                    OnboardingAppSelection(
                        viewModel = viewModel,
                        onSelectionChange = { count ->
                            selectedAppsCount = count
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Modern action button with gradient
                Button(
                    onClick = currentStepData.onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Text(
                        text = currentStepData.actionText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Progress indicators with animation
                Spacer(modifier = Modifier.height(40.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(steps.size) { index ->
                        val isActive = index == currentStep
                        val isCompleted = index < currentStep
                        
                        val targetWidth by animateDpAsState(
                            targetValue = if (isActive) 40.dp else 12.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "width"
                        )
                        
                        val targetColor by animateColorAsState(
                            targetValue = when {
                                isCompleted -> MaterialTheme.colorScheme.primary
                                isActive -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            label = "color"
                        )
                        
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .width(targetWidth)
                                .height(12.dp)
                                .background(
                                    color = targetColor,
                                    shape = RoundedCornerShape(6.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

data class OnboardingStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val actionText: String,
    val onAction: () -> Unit,
    val checkPermission: (() -> Boolean)? = null,
    val isAppSelection: Boolean = false
)

@Composable
fun OnboardingAppSelection(
    viewModel: AppSelectionViewModel,
    onSelectionChange: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val apps = uiState.apps
    val selectedApps = remember { mutableStateMapOf<String, Int>() } // packageName -> limit in minutes
    
    LaunchedEffect(selectedApps.size) {
        onSelectionChange(selectedApps.size)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Popular apps to monitor:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = apps.take(10), // Show top 10 apps
                    key = { it.packageName }
                ) { app ->
                    OnboardingAppItem(
                        app = app,
                        isSelected = selectedApps.containsKey(app.packageName),
                        selectedLimit = selectedApps[app.packageName] ?: 60,
                        onToggle = { selected ->
                            if (selected) {
                                selectedApps[app.packageName] = 60 // Default 60 minutes
                                viewModel.toggleAppSelection(app.packageName)
                            } else {
                                selectedApps.remove(app.packageName)
                                viewModel.toggleAppSelection(app.packageName)
                            }
                        },
                        onLimitChange = { newLimit ->
                            selectedApps[app.packageName] = newLimit
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingAppItem(
    app: com.example.braintrap.data.model.AppInfo,
    isSelected: Boolean,
    selectedLimit: Int,
    onToggle: (Boolean) -> Unit,
    onLimitChange: (Int) -> Unit
) {
    var showLimitPicker by remember { mutableStateOf(false) }
    
    Card(
        onClick = { onToggle(!isSelected) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onToggle
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (isSelected) {
                        Text(
                            text = "$selectedLimit min/day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            if (isSelected) {
                FilledTonalButton(
                    onClick = { showLimitPicker = true },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(
                        text = "Set Limit",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
    
    if (showLimitPicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLimitPicker = false },
            title = { Text("Set Daily Limit") },
            text = {
                Column {
                    Text("How many minutes per day for ${app.appName}?")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val limits = listOf(15, 30, 60, 90, 120, 180)
                    limits.forEach { limit ->
                        FilterChip(
                            selected = selectedLimit == limit,
                            onClick = { 
                                onLimitChange(limit)
                                showLimitPicker = false
                            },
                            label = { Text("$limit min") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLimitPicker = false }) {
                    Text("Done")
                }
            }
        )
    }
}


