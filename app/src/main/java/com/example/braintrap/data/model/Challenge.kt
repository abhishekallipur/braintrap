package com.example.braintrap.data.model

/**
 * Represents an arithmetic challenge problem
 */
data class Challenge(
    val problem: String,
    val answer: Int,
    val difficulty: ChallengeDifficulty,
    val timeLimitSeconds: Int = 30
)

enum class ChallengeDifficulty {
    EASY,      // Simple addition/subtraction (1st unlock)
    MEDIUM,    // Multiplication/division (2nd unlock)
    HARD       // Multi-step problems (3rd+ unlock)
}

