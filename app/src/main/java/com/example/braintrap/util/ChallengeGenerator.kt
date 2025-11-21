package com.example.braintrap.util

import com.example.braintrap.data.model.Challenge
import com.example.braintrap.data.model.ChallengeDifficulty
import kotlin.random.Random

object ChallengeGenerator {
    private const val UNLOCK_TIME_MINUTES = 15
    private const val PROBLEMS_REQUIRED = 3
    private const val TIME_LIMIT_SECONDS = 30
    
    private var questionTypeHistory = mutableListOf<QuestionType>()
    
    enum class QuestionType {
        ARITHMETIC, SEQUENCE, PATTERN, MENTAL_MATH, LOGIC, COUNTDOWN
    }
    
    fun generateChallenge(difficulty: ChallengeDifficulty): Challenge {
        // Ensure variety by not repeating the same question type consecutively
        val availableTypes = QuestionType.values().toList()
        val questionType = availableTypes
            .filterNot { questionTypeHistory.lastOrNull() == it }
            .random()
        
        questionTypeHistory.add(questionType)
        if (questionTypeHistory.size > 5) questionTypeHistory.removeAt(0)
        
        val (problem, answer) = when (difficulty) {
            ChallengeDifficulty.EASY -> generateEasyProblem(questionType)
            ChallengeDifficulty.MEDIUM -> generateMediumProblem(questionType)
            ChallengeDifficulty.HARD -> generateHardProblem(questionType)
        }
        
        return Challenge(
            problem = problem,
            answer = answer,
            difficulty = difficulty,
            timeLimitSeconds = TIME_LIMIT_SECONDS
        )
    }
    
    fun getDifficultyForUnlockAttempt(attemptNumber: Int): ChallengeDifficulty {
        return when {
            attemptNumber == 1 -> ChallengeDifficulty.EASY
            attemptNumber == 2 -> ChallengeDifficulty.MEDIUM
            else -> ChallengeDifficulty.HARD
        }
    }
    
    fun getUnlockTimeMinutes(): Int = UNLOCK_TIME_MINUTES
    
    fun getProblemsRequired(): Int = PROBLEMS_REQUIRED
    
