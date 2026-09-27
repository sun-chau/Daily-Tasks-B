package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ImportanceLevel
import com.example.data.TaskEntity
import com.example.ui.TaskViewModel
import com.example.ui.components.BrutalistBadge
import com.example.ui.components.BrutalistButton
import com.example.ui.components.BrutalistCard
import com.example.ui.components.BrutalistCheckbox
import com.example.ui.components.ChronometerDisplay
import com.example.ui.components.ImportanceBadge
import com.example.ui.theme.BrutalistAmber
import com.example.ui.theme.BrutalistBlack
import com.example.ui.theme.BrutalistCyan
import com.example.ui.theme.BrutalistLime
import com.example.ui.theme.BrutalistRed
import com.example.ui.theme.BrutalistWhite
import com.example.ui.theme.BrutalistYellow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier,
    autoOpenAddDialog: Boolean = false
) {
    val dailyTasks by viewModel.dailyTasks.collectAsStateWithLifecycle()
    val todayTimetable by viewModel.todayTimetable.collectAsStateWithLifecycle()
    val sortOrder by viewModel.taskSortOrder.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddExtraDialog by remember { mutableStateOf(false) }
    var showAddFutureDialog by remember { mutableStateOf(false) }
    var showAddStandardDialog by remember { mutableStateOf(autoOpenAddDialog) }
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }
    var isScheduleExpanded by remember { mutableStateOf(false) }

    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredTasks = remember(dailyTasks, selectedFilter, sortOrder) {
        val filtered = when (selectedFilter) {
            "VERY_IMPORTANT" -> dailyTasks.filter { it.importance == ImportanceLevel.VERY_IMPORTANT.name }
            "CONSISTENCY" -> dailyTasks.filter { it.needsConsistency }
            "EXTRA" -> dailyTasks.filter { it.isExtraTask }
            "ACTIVE" -> dailyTasks.filter { !it.isCompleted }
            "COMPLETED" -> dailyTasks.filter { it.isCompleted }
            else -> dailyTasks
        }

        when (sortOrder) {
            "IMPORTANCE" -> filtered.sortedWith(
                compareBy<TaskEntity> { it.isCompleted }
                    .thenBy {
                        when (it.importance) {
                            ImportanceLevel.VERY_IMPORTANT.name -> 0
                            ImportanceLevel.MEDIUM.name -> 1
                            else -> 2
                        }
                    }
                    .thenByDescending { it.needsConsistency }
            )
            "STATUS" -> filtered.sortedBy { it.isCompleted }
            "CHRONOLOGICAL" -> filtered.sortedBy { it.createdAt }
            else -> filtered // DEFAULT order preserved from database
        }
    }

    val completedCount = dailyTasks.count { it.isCompleted }
    val totalCount = dailyTasks.size
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Brutalist Header Banner
            BrutalistCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.primary,
                borderColor = MaterialTheme.colorScheme.outline
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrutalistBadge(
                            text = "TODAY'S AGENDA",
                            backgroundColor = BrutalistBlack,
                            textColor = BrutalistYellow
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Heading in full date format as requested
                    Text(
                        text = viewModel.todayFullHeading.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        color = BrutalistBlack,
                        modifier = Modifier.testTag("heading_full_date")
                    )
                }
            }
        }

        // Unified Action Button: combines Add Extra and Add Daily into a single streamlined deck
        item {
            BrutalistButton(
                onClick = { showAddStandardDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_task_unified_button"),
                backgroundColor = BrutalistYellow,
                contentColor = BrutalistBlack,
                shadowOffset = 3.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Task",
                        tint = BrutalistBlack,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ADD OBJECTIVE / EXTRA TASK",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = BrutalistBlack
                    )
                }
            }
        }

        // Today's Timetable / Classes Section - Space-saving "Now / Next Up" Hero Card
        if (todayTimetable.isNotEmpty()) {
            item {
                val nowMinutes = remember(todayTimetable) {
                    val cal = Calendar.getInstance()
                    cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                }

                val sortedSchedule = remember(todayTimetable) {
                    todayTimetable.sortedBy { TaskViewModel.parseTimeToMinutes(it.startTime) }
                }

                // Check for ongoing class: startTime <= nowMinutes < endTime
                val ongoingClass = sortedSchedule.firstOrNull { event ->
                    val start = TaskViewModel.parseTimeToMinutes(event.startTime)
                    val end = TaskViewModel.parseTimeToMinutes(event.endTime)
                    start <= nowMinutes && nowMinutes < end
                }

                // Check for next upcoming class: startTime > nowMinutes
                val nextClass = if (ongoingClass == null) {
                    sortedSchedule.firstOrNull { event ->
                        TaskViewModel.parseTimeToMinutes(event.startTime) > nowMinutes
                    }
                } else null

                val allEnded = ongoingClass == null && nextClass == null
                val lastEndedClass = if (allEnded) sortedSchedule.lastOrNull() else null
                val heroClass = ongoingClass ?: nextClass ?: lastEndedClass
                val isLiveNow = ongoingClass != null
                val isUpNext = ongoingClass == null && nextClass != null

                BrutalistCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("today_classes_hero_card"),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = if (isLiveNow) BrutalistLime else MaterialTheme.colorScheme.outline
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val statusBadgeText = when {
                                    isLiveNow -> "● LIVE NOW"
                                    isUpNext -> "UP NEXT"
                                    allEnded -> "ALL LECTURES ENDED"
                                    else -> "TODAY'S CLASSES"
                                }
                                val statusBadgeBg = when {
                                    isLiveNow -> BrutalistLime
                                    isUpNext -> BrutalistYellow
                                    allEnded -> BrutalistBlack
                                    else -> BrutalistCyan
                                }
                                val statusBadgeTextCol = when {
                                    allEnded -> BrutalistLime
                                    else -> BrutalistBlack
                                }

                                BrutalistBadge(
                                    text = statusBadgeText,
                                    backgroundColor = statusBadgeBg,
                                    textColor = statusBadgeTextCol
                                )
                                Text(
                                    text = "${sortedSchedule.size} CLASSES TODAY",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (sortedSchedule.size > 1 || allEnded) {
                                Box(
                                    modifier = Modifier
                                        .background(if (isScheduleExpanded) BrutalistBlack else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.5.dp, MaterialTheme.colorScheme.outline)
                                        .clickable { isScheduleExpanded = !isScheduleExpanded }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isScheduleExpanded) "COLLAPSE ▲" else if (allEnded) "VIEW ALL ▼" else "+${sortedSchedule.size - 1} MORE ▼",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = if (isScheduleExpanded) BrutalistYellow else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (allEnded) {
                            // Highlighting that all scheduled classes for today have concluded
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "✓ All ${sortedSchedule.size} classes completed for today",
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = BrutalistLime
                                    )
                                    lastEndedClass?.let {
                                        Text(
                                            text = "Ended ${it.endTime}",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        } else {
                            heroClass?.let { event ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isLiveNow) BrutalistLime.copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isLiveNow) BrutalistLime else MaterialTheme.colorScheme.outline
                                        )
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = event.title,
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (event.startTime.isNotBlank()) {
                                            val startMin = TaskViewModel.parseTimeToMinutes(event.startTime)
                                            val endMin = TaskViewModel.parseTimeToMinutes(event.endTime)
                                            val remainingText = if (isLiveNow) {
                                                val left = (endMin - nowMinutes).coerceAtLeast(0)
                                                "${left}m remaining"
                                            } else if (isUpNext) {
                                                val until = (startMin - nowMinutes).coerceAtLeast(0)
                                                "in ${until}m"
                                            } else ""

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (remainingText.isNotBlank()) {
                                                    BrutalistBadge(
                                                        text = "⏳ $remainingText",
                                                        backgroundColor = if (isLiveNow) BrutalistLime else BrutalistYellow,
                                                        textColor = BrutalistBlack
                                                    )
                                                }
                                                BrutalistBadge(
                                                    text = "${event.startTime} - ${event.endTime}",
                                                    backgroundColor = BrutalistBlack,
                                                    textColor = if (isLiveNow) BrutalistLime else BrutalistWhite
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (event.location.isNotBlank()) {
                                            Text(
                                                text = "📍 ${event.location}",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = BrutalistAmber
                                            )
                                        }
                                        if (event.professor.isNotBlank()) {
                                            Text(
                                                text = "👨‍🏫 ${event.professor}",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = BrutalistCyan
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = isScheduleExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val itemsToShow = if (allEnded) sortedSchedule else sortedSchedule.filter { it.id != heroClass?.id }
                                itemsToShow.forEach { classEvent ->
                                    val isClassPast = TaskViewModel.parseTimeToMinutes(classEvent.endTime) <= nowMinutes
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isClassPast) 0.25f else 0.45f))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isClassPast) "✓ ${classEvent.title}" else classEvent.title,
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isClassPast) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                if (classEvent.location.isNotBlank()) {
                                                    Text(
                                                        text = "📍 ${classEvent.location}",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 10.sp,
                                                        color = BrutalistAmber
                                                    )
                                                }
                                                if (classEvent.professor.isNotBlank()) {
                                                    Text(
                                                        text = "👨‍🏫 ${classEvent.professor}",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 10.sp,
                                                        color = BrutalistCyan
                                                    )
                                                }
                                            }
                                        }

                                        if (classEvent.startTime.isNotBlank()) {
                                            Text(
                                                text = "${classEvent.startTime} - ${classEvent.endTime}",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = if (isClassPast) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Filter / Tag tabs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "ALL (${dailyTasks.size})",
                        "ACTIVE" to "ACTIVE (${dailyTasks.count { !it.isCompleted }})",
                        "VERY_IMPORTANT" to "CRITICAL",
                        "CONSISTENCY" to "CONSISTENCY",
                        "EXTRA" to "EXTRA",
                        "COMPLETED" to "DONE"
                    )

                    filters.forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface)
                                .border(2.dp, MaterialTheme.colorScheme.outline)
                                .clickable {
                                    com.example.util.FeedbackManager.performClickFeedback(context)
                                    selectedFilter = key
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Sorting quick bar (Default, Importance, Chronological, Status)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SORT:",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    val sortOptions = listOf(
                        "DEFAULT" to "DEFAULT",
                        "IMPORTANCE" to "PRIORITY",
                        "STATUS" to "STATUS",
                        "CHRONOLOGICAL" to "TIME"
                    )

                    sortOptions.forEach { (key, label) ->
                        val isCurrentSort = sortOrder == key
                        Box(
                            modifier = Modifier
                                .background(if (isCurrentSort) BrutalistYellow else MaterialTheme.colorScheme.surface)
                                .border(1.5.dp, MaterialTheme.colorScheme.outline)
                                .clickable {
                                    com.example.util.FeedbackManager.performClickFeedback(context)
                                    viewModel.setTaskSortOrder(key)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (isCurrentSort) BrutalistBlack else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Empty state
        if (filteredTasks.isEmpty()) {
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
                            text = "[ NO TASKS FOUND ]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add an extra task or daily objective using the action buttons above.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Tasks list with checkboxes in front of each task
        items(filteredTasks, key = { it.id }) { task ->
            TaskItemRow(
                task = task,
                onToggle = { viewModel.toggleTask(task) },
                onLongPress = { taskToDelete = task },
                onDeleteQuick = { viewModel.deleteTask(task) },
                onRescheduleTomorrow = { viewModel.remindTomorrowEarly(task) }
            )
        }

        // Evening Reflection Card
        if (currentHour >= 18 || completedCount > 0) {
            item {
                BrutalistCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("evening_reflection_card"),
                    backgroundColor = BrutalistBlack,
                    borderColor = BrutalistYellow,
                    borderWidth = 2.5.dp,
                    shadowOffset = 3.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BrutalistBadge(
                                text = "EVENING REFLECTION",
                                backgroundColor = BrutalistYellow,
                                textColor = BrutalistBlack
                            )
                            Text(
                                text = "$completedCount OF $totalCount COMPLETED",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = BrutalistLime
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val reflectionMsg = when {
                            completedCount == totalCount && totalCount > 0 -> "All scheduled objectives finished! Great day of academic discipline. Rest well for tomorrow!"
                            completedCount > 0 -> "Solid progress: $completedCount done. Tap reschedule on uncompleted tasks to roll them over to tomorrow early."
                            else -> "Review your goals and prepare for tomorrow's classes!"
                        }
                        Text(
                            text = reflectionMsg,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = BrutalistWhite
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Long press delete confirmation dialog
    taskToDelete?.let { task ->
        Dialog(onDismissRequest = { taskToDelete = null }) {
            BrutalistCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = BrutalistRed
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "DELETE TASK?",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = BrutalistRed
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Are you sure you want to permanently delete \"${task.title}\"?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BrutalistButton(
                            onClick = { taskToDelete = null },
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
                                viewModel.deleteTask(task)
                                taskToDelete = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_delete_button"),
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

    // Dialog 1: Add Extra Task
    if (showAddExtraDialog) {
        AddExtraTaskDialog(
            onDismiss = { showAddExtraDialog = false },
            onConfirm = { title, desc, importance, label ->
                viewModel.addDailyTask(
                    title = title,
                    description = desc,
                    importance = importance,
                    customLabel = label,
                    needsConsistency = false,
                    isExtraTask = true
                )
                showAddExtraDialog = false
            }
        )
    }

    // Dialog 2: Add Future Task with Chronometer
    if (showAddFutureDialog) {
        AddFutureTaskDialog(
            onDismiss = { showAddFutureDialog = false },
            onConfirm = { title, desc, epochMillis, importance, label, reminder ->
                viewModel.addFutureTask(
                    title = title,
                    description = desc,
                    targetEpochMillis = epochMillis,
                    importance = importance,
                    customLabel = label,
                    hasReminder = reminder
                )
                showAddFutureDialog = false
            }
        )
    }

    // Dialog: Add Daily Objective / Extra Task (Unified)
    if (showAddStandardDialog) {
        AddDailyTaskDialog(
            onDismiss = { showAddStandardDialog = false },
            onConfirm = { title, desc, importance, label, consistency, isExtra ->
                viewModel.addDailyTask(
                    title = title,
                    description = desc,
                    importance = importance,
                    customLabel = label,
                    needsConsistency = consistency,
                    isExtraTask = isExtra
                )
                showAddStandardDialog = false
            }
        )
    }
}

/**
 * Task row item with prominent checkbox in front of each task as requested.
 * Long press card to delete task.
 * Daily Objective, Future Task, and Extra Task look distinct from each other.
 * Distinctive effect is applied only to importance tag, not the card itself.
 */
@Composable
fun TaskItemRow(
    task: TaskEntity,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onDeleteQuick: () -> Unit = {},
    onRescheduleTomorrow: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isVeryImportant = task.importance == ImportanceLevel.VERY_IMPORTANT.name

    // Visual differentiation matching the command deck button colors:
    // Daily Objective: Surface (matches Daily button)
    // Extra Task: BrutalistAmber (matches Extra button)
    // Future Task: BrutalistCyan (matches Future button)
    val (cardBg, cardBorder, shadowOffset) = when {
        task.isCompleted -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            2.5.dp
        )
        task.isExtraTask -> Triple(
            BrutalistAmber,
            BrutalistBlack,
            4.dp
        )
        task.isFutureTask -> Triple(
            BrutalistCyan,
            BrutalistBlack,
            4.dp
        )
        else -> Triple(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.outline,
            3.5.dp
        )
    }

    val taskContentColor = when {
        task.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
        task.isExtraTask || task.isFutureTask -> BrutalistBlack
        else -> MaterialTheme.colorScheme.onSurface
    }

    BrutalistCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}"),
        backgroundColor = cardBg,
        borderColor = cardBorder,
        borderWidth = 2.5.dp,
        shadowOffset = shadowOffset,
        onLongClick = onLongPress
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox in front of each task as requested
            BrutalistCheckbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("checkbox_${task.id}"),
                checkedColor = when {
                    task.isExtraTask -> BrutalistBlack
                    task.isFutureTask -> BrutalistBlack
                    else -> BrutalistYellow
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Tags row horizontally scrollable if tags don't fit in one line
                // Compact streamlined badge sizing to preserve space
                val hasAnyTags = task.needsConsistency ||
                        task.importance.isNotBlank() ||
                        task.customLabel.isNotBlank() ||
                        task.hasReminder

                if (hasAnyTags) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (task.needsConsistency) {
                            BrutalistBadge(
                                text = "⚡",
                                backgroundColor = BrutalistLime,
                                textColor = BrutalistBlack
                            )
                        }

                        ImportanceBadge(importance = task.importance)

                        if (task.customLabel.isNotBlank()) {
                            BrutalistBadge(
                                text = task.customLabel,
                                backgroundColor = BrutalistBlack,
                                textColor = BrutalistWhite
                            )
                        }

                        if (task.hasReminder) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "Reminder Active",
                                tint = if (task.isExtraTask || task.isFutureTask) BrutalistBlack else BrutalistAmber,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Title
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = taskContentColor
                )

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = taskContentColor.copy(alpha = 0.75f),
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                }

                // Future task chronometer display if scheduled for today
                if (task.isFutureTask && task.targetDateTime != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ChronometerDisplay(
                        targetEpochMillis = task.targetDateTime,
                        isCompleted = task.isCompleted
                    )
                }
            }

            // Quick Schedule / Action button (Google Gmail style quick action)
            if (!task.isCompleted && !task.isFutureTask) {
                IconButton(
                    onClick = onRescheduleTomorrow,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Remind Early Tomorrow",
                        tint = if (task.isExtraTask) BrutalistBlack else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            IconButton(
                onClick = onDeleteQuick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Quick Delete",
                    tint = if (task.isExtraTask || task.isFutureTask) BrutalistBlack.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Dialog for adding Extra Task completed that day.
 */
@Composable
fun AddExtraTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, importance: String, customLabel: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var importance by remember { mutableStateOf(ImportanceLevel.MEDIUM.name) }
    var customLabel by remember { mutableStateOf("BONUS") }

    Dialog(onDismissRequest = onDismiss) {
        BrutalistCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surface,
            borderColor = MaterialTheme.colorScheme.outline
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ADD EXTRA TASK",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    BrutalistBadge(
                        text = "DAY BONUS",
                        backgroundColor = BrutalistAmber,
                        textColor = BrutalistBlack
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("extra_task_title_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Details (Optional)", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text("Custom Label / Tag", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "IMPORTANCE LEVEL",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImportanceLevel.values().forEach { level ->
                        val isSelected = importance == level.name
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSelected) BrutalistBlack else MaterialTheme.colorScheme.surface)
                                .border(2.dp, MaterialTheme.colorScheme.outline)
                                .clickable { importance = level.name }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = level.name.replace("_", " "),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = if (isSelected) BrutalistWhite else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(title, desc, importance, customLabel)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_extra_task_button"),
                        backgroundColor = BrutalistAmber,
                        contentColor = BrutalistBlack
                    ) {
                        Text("CONFIRM", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

/**
 * Dialog for adding Future Task with Date/Time picker and Chronometer.
 */
@Composable
fun AddFutureTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, epochMillis: Long, importance: String, customLabel: String, reminder: Boolean) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var importance by remember { mutableStateOf(ImportanceLevel.VERY_IMPORTANT.name) }
    var customLabel by remember { mutableStateOf("DEADLINE") }
    var enableReminder by remember { mutableStateOf(true) }

    val calendar = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
        }
    }

    var selectedEpochMillis by remember { mutableStateOf(calendar.timeInMillis) }
    val displayFormat = SimpleDateFormat("MMM d, yyyy 'at' hh:mm a", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        BrutalistCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surface,
            borderColor = MaterialTheme.colorScheme.outline
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PLAN FUTURE TASK",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    BrutalistBadge(
                        text = "CHRONOMETER",
                        backgroundColor = BrutalistCyan,
                        textColor = BrutalistBlack
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Future Task Title *", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("future_task_title_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text("Custom Tag / Label", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Date & Time Picker Selector Box
                Text(
                    text = "TARGET DATE & TIME",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, MaterialTheme.colorScheme.outline)
                        .clickable {
                            val now = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    calendar.set(Calendar.YEAR, year)
                                    calendar.set(Calendar.MONTH, month)
                                    calendar.set(Calendar.DAY_OF_MONTH, day)

                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                                            calendar.set(Calendar.MINUTE, minute)
                                            calendar.set(Calendar.SECOND, 0)
                                            selectedEpochMillis = calendar.timeInMillis
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = displayFormat.format(Date(selectedEpochMillis)),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        BrutalistBadge(
                            text = "CHANGE",
                            backgroundColor = BrutalistBlack,
                            textColor = BrutalistWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Importance selection
                Text(
                    text = "IMPORTANCE (COLOR TAG)",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImportanceLevel.values().forEach { level ->
                        val isSelected = importance == level.name
                        val highlightColor = when (level) {
                            ImportanceLevel.VERY_IMPORTANT -> BrutalistRed
                            ImportanceLevel.MEDIUM -> BrutalistAmber
                            ImportanceLevel.LOW -> BrutalistCyan
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSelected) highlightColor else MaterialTheme.colorScheme.surface)
                                .border(2.dp, MaterialTheme.colorScheme.outline)
                                .clickable { importance = level.name }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = level.name.replace("_", " "),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = if (isSelected && level == ImportanceLevel.VERY_IMPORTANT) BrutalistWhite else BrutalistBlack
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reminder toggle as requested: "Reminder will only be for those tasks that needs constant consistency and future upcoming planned tasks."
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NOTIFY WHEN DUE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Utilitarian alarm reminder",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = enableReminder,
                        onCheckedChange = { enableReminder = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BrutalistBlack,
                            checkedTrackColor = BrutalistYellow
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(title, desc, selectedEpochMillis, importance, customLabel, enableReminder)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_future_task_button"),
                        backgroundColor = BrutalistCyan,
                        contentColor = BrutalistBlack
                    ) {
                        Text("SCHEDULE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

/**
 * Dialog for adding Standard Daily Objective.
 */
@Composable
fun AddDailyTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, importance: String, customLabel: String, consistency: Boolean, isExtra: Boolean) -> Unit
) {
    var taskType by remember { mutableStateOf("DAILY") } // "DAILY" or "EXTRA"
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var importance by remember { mutableStateOf(ImportanceLevel.MEDIUM.name) }
    var customLabel by remember { mutableStateOf("WORK") }
    var needsConsistency by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        BrutalistCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surface,
            borderColor = MaterialTheme.colorScheme.outline
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (taskType == "EXTRA") "ADD EXTRA TASK" else "NEW DAILY OBJECTIVE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    BrutalistBadge(
                        text = if (taskType == "EXTRA") "DAY BONUS" else "TODAY",
                        backgroundColor = if (taskType == "EXTRA") BrutalistAmber else BrutalistYellow,
                        textColor = BrutalistBlack
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Unified selector: Daily Objective vs Extra Task (Today Only)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isExtra = taskType == "EXTRA"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (!isExtra) BrutalistYellow else MaterialTheme.colorScheme.surface)
                            .border(2.dp, MaterialTheme.colorScheme.outline)
                            .clickable {
                                taskType = "DAILY"
                                if (customLabel == "BONUS") customLabel = "WORK"
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DAILY OBJECTIVE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = BrutalistBlack
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isExtra) BrutalistAmber else MaterialTheme.colorScheme.surface)
                            .border(2.dp, MaterialTheme.colorScheme.outline)
                            .clickable {
                                taskType = "EXTRA"
                                customLabel = "BONUS"
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "EXTRA TASK (TODAY)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = BrutalistBlack
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Description *", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_task_title_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text("Custom Tag / Label (e.g. WORK, GYM)", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "IMPORTANCE LEVEL",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImportanceLevel.values().forEach { level ->
                        val isSelected = importance == level.name
                        val highlightColor = when (level) {
                            ImportanceLevel.VERY_IMPORTANT -> BrutalistRed
                            ImportanceLevel.MEDIUM -> BrutalistAmber
                            ImportanceLevel.LOW -> BrutalistCyan
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSelected) highlightColor else MaterialTheme.colorScheme.surface)
                                .border(2.dp, MaterialTheme.colorScheme.outline)
                                .clickable { importance = level.name }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = level.name.replace("_", " "),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = if (isSelected && level == ImportanceLevel.VERY_IMPORTANT) BrutalistWhite else BrutalistBlack
                            )
                        }
                    }
                }

                if (taskType == "DAILY") {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Consistency toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CONSTANT CONSISTENCY HABIT",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Tracks habit streak & enables daily reminder",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = needsConsistency,
                            onCheckedChange = { needsConsistency = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BrutalistBlack,
                                checkedTrackColor = BrutalistLime
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(
                                    title,
                                    desc,
                                    importance,
                                    customLabel,
                                    if (taskType == "EXTRA") false else needsConsistency,
                                    taskType == "EXTRA"
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_daily_task_button"),
                        backgroundColor = if (taskType == "EXTRA") BrutalistAmber else BrutalistYellow,
                        contentColor = BrutalistBlack
                    ) {
                        Text(if (taskType == "EXTRA") "ADD EXTRA" else "ADD OBJECTIVE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
