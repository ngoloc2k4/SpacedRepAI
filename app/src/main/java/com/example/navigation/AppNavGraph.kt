package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.FlashcardApplication
import com.example.ui.ai.AiGeneratorScreen
import com.example.ui.ai.AiGeneratorViewModel
import com.example.ui.cards.CardListScreen
import com.example.ui.cards.CardListViewModel
import com.example.ui.decks.DeckListScreen
import com.example.ui.decks.DeckListViewModel
import com.example.ui.io.ImportExportScreen
import com.example.ui.io.ImportExportViewModel
import com.example.ui.review.ReviewScreen
import com.example.ui.review.ReviewViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.stats.StatsScreen
import com.example.ui.stats.StatsViewModel

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val app = context.applicationContext as FlashcardApplication
    val repository = app.repository
    val aiService = app.aiService
    val ttsManager = app.ttsManager
    val appSettingsManager = app.appSettingsManager
    val reminderManager = app.reminderManager

    NavHost(
        navController = navController,
        startDestination = Screen.Decks.route,
        modifier = modifier
    ) {
        // 1. Deck List & Daily Dashboard
        composable(Screen.Decks.route) {
            val viewModel: DeckListViewModel = viewModel(
                factory = DeckListViewModel.Factory(repository)
            )
            DeckListScreen(
                viewModel = viewModel,
                onNavigateToCards = { deckId ->
                    navController.navigate(Screen.Cards.createRoute(deckId))
                },
                onNavigateToReview = { deckId ->
                    navController.navigate(Screen.Review.createRoute(deckId))
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
                },
                onNavigateToAiGenerator = { deckId ->
                    navController.navigate(Screen.AiGenerator.createRoute(deckId))
                },
                onNavigateToImportExport = { deckId ->
                    navController.navigate(Screen.ImportExport.createRoute(deckId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // 2. Card Management for a Deck
        composable(
            route = Screen.Cards.route,
            arguments = listOf(
                navArgument("deckId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getLong("deckId") ?: 0L
            val viewModel: CardListViewModel = viewModel(
                key = "cards_$deckId",
                factory = CardListViewModel.Factory(deckId, repository)
            )
            CardListScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReview = { id ->
                    navController.navigate(Screen.Review.createRoute(id))
                },
                onNavigateToAiGenerator = { id ->
                    navController.navigate(Screen.AiGenerator.createRoute(id))
                }
            )
        }

        // 3. SM-2 Review Screen (with AI evaluator, mnemonic & explanation, TTS & Timer)
        composable(
            route = Screen.Review.route,
            arguments = listOf(
                navArgument("deckId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getLong("deckId") ?: 0L
            val viewModel: ReviewViewModel = viewModel(
                key = "review_$deckId",
                factory = ReviewViewModel.Factory(deckId, repository, aiService, ttsManager, appSettingsManager)
            )
            ReviewScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 4. Statistics Dashboard
        composable(Screen.Stats.route) {
            val viewModel: StatsViewModel = viewModel(
                factory = StatsViewModel.Factory(repository)
            )
            StatsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 5. AI Flashcard Generator
        composable(
            route = Screen.AiGenerator.route,
            arguments = listOf(
                navArgument("deckId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getLong("deckId") ?: 0L
            val viewModel: AiGeneratorViewModel = viewModel(
                key = "ai_gen_$deckId",
                factory = AiGeneratorViewModel.Factory(deckId, repository, aiService)
            )
            AiGeneratorScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDeckCards = { targetDeckId ->
                    navController.popBackStack()
                    navController.navigate(Screen.Cards.createRoute(targetDeckId))
                }
            )
        }

        // 6. Import / Export (JSON Backup & Restore)
        composable(
            route = Screen.ImportExport.route,
            arguments = listOf(
                navArgument("deckId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getLong("deckId") ?: 0L
            val viewModel: ImportExportViewModel = viewModel(
                key = "io_$deckId",
                factory = ImportExportViewModel.Factory(deckId, repository)
            )
            ImportExportScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 7. Settings & Preferences (Phase 9 & Multi-provider AI)
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(
                    appSettingsManager = appSettingsManager,
                    reminderManager = reminderManager,
                    ttsManager = ttsManager,
                    repository = repository,
                    aiService = aiService
                )
            )
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
