package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.TaskViewModel
import com.example.ui.navigation.AppDestination
import com.example.ui.navigation.BrutalistBottomBar
import com.example.ui.screens.FutureTasksScreen
import com.example.ui.screens.HeatMapScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TimetableScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openTimetable = intent?.getBooleanExtra("action_open_timetable", false) ?: false
        val initialDest = if (openTimetable) AppDestination.TIMETABLE else AppDestination.TODAY

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

            // Observe Midnight Rollover / Date change broadcast
            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        viewModel.refreshDateRollover()
                    }
                }
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_DATE_CHANGED)
                    addAction(Intent.ACTION_TIME_CHANGED)
                    addAction(Intent.ACTION_TIMEZONE_CHANGED)
                }
                registerReceiver(receiver, filter)
                onDispose {
                    unregisterReceiver(receiver)
                }
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainAppContainer(
                    viewModel = viewModel,
                    initialDestination = initialDest,
                    openAddDialog = intent?.getBooleanExtra("action_open_add_dialog", false) ?: false
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh date rollover state on app resume
        viewModel.refreshDateRollover()
    }
}

@Composable
fun MainAppContainer(
    viewModel: TaskViewModel,
    initialDestination: AppDestination = AppDestination.TODAY,
    openAddDialog: Boolean = false
) {
    var currentDestination by remember { mutableStateOf(initialDestination) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BrutalistBottomBar(
                currentDestination = currentDestination,
                onNavigate = { currentDestination = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(bottom = innerPadding.calculateBottomPadding())
                .statusBarsPadding()
        ) {
            when (currentDestination) {
                AppDestination.TODAY -> HomeScreen(viewModel = viewModel, autoOpenAddDialog = openAddDialog)
                AppDestination.TIMETABLE -> TimetableScreen(viewModel = viewModel)
                AppDestination.FUTURE -> FutureTasksScreen(viewModel = viewModel)
                AppDestination.HEATMAP -> HeatMapScreen(viewModel = viewModel)
                AppDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
