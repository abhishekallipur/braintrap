package com.example.braintrap.ui

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
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
import com.example.braintrap.ui.theme.BrainTrapTheme

class NfcVerificationActivity : ComponentActivity() {
    
    private var nfcAdapter: NfcAdapter? = null
    private var verified = mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        // Check if launched by NFC intent
        handleIntent(intent)
        
        setContent {
            BrainTrapTheme {
                NfcVerificationScreen()
            }
        }
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
        if (verified.value) return
        
        val tagId = tag.id.joinToString(":") { String.format("%02X", it) }
        val prefs = getSharedPreferences("braintrap_prefs", MODE_PRIVATE)
        val registeredTag = prefs.getString("registered_nfc_tag", null)
        
        if (registeredTag == tagId) {
            verified.value = true
            
            // Return success result
            setResult(RESULT_OK)
            
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                finish()
            }, 800)
        } else {
            // Show error - wrong tag
            android.widget.Toast.makeText(
                this,
                "Wrong NFC tag. Please use your registered tag.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    @Composable
    fun NfcVerificationScreen() {
        val isVerified by verified
        
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
        
        val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isVerified) {
                            listOf(Color(0xFF4CAF50), Color(0xFF388E3C))
                        } else {
                            listOf(Color(0xFF1976D2), Color(0xFF0D47A1))
                        },
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
                // Icon
                Icon(
                    if (isVerified) Icons.Default.CheckCircle else Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .scale(if (isVerified) 1f else pulse),
                    tint = Color.White
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Title
                Text(
                    text = if (isVerified) "Verified!" else "🔒 Focus Mode Active",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
                            text = if (isVerified) "Access Granted" else "Tap NFC Tag to Continue",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = if (isVerified) {
                                "You can now access BrainTrap"
                            } else {
                                "BrainTrap is locked during Focus Mode.\nScan your NFC tag to access the app."
                            },
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Cancel button
                if (!isVerified) {
                    OutlinedButton(
                        onClick = { finish() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
