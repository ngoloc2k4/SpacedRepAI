package com.example.ui.review

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.CardEntity
import com.example.domain.ai.AnswerEvaluation
import com.example.domain.ai.EvaluationStatus
import com.example.domain.srs.IntervalPreview
import com.example.domain.srs.ReviewRating
import com.example.ui.cards.CardStateBadge
import com.example.ui.theme.SrsAgain
import com.example.ui.theme.SrsAgainContainer
import com.example.ui.theme.SrsEasy
import com.example.ui.theme.SrsEasyContainer
import com.example.ui.theme.SrsGood
import com.example.ui.theme.SrsGoodContainer
import com.example.ui.theme.SrsHard
import com.example.ui.theme.SrsHardContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.deck?.name ?: stringResource(R.string.review_screen_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!uiState.isCompleted && uiState.totalQueueSize > 0) {
                            Text(
                                text = "${uiState.currentIndex + 1} / ${uiState.totalQueueSize}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("review_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_decks)
                        )
                    }
                },
                actions = {
                    if (!uiState.isCompleted && uiState.currentCard != null) {
                        IconButton(
                            onClick = { viewModel.toggleTypeAnswerMode() },
                            modifier = Modifier.size(48.dp).testTag("toggle_type_answer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EditNote,
                                contentDescription = stringResource(R.string.type_answer_toggle),
                                tint = if (uiState.typeAnswerMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!uiState.isCompleted && uiState.totalQueueSize > 0) {
                LinearProgressIndicator(
                    progress = { uiState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .testTag("review_progress_bar"),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .testTag("review_loading_indicator")
                        )
                    }

                    uiState.isCompleted || uiState.currentCard == null -> {
                        ReviewCompletedView(
                            uiState = uiState,
                            onFinishClick = onNavigateBack,
                            onRestartClick = { viewModel.restartSession() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {
                        val card = uiState.currentCard!!
                        ActiveStudyCardView(
                            card = card,
                            isFlipped = uiState.isFlipped,
                            typeAnswerMode = uiState.typeAnswerMode,
                            userAnswer = uiState.userAnswer,
                            isAiEvaluating = uiState.isAiEvaluating,
                            aiEvaluation = uiState.aiEvaluation,
                            intervalPreviews = uiState.intervalPreviews,
                            isSpeaking = uiState.isSpeaking,
                            timerTotalSeconds = uiState.timerTotalSeconds,
                            timerSecondsRemaining = uiState.timerSecondsRemaining,
                            onSpeakFront = { viewModel.speakCurrentFront() },
                            onSpeakBack = { viewModel.speakCurrentBack() },
                            onStopSpeaking = { viewModel.stopSpeaking() },
                            onUserAnswerChanged = { viewModel.onUserAnswerChanged(it) },
                            onEvaluateWithAi = { viewModel.evaluateAnswerWithAi() },
                            onExplainWithAi = { viewModel.requestAiExplanation() },
                            onMnemonicWithAi = { viewModel.requestAiMnemonic() },
                            onFlip = {
                                if (uiState.hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                viewModel.flipCard()
                            },
                            onRate = { rating ->
                                if (uiState.hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                viewModel.rateCard(rating)
                            }
                        )
                    }
                }
            }
        }
    }

    // AI Explanation / Mnemonic Modal Dialog
    if (uiState.aiDialogTitle != null && uiState.aiDialogContent != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAiDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = uiState.aiDialogTitle ?: "")
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = uiState.aiDialogContent ?: "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAiDialog() }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}

@Composable
fun ActiveStudyCardView(
    card: CardEntity,
    isFlipped: Boolean,
    typeAnswerMode: Boolean,
    userAnswer: String,
    isAiEvaluating: Boolean,
    aiEvaluation: AnswerEvaluation?,
    intervalPreviews: Map<ReviewRating, IntervalPreview>,
    isSpeaking: Boolean = false,
    timerTotalSeconds: Int = 0,
    timerSecondsRemaining: Int = 0,
    onSpeakFront: () -> Unit = {},
    onSpeakBack: () -> Unit = {},
    onStopSpeaking: () -> Unit = {},
    onUserAnswerChanged: (String) -> Unit,
    onEvaluateWithAi: () -> Unit,
    onExplainWithAi: () -> Unit,
    onMnemonicWithAi: () -> Unit,
    onFlip: () -> Unit,
    onRate: (ReviewRating) -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "card_flip_rotation"
    )

    val againInterval = intervalPreviews[ReviewRating.AGAIN]?.intervalText ?: "10m"
    val hardInterval = intervalPreviews[ReviewRating.HARD]?.intervalText ?: "1d"
    val goodInterval = intervalPreviews[ReviewRating.GOOD]?.intervalText ?: "6d"
    val easyInterval = intervalPreviews[ReviewRating.EASY]?.intervalText ?: "15d"

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Countdown timer bar if enabled
        if (timerTotalSeconds > 0 && !isFlipped) {
            val progressFraction = timerSecondsRemaining.toFloat() / timerTotalSeconds.toFloat()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (timerSecondsRemaining <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (timerSecondsRemaining > 0) {
                            stringResource(R.string.card_timer_remaining, timerSecondsRemaining)
                        } else {
                            stringResource(R.string.card_timer_time_up)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (timerSecondsRemaining <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .width(100.dp)
                        .height(6.dp),
                    color = if (timerSecondsRemaining <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Flashcard Body (Tappable to flip)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onFlip
                )
                .testTag("flashcard_body"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp)
            ) {
                if (rotation <= 90f) {
                    // Front Face
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "QUESTION",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            if (isSpeaking) onStopSpeaking() else onSpeakFront()
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("tts_front_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.VolumeUp,
                                            contentDescription = stringResource(R.string.tts_speak_front),
                                            tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                CardStateBadge(state = card.state)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = card.front,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("card_front_text")
                            )

                            // Type answer area if enabled
                            if (typeAnswerMode) {
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = userAnswer,
                                    onValueChange = onUserAnswerChanged,
                                    label = { Text(stringResource(R.string.type_answer_placeholder)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("user_answer_input"),
                                    maxLines = 3
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onEvaluateWithAi,
                                    enabled = userAnswer.isNotBlank() && !isAiEvaluating,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("eval_with_ai_button")
                                ) {
                                    if (isAiEvaluating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.ai_evaluating))
                                    } else {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.evaluate_with_ai))
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.tap_to_reveal),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    // Back Face (Mirrored 180deg so text displays naturally)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f }
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "ANSWER",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            if (isSpeaking) onStopSpeaking() else onSpeakBack()
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("tts_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.VolumeUp,
                                            contentDescription = stringResource(R.string.tts_speak_back),
                                            tint = if (isSpeaking) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                CardStateBadge(state = card.state)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = card.front,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = card.back,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("card_back_text")
                            )

                            // If AI evaluation was run
                            if (aiEvaluation != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = when (aiEvaluation.status) {
                                        EvaluationStatus.CORRECT -> SrsGoodContainer
                                        EvaluationStatus.PARTIALLY_CORRECT -> SrsHardContainer
                                        EvaluationStatus.INCORRECT -> SrsAgainContainer
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.AutoAwesome,
                                                contentDescription = null,
                                                tint = when (aiEvaluation.status) {
                                                    EvaluationStatus.CORRECT -> SrsGood
                                                    EvaluationStatus.PARTIALLY_CORRECT -> SrsHard
                                                    EvaluationStatus.INCORRECT -> SrsAgain
                                                },
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = when (aiEvaluation.status) {
                                                    EvaluationStatus.CORRECT -> stringResource(R.string.ai_eval_result_correct)
                                                    EvaluationStatus.PARTIALLY_CORRECT -> stringResource(R.string.ai_eval_result_partial)
                                                    EvaluationStatus.INCORRECT -> stringResource(R.string.ai_eval_result_incorrect)
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = aiEvaluation.feedback,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            // AI helper chips (Explain & Mnemonic)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = onExplainWithAi,
                                    modifier = Modifier.weight(1f).height(40.dp).testTag("btn_ai_explain")
                                ) {
                                    Icon(Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_ai_explain), style = MaterialTheme.typography.labelSmall)
                                }
                                FilledTonalButton(
                                    onClick = onMnemonicWithAi,
                                    modifier = Modifier.weight(1f).height(40.dp).testTag("btn_ai_mnemonic")
                                ) {
                                    Icon(Icons.Filled.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_ai_mnemonic), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.tap_to_hide),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Area: Flip button OR 4 Rating buttons
        if (!isFlipped) {
            Button(
                onClick = onFlip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("reveal_answer_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.TouchApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.tap_to_reveal), style = MaterialTheme.typography.titleSmall)
            }
        } else {
            // 4 SM-2 Rating Buttons: Again, Hard, Good, Easy
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rating_buttons_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RatingButton(
                    label = stringResource(R.string.rating_again),
                    sub = againInterval,
                    containerColor = SrsAgainContainer,
                    contentColor = SrsAgain,
                    onClick = { onRate(ReviewRating.AGAIN) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rate_again_button")
                )
                RatingButton(
                    label = stringResource(R.string.rating_hard),
                    sub = hardInterval,
                    containerColor = SrsHardContainer,
                    contentColor = SrsHard,
                    onClick = { onRate(ReviewRating.HARD) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rate_hard_button")
                )
                RatingButton(
                    label = stringResource(R.string.rating_good),
                    sub = goodInterval,
                    containerColor = SrsGoodContainer,
                    contentColor = SrsGood,
                    onClick = { onRate(ReviewRating.GOOD) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rate_good_button")
                )
                RatingButton(
                    label = stringResource(R.string.rating_easy),
                    sub = easyInterval,
                    containerColor = SrsEasyContainer,
                    contentColor = SrsEasy,
                    onClick = { onRate(ReviewRating.EASY) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rate_easy_button")
                )
            }
        }
    }
}

@Composable
fun RatingButton(
    label: String,
    sub: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ReviewCompletedView(
    uiState: ReviewUiState,
    onFinishClick: () -> Unit,
    onRestartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(24.dp)
            .testTag("review_completed_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.session_completed_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.session_completed_msg, uiState.reviewedCount, uiState.formattedDuration),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Review ratings recap
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    RatingRecapItem(stringResource(R.string.recap_reviewed), "${uiState.reviewedCount}", MaterialTheme.colorScheme.primary)
                    RatingRecapItem(stringResource(R.string.recap_time), uiState.formattedDuration, MaterialTheme.colorScheme.secondary)
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    RatingRecapItem(stringResource(R.string.rating_again), "${uiState.againCount}", SrsAgain)
                    RatingRecapItem(stringResource(R.string.rating_hard), "${uiState.hardCount}", SrsHard)
                    RatingRecapItem(stringResource(R.string.rating_good), "${uiState.goodCount}", SrsGood)
                    RatingRecapItem(stringResource(R.string.rating_easy), "${uiState.easyCount}", SrsEasy)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onFinishClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("finish_session_button")
        ) {
            Text(stringResource(R.string.back_to_decks))
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onRestartClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("restart_session_button")
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.review_again))
        }
    }
}

@Composable
fun RatingRecapItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
