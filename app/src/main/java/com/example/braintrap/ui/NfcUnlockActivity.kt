package com.example.braintrap.ui

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.braintrap.service.AppBlockingService
import com.example.braintrap.ui.theme.BrainTrapTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NfcUnlockActivity : ComponentActivity() {
    
    private var nfcAdapter: NfcAdapter? = null
    private var packageName: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        packageName = intent.getStringExtra("PACKAGE_NAME")
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        setContent {
            BrainTrapTheme {
                NfcUnlockScreen(
                    hasNfc = nfcAdapter != null,
                    onCancel = { finish() }
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        setupForegroundDispatch()
    }
    
    override fun onPause() {
        super.onPause()
        stopForegroundDispatch()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                handleNfcTag(tag)
            }
        }
    }
    
    private fun setupForegroundDispatch() {
        nfcAdapter?.let { adapter ->
            val intent = Intent(this, javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_MUTABLE
            )
            val filters = arrayOf(
                IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            )
            adapter.enableForegroundDispatch(this, pendingIntent, filters, null)
        }
    }
    
    private fun stopForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(this)
    }
    
    private fun handleNfcTag(tag: Tag) {
        val tagId = tag.id.joinToString(":") { String.format("%02X", it) }
        val prefs = getSharedPreferences("braintrap_prefs", MODE_PRIVATE)
        val registeredTagId = prefs.getString("registered_nfc_tag", null)
        
        if (registeredTagId == null) {
            // No tag registered, show error
            setContent {
                BrainTrapTheme {
                    NfcErrorScreen(
                        message = "No NFC tag registered. Please register a tag in Settings first.",
                        onDismiss = { finish() }
                    )
                }
            }
            return
        }
        
        if (tagId != registeredTagId) {
            // Wrong tag, show error
            setContent {
                BrainTrapTheme {
                    NfcErrorScreen(
                        message = "Wrong NFC tag. Please use your registered tag.",
                        onDismiss = { finish() }
                    )
                }
            }
            return
        }
        
        // Correct tag - grant temporary access and launch app
        packageName?.let { pkg ->
            val bonusMinutes = prefs.getInt("bonus_time_minutes", 15)
            
            AppBlockingService.instance?.temporarilyAllowApp(pkg, bonusMinutes)
            
            // Show success animation
            setContent {
                BrainTrapTheme {
                    NfcSuccessScreen(
                        onDismiss = {
                            launchApp(pkg)
                        }
                    )
                }
            }
            
            // Auto-launch app after 1.5 seconds
            window.decorView.postDelayed({
                launchApp(pkg)
            }, 1500)
        }
    }
    
    private fun launchApp(packageName: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(launchIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            finish()
        }
    }
}

@Composable
fun NfcUnlockScreen(
    hasNfc: Boolean,
    onCancel: () -> Unit
) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("NFC Unlock") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (hasNfc) {
                // NFC animation
                Text(
                    text = "📱",
                    fontSize = 120.sp,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                Text(
                    text = "Tap NFC Tag",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Place your phone near an NFC tag to unlock this app",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
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
                        Text(
                            text = "💡 How it works",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "• Hold your phone close to any NFC tag\n" +
                                    "• The app will unlock automatically\n" +
                                    "• You can use NFC cards, stickers, or key fobs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            } else {
                Text(
                    text = "⚠️",
                    fontSize = 80.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Text(
                    text = "NFC Not Available",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Your device doesn't support NFC or it's disabled. Please enable NFC in Settings or use Math Challenge mode instead.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go Back")
                }
            }
        }
    }
}

@Composable
fun NfcSuccessScreen(onDismiss: () -> Unit) {
    var scale by remember { mutableStateOf(0f) }
    var alpha by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        // Animate scale and alpha
        androidx.compose.animation.core.animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            )
        ) { value, _ ->
            scale = value
            alpha = value
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    alpha = alpha
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "✅",
                fontSize = 120.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Text(
                text = "Unlocked!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Launching app...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun NfcErrorScreen(
    message: String,
    onDismiss: () -> Unit
) {
    var shake by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        // Shake animation
        repeat(3) {
            animate(
                initialValue = -20f,
                targetValue = 20f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh
                )
            ) { value, _ ->
                shake = value
            }
            animate(
                initialValue = 20f,
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh
                )
            ) { value, _ ->
                shake = value
            }
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .graphicsLayer(
                    translationX = shake
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "❌",
                fontSize = 120.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Text(
                text = "Access Denied",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Close")
            }
        }
    }
}