    private fun generateEasyProblem(type: QuestionType): Pair<String, Int> {
        return when (type) {
            QuestionType.ARITHMETIC -> {
                // Simple but tricky: use friendly numbers
                when (Random.nextInt(4)) {
                    0 -> {
                        // Numbers that end in 5 or 0
                        val num1 = Random.nextInt(1, 10) * 5
                        val num2 = Random.nextInt(1, 10) * 5
                        Pair("$num1 + $num2", num1 + num2)
                    }
                    1 -> {
                        // Add to make 10, 20, 30...
                        val base = Random.nextInt(2, 8) * 10
                        val subtract = Random.nextInt(1, 9)
                        Pair("$base - $subtract", base - subtract)
                    }
                    2 -> {
                        // Reverse thinking
                        val result = Random.nextInt(10, 30)
                        val part = Random.nextInt(5, result - 3)
                        Pair("What + $part = $result?", result - part)
                    }
                    else -> {
                        // Simple doubles
                        val num = Random.nextInt(5, 25)
                        Pair("$num + $num", num * 2)
                    }
                }
            }
            QuestionType.SEQUENCE -> {
                // Obvious patterns but require attention
                when (Random.nextInt(3)) {
                    0 -> {
                        // Count by 2s, 5s, or 10s
                        val step = listOf(2, 5, 10).random()
                        val start = Random.nextInt(1, 6)
                        val seq = listOf(start, start + step, start + step * 2)
                        Pair("Next: ${seq.joinToString(", ")}, ?", start + step * 3)
                    }
                    1 -> {
                        // Repeating pattern
                        val pattern = listOf(1, 2, 1, 2)
                        Pair("Pattern: ${pattern.joinToString(", ")}, ?", 1)
                    }
                    else -> {
                        // Growing by 1
                        val start = Random.nextInt(10, 20)
                        Pair("${start}, ${start + 1}, ${start + 2}, ?", start + 3)
                    }
                }
            }
            QuestionType.PATTERN -> {
                when (Random.nextInt(4)) {
                    0 -> {
                        // Mirror numbers
                        val num = Random.nextInt(11, 20)
                        Pair("If 1 → 10, 2 → 20, then $num → ?", num * 10)
                    }
                    1 -> {
                        // Half of even number
                        val num = Random.nextInt(5, 15) * 2
                        Pair("Half of $num?", num / 2)
                    }
                    2 -> {
                        // Missing number
                        val answer = Random.nextInt(5, 15)
                        val total = answer + answer
                        Pair("$total ÷ 2 = ?", answer)
                    }
                    else -> {
                        // Plus 1 trick
                        val num = Random.nextInt(10, 50)
                        Pair("$num + 1 = ?", num + 1)
                    }
                }
            }
            QuestionType.MENTAL_MATH -> {
                when (Random.nextInt(3)) {
                    0 -> {
                        // Round number tricks
                        val base = Random.nextInt(2, 8) * 10
                        val add = Random.nextInt(1, 10)
                        Pair("$base + $add", base + add)
                    }
                    1 -> {
                        // Counting coins (multiples of 5)
                        val coins = Random.nextInt(3, 10)
                        Pair("$coins coins × 5¢ = ?¢", coins * 5)
                    }
                    else -> {
                        // Simple double-digit addition
                        val a = Random.nextInt(11, 20)
                        val b = Random.nextInt(1, 10)
                        Pair("$a + $b", a + b)
                    }
                }
            }
            QuestionType.LOGIC -> {
                when (Random.nextInt(3)) {
                    0 -> {
                        // Age riddles
                        val age = Random.nextInt(5, 15)
                        val years = Random.nextInt(3, 8)
                        Pair("I'm $age now, I was ? ${years} years ago", age - years)
                    }
                    1 -> {
                        // Sharing equally
                        val people = Random.nextInt(2, 6)
                        val each = Random.nextInt(3, 8)
                        Pair("$people people, $each each = ?", people * each)
                    }
                    else -> {
                        // Difference trick
                        val big = Random.nextInt(20, 40)
                        val small = Random.nextInt(10, big - 5)
                        Pair("$big - $small = ?", big - small)
                    }
                }
            }
            QuestionType.COUNTDOWN -> {
                when (Random.nextInt(3)) {
                    0 -> {
                        // Subtract to zero
                        val num = Random.nextInt(10, 30)
                        Pair("$num - $num = ?", 0)
                    }
                    1 -> {
                        // Count backwards
                        val start = Random.nextInt(15, 30)
                        val back = Random.nextInt(3, 8)
                        Pair("$start, ${start - 1}, ${start - 2}, $start - $back = ?", start - back)
                    }
                    else -> {
                        // Ten less
                        val num = Random.nextInt(20, 50)
                        Pair("$num - 10 = ?", num - 10)
                    }
                }
            }
        }
    }
    
