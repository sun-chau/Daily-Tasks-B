package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.TaskViewModel
import com.example.ui.components.BrutalistBadge
import com.example.ui.components.BrutalistButton
import com.example.ui.components.BrutalistCard
import com.example.ui.theme.BrutalistAmber
import com.example.ui.theme.BrutalistBlack
import com.example.ui.theme.BrutalistCyan
import com.example.ui.theme.BrutalistLime
import com.example.ui.theme.BrutalistRed
import com.example.ui.theme.BrutalistWhite
import com.example.ui.theme.BrutalistYellow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val highContrast by viewModel.highContrastEnabled.collectAsStateWithLifecycle()
    val remindersEnabled by viewModel.remindersEnabled.collectAsStateWithLifecycle()
    val reminderHour by viewModel.reminderHour.collectAsStateWithLifecycle()
    val reminderMinute by viewModel.reminderMinute.collectAsStateWithLifecycle()
    val preClassAlerts by viewModel.preClassAlertsEnabled.collectAsStateWithLifecycle()
    val preClassLeadTime by viewModel.preClassLeadTimeMinutes.collectAsStateWithLifecycle()
    val weeklyGoal by viewModel.weeklyGoal.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val hapticIntensity by viewModel.hapticIntensity.collectAsStateWithLifecycle()
    val collegeStart by viewModel.collegeStartTime.collectAsStateWithLifecycle()
    val collegeEnd by viewModel.collegeEndTime.collectAsStateWithLifecycle()
    val attendanceThreshold by viewModel.attendanceThresholdPercent.collectAsStateWithLifecycle()
    val collegeWeekendDays by viewModel.collegeWeekendDays.collectAsStateWithLifecycle()
    val quietHoursEnabled by viewModel.quietHoursEnabled.collectAsStateWithLifecycle()
    val quietHoursStart by viewModel.quietHoursStart.collectAsStateWithLifecycle()
    val quietHoursEnd by viewModel.quietHoursEnd.collectAsStateWithLifecycle()
    val semesterStartDate by viewModel.semesterStartDate.collectAsStateWithLifecycle()
    val semesterEndDate by viewModel.semesterEndDate.collectAsStateWithLifecycle()
    val timelineTrackEnabled by viewModel.timelineTrackEnabled.collectAsStateWithLifecycle()
    val colorCodedSubjectsEnabled by viewModel.colorCodedSubjectsEnabled.collectAsStateWithLifecycle()
    val horizontalHeatMapEnabled by viewModel.horizontalHeatMapEnabled.collectAsStateWithLifecycle()

    var showWipeModal by remember { mutableStateOf(false) }
    var showBackupRestoreModal by remember { mutableStateOf(false) }
    var isCreditsExpanded by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission required for alerts", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Header banner
            BrutalistCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = BrutalistYellow,
                borderColor = MaterialTheme.colorScheme.outline
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrutalistBadge(
                            text = "PREFERENCES & CONFIG",
                            backgroundColor = BrutalistBlack,
                            textColor = BrutalistYellow
                        )
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = BrutalistBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "SYSTEM SETTINGS",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        color = BrutalistBlack
                    )
                }
            }
        }

        // Section Card 1: NOTIFICATIONS & ALARMS
        item {
            BrutalistCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NOTIFICATIONS & ALARMS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        Icon(imageVector = Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Daily habit reminder switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DAILY HABIT REMINDER", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            val formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s",
                                if (reminderHour == 0 || reminderHour == 12) 12 else reminderHour % 12,
                                reminderMinute,
                                if (reminderHour >= 12) "PM" else "AM"
                            )
                            Text("Dispatches daily at $formattedTime", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = {
                                viewModel.setRemindersEnabled(it)
                                if (it && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = BrutalistBlack, checkedTrackColor = BrutalistYellow)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pre-Class Alert Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PRE-CLASS LECTURE ALERTS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("Notification before class starts", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = preClassAlerts,
                            onCheckedChange = {
                                viewModel.setPreClassAlertsEnabled(it)
                                if (it && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = BrutalistBlack, checkedTrackColor = BrutalistCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Pre-class lead time selector pills (5m, 10m, 15m, 30m)
                    Text("ALERT LEAD TIME:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 10, 15, 30).forEach { mins ->
                            val isSelected = preClassLeadTime == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) BrutalistCyan else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outline)
                                    .clickable { viewModel.setPreClassLeadTimeMinutes(mins) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${mins}M", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (isSelected) BrutalistBlack else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quiet Hours / DND Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("QUIET HOURS (DO NOT DISTURB)", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("Mute non-critical alarms ($quietHoursStart - $quietHoursEnd)", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = quietHoursEnabled,
                            onCheckedChange = { viewModel.setQuietHoursEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = BrutalistBlack, checkedTrackColor = BrutalistAmber)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Test Notification Button with Visual & Audio Indicator
                    BrutalistButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.triggerTestNotification()
                                Toast.makeText(context, "Test notification dispatched to status bar!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shadowOffset = 2.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TEST NOTIFICATION & SOUND (ALARM TEST)", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                        }
                    }
                }
            }
        }

        // Section Card 2: COLLEGE & TIMETABLE
        item {
            BrutalistCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COLLEGE & TIMETABLE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        Icon(imageVector = Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "College Hours: $collegeStart — $collegeEnd",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Semester Duration config
                    Text("SEMESTER TIMEFRAME:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("START", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrutalistCyan)
                                Text(semesterStartDate, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("END", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrutalistAmber)
                                Text(semesterEndDate, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // College Weekend Days selection
                    Text("COLLEGE WEEKEND DAYS (OFF DAYS):", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach { day ->
                            val isWeekend = collegeWeekendDays.contains(day)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isWeekend) BrutalistYellow else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outline)
                                    .clickable { viewModel.toggleCollegeWeekendDay(day) }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(day, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = if (isWeekend) BrutalistBlack else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Attendance Threshold Alert slider
                    Text(
                        text = "ATTENDANCE THRESHOLD ALERT: $attendanceThreshold%",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (attendanceThreshold < 75) BrutalistRed else BrutalistLime
                    )
                    Slider(
                        value = attendanceThreshold.toFloat(),
                        onValueChange = { viewModel.setAttendanceThreshold(it.toInt()) },
                        valueRange = 60f..90f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = BrutalistBlack,
                            activeTrackColor = BrutalistLime,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Academic Goal slider
                    Text(
                        text = "ACADEMIC GOAL TARGET: $weeklyGoal TASKS / WEEK",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Slider(
                        value = weeklyGoal.toFloat(),
                        onValueChange = { viewModel.setWeeklyGoal(it.toInt()) },
                        valueRange = 5f..50f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = BrutalistBlack,
                            activeTrackColor = BrutalistCyan,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }

        // Section Card 3: DISPLAY & HAPTICS
        item {
            BrutalistCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DISPLAY & HAPTICS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        Icon(imageVector = Icons.Default.BrightnessHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Theme selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isLightActive = !isDarkTheme
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isLightActive) BrutalistYellow else MaterialTheme.colorScheme.surface)
                                .border(2.dp, MaterialTheme.colorScheme.outline)
                                .clickable { viewModel.setDarkTheme(false) }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("LIGHT THEME", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (isLightActive) BrutalistBlack else MaterialTheme.colorScheme.onSurface)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isDarkTheme) BrutalistBlack else MaterialTheme.colorScheme.surface)
                                .border(2.dp, MaterialTheme.colorScheme.outline)
                                .clickable { viewModel.setDarkTheme(true) }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("DARK THEME", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (isDarkTheme) BrutalistWhite else MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // High Contrast Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("HIGH DYNAMIC CONTRAST", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("Sharpened borders and high luminance text", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = highContrast,
                            onCheckedChange = { viewModel.setHighContrastEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = BrutalistBlack, checkedTrackColor = BrutalistCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Timeline schedule track toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TIMELINE TRACK IN TIMETABLE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("Vertical connecting guide line for classes", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = timelineTrackEnabled,
                            onCheckedChange = { viewModel.setTimelineTrackEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = BrutalistBlack, checkedTrackColor = BrutalistYellow)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Color-coded subjects toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("COLOR-CODED SUBJECT TAGS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("Distinct vibrant accent borders by subject", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = colorCodedSubjectsEnabled,
                            onCheckedChange = { viewModel.setColorCodedSubjectsEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = BrutalistBlack, checkedTrackColor = BrutalistLime)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Haptic Intensity with visual preview and test pulse
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TACTILE HAPTIC INTENSITY:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(
                            text = "PREVIEW: $hapticIntensity",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            color = BrutalistLime
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("LOW", "MEDIUM", "HIGH").forEach { level ->
                            val isSelected = hapticIntensity == level
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) BrutalistLime else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outline)
                                    .clickable {
                                        viewModel.setHapticIntensity(level)
                                        com.example.util.FeedbackManager.performTaskCompletedFeedback(context)
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(level, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (isSelected) BrutalistBlack else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }

        // Section Card 4: DATA & DANGER ZONE
        item {
            BrutalistCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = BrutalistRed
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DATA & DANGER ZONE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = BrutalistRed
                        )
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = BrutalistRed, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Export JSON backup, restore data, reseed test fixtures, or erase database with 2-step verification.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Backup & Restore button
                    BrutalistButton(
                        onClick = { showBackupRestoreModal = true },
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = BrutalistCyan,
                        contentColor = BrutalistBlack,
                        shadowOffset = 2.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DATA BACKUP & RESTORE (JSON)", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Reseed demo (kept for testing as user noted)
                        BrutalistButton(
                            onClick = {
                                viewModel.reseedSampleData()
                                Toast.makeText(context, "Reseeded demo tasks & timetable", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            backgroundColor = BrutalistYellow,
                            contentColor = BrutalistBlack
                        ) {
                            Text("RESEED DEMO", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }

                        // Safe wipe with 2-step verification
                        BrutalistButton(
                            onClick = { showWipeModal = true },
                            modifier = Modifier.weight(1f),
                            backgroundColor = BrutalistRed,
                            contentColor = BrutalistWhite
                        ) {
                            Text("WIPE DATA...", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Section 5: Version & Collapsed Credits Card with Feedback Button
        item {
            BrutalistCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                borderColor = MaterialTheme.colorScheme.outline
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCreditsExpanded = !isCreditsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "COLLEGE PLANNER • v2.0",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Offline Room • Collection Widgets • Brutalist Architecture",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        Icon(
                            imageVector = if (isCreditsExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Toggle Credits",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isCreditsExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Built for college students seeking strict focus, automated academic timetable tracking, and streak consistency without cloud bloat.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Feedback button that opens user's email client
                        BrutalistButton(
                            onClick = {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:support@collegeplanner.app")
                                    putExtra(Intent.EXTRA_SUBJECT, "College Planner Feedback & Feature Request")
                                }
                                try {
                                    context.startActivity(Intent.createChooser(emailIntent, "Send Feedback via Email"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = BrutalistYellow,
                            contentColor = BrutalistBlack,
                            shadowOffset = 2.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SEND FEEDBACK VIA EMAIL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Two-Step Confirmation Modal with 5-Second Safety Countdown & Type 'DELETE'
    if (showWipeModal) {
        var confirmationText by remember { mutableStateOf("") }
        var countdown by remember { mutableIntStateOf(5) }

        LaunchedEffect(Unit) {
            while (countdown > 0) {
                delay(1000L)
                countdown--
            }
        }

        Dialog(onDismissRequest = { showWipeModal = false }) {
            BrutalistCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = BrutalistRed
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "CONFIRM COMPLETE DATA WIPE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = BrutalistRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will erase all daily objectives, class timetable lectures, streak metrics, and countdowns. This cannot be undone.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Type \"DELETE\" to confirm:",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = BrutalistRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = confirmationText,
                        onValueChange = { confirmationText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("DELETE", fontFamily = FontFamily.Monospace) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val canWipe = countdown == 0 && confirmationText.trim().equals("DELETE", ignoreCase = false)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BrutalistButton(
                            onClick = { showWipeModal = false },
                            modifier = Modifier.weight(1f),
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Text("CANCEL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        BrutalistButton(
                            onClick = {
                                if (canWipe) {
                                    viewModel.clearAllData()
                                    showWipeModal = false
                                    Toast.makeText(context, "Database completely wiped", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            backgroundColor = if (canWipe) BrutalistRed else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (canWipe) BrutalistWhite else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        ) {
                            val btnText = if (countdown > 0) "WAIT ($countdown)" else "ERASE ALL"
                            Text(btnText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // Full JSON Backup & Restore Modal
    if (showBackupRestoreModal) {
        var jsonInput by remember { mutableStateOf("") }
        var isExporting by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showBackupRestoreModal = false }) {
            BrutalistCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = BrutalistCyan
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "DATA BACKUP & RESTORE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Export your entire academic schedule and tasks as JSON to copy to clipboard, or paste a backup to restore.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    BrutalistButton(
                        onClick = {
                            coroutineScope.launch {
                                isExporting = true
                                val json = viewModel.exportDataToJson()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("CollegePlannerBackup", json)
                                clipboard.setPrimaryClip(clip)
                                isExporting = false
                                Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = BrutalistYellow,
                        contentColor = BrutalistBlack
                    ) {
                        Text(if (isExporting) "EXPORTING..." else "EXPORT BACKUP TO CLIPBOARD", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("RESTORE FROM JSON:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = { jsonInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        placeholder = { Text("Paste JSON backup here...", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BrutalistButton(
                            onClick = { showBackupRestoreModal = false },
                            modifier = Modifier.weight(1f),
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Text("CLOSE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        BrutalistButton(
                            onClick = {
                                if (jsonInput.isNotBlank()) {
                                    coroutineScope.launch {
                                        val success = viewModel.importDataFromJson(jsonInput.trim())
                                        if (success) {
                                            showBackupRestoreModal = false
                                            Toast.makeText(context, "Data successfully restored!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Invalid JSON backup format", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            backgroundColor = BrutalistCyan,
                            contentColor = BrutalistBlack
                        ) {
                            Text("RESTORE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
