package com.example.ui.cards

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.CardEntity
import com.example.domain.srs.CardState
import com.example.ui.theme.StateLearningColor
import com.example.ui.theme.StateNewColor
import com.example.ui.theme.StateRelearningColor
import com.example.ui.theme.StateReviewColor
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreen(
    viewModel: CardListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReview: (Long) -> Unit,
    onNavigateToAiGenerator: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var cardToEdit by remember { mutableStateOf<CardEntity?>(null) }
    var cardToDelete by remember { mutableStateOf<CardEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = uiState.deck?.name ?: stringResource(R.string.cards_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("cards_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_decks)
                        )
                    }
                },
                actions = {
                    uiState.deck?.id?.let { deckId ->
                        IconButton(
                            onClick = { onNavigateToAiGenerator(deckId) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("top_ai_gen_button")
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = stringResource(R.string.nav_ai_generator),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (uiState.cards.isNotEmpty()) {
                        IconButton(
                            onClick = { uiState.deck?.id?.let { onNavigateToReview(it) } },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("top_study_button")
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.study),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_card_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_card_btn))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("loading_cards_indicator")
                    )
                }

                uiState.cards.isEmpty() && uiState.searchQuery.isBlank() -> {
                    EmptyCardsView(
                        onAddCardClick = { showAddDialog = true },
                        onAiGenerateClick = { uiState.deck?.id?.let { onNavigateToAiGenerator(it) } },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("card_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Search bar item
                        item(key = "search_header") {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cards_search_input"),
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.cards_search_hint),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotBlank()) {
                                        IconButton(
                                            onClick = { viewModel.onSearchQueryChanged("") },
                                            modifier = Modifier.testTag("cards_search_clear")
                                        ) {
                                            Icon(
                                                Icons.Filled.Clear,
                                                contentDescription = stringResource(R.string.action_close)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Summary & Counter header
                        if (uiState.totalCount > 0) {
                            item(key = "counter_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.cards_showing_paged,
                                            uiState.displayedCount,
                                            uiState.totalCount
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (uiState.cards.isEmpty() && uiState.searchQuery.isNotBlank()) {
                            item(key = "no_search_results") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.cards_no_search_results),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        items(uiState.cards, key = { it.id }) { card ->
                            CardRowItem(
                                card = card,
                                onEditClick = { cardToEdit = card },
                                onDeleteClick = { cardToDelete = card }
                            )
                        }

                        // Pagination Load More button
                        if (uiState.hasMoreCards) {
                            item(key = "load_more_footer") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("loading_more_indicator")
                                        )
                                    } else {
                                        OutlinedButton(
                                            onClick = { viewModel.loadNextPage() },
                                            modifier = Modifier
                                                .fillMaxWidth(0.7f)
                                                .testTag("load_more_cards_btn"),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Text(stringResource(R.string.cards_load_more))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CardEditorDialog(
            title = stringResource(R.string.add_card_dialog_title),
            initialFront = "",
            initialBack = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { front, back ->
                viewModel.addCard(front, back)
                showAddDialog = false
            }
        )
    }

    cardToEdit?.let { target ->
        CardEditorDialog(
            title = stringResource(R.string.edit_card_dialog_title),
            initialFront = target.front,
            initialBack = target.back,
            onDismiss = { cardToEdit = null },
            onConfirm = { front, back ->
                viewModel.updateCard(target, front, back)
                cardToEdit = null
            }
        )
    }

    cardToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { cardToDelete = null },
            title = { Text(stringResource(R.string.delete_card_dialog_title)) },
            text = { Text(stringResource(R.string.delete_card_confirm_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCard(target.id)
                        cardToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_card_button")
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { cardToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun CardRowItem(
    card: CardEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_item_${card.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CardStateBadge(state = card.state)

                Row {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("edit_card_button_${card.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.edit_card_dialog_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_card_button_${card.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete_card_dialog_title),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Q: ${card.front}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "A: ${card.back}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // SRS Metadata summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stringResource(R.string.card_meta_interval)}: ${card.intervalDays}d",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${stringResource(R.string.card_meta_reps)}: ${card.repetitions}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${stringResource(R.string.card_meta_ease)}: " + String.format(Locale.US, "%.2f", card.easeFactor),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CardStateBadge(state: CardState) {
    val color = when (state) {
        CardState.NEW -> StateNewColor
        CardState.LEARNING -> StateLearningColor
        CardState.REVIEW -> StateReviewColor
        CardState.RELEARNING -> StateRelearningColor
    }
    val label = when (state) {
        CardState.NEW -> stringResource(R.string.state_new)
        CardState.LEARNING -> stringResource(R.string.state_learning)
        CardState.REVIEW -> stringResource(R.string.state_review)
        CardState.RELEARNING -> stringResource(R.string.state_relearning)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun EmptyCardsView(
    onAddCardClick: () -> Unit,
    onAiGenerateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(32.dp)
            .testTag("empty_cards_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Style,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_cards_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_cards_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onAddCardClick,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
                .testTag("empty_add_card_button")
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_card_btn))
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = onAiGenerateClick,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
                .testTag("empty_ai_gen_button")
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.nav_ai_generator))
        }
    }
}

@Composable
fun CardEditorDialog(
    title: String,
    initialFront: String,
    initialBack: String,
    onDismiss: () -> Unit,
    onConfirm: (front: String, back: String) -> Unit
) {
    var front by remember { mutableStateOf(initialFront) }
    var back by remember { mutableStateOf(initialBack) }
    var frontError by remember { mutableStateOf(false) }
    var backError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = front,
                    onValueChange = {
                        front = it
                        if (it.isNotBlank()) frontError = false
                    },
                    label = { Text(stringResource(R.string.card_front_label)) },
                    placeholder = { Text(stringResource(R.string.card_front_placeholder)) },
                    isError = frontError,
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_front_input")
                )

                OutlinedTextField(
                    value = back,
                    onValueChange = {
                        back = it
                        if (it.isNotBlank()) backError = false
                    },
                    label = { Text(stringResource(R.string.card_back_label)) },
                    placeholder = { Text(stringResource(R.string.card_back_placeholder)) },
                    isError = backError,
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_back_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var hasError = false
                    if (front.isBlank()) {
                        frontError = true
                        hasError = true
                    }
                    if (back.isBlank()) {
                        backError = true
                        hasError = true
                    }
                    if (!hasError) {
                        onConfirm(front, back)
                    }
                },
                modifier = Modifier.testTag("save_card_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_card_button")
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
