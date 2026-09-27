package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrutalistBlack
import com.example.ui.theme.BrutalistWhite
import com.example.ui.theme.BrutalistYellow

enum class AppDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val tag: String
) {
    TODAY("today", "TODAY", Icons.Default.Today, "nav_today"),
    TIMETABLE("timetable", "CLASSES", Icons.Default.CalendarMonth, "nav_timetable"),
    FUTURE("future", "FUTURE", Icons.Default.Schedule, "nav_future"),
    HEATMAP("heatmap", "HEAT MAP", Icons.Default.GridOn, "nav_heatmap"),
    SETTINGS("settings", "SETTINGS", Icons.Default.Settings, "nav_settings")
}

@Composable
fun BrutalistBottomBar(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 2.5.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(0.dp)
            )
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AppDestination.values().forEach { destination ->
                val isSelected = destination == currentDestination

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(destination.tag)
                ) {
                    if (isSelected) {
                        // Hard shadow for active tab
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(2.dp, 2.dp)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) BrutalistYellow else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                            .clickable { onNavigate(destination) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title,
                                tint = if (isSelected) BrutalistBlack else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = destination.title,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                fontSize = 9.sp,
                                maxLines = 1,
                                color = if (isSelected) BrutalistBlack else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}
