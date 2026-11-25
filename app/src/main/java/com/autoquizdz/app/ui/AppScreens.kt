@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.autoquizdz.app.ui

import android.Manifest
import android.app.Activity
import android.app.Application
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoquizdz.app.ads.AdPolicyState
import com.autoquizdz.app.ads.BannerAdView
import com.autoquizdz.app.ads.InterstitialAdHelper
import com.autoquizdz.app.ads.RewardedAdHelper
import com.autoquizdz.app.data.*
import com.autoquizdz.app.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    data object Splash : Screen()
    data object Onboarding : Screen()
    data object MainTabs : Screen()
    data class Practice(
        val title: String,
        val questionIds: List<Int>,
        val isDailyChallenge: Boolean = false
    ) : Screen()
    data class ExamSession(
        val questionIds: List<Int>,
        val durationMinutes: Int,
        val passingPercent: Int
    ) : Screen()
    data class ExamResultView(
        val questions: List<QuestionEntity>,
        val selectedAnswers: Map<Int, Int>,
        val passingPercent: Int,
        val durationSeconds: Int
    ) : Screen()
    data object Mistakes : Screen()
    data object Favorites : Screen()
    data object Settings : Screen()
    data object About : Screen()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val db = AppDatabase.getInstance(application)
    val prefs = UserPrefs(application)

    val questions: StateFlow<List<QuestionEntity>> = db.questionDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roadSigns: StateFlow<List<RoadSignEntity>> = db.roadSignDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val examResults: StateFlow<List<ExamResultEntity>> = db.examResultDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var isArabic by mutableStateOf(prefs.language == "ar")
        private set

    var xp by mutableIntStateOf(prefs.xp)
        private set

    var streak by mutableIntStateOf(prefs.streak)
        private set

    var totalStudySeconds by mutableIntStateOf(prefs.totalStudySeconds)
        private set

    var notificationsEnabled by mutableStateOf(prefs.notificationsEnabled)
        private set

    var contentVersion by mutableIntStateOf(prefs.contentVersion)
        private set

    var syncStatus by mutableStateOf("")
        private set

    var isSyncing by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (db.questionDao().getCount() == 0) {
                db.questionDao().upsertAll(SeedData.initialQuestions())
            }
            if (db.roadSignDao().getCount() == 0) {
                db.roadSignDao().upsertAll(SeedData.initialRoadSigns())
            }
        }
    }

    fun setLanguage(lang: String) {
        prefs.language = lang
        isArabic = (lang == "ar")
    }

    fun completeOnboarding() {
        prefs.onboardingDone = true
    }

    fun toggleFavorite(q: QuestionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.questionDao().setFavorite(q.id, !q.isFavorite)
        }
    }

    fun recordAnswer(questionId: Int, isCorrect: Boolean, secondsSpent: Int = 10) {
        viewModelScope.launch(Dispatchers.IO) {
            db.questionDao().recordAnswer(
                id = questionId,
                correctDelta = if (isCorrect) 1 else 0,
                isWrong = !isCorrect
            )
            val earned = if (isCorrect) 10 else 2
            prefs.recordStudyActivity(earned, secondsSpent)
            refreshPrefs()
        }
    }

    fun completeDailyChallenge(correctCount: Int) {
        prefs.dailyChallengeDate = prefs.todayKey()
        prefs.recordStudyActivity(30 + correctCount * 5, 60)
        refreshPrefs()
    }

    fun grantBonusXp(amount: Int) {
        prefs.recordStudyActivity(amount, 0)
        refreshPrefs()
    }

    fun saveExamResult(
        totalQuestions: Int,
        correctAnswers: Int,
        passingPercent: Int,
        durationSeconds: Int
    ) {
        val pct = if (totalQuestions == 0) 0 else (correctAnswers * 100) / totalQuestions
        val passed = pct >= passingPercent
        viewModelScope.launch(Dispatchers.IO) {
            db.examResultDao().insert(
                ExamResultEntity(
                    totalQuestions = totalQuestions,
                    correctAnswers = correctAnswers,
                    passingPercent = passingPercent,
                    passed = passed,
                    durationSeconds = durationSeconds
                )
            )
            prefs.recordStudyActivity(if (passed) 60 else 20, durationSeconds)
            refreshPrefs()
        }
    }

    fun setNotifications(enabled: Boolean) {
        prefs.notificationsEnabled = enabled
        notificationsEnabled = enabled
        if (enabled) ReminderScheduler.scheduleDaily(getApplication())
        else ReminderScheduler.cancelDaily(getApplication())
    }

    fun syncRemoteContent(url: String) {
        prefs.remoteJsonUrl = url.trim()
        viewModelScope.launch {
            isSyncing = true
            syncStatus = if (isArabic) "جاري التحقق..." else "Synchronisation..."
            val result = ContentSyncManager.syncFromUrl(db, prefs, url.trim())
            isSyncing = false
            contentVersion = result.version
            syncStatus = if (result.success) {
                if (isArabic) "تم التحديث بنجاح (الإصدار ${result.version}) - أسئلة: ${result.questionsAddedOrUpdated}"
                else "Mise à jour réussie (v${result.version}) - Questions : ${result.questionsAddedOrUpdated}"
            } else {
                result.message
            }
        }
    }

    fun resetAllProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            db.questionDao().resetAllStats()
            db.examResultDao().clearAll()
            prefs.resetProgress()
            refreshPrefs()
        }
    }

    private fun refreshPrefs() {
        xp = prefs.xp
        streak = prefs.streak
        totalStudySeconds = prefs.totalStudySeconds
    }
}

