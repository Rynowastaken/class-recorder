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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ryno.classrecorder.core.RecordingRecord
import com.ryno.classrecorder.ui.MainViewModel
import com.ryno.classrecorder.ui.components.BackdropVeil
import com.ryno.classrecorder.ui.components.DshBackdrop
import com.ryno.classrecorder.ui.components.GlassIconButton
import com.ryno.classrecorder.ui.components.GlassSurface
import com.ryno.classrecorder.ui.components.LocalBackdropLayer
import com.ryno.classrecorder.ui.components.PillSegment
import com.ryno.classrecorder.ui.components.SegmentedPill
import com.ryno.classrecorder.ui.components.rememberSafeSideInsets
import com.ryno.classrecorder.ui.screens.LibraryScreen
import com.ryno.classrecorder.ui.screens.PlaybackScreen
import com.ryno.classrecorder.ui.screens.RecordScreen
import com.ryno.classrecorder.ui.screens.ScheduleScreen
import com.ryno.classrecorder.ui.screens.SettingsScreen
import com.ryno.classrecorder.ui.theme.AppShape
import com.ryno.classrecorder.ui.theme.AppSpace
import com.ryno.classrecorder.ui.theme.ClassRecorderTheme
import com.ryno.classrecorder.ui.theme.DshMotion
import com.ryno.classrecorder.ui.theme.GlassTier
import com.ryno.classrecorder.ui.theme.StatusTone
import kotlinx.coroutines.launch

private val ROOT_TABS = listOf(
    PillSegment(0, "Recorder"),
    PillSegment(1, "Schedule"),
    PillSegment(2, "Recordings")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val ui by vm.ui.collectAsState()
            ClassRecorderTheme(
                themeMode = ui.themeMode,
                accentId = ui.accentId
            ) {
                ClassRecorderApp(vm = vm)
            }
        }
    }
}

