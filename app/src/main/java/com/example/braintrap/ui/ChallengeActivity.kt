package com.example.braintrap.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.braintrap.ui.theme.BrainTrapTheme
import com.example.braintrap.ui.viewmodel.ChallengeViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color

@AndroidEntryPoint
class ChallengeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent.getStringExtra("package_name") ?: ""
        val fromBlocking = intent.getBooleanExtra("from_blocking", false)
        val prefs = getSharedPreferences("braintrap_prefs", Context.MODE_PRIVATE)
        val bonusMinutes = prefs.getInt("bonus_time_minutes", 45)
        
        setContent {
            BrainTrapTheme {
                val viewModel: ChallengeViewModel = hiltViewModel()
                ChallengeScreen(
                    packageName = packageName,
                    viewModel = viewModel,
                    fromBlocking = fromBlocking,
                    bonusMinutes = bonusMinutes,
                    onComplete = {
                        // Launch the blocked app that user was trying to access
                        if (packageName.isNotEmpty()) {
                            try {
                                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(launchIntent)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        finish()
                    }
                )
            }
        }
    }
    
    companion object {
        fun createIntent(context: Context, packageName: String): Intent {
            return Intent(context, ChallengeActivity::class.java).apply {
                putExtra("package_name", packageName)
                putExtra("from_blocking", false)
            }
        }
    }
}

@Composable
fun ChallengeScreen(
    packageName: String,
    viewModel: ChallengeViewModel,
    fromBlocking: Boolean = false,
    bonusMinutes: Int = 45,
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.startChallenge(packageName)
    }
    
    // Show achievement notification
    uiState.newAchievement?.let { achievement ->
        AlertDialog(
            onDismissRequest = { },
            icon = { Text(achievement.icon, fontSize = 48.sp) },
            title = { Text("🏆 Achievement Unlocked!") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = { }
        )
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Motivational quote at top
            if (uiState.motivationalQuote.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = uiState.motivationalQuote,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🧮 Math Challenge",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        // Streak indicator
                        if (uiState.streak > 0) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🔥",
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${uiState.streak}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Problem ${uiState.currentProblem} of ${uiState.totalProblems}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (uiState.currentChallenge != null) {
                val challenge = uiState.currentChallenge
                
                // Timer Card with pulsing animation when low
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (uiState.timeRemaining < 10) 1.05f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "timer_pulse"
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(scale),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            uiState.timeRemaining < 10 -> MaterialTheme.colorScheme.errorContainer
                            uiState.speedBonus -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏱️ ${uiState.timeRemaining}s",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (uiState.speedBonus) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "⚡ FAST!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Problem Card with slide animation
                AnimatedContent(
                    targetState = challenge?.problem ?: "",
                    transitionSpec = {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
                    },
                    label = "problem_transition"
                ) { problem ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = problem,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = when {
                                problem.length > 20 -> 36.sp
                                problem.length > 10 -> 42.sp
                                else -> 48.sp
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Answer feedback
                AnimatedVisibility(
                    visible = uiState.lastAnswerCorrect != null,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.lastAnswerCorrect == true)
                                Color(0xFF4CAF50).copy(alpha = 0.2f)
                            else
                                Color(0xFFF44336).copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = if (uiState.lastAnswerCorrect == true) "✓ Correct!" else "✗ Wrong!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = if (uiState.lastAnswerCorrect == true)
                                Color(0xFF2E7D32)
                            else
                                Color(0xFFC62828)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                var answerInput by remember { mutableStateOf("") }
                
                OutlinedTextField(
                    value = answerInput,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '-' }) answerInput = it },
                    label = { Text("Your Answer") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Button(
                    onClick = {
                        answerInput.toIntOrNull()?.let { answer ->
                            viewModel.submitAnswer(answer)
                            answerInput = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    enabled = answerInput.isNotBlank()
                ) {
                    Text(
                        "Submit Answer",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (uiState.isCompleted) {
                Spacer(modifier = Modifier.height(32.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "✓",
                            fontSize = 64.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Challenge Complete!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "You've earned $bonusMinutes minutes of additional time!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The app will unlock automatically",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onComplete,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                "Continue",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Auto-close after 2 seconds
                LaunchedEffect(uiState.isCompleted) {
                    if (uiState.isCompleted) {
                        kotlinx.coroutines.delay(2000)
                        onComplete()
                    }
                }
            }
            
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

