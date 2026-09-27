package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun FutureTasksScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val futureTasks by viewModel.futureTasks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedSort by remember { mutableStateOf("DEADLINE") }
    var selectedTypeFilter by remember { mutableStateOf("ALL_TYPES") } // ALL_TYPES, ASSIGNMENT, EXAM

    val nowMillis = remember { System.currentTimeMillis() }

    val overdueTasks = remember(futureTasks, nowMillis) {
        futureTasks.filter { !it.isCompleted && (it.targetDateTime ?: Long.MAX_VALUE) < nowMillis }
    }

    val filteredFutureTasks = remember(futureTasks, selectedFilter, selectedSort, selectedTypeFilter) {
        val typeFiltered = when (selectedTypeFilter) {
            "ASSIGNMENT" -> futureTasks.filter { it.customLabel.contains("ASSIGNMENT", ignoreCase = true) || it.title.contains("Assignment", ignoreCase = true) }
            "EXAM" -> futureTasks.filter { it.customLabel.contains("EXAM", ignoreCase = true) || it.title.contains("Exam", ignoreCase = true) || it.title.contains("Test", ignoreCase = true) }
            else -> futureTasks
        }

        val filtered = when (selectedFilter) {
            "ACTIVE" -> typeFiltered.filter { !it.isCompleted }
            "VERY_IMPORTANT" -> typeFiltered.filter { it.importance == ImportanceLevel.VERY_IMPORTANT.name }
            "REMINDERS" -> typeFiltered.filter { it.hasReminder }
            "COMPLETED" -> typeFiltered.filter { it.isCompleted }
            else -> typeFiltered
        }

        when (selectedSort) {
            "PRIORITY" -> filtered.sortedWith(
                compareBy<TaskEntity> { it.isCompleted }
                    .thenBy {
                        when (it.importance) {
                            ImportanceLevel.VERY_IMPORTANT.name -> 0
                            ImportanceLevel.MEDIUM.name -> 1
                            else -> 2
                        }
                    }
                    .thenBy { it.targetDateTime ?: Long.MAX_VALUE }
            )
            "ALPHABETICAL" -> filtered.sortedBy { it.title.lowercase(Locale.getDefault()) }
            else -> filtered.sortedWith(
                compareBy<TaskEntity> { it.isCompleted }
                    .thenBy { it.targetDateTime ?: Long.MAX_VALUE }
            ) // DEADLINE first
        }
    }

    val pendingFutureTasks = remember(futureTasks) {
        futureTasks.filter { !it.isCompleted }
    }
    val nearestTask = pendingFutureTasks.minByOrNull { it.targetDateTime ?: Long.MAX_VALUE }

    val groupedHorizons = remember(filteredFutureTasks, nowMillis) {
        val overdue = mutableListOf<TaskEntity>()
        val thisWeek = mutableListOf<TaskEntity>()
        val nextWeek = mutableListOf<TaskEntity>()
        val later = mutableListOf<TaskEntity>()
        val completed = mutableListOf<TaskEntity>()

        val oneWeekMillis = 7 * 86400000L
        val twoWeeksMillis = 14 * 86400000L

        for (t in filteredFutureTasks) {
            val dt = t.targetDateTime ?: Long.MAX_VALUE
            when {
                t.isCompleted -> completed.add(t)
                dt < nowMillis -> overdue.add(t)
                dt <= nowMillis + oneWeekMillis -> thisWeek.add(t)
                dt <= nowMillis + twoWeeksMillis -> nextWeek.add(t)
                else -> later.add(t)
            }
        }

        listOf(
            "OVERDUE DEADLINES" to overdue,
            "THIS WEEK (NEXT 7 DAYS)" to thisWeek,
            "NEXT WEEK (7-14 DAYS)" to nextWeek,
            "LATER / NEXT MONTH" to later,
            "COMPLETED ARCHIVE" to completed
        ).filter { it.second.isNotEmpty() }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Header card
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
                            text = "UPCOMING DEADLINES",
                            backgroundColor = BrutalistBlack,
                            textColor = BrutalistCyan
                        )
                        BrutalistBadge(
                            text = "${pendingFutureTasks.size} SCHEDULED",
                            backgroundColor = BrutalistBlack,
                            textColor = BrutalistWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "FUTURE TARGETS & EXAMS",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        color = BrutalistBlack
                    )

                    // Nearest target banner if available
                    if (nearestTask != null && nearestTask.targetDateTime != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BrutalistBlack)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "NEAREST: ${nearestTask.title.uppercase(Locale.getDefault())}",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = BrutalistYellow,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ChronometerDisplay(
                                    targetEpochMillis = nearestTask.targetDateTime,
                                    isCompleted = nearestTask.isCompleted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Overdue Banner Prompt if any task has passed deadline
        if (overdueTasks.isNotEmpty()) {
            item {
                BrutalistCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = BrutalistRed,
                    borderColor = MaterialTheme.colorScheme.outline
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = BrutalistWhite,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "OVERDUE DEADLINES DETECTED (${overdueTasks.size})",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = BrutalistWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Target deadlines passed without completion. Move to Today or mark finished:",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = BrutalistWhite.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(BrutalistWhite)
                                    .border(1.5.dp, BrutalistBlack)
                                    .clickable {
                                        overdueTasks.forEach { viewModel.moveFutureTaskToToday(it) }
                                        Toast.makeText(context, "Moved ${overdueTasks.size} tasks to Today's Agenda", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text("MOVE ALL TO TODAY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 10.sp, color = BrutalistBlack)
                            }
                            Box(
                                modifier = Modifier
                                    .background(BrutalistBlack)
                                    .border(1.5.dp, BrutalistWhite)
                                    .clickable {
                                        overdueTasks.forEach { viewModel.toggleTask(it) }
                                        Toast.makeText(context, "Marked complete", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text("MARK COMPLETED", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 10.sp, color = BrutalistWhite)
                            }
                        }
                    }
                }
            }
        }

        // Action Button: Add New Future Task
        item {
            BrutalistButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_future_task_page_button"),
                backgroundColor = BrutalistYellow,
                contentColor = BrutalistBlack
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = BrutalistBlack,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ SCHEDULE NEW FUTURE TASK",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = BrutalistBlack
                    )
                }
            }
        }

        // Assignment / Exam Category Toggle & Time Horizon Selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "ALL_TYPES" to "ALL TYPES",
                    "ASSIGNMENT" to "📝 ASSIGNMENTS",
                    "EXAM" to "🎓 EXAMS & TESTS"
                ).forEach { (key, label) ->
                    val isSelected = selectedTypeFilter == key
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) BrutalistBlack else MaterialTheme.colorScheme.surface)
                            .border(1.5.dp, MaterialTheme.colorScheme.outline)
                            .clickable { selectedTypeFilter = key }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isSelected) BrutalistYellow else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Filter & Sort row for Future tasks
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "ALL (${futureTasks.size})",
                        "ACTIVE" to "ACTIVE (${futureTasks.count { !it.isCompleted }})",
                        "VERY_IMPORTANT" to "CRITICAL",
                        "REMINDERS" to "ALERTS",
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
                        "DEADLINE" to "DEADLINE",
                        "PRIORITY" to "PRIORITY",
                        "ALPHABETICAL" to "A-Z"
                    )

                    sortOptions.forEach { (key, label) ->
                        val isCurrentSort = selectedSort == key
                        Box(
                            modifier = Modifier
                                .background(if (isCurrentSort) BrutalistCyan else MaterialTheme.colorScheme.surface)
                                .border(1.5.dp, MaterialTheme.colorScheme.outline)
                                .clickable {
                                    com.example.util.FeedbackManager.performClickFeedback(context)
                                    selectedSort = key
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
        if (filteredFutureTasks.isEmpty()) {
            item {
                BrutalistCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "[ NO FUTURE TASKS FOUND ]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Schedule a task with target date, time, and chronometer timer.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Grouped horizons
        groupedHorizons.forEach { (horizonTitle, taskList) ->
            item(key = "header_$horizonTitle") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val badgeBg = when {
                        horizonTitle.startsWith("OVERDUE") -> BrutalistRed
                        horizonTitle.startsWith("THIS WEEK") -> BrutalistYellow
                        horizonTitle.startsWith("NEXT WEEK") -> BrutalistCyan
                        else -> BrutalistBlack
                    }
                    val badgeFg = when {
                        horizonTitle.startsWith("THIS WEEK") -> BrutalistBlack
                        else -> BrutalistWhite
                    }
                    BrutalistBadge(
                        text = horizonTitle,
                        backgroundColor = badgeBg,
                        textColor = badgeFg
                    )
                    Text(
                        text = "${taskList.size} TASKS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            items(taskList, key = { it.id }) { task ->
                FutureTaskItemCard(
                    task = task,
                    onToggle = { viewModel.toggleTask(task) },
                    onDelete = { viewModel.deleteTask(task) },
                    onMoveToToday = {
                        viewModel.moveFutureTaskToToday(task)
                        Toast.makeText(context, "Moved \"${task.title}\" to Today", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showAddDialog) {
        AddFutureTaskDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, epochMillis, importance, label, reminder ->
                viewModel.addFutureTask(
                    title = title,
                    description = desc,
                    targetEpochMillis = epochMillis,
                    importance = importance,
                    customLabel = label,
                    hasReminder = reminder
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun FutureTaskItemCard(
    task: TaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onMoveToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("EEE, MMM d, yyyy 'at' hh:mm a", Locale.getDefault()) }
    val formattedTarget = remember(task.targetDateTime) {
        if (task.targetDateTime != null) sdf.format(Date(task.targetDateTime)) else "No Date Set"
    }

    val nowMillis = System.currentTimeMillis()
    val isOverdue = !task.isCompleted && task.targetDateTime != null && task.targetDateTime < nowMillis
    val hoursRemaining = if (task.targetDateTime != null) (task.targetDateTime - nowMillis) / 3600000 else 999

    // Dynamic urgency badge calculation
    val (urgencyText, urgencyBg, urgencyFg) = when {
        task.isCompleted -> Triple("DONE", BrutalistLime, BrutalistBlack)
        isOverdue -> Triple("OVERDUE", BrutalistRed, BrutalistWhite)
        hoursRemaining < 24 -> Triple("DUE TODAY", BrutalistYellow, BrutalistBlack)
        hoursRemaining < 72 -> Triple("THIS WEEK", BrutalistCyan, BrutalistBlack)
        else -> Triple("UPCOMING", BrutalistBlack, BrutalistWhite)
    }

    val cardBg = if (task.isCompleted) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    val infiniteTransition = rememberInfiniteTransition(label = "overdue_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val cardBorder = if (isOverdue) BrutalistRed.copy(alpha = pulseAlpha) else MaterialTheme.colorScheme.outline

    BrutalistCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("future_task_item_${task.id}"),
        backgroundColor = cardBg,
        borderColor = cardBorder,
        borderWidth = 2.5.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrutalistBadge(
                        text = urgencyText,
                        backgroundColor = urgencyBg,
                        textColor = urgencyFg
                    )

                    ImportanceBadge(importance = task.importance)

                    if (task.customLabel.isNotBlank()) {
                        BrutalistBadge(
                            text = task.customLabel,
                            backgroundColor = BrutalistBlack,
                            textColor = BrutalistWhite
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Move to Today button
                    if (!task.isCompleted) {
                        Box(
                            modifier = Modifier
                                .background(BrutalistYellow)
                                .border(1.dp, BrutalistBlack)
                                .clickable { onMoveToToday() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("MOVE TO TODAY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 9.sp, color = BrutalistBlack)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Future Task",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Checkbox and Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrutalistCheckbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle() },
                    checkedColor = BrutalistCyan
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.isCompleted) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = if (task.isCompleted) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            },
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chronometer & Target Time Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline)
                    .padding(8.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = formattedTarget,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (task.targetDateTime != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        ChronometerDisplay(
                            targetEpochMillis = task.targetDateTime,
                            isCompleted = task.isCompleted
                        )
                    }
                }
            }

            val isExam = task.customLabel.contains("EXAM", ignoreCase = true) ||
                    task.title.contains("Exam", ignoreCase = true) ||
                    task.title.contains("Test", ignoreCase = true)

            if (isExam) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📖 EXAM SYLLABUS PREPARATION",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = BrutalistCyan
                        )
                        Text(
                            text = if (task.isCompleted) "100% READY" else "IN PROGRESS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            color = if (task.isCompleted) BrutalistLime else BrutalistAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, BrutalistBlack)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(if (task.isCompleted) 1.0f else 0.70f)
                                .background(if (task.isCompleted) BrutalistLime else BrutalistYellow)
                        )
                    }
                }
            }
        }
    }
}
