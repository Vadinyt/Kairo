package com.kairo.agent.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.kairo.agent.R
import com.kairo.agent.agent.AgentPersona
import com.kairo.agent.agent.AndroidAgentTools
import com.kairo.agent.agent.ScreenContextHolder
import com.kairo.agent.data.AgentSettings
import com.kairo.agent.data.AgentSettingsStore
import com.kairo.agent.data.AiCoreModelProfile
import com.kairo.agent.data.ChatHistoryStore
import com.kairo.agent.data.ChatRecord
import com.kairo.agent.data.EngineBackendMode
import com.kairo.agent.data.MemoryStore
import com.kairo.agent.engine.CompatibilityChecker
import com.kairo.agent.engine.ModelAvailability
import com.kairo.agent.service.WakeWordService
import com.kairo.agent.ui.components.animatedAiBorder
import com.kairo.agent.ui.components.BlurIntoText
import com.kairo.agent.ui.components.GemmASparkleIcon
import com.kairo.agent.ui.components.ResponseShineSweep
import com.kairo.agent.ui.components.StarDustParticles
import com.kairo.agent.ui.components.ListeningWaveformIndicator
import com.kairo.agent.ui.components.ReactiveAuroraBackground
import com.kairo.agent.ui.components.RichResponseText
import com.kairo.agent.ui.components.ThinkingDotsIndicator
import com.kairo.agent.ui.onboarding.OnboardingScreen
import com.kairo.agent.ui.theme.GemmATheme
import com.kairo.agent.update.UpdateCheckResult
import com.kairo.agent.update.UpdateChecker
import com.kairo.agent.voice.VoiceController
import kotlinx.coroutines.launch

private val GeminiBgTop = Color(0xFF000000)
private val GeminiBgGlow = Color(0xFF0B193C)
private val GeminiDrawerBg = Color(0xFF0E0F11)
private val GeminiPillBg = Color(0xFF1E1F20)
private val GeminiChipBg = Color(0xFF282A2C)
private val GeminiTextPrimary = Color(0xFFE3E3E3)
private val GeminiTextSecondary = Color(0xFF8E918F)
private val GeminiAccentBlue = Color(0xFFA8C7FA)
private val GeminiListeningRed = Color(0xFFF28B82)
private val ActionSuccessGreen = Color(0xFF81C995)
private val ActionErrorRed = Color(0xFFF28B82)