@Composable
fun AutoQuizRoot(vm: MainViewModel, activity: Activity) {
    val strings = remember(vm.isArabic) { AppStrings(vm.isArabic) }
    val questions by vm.questions.collectAsState()
    val signs by vm.roadSigns.collectAsState()
    val examHistory by vm.examResults.collectAsState()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Enforce AdMob Policy Guard automatically based on active screen
    LaunchedEffect(currentScreen) {
        AdPolicyState.isProtectedFlowActive = when (currentScreen) {
            is Screen.Practice, is Screen.ExamSession -> true
            else -> false
        }
    }

    when (val screen = currentScreen) {
        is Screen.Splash -> {
            SplashScreen(strings) {
                currentScreen = if (vm.prefs.onboardingDone) Screen.MainTabs else Screen.Onboarding
            }
        }

        is Screen.Onboarding -> {
            OnboardingScreen(
                strings = strings,
                isArabic = vm.isArabic,
                onSelectLanguage = { vm.setLanguage(it) },
                onContinue = {
                    vm.completeOnboarding()
                    currentScreen = Screen.MainTabs
                }
            )
        }

        is Screen.MainTabs -> {
            Scaffold(
                bottomBar = {
                    Column {
                        BannerAdView()
                        NavigationBar(containerColor = Color.White) {
                            val tabs = listOf(
                                Triple(0, strings.navHome, Icons.Filled.Home),
                                Triple(1, strings.navLearn, Icons.Filled.School),
                                Triple(2, strings.navExam, Icons.Filled.Quiz),
                                Triple(3, strings.navSigns, Icons.Filled.Traffic),
                                Triple(4, strings.navProfile, Icons.Filled.Person)
                            )
                            tabs.forEach { (index, label, icon) ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick = {
                                        if (selectedTab == 3 && index != 3) {
                                            InterstitialAdHelper.showIfAllowed(activity)
                                        }
                                        selectedTab = index
                                    },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DzGreen,
                                        selectedTextColor = DzGreen,
                                        indicatorColor = DzGreen.copy(alpha = 0.14f)
                                    )
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                    when (selectedTab) {
                        0 -> HomeDashboard(
                            vm = vm,
                            strings = strings,
                            questions = questions,
                            onContinueLearning = {
                                val ids = questions.shuffled().take(10).map { it.id }
                                currentScreen = Screen.Practice(strings.continueLearning, ids)
                            },
                            onStartExamTab = { selectedTab = 2 },
                            onOpenSignsTab = { selectedTab = 3 },
                            onOpenLearnTab = { selectedTab = 1 },
                            onOpenDailyChallenge = {
                                val ids = questions.shuffled().take(10).map { it.id }
                                currentScreen = Screen.Practice(strings.dailyChallenge, ids, isDailyChallenge = true)
                            },
                            onOpenMistakes = { currentScreen = Screen.Mistakes },
                            onOpenFavorites = { currentScreen = Screen.Favorites }
                        )

                        1 -> LearnCategoriesScreen(
                            strings = strings,
                            questions = questions,
                            onStartPractice = { title, ids ->
                                currentScreen = Screen.Practice(title, ids)
                            },
                            onOpenMistakes = { currentScreen = Screen.Mistakes },
                            onOpenFavorites = { currentScreen = Screen.Favorites }
                        )

                        2 -> ExamSetupScreen(
                            strings = strings,
                            totalAvailable = questions.size,
                            onStartExam = { count, mins, passPct ->
                                val ids = questions.shuffled().take(count).map { it.id }
                                currentScreen = Screen.ExamSession(ids, mins, passPct)
                            }
                        )

                        3 -> RoadSignsScreen(strings = strings, signs = signs)

                        4 -> ProfileDashboardScreen(
                            vm = vm,
                            strings = strings,
                            questions = questions,
                            examHistory = examHistory,
                            activity = activity,
                            onOpenSettings = { currentScreen = Screen.Settings },
                            onOpenAbout = { currentScreen = Screen.About },
                            onOpenDailyChallenge = {
                                val ids = questions.shuffled().take(10).map { it.id }
                                currentScreen = Screen.Practice(strings.dailyChallenge, ids, isDailyChallenge = true)
                            }
                        )
                    }
                }
            }
        }

        is Screen.Practice -> {
            val sessionQuestions = remember(screen.questionIds, questions) {
                val map = questions.associateBy { it.id }
                screen.questionIds.mapNotNull { map[it] }
            }
            QuestionPracticeScreen(
                title = screen.title,
                questions = sessionQuestions,
                strings = strings,
                isDailyChallenge = screen.isDailyChallenge,
                onToggleFavorite = { vm.toggleFavorite(it) },
                onAnswerRecorded = { qId, correct -> vm.recordAnswer(qId, correct) },
                onFinishDaily = { correctCount -> vm.completeDailyChallenge(correctCount) },
                onBack = { currentScreen = Screen.MainTabs }
            )
        }

        is Screen.ExamSession -> {
            val examQuestions = remember(screen.questionIds, questions) {
                val map = questions.associateBy { it.id }
                screen.questionIds.mapNotNull { map[it] }
            }
            ExamSimulatorScreen(
                questions = examQuestions,
                durationMinutes = screen.durationMinutes,
                strings = strings,
                onSubmit = { answers, elapsedSeconds ->
                    val correct = examQuestions.count { q -> answers[q.id] == q.correctIndex }
                    vm.saveExamResult(
                        totalQuestions = examQuestions.size,
                        correctAnswers = correct,
                        passingPercent = screen.passingPercent,
                        durationSeconds = elapsedSeconds
                    )
                    currentScreen = Screen.ExamResultView(
                        questions = examQuestions,
                        selectedAnswers = answers,
                        passingPercent = screen.passingPercent,
                        durationSeconds = elapsedSeconds
                    )
                },
                onCancel = { currentScreen = Screen.MainTabs }
            )
        }

        is Screen.ExamResultView -> {
            ExamResultScreen(
                questions = screen.questions,
                selectedAnswers = screen.selectedAnswers,
                passingPercent = screen.passingPercent,
                durationSeconds = screen.durationSeconds,
                strings = strings,
                onDone = { currentScreen = Screen.MainTabs }
            )
        }

        is Screen.Mistakes -> {
            val mistakeQuestions = questions.filter { it.lastWrong }
            QuestionListScreen(
                title = strings.recentMistakes,
                questions = mistakeQuestions,
                strings = strings,
                onPracticeAll = {
                    if (mistakeQuestions.isNotEmpty()) {
                        currentScreen = Screen.Practice(strings.recentMistakes, mistakeQuestions.map { it.id })
                    }
                },
                onBack = { currentScreen = Screen.MainTabs }
            )
        }

        is Screen.Favorites -> {
            val favQuestions = questions.filter { it.isFavorite }
            QuestionListScreen(
                title = strings.favorites,
                questions = favQuestions,
                strings = strings,
                onPracticeAll = {
                    if (favQuestions.isNotEmpty()) {
                        currentScreen = Screen.Practice(strings.favorites, favQuestions.map { it.id })
                    }
                },
                onBack = { currentScreen = Screen.MainTabs }
            )
        }

        is Screen.Settings -> {
            SettingsScreen(
                vm = vm,
                strings = strings,
                onOpenAbout = { currentScreen = Screen.About },
                onBack = { currentScreen = Screen.MainTabs }
            )
        }

        is Screen.About -> {
            AboutScreen(
                strings = strings,
                onBack = { currentScreen = Screen.MainTabs }
            )
        }
    }
}

@Composable
private fun SplashScreen(strings: AppStrings, onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1200)
        onDone()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(DzDark),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AutoQuizLogo(size = 104.dp)
            Spacer(Modifier.height(20.dp))
            Text(strings.appName, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(strings.subtitle, color = Color(0xFF94A3B8), fontSize = 14.sp)
        }
    }
}

