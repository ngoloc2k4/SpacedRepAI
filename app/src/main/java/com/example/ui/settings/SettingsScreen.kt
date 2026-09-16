package com.example.ui.settings

import android.Manifest
import android.os.Build
import android.widget.TimePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.preferences.AiProvider
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showLeechCardsDialog by remember { mutableStateOf(false) }
    val testSentMsg = stringResource(R.string.setting_reminder_test_sent)

    // Notification permission launcher for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.updateReminder(true, uiState.settings.reminderHour, uiState.settings.reminderMinute)
        }
    }

    LaunchedEffect(uiState.testNotificationSent) {
        if (uiState.testNotificationSent) {
            snackbarHostState.showSnackbar(testSentMsg)
            viewModel.clearNotificationSentFlag()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_decks)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Study Limits & Card Timer
            item {
                SettingsSectionCard(
                    title = stringResource(R.string.settings_study_section),
                    icon = Icons.Filled.Timer
                ) {
                    // Daily New Cards Limit
                    Text(
                        text = stringResource(R.string.setting_new_cards_limit),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.setting_new_cards_limit_desc, uiState.settings.dailyNewCardsLimit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 20, 30, 50).forEach { count ->
                            FilterChip(
                                selected = uiState.settings.dailyNewCardsLimit == count,
                                onClick = { viewModel.updateDailyNewCardsLimit(count) },
                                label = { Text("$count") },
                                modifier = Modifier.testTag("limit_new_$count")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Daily Review Cards Limit
                    Text(
                        text = stringResource(R.string.setting_review_cards_limit),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.setting_review_cards_limit_desc, uiState.settings.dailyReviewCardsLimit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(20, 50, 100, 200).forEach { count ->
                            FilterChip(
                                selected = uiState.settings.dailyReviewCardsLimit == count,
                                onClick = { viewModel.updateDailyReviewCardsLimit(count) },
                                label = { Text("$count") },
                                modifier = Modifier.testTag("limit_review_$count")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Card Timer
                    Text(
                        text = stringResource(R.string.setting_timer),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.setting_timer_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to stringResource(R.string.setting_timer_disabled), 15 to "15s", 30 to "30s", 60 to "60s").forEach { (sec, label) ->
                            FilterChip(
                                selected = uiState.settings.cardTimerSeconds == sec,
                                onClick = { viewModel.updateCardTimer(sec) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("timer_chip_$sec")
                            )
                        }
                    }
                }
            }

            // 2. Audio & Speech (TTS)
            item {
                SettingsSectionCard(
                    title = stringResource(R.string.audio_settings_title),
                    icon = Icons.AutoMirrored.Filled.VolumeUp
                ) {
                    // Auto-play front
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setting_auto_speak_front),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.setting_auto_speak_front_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.settings.autoSpeakFront,
                            onCheckedChange = { viewModel.updateAutoSpeakFront(it) },
                            modifier = Modifier.testTag("switch_auto_speak_front")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Auto-play back
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setting_auto_speak_back),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.setting_auto_speak_back_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.settings.autoSpeakBack,
                            onCheckedChange = { viewModel.updateAutoSpeakBack(it) },
                            modifier = Modifier.testTag("switch_auto_speak_back")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Speech Rate Slider
                    Text(
                        text = stringResource(R.string.setting_speech_rate, uiState.settings.speechRate),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = uiState.settings.speechRate,
                        onValueChange = { viewModel.updateSpeechRate(it) },
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        modifier = Modifier.testTag("slider_speech_rate")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Speech Pitch Slider
                    Text(
                        text = stringResource(R.string.setting_speech_pitch, uiState.settings.speechPitch),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = uiState.settings.speechPitch,
                        onValueChange = { viewModel.updateSpeechPitch(it) },
                        valueRange = 0.5f..1.5f,
                        steps = 9,
                        modifier = Modifier.testTag("slider_speech_pitch")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Test Voice button
                    val testText = stringResource(R.string.setting_tts_test_text)
                    OutlinedButton(
                        onClick = { viewModel.testTts(testText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("test_voice_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.setting_tts_test_btn))
                    }
                }
            }

            // 3. Haptic Feedback
            item {
                SettingsSectionCard(
                    title = stringResource(R.string.setting_haptic_title),
                    icon = Icons.Filled.Vibration
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setting_haptic_toggle),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.setting_haptic_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.settings.hapticEnabled,
                            onCheckedChange = { viewModel.updateHaptic(it) },
                            modifier = Modifier.testTag("switch_haptic_feedback")
                        )
                    }
                }
            }

            // 4. Daily Study Notifications
            item {
                SettingsSectionCard(
                    title = stringResource(R.string.settings_reminder_section),
                    icon = Icons.Filled.Notifications
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setting_reminder_enable),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.setting_reminder_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.settings.reminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.updateReminder(enabled, uiState.settings.reminderHour, uiState.settings.reminderMinute)
                                }
                            },
                            modifier = Modifier.testTag("switch_daily_reminder")
                        )
                    }

                    if (uiState.settings.reminderEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.setting_reminder_time),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(
                                        R.string.setting_reminder_time_desc,
                                        uiState.settings.reminderHour,
                                        uiState.settings.reminderMinute
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = { showTimePickerDialog = true },
                                modifier = Modifier.testTag("pick_reminder_time_button")
                            ) {
                                Icon(Icons.Filled.Alarm, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(String.format(Locale.US, "%02d:%02d", uiState.settings.reminderHour, uiState.settings.reminderMinute))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.sendTestReminder() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("send_test_notification_btn")
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.setting_reminder_test_btn))
                        }
                    }
                }
            }

            // 5. Spaced Repetition & Leech Cards
            item {
                SettingsSectionCard(
                    title = stringResource(R.string.settings_srs_section),
                    icon = Icons.Filled.Psychology
                ) {
                    Text(
                        text = stringResource(R.string.setting_leech_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.setting_leech_desc, uiState.leechCardsCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showLeechCardsDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("view_leech_cards_btn")
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.setting_leech_btn))
                    }
                }
            }

            // 6. AI Provider & Custom Models (Multi-Provider Support)
            item {
                var apiKeyVisible by remember { mutableStateOf(false) }

                SettingsSectionCard(
                    title = stringResource(R.string.settings_ai_section),
                    icon = Icons.Filled.AutoAwesome
                ) {
                    Text(
                        text = stringResource(R.string.setting_ai_provider),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.setting_ai_provider_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Provider Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = uiState.settings.aiProvider == AiProvider.GEMINI,
                                    onClick = { viewModel.updateAiProvider(AiProvider.GEMINI) },
                                    label = { Text("Gemini") },
                                    modifier = Modifier.testTag("chip_provider_gemini")
                                )
                                FilterChip(
                                    selected = uiState.settings.aiProvider == AiProvider.OPENROUTER,
                                    onClick = { viewModel.updateAiProvider(AiProvider.OPENROUTER) },
                                    label = { Text("OpenRouter") },
                                    modifier = Modifier.testTag("chip_provider_openrouter")
                                )
                                FilterChip(
                                    selected = uiState.settings.aiProvider == AiProvider.GROQ,
                                    onClick = { viewModel.updateAiProvider(AiProvider.GROQ) },
                                    label = { Text("Groq") },
                                    modifier = Modifier.testTag("chip_provider_groq")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = uiState.settings.aiProvider == AiProvider.NVIDIA,
                                    onClick = { viewModel.updateAiProvider(AiProvider.NVIDIA) },
                                    label = { Text("NVIDIA NIM") },
                                    modifier = Modifier.testTag("chip_provider_nvidia")
                                )
                                FilterChip(
                                    selected = uiState.settings.aiProvider == AiProvider.CUSTOM,
                                    onClick = { viewModel.updateAiProvider(AiProvider.CUSTOM) },
                                    label = { Text("Custom / OpenAI") },
                                    modifier = Modifier.testTag("chip_provider_custom")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // HTTP Method Chips
                    Text(
                        text = stringResource(R.string.setting_ai_method),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("POST", "GET", "PUT").forEach { method ->
                            FilterChip(
                                selected = uiState.settings.getEffectiveHttpMethod() == method,
                                onClick = { viewModel.updateAiHttpMethod(method) },
                                label = { Text(method) },
                                modifier = Modifier.testTag("chip_http_method_$method")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // API Endpoint URL
                    OutlinedTextField(
                        value = uiState.settings.aiEndpoint,
                        onValueChange = { viewModel.updateAiEndpoint(it) },
                        label = { Text(stringResource(R.string.setting_ai_endpoint)) },
                        placeholder = { Text(uiState.settings.aiProvider.defaultEndpoint) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_ai_endpoint")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // AI Model Name
                    OutlinedTextField(
                        value = uiState.settings.aiModel,
                        onValueChange = { viewModel.updateAiModel(it) },
                        label = { Text(stringResource(R.string.setting_ai_model)) },
                        placeholder = { Text(uiState.settings.aiProvider.defaultModel) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_ai_model")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // API Key Field
                    OutlinedTextField(
                        value = uiState.settings.aiApiKey,
                        onValueChange = { viewModel.updateAiApiKey(it) },
                        label = { Text(stringResource(R.string.setting_ai_key)) },
                        placeholder = { Text(stringResource(R.string.setting_ai_key_hint)) },
                        supportingText = { Text(stringResource(R.string.setting_ai_key_help)) },
                        singleLine = true,
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    imageVector = if (apiKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (apiKeyVisible) "Hide key" else "Show key"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_ai_api_key")
                    )

                    // Hardware KeyStore Security Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp)
                            .testTag("keystore_security_badge")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.security_keystore_secured),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Custom HTTP Headers (Optional)
                    OutlinedTextField(
                        value = uiState.settings.aiCustomHeaders,
                        onValueChange = { viewModel.updateAiCustomHeaders(it) },
                        label = { Text(stringResource(R.string.setting_ai_headers)) },
                        placeholder = { Text(stringResource(R.string.setting_ai_headers_hint)) },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_ai_headers")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons: Reset Defaults & Test Connection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.resetAiDefaults() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_reset_ai_defaults")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.setting_ai_reset_defaults), maxLines = 1)
                        }

                        Button(
                            onClick = { viewModel.testAiConnection() },
                            enabled = !uiState.isTestingAi,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("btn_test_ai_connection")
                        ) {
                            if (uiState.isTestingAi) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.setting_ai_testing))
                            } else {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.setting_ai_test_btn))
                            }
                        }
                    }
                }
            }
        }
    }

    // Time Picker Dialog
    if (showTimePickerDialog) {
        var selectedHour by remember { mutableStateOf(uiState.settings.reminderHour) }
        var selectedMinute by remember { mutableStateOf(uiState.settings.reminderMinute) }

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = { Text(stringResource(R.string.setting_reminder_time)) },
            text = {
                AndroidView(
                    factory = { ctx ->
                        TimePicker(ctx).apply {
                            setIs24HourView(true)
                            hour = selectedHour
                            minute = selectedMinute
                            setOnTimeChangedListener { _, h, m ->
                                selectedHour = h
                                selectedMinute = m
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateReminder(true, selectedHour, selectedMinute)
                        showTimePickerDialog = false
                    },
                    modifier = Modifier.testTag("confirm_time_picker")
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Leech Cards Dialog
    if (showLeechCardsDialog) {
        AlertDialog(
            onDismissRequest = { showLeechCardsDialog = false },
            title = { Text(stringResource(R.string.leech_screen_title)) },
            text = {
                if (uiState.leechCards.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.leech_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.leech_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.leechCards.size) { idx ->
                            val card = uiState.leechCards[idx]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = card.front,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = card.back,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "State: ${card.state} | Ease: ${String.format(Locale.US, "%.2f", card.easeFactor)} | Reps: ${card.repetitions} | Interval: ${card.intervalDays}d",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLeechCardsDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    // AI Connection Test Result Dialog
    if (uiState.aiTestSuccess != null) {
        val isSuccess = uiState.aiTestSuccess == true
        AlertDialog(
            onDismissRequest = { viewModel.dismissAiTestDialog() },
            icon = {
                Icon(
                    imageVector = if (isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error,
                    contentDescription = null,
                    tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isSuccess) stringResource(R.string.setting_ai_test_success) else stringResource(R.string.setting_ai_test_failed),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = uiState.aiTestMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissAiTestDialog() },
                    modifier = Modifier.testTag("dismiss_ai_test_dialog")
                ) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}
