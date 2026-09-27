package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ImportanceLevel
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class HeatMapDay(
    val dateString: String,
    val date: Date,
    val dayOfWeek: Int,
    val count: Int,
    val isFreeze: Boolean = false
)

data class PeriodSummaryReport(
    val id: String,
    val title: String,
    val subtitle: String,
    val completedCount: Int,
    val totalScheduled: Int,
    val consistencyRatePercent: Int,
    val dailyBreakdown: Map<String, Int> = emptyMap(),
    val isCurrent: Boolean = false
)

@Composable
fun HeatMapScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val metrics by viewModel.productivityMetrics.collectAsStateWithLifecycle()
    val allCompletedTasks by viewModel.allCompletedTasks.collectAsStateWithLifecycle()

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateSdf = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }

    var selectedDayDateStr by remember { mutableStateOf<String?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var showFreezeDialog by remember { mutableStateOf(false) }

    // Summary reports view & comparison
    var selectedReportMode by remember { mutableStateOf<String>("WEEKLY") }
    var selectedWeekIndex by remember { mutableStateOf(0) }
    var selectedMonthIndex by remember { mutableStateOf(0) }

    var isComparing by remember { mutableStateOf(false) }
    var comparePeriodAIndex by remember { mutableStateOf(0) }
    var comparePeriodBIndex by remember { mutableStateOf(1) }

    // Filter tasks by category/subject
    val categoryFilteredTasks = remember(allCompletedTasks, selectedCategoryFilter) {
        if (selectedCategoryFilter == "ALL") allCompletedTasks
        else allCompletedTasks.filter {
            it.customLabel.contains(selectedCategoryFilter, ignoreCase = true) ||
            it.importance.equals(selectedCategoryFilter, ignoreCase = true)
        }
    }

    // Weekly summary reports
    val weeklyReports = remember(categoryFilteredTasks, metrics.dailyCounts) {
        val reports = mutableListOf<PeriodSummaryReport>()
        val weekFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        for (w in 0..5) {
            val calStart = Calendar.getInstance()
            calStart.firstDayOfWeek = Calendar.MONDAY
            calStart.set(Calendar.HOUR_OF_DAY, 0)
            calStart.set(Calendar.MINUTE, 0)
            calStart.set(Calendar.SECOND, 0)
            calStart.set(Calendar.MILLISECOND, 0)
            calStart.add(Calendar.WEEK_OF_YEAR, -w)
            calStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

            val calEnd = Calendar.getInstance()
            calEnd.timeInMillis = calStart.timeInMillis
            calEnd.add(Calendar.DAY_OF_MONTH, 6)
            calEnd.set(Calendar.HOUR_OF_DAY, 23)
            calEnd.set(Calendar.MINUTE, 59)
            calEnd.set(Calendar.SECOND, 59)

            val label = when (w) {
                0 -> "THIS WEEK (CURRENT)"
                1 -> "LAST WEEK (-1 WK)"
                else -> "-$w WEEKS AGO"
            }
            val rangeStr = "${weekFormat.format(calStart.time)} - ${weekFormat.format(calEnd.time)}"

            val weekTasks = categoryFilteredTasks.filter {
                val ts = it.completedAt ?: it.createdAt
                ts in calStart.timeInMillis..calEnd.timeInMillis
            }
            val completed = weekTasks.size
            val totalEstimate = maxOf(completed, 7)
            val rate = if (totalEstimate > 0) ((completed.toFloat() / totalEstimate.toFloat()) * 100).toInt().coerceIn(0, 100) else 100

            reports.add(
                PeriodSummaryReport(
                    id = "week_$w",
                    title = label,
                    subtitle = rangeStr,
                    completedCount = completed,
                    totalScheduled = totalEstimate,
                    consistencyRatePercent = rate,
                    isCurrent = (w == 0)
                )
            )
        }
        reports
    }

    // Generate past 16 weeks (112 days) of grid data organized by columns (weeks) and rows (7 days: Mon-Sun)
    val weeksData = remember(metrics.dailyCounts, metrics.streakFreezeDates, categoryFilteredTasks) {
        val weeks = mutableListOf<List<HeatMapDay>>()
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY

        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysToSunday = if (dayOfWeek == Calendar.SUNDAY) 0 else 8 - dayOfWeek
        cal.add(Calendar.DAY_OF_MONTH, daysToSunday)

        val totalWeeks = 16
        cal.add(Calendar.WEEK_OF_YEAR, -(totalWeeks - 1))
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        for (w in 0 until totalWeeks) {
            val weekDays = mutableListOf<HeatMapDay>()
            for (d in 0..6) {
                val dateStr = sdf.format(cal.time)
                val count = categoryFilteredTasks.count { it.date == dateStr }
                val isFreeze = metrics.streakFreezeDates.contains(dateStr)
                weekDays.add(
                    HeatMapDay(
                        dateString = dateStr,
                        date = cal.time,
                        dayOfWeek = cal.get(Calendar.DAY_OF_WEEK),
                        count = count,
                        isFreeze = isFreeze
                    )
                )
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
            weeks.add(weekDays)
        }
        weeks
    }

    val selectedDayTasks = remember(selectedDayDateStr, allCompletedTasks) {
        if (selectedDayDateStr == null) emptyList()
        else allCompletedTasks.filter { it.date == selectedDayDateStr }
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
                backgroundColor = BrutalistLime,
                borderColor = MaterialTheme.colorScheme.outline
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrutalistBadge(
                            text = "HABIT HEAT MAP",
                            backgroundColor = BrutalistBlack,
                            textColor = BrutalistLime
                        )
                        BrutalistBadge(
                            text = "${metrics.totalCompletedAllTime} OBJECTIVES COMPLETED",
                            backgroundColor = BrutalistBlack,
                            textColor = BrutalistWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "CONSISTENCY MATRIX",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        color = BrutalistBlack
                    )
                }
            }
        }

        // Streak Card + Shareable Productivity Card + Freeze Streak
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Streak Card
                BrutalistCard(
                    modifier = Modifier.weight(1f),
                    backgroundColor = BrutalistYellow,
                    borderColor = MaterialTheme.colorScheme.outline
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = BrutalistBlack,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "STREAK",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = BrutalistBlack
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${metrics.currentStreakDays} DAYS",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = BrutalistBlack
                        )
                        Text(
                            text = "BEST: ${metrics.bestStreakDays} DAYS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = BrutalistBlack.copy(alpha = 0.7f)
                        )
                    }
                }

                // Consistency Rate Card
                BrutalistCard(
                    modifier = Modifier.weight(1f),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outline
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = BrutalistLime,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "RATE",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${metrics.consistencyRatePercent}%",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${metrics.thisWeekCompleted} THIS WEEK",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Action Deck: Share Productivity Card + Streak Freeze (Sick Day Grace)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Shareable Productivity Snapshot
                BrutalistButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "🔥 My College & Daily Task Consistency: ${metrics.currentStreakDays}-day unbroken streak! ${metrics.totalCompletedAllTime} objectives completed with ${metrics.consistencyRatePercent}% consistency on College Planner."
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Productivity Card"))
                    },
                    modifier = Modifier.weight(1f),
                    backgroundColor = BrutalistCyan,
                    contentColor = BrutalistBlack,
                    shadowOffset = 2.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = BrutalistBlack, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SHARE CARD", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp, color = BrutalistBlack)
                    }
                }

                // Streak Freeze / Sick Day Grace Button
                BrutalistButton(
                    onClick = { showFreezeDialog = true },
                    modifier = Modifier.weight(1f),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowOffset = 2.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.AcUnit, contentDescription = null, tint = BrutalistCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("STREAK FREEZE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Category / Subject Filter Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("ALL", "VERY_IMPORTANT", "HABIT", "DEEP WORK", "ACADEMIC", "HEALTH").forEach { cat ->
                    val isSelected = selectedCategoryFilter == cat
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface)
                            .border(1.5.dp, MaterialTheme.colorScheme.outline)
                            .clickable { selectedCategoryFilter = cat }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = cat.replace("_", " "),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // GitHub Profile Page Style Heat Map
        item {
            BrutalistCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("github_heatmap_card"),
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HABIT MATRIX • TAP CELL TO INSPECT",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "PAST 16 WEEKS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal scrolling container for weeks grid
                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .padding(vertical = 4.dp)
                    ) {
                        // Day of week labels on left
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            val labels = listOf("M", "T", "W", "T", "F", "S", "S")
                            labels.forEach { label ->
                                Box(
                                    modifier = Modifier.size(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        // Weeks columns
                        weeksData.forEach { weekDays ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                weekDays.forEach { day ->
                                    val cellColor = if (day.isFreeze) BrutalistCyan else getHeatMapColor(day.count)
                                    val isSelected = selectedDayDateStr == day.dateString

                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(cellColor, RoundedCornerShape(1.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) BrutalistRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(1.dp)
                                            )
                                            .clickable {
                                                com.example.util.FeedbackManager.performClickFeedback(context)
                                                selectedDayDateStr = day.dateString
                                            }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // GitHub Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Freeze",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = BrutalistCyan
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(10.dp)
                                .background(BrutalistCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Less",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        listOf(0, 1, 2, 4, 6).forEach { sampleCount ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(10.dp)
                                    .background(getHeatMapColor(sampleCount))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            )
                        }
                        Text(
                            text = "More",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Clean Inline Day Inspector directly under the matrix
        if (selectedDayDateStr != null) {
            item {
                BrutalistCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = BrutalistYellow,
                    borderColor = MaterialTheme.colorScheme.outline
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DAY INSPECTOR: $selectedDayDateStr",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = BrutalistBlack
                            )
                            BrutalistBadge(
                                text = "${selectedDayTasks.size} COMPLETED",
                                backgroundColor = BrutalistBlack,
                                textColor = BrutalistYellow
                            )
                        }

                        if (selectedDayTasks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            selectedDayTasks.forEach { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• [${task.importance}] ${task.title}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrutalistBlack
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No logged task completions recorded on this calendar day.",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = BrutalistBlack.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Productivity Trends Insights
        item {
            Text(
                text = "WEEKLY TRENDS & SUMMARY",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Weekly Report cards
        items(weeklyReports, key = { it.id }) { report ->
            BrutalistCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = if (report.isCurrent) BrutalistLime.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                borderColor = if (report.isCurrent) BrutalistLime else MaterialTheme.colorScheme.outline
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = report.title,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = report.subtitle,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = "${report.completedCount} DONE (${report.consistencyRatePercent}%)",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = BrutalistLime
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Streak Freeze / Sick Day Grace Dialog with Mandatory Reason
    if (showFreezeDialog) {
        var freezeReason by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showFreezeDialog = false }) {
            BrutalistCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = BrutalistCyan
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "STREAK FREEZE / SICK GRACE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Protect your streak when sick or away. You MUST provide a specific reason or description to apply the freeze.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = freezeReason,
                        onValueChange = {
                            freezeReason = it
                            if (it.isNotBlank()) errorMessage = ""
                        },
                        label = { Text("Reason for Freeze / Grace *", fontFamily = FontFamily.Monospace) },
                        placeholder = { Text("e.g. Flu / Doctor appointment / Family travel", fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (errorMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(errorMessage, color = BrutalistRed, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BrutalistButton(
                            onClick = { showFreezeDialog = false },
                            modifier = Modifier.weight(1f),
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Text("CANCEL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        BrutalistButton(
                            onClick = {
                                if (freezeReason.isBlank()) {
                                    errorMessage = "You must specify a reason for this streak freeze/grace."
                                } else {
                                    viewModel.addStreakFreeze(viewModel.todayDateString, freezeReason.trim())
                                    showFreezeDialog = false
                                    Toast.makeText(context, "Streak freeze applied for today!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            backgroundColor = BrutalistCyan,
                            contentColor = BrutalistBlack
                        ) {
                            Text("APPLY FREEZE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getHeatMapColor(count: Int): Color {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    return when {
        count <= 0 -> if (isDark) Color(0xFF21262D) else Color(0xFFEBEDF0)
        count == 1 -> if (isDark) Color(0xFF0E4429) else Color(0xFF9BE9A8)
        count in 2..3 -> if (isDark) Color(0xFF006D32) else Color(0xFF40C463)
        count in 4..5 -> if (isDark) Color(0xFF26A641) else Color(0xFF30A14E)
        else -> if (isDark) Color(0xFF39D353) else Color(0xFF216E39)
    }
}