@Composable
fun ClassRecorderApp(vm: MainViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()
    val recorder by vm.recorderState.collectAsState()
    val context = LocalContext.current
    val reduced = ClassRecorderTheme.reducedMotion
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val rootBackdropLayer = rememberGraphicsLayer()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { ROOT_TABS.size })
    val pageScrollVeils = remember { mutableStateMapOf(0 to 0f, 1 to 0f, 2 to 0f) }

    var showSettings by remember { mutableStateOf(false) }
    var activePlaybackId by remember { mutableStateOf<String?>(null) }
    var lastPlaybackSnapshot by remember { mutableStateOf<RecordingRecord?>(null) }

    val playbackRecording = remember(activePlaybackId, ui.recordings) {
        activePlaybackId?.let { id -> ui.recordings.find { it.id == id } }
    }
    LaunchedEffect(playbackRecording) {
        if (playbackRecording != null) {
            lastPlaybackSnapshot = playbackRecording
        } else if (activePlaybackId != null) {
            // Recording was deleted externally (e.g. via MCP): dismiss overlay cleanly
            activePlaybackId = null
        }
    }

    val overlayOpen = playbackRecording != null || showSettings

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
        ui.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.clearMessage()
        }
    }

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val chromeBarHeight = 56.dp
    val topReserved = statusTop + chromeBarHeight

    val currentVeilAlpha = pageScrollVeils[pagerState.currentPage] ?: 0f
    val fractionalPage = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
        .coerceIn(0f, (ROOT_TABS.size - 1).toFloat())

    DshBackdrop(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val (startInset, endInset) = rememberSafeSideInsets(maxWidth)

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (overlayOpen) {
                            Modifier
                                .semantics { hideFromAccessibility() }
                                .clearAndSetSemantics {}
                        } else Modifier
                    )
            ) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 2,
                    userScrollEnabled = !overlayOpen,
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            rootBackdropLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(rootBackdropLayer)
                        }
                ) { tab ->
                    when (tab) {
                        0 -> RecordScreen(
                            ui = ui,
                            recorder = recorder,
                            hasMicPermission = hasMicPermission,
                            onRequestMicPermission = ::requestPermissions,
                            onStartRecording = { vm.startRecording() },
                            onStopRecording = { vm.stopRecording() },
                            onDiscardRecording = { vm.discardRecording() },
                            onTogglePause = { vm.togglePause() },
                            onSelectCourseOverride = { vm.selectCourseOverride(it) },
                            onToggleMcp = { vm.toggleMcpServer() },
                            onShowMessage = { vm.showMessage(it) },
                            topContentInset = topReserved,
                            onScrolledUnderChange = { pageScrollVeils[0] = it }
                        )
                        1 -> ScheduleScreen(
                            ui = ui,
                            onSyncSchedule = { vm.syncSchedule(it) },
                            onRecordCourse = { course, slot ->
                                if (hasMicPermission) {
                                    vm.startRecordingForCourse(course, slot)
                                    scope.launch {
                                        pagerState.animateScrollToPage(0, animationSpec = DshMotion.standardTween(reduced))
                                    }
                                } else {
                                    requestPermissions()
                                }
                            },
                            topContentInset = topReserved,
                            onScrolledUnderChange = { pageScrollVeils[1] = it }
                        )
                        else -> LibraryScreen(
                            ui = ui,
                            isActiveTab = pagerState.currentPage == 2 && !overlayOpen && !recorder.isRecording,
                            onToggleMcp = { vm.toggleMcpServer() },
                            onDeleteRecording = { vm.deleteRecording(it) },
                            onShowMessage = { vm.showMessage(it) },
                            onOpenPlayback = { rec ->
                                lastPlaybackSnapshot = rec
                                activePlaybackId = rec.id
                            },
                            topContentInset = topReserved,
                            onScrolledUnderChange = { pageScrollVeils[2] = it }
                        )
                    }
                }

                BackdropVeil(
                    fromTop = true,
                    solidHeight = topReserved,
                    feather = 14.dp,
                    alpha = currentVeilAlpha,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                CompositionLocalProvider(LocalBackdropLayer provides rootBackdropLayer) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = statusTop)
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = chromeBarHeight)
                            .padding(
                                start = AppSpace.floatInset + startInset,
                                end = AppSpace.floatInset + endInset,
                                top = 4.dp,
                                bottom = 4.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SegmentedPill(
                            segments = ROOT_TABS,
                            selected = pagerState.currentPage,
                            onSelect = { targetPage ->
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        page = targetPage,
                                        animationSpec = DshMotion.standardTween(reduced)
                                    )
                                }
                            },
                            chromeGlass = true,
                            fractionalIndex = fractionalPage,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(8.dp))

                        GlassIconButton(
                            icon = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            onClick = { showSettings = true },
                            tier = GlassTier.Chrome,
                            badgeTone = if (ui.mcpStatus.running) StatusTone.Ok else null
                        )
                    }
                }
            }

            // Full-screen Playback push overlay
            AnimatedVisibility(
                visible = playbackRecording != null,
                enter = slideInHorizontally(
                    animationSpec = DshMotion.glideTween(reduced),
                    initialOffsetX = { full -> full / 5 }
                ) + fadeIn(DshMotion.quickTween(reduced)),
                exit = slideOutHorizontally(
                    animationSpec = DshMotion.glideTween(reduced),
                    targetOffsetX = { full -> full / 5 }
                ) + fadeOut(DshMotion.quickTween(reduced))
            ) {
                val rec = playbackRecording ?: lastPlaybackSnapshot
                if (rec != null) {
                    PlaybackScreen(
                        recording = rec,
                        onDismiss = { activePlaybackId = null },
                        onUpdateNotes = { id, notes, tags -> vm.updateNotes(id, notes, tags) },
                        onDeleteRecording = { id ->
                            activePlaybackId = null
                            vm.deleteRecording(id)
                        },
                        onShowMessage = { vm.showMessage(it) }
                    )
                }
            }

            // Full-screen Settings push overlay
            AnimatedVisibility(
                visible = showSettings,
                enter = slideInHorizontally(
                    animationSpec = DshMotion.glideTween(reduced),
                    initialOffsetX = { full -> full / 5 }
                ) + fadeIn(DshMotion.quickTween(reduced)),
                exit = slideOutHorizontally(
                    animationSpec = DshMotion.glideTween(reduced),
                    targetOffsetX = { full -> full / 5 }
                ) + fadeOut(DshMotion.quickTween(reduced))
            ) {
                SettingsScreen(
                    ui = ui,
                    onDismiss = { showSettings = false },
                    onSelectThemeMode = { vm.setThemeMode(it) },
                    onSelectAccentId = { vm.setAccentId(it) },
                    onSaveFilenameTemplate = { tmpl, fmt -> vm.saveFilenameTemplate(tmpl, fmt) },
                    onSyncSchedule = { vm.syncSchedule(it) },
                    onToggleMcp = { vm.toggleMcpServer() },
                    onShowMessage = { vm.showMessage(it) }
                )
            }

            // Topmost Chrome-tier Mist Glass snackbar host so status messages surface above Settings & Playback too
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = AppSpace.gutter + startInset,
                        end = AppSpace.gutter + endInset,
                        bottom = navBottom + if (overlayOpen) 24.dp else 88.dp
                    )
            ) { data ->
                GlassSurface(
                    tier = GlassTier.Chrome,
                    shape = AppShape.radiusLg,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = data.visuals.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
