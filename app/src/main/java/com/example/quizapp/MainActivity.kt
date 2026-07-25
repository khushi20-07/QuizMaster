package com.example.quizapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val Purple = Color(0xFF6750A4)
private val PurpleDark = Color(0xFF4F378B)
private val LightBackground = Color(0xFFF8F7FC)
private val DarkBackground = Color(0xFF121212)
private val CorrectColor = Color(0xFFE8F5E9)
private val WrongColor = Color(0xFFFFEBEE)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val category: String,
    val difficulty: String,
    val explanation: String
)

enum class AppScreen {
    HOME,
    QUIZ,
    RESULT,
    LEADERBOARD,
    REVIEW,
    ANALYTICS
}

data class UserAnswer(
    val question: QuizQuestion,
    val selectedIndex: Int?,
    val isCorrect: Boolean
)

data class LeaderboardEntry(
    val name: String,
    val score: Int,
    val level: Int
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            QuizMasterApp(this)
        }
    }
}

@Composable
fun QuizMasterApp(context: Context) {

    val preferences = remember {
        context.getSharedPreferences(
            "quiz_master_data",
            Context.MODE_PRIVATE
        )
    }

    val allQuestions = remember {
        createQuestions()
    }

    var screen by remember {
        mutableStateOf(AppScreen.HOME)
    }

    var selectedCategory by remember {
        mutableStateOf("All")
    }

    var selectedDifficulty by remember {
        mutableStateOf("All")
    }

    var selectedQuestions by remember {
        mutableStateOf(allQuestions)
    }

    var userAnswers by remember {
        mutableStateOf(listOf<UserAnswer>())
    }

    var xp by remember {
        mutableStateOf(preferences.getInt("user_xp", 0))
    }

    var level by remember {
        mutableStateOf(preferences.getInt("user_level", 1))
    }

    var soundEnabled by remember {
        mutableStateOf(preferences.getBoolean("sound_enabled", true))
    }

    var currentIndex by remember {
        mutableStateOf(0)
    }

    var selectedAnswer by remember {
        mutableStateOf<Int?>(null)
    }

    var answerSubmitted by remember {
        mutableStateOf(false)
    }

    var timeLeft by remember {
        mutableStateOf(15)
    }

    var score by remember {
        mutableStateOf(0)
    }

    var wrongAnswers by remember {
        mutableStateOf(0)
    }

    var skippedAnswers by remember {
        mutableStateOf(0)
    }

    var currentStreak by remember {
        mutableStateOf(0)
    }

    var bestStreak by remember {
        mutableStateOf(
            preferences.getInt("best_streak", 0)
        )
    }

    var bestScore by remember {
        mutableStateOf(
            preferences.getInt("best_score", 0)
        )
    }

    var darkMode by remember {
        mutableStateOf(
            preferences.getBoolean("dark_mode", false)
        )
    }

    fun startQuiz() {

        val filtered = allQuestions.filter {

            val categoryMatch =
                selectedCategory == "All" ||
                        it.category == selectedCategory

            val difficultyMatch =
                selectedDifficulty == "All" ||
                        it.difficulty == selectedDifficulty

            categoryMatch && difficultyMatch
        }

        selectedQuestions =
            if (filtered.isEmpty()) {
                allQuestions
            } else {
                filtered
            }

                currentIndex = 0
        selectedAnswer = null
        answerSubmitted = false
        timeLeft = 15
        score = 0
        wrongAnswers = 0
        skippedAnswers = 0
        currentStreak = 0
        userAnswers = emptyList()
        screen = AppScreen.QUIZ
    }

    fun restartQuiz() {

        screen = AppScreen.HOME
        currentIndex = 0
        selectedAnswer = null
        answerSubmitted = false
        timeLeft = 15
        score = 0
        wrongAnswers = 0
        skippedAnswers = 0
        currentStreak = 0
        userAnswers = emptyList()
    }

    MaterialTheme {

        when (screen) {

            AppScreen.HOME -> {

                HomeScreen(

                    darkMode = darkMode,

                    onDarkModeChange = {

                        darkMode = it

                        preferences
                            .edit()
                            .putBoolean(
                                "dark_mode",
                                it
                            )
                            .apply()
                    },

                    selectedCategory =
                        selectedCategory,

                    onCategorySelected = {
                        selectedCategory = it
                    },

                    selectedDifficulty =
                        selectedDifficulty,

                    onDifficultySelected = {
                        selectedDifficulty = it
                    },

                    bestScore =
                        bestScore,

                    bestStreak =
                        bestStreak,

                    onStartQuiz = {
                        startQuiz()
                    }
                )
            }

            AppScreen.QUIZ -> {

                val question =
                    selectedQuestions[currentIndex]

                LaunchedEffect(
                    currentIndex,
                    answerSubmitted
                ) {

                    if (!answerSubmitted) {

                        timeLeft = 15

                        while (
                            timeLeft > 0 &&
                            !answerSubmitted
                        ) {

                            delay(1000)
                            timeLeft--
                        }

                                                if (
                            timeLeft == 0 &&
                            !answerSubmitted
                        ) {

                            skippedAnswers++
                            currentStreak = 0
                            userAnswers = userAnswers + UserAnswer(
                                question = question,
                                selectedIndex = null,
                                isCorrect = false
                            )
                            answerSubmitted = true
                        }
                    }
                }

                QuizScreen(

                    question =
                        question,

                    questionNumber =
                        currentIndex + 1,

                    totalQuestions =
                        selectedQuestions.size,

                    selectedAnswer =
                        selectedAnswer,

                    answerSubmitted =
                        answerSubmitted,

                    timeLeft =
                        timeLeft,

                    streak =
                        currentStreak,

                    onAnswerSelected = {

                        if (!answerSubmitted) {
                            selectedAnswer = it
                        }
                    },

                    onNext = {

                        if (!answerSubmitted) {

                                                        if (
                                selectedAnswer ==
                                question.correctAnswer
                            ) {

                                score++
                                currentStreak++
                                xp += if (currentStreak >= 3) 20 else 10

                                if (
                                    currentStreak >
                                    bestStreak
                                ) {

                                    bestStreak =
                                        currentStreak

                                    preferences
                                        .edit()
                                        .putInt(
                                            "best_streak",
                                            bestStreak
                                        )
                                        .apply()
                                }

                            } else {

                                wrongAnswers++
                                currentStreak = 0
                            }

                            userAnswers = userAnswers + UserAnswer(
                                question = question,
                                selectedIndex = selectedAnswer,
                                isCorrect = selectedAnswer == question.correctAnswer
                            )

                            answerSubmitted = true

                        } else {

                            if (
                                currentIndex <
                                selectedQuestions.lastIndex
                            ) {

                                currentIndex++
                                selectedAnswer = null
                                answerSubmitted = false
                                timeLeft = 15

                            } else {

                                // Quiz Finished
                                xp += 50
                                
                                // Level Up Check
                                val newLevel = (xp / 100) + 1
                                if (newLevel > level) {
                                    level = newLevel
                                }

                                preferences.edit()
                                    .putInt("user_xp", xp)
                                    .putInt("user_level", level)
                                    .apply()

                                if (
                                    score > bestScore
                                ) {

                                    bestScore = score

                                    preferences
                                        .edit()
                                        .putInt(
                                            "best_score",
                                            bestScore
                                        )
                                        .apply()
                                }

                                screen =
                                    AppScreen.RESULT
                            }
                        }
                    }
                )
            }

            AppScreen.RESULT -> {

                ResultScreen(

                    score =
                        score,

                    totalQuestions =
                        selectedQuestions.size,

                    wrongAnswers =
                        wrongAnswers,

                    skippedAnswers =
                        skippedAnswers,

                    bestScore =
                        bestScore,

                    bestStreak =
                        bestStreak,

                    onRestart =
                        ::restartQuiz
                )
            }

            AppScreen.LEADERBOARD -> {
                // Placeholder for Leaderboard
            }

            AppScreen.REVIEW -> {
                // Placeholder for Review
            }

            AppScreen.ANALYTICS -> {
                // Placeholder for Analytics
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(

    darkMode: Boolean,

    onDarkModeChange: (Boolean) -> Unit,

    selectedCategory: String,

    onCategorySelected: (String) -> Unit,

    selectedDifficulty: String,

    onDifficultySelected: (String) -> Unit,

    bestScore: Int,

    bestStreak: Int,

    onStartQuiz: () -> Unit
) {

    val background =
        if (darkMode) {
            DarkBackground
        } else {
            LightBackground
        }

    val textColor =
        if (darkMode) {
            Color.White
        } else {
            Color.Black
        }

    Scaffold(

        containerColor =
            background,

        topBar = {

            TopAppBar(

                title = {

                    Row(

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(

                            modifier =
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Purple
                                    ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text = "🧠",
                                fontSize = MaterialTheme
                                    .typography
                                    .headlineSmall
                                    .fontSize
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.width(10.dp)
                        )

                        Column {

                            Text(

                                text =
                                    "QuizMaster",

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(

                                text =
                                    "Challenge your mind",

                                style =
                                    MaterialTheme
                                        .typography
                                        .labelSmall
                            )
                        }
                    }
                },

                actions = {

                    Row(

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text = "🌙"
                        )

                        Switch(

                            checked =
                                darkMode,

                            onCheckedChange =
                                onDarkModeChange
                        )
                    }
                },

                colors =
                    TopAppBarDefaults
                        .topAppBarColors(
                            containerColor =
                                background
                        )
            )
        }

    ) { paddingValues ->

        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            item {

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(24.dp),

                    colors =
                        CardDefaults
                            .cardColors(
                                containerColor =
                                    Purple
                            )
                ) {

                    Column(

                        modifier =
                            Modifier.padding(24.dp)
                    ) {

                        Text(

                            text =
                                "Ready to test your knowledge? 🚀",

                            color =
                                Color.White,

                            style =
                                MaterialTheme
                                    .typography
                                    .headlineSmall,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(

                            text =
                                "Choose a challenge, build your streak and become a QuizMaster.",

                            color =
                                Color.White.copy(
                                    alpha = 0.9f
                                )
                        )
                    }
                }
            }

            item {

                Text(

                    text =
                        "📚 Choose Category",

                    color =
                        textColor,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    fontWeight =
                        FontWeight.Bold
                )
            }

            item {

                LazyRow(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    items(
                        listOf(
                            "All",
                            "Android",
                            "Programming",
                            "General"
                        )
                    ) { category ->

                        FilterChip(
                            modifier = Modifier.height(44.dp),

                            selected =
                                selectedCategory == category,

                            onClick = {
                                onCategorySelected(category)
                            },

                            label = {
                                Text(
                                    text = category,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            }

            item {

                Text(

                    text =
                        "⚡ Difficulty Level",

                    color =
                        textColor,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    fontWeight =
                        FontWeight.Bold
                )
            }

            item {

                LazyRow(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    items(
                        listOf(
                            "All",
                            "Easy",
                            "Medium",
                            "Hard"
                        )
                    ) { difficulty ->

                        FilterChip(
                            modifier = Modifier.height(44.dp),

                            selected =
                                selectedDifficulty == difficulty,

                            onClick = {
                                onDifficultySelected(difficulty)
                            },

                            label = {
                                Text(
                                    text = difficulty,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            }

            item {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    StatCard(

                        title =
                            "Best Score",

                        value =
                            bestScore.toString(),

                        emoji =
                            "🏆",

                        modifier =
                            Modifier.weight(1f)
                    )

                    StatCard(

                        title =
                            "Best Streak",

                        value =
                            bestStreak.toString(),

                        emoji =
                            "🔥",

                        modifier =
                            Modifier.weight(1f)
                    )
                }
            }

            item {

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(18.dp)
                ) {

                    Column(

                        modifier =
                            Modifier.padding(18.dp)
                    ) {

                        Text(

                            text =
                                "🏅 Achievements",

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )

                        Row(

                            horizontalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {

                            Text(
                                text =
                                    "🎯 First Quiz"
                            )

                            Text(
                                text =
                                    "🔥 Streak Master"
                            )

                            Text(
                                text =
                                    "🏆 Champion"
                            )
                        }
                    }
                }
            }

            item {

                Button(

                    onClick =
                        onStartQuiz,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(58.dp),

                    shape =
                        RoundedCornerShape(16.dp),

                    colors =
                        ButtonDefaults
                            .buttonColors(
                                containerColor =
                                    Purple
                            )
                ) {

                    Text(

                        text =
                            "🚀 Start Quiz",

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(

    title: String,

    value: String,

    emoji: String,

    modifier: Modifier
) {

    Card(

        modifier =
            modifier,

        shape =
            RoundedCornerShape(18.dp)
    ) {

        Column(

            modifier =
                Modifier.padding(18.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    emoji
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(

                text =
                    value,

                style =
                    MaterialTheme
                        .typography
                        .headlineMedium,

                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    title
            )
        }
    }
}

@Composable
fun QuizScreen(

    question: QuizQuestion,

    questionNumber: Int,

    totalQuestions: Int,

    selectedAnswer: Int?,

    answerSubmitted: Boolean,

    timeLeft: Int,

    streak: Int,

    onAnswerSelected: (Int) -> Unit,

    onNext: () -> Unit
) {

    Scaffold(

        containerColor =
            LightBackground

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
        ) {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(

                    text =
                        "Question $questionNumber/$totalQuestions",

                    fontWeight =
                        FontWeight.Bold
                )

                Text(

                    text =
                        "⏱️ ${timeLeft}s",

                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            LinearProgressIndicator(

                progress = {

                    questionNumber
                        .toFloat() /
                            totalQuestions
                },

                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            if (streak > 0) {

                Text(

                    text =
                        "🔥 Current Streak: $streak",

                    color =
                        Purple,

                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(22.dp)
            ) {

                Column(

                    modifier =
                        Modifier.padding(22.dp)
                ) {

                    Text(

                        text =
                            "${question.category} • ${question.difficulty}",

                        color =
                            Purple,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Text(

                        text =
                            question.question,

                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            LazyColumn(

                modifier =
                    Modifier.weight(1f),

                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                itemsIndexed(

                    question.options
                ) { index, option ->

                    val isCorrect =
                        index ==
                                question.correctAnswer

                    val isSelected =
                        index ==
                                selectedAnswer

                    val cardColor =

                        if (
                            answerSubmitted &&
                            isCorrect
                        ) {

                            CorrectColor

                        } else if (
                            answerSubmitted &&
                            isSelected
                        ) {

                            WrongColor

                        } else {

                            Color.White
                        }

                    Card(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {

                                    if (
                                        !answerSubmitted
                                    ) {

                                        onAnswerSelected(
                                            index
                                        )
                                    }
                                },

                        shape =
                            RoundedCornerShape(16.dp),

                        colors =
                            CardDefaults
                                .cardColors(
                                    containerColor =
                                        cardColor
                                )
                    ) {

                        Row(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                        ) {

                            Text(

                                text =
                                    "${'A' + index}.",

                                fontWeight =
                                    FontWeight.Bold,

                                color =
                                    Purple
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(14.dp)
                            )

                            Text(
                                text =
                                    option
                            )
                        }
                    }
                }
            }

            if (answerSubmitted) {

                Card(

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(

                        modifier =
                            Modifier.padding(14.dp)
                    ) {

                        Text(

                            text =

                                if (
                                    selectedAnswer ==
                                    question.correctAnswer
                                ) {

                                    "🎉 Correct Answer!"

                                } else if (
                                    selectedAnswer == null
                                ) {

                                    "⏰ Time's up!"

                                } else {

                                    "❌ Incorrect Answer"
                                },

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )

                        Text(

                            text =
                                "💡 ${question.explanation}"
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )
            }

            Button(

                onClick =
                    onNext,

                enabled =
                    selectedAnswer != null ||
                            timeLeft == 0,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp),

                shape =
                    RoundedCornerShape(16.dp),

                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                Purple
                        )
            ) {

                Text(

                    text =

                        if (!answerSubmitted) {

                            "Submit Answer"

                        } else if (
                            questionNumber <
                            totalQuestions
                        ) {

                            "Next Question ➜"

                        } else {

                            "View Result 🏆"
                        }
                )
            }
        }
    }
}

@Composable
fun ResultScreen(

    score: Int,

    totalQuestions: Int,

    wrongAnswers: Int,

    skippedAnswers: Int,

    bestScore: Int,

    bestStreak: Int,

    onRestart: () -> Unit
) {

    val percentage =

        if (totalQuestions > 0) {

            score * 100 /
                    totalQuestions

        } else {

            0
        }

    LazyColumn(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    LightBackground
                )
                .padding(24.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        item {

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            Text(

                text =
                    "🏆 Quiz Completed!",

                style =
                    MaterialTheme
                        .typography
                        .headlineMedium,

                fontWeight =
                    FontWeight.Bold
            )
        }

        item {

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(24.dp),

                colors =
                    CardDefaults
                        .cardColors(
                            containerColor =
                                Purple
                        )
            ) {

                Column(

                    modifier =
                        Modifier.padding(28.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(

                        text =
                            "🎉",

                        style =
                            MaterialTheme
                                .typography
                                .displayMedium
                    )

                    Text(

                        text =
                            "$score / $totalQuestions",

                        color =
                            Color.White,

                        style =
                            MaterialTheme
                                .typography
                                .displaySmall,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(

                        text =
                            "$percentage% Accuracy",

                        color =
                            Color.White
                    )
                }
            }
        }

        item {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                StatCard(

                    title =
                        "Correct",

                    value =
                        score.toString(),

                    emoji =
                        "✅",

                    modifier =
                        Modifier.weight(1f)
                )

                StatCard(

                    title =
                        "Wrong",

                    value =
                        wrongAnswers.toString(),

                    emoji =
                        "❌",

                    modifier =
                        Modifier.weight(1f)
                )

                StatCard(

                    title =
                        "Skipped",

                    value =
                        skippedAnswers.toString(),

                    emoji =
                        "⏭️",

                    modifier =
                        Modifier.weight(1f)
                )
            }
        }

        item {

            Text(

                text =

                    when {

                        percentage >= 90 ->
                            "🌟 Quiz Champion!"

                        percentage >= 70 ->
                            "🔥 Excellent Performance!"

                        percentage >= 50 ->
                            "👏 Good Job! Keep Improving!"

                        else ->
                            "💪 Keep Practicing!"
                    },

                style =
                    MaterialTheme
                        .typography
                        .titleMedium,

                fontWeight =
                    FontWeight.Bold
            )
        }

        item {

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(18.dp)
            ) {

                Column(

                    modifier =
                        Modifier.padding(18.dp)
                ) {

                    Text(

                        text =
                            "🏅 Achievement Unlocked",

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(

                        text =

                            when {

                                percentage == 100 ->
                                    "🏆 Perfect Score Champion"

                                percentage >= 80 ->
                                    "🌟 Knowledge Master"

                                bestStreak >= 3 ->
                                    "🔥 Streak Master"

                                else ->
                                    "🎯 Quiz Explorer"
                            }
                    )
                }
            }
        }

        item {

            Text(

                text =
                    "🏆 Best Score: $bestScore   🔥 Best Streak: $bestStreak"
            )
        }

        item {

            Button(

                onClick =
                    onRestart,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),

                shape =
                    RoundedCornerShape(16.dp),

                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                Purple
                        )
            ) {

                Text(
                    text =
                        "🔄 Play Again"
                )
            }
        }
    }
}

fun createQuestions(): List<QuizQuestion> {

    return listOf(

        QuizQuestion(

            "Which language is officially preferred for modern Android development?",

            listOf(
                "Kotlin",
                "HTML",
                "SQL",
                "CSS"
            ),

            0,

            "Android",

            "Easy",

            "Kotlin is Google's preferred language for modern Android development."
        ),

        QuizQuestion(

            "Which data structure follows the LIFO principle?",

            listOf(
                "Queue",
                "Stack",
                "Array",
                "Tree"
            ),

            1,

            "Programming",

            "Easy",

            "A stack follows Last In, First Out, also known as LIFO."
        ),

        QuizQuestion(

            "What does CPU stand for?",

            listOf(
                "Central Processing Unit",
                "Computer Personal Unit",
                "Central Program Utility",
                "Control Processing User"
            ),

            0,

            "General",

            "Easy",

            "CPU stands for Central Processing Unit."
        ),

        QuizQuestion(

            "Which technology is used to build modern declarative Android UI?",

            listOf(
                "Jetpack Compose",
                "MySQL",
                "Git",
                "Firebase"
            ),

            0,

            "Android",

            "Medium",

            "Jetpack Compose is Android's modern declarative UI toolkit."
        ),

        QuizQuestion(

            "Which keyword declares a read-only variable in Kotlin?",

            listOf(
                "var",
                "let",
                "val",
                "const"
            ),

            2,

            "Programming",

            "Medium",

            "The val keyword creates a read-only reference in Kotlin."
        ),

        QuizQuestion(

            "What does UI stand for?",

            listOf(
                "User Interface",
                "Universal Internet",
                "User Information",
                "Utility Interface"
            ),

            0,

            "Android",

            "Easy",

            "UI means User Interface, the visual part users interact with."
        ),

        QuizQuestion(

            "Which concept allows a class to acquire properties of another class?",

            listOf(
                "Inheritance",
                "Compilation",
                "Iteration",
                "Encapsulation"
            ),

            0,

            "Programming",

            "Hard",

            "Inheritance allows a class to reuse properties and behavior from another class."
        ),

        QuizQuestion(

            "Which file is commonly used to define Android app permissions?",

            listOf(
                "AndroidManifest.xml",
                "MainActivity.kt",
                "build.gradle",
                "settings.json"
            ),

            0,

            "Android",

            "Medium",

            "AndroidManifest.xml contains important app configuration and permissions."
        ),

        QuizQuestion(

            "Which device is primarily used to input text?",

            listOf(
                "Monitor",
                "Printer",
                "Keyboard",
                "Speaker"
            ),

            2,

            "General",

            "Easy",

            "A keyboard is an input device used to enter text and commands."
        ),

        QuizQuestion(

            "Which programming language is widely used in data science and AI?",

            listOf(
                "Python",
                "HTML",
                "CSS",
                "XML"
            ),

            0,

            "Programming",

            "Easy",

            "Python is widely used for data science, machine learning and AI."
        )
    )
}