package com.example.ui.ai

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGeneratorScreen(
    viewModel: AiGeneratorViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDeckCards: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var deckDropdownExpanded by remember { mutableStateOf(false) }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onImageSelected(context, uri)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.ai_gen_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("ai_gen_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                .testTag("ai_generator_content"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Selector Tabs (Topic vs Image)
            item(key = "input_mode_tabs") {
                PrimaryTabRow(
                    selectedTabIndex = if (uiState.inputMode == AiInputMode.TEXT) 0 else 1,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = uiState.inputMode == AiInputMode.TEXT,
                        onClick = { viewModel.setInputMode(AiInputMode.TEXT) },
                        text = { Text(stringResource(R.string.ai_gen_mode_topic)) },
                        icon = { Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.testTag("tab_mode_topic")
                    )
                    Tab(
                        selected = uiState.inputMode == AiInputMode.IMAGE,
                        onClick = { viewModel.setInputMode(AiInputMode.IMAGE) },
                        text = { Text(stringResource(R.string.ai_gen_mode_image)) },
                        icon = { Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.testTag("tab_mode_image")
                    )
                }
            }

            // Input Form Card
            item(key = "generator_input_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Destination Selector Section
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.ai_gen_dest_label),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = uiState.saveDestination == SaveDestination.CURRENT_DECK,
                                    onClick = { viewModel.setSaveDestination(SaveDestination.CURRENT_DECK) },
                                    label = { Text(stringResource(R.string.ai_gen_dest_current)) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                    modifier = Modifier.testTag("chip_dest_current")
                                )
                                FilterChip(
                                    selected = uiState.saveDestination == SaveDestination.NEW_DECK,
                                    onClick = { viewModel.setSaveDestination(SaveDestination.NEW_DECK) },
                                    label = { Text(stringResource(R.string.ai_gen_dest_new)) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                    modifier = Modifier.testTag("chip_dest_new")
                                )
                            }

                            // If Current Deck: show deck dropdown
                            if (uiState.saveDestination == SaveDestination.CURRENT_DECK && uiState.decks.isNotEmpty()) {
                                val selectedDeck = uiState.decks.find { it.id == uiState.selectedDeckId } ?: uiState.decks.first()
                                ExposedDropdownMenuBox(
                                    expanded = deckDropdownExpanded,
                                    onExpandedChange = { deckDropdownExpanded = !deckDropdownExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = stringResource(R.string.ai_gen_select_deck, selectedDeck.name),
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(stringResource(R.string.nav_decks)) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deckDropdownExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .testTag("target_deck_selector")
                                    )
                                    ExposedDropdownMenu(
                                        expanded = deckDropdownExpanded,
                                        onDismissRequest = { deckDropdownExpanded = false }
                                    ) {
                                        uiState.decks.forEach { deck ->
                                            DropdownMenuItem(
                                                text = { Text(deck.name) },
                                                onClick = {
                                                    viewModel.onDeckSelected(deck.id)
                                                    deckDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // If New Deck: show new deck name & desc inputs
                            if (uiState.saveDestination == SaveDestination.NEW_DECK) {
                                OutlinedTextField(
                                    value = uiState.newDeckName,
                                    onValueChange = { viewModel.onNewDeckNameChanged(it) },
                                    label = { Text(stringResource(R.string.ai_gen_new_deck_name)) },
                                    placeholder = { Text(stringResource(R.string.ai_gen_new_deck_name_hint)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_new_deck_name")
                                )
                                OutlinedTextField(
                                    value = uiState.newDeckDescription,
                                    onValueChange = { viewModel.onNewDeckDescriptionChanged(it) },
                                    label = { Text(stringResource(R.string.ai_gen_new_deck_desc)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_new_deck_desc")
                                )
                            }
                        }

                        // Content input based on mode
                        if (uiState.inputMode == AiInputMode.TEXT) {
                            // TEXT Mode Topic input
                            OutlinedTextField(
                                value = uiState.topic,
                                onValueChange = { viewModel.onTopicChanged(it) },
                                label = { Text(stringResource(R.string.ai_gen_topic_label)) },
                                placeholder = { Text(stringResource(R.string.ai_gen_topic_hint)) },
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_topic_input")
                            )
                        } else {
                            // IMAGE Mode: Upload & Vision
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (uiState.selectedImageUri != null) {
                                    // Image Preview
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    ) {
                                        AsyncImage(
                                            model = uiState.selectedImageUri,
                                            contentDescription = "Selected Notes Image",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    // Controls: Change Image / Remove
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("btn_change_image")
                                        ) {
                                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.ai_gen_image_change_btn))
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.clearSelectedImage() },
                                            modifier = Modifier
                                                .height(44.dp)
                                                .testTag("btn_remove_image")
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(stringResource(R.string.ai_gen_image_remove_btn))
                                        }
                                    }
                                } else {
                                    // Empty state: pick image card
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                            .testTag("btn_pick_image_card"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.AddPhotoAlternate,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(44.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.ai_gen_image_title),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = stringResource(R.string.ai_gen_image_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Button(
                                                onClick = {
                                                    photoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Text(stringResource(R.string.ai_gen_image_pick_btn))
                                            }
                                        }
                                    }
                                }

                                // Extra Prompt Hint
                                OutlinedTextField(
                                    value = uiState.imagePromptHint,
                                    onValueChange = { viewModel.onImagePromptHintChanged(it) },
                                    label = { Text(stringResource(R.string.ai_gen_image_hint_label)) },
                                    placeholder = { Text(stringResource(R.string.ai_gen_image_hint_placeholder)) },
                                    maxLines = 3,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_image_hint")
                                )
                            }
                        }

                        // Count slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.ai_gen_count_label),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${uiState.cardCount}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = uiState.cardCount.toFloat(),
                                onValueChange = { viewModel.onCardCountChanged(it.toInt()) },
                                valueRange = 3f..15f,
                                steps = 11,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_count_slider")
                            )
                        }

                        if (uiState.errorMessage != null) {
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // Generate Button
                        Button(
                            onClick = { viewModel.generateCards() },
                            enabled = !uiState.isGenerating && !uiState.isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_cards_button")
                        ) {
                            if (uiState.isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    if (uiState.inputMode == AiInputMode.IMAGE)
                                        stringResource(R.string.ai_gen_loading_image)
                                    else
                                        stringResource(R.string.ai_gen_loading)
                                )
                            } else {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (uiState.inputMode == AiInputMode.IMAGE)
                                        stringResource(R.string.ai_gen_btn_generate_image)
                                    else
                                        stringResource(R.string.ai_gen_btn_generate)
                                )
                            }
                        }
                    }
                }
            }

            // Preview Section (when cards have been generated)
            if (uiState.generatedCards.isNotEmpty()) {
                item(key = "preview_header") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.ai_gen_preview_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row {
                                OutlinedButton(
                                    onClick = { viewModel.selectAll(true) },
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(stringResource(R.string.ai_gen_select_all), style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = { viewModel.selectAll(false) },
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(stringResource(R.string.ai_gen_deselect_all), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.ai_gen_preview_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(uiState.generatedCards, key = { it.id }) { card ->
                    GeneratedCardPreviewItem(
                        card = card,
                        onToggle = { viewModel.toggleCardSelection(card.id) },
                        onContentChange = { f, b -> viewModel.updateCardContent(card.id, f, b) }
                    )
                }

                item(key = "save_action_button") {
                    val selectedCount = uiState.generatedCards.count { it.isSelected }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveSelectedCards() },
                            enabled = selectedCount > 0 && !uiState.isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_selected_cards_button")
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saving...")
                            } else {
                                Icon(Icons.Filled.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.ai_gen_save_btn, selectedCount))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.ai_gen_disclaimer),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Success Dialog when cards saved
    if (uiState.successSavedCount != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccessDialog() },
            icon = {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Cards Saved Successfully",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.ai_gen_success_details,
                        uiState.successSavedCount ?: 0,
                        uiState.savedToDeckName ?: "Deck"
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = uiState.savedToDeckId ?: 0L
                        viewModel.dismissSuccessDialog()
                        if (targetId != 0L) {
                            onNavigateToDeckCards(targetId)
                        }
                    },
                    modifier = Modifier.testTag("btn_view_saved_deck")
                ) {
                    Text(stringResource(R.string.ai_gen_view_deck_btn))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissSuccessDialog() }
                ) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}

@Composable
fun GeneratedCardPreviewItem(
    card: EditableGeneratedCard,
    onToggle: () -> Unit,
    onContentChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("preview_card_${card.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (card.isSelected) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (card.isSelected) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = card.isSelected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("checkbox_${card.id}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = card.front,
                    onValueChange = { onContentChange(it, card.back) },
                    label = { Text(stringResource(R.string.card_front_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = card.back,
                    onValueChange = { onContentChange(card.front, it) },
                    label = { Text(stringResource(R.string.card_back_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }
        }
    }
}