    private fun generateMediumProblem(type: QuestionType): Pair<String, Int> {
        return when (type) {
            QuestionType.ARITHMETIC -> {
                when (Random.nextInt(4)) {
                    0 -> {
                        // Multiplication with small numbers
                        val num1 = Random.nextInt(2, 9)
                        val num2 = Random.nextInt(2, 9)
                        Pair("$num1 × $num2", num1 * num2)
                    }
                    1 -> {
                        // Division with exact answers
                        val divisor = Random.nextInt(2, 7)
                        val result = Random.nextInt(3, 10)
                        Pair("${divisor * result} ÷ $divisor", result)
                    }
                    2 -> {
                        // Two-step with small numbers
                        val a = Random.nextInt(10, 20)
                        val b = Random.nextInt(5, 10)
                        val c = Random.nextInt(3, 8)
                        Pair("$a + $b - $c", a + b - c)
                    }
                    else -> {
                        // Missing factor
                        val factor = Random.nextInt(3, 8)
                        val result = Random.nextInt(20, 50)
                        val answer = result / factor
                        Pair("? × $factor = $result", answer)
                    }
                }
            }
            QuestionType.SEQUENCE -> {
                when (Random.nextInt(3)) {
                    0 -> {
                        // Skip counting
                        val skip = Random.nextInt(3, 6)
                        val start = Random.nextInt(2, 10)
                        val seq = listOf(start, start + skip, start + skip * 2)
                        Pair("${seq.joinToString(", ")}, ?", start + skip * 3)
                    }
                    1 -> {
                        // Subtract pattern
                        val start = Random.nextInt(30, 50)
                        val step = Random.nextInt(3, 7)
                        Pair("$start, ${start - step}, ${start - step * 2}, ?", start - step * 3)
                    }
                    else -> {
                        // Times table pattern
                        val table = Random.nextInt(2, 6)
                        Pair("$table, ${table * 2}, ${table * 3}, ?", table * 4)
                    }
                }
            }
            QuestionType.PATTERN -> {
                when (Random.nextInt(4)) {
                    0 -> {
                        // Square of small numbers
                        val num = Random.nextInt(4, 10)
                        Pair("$num × $num = ?", num * num)
                    }
                    1 -> {
                        // Digit sum trick
                        val num = Random.nextInt(11, 30)
                        val tens = num / 10
                        val ones = num % 10
                        Pair("$num: ${tens} + ${ones} = ?", tens + ones)
                    }
                    2 -> {
                        // Double and add
                        val num = Random.nextInt(5, 15)
                        Pair("($num × 2) + 1 = ?", num * 2 + 1)
                    }
                    else -> {
                        // Round to nearest 10
                        val num = Random.nextInt(15, 45)
                        val rounded = ((num + 5) / 10) * 10
                        Pair("Round $num to nearest 10", rounded)
                    }
                }
            }
            QuestionType.MENTAL_MATH -> {
                when (Random.nextInt(3)) {
                    0 -> {
                        // Near-tens addition
                        val base = Random.nextInt(3, 7) * 10
                        val near = Random.nextInt(8, 13)
                        Pair("$base + $near", base + near)
                    }
                    1 -> {
                        // Money math
                        val dollars = Random.nextInt(5, 20)
                        val quarters = Random.nextInt(1, 4)
                        Pair("$$dollars + ${quarters} quarters = $$?", dollars + quarters)
                    }
                    else -> {
                        // Friendly subtraction
                        val big = Random.nextInt(30, 60)
                        val small = Random.nextInt(10, 20)
                        Pair("$big - $small", big - small)
                    }
                }
            }
            QuestionType.LOGIC -> {
                when (Random.nextInt(3)) {
                    0 -> {
                        // Groups of items
                        val groups = Random.nextInt(3, 7)
                        val each = Random.nextInt(4, 9)
                        Pair("$groups boxes, $each in each = ?", groups * each)
                    }
                    1 -> {
                        // Before/after time
                        val hours = Random.nextInt(3, 10)
                        Pair("3 hours from now, how many in $hours hours?", hours - 3)
                    }
                    else -> {
                        // Sharing puzzle
                        val total = Random.nextInt(20, 50)
                        val people = listOf(2, 3, 4, 5).random()
                        if (total % people == 0) {
                            Pair("$total ÷ $people people = ?", total / people)
                        } else {
                            val each = Random.nextInt(5, 12)
                            Pair("$people × $each = ?", people * each)
                        }
                    }
                }
            }
            QuestionType.COUNTDOWN -> {
                when (Random.nextInt(3)) {
                    0 -> {
                        // Subtract by 5s
                        val start = Random.nextInt(40, 70)
                        val steps = Random.nextInt(2, 5)
                        Pair("$start - ${steps * 5} = ?", start - steps * 5)
                    }
                    1 -> {
                        // Halve it
                        val num = Random.nextInt(10, 30) * 2
                        Pair("$num ÷ 2 = ?", num / 2)
                    }
                    else -> {
                        // Go back 10
                        val num = Random.nextInt(50, 90)
                        Pair("$num - 10 - 10 = ?", num - 20)
                    }
                }
            }
        }
    }
    
