package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TaskEntity
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
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimetableScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val allEvents by viewModel.allTimetableEvents.collectAsStateWithLifecycle()
    val todayCode = viewModel.todayDayOfWeek
    val context = LocalContext.current
    val timelineTrackEnabled by viewModel.timelineTrackEnabled.collectAsStateWithLifecycle()
    val colorCodedSubjectsEnabled by viewModel.colorCodedSubjectsEnabled.collectAsStateWithLifecycle()
    val attendanceTargetPct by viewModel.attendanceThresholdPercent.collectAsStateWithLifecycle()

    var selectedDayFilter by remember { mutableStateOf(todayCode) }
    var showAddDialog by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<TaskEntity?>(null) }
    var eventToInlineEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var showWeekOverviewModal by remember { mutableStateOf(false) }
    var showAttendanceCalculatorModal by remember { mutableStateOf(false) }
    var eventForNotes by remember { mutableStateOf<TaskEntity?>(null) }

    // Academic week days: Mon through Sun (ALL tab removed in favor of Week Overview button)
    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    val filteredEvents = remember(allEvents, selectedDayFilter) {
        val list = allEvents.filter { event ->
            event.daysOfWeek.contains(selectedDayFilter, ignoreCase = true) ||
                    event.daysOfWeek == "ALL" ||
                    event.daysOfWeek.isBlank()
        }
        list.sortedBy { TaskViewModel.parseTimeToMinutes(it.startTime) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Brutalist Banner
            BrutalistCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = BrutalistCyan,
                borderColor = MaterialTheme.colorScheme.outline
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrutalistBadge(
                            text = "COLLEGE SCHEDULE",
                            backgroundColor = BrutalistBlack,
                            textColor = BrutalistWhite
                        )
                        BrutalistBadge(
                            text = "TODAY: $todayCode",
                            backgroundColor = BrutalistBlack,
                            textColor = BrutalistYellow
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ACADEMIC TIMETABLE",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        color = BrutalistBlack
                    )
                }
            }
        }

        // Action Deck: Add Lecture + Week Overview + Bunk Calculator + Copy Day
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BrutalistButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("add_timetable_event_button"),
                    backgroundColor = BrutalistYellow,
                    contentColor = BrutalistBlack,
                    shadowOffset = 2.dp,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "+ ADD CLASS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.5.sp,
                        color = BrutalistBlack
                    )
                }

                BrutalistButton(
                    onClick = { showWeekOverviewModal = true },
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("week_overview_button"),
                    backgroundColor = BrutalistCyan,
                    contentColor = BrutalistBlack,
                    shadowOffset = 2.dp,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "WEEK VIEW",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.5.sp,
                        color = BrutalistBlack
                    )
                }

                BrutalistButton(
                    onClick = { showAttendanceCalculatorModal = true },
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("bunk_calculator_button"),
                    backgroundColor = BrutalistLime,
                    contentColor = BrutalistBlack,
                    shadowOffset = 2.dp,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "BUNK CALC",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.5.sp,
                        color = BrutalistBlack
                    )
                }

                BrutalistButton(
                    onClick = { showCopyDialog = true },
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("copy_timetable_day_button"),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowOffset = 2.dp,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "COPY",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Day Selector Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                days.forEach { day ->
                    val isSelected = selectedDayFilter == day
                    val isToday = day == todayCode
                    val isAllTab = day == "ALL"

                    Box(
                        modifier = Modifier
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.onBackground
                                    isToday -> BrutalistAmber.copy(alpha = 0.35f)
                                    isAllTab -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.5.dp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            .clickable {
                                com.example.util.FeedbackManager.performClickFeedback(context)
                                selectedDayFilter = day
                            }
                            .padding(horizontal = 11.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = if (isToday) "$day ★" else if (isAllTab) "ALL (WEEK)" else day,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Empty state
        if (filteredEvents.isEmpty()) {
            item {
                BrutalistCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "[ NO LECTURES SCHEDULED ]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No classes scheduled for $selectedDayFilter. Tap above to add a new lecture.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // List of Timetable Events with inline breaks and free periods
        items(filteredEvents.indices.toList(), key = { filteredEvents[it].id }) { index ->
            val event = filteredEvents[index]

            // Check if there is a free period gap between this class and the previous one
            if (index > 0) {
                val prevEvent = filteredEvents[index - 1]
                val prevEndMin = TaskViewModel.parseTimeToMinutes(prevEvent.endTime)
                val currentStartMin = TaskViewModel.parseTimeToMinutes(event.startTime)
                val breakMinutes = currentStartMin - prevEndMin
                if (breakMinutes in 10..240) {
                    BreakIndicatorCard(breakMinutes = breakMinutes)
                }
            }

            TimetableEventCard(
                event = event,
                onDelete = { eventToDelete = event },
                onEdit = { eventToInlineEdit = event },
                onToggleHoliday = { viewModel.toggleHolidayCancellation(event) },
                onAttend = { viewModel.markAttendance(event, attended = true) },
                onMiss = { viewModel.markAttendance(event, attended = false) },
                onOpenNotes = { eventForNotes = event },
                targetAttendancePct = attendanceTargetPct,
                colorCoded = colorCodedSubjectsEnabled,
                timelineTrack = timelineTrackEnabled
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Week Overview Modal
    if (showWeekOverviewModal) {
        WeekOverviewModal(
            allEvents = allEvents,
            onDismiss = { showWeekOverviewModal = false }
        )
    }

    // Attendance & Bunk Margin Calculator Modal
    if (showAttendanceCalculatorModal) {
        AttendanceCalculatorModal(
            allEvents = allEvents,
            targetPct = attendanceTargetPct,
            onDismiss = { showAttendanceCalculatorModal = false }
        )
    }

    // Lecture Notes Dialog
    eventForNotes?.let { event ->
        LectureNotesDialog(
            event = event,
            onDismiss = { eventForNotes = null },
            onSave = { updatedNotes ->
                viewModel.updateLectureNotes(event, updatedNotes)
                eventForNotes = null
            }
        )
    }

    // Delete Confirmation Dialog
    eventToDelete?.let { event ->
        Dialog(onDismissRequest = { eventToDelete = null }) {
            BrutalistCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = BrutalistRed
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "DELETE LECTURE?",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = BrutalistRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Are you sure you want to delete \"${event.title}\" from your timetable?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BrutalistButton(
                            onClick = { eventToDelete = null },
                            modifier = Modifier.weight(1f),
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Text(
                                text = "CANCEL",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        BrutalistButton(
                            onClick = {
                                viewModel.deleteTask(event)
                                eventToDelete = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_delete_timetable_button"),
                            backgroundColor = BrutalistRed,
                            contentColor = BrutalistWhite
                        ) {
                            Text(
                                text = "DELETE",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = BrutalistWhite
                            )
                        }
                    }
                }
            }
        }
    }

    // Quick Inline Edit Dialog
    eventToInlineEdit?.let { event ->
        InlineEditTimetableDialog(
            event = event,
            onDismiss = { eventToInlineEdit = null },
            onSave = { updated ->
                viewModel.updateTimetableEvent(updated)
                eventToInlineEdit = null
            }
        )
    }

    // Copy Day Dialog
    if (showCopyDialog) {
        CopyDayDialog(
            currentDay = selectedDayFilter,
            onDismiss = { showCopyDialog = false },
            onConfirm = { sourceDay, targetDay ->
                viewModel.copyTimetableDay(sourceDay, targetDay)
                showCopyDialog = false
                Toast.makeText(context, "Copied $sourceDay schedule to $targetDay", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Add Lecture Dialog
    if (showAddDialog) {
        AddTimetableDialog(
            defaultDay = if (selectedDayFilter != "ALL") selectedDayFilter else todayCode,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, startTime, endTime, daysOfWeek, location, prof, label, reminder ->
                viewModel.addTimetableEvent(
                    title = title,
                    description = desc,
                    startTime = startTime,
                    endTime = endTime,
                    daysOfWeek = daysOfWeek,
                    location = location,
                    professor = prof,
                    customLabel = label,
                    hasReminder = reminder
                )
                showAddDialog = false
            }
        )
    }
}

/**
 * Free / Period Break Indicator Card
 */
@Composable
fun BreakIndicatorCard(breakMinutes: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            .padding(vertical = 5.dp, horizontal = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "☕ FREE BREAK / RECESS GAP",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = BrutalistAmber
            )
            Text(
                text = "${breakMinutes} MIN DURATION",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun TimetableEventCard(
    event: TaskEntity,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onToggleHoliday: () -> Unit,
    onAttend: () -> Unit,
    onMiss: () -> Unit,
    onOpenNotes: () -> Unit = {},
    targetAttendancePct: Int = 75,
    colorCoded: Boolean = true,
    timelineTrack: Boolean = true
) {
    val totalAttendance = event.attendedCount + event.missedCount
    val attendancePct = if (totalAttendance > 0) ((event.attendedCount.toFloat() / totalAttendance) * 100).toInt() else 100
    val isHoliday = event.isHolidayCancelled

    // Safe Margin / Bunk Calculation
    val targetRatio = (targetAttendancePct / 100.0).coerceIn(0.1, 0.99)
    val safeBunks = if (totalAttendance > 0 && attendancePct >= targetAttendancePct) {
        val maxTotalPossible = (event.attendedCount / targetRatio).toInt()
        maxOf(0, maxTotalPossible - totalAttendance)
    } else 0

    val classesNeeded = if (totalAttendance > 0 && attendancePct < targetAttendancePct) {
        val deficit = targetRatio * totalAttendance - event.attendedCount
        val needed = kotlin.math.ceil(deficit / (1.0 - targetRatio)).toInt()
        maxOf(1, needed)
    } else 0

    val subjectAccentColor = if (colorCoded && !isHoliday) {
        val colors = listOf(BrutalistCyan, BrutalistAmber, BrutalistLime, BrutalistYellow)
        val hash = kotlin.math.abs(event.title.trim().uppercase().hashCode())
        colors[hash % colors.size]
    } else {
        MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical timeline track indicator if enabled
        if (timelineTrack) {
            Column(
                modifier = Modifier
                    .width(14.dp)
                    .padding(end = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isHoliday) BrutalistAmber else subjectAccentColor)
                        .border(1.dp, BrutalistBlack)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(60.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                )
            }
        }

        BrutalistCard(
            modifier = Modifier.weight(1f),
            backgroundColor = if (isHoliday) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
            borderColor = if (isHoliday) BrutalistAmber else if (colorCoded) subjectAccentColor else MaterialTheme.colorScheme.outline,
            borderWidth = if (colorCoded && !isHoliday) 2.5.dp else 2.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Top badges row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val timeText = if (event.startTime.isNotBlank()) {
                            "${event.startTime} - ${event.endTime}"
                        } else {
                            "TIME TBA"
                        }
                        BrutalistBadge(
                            text = "◷ $timeText",
                            backgroundColor = BrutalistBlack,
                            textColor = BrutalistYellow
                        )

                        if (isHoliday) {
                            BrutalistBadge(
                                text = "NO CLASS (OFF)",
                                backgroundColor = BrutalistAmber,
                                textColor = BrutalistBlack
                            )
                        } else if (event.customLabel.isNotBlank()) {
                            BrutalistBadge(
                                text = event.customLabel,
                                backgroundColor = subjectAccentColor,
                                textColor = BrutalistBlack
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenNotes, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Lecture Notes",
                                tint = if (event.description.isNotBlank()) BrutalistAmber else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit event",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete event",
                                tint = BrutalistRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = event.title,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = if (isHoliday) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                )

                // Notes / Syllabus Attachment Indicator
                if (event.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            .clickable { onOpenNotes() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "📝 NOTES: ${event.description}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (event.professor.isNotBlank() || event.location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (event.professor.isNotBlank()) {
                            Text(
                                text = "👨‍🏫 ${event.professor}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = BrutalistCyan
                            )
                        }
                        if (event.location.isNotBlank()) {
                            Text(
                                text = "📍 ${event.location}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = BrutalistAmber
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Attendance tracker bar, Safe Margin, and Quick Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ATTENDANCE: $attendancePct%",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                color = if (attendancePct < targetAttendancePct) BrutalistRed else BrutalistLime
                            )
                            Text(
                                text = "(${event.attendedCount}/${totalAttendance})",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        // Safe Margin / Bunk calculation display
                        val marginText = when {
                            totalAttendance == 0 -> "Target: $targetAttendancePct% • No logs yet"
                            attendancePct >= targetAttendancePct -> "Safe to bunk: $safeBunks class${if (safeBunks == 1) "" else "es"}"
                            else -> "Shortage! Must attend $classesNeeded class${if (classesNeeded == 1) "" else "es"}"
                        }
                        Text(
                            text = marginText,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            color = if (attendancePct >= targetAttendancePct) BrutalistLime else BrutalistRed
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        // Attended button
                        Box(
                            modifier = Modifier
                                .background(BrutalistLime)
                                .border(1.dp, BrutalistBlack)
                                .clickable { onAttend() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text("+ATTENDED", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 8.5.sp, color = BrutalistBlack)
                        }

                        // Missed button
                        Box(
                            modifier = Modifier
                                .background(BrutalistRed)
                                .border(1.dp, BrutalistBlack)
                                .clickable { onMiss() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text("+MISSED", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 8.5.sp, color = BrutalistWhite)
                        }

                        // Holiday toggle
                        Box(
                            modifier = Modifier
                                .background(if (isHoliday) BrutalistAmber else MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                .clickable { onToggleHoliday() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(if (isHoliday) "OFF" else "HOLIDAY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Week Overview Modal displaying Mon through Sun timetable side-by-side
 */
@Composable
fun WeekOverviewModal(
    allEvents: List<TaskEntity>,
    onDismiss: () -> Unit
) {
    val weekDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        BrutalistCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp),
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
                        text = "WEEKLY SCHEDULE OVERVIEW",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(weekDays) { day ->
                        val dayEvents = allEvents.filter {
                            it.daysOfWeek.contains(day, ignoreCase = true) || it.daysOfWeek == "ALL" || it.daysOfWeek.isBlank()
                        }.sortedBy { TaskViewModel.parseTimeToMinutes(it.startTime) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.5.dp, MaterialTheme.colorScheme.outline)
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BrutalistBadge(
                                        text = day,
                                        backgroundColor = BrutalistBlack,
                                        textColor = BrutalistYellow
                                    )
                                    Text(
                                        text = "${dayEvents.size} LECTURES",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (dayEvents.isEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "No classes scheduled",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                } else {
                                    dayEvents.forEach { ev ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "• ${ev.title}",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${ev.startTime} - ${ev.endTime}",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                color = BrutalistAmber
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                BrutalistButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = BrutalistYellow,
                    contentColor = BrutalistBlack
                ) {
                    Text("CLOSE OVERVIEW", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

/**
 * Attendance & Bunk Safe-Margin Calculator Modal
 */
@Composable
fun AttendanceCalculatorModal(
    allEvents: List<TaskEntity>,
    targetPct: Int,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        BrutalistCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp),
            backgroundColor = MaterialTheme.colorScheme.surface,
            borderColor = BrutalistLime
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BUNK & ATTENDANCE CALCULATOR",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                BrutalistBadge(
                    text = "TARGET THRESHOLD: $targetPct%",
                    backgroundColor = BrutalistBlack,
                    textColor = BrutalistLime
                )

                Spacer(modifier = Modifier.height(10.dp))

                val uniqueSubjects = remember(allEvents) {
                    allEvents.distinctBy { it.title.trim().uppercase() }
                }

                if (uniqueSubjects.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("No courses registered in timetable.", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uniqueSubjects) { event ->
                            val total = event.attendedCount + event.missedCount
                            val pct = if (total > 0) ((event.attendedCount.toFloat() / total) * 100).toInt() else 100
                            val targetRatio = (targetPct / 100.0).coerceIn(0.1, 0.99)

                            val safeBunks = if (total > 0 && pct >= targetPct) {
                                val maxPossible = (event.attendedCount / targetRatio).toInt()
                                maxOf(0, maxPossible - total)
                            } else 0

                            val needed = if (total > 0 && pct < targetPct) {
                                val deficit = targetRatio * total - event.attendedCount
                                kotlin.math.ceil(deficit / (1.0 - targetRatio)).toInt()
                            } else 0

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.5.dp, if (pct < targetPct) BrutalistRed else BrutalistLime)
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = event.title,
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "$pct%",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = if (pct < targetPct) BrutalistRed else BrutalistLime
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${event.attendedCount} Attended • ${event.missedCount} Missed (Total: $total)",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))
                                    val statusMsg = when {
                                        total == 0 -> "No classes held yet."
                                        pct >= targetPct -> "🟢 SAFE: You can bunk $safeBunks class${if (safeBunks == 1) "" else "es"} and stay >= $targetPct%"
                                        else -> "🔴 SHORTAGE: You MUST attend next $needed consecutive class${if (needed == 1) "" else "es"} to recover to $targetPct%"
                                    }
                                    Text(
                                        text = statusMsg,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (pct >= targetPct) BrutalistLime else BrutalistRed
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                BrutalistButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = BrutalistLime,
                    contentColor = BrutalistBlack
                ) {
                    Text("DONE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

/**
 * Lecture Notes & Syllabus Quick Editor Dialog
 */
@Composable
fun LectureNotesDialog(
    event: TaskEntity,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var notesText by remember { mutableStateOf(event.description) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        BrutalistCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surface,
            borderColor = BrutalistAmber
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LECTURE NOTES & SYLLABUS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                    BrutalistBadge(
                        text = event.title.take(12),
                        backgroundColor = BrutalistBlack,
                        textColor = BrutalistAmber
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes, Topics Covered & Next Class Prep", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BrutalistButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Text("CANCEL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    BrutalistButton(
                        onClick = { onSave(notesText) },
                        modifier = Modifier.weight(1f),
                        backgroundColor = BrutalistAmber,
                        contentColor = BrutalistBlack
                    ) {
                        Text("SAVE NOTES", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

/**
 * Quick inline editing dialog for a lecture
 */
@Composable
fun InlineEditTimetableDialog(
    event: TaskEntity,
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit
) {
    var title by remember { mutableStateOf(event.title) }
    var location by remember { mutableStateOf(event.location) }
    var professor by remember { mutableStateOf(event.professor) }
    var startTime by remember { mutableStateOf(event.startTime) }
    var endTime by remember { mutableStateOf(event.endTime) }

    Dialog(onDismissRequest = onDismiss) {
        BrutalistCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surface,
            borderColor = MaterialTheme.colorScheme.outline
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "EDIT LECTURE DETAILS",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time", fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time", fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Venue / Room", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = professor,
                    onValueChange = { professor = it },
                    label = { Text("Professor", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BrutalistButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Text("CANCEL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    BrutalistButton(
                        onClick = {
                            onSave(event.copy(
                                title = title.trim(),
                                startTime = startTime.trim(),
                                endTime = endTime.trim(),
                                location = location.trim(),
                                professor = professor.trim()
                            ))
                        },
                        modifier = Modifier.weight(1f),
                        backgroundColor = BrutalistYellow,
                        contentColor = BrutalistBlack
                    ) {
                        Text("SAVE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/**
 * Copy/Duplicate day timetable dialog
 */
@Composable
fun CopyDayDialog(
    currentDay: String,
    onDismiss: () -> Unit,
    onConfirm: (sourceDay: String, targetDay: String) -> Unit
) {
    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
    var sourceDay by remember { mutableStateOf(if (currentDay in days) currentDay else "MON") }
    var targetDay by remember { mutableStateOf("TUE") }

    Dialog(onDismissRequest = onDismiss) {
        BrutalistCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surface,
            borderColor = MaterialTheme.colorScheme.outline
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "DUPLICATE DAY SCHEDULE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Quickly copy all lecture slots from one day into another day.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("FROM DAY:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    days.forEach { d ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (sourceDay == d) BrutalistBlack else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                            .clickable { sourceDay = d }
                            .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(d, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (sourceDay == d) BrutalistWhite else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("TO DAY:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    days.forEach { d ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (targetDay == d) BrutalistYellow else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                            .clickable { targetDay = d }
                            .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(d, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (targetDay == d) BrutalistBlack else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BrutalistButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Text("CANCEL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    BrutalistButton(
                        onClick = { onConfirm(sourceDay, targetDay) },
                        modifier = Modifier.weight(1f),
                        backgroundColor = BrutalistCyan,
                        contentColor = BrutalistBlack
                    ) {
                        Text("COPY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTimetableDialog(
    defaultDay: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, startTime: String, endTime: String, daysOfWeek: String, location: String, professor: String, label: String, reminder: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("10:00 AM") }
    var endTime by remember { mutableStateOf("11:00 AM") }
    var location by remember { mutableStateOf("") }
    var professor by remember { mutableStateOf("") }
    var customLabel by remember { mutableStateOf("LECTURE") }
    var hasReminder by remember { mutableStateOf(true) }

    val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
    val selectedDays = remember {
        mutableStateOf(
            if (defaultDay in allDays) mutableSetOf(defaultDay) else mutableSetOf("MON", "WED", "FRI")
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        BrutalistCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surface,
            borderColor = MaterialTheme.colorScheme.outline
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "NEW TIMETABLE LECTURE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Course / Lecture Title", fontFamily = FontFamily.Monospace) },
                    placeholder = { Text("e.g. Statistics, Physics Lab", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timetable_title_input"),
                    shape = RoundedCornerShape(0.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Venue / Room / Hall", fontFamily = FontFamily.Monospace) },
                    placeholder = { Text("e.g. Hall B - Room 204", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timetable_location_input"),
                    shape = RoundedCornerShape(0.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = professor,
                    onValueChange = { professor = it },
                    label = { Text("Professor / Instructor", fontFamily = FontFamily.Monospace) },
                    placeholder = { Text("e.g. Dr. Sharma, Prof. Alan Turing", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timetable_professor_input"),
                    shape = RoundedCornerShape(0.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("timetable_start_time_input"),
                        shape = RoundedCornerShape(0.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("timetable_end_time_input"),
                        shape = RoundedCornerShape(0.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "RECURRING DAYS:",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allDays.forEach { day ->
                        val isDaySelected = selectedDays.value.contains(day)
                        Box(
                            modifier = Modifier
                                .background(if (isDaySelected) BrutalistYellow else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.5.dp, MaterialTheme.colorScheme.outline)
                                .clickable {
                                    val current = selectedDays.value.toMutableSet()
                                    if (current.contains(day)) {
                                        if (current.size > 1) current.remove(day)
                                    } else {
                                        current.add(day)
                                    }
                                    selectedDays.value = current
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = day,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = if (isDaySelected) BrutalistBlack else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reminder switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PRE-CLASS ALERT REMINDER",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = hasReminder,
                        onCheckedChange = { hasReminder = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BrutalistYellow,
                            checkedTrackColor = BrutalistBlack
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BrutalistButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Text(
                            text = "CANCEL",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    BrutalistButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                val daysStr = selectedDays.value.joinToString(",")
                                onConfirm(
                                    title,
                                    description,
                                    startTime,
                                    endTime,
                                    daysStr,
                                    location,
                                    professor,
                                    customLabel,
                                    hasReminder
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_timetable_button"),
                        backgroundColor = BrutalistYellow,
                        contentColor = BrutalistBlack
                    ) {
                        Text(
                            text = "SAVE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = BrutalistBlack
                        )
                    }
                }
            }
        }
    }
}