@Composable
private fun OnboardingScreen(
    strings: AppStrings,
    isArabic: Boolean,
    onSelectLanguage: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DzSurfaceLight)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AutoQuizLogo(size = 88.dp)
        Spacer(Modifier.height(20.dp))
        Text(strings.onboardingTitle, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DzDark, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(strings.onboardingDesc, fontSize = 15.sp, color = Color(0xFF475569), textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))

        Text(strings.chooseLang, fontWeight = FontWeight.SemiBold, color = DzDark)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = isArabic,
                onClick = { onSelectLanguage("ar") },
                label = { Text("العربية (الافتراضية)") }
            )
            FilterChip(
                selected = !isArabic,
                onClick = { onSelectLanguage("fr") },
                label = { Text("Français") }
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DzGreen),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(strings.startNow, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HomeDashboard(
    vm: MainViewModel,
    strings: AppStrings,
    questions: List<QuestionEntity>,
    onContinueLearning: () -> Unit,
    onStartExamTab: () -> Unit,
    onOpenSignsTab: () -> Unit,
    onOpenLearnTab: () -> Unit,
    onOpenDailyChallenge: () -> Unit,
    onOpenMistakes: () -> Unit,
    onOpenFavorites: () -> Unit
) {
    val answered = questions.count { it.timesAnswered > 0 }
    val totalAttempts = questions.sumOf { it.timesAnswered }
    val totalCorrect = questions.sumOf { it.timesCorrect }
    val successPct = if (totalAttempts == 0) 0 else (totalCorrect * 100) / totalAttempts
    val mistakesCount = questions.count { it.lastWrong }
    val progressRatio = if (questions.isEmpty()) 0f else answered.toFloat() / questions.size.toFloat()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DzSurfaceLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DzDark),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AutoQuizLogo(size = 46.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.appName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(strings.subtitle, color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                        Surface(
                            color = DzGreen,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                "${strings.levelLabel} ${vm.prefs.currentLevel()}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = DzGreenBright,
                        trackColor = Color(0xFF334155)
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatMini("${vm.streak}", strings.streakLabel)
                        StatMini("$answered/${questions.size}", strings.completedLabel)
                        StatMini("$successPct%", strings.successRate)
                        StatMini("${vm.xp} XP", "XP")
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onContinueLearning,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DzGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(strings.continueLearning, fontSize = 13.sp, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onStartExamTab,
                    modifier = Modifier.weight(1f).height(52.dp),
                    border = BorderStroke(1.5.dp, DzGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Quiz, contentDescription = null, tint = DzGreen)
                    Spacer(Modifier.width(4.dp))
                    Text(strings.startExam, color = DzGreen, fontSize = 13.sp, maxLines = 1)
                }
            }
        }

        item {
            DashboardActionCard(
                title = strings.dailyChallenge,
                subtitle = if (vm.isArabic) "10 أسئلة يومية لكسب +30 XP إضافية" else "10 questions du jour (+30 XP bonus)",
                icon = Icons.Filled.LocalFireDepartment,
                accent = DzAmber,
                onClick = onOpenDailyChallenge
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallTileCard(
                    title = strings.roadSigns,
                    icon = Icons.Filled.Traffic,
                    color = DzGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSignsTab
                )
                SmallTileCard(
                    title = strings.categories,
                    icon = Icons.Filled.School,
                    color = DzBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenLearnTab
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallTileCard(
                    title = "${strings.recentMistakes} ($mistakesCount)",
                    icon = Icons.Filled.ErrorOutline,
                    color = DzRed,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenMistakes
                )
                SmallTileCard(
                    title = strings.favorites,
                    icon = Icons.Filled.Favorite,
                    color = DzDark,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenFavorites
                )
            }
        }
    }
}

@Composable
private fun StatMini(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(label, color = Color(0xFF94A3B8), fontSize = 11.sp)
    }
}