    private fun generateHardProblem(type: QuestionType): Pair<String, Int> {
        return when (type) {
            QuestionType.ARITHMETIC -> {
                when (Random.nextInt(4)) {
                    0 -> {
                        // Order of operations (small numbers)
                        val a = Random.nextInt(5, 15)
                        val b = Random.nextInt(2, 6)
                        val c = Random.nextInt(2, 6)
                        Pair("$a + $b × $c", a + b * c)
                    }
                    1 -> {
                        // Reverse division
                        val divisor = Random.nextInt(3, 8)
                        val quotient = Random.nextInt(5, 12)
                        Pair("? ÷ $divisor = $quotient", divisor * quotient)
                    }
                    2 -> {
                        // Three numbers trick
                        val a = Random.nextInt(10, 25)
                        val b = Random.nextInt(5, 12)
                        val c = Random.nextInt(3, 8)
                        Pair("$a - $b + $c", a - b + c)
                    }
                    else -> {
                        // Percentage trick (10% or 50%)
                        val num = Random.nextInt(20, 100)
                        if (Random.nextBoolean()) {
                            Pair("50% of $num = ?", num / 2)
                        } else {
                            Pair("10% of $num = ?", num / 10)
                        }
                    }
                }
            }
            QuestionType.SEQUENCE -> {
                when (Random.nextInt(3)) {
                    0 -> {
                        // Fibonacci-lite (small numbers)
                        val a = Random.nextInt(1, 5)
                        val b = Random.nextInt(1, 5)
                        val c = a + b
                        val d = b + c
                        Pair("${a}, ${b}, ${c}, ${d}, ?", c + d)
                    }
                    1 -> {
                        // Alternating pattern
                        val base = Random.nextInt(5, 15)
                        Pair("$base, ${base + 2}, ${base}, ${base + 2}, ?", base)
                    }
                    else -> {
                        // Powers of 2
                        val start = listOf(2, 4).random()
                        Pair("$start, ${start * 2}, ${start * 4}, ?", start * 8)
                    }
                }
            }
            QuestionType.PATTERN -> {
                when (Random.nextInt(4)) {
                    0 -> {
                        // Square trick with small numbers
                        val num = Random.nextInt(6, 12)
                        Pair("$num² = ?", num * num)
                    }
                    1 -> {
                        // Sum of digits
                        val num = Random.nextInt(25, 99)
                        val sum = (num / 10) + (num % 10)
                        Pair("Sum digits of $num", sum)
                    }
                    2 -> {
                        // Triple it
                        val num = Random.nextInt(8, 20)
                        Pair("$num × 3 = ?", num * 3)
                    }
                    else -> {
                        // Reverse number
                        val tens = Random.nextInt(2, 8)
                        val ones = Random.nextInt(1, 9)
                        val original = tens * 10 + ones
                        val reversed = ones * 10 + tens
                        Pair("Reverse $original = ?", reversed)
                    }
                }
            }
            QuestionType.MENTAL_MATH -> {
                when (Random.nextInt(3)) {
                    0 -> {
                        // Near-hundred math
                        val base = Random.nextInt(2, 5) * 100
                        val adjust = Random.nextInt(10, 30)
                        if (Random.nextBoolean()) {
                            Pair("$base - $adjust", base - adjust)
                        } else {
                            Pair("$base + $adjust", base + adjust)
                        }
                    }
                    1 -> {
                        // Multiply by 10, then adjust
                        val num = Random.nextInt(5, 20)
                        val times10 = num * 10
                        Pair("($num × 10) - $num = ?", times10 - num)
                    }
                    else -> {
                        // Split and add
                        val a = Random.nextInt(20, 40)
                        val b = Random.nextInt(20, 40)
                        Pair("$a + $b", a + b)
                    }
                }
            }
            QuestionType.LOGIC -> {
                when (Random.nextInt(3)) {
                    0 -> {
                        // Age puzzle with small numbers
                        val current = Random.nextInt(8, 20)
                        val years = Random.nextInt(3, 8)
                        Pair("Age $current, in $years years = ?", current + years)
                    }
                    1 -> {
                        // Price calculation
                        val items = Random.nextInt(3, 8)
                        val price = Random.nextInt(5, 15)
                        Pair("$items items × $$price = $$?", items * price)
                    }
                    else -> {
                        // Missing part
                        val total = Random.nextInt(50, 100)
                        val part = Random.nextInt(20, total - 10)
                        Pair("Total $total, spent $part, left = ?", total - part)
                    }
                }
            }
            QuestionType.COUNTDOWN -> {
                when (Random.nextInt(3)) {
                    0 -> {
                        // Multi-step subtraction
                        val start = Random.nextInt(60, 100)
                        val sub1 = Random.nextInt(10, 20)
                        val sub2 = Random.nextInt(5, 15)
                        Pair("$start - $sub1 - $sub2", start - sub1 - sub2)
                    }
                    1 -> {
                        // Quarter it
                        val num = Random.nextInt(10, 25) * 4
                        Pair("$num ÷ 4 = ?", num / 4)
                    }
                    else -> {
                        // Backwards by 15
                        val num = Random.nextInt(70, 120)
                        Pair("$num - 15 = ?", num - 15)
                    }
                }
            }
        }
    }
}