private val AgentQuickActions = listOf(
    "Catch me up" to "Catch me up on my recent notifications",
    "Health today" to "Check my Health Connect steps and heart rate today",
    "Set 7:30 alarm" to "Set an alarm for 7:30 AM",
    "Flashlight on" to "Turn on the flashlight",
    "Battery" to "What is my battery status?",
    "Call..." to "Call ",
    "Text..." to "Text ",
    "Send image" to "Send this image",
    "Remember" to "Remember that "
)

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: MainScreenViewModel = viewModel {
        val memoryStore = MemoryStore(context)
        MainScreenViewModel(
            chatHistoryStore = ChatHistoryStore(context),
            memoryStore = memoryStore,
            agentTools = AndroidAgentTools(context, memoryStore),
            settingsStore = AgentSettingsStore(context),
            compatibilityChecker = CompatibilityChecker(context),
            updateChecker = UpdateChecker(context)
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val wakeWordRunning by WakeWordService.isRunning.collectAsStateWithLifecycle()

    val voiceController = remember(context) {
        VoiceController(context) { spoken ->
            viewModel.sendPrompt(spoken)
        }
    }
    val voiceState by voiceController.state.collectAsStateWithLifecycle()

    LaunchedEffect(voiceState.voiceError) {
        voiceState.voiceError?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val speechIntentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (spoken.isNotEmpty()) {
            viewModel.sendPrompt(spoken)
        }
    }

    DisposableEffect(voiceController) {
        onDispose { voiceController.release() }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) voiceController.toggleListening()
    }

    val wakePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val micOk = results[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (micOk) WakeWordService.start(context)
    }

    val healthPermissions = remember {
        setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(RestingHeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(HydrationRecord::class),
            HealthPermission.getReadPermission(FloorsClimbedRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        )
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.runCompatibilityChecks()
    }
    val callPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.runCompatibilityChecks()
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.runCompatibilityChecks()
    }
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) {
        viewModel.runCompatibilityChecks()
    }

    var attachedImage by remember { mutableStateOf<Bitmap?>(null) }

    LifecycleResumeEffect(Unit) {
        viewModel.consumeOverlayHandoff()
        onPauseOrDispose {}
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachedImage = ScreenContextHolder.decodeBitmapFromUri(context, uri)
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshChats()
        viewModel.refreshMemories()
        viewModel.runCompatibilityChecks()
        onPauseOrDispose {}
    }

    AnimatedContent(
        targetState = state.settings.hasCompletedOnboarding,
        transitionSpec = {
            (slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it / 16 } + fadeIn(tween(340))) togetherWith
                fadeOut(tween(220))
        },
        label = "onboarding_to_main"
    ) { onboarded ->
        if (!onboarded) {
            OnboardingScreen(
                availability = state.availability,
                availableAiCoreModels = state.availableAiCoreModels,
                settings = state.settings,
                compatibilityReport = state.compatibilityReport,
                isCheckingCompatibility = state.isCheckingCompatibility,
                healthConnectGranted = state.healthConnectGranted,
                onRunChecks = viewModel::runCompatibilityChecks,
                onDownloadModel = viewModel::downloadModel,
                onUpdateSettings = viewModel::updateSettings,
                onGrantMic = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                onGrantLocation = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                onGrantContacts = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                onGrantCall = { callPermissionLauncher.launch(Manifest.permission.CALL_PHONE) },
                onGrantHealthConnect = {
                    if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                        healthPermissionLauncher.launch(healthPermissions)
                    } else {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")))
                    }
                },
                onOpenNotificationAccess = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                onOpenAssistantSettings = { openDefaultAssistantSettings(context) },
                onComplete = { name ->
                    context.getSharedPreferences("kairo_ui_prefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("pending_intro_tour", true).apply()
                    viewModel.completeOnboarding(name)
                }
            )
        } else {
            MainScreenContent(
                state = state,
                isListening = voiceState.isListening,
                rmsDb = voiceState.rmsDb,
                partialTranscript = voiceState.partialText,
                wakeWordEnabled = wakeWordRunning,
                attachedImage = attachedImage,
                onPickImage = {
                    imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onClearImage = { attachedImage = null },
                onSendPrompt = { prompt ->
                    val img = attachedImage
                    attachedImage = null
                    viewModel.sendPrompt(prompt, img)
                },
                onThinkHarder = { prompt ->
                    viewModel.sendPrompt(prompt, thinkHarder = true)
                },
                onSelectPersona = viewModel::selectPersona,
                onUpdateSettings = viewModel::updateSettings,
                onToggleIncognito = viewModel::toggleIncognitoMode,
                onRunCompatibilityChecks = viewModel::runCompatibilityChecks,
                onNewChat = { viewModel.startNewChat(incognito = false) },
                onNewIncognitoChat = { viewModel.startNewChat(incognito = true) },
                onRegenerateTurn = viewModel::regenerateTurn,
                onSelectTurnVersion = viewModel::selectTurnVersion,
                onSelectChat = viewModel::loadChatIntoView,
                onDeleteChat = viewModel::deleteChat,
                onClearChats = viewModel::clearChats,
                onAddMemory = viewModel::addMemory,
                onDeleteMemory = viewModel::deleteMemory,
                onClearMemories = viewModel::clearMemories,
                onToggleWakeWord = { enable ->
                    if (!enable) {
                        WakeWordService.stop(context)
                    } else {
                        val needed = buildList {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                add(Manifest.permission.RECORD_AUDIO)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        if (needed.isEmpty()) WakeWordService.start(context)
                        else wakePermissionLauncher.launch(needed.toTypedArray())
                    }
                },
                onRefreshStatus = viewModel::refreshStatus,
                onDownloadModel = viewModel::downloadModel,
                onWarmupModel = viewModel::warmupModel,
                onCheckUpdates = viewModel::checkForUpdates,
                onFetchReleaseNotes = viewModel::fetchReleaseNotes,
                onOpenUrl = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                onMicClick = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        if (wakeWordRunning) WakeWordService.stop(context)
                        if (android.speech.SpeechRecognizer.isRecognitionAvailable(context)) {
                            voiceController.toggleListening()
                        } else {
                            val speechIntent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            }
                            runCatching { speechIntentLauncher.launch(speechIntent) }
                                .onFailure {
                                    android.widget.Toast.makeText(context, "Speech recognition unavailable", android.widget.Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                },
                onSpeakResponse = { text ->
                    voiceController.setTtsEnabled(true)
                    voiceController.speakIfEnabled(text)
                },
                onGrantContacts = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                onGrantLocation = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                onGrantCall = { callPermissionLauncher.launch(Manifest.permission.CALL_PHONE) },
                onGrantHealthConnect = {
                    if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                        healthPermissionLauncher.launch(healthPermissions)
                    } else {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")))
                    }
                },
                onOpenNotificationAccess = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                onOpenAssistantSettings = { openDefaultAssistantSettings(context) },
                modifier = modifier
            )
        }
    }
}

private val UniqueGreetings = listOf(
    "Let's get started",
    "What's on your mind?",
    "Ready when you are",
    "Purely local. Completely yours.",
    "Ask anything, command anything.",
    "Your silicon, your rules."
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun MainScreenContent(
    state: GemmAHomeState,
    isListening: Boolean,
    rmsDb: Float = 0f,
    partialTranscript: String,
    wakeWordEnabled: Boolean,
    attachedImage: Bitmap? = null,
    onPickImage: () -> Unit = {},
    onClearImage: () -> Unit = {},
    onSendPrompt: (String) -> Unit,
    onThinkHarder: (String) -> Unit = {},
    onSelectPersona: (AgentPersona) -> Unit = {},
    onUpdateSettings: (AgentSettings) -> Unit = {},
    onToggleIncognito: () -> Unit = {},
    onRunCompatibilityChecks: () -> Unit = {},
    onNewChat: () -> Unit,
    onNewIncognitoChat: () -> Unit = {},
    onRegenerateTurn: (Int) -> Unit = {},
    onSelectTurnVersion: (Int, Int) -> Unit = { _, _ -> },
    onSelectChat: (ChatRecord) -> Unit,
    onDeleteChat: (String) -> Unit = {},
    onClearChats: () -> Unit,
    onAddMemory: (String) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onClearMemories: () -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onRefreshStatus: () -> Unit,
    onDownloadModel: () -> Unit,
    onWarmupModel: () -> Unit,
    onCheckUpdates: () -> Unit = {},
    onFetchReleaseNotes: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    onMicClick: () -> Unit,
    onSpeakResponse: (String) -> Unit = {},
    onGrantContacts: () -> Unit = {},
    onGrantLocation: () -> Unit = {},
    onGrantCall: () -> Unit = {},
    onGrantHealthConnect: () -> Unit = {},
    onOpenNotificationAccess: () -> Unit = {},
    onOpenAssistantSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showModelSheet by remember { mutableStateOf(false) }
    var showMemorySheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var showConnectionsSheet by remember { mutableStateOf(false) }
    var showReleaseNotesSheet by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var liveSheetFraction by remember { mutableStateOf(0f) }
    var sparkleTaps by remember { mutableStateOf(0) }
    var pendingDeleteChat by remember { mutableStateOf<ChatRecord?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var draft by remember { mutableStateOf("") }
    var showIntroTour by remember {
        mutableStateOf(
            context.getSharedPreferences("kairo_ui_prefs", Context.MODE_PRIVATE)
                .getBoolean("pending_intro_tour", false)
        )
    }
    var introTourStep by remember { mutableStateOf(0) }
    val pkgInfo = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    }
    val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        pkgInfo?.longVersionCode?.toInt() ?: 1
    } else {
        @Suppress("DEPRECATION")
        pkgInfo?.versionCode ?: 1
    }
    val currentVersionName = pkgInfo?.versionName ?: "1.2.0"
    var showWhatsNewDialog by remember {
        val prefs = context.getSharedPreferences("kairo_ui_prefs", Context.MODE_PRIVATE)
        val lastCode = prefs.getInt("last_seen_version_code", -1)
        val updated = lastCode != currentVersionCode && state.settings.hasCompletedOnboarding
        mutableStateOf(updated)
    }

    if (showWhatsNewDialog) {
        val latestNotes = state.releaseNotes.firstOrNull()?.body?.takeIf { it.isNotBlank() }
            ?: "## v$currentVersionName\n- Response version switcher (scroll between regenerated answers)\n- Multi-Agent Collaboration mode in Settings\n- Exact popup-to-fullscreen chat handoff\n- Incognito Chat directly in New Chat (24h auto-expire, no memory)\n- Multi-language support (English, Dutch, German, Russian)\n- User-friendly action confirmations"
        WhatsNewChangelogDialog(
            versionName = currentVersionName,
            changelogMarkdown = latestNotes,
            onDismiss = {
                context.getSharedPreferences("kairo_ui_prefs", Context.MODE_PRIVATE)
                    .edit().putInt("last_seen_version_code", currentVersionCode).apply()
                showWhatsNewDialog = false
            }
        )
    }

    if (showIntroTour) {
        val tourSteps = listOf(
            Triple(
                "Welcome to Kairo",
                "Kairo runs 100% on-device with Gemma 4. Type any question, or tap the star button for modes & quick prompts.",
                "Try: \"What's my battery and storage status?\""
            ),
            Triple(
                "Guarantee Tools with / or @",
                "Prefix any tool with / or @ (like @weather, /python, or @find_local_image) to force Kairo to execute that exact tool.",
                "Try: \"@weather current\""
            ),
            Triple(
                "Chess Coach & Screen Context",
                "Open Kairo's floating overlay over Chess.com or any app, switch to Chess Coach mode, and ask for next-move ideas or threats.",
                "Try: \"Analyze this position and suggest the best move\""
            ),
            Triple(
                "Privacy & Thermal Guard",
                "Use Incognito Context in Settings for zero-history sessions. Kairo also monitors live device temperature while generating.",
                "Ready? Try it yourself now!"
            )
        )
        val (title, desc, example) = tourSteps[introTourStep]
        val isLast = introTourStep == tourSteps.lastIndex
        val dismissTour = {
            context.getSharedPreferences("kairo_ui_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("pending_intro_tour", false).apply()
            showIntroTour = false
            introTourStep = 0
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = dismissTour,
            containerColor = GeminiDrawerBg,
            titleContentColor = GeminiTextPrimary,
            textContentColor = GeminiTextSecondary,
            icon = { GemmASparkleIcon(size = 36.dp, isAnimating = true) },
            title = { Text("$title (${introTourStep + 1}/${tourSteps.size})", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(desc, style = MaterialTheme.typography.bodyMedium, color = GeminiTextPrimary)
                    Surface(
                        color = GeminiPillBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable {
                            val clean = example.removePrefix("Try: \"").removeSuffix("\"")
                            if (!isLast) {
                                draft = clean
                                dismissTour()
                            }
                        }
                    ) {
                        Text(
                            text = example,
                            color = GeminiAccentBlue,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (isLast) dismissTour() else introTourStep += 1
                }) {
                    Text(if (isLast) "Ready? Try it yourself now!" else "Next")
                }
            },
            dismissButton = {
                TextButton(onClick = dismissTour) { Text("Skip", color = GeminiTextSecondary) }
            }
        )
    }

    if (showClearAllConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            containerColor = GeminiDrawerBg,
            titleContentColor = GeminiTextPrimary,
            textContentColor = GeminiTextSecondary,
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFF28B82)) },
            title = { Text("Clear all chats?", fontWeight = FontWeight.SemiBold) },
            text = { Text("This will permanently delete all ${state.recentChats.size} saved conversations from your device.") },
            confirmButton = {
                Button(onClick = {
                    onClearChats()
                    showClearAllConfirm = false
                }) { Text("Clear All") }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) { Text("Cancel", color = GeminiTextSecondary) }
            }
        )
    }

    pendingDeleteChat?.let { chatToDelete ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDeleteChat = null },
            containerColor = GeminiDrawerBg,
            titleContentColor = GeminiTextPrimary,
            textContentColor = GeminiTextSecondary,
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFF28B82)) },
            title = { Text("Delete chat?", fontWeight = FontWeight.SemiBold) },
            text = { Text("Remove \"${chatToDelete.prompt.take(60)}\" from your local chat history? This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    onDeleteChat(chatToDelete.id)
                    pendingDeleteChat = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteChat = null }) { Text("Cancel", color = GeminiTextSecondary) }
            }
        )
    }

    state.pendingToolConfirmation?.let { pending ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pending.onDecision(false) },
            containerColor = GeminiDrawerBg,
            titleContentColor = GeminiTextPrimary,
            textContentColor = GeminiTextSecondary,
            title = { Text("Confirm Action: ${pending.toolName}", fontWeight = FontWeight.SemiBold) },
            text = {
                Text("Kairo is about to execute `${pending.toolName}`:\n\n${pending.detail}\n\nAllow this action?")
            },
            confirmButton = {
                Button(onClick = { pending.onDecision(true) }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = { pending.onDecision(false) }) { Text("Cancel", color = GeminiTextSecondary) }
            }
        )
    }
    var showIdleEasterEgg by remember { mutableStateOf(false) }
    var greetingSeed by remember { mutableStateOf(0) }
    var showQuickActions by remember { mutableStateOf(false) }
    var showDrawerSearch by remember { mutableStateOf(false) }
    var drawerSearchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val localContext = LocalContext.current
    val palette = com.kairo.agent.ui.theme.LocalKairoPalette.current

    LaunchedEffect(partialTranscript) { if (partialTranscript.isNotEmpty()) draft = partialTranscript }
    LaunchedEffect(state.activeTurns.size, state.activeTurns.lastOrNull()?.response?.length) {
        if (state.activeTurns.isNotEmpty()) listState.animateScrollToItem(state.activeTurns.lastIndex)
    }
    LaunchedEffect(state.activeTurns.size, draft, isListening) {
        showIdleEasterEgg = false
        kotlinx.coroutines.delay(45_000L)
        showIdleEasterEgg = true
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = palette.surface,
                drawerContentColor = GeminiTextPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Kairo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = GeminiTextPrimary,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                        Row {
                            IconButton(onClick = {
                                showDrawerSearch = !showDrawerSearch
                                if (!showDrawerSearch) drawerSearchQuery = ""
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Search chats", tint = GeminiTextPrimary)
                            }
                            IconButton(onClick = { scope.launch { drawerState.close() } }) {
                                Icon(Icons.Default.Close, contentDescription = "Close drawer", tint = GeminiTextPrimary)
                            }
                        }
                    }

                AnimatedVisibility(visible = showDrawerSearch) {
                        OutlinedTextField(
                            value = drawerSearchQuery,
                            onValueChange = { drawerSearchQuery = it },
                            placeholder = { Text("Search recent chats", color = GeminiTextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = GeminiAccentBlue)
                                ) {
                                    onNewChat()
                                    scope.launch { drawerState.close() }
                                },
                            shape = RoundedCornerShape(50),
                            color = GeminiPillBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = GeminiTextPrimary, modifier = Modifier.size(18.dp))
                                Text(stringResource(R.string.new_chat), color = GeminiTextPrimary, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = GeminiAccentBlue)
                                ) {
                                    onNewIncognitoChat()
                                    scope.launch { drawerState.close() }
                                },
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF282235)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Policy, contentDescription = "Incognito chat", tint = GeminiAccentBlue, modifier = Modifier.size(18.dp))
                                Text(stringResource(R.string.incognito_chat), color = GeminiAccentBlue, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    DrawerMenuRow(Icons.Default.NewReleases, "Release Notes") {
                        scope.launch { drawerState.close() }
                        onFetchReleaseNotes()
                        showReleaseNotesSheet = true
                    }
                    DrawerMenuRow(Icons.Default.Policy, "Privacy, Terms & Credits") {
                        scope.launch { drawerState.close() }
                        showPrivacySheet = true
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onToggleWakeWord(!wakeWordEnabled) }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Icon(
                                imageVector = Icons.Default.Hearing,
                                contentDescription = null,
                                tint = if (wakeWordEnabled) GeminiAccentBlue else GeminiTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("Hey Kairo wake word", color = GeminiTextPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(checked = wakeWordEnabled, onCheckedChange = onToggleWakeWord)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF232528))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent", style = MaterialTheme.typography.labelLarge, color = GeminiTextSecondary)
                        if (state.recentChats.isNotEmpty()) {
                            TextButton(onClick = { showClearAllConfirm = true }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = GeminiTextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Clear", color = GeminiTextSecondary)
                            }
                        }
                    }

                    val filteredChats = remember(state.recentChats, drawerSearchQuery) {
                        if (drawerSearchQuery.isBlank()) state.recentChats
                        else state.recentChats.filter {
                            it.prompt.contains(drawerSearchQuery, ignoreCase = true) ||
                                it.response.contains(drawerSearchQuery, ignoreCase = true)
                        }
                    }

                    if (filteredChats.isEmpty()) {
                        Text(
                            text = if (state.recentChats.isEmpty()) "No recent chats yet." else "No matching chats.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GeminiTextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                        Spacer(Modifier.height(24.dp))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            filteredChats.forEach { chat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(color = GeminiAccentBlue)
                                        ) {
                                            onSelectChat(chat)
                                            scope.launch { drawerState.close() }
                                        }
                                        .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(chat.prompt, color = GeminiTextPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (chat.timestampMs > 0L) {
                                            Text(
                                                text = DateUtils.getRelativeTimeSpanString(chat.timestampMs, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                                                color = GeminiTextSecondary,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                    IconButton(onClick = { pendingDeleteChat = chat }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete chat", tint = GeminiTextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                    } // end scrollable drawer content

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF232528))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Kairo • by DaManMikey", color = GeminiTextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("ON-DEVICE • GEMMA 4", color = GeminiAccentBlue, style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = {
                            scope.launch { drawerState.close() }
                            showSettingsScreen = true
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = GeminiTextPrimary)
                        }
                    }
                }
            }
        }
    ) {
        val density = LocalDensity.current
        val drawerWidthPx = with(density) { 360.dp.toPx() }
        val drawerOffset = drawerState.currentOffset
        val drawerProgress = if (drawerOffset.isNaN()) {
            if (drawerState.isOpen) 1f else 0f
        } else {
            (1f + (drawerOffset / drawerWidthPx)).coerceIn(0f, 1f)
        }
        val anySheetOpen = showModelSheet || showMemorySheet || showPrivacySheet || showConnectionsSheet || showReleaseNotesSheet
        BackHandler(enabled = state.activeTurns.isNotEmpty() && !anySheetOpen && !drawerState.isOpen) {
            onNewChat()
        }
        LaunchedEffect(anySheetOpen) {
            if (!anySheetOpen) liveSheetFraction = 0f
        }
        val fallbackSheetBlur by animateFloatAsState(
            targetValue = if (anySheetOpen) 12f else 0f,
            animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
            label = "sheet_blur"
        )
        val effectiveSheetBlur = if (anySheetOpen && liveSheetFraction > 0f) liveSheetFraction * 12f else fallbackSheetBlur
        val liveBlur = maxOf(effectiveSheetBlur, drawerProgress * 14f).dp

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(palette.bg)
                .blur(liveBlur)
        ) {
            ReactiveAuroraBackground(
                isGenerating = state.isGenerating,
                isListening = isListening,
                rmsDb = rmsDb
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) { TwoLineMenuIcon() }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = GeminiAccentBlue)
                                ) { showModelSheet = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val modelSubtitle = if (state.settings.usesNativeModel) "GGUF" else when (state.settings.backendMode) {
                                com.kairo.agent.data.EngineBackendMode.CLOUD_AI -> "Cloud • ${state.settings.cloudModelName}"
                                com.kairo.agent.data.EngineBackendMode.LOCAL_HTTP -> state.settings.localModelName
                                else -> "Gemma 4"
                            }
                            Text("Kairo", color = GeminiTextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                            Text(modelSubtitle, color = GeminiTextSecondary, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Model status", tint = GeminiTextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (state.settings.backendMode == EngineBackendMode.CLOUD_AI) {
                            Surface(
                                color = Color(0xFF4A1E24),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.clickable {
                                    onUpdateSettings(state.settings.copy(backendMode = EngineBackendMode.AUTO))
                                }
                            ) {
                                Text(
                                    text = "Disable Cloud AI",
                                    color = Color(0xFFF28B82),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        } else if (state.settings.showNerdStats && state.nerdStats != null && state.nerdStats.directTool) {
                            Surface(color = GeminiChipBg, shape = RoundedCornerShape(50)) {
                                Text("Android • direct", color = GeminiAccentBlue,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        } else if (state.settings.showNerdStats && state.nerdStats != null && state.nerdStats.tokensPerSec > 0f) {
                            Surface(color = GeminiChipBg, shape = RoundedCornerShape(50)) {
                                Text(
                                    text = "${"%.1f".format(state.nerdStats.tokensPerSec)} tok/s • ${state.nerdStats.ramUsedMb}MB",
                                    color = GeminiAccentBlue,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        IconButton(onClick = {
                            greetingSeed++
                            sparkleTaps = 0
                            onNewChat()
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "New chat", tint = GeminiTextPrimary, modifier = Modifier.size(21.dp))
                        }
                    }
                }

                var lastNonEmptyTurns by remember { mutableStateOf<List<ConversationTurn>>(emptyList()) }
                LaunchedEffect(state.activeTurns) {
                    if (state.activeTurns.isNotEmpty()) {
                        lastNonEmptyTurns = state.activeTurns
                    }
                }

                SharedTransitionLayout(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    val oneUiEase = remember { CubicBezierEasing(0.117f, 0.126f, 0.312f, 1f) }
                    AnimatedContent(
                        targetState = state.activeTurns.isEmpty(),
                        transitionSpec = {
                            fadeIn(tween(380, easing = oneUiEase)) togetherWith fadeOut(tween(240))
                        },
                        label = "sparkle_shared_transition"
                    ) { isEmpty ->
                        if (isEmpty) {
                            var homeEntered by remember(greetingSeed) { mutableStateOf(false) }
                            LaunchedEffect(greetingSeed) {
                                homeEntered = false
                                kotlinx.coroutines.delay(50L)
                                homeEntered = true
                            }
                            val homeAlpha by animateFloatAsState(
                                targetValue = if (homeEntered) 1f else 0f,
                                animationSpec = tween(560, easing = oneUiEase),
                                label = "home_welcome_alpha"
                            )
                            val homeRise by animateFloatAsState(
                                targetValue = if (homeEntered) 0f else 36f,
                                animationSpec = tween(620, easing = oneUiEase),
                                label = "home_welcome_rise"
                            )
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(18.dp),
                                    modifier = Modifier
                                        .padding(horizontal = 24.dp)
                                        .graphicsLayer {
                                            alpha = homeAlpha
                                            translationY = homeRise
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .sharedElement(
                                                sharedContentState = rememberSharedContentState(key = "kairo_sparkle"),
                                                animatedVisibilityScope = this@AnimatedContent
                                            )
                                            .clip(CircleShape)
                                            .combinedClickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = false, color = GeminiAccentBlue),
                                                onClick = { sparkleTaps++ },
                                                onLongClick = {
                                                    android.widget.Toast.makeText(
                                                        localContext,
                                                        "✦ Kairo Engine • On-Device • Built by DaManMikey",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            )
                                            .padding(8.dp)
                                    ) {
                                        GemmASparkleIcon(size = 44.dp, isAnimating = state.isGenerating || sparkleTaps >= 5)
                                    }
                                    val baseGreeting = remember(greetingSeed) {
                                        UniqueGreetings[greetingSeed % UniqueGreetings.size]
                                    }
                                    val greeting = when {
                                        sparkleTaps >= 15 -> "✦ Overclocked local vibes!"
                                        sparkleTaps >= 10 -> "Pure local silicon, zero cloud."
                                        sparkleTaps >= 5 -> "All thoughts stay on-device ✦"
                                        else -> baseGreeting
                                    }
                                    BlurIntoText(key = greeting) {
                                        Text(greeting, color = GeminiTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Normal)
                                    }

                                    if (state.isGenerating || state.isThermallyThrottled) {
                    val tempStr = if (state.deviceTempC > 0f) "${"%.1f".format(state.deviceTempC)}°C • " else ""
                    Surface(
                        color = if (state.isThermallyThrottled) Color(0xFF4A1E24) else GeminiPillBg,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (state.isThermallyThrottled) "⚠ Thermal Guard: $tempStr${state.thermalLabel}" else "Device: $tempStr${state.thermalLabel}",
                            color = if (state.isThermallyThrottled) Color(0xFFF28B82) else GeminiTextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                val slashOrAtQuery = remember(draft) {
                    val m = Regex("""(?:^|\s)([/@])([a-z_]*)$""").find(draft)
                    m?.let { it.groupValues[1] to it.groupValues[2] }
                }
                if (slashOrAtQuery != null) {
                    val (sigil, prefix) = slashOrAtQuery
                    val allTools = listOf(
                        "get_weather" to "Live local weather",
                        "get_current_time" to "Exact local time",
                        "run_python_script" to "Run Python math/script",
                        "find_local_image" to "Attach photo from device",
                        "search_ephemeral_memory" to "7-day clipboard/screen memory",
                        "web_search" to "In-app search",
                        "get_health_summary" to "Health Connect insights",
                        "call_person" to "Call contact",
                        "send_whatsapp" to "Send WhatsApp message"
                    ).filter { it.first.contains(prefix, ignoreCase = true) }
                    if (allTools.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allTools.forEach { (toolName, desc) ->
                                AssistChip(
                                    onClick = {
                                        draft = draft.replace(Regex("""([/@])[a-z_]*$"""), "$sigil$toolName ")
                                    },
                                    label = { Text("$sigil$toolName • $desc", color = GeminiAccentBlue, style = MaterialTheme.typography.labelSmall) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = GeminiPillBg),
                                    border = null
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = showIdleEasterEgg) {
                                        Surface(color = GeminiPillBg, shape = RoundedCornerShape(50)) {
                                            Text(
                                                text = "You still there? 👀",
                                                color = GeminiAccentBlue,
                                                style = MaterialTheme.typography.labelMedium,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                            )
                                        }
                                    }

                                    // Example prompts right after setup / on empty screen
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            "Check my battery & status",
                                            "Catch me up on notifications",
                                            "Set a 7:30 AM alarm",
                                            "What can you do?"
                                        ).forEach { example ->
                                            AssistChip(
                                                onClick = { onSendPrompt(example) },
                                                label = { Text(example, color = GeminiTextPrimary, style = MaterialTheme.typography.labelMedium) },
                                                colors = AssistChipDefaults.assistChipColors(containerColor = GeminiPillBg),
                                                border = null
                                            )
                                        }
                                    }

                                    if (state.availability == ModelAvailability.DOWNLOADABLE && !state.settings.bypassCompatibility) {
                                        FilledTonalButton(onClick = onDownloadModel) {
                                            Icon(Icons.Default.Download, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Download Gemma 4")
                                        }
                                    }
                                }
                            }
                        } else {
                            val turnsToRender = if (state.activeTurns.isNotEmpty()) state.activeTurns else lastNonEmptyTurns
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                items(turnsToRender.size) { idx ->
                                    val turn = turnsToRender[idx]
                                    val sparkleModifier = if (idx == 0) {
                                        Modifier.sharedElement(
                                            sharedContentState = rememberSharedContentState(key = "kairo_sparkle"),
                                            animatedVisibilityScope = this@AnimatedContent
                                        )
                                    } else {
                                        Modifier
                                    }
                                    Box(modifier = Modifier.animateItem()) {
                                        ConversationTurnItem(
                                            turn = turn,
                                            sparkleModifier = sparkleModifier,
                                            onSpeak = { onSpeakResponse(turn.response) },
                                            onRegenerate = { onRegenerateTurn(idx) },
                                            onSelectVersion = { ver -> onSelectTurnVersion(idx, ver) },
                                            onThinkHarder = { onThinkHarder(turn.prompt) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (state.incognitoMode) {
                    Surface(
                        color = Color(0xFF282235),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Policy, contentDescription = null, tint = GeminiAccentBlue, modifier = Modifier.size(16.dp))
                                Text(
                                    text = stringResource(R.string.incognito_banner),
                                    color = GeminiAccentBlue,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            TextButton(onClick = onNewChat, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                Text(stringResource(R.string.exit_incognito), color = GeminiTextPrimary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                if (state.isGenerating || state.isThermallyThrottled) {
                    val tempStr = if (state.deviceTempC > 0f) "${"%.1f".format(state.deviceTempC)}°C • " else ""
                    Surface(
                        color = if (state.isThermallyThrottled) Color(0xFF4A1E24) else GeminiPillBg,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (state.isThermallyThrottled) "⚠ Thermal Guard: $tempStr${state.thermalLabel}" else "Device: $tempStr${state.thermalLabel}",
                            color = if (state.isThermallyThrottled) Color(0xFFF28B82) else GeminiTextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                val slashOrAtQuery = remember(draft) {
                    val m = Regex("""(?:^|\s)([/@])([a-z_]*)$""").find(draft)
                    m?.let { it.groupValues[1] to it.groupValues[2] }
                }
                if (slashOrAtQuery != null) {
                    val (sigil, prefix) = slashOrAtQuery
                    val allTools = listOf(
                        "get_weather" to "Live local weather",
                        "get_current_time" to "Exact local time",
                        "run_python_script" to "Run Python math/script",
                        "find_local_image" to "Attach photo from device",
                        "search_ephemeral_memory" to "7-day clipboard/screen memory",
                        "web_search" to "In-app search",
                        "get_health_summary" to "Health Connect insights",
                        "call_person" to "Call contact",
                        "send_whatsapp" to "Send WhatsApp message"
                    ).filter { it.first.contains(prefix, ignoreCase = true) }
                    if (allTools.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allTools.forEach { (toolName, desc) ->
                                AssistChip(
                                    onClick = {
                                        draft = draft.replace(Regex("""([/@])[a-z_]*$"""), "$sigil$toolName ")
                                    },
                                    label = { Text("$sigil$toolName • $desc", color = GeminiAccentBlue, style = MaterialTheme.typography.labelSmall) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = GeminiPillBg),
                                    border = null
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showQuickActions,
                    enter = expandVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(220)),
                    exit = shrinkVertically() + fadeOut(tween(160))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AgentPersona.entries.forEach { persona ->
                                FilterChip(
                                    selected = state.selectedPersona == persona,
                                    onClick = { onSelectPersona(persona) },
                                    label = { Text(persona.label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = GeminiPillBg,
                                        selectedContainerColor = GeminiAccentBlue.copy(alpha = 0.22f),
                                        labelColor = GeminiTextSecondary,
                                        selectedLabelColor = GeminiAccentBlue
                                    ),
                                    border = null
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AgentQuickActions.forEach { (label, promptText) ->
                                AssistChip(
                                    onClick = {
                                        showQuickActions = false
                                        if (promptText.endsWith(" ")) draft = promptText
                                        else onSendPrompt(promptText)
                                    },
                                    label = { Text(label, color = GeminiTextPrimary) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = GeminiChipBg),
                                    border = null
                                )
                            }
                        }
                    }
                }

                if (attachedImage != null) {
                    Surface(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = GeminiChipBg,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                bitmap = attachedImage.asImageBitmap(),
                                contentDescription = "Selected image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Text("Image attached", color = GeminiTextPrimary, style = MaterialTheme.typography.labelMedium)
                            IconButton(onClick = onClearImage, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Remove image", tint = GeminiTextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                val submitDraft = {
                    val trimmed = draft.trim()
                    if (trimmed.isNotEmpty() || attachedImage != null) {
                        onSendPrompt(trimmed.ifEmpty { "Describe this image." })
                        draft = ""
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .animatedAiBorder(isActive = state.isGenerating, cornerRadius = 28.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = palette.pill
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showQuickActions = !showQuickActions }) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Modes & suggestions",
                                tint = if (showQuickActions || state.selectedPersona != AgentPersona.DEFAULT) GeminiAccentBlue else GeminiTextPrimary
                            )
                        }
                        IconButton(onClick = onPickImage) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Upload image", tint = GeminiAccentBlue)
                        }
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            textStyle = TextStyle(color = GeminiTextPrimary, fontSize = 16.sp),
                            cursorBrush = SolidColor(GeminiAccentBlue),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submitDraft() }),
                            maxLines = 4,
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (draft.isEmpty()) {
                                    if (isListening) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            ListeningWaveformIndicator()
                                            Text(stringResource(R.string.listening), color = GeminiAccentBlue, fontSize = 16.sp)
                                        }
                                    } else {
                                        Text(stringResource(R.string.ask_kairo), color = GeminiTextSecondary, fontSize = 16.sp)
                                    }
                                }
                                innerTextField()
                            }
                        )
                        IconButton(onClick = onMicClick) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Voice input",
                                tint = if (isListening) GeminiListeningRed else GeminiTextPrimary
                            )
                        }
                        val canSend = draft.isNotBlank() || attachedImage != null
                        val sendScale by animateFloatAsState(
                            targetValue = if (canSend) 1.06f else 0.96f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "send_scale"
                        )
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .scale(sendScale)
                                .clip(CircleShape)
                                .background(if (canSend) GeminiAccentBlue else GeminiChipBg)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = Color.White),
                                    enabled = canSend,
                                    onClick = submitDraft
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Send",
                                tint = if (canSend) GeminiBgTop else GeminiTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showModelSheet) {
        val modelSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val travelPx = with(LocalDensity.current) { 420.dp.toPx() }
        LaunchedEffect(modelSheetState) {
            androidx.compose.runtime.snapshotFlow {
                runCatching { modelSheetState.requireOffset() }.getOrNull()
            }.collect { offset ->
                if (offset != null && travelPx > 0f) {
                    liveSheetFraction = (1f - (offset / travelPx)).coerceIn(0f, 1f)
                }
            }
        }
        ModalBottomSheet(
            sheetState = modelSheetState,
            onDismissRequest = {
                liveSheetFraction = 0f
                showModelSheet = false
            },
            containerColor = GeminiDrawerBg,
            contentColor = GeminiTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val headerTitle = if (state.settings.usesNativeModel) "Kairo • Embedded GGUF" else when (state.settings.backendMode) {
                        com.kairo.agent.data.EngineBackendMode.CLOUD_AI -> "Kairo • Cloud (${state.settings.cloudModelName})"
                        com.kairo.agent.data.EngineBackendMode.LOCAL_HTTP -> "Kairo • ${state.settings.localModelName}"
                        else -> "Kairo • Powered by Gemma 4"
                    }
                    Text(headerTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onRefreshStatus) { Text("Refresh", color = GeminiAccentBlue) }
                }

                if (state.settings.backendMode == com.kairo.agent.data.EngineBackendMode.CLOUD_AI) {
                    Surface(color = Color(0xFF4A1E24), shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Cloud AI Active (${state.settings.cloudModelName})", color = Color(0xFFF28B82), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Text("Privacy is not guaranteed while Cloud AI is enabled.", color = GeminiTextSecondary, style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = { onUpdateSettings(state.settings.copy(backendMode = com.kairo.agent.data.EngineBackendMode.AUTO)) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Disable Cloud AI") }
                        }
                    }
                }

                val statusText = when {
                    state.settings.usesNativeModel -> "Embedded llama.cpp • ${state.settings.nativeModelName}"
                    state.settings.backendMode == com.kairo.agent.data.EngineBackendMode.CLOUD_AI -> "Using external Cloud AI (${state.settings.cloudEndpointUrl})"
                    state.settings.backendMode == com.kairo.agent.data.EngineBackendMode.LOCAL_HTTP -> "Using Local HTTP (${state.settings.localEndpointUrl})"
                    state.settings.bypassCompatibility -> "Bypass Mode active (${state.settings.backendMode.name})"
                    state.availability == ModelAvailability.CHECKING -> "Checking AICore availability..."
                    state.availability == ModelAvailability.AVAILABLE -> "Gemma 4 is ready on-device"
                    state.availability == ModelAvailability.DOWNLOADABLE -> "Gemma 4 is ready to download"
                    state.availability == ModelAvailability.DOWNLOADING -> "Downloading Gemma 4..."
                    else -> "AICore unavailable (tap Settings)"
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.availability == ModelAvailability.CHECKING || state.isWarmingUp) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                    Text(statusText, color = GeminiTextPrimary)
                }

                if (state.availability == ModelAvailability.DOWNLOADING && state.totalDownloadBytes > 0L) {
                    LinearProgressIndicator(progress = { (state.downloadProgressBytes.toFloat() / state.totalDownloadBytes.toFloat()).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                }

                // AICore Model Selector (Auto + discovered Stable/Preview Fast/Full models)
                Text(if (state.settings.usesNativeModel) "AICore fallback (disable GGUF in Settings)" else "AICore Model", color = GeminiAccentBlue, style = MaterialTheme.typography.labelLarge)
                Surface(
                    color = Color(0xFF141619),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val selectedProfile = state.settings.aiCoreModelProfile
                        val autoSelected = selectedProfile == AiCoreModelProfile.AUTO
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (autoSelected) Color(0xFF1E2A45) else Color.Transparent)
                                .clickable {
                                    onUpdateSettings(state.settings.copy(aiCoreModelProfile = AiCoreModelProfile.AUTO))
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto (Best Available)", color = GeminiTextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("Prefers Stable, falls back to Preview if needed", color = GeminiTextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                            if (autoSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GeminiAccentBlue, modifier = Modifier.size(18.dp))
                            }
                        }

                        state.availableAiCoreModels.forEach { option ->
                            val isSelected = selectedProfile == option.profile
                            val isAvailable = option.availability == ModelAvailability.AVAILABLE
                            val isDownloadable = option.availability == ModelAvailability.DOWNLOADABLE
                            val statusLabel = when (option.availability) {
                                ModelAvailability.AVAILABLE -> "Available"
                                ModelAvailability.DOWNLOADABLE -> "Downloadable"
                                ModelAvailability.DOWNLOADING -> "Downloading"
                                ModelAvailability.CHECKING -> "Checking"
                                ModelAvailability.UNAVAILABLE -> "Unavailable"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF1E2A45) else Color.Transparent)
                                    .clickable(enabled = isAvailable || isDownloadable || isSelected) {
                                        onUpdateSettings(state.settings.copy(aiCoreModelProfile = option.profile))
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        option.baseModelName ?: option.label,
                                        color = if (isAvailable || isDownloadable) GeminiTextPrimary else GeminiTextSecondary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${option.label} • $statusLabel",
                                        color = if (isAvailable) Color(0xFF81C784) else GeminiTextSecondary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GeminiAccentBlue, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                if (state.availability == ModelAvailability.DOWNLOADABLE) {
                    Button(onClick = onDownloadModel, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Download Gemma 4")
                    }
                } else if (state.availability == ModelAvailability.AVAILABLE) {
                    FilledTonalButton(onClick = onWarmupModel, enabled = !state.isWarmingUp, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.isWarmingUp) "Warming up..." else "Warm up Gemma 4")
                    }
                }

                AppLanguagePickerRow(
                    selectedAppLang = state.settings.appLanguage,
                    selectedModelLang = state.settings.preferredLanguage,
                    onSelectAppAndModelLang = { appLang, modelLang ->
                        onUpdateSettings(state.settings.copy(appLanguage = appLang, preferredLanguage = modelLang))
                    },
                    accentColor = GeminiAccentBlue
                )

                OutlinedButton(
                    onClick = {
                        showModelSheet = false
                        onOpenAssistantSettings()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Set as Default Digital Assistant")
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showConnectionsSheet) {
        val ctx = LocalContext.current
        val micGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val contactsGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val locationGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val callGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val notifGranted = com.kairo.agent.service.NotificationCatchUpStore.isAccessGranted(ctx)
        val writeSettingsGranted = Settings.System.canWrite(ctx)

        var pendingDisclosure by remember { mutableStateOf<Triple<String, String, () -> Unit>?>(null) }
        pendingDisclosure?.let { (title, body, proceed) ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { pendingDisclosure = null },
                containerColor = GeminiDrawerBg,
                titleContentColor = GeminiTextPrimary,
                textContentColor = GeminiTextSecondary,
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                text = { Text(body) },
                confirmButton = {
                    Button(onClick = {
                        pendingDisclosure = null
                        proceed()
                    }) { Text("Continue") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDisclosure = null }) { Text("Cancel", color = GeminiTextSecondary) }
                }
            )
        }

        val connSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val connTravelPx = with(LocalDensity.current) { 420.dp.toPx() }
        LaunchedEffect(connSheetState) {
            androidx.compose.runtime.snapshotFlow {
                runCatching { connSheetState.requireOffset() }.getOrNull()
            }.collect { offset ->
                if (offset != null && connTravelPx > 0f) {
                    liveSheetFraction = (1f - (offset / connTravelPx)).coerceIn(0f, 1f)
                }
            }
        }
        ModalBottomSheet(sheetState = connSheetState, onDismissRequest = { liveSheetFraction = 0f; showConnectionsSheet = false }, containerColor = GeminiDrawerBg, contentColor = GeminiTextPrimary) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Connections & Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Manage optional Android permissions and power features. All processing stays on-device.", style = MaterialTheme.typography.bodySmall, color = GeminiTextSecondary)

                ConnectionPermRow("Microphone", "Voice prompts & wake word", micGranted) {
                    pendingDisclosure = Triple("Microphone Access", "Used strictly when you tap the mic or enable 'Hey Kairo' wake word. Audio is processed locally and never uploaded.", onMicClick)
                }
                ConnectionPermRow("Location", "Local weather, nearby places & GPS context", locationGranted) {
                    pendingDisclosure = Triple("Location Access", "Used only when you ask location-based questions (weather, nearby places). Coordinates stay 100% on-device.", onGrantLocation)
                }
                ConnectionPermRow("Contacts", "Call & text people by name", contactsGranted) {
                    pendingDisclosure = Triple("Contacts Access", "Used locally on-device solely to resolve spoken/typed contact names to phone numbers or emails.", onGrantContacts)
                }
                ConnectionPermRow("Direct Calling", "Place calls directly", callGranted) {
                    pendingDisclosure = Triple("Direct Calling Access", "Allows Kairo to place calls directly when you ask instead of stopping at the dialer.", onGrantCall)
                }
                ConnectionPermRow("Notification Catch-Up", "Summarize unread messages", notifGranted) {
                    pendingDisclosure = Triple("Notification Catch-Up", "Reads unread notifications locally in memory so Kairo can summarize what you missed.", onOpenNotificationAccess)
                }
                ConnectionPermRow("Health Connect", "Steps, heart rate, sleep, distance, calories & workouts", state.healthConnectGranted) {
                    pendingDisclosure = Triple("Health Connect Access", "Reads your local Health Connect fitness & sleep metrics solely for private on-device summaries.", onGrantHealthConnect)
                }
                ConnectionPermRow("System Brightness", "Adjust screen brightness", writeSettingsGranted) {
                    pendingDisclosure = Triple("Modify System Settings", "Lets Kairo adjust screen brightness directly when you ask.") {
                        val pkgIntent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                        val fallbackIntent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        val target = if (pkgIntent.resolveActivity(ctx.packageManager) != null) pkgIntent else fallbackIntent
                        ctx.startActivity(target)
                    }
                }

                HorizontalDivider(color = Color(0xFF232528), modifier = Modifier.padding(vertical = 4.dp))

                SetupSwitchRow(
                    title = "Root / Shell Power Mode",
                    subtitle = "Allow shell / root commands when requested",
                    checked = state.settings.rootPowerMode,
                    onCheckedChange = { onUpdateSettings(state.settings.copy(rootPowerMode = it)) }
                )
                SetupSwitchRow(
                    title = "Live Nerd Stats (tok/s, RAM)",
                    subtitle = "Show inference speed & memory usage",
                    checked = state.settings.showNerdStats,
                    onCheckedChange = { onUpdateSettings(state.settings.copy(showNerdStats = it)) }
                )
                SetupSwitchRow(
                    title = "Cover-Screen Mini-Mode Overlay",
                    subtitle = "Start overlay as a compact pill until a reply streams",
                    checked = state.settings.miniModeOverlay,
                    onCheckedChange = { onUpdateSettings(state.settings.copy(miniModeOverlay = it)) }
                )

                HorizontalDivider(color = Color(0xFF232528), modifier = Modifier.padding(vertical = 4.dp))
                Text("Connected Apps & Actions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = GeminiAccentBlue)
                Text(
                    "• Split-Screen Multitasking (open two apps side-by-side)\n" +
                        "• Spotify & System Media Controls (play, pause, next, prev)\n" +
                        "• WhatsApp, SMS, Phone & Email\n" +
                        "• Maps Navigation, Nearby Search & YouTube\n" +
                        "• Clock Alarms/Timers, Calendar Events & Play Store\n" +
                        "• Home Screen & Cover-Screen Widgets",
                    style = MaterialTheme.typography.bodySmall,
                    color = GeminiTextSecondary
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showReleaseNotesSheet) {
        var selectedVersionFilter by remember { mutableStateOf<String?>(null) }
        val releaseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val releaseTravelPx = with(LocalDensity.current) { 420.dp.toPx() }
        LaunchedEffect(releaseSheetState) {
            androidx.compose.runtime.snapshotFlow {
                runCatching { releaseSheetState.requireOffset() }.getOrNull()
            }.collect { offset ->
                if (offset != null && releaseTravelPx > 0f) {
                    liveSheetFraction = (1f - (offset / releaseTravelPx)).coerceIn(0f, 1f)
                }
            }
        }
        ModalBottomSheet(
            sheetState = releaseSheetState,
            onDismissRequest = {
                liveSheetFraction = 0f
                showReleaseNotesSheet = false
            },
            containerColor = GeminiDrawerBg,
            contentColor = GeminiTextPrimary
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Release Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onFetchReleaseNotes, enabled = !state.isLoadingReleaseNotes) {
                        Text(if (state.isLoadingReleaseNotes) "Loading..." else "Refresh", color = GeminiAccentBlue)
                    }
                }

                if (state.releaseNotes.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedVersionFilter == null,
                            onClick = { selectedVersionFilter = null },
                            label = { Text("All") }
                        )
                        state.releaseNotes.forEach { rel ->
                            FilterChip(
                                selected = selectedVersionFilter == rel.tagName,
                                onClick = { selectedVersionFilter = rel.tagName },
                                label = { Text(rel.tagName) }
                            )
                        }
                    }
                }

                if (state.isLoadingReleaseNotes) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (state.releaseNotesError != null) {
                    Text(state.releaseNotesError, style = MaterialTheme.typography.bodySmall, color = GeminiTextSecondary)
                    OutlinedButton(
                        onClick = { onOpenUrl(UpdateChecker.RELEASES_PAGE_URL) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Open GitHub Releases") }
                } else if (state.releaseNotes.isEmpty()) {
                    Text("No releases loaded yet.", style = MaterialTheme.typography.bodySmall, color = GeminiTextSecondary)
                } else {
                    val filteredNotes = if (selectedVersionFilter == null) state.releaseNotes
                    else state.releaseNotes.filter { it.tagName == selectedVersionFilter }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredNotes, key = { it.tagName }) { rel ->
                            Surface(color = GeminiPillBg, shape = RoundedCornerShape(14.dp)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(rel.title, color = GeminiAccentBlue, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Text(rel.publishedDate, color = GeminiTextSecondary, style = MaterialTheme.typography.labelSmall)
                                    }
                                    val bodyToRender = if (selectedVersionFilter != null) {
                                        extractVersionSection(rel.body, selectedVersionFilter!!)
                                    } else {
                                        rel.body
                                    }
                                    if (bodyToRender.isNotBlank()) {
                                        RichResponseText(text = bodyToRender, textColor = GeminiTextPrimary)
                                    }
                                    if (rel.apkUrl != null) {
                                        TextButton(onClick = { onOpenUrl(rel.apkUrl) }) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Download APK", color = GeminiAccentBlue)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showMemorySheet) {
        var newMemoryText by remember { mutableStateOf("") }
        val memSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val memTravelPx = with(LocalDensity.current) { 420.dp.toPx() }
        LaunchedEffect(memSheetState) {
            androidx.compose.runtime.snapshotFlow {
                runCatching { memSheetState.requireOffset() }.getOrNull()
            }.collect { offset ->
                if (offset != null && memTravelPx > 0f) {
                    liveSheetFraction = (1f - (offset / memTravelPx)).coerceIn(0f, 1f)
                }
            }
        }
        ModalBottomSheet(sheetState = memSheetState, onDismissRequest = { liveSheetFraction = 0f; showMemorySheet = false }, containerColor = GeminiDrawerBg, contentColor = GeminiTextPrimary) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Kairo Memory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (state.memories.isNotEmpty()) {
                        TextButton(onClick = onClearMemories) { Text("Clear all", color = GeminiListeningRed) }
                    }
                }
                Text("Kairo automatically saves important facts with <memory> tags and injects them across all chats.", style = MaterialTheme.typography.bodySmall, color = GeminiTextSecondary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newMemoryText,
                        onValueChange = { newMemoryText = it },
                        placeholder = { Text("Add a memory...", color = GeminiTextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    FilledTonalButton(onClick = {
                        if (newMemoryText.isNotBlank()) {
                            onAddMemory(newMemoryText)
                            newMemoryText = ""
                        }
                    }) { Text("Save") }
                }
                if (state.memories.isEmpty()) {
                    Text("No memories saved yet.", style = MaterialTheme.typography.bodyMedium, color = GeminiTextSecondary, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.memories, key = { it.id }) { item ->
                            Surface(color = GeminiPillBg, shape = RoundedCornerShape(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.fact, color = GeminiTextPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { onDeleteMemory(item.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete memory", tint = GeminiTextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showPrivacySheet) {
        val privSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val privTravelPx = with(LocalDensity.current) { 420.dp.toPx() }
        LaunchedEffect(privSheetState) {
            androidx.compose.runtime.snapshotFlow {
                runCatching { privSheetState.requireOffset() }.getOrNull()
            }.collect { offset ->
                if (offset != null && privTravelPx > 0f) {
                    liveSheetFraction = (1f - (offset / privTravelPx)).coerceIn(0f, 1f)
                }
            }
        }
        ModalBottomSheet(sheetState = privSheetState, onDismissRequest = { liveSheetFraction = 0f; showPrivacySheet = false }, containerColor = GeminiDrawerBg, contentColor = GeminiTextPrimary) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Privacy Policy, Terms & Credits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("100% Local On-Device Agent", style = MaterialTheme.typography.labelLarge, color = GeminiAccentBlue)
                Text(
                    "• All prompts, images, screen context, notifications, and voice transcriptions are processed locally via Android AICore (Gemma 4) or your own local model endpoint.\n" +
                        "• Recent chats and saved memories are stored only in private app storage and can be deleted anytime.\n" +
                        "• Zero analytics, zero ads, and zero tracking SDKs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GeminiTextSecondary
                )
                Text("Terms of Service (MIT)", style = MaterialTheme.typography.labelLarge, color = GeminiAccentBlue)
                Text("Kairo is open-source software provided under the MIT License without warranty. Always verify AI-generated responses and device actions.", style = MaterialTheme.typography.bodySmall, color = GeminiTextSecondary)
                HorizontalDivider(color = Color(0xFF232528))
                Text("Credits & Source Code", style = MaterialTheme.typography.labelLarge, color = GeminiAccentBlue)
                Text("Kairo (/kaj/roo/) • Created by DaManMikey • Powered by on-device Gemma 4.", style = MaterialTheme.typography.bodySmall, color = GeminiTextSecondary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onOpenUrl("https://damanmikey.me/") }, modifier = Modifier.weight(1f)) { Text("damanmikey.me") }
                    OutlinedButton(onClick = { onOpenUrl("https://github.com/DaDevMikey/Kairo") }, modifier = Modifier.weight(1f)) { Text("Source Code") }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showSettingsScreen) {
        SettingsScreen(
            state = state,
            wakeWordEnabled = wakeWordEnabled,
            onBack = { showSettingsScreen = false },
            onUpdateSettings = onUpdateSettings,
            onToggleWakeWord = onToggleWakeWord,
            onOpenMemory = {
                showSettingsScreen = false
                showMemorySheet = true
            },
            onOpenConnections = {
                showSettingsScreen = false
                showConnectionsSheet = true
            },
            onOpenReleaseNotes = {
                showSettingsScreen = false
                onFetchReleaseNotes()
                showReleaseNotesSheet = true
            },
            onCheckUpdates = onCheckUpdates,
            onReplayTour = {
                showSettingsScreen = false
                introTourStep = 0
                showIntroTour = true
            },
            onOpenUrl = onOpenUrl
        )
    }
}

@Composable
private fun ConnectionPermRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    onAction: () -> Unit
) {
    Surface(color = GeminiPillBg, shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = GeminiTextPrimary)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = GeminiTextSecondary)
            }
            if (granted) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ActionSuccessGreen, modifier = Modifier.size(16.dp))
                    Text("Granted", color = ActionSuccessGreen, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                FilledTonalButton(onClick = onAction) { Text("Allow") }
            }
        }
    }
}

@Composable
private fun SetupSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = GeminiTextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = GeminiTextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DrawerMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = GeminiAccentBlue),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = GeminiTextPrimary, modifier = Modifier.size(20.dp))
        Text(text = label, color = GeminiTextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ConversationTurnItem(
    turn: ConversationTurn,
    sparkleModifier: Modifier = Modifier,
    onSpeak: () -> Unit = {},
    onRegenerate: () -> Unit = {},
    onSelectVersion: (Int) -> Unit = {},
    onThinkHarder: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    val responseBlur by animateDpAsState(
        targetValue = if (revealed) 0.dp else 8.dp,
        animationSpec = tween(320),
        label = "turn_blur"
    )

    Column(modifier = Modifier.fillMaxWidth().blur(responseBlur), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (turn.image != null) {
                Image(
                    bitmap = turn.image.asImageBitmap(),
                    contentDescription = "Uploaded image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(140.dp).clip(RoundedCornerShape(16.dp))
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(turn.prompt))
                        android.widget.Toast.makeText(context, "Prompt copied", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy prompt", tint = GeminiTextSecondary, modifier = Modifier.size(14.dp))
                }
                Surface(color = GeminiChipBg, shape = RoundedCornerShape(20.dp), modifier = Modifier.widthIn(max = 280.dp)) {
                    SelectionContainer {
                        Text(turn.prompt, color = GeminiTextPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            GemmASparkleIcon(size = 22.dp, isAnimating = turn.isStreaming, modifier = sparkleModifier.padding(top = 2.dp))
            Column(modifier = Modifier.weight(1f).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimatedVisibility(
                    visible = turn.actionSummary != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    turn.actionSummary?.let { summary ->
                        Surface(color = GeminiChipBg, shape = RoundedCornerShape(12.dp)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (turn.actionSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (turn.actionSuccess) ActionSuccessGreen else ActionErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(summary, color = GeminiTextPrimary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                var wasStreaming by remember { mutableStateOf(turn.isStreaming) }
                var shineTrigger by remember { mutableStateOf(false) }
                LaunchedEffect(turn.isStreaming) {
                    if (wasStreaming && !turn.isStreaming && turn.response.isNotEmpty()) {
                        shineTrigger = true
                    }
                    wasStreaming = turn.isStreaming
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.matchParentSize().clip(RoundedCornerShape(14.dp))) {
                        if (turn.isStreaming) {
                            StarDustParticles(modifier = Modifier.matchParentSize())
                        }
                        ResponseShineSweep(trigger = shineTrigger, modifier = Modifier.matchParentSize())
                    }
                    if (turn.isStreaming && turn.response.isEmpty()) {
                        ThinkingDotsIndicator()
                    } else {
                        RichResponseText(
                            text = if (turn.isStreaming) "${turn.response} ▌" else turn.response,
                            textColor = GeminiTextPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                if (turn.responseImage != null) {
                    Image(
                        bitmap = turn.responseImage.asImageBitmap(),
                        contentDescription = "Attached image from Kairo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }

                if (!turn.isStreaming && turn.response.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(turn.response))
                                android.widget.Toast.makeText(context, "Response copied", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy response", tint = GeminiTextSecondary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onSpeak, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Read aloud", tint = GeminiTextSecondary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, turn.response)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share via Kairo"))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share response", tint = GeminiTextSecondary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onRegenerate, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = GeminiTextSecondary, modifier = Modifier.size(16.dp))
                        }
                        if (turn.versions.size > 1) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { onSelectVersion(turn.selectedVersionIndex - 1) },
                                    enabled = turn.selectedVersionIndex > 0,
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text("‹", color = GeminiTextSecondary)
                                }
                                Text(
                                    text = "${turn.selectedVersionIndex + 1}/${turn.versions.size}",
                                    color = GeminiTextSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                TextButton(
                                    onClick = { onSelectVersion(turn.selectedVersionIndex + 1) },
                                    enabled = turn.selectedVersionIndex < turn.versions.lastIndex,
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text("›", color = GeminiTextSecondary)
                                }
                            }
                        }
                        IconButton(onClick = onThinkHarder, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = stringResource(R.string.think_harder_cd),
                                tint = GeminiAccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TwoLineMenuIcon() {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(4.dp)) {
        Box(modifier = Modifier.width(18.dp).height(2.dp).background(GeminiTextPrimary, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.width(18.dp).height(2.dp).background(GeminiTextPrimary, RoundedCornerShape(1.dp)))
    }
}

private fun openDefaultAssistantSettings(context: Context) {
    val voiceIntent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
    val fallbackIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
    context.startActivity(if (voiceIntent.resolveActivity(context.packageManager) != null) voiceIntent else fallbackIntent)
}

@Preview(showBackground = true, showSystemUi = true)
private fun extractVersionSection(body: String, tagName: String): String {
    val normalizedTag = tagName.trim().removePrefix("v")
    val lines = body.lines()
    val startIdx = lines.indexOfFirst { line ->
        val t = line.trim()
        t.startsWith("## ") && (t.contains("v$normalizedTag", ignoreCase = true) || t.contains(normalizedTag, ignoreCase = true))
    }
    if (startIdx == -1) return body
    val endIdx = ((startIdx + 1) until lines.size).firstOrNull { idx ->
        lines[idx].trim().startsWith("## ")
    } ?: lines.size
    return lines.subList(startIdx, endIdx).joinToString("\n").trim()
}

@Composable
fun MainScreenPreview() {
    GemmATheme(darkTheme = true) {
        MainScreenContent(
            state = GemmAHomeState(availability = ModelAvailability.AVAILABLE),
            isListening = false,
            partialTranscript = "",
            wakeWordEnabled = false,
            onSendPrompt = {},
            onNewChat = {},
            onSelectChat = {},
            onClearChats = {},
            onAddMemory = {},
            onDeleteMemory = {},
            onClearMemories = {},
            onToggleWakeWord = {},
            onRefreshStatus = {},
            onDownloadModel = {},
            onWarmupModel = {},
            onMicClick = {},
            onOpenAssistantSettings = {}
        )
    }
}