@Composable
private fun DashboardActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DzDark)
                Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF94A3B8))
        }
    }
}

@Composable
private fun SmallTileCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DzDark)
        }
    }
}

@Composable
private fun LearnCategoriesScreen(
    strings: AppStrings,
    questions: List<QuestionEntity>,
    onStartPractice: (String, List<Int>) -> Unit,
    onOpenMistakes: () -> Unit,
    onOpenFavorites: () -> Unit
) {
    var selectedDiff by remember { mutableStateOf("all") }

    val filteredQuestions = remember(questions, selectedDiff) {
        if (selectedDiff == "all") questions else questions.filter { it.difficulty == selectedDiff }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DzSurfaceLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(strings.categories, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DzDark)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "all" to strings.allDifficulty,
                    "easy" to strings.easy,
                    "medium" to strings.medium,
                    "hard" to strings.hard
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedDiff == key,
                        onClick = { selectedDiff = key },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val ids = filteredQuestions.shuffled().map { it.id }
                        if (ids.isNotEmpty()) onStartPractice(strings.randomPractice, ids)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DzGreen),
                    modifier = Modifier.weight(1f)
                ) { Text(strings.randomPractice, fontSize = 12.sp) }
                OutlinedButton(onClick = onOpenMistakes, modifier = Modifier.weight(1f)) {
                    Text(strings.recentMistakes, fontSize = 12.sp, color = DzRed)
                }
                OutlinedButton(onClick = onOpenFavorites) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = DzRed, modifier = Modifier.size(18.dp))
                }
            }
        }

        items(SeedData.allCategories) { cat ->
            val catQuestions = filteredQuestions.filter { it.categoryId == cat.id }
            Card(
                modifier = Modifier.fillMaxWidth().clickable(enabled = catQuestions.isNotEmpty()) {
                    onStartPractice(cat.name(strings.isArabic), catQuestions.map { it.id })
                },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cat.name(strings.isArabic), fontWeight = FontWeight.SemiBold, color = DzDark)
                        Text(
                            "${catQuestions.size} ${if (strings.isArabic) "أسئلة" else "questions"}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DzGreen)
                }
            }
        }
    }
}

