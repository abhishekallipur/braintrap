package com.example.braintrap.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.braintrap.admin.DeviceAdminReceiver
import com.example.braintrap.service.AppBlockingService
import com.example.braintrap.ui.theme.BrainTrapTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FocusModeBlockActivity : ComponentActivity() {
    
    private var nfcAdapter: NfcAdapter? = null
    private var countDownTimer: CountDownTimer? = null
    private var packageName: String = ""
    private var timeRemaining = mutableStateOf(10)
    private var isUnlocked = mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        packageName = intent.getStringExtra("PACKAGE_NAME") ?: ""
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        // Check if launched by NFC intent
        handleIntent(intent)
        
        // Check and request device admin if not active
        checkDeviceAdmin()
        
        setContent {
            BrainTrapTheme {
                FocusModeBlockScreen()
            }
        }
        
        // Start 10 second countdown to shutdown
        startShutdownCountdown()
    }
    
    private fun handleIntent(intent: Intent?) {
        intent?.let {
            if (NfcAdapter.ACTION_TAG_DISCOVERED == it.action ||
                NfcAdapter.ACTION_NDEF_DISCOVERED == it.action ||
                NfcAdapter.ACTION_TECH_DISCOVERED == it.action) {
                
                val tag = it.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
                tag?.let { nfcTag -> handleNfcTag(nfcTag) }
            }
        }
    }
    
    private fun checkDeviceAdmin() {
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, DeviceAdminReceiver::class.java)
        
        if (!devicePolicyManager.isAdminActive(componentName)) {
            // Request device admin activation
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "BrainTrap needs Device Admin permission to lock your device when Focus Mode timer expires.")
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Could not activate device admin
            }
        }
    }
    
    private fun startShutdownCountdown() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining.value = (millisUntilFinished / 1000).toInt()
            }
            
            override fun onFinish() {
                timeRemaining.value = 0
                // Shutdown the device
                shutdownDevice()
            }
        }.start()
    }
    
    private fun shutdownDevice() {
        try {
            // Method 1: Try using root command for actual shutdown
            try {
                Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot -p"))
            } catch (e: Exception) {
                // If root not available, try non-root shutdown
            }
            
            // Method 2: Use device admin to force lock and then attempt shutdown
            val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val componentName = ComponentName(this, DeviceAdminReceiver::class.java)
            
            if (devicePolicyManager.isAdminActive(componentName)) {
                // Lock the device first
                devicePolicyManager.lockNow()
                
                // Try broadcast for shutdown (may work on some devices)
                try {
                    val intent = Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN")
                    intent.putExtra("android.intent.extra.KEY_CONFIRM", false)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    sendBroadcast(intent)
                } catch (e: Exception) {
                    // Ignore
                }
                
                // Try another method
                try {
                    val intent = Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN")
                    intent.putExtra("android.intent.extra.KEY_CONFIRM", false)
                    intent.flags = Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                } catch (e: Exception) {
                    // Ignore
                }
            }
            
            // Delay before finishing to ensure lock takes effect
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                finish()
            }, 1000)
        } catch (e: Exception) {
            finish()
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
    
    override fun onResume() {
        super.onResume()
        
        // Enable NFC foreground dispatch
        nfcAdapter?.let { adapter ->
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = android.app.PendingIntent.getActivity(
                this, 0, intent,
                android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            adapter.enableForegroundDispatch(this, pendingIntent, null, null)
        }
    }
    
    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }
    
    private fun handleNfcTag(tag: Tag) {
        // Prevent duplicate processing
        if (isUnlocked.value) return
        
        val tagId = tag.id.joinToString(":") { String.format("%02X", it) }
        val prefs = getSharedPreferences("braintrap_prefs", MODE_PRIVATE)
        val registeredTag = prefs.getString("registered_nfc_tag", null)
        
        if (registeredTag == tagId) {
            // Mark as unlocked
            isUnlocked.value = true
            
            // Cancel shutdown timer
            countDownTimer?.cancel()
            timeRemaining.value = -1 // Special value to indicate unlocked
            
            // Grant temporary access for 10 minutes
            AppBlockingService.instance?.temporarilyAllowApp(packageName, 10)
            
            // Show notification about temporary access
            android.widget.Toast.makeText(
                this,
                "Access granted for 10 minutes. App will be blocked again after that.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            
            // Small delay to show success, then launch app
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                }
                finish()
            }, 500)
        } else {
            // Show error - wrong tag
            android.widget.Toast.makeText(
                this,
                "Wrong NFC tag! Use registered tag to unlock.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    @Composable
    fun FocusModeBlockScreen() {
        val currentTime by timeRemaining
        val unlocked by isUnlocked
        
        val infiniteTransition = rememberInfiniteTransition(label = "background")
        val gradientOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "gradient"
        )
        
        val scale by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFD32F2F),
                            Color(0xFFB71C1C)
                        ),
                        startY = gradientOffset,
                        endY = gradientOffset + 500f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                // Warning Icon
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale),
                    tint = Color.White
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Focus Mode Title
                Text(
                    text = "🔒 FOCUS MODE ACTIVE",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Countdown Timer or Success
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (unlocked) Color.Green.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (unlocked) {
                        Text(
                            text = "✓",
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 48.dp, vertical = 24.dp)
                        )
                    } else {
                        Text(
                            text = "$currentTime",
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 48.dp, vertical = 24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Instructions
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = if (unlocked) "Unlocked!" else "Tap NFC Tag to Unlock",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = if (unlocked) "Opening app..." else "Device will shutdown in $currentTime seconds if NFC is not scanned",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Cancel button
                OutlinedButton(
                    onClick = { finish() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Close App")
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
