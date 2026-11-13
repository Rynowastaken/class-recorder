package com.ryno.classrecorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ryno.classrecorder.ui.MainViewModel
import com.ryno.classrecorder.ui.screens.LibraryScreen
import com.ryno.classrecorder.ui.screens.RecordScreen
import com.ryno.classrecorder.ui.screens.ScheduleScreen
import com.ryno.classrecorder.ui.theme.ClassRecorderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClassRecorderTheme {
                ClassRecorderApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassRecorderApp(vm: MainViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()
    val recorder by vm.recorderState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var openMcpSheet by remember { mutableStateOf(false) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasMicPermission = grants[Manifest.permission.RECORD_AUDIO] == true
    }

    fun requestPermissions() {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permLauncher.launch(perms.toTypedArray())
    }

    LaunchedEffect(ui.statusMessage) {
        ui.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (selectedTab) {
                            0 -> "Class Recorder"
                            1 -> "Class Schedule"
                            else -> "Library & Agents"
                        }
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Mic, contentDescription = "Record") },
                    label = { Text("Record") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Schedule") },
                    label = { Text("Schedule") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Library") },
                    label = { Text("Library") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> RecordScreen(
                    ui = ui,
                    recorder = recorder,
                    hasMicPermission = hasMicPermission,
                    onRequestMicPermission = ::requestPermissions,
                    onStartRecording = { vm.startRecording() },
                    onStopRecording = { vm.stopRecording() },
                    onTogglePause = { vm.togglePause() },
                    onSelectCourseOverride = { vm.selectCourseOverride(it) },
                    onOpenMcpSheet = {
                        openMcpSheet = true
                        selectedTab = 2
                    }
                )
                1 -> ScheduleScreen(
                    ui = ui,
                    onSyncSchedule = { vm.syncSchedule(it) },
                    onRecordCourse = { course, slot ->
                        if (hasMicPermission) {
                            vm.startRecordingForCourse(course, slot)
                            selectedTab = 0
                        } else {
                            requestPermissions()
                        }
                    }
                )
                else -> LibraryScreen(
                    ui = ui,
                    showMcpSheetInitial = openMcpSheet,
                    onDismissMcpSheet = { openMcpSheet = false },
                    onToggleMcp = { vm.toggleMcpServer() },
                    onUpdateNotes = { id, notes, tags -> vm.updateNotes(id, notes, tags) },
                    onDeleteRecording = { vm.deleteRecording(it) }
                )
            }
        }
    }
}
