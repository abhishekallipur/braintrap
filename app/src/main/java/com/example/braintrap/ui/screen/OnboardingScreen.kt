package com.example.braintrap.ui.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.braintrap.util.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    context: Context,
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    
    val steps = remember {
        listOf(
        OnboardingStep(
            icon = Icons.Default.Star,
            title = "Welcome to Mindful Usage",
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
            title = "You're All Set!",
            description = "Now you can select apps to monitor and set your daily time limits.",
            actionText = "Start Using App",
            onAction = onComplete
        )
        )
    }
    
    // Check permissions periodically
    LaunchedEffect(currentStep) {
        while (true) {
            kotlinx.coroutines.delay(500)
            val step = steps.getOrNull(currentStep)
            if (step?.checkPermission?.invoke() == true) {
                if (currentStep < steps.size - 1) {
                    currentStep++
                }
            }
        }
    }
    
    val currentStepData = steps.getOrNull(currentStep)
    
    if (currentStepData != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Icon(
                imageVector = currentStepData.icon,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            Text(
                text = currentStepData.title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description
            Text(
                text = currentStepData.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Action Button
            Button(
                onClick = currentStepData.onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = currentStepData.actionText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Progress indicators
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(steps.size) { index ->
                    val isActive = index == currentStep
                    val isCompleted = index < currentStep
                    val size = if (isActive) 12.dp else 8.dp
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(size),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(size),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else if (isActive) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(size),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(size)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                        }
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
    val checkPermission: (() -> Boolean)? = null
)

