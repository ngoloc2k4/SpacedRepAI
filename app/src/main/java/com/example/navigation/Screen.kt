package com.example.navigation

sealed class Screen(val route: String) {
    data object Decks : Screen("decks")
    data object Cards : Screen("cards/{deckId}") {
        fun createRoute(deckId: Long) = "cards/$deckId"
    }
    data object Review : Screen("review/{deckId}") {
        fun createRoute(deckId: Long) = "review/$deckId"
    }
    data object Stats : Screen("stats")
    data object AiGenerator : Screen("ai_generator/{deckId}") {
        fun createRoute(deckId: Long = 0L) = "ai_generator/$deckId"
    }
    data object ImportExport : Screen("import_export/{deckId}") {
        fun createRoute(deckId: Long = 0L) = "import_export/$deckId"
    }
    data object Settings : Screen("settings")
}