@Composable
private fun QuestionPracticeScreen(
    title: String,
    questions: List<QuestionEntity>,
    strings: AppStrings,
    isDailyChallenge: Boolean,
    onToggleFavorite: (QuestionEntity) -> Unit,
    onAnswerRecorded: (Int, Boolean) -> Unit,
    onFinishDaily: (Int) -> Unit,
    onBack: () -> Unit
) {
    if (questions.isEmpty()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    }
                )
            }
        ) { p ->
            Box(Modifier.padding(p).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(strings.noQuestions)
            }
        }
        return
    }

    var index by remember { mutableIntStateOf(0) }
    var selectedOption by remember(index) { mutableStateOf<Int?>(null) }
    var sessionCorrect by remember { mutableIntStateOf(0) }
    val q = questions[index]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$title (${index + 1}/${questions.size})", fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = { onToggleFavorite(q) }) {
                        Icon(
                            if (q.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = if (q.isFavorite) DzRed else DzDark
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(DzSurfaceLight)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinearProgressIndicator(
                progress = { (index + 1).toFloat() / questions.size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = DzGreen
            )

            Surface(color = DzGreen.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                Text(
                    strings.practiceBadge,
                    color = DzGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            if (q.signCode != null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    RoadSignCanvas(signCode = q.signCode, modifier = Modifier.size(104.dp))
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Text(
                    q.text(strings.isArabic),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = DzDark,
                    modifier = Modifier.padding(16.dp)
                )
            }

            q.options(strings.isArabic).forEachIndexed { optIdx, optText ->
                val answered = selectedOption != null
                val isCorrectOpt = optIdx == q.correctIndex
                val isChosen = optIdx == selectedOption
                val bg = when {
                    !answered -> Color.White
                    isCorrectOpt -> DzGreenBright.copy(alpha = 0.18f)
                    isChosen -> DzRed.copy(alpha = 0.18f)
                    else -> Color.White
                }
                val border = when {
                    !answered -> Color(0xFFE2E8F0)
                    isCorrectOpt -> DzGreenBright
                    isChosen -> DzRed
                    else -> Color(0xFFE2E8F0)
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !answered) {
                        selectedOption = optIdx
                        val correct = optIdx == q.correctIndex
                        if (correct) sessionCorrect++
                        onAnswerRecorded(q.id, correct)
                    },
                    colors = CardDefaults.cardColors(containerColor = bg),
                    border = BorderStroke(1.5.dp, border),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(optText, modifier = Modifier.weight(1f), fontSize = 15.sp, color = DzDark)
                        if (answered && isCorrectOpt) {
                            Icon(Icons.Filled.CheckCircle, null, tint = DzGreenBright)
                        } else if (answered && isChosen) {
                            Icon(Icons.Filled.Cancel, null, tint = DzRed)
                        }
                    }
                }
            }

            if (selectedOption != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    border = BorderStroke(1.dp, DzGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(strings.explanationTitle, fontWeight = FontWeight.Bold, color = DzGreen)
                        Spacer(Modifier.height(4.dp))
                        Text(q.explanation(strings.isArabic), fontSize = 14.sp, color = DzDark)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (index > 0) {
                    OutlinedButton(onClick = { index-- }, modifier = Modifier.weight(1f)) {
                        Text(strings.previous)
                    }
                }
                Button(
                    onClick = {
                        if (index < questions.lastIndex) {
                            index++
                        } else {
                            if (isDailyChallenge) onFinishDaily(sessionCorrect)
                            onBack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DzGreen)
                ) {
                    Text(if (index < questions.lastIndex) strings.next else strings.finish)
                }
            }
        }
    }
}

@Composable
private fun ExamSetupScreen(
    strings: AppStrings,
    totalAvailable: Int,
    onStartExam: (Int, Int, Int) -> Unit
) {
    val maxQ = totalAvailable.coerceAtLeast(5)
    var questionCount by remember { mutableIntStateOf(minOf(20, maxQ)) }
    var minutes by remember { mutableIntStateOf(20) }
    var passPercent by remember { mutableIntStateOf(75) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DzSurfaceLight)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(strings.examConfigTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DzDark)

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)), border = BorderStroke(1.dp, DzAmber)) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Info, null, tint = DzAmber)
                Spacer(Modifier.width(10.dp))
                Text(strings.examDisclaimer, fontSize = 13.sp, color = DzDark)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("${strings.numQuestions}: $questionCount", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 15, 20, maxQ).distinct().filter { it <= maxQ }.forEach { opt ->
                        FilterChip(selected = questionCount == opt, onClick = { questionCount = opt }, label = { Text("$opt") })
                    }
                }

                Text("${strings.timeMinutes}: $minutes", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 15, 20, 30).forEach { opt ->
                        FilterChip(selected = minutes == opt, onClick = { minutes = opt }, label = { Text("$opt min") })
                    }
                }

                Text("${strings.passScore}: $passPercent%", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(70, 75, 80, 85).forEach { opt ->
                        FilterChip(selected = passPercent == opt, onClick = { passPercent = opt }, label = { Text("$opt%") })
                    }
                }
            }
        }

        Button(
            onClick = { onStartExam(questionCount, minutes, passPercent) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DzGreen),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Quiz, null)
            Spacer(Modifier.width(8.dp))
            Text(strings.startExam, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExamSimulatorScreen(
    questions: List<QuestionEntity>,
    durationMinutes: Int,
    strings: AppStrings,
    onSubmit: (Map<Int, Int>, Int) -> Unit,
    onCancel: () -> Unit
) {
    if (questions.isEmpty()) {
        onCancel()
        return
    }
    var index by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<Int, Int>() }
    val totalSeconds = durationMinutes * 60
    var remainingSeconds by remember { mutableIntStateOf(totalSeconds) }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
        onSubmit(answers.toMap(), totalSeconds)
    }

    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    val q = questions[index]

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Timer, null, tint = if (remainingSeconds < 60) DzRed else DzGreen)
                        Spacer(Modifier.width(6.dp))
                        Text(String.format("%02d:%02d", mins, secs), fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    TextButton(onClick = { onSubmit(answers.toMap(), totalSeconds - remainingSeconds) }) {
                        Text(strings.submitExam, color = DzGreen, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(DzSurfaceLight)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinearProgressIndicator(
                progress = { (index + 1).toFloat() / questions.size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = DzGreen
            )

            // Question navigation strip
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(questions) { idx, item ->
                    val isAnswered = answers.containsKey(item.id)
                    val isCurrent = idx == index
                    Surface(
                        modifier = Modifier.size(34.dp).clickable { index = idx },
                        shape = CircleShape,
                        color = when {
                            isCurrent -> DzGreen
                            isAnswered -> DzGreenBright.copy(alpha = 0.25f)
                            else -> Color.White
                        },
                        border = BorderStroke(1.dp, if (isCurrent) DzGreen else Color(0xFFCBD5E1))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${idx + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) Color.White else DzDark
                            )
                        }
                    }
                }
            }

            if (q.signCode != null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    RoadSignCanvas(signCode = q.signCode, modifier = Modifier.size(96.dp))
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Text(
                    q.text(strings.isArabic),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = DzDark,
                    modifier = Modifier.padding(16.dp)
                )
            }

            q.options(strings.isArabic).forEachIndexed { optIdx, text ->
                val selected = answers[q.id] == optIdx
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { answers[q.id] = optIdx },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) DzGreen.copy(alpha = 0.14f) else Color.White
                    ),
                    border = BorderStroke(1.5.dp, if (selected) DzGreen else Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text, modifier = Modifier.padding(14.dp), fontSize = 15.sp, color = DzDark)
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { if (index > 0) index-- },
                    enabled = index > 0,
                    modifier = Modifier.weight(1f)
                ) { Text(strings.previous) }
                if (index < questions.lastIndex) {
                    Button(
                        onClick = { index++ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DzGreen)
                    ) { Text(strings.next) }
                } else {
                    Button(
                        onClick = { onSubmit(answers.toMap(), totalSeconds - remainingSeconds) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DzDark)
                    ) { Text(strings.submitExam) }
                }
            }
        }
    }
}

