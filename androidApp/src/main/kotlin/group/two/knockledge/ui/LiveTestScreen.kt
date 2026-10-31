package group.two.knockledge.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import group.two.knockledge.R
import group.two.knockledge.data.remote.GoogleIdTokenClient
import group.two.knockledge.di.KnockledgeComponent
import group.two.knockledge.ui.auth.AuthScreen
import group.two.knockledge.ui.auth.AuthViewModel
import group.two.knockledge.ui.playback.NotePlaybackViewModel
import group.two.knockledge.ui.processing.NoteProcessingViewModel
import group.two.knockledge.ui.settings.SettingsViewModel
import group.two.knockledge.ui.tabs.NotesTab
import group.two.knockledge.ui.tabs.RecordTab
import group.two.knockledge.ui.tabs.SettingsTab
import kotlinx.coroutines.launch

/**
 * Utilitarian top-level Material 3 live test UI shell with tab navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTestScreen(
    uiState: LiveTestUiState,
    onTabSelected: (LiveTestTab) -> Unit,
    recordTabContent: @Composable () -> Unit,
    notesTabContent: @Composable () -> Unit,
    settingsTabContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name) + " - Live Test Shell",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                PrimaryTabRow(
                    selectedTabIndex = uiState.activeTab.ordinal,
                ) {
                    LiveTestTab.entries.forEach { tab ->
                        val labelRes = when (tab) {
                            LiveTestTab.RECORD -> R.string.tab_record
                            LiveTestTab.NOTES -> R.string.tab_notes
                            LiveTestTab.SETTINGS -> R.string.tab_settings
                        }
                        Tab(
                            selected = uiState.activeTab == tab,
                            onClick = { onTabSelected(tab) },
                            text = { Text(text = stringResource(labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (uiState.activeTab) {
                LiveTestTab.RECORD -> recordTabContent()
                LiveTestTab.NOTES -> notesTabContent()
                LiveTestTab.SETTINGS -> settingsTabContent()
            }
        }
    }
}

/**
 * Convenience stateful overload connecting a [LiveTestController].
 */
@Composable
fun LiveTestScreen(
    controller: LiveTestController,
    recordTabContent: @Composable () -> Unit,
    notesTabContent: @Composable () -> Unit,
    settingsTabContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by controller.uiState.collectAsState()
    LiveTestScreen(
        uiState = uiState,
        onTabSelected = controller::selectTab,
        recordTabContent = recordTabContent,
        notesTabContent = notesTabContent,
        settingsTabContent = settingsTabContent,
        modifier = modifier,
    )
}

/**
 * Full application shell host composable that binds all subsystem ViewModels,
 * observes [group.two.knockledge.data.NoteRepository.observeNotes] across user switching,
 * and passes active state into [LiveTestScreen].
 */
@Composable
fun LiveTestAppShell(
    component: KnockledgeComponent,
    hasRecordAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    // 1. Core Graph & Data References
    val graph = remember(component) { component.graph() }
    val dataGraph = remember(component) { component.data() }
    val appSettings = remember(component) { component.appSettings() }
    val audioPlayer = remember(component) { component.audioPlayer() }
    val pipeline = remember(component) { component.pipeline() }
    val aiNotifier = remember(component) { component.aiNotificationNotifier() }

    // 2. Subsystem ViewModels & Coordinators
    val liveTestController = remember(component) {
        LiveTestCoordinator(initialPermission = hasRecordAudioPermission)
    }
    val recordingViewModel = remember(component, graph, appSettings) {
        KnockRecordingViewModel(graph.controller, appSettings)
    }
    val playbackViewModel = remember(component, audioPlayer) {
        NotePlaybackViewModel(audioPlayer)
    }
    val processingViewModel = remember(component, dataGraph, pipeline, aiNotifier) {
        NoteProcessingViewModel(dataGraph.notes, dataGraph.auth, pipeline, aiNotifier)
    }
    val settingsViewModel = remember(component, appSettings) {
        SettingsViewModel(appSettings)
    }
    val context = LocalContext.current
    val authViewModel = remember(component, dataGraph, context) {
        AuthViewModel(dataGraph.auth, googleToken = {
            val appContext = context.applicationContext
            val webClientIdRes = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
            val webClientId = if (webClientIdRes != 0) appContext.getString(webClientIdRes) else ""
            if (webClientId.isBlank()) {
                throw IllegalStateException("Google Sign-In is not configured: default_web_client_id missing in google-services.json")
            }
            GoogleIdTokenClient(webClientId).fetch(context)
        })
    }

    // Update permission in coordinator
    LaunchedEffect(hasRecordAudioPermission) {
        liveTestController.onPermissionResult(hasRecordAudioPermission)
    }

    // 3. Reactive State Observations & Authentication Gate
    val authState by authViewModel.state.collectAsState()
    val currentUser = authState.user

    if (currentUser == null) {
        Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
            AuthScreen(
                state = authState,
                onGoogle = authViewModel::signInWithGoogle,
                onSignIn = { email, pass -> authViewModel.signInWithEmail(email, pass) },
                onRegister = { name, email, pass -> authViewModel.register(name, email, pass) },
                modifier = Modifier.padding(innerPadding),
            )
        }
    } else {
        val activeUserId = currentUser.uid
        val notesFlow = remember(dataGraph, activeUserId) { dataGraph.notes.observeNotes(activeUserId) }
        val notes by notesFlow.collectAsState(initial = emptyList())

        val liveTestUiState by liveTestController.uiState.collectAsState()
        val playbackState by playbackViewModel.playbackState.collectAsState()
        val processingUiState by processingViewModel.processingState.collectAsState()
        val settingsUiState by settingsViewModel.uiState.collectAsState()
        val autoAiEnabled by appSettings.autoAiProcessingEnabled.collectAsState()

        // 4. Integrated Shell Rendering
        LiveTestScreen(
            uiState = liveTestUiState,
            onTabSelected = liveTestController::selectTab,
            recordTabContent = {
                RecordTab(
                    recordingViewModel = recordingViewModel,
                    liveTestController = liveTestController,
                    onRequestPermission = onRequestPermission,
                )
            },
            notesTabContent = {
                NotesTab(
                    notes = notes,
                    selectedNote = liveTestUiState.selectedNote,
                    playbackState = playbackState,
                    processingUiState = processingUiState,
                    onSelectNote = { note ->
                        liveTestController.selectNote(note)
                        playbackViewModel.loadNote(note)
                    },
                    onDeleteNote = { noteId ->
                        scope.launch {
                            dataGraph.notes.deleteNote(noteId, activeUserId)
                            if (liveTestUiState.selectedNote?.id == noteId) {
                                liveTestController.selectNote(null)
                            }
                        }
                    },
                    onTogglePlayPause = playbackViewModel::togglePlayPause,
                    onSeekTo = playbackViewModel::seekTo,
                    onSelectChapter = playbackViewModel::selectChapter,
                    onProcessNote = processingViewModel::processNote,
                )
            },
            settingsTabContent = {
                SettingsTab(
                    settingsUiState = settingsUiState,
                    authState = authState,
                    autoAiEnabled = autoAiEnabled,
                    onAutoAiChanged = appSettings::setAutoAiProcessingEnabled,
                    onBufferDurationChanged = settingsViewModel::setBufferDurationSeconds,
                    onKnockThresholdChanged = settingsViewModel::setKnockSensorThreshold,
                    onSignOut = authViewModel::signOut,
                )
            },
            modifier = modifier,
        )
    }
}
