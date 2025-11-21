package com.example.braintrap.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.braintrap.ui.viewmodel.ActivityGridViewModel
import java.text.SimpleDateFormat
import java.util.*

data class DayActivity(
    val date: Date,
    val totalBlockedApps: Int,
    val exceededApps: Int, // Apps that exceeded their limit
    val stayedWithinLimit: Int, // Apps that stayed within limit
    val totalTimeSavedMinutes: Long
) {
    val isSuccess: Boolean get() = exceededApps == 0 && totalBlockedApps > 0
    val isPartialSuccess: Boolean get() = exceededApps > 0 && stayedWithinLimit > 0
    val isFailure: Boolean get() = exceededApps > 0 && stayedWithinLimit == 0
    val isEmpty: Boolean get() = totalBlockedApps == 0
    
    val color: Color
        get() = when {
            isEmpty -> Color(0xFFEBEDF0) // Gray - no data
            isSuccess -> Color(0xFF39D353) // Green - success
            isPartialSuccess -> Color(0xFFFFA500) // Orange - partial
            isFailure -> Color(0xFFFF4444) // Red - failure
            else -> Color(0xFFEBEDF0)
        }
    
    val intensity: Float
        get() = when {
            isEmpty -> 0f
            isSuccess -> 1f
            isPartialSuccess -> 0.6f
            else -> 0.3f
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityGridScreen(
    viewModel: ActivityGridViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    var selectedDay by remember { mutableStateOf<DayActivity?>(null) }
    
    // Get real activity data from ViewModel
    val activityData by viewModel.activityData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Overview") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
            item {
                Text(
                    text = "Your Activity",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Daily adherence to app time limits",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                // Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Success Days",
                        value = activityData.count { it.isSuccess }.toString(),
                        color = Color(0xFF39D353),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Partial",
                        value = activityData.count { it.isPartialSuccess }.toString(),
                        color = Color(0xFFFFA500),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Failed",
                        value = activityData.count { it.isFailure }.toString(),
                        color = Color(0xFFFF4444),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                // Contribution Grid
                ContributionGrid(
                    activities = activityData,
                    onDayClick = { day -> selectedDay = day }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Less",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LegendBox(Color(0xFFEBEDF0))
                    LegendBox(Color(0xFFFF4444).copy(alpha = 0.3f))
                    LegendBox(Color(0xFFFFA500))
                    LegendBox(Color(0xFF39D353))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "More",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Selected Day Details
            item {
                selectedDay?.let { day ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(day.date),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            DetailRow("Apps Monitored", day.totalBlockedApps.toString())
                            DetailRow("Stayed Within Limit", day.stayedWithinLimit.toString(), Color(0xFF39D353))
                            DetailRow("Exceeded Limit", day.exceededApps.toString(), Color(0xFFFF4444))
                            DetailRow("Time Saved", "${day.totalTimeSavedMinutes} min")
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val status = when {
                                day.isEmpty -> "No data"
                                day.isSuccess -> "Perfect Day! 🎉"
                                day.isPartialSuccess -> "Could be better"
                                else -> "Exceeded limits"
                            }
                            
                            Text(
                                text = status,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = day.color
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun ContributionGrid(
    activities: List<DayActivity>,
    onDayClick: (DayActivity) -> Unit
) {
    val weeks = activities.chunked(7)
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Month labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, bottom = 4.dp)
        ) {
            val months = getMonthLabels(activities)
            months.forEach { (month, width) ->
                Text(
                    text = month,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width((width * 16).dp),
                    fontSize = 10.sp
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Day labels
            Column(
                modifier = Modifier.padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Mon", "", "Wed", "", "Fri", "", "Sun").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.height(12.dp),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weeks.forEach { week ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { day ->
                            ActivityBox(
                                activity = day,
                                onClick = { onDayClick(day) }
                            )
                        }
                        // Fill empty days in incomplete week
                        repeat(7 - week.size) {
                            Spacer(modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityBox(
    activity: DayActivity,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(activity.color.copy(alpha = if (activity.isEmpty) 0.2f else activity.intensity))
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                shape = RoundedCornerShape(2.dp)
            )
            .clickable {
                isPressed = true
                onClick()
            }
    )
}

@Composable
fun LegendBox(color: Color) {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .size(12.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

fun generateActivityData(): List<DayActivity> {
    val calendar = Calendar.getInstance()
    val today = calendar.time
    calendar.add(Calendar.DAY_OF_YEAR, -365)
    
    val activities = mutableListOf<DayActivity>()
    
    while (calendar.time <= today) {
        val random = Random(calendar.timeInMillis)
        val totalApps = random.nextInt(6) // 0-5 apps
        val exceededApps = if (totalApps > 0) random.nextInt(totalApps + 1) else 0
        val stayedWithin = totalApps - exceededApps
        val timeSaved = if (stayedWithin > 0) random.nextInt(120).toLong() else 0L
        
        activities.add(
            DayActivity(
                date = calendar.time.clone() as Date,
                totalBlockedApps = totalApps,
                exceededApps = exceededApps,
                stayedWithinLimit = stayedWithin,
                totalTimeSavedMinutes = timeSaved
            )
        )
        
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }
    
    return activities
}

fun getMonthLabels(activities: List<DayActivity>): List<Pair<String, Int>> {
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    val labels = mutableListOf<Pair<String, Int>>()
    var currentMonth = ""
    var weekCount = 0
    
    activities.chunked(7).forEach { week ->
        week.firstOrNull()?.let { day ->
            val month = monthFormat.format(day.date)
            if (month != currentMonth) {
                if (currentMonth.isNotEmpty()) {
                    labels.add(Pair(currentMonth, weekCount))
                }
                currentMonth = month
                weekCount = 1
            } else {
                weekCount++
            }
        }
    }
    
    if (currentMonth.isNotEmpty()) {
        labels.add(Pair(currentMonth, weekCount))
    }
    
    return labels
}