@Composable
private fun ExamResultScreen(
    questions: List<QuestionEntity>,
    selectedAnswers: Map<Int, Int>,
    passingPercent: Int,
    durationSeconds: Int,
    strings: AppStrings,
    onDone: () -> Unit
) {
    val correct = questions.count { selectedAnswers[it.id] == it.correctIndex }
    val percent = if (questions.isEmpty()) 0 else (correct * 100) / questions.size
    val passed = percent >= passingPercent

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.detailedCorrection) },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().background(DzSurfaceLight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (passed) DzGreen else DzRed),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            if (passed) Icons.Filled.EmojiEvents else Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (passed) strings.passedText else strings.failedText,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$correct / ${questions.size} ($percent%)",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "${durationSeconds / 60}m ${durationSeconds % 60}s",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            itemsIndexed(questions) { idx, q ->
                val userIdx = selectedAnswers[q.id]
                val isRight = userIdx == q.correctIndex
                val opts = q.options(strings.isArabic)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, if (isRight) DzGreenBright else DzRed),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${idx + 1}. ${q.text(strings.isArabic)}", fontWeight = FontWeight.Bold, color = DzDark)
                        Text(
                            "${strings.yourAnswer} ${if (userIdx != null) opts[userIdx] else strings.unanswered}",
                            color = if (isRight) DzGreen else DzRed,
                            fontSize = 13.sp
                        )
                        if (!isRight) {
                            Text(
                                "${strings.correctAnswer} ${opts[q.correctIndex]}",
                                color = DzGreen,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                        Text(q.explanation(strings.isArabic), fontSize = 12.sp, color = Color(0xFF475569))
                    }
                }
            }

            item {
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DzDark)
                ) {
                    Text(strings.navHome)
                }
            }
        }
    }
}

