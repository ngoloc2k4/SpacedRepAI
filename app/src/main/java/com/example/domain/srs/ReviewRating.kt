package com.example.domain.srs

enum class ReviewRating(val quality: Int, val label: String) {
    AGAIN(quality = 1, label = "Again"),
    HARD(quality = 3, label = "Hard"),
    GOOD(quality = 4, label = "Good"),
    EASY(quality = 5, label = "Easy")
}