@Composable
private fun RoadSignsScreen(strings: AppStrings, signs: List<RoadSignEntity>) {
    var selectedCat by remember { mutableStateOf("all") }
    var activeSign by remember { mutableStateOf<RoadSignEntity?>(null) }

    val filtered = remember(signs, selectedCat) {
        if (selectedCat == "all") signs else signs.filter { it.categoryId == selectedCat }
    }

    if (activeSign != null) {
        val sign = activeSign!!
        AlertDialog(
            onDismissRequest = { activeSign = null },
            confirmButton = {
                TextButton(onClick = { activeSign = null }) { Text(strings.finish) }
            },
            title = { Text(sign.name(strings.isArabic), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RoadSignCanvas(signCode = sign.code, modifier = Modifier.size(120.dp))
                    Text("${strings.meaning}: ${sign.meaning(strings.isArabic)}", fontWeight = FontWeight.SemiBold)
                    Text(sign.explanation(strings.isArabic), fontSize = 14.sp)
                    Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(10.dp)) {
                        Text(
                            "${strings.example}: ${sign.example(strings.isArabic)}",
                            fontSize = 13.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DzSurfaceLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(strings.roadSigns, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DzDark)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCat == "all",
                        onClick = { selectedCat = "all" },
                        label = { Text(strings.allDifficulty) }
                    )
                }
                items(SeedData.allSignCategories) { cat ->
                    FilterChip(
                        selected = selectedCat == cat.id,
                        onClick = { selectedCat = cat.id },
                        label = { Text(cat.name(strings.isArabic)) }
                    )
                }
            }
        }

        items(filtered) { sign ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { activeSign = sign },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoadSignCanvas(signCode = sign.code, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sign.name(strings.isArabic), fontWeight = FontWeight.Bold, color = DzDark)
                        Text(sign.meaning(strings.isArabic), fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionListScreen(
    title: String,
    questions: List<QuestionEntity>,
    strings: AppStrings,
    onPracticeAll: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$title (${questions.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    if (questions.isNotEmpty()) {
                        TextButton(onClick = onPracticeAll) {
                            Text(strings.startNow, color = DzGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (questions.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(strings.noQuestions)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().background(DzSurfaceLight),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(questions) { q ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(q.text(strings.isArabic), fontWeight = FontWeight.SemiBold, color = DzDark)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${strings.correctAnswer} ${q.options(strings.isArabic)[q.correctIndex]}",
                                color = DzGreen,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDashboardScreen(
    vm: MainViewModel,
    strings: AppStrings,
    questions: List<QuestionEntity>,
    examHistory: List<ExamResultEntity>,
    activity: Activity,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenDailyChallenge: () -> Unit
) {
    val context = LocalContext.current
    val totalAttempts = questions.sumOf { it.timesAnswered }
    val totalCorrect = questions.sumOf { it.timesCorrect }
    val successPct = if (totalAttempts == 0) 0 else (totalCorrect * 100) / totalAttempts

    val weakCats = remember(questions) {
        SeedData.allCategories.mapNotNull { cat ->
            val list = questions.filter { it.categoryId == cat.id && it.timesAnswered > 0 }
            if (list.isEmpty()) null else {
                val pct = (list.sumOf { it.timesCorrect } * 100) / list.sumOf { it.timesAnswered }
                cat to pct
            }
        }.sortedBy { it.second }.take(4)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DzSurfaceLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings.progressTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, null, tint = DzDark) }
                IconButton(onClick = onOpenAbout) { Icon(Icons.Filled.Info, null, tint = DzDark) }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = DzDark), shape = RoundedCornerShape(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatMini("${vm.prefs.currentLevel()}", strings.levelLabel)
                    StatMini("${vm.xp}", "XP")
                    StatMini("$successPct%", strings.successRate)
                    StatMini("${vm.totalStudySeconds / 60}m", strings.studyTime)
                }
            }
        }

        item {
            Button(
                onClick = {
                    RewardedAdHelper.show(
                        activity = activity,
                        onRewardEarned = {
                            vm.grantBonusXp(50)
                            Toast.makeText(context, "+50 XP!", Toast.LENGTH_SHORT).show()
                        },
                        onUnavailable = {
                            Toast.makeText(
                                context,
                                if (strings.isArabic) "الإعلان غير جاهز حالياً، حاول لاحقاً." else "Annonce indisponible pour le moment.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DzGreen)
            ) {
                Icon(Icons.Filled.Star, null)
                Spacer(Modifier.width(8.dp))
                Text(strings.bonusXpBtn)
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(strings.badgesTitle, fontWeight = FontWeight.Bold, color = DzDark)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (vm.streak >= 1) AssistChip(onClick = onOpenDailyChallenge, label = { Text("🔥 ${vm.streak}d") })
                        if (totalCorrect >= 10) AssistChip(onClick = {}, label = { Text("⭐ 10+ OK") })
                        if (examHistory.any { it.passed }) AssistChip(onClick = {}, label = { Text("🏆 Exam Pass") })
                        AssistChip(onClick = {}, label = { Text("🇩🇿 Lvl ${vm.prefs.currentLevel()}") })
                    }
                }
            }
        }

        if (weakCats.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(strings.weakCategories, fontWeight = FontWeight.Bold, color = DzDark)
                        weakCats.forEach { (cat, pct) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(cat.name(strings.isArabic), fontSize = 13.sp)
                                Text("$pct%", fontWeight = FontWeight.Bold, color = if (pct < 70) DzRed else DzGreen)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(strings.examHistory, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DzDark)
        }

        items(examHistory.take(10)) { res ->
            val pct = if (res.totalQuestions == 0) 0 else (res.correctAnswers * 100) / res.totalQuestions
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${res.correctAnswers}/${res.totalQuestions} ($pct%)", fontWeight = FontWeight.Bold)
                    Text(
                        if (res.passed) strings.passedText else strings.failedText,
                        color = if (res.passed) DzGreen else DzRed,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    vm: MainViewModel,
    strings: AppStrings,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit
) {
    var jsonUrl by remember { mutableStateOf(vm.prefs.remoteJsonUrl) }
    var showResetDialog by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        vm.setNotifications(granted)
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(strings.resetProgress) },
            text = { Text(strings.resetConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetAllProgress()
                    showResetDialog = false
                }) { Text(strings.confirm, color = DzRed) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settings) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(DzSurfaceLight)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Language, null, tint = DzGreen)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.chooseLang, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(selected = vm.isArabic, onClick = { vm.setLanguage("ar") }, label = { Text("العربية") })
                        FilterChip(selected = !vm.isArabic, onClick = { vm.setLanguage("fr") }, label = { Text("Français") })
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Notifications, null, tint = DzGreen)
                    Spacer(Modifier.width(10.dp))
                    Text(strings.notifications, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Switch(
                        checked = vm.notificationsEnabled,
                        onCheckedChange = { enable ->
                            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                vm.setNotifications(enable)
                            }
                        }
                    )
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Sync, null, tint = DzGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("${strings.contentSync} (v${vm.contentVersion})", fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = jsonUrl,
                        onValueChange = { jsonUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Raw JSON URL") }
                    )
                    Button(
                        onClick = { vm.syncRemoteContent(jsonUrl) },
                        enabled = !vm.isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = DzGreen)
                    ) {
                        Text(strings.syncNow)
                    }
                    if (vm.syncStatus.isNotBlank()) {
                        Text(vm.syncStatus, fontSize = 12.sp, color = Color(0xFF475569))
                    }
                }
            }

            OutlinedButton(
                onClick = onOpenAbout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Lock, null)
                Spacer(Modifier.width(8.dp))
                Text(strings.aboutTitle)
            }

            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, DzRed)
            ) {
                Icon(Icons.Filled.Delete, null, tint = DzRed)
                Spacer(Modifier.width(8.dp))
                Text(strings.resetProgress, color = DzRed)
            }
        }
    }
}

@Composable
private fun AboutScreen(strings: AppStrings, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.aboutTitle, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(DzSurfaceLight)
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Text(
                    strings.aboutBody,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = DzDark
                )
            }
        }
    }
}
