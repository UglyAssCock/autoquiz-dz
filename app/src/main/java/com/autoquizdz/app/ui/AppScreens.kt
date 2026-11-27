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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoquizdz.app.R
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
    data object LearnCategories : Screen()
    data object ExamSetup : Screen()
    data object RoadSigns : Screen()
    data object PrioritySection : Screen()
    data object Mistakes : Screen()
    data object Favorites : Screen()
    data object Achievements : Screen()
    data object LicenseGuide : Screen()
    data object SuccessTips : Screen()
    data object Settings : Screen()
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
            if (db.questionDao().getCount() < 100) {
                runCatching {
                    db.questionDao().upsertAll(AssetSeeder.loadQuestionsFromAssets(getApplication()))
                }
            }
            if (db.roadSignDao().getCount() < 170) {
                runCatching {
                    db.roadSignDao().upsertAll(AssetSeeder.loadSignsFromAssets(getApplication()))
                }
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
            db.questionDao().recordAnswer(questionId, if (isCorrect) 1 else 0, !isCorrect)
            prefs.recordStudyActivity(if (isCorrect) 10 else 2, secondsSpent)
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

    fun saveExamResult(totalQuestions: Int, correctAnswers: Int, passingPercent: Int, durationSeconds: Int) {
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
            syncStatus = if (isArabic) "جاري التحقق من التحديثات..." else "Synchronisation..."
            val result = ContentSyncManager.syncFromUrl(db, prefs, url.trim())
            isSyncing = false
            contentVersion = result.version
            syncStatus = if (result.success) {
                if (isArabic) "تم التحديث (v${result.version}) - أسئلة: ${result.questionsAddedOrUpdated}"
                else "Mis à jour (v${result.version}) - Questions : ${result.questionsAddedOrUpdated}"
            } else result.message
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
    val questions by vm.questions.collectAsState()
    val signs by vm.roadSigns.collectAsState()
    val examHistory by vm.examResults.collectAsState()
    val signsByCode = remember(signs) { signs.associateBy { it.code } }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Strict AdMob Policy Guard: NEVER show ads during Practice or Exam sessions
    LaunchedEffect(currentScreen) {
        AdPolicyState.isProtectedFlowActive = when (currentScreen) {
            is Screen.Practice, is Screen.ExamSession -> true
            else -> false
        }
    }

    val ar = vm.isArabic

    when (val screen = currentScreen) {
        is Screen.Splash -> SplashScreen(ar) {
            currentScreen = if (vm.prefs.onboardingDone) Screen.MainTabs else Screen.Onboarding
        }

        is Screen.Onboarding -> OnboardingScreen(
            isArabic = ar,
            onSelectLanguage = { vm.setLanguage(it) },
            onContinue = {
                vm.completeOnboarding()
                currentScreen = Screen.MainTabs
            }
        )

        is Screen.MainTabs -> {
            Scaffold(
                bottomBar = {
                    Column {
                        BannerAdView()
                        NavigationBar(containerColor = Color.White, tonalElevation = 4.dp) {
                            val tabs = listOf(
                                Triple(0, if (ar) "الرئيسية" else "Accueil", Icons.Filled.Home),
                                Triple(1, if (ar) "النتائج" else "Résultats", Icons.Filled.EmojiEvents),
                                Triple(2, if (ar) "البحث" else "Recherche", Icons.Filled.Search),
                                Triple(3, if (ar) "معلومات" else "Infos", Icons.Filled.Info),
                                Triple(4, if (ar) "حسابي" else "Compte", Icons.Filled.Person)
                            )
                            tabs.forEach { (idx, label, icon) ->
                                NavigationBarItem(
                                    selected = selectedTab == idx,
                                    onClick = { selectedTab = idx },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF2563EB),
                                        selectedTextColor = Color(0xFF2563EB),
                                        indicatorColor = Color(0xFFDBEAFE)
                                    )
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                    when (selectedTab) {
                        0 -> HomeDesignScreen(
                            vm = vm,
                            questions = questions,
                            signsCount = signs.size,
                            onOpenLearn = { currentScreen = Screen.LearnCategories },
                            onOpenExam = { currentScreen = Screen.ExamSetup },
                            onOpenPriority = { currentScreen = Screen.PrioritySection },
                            onOpenSigns = { currentScreen = Screen.RoadSigns },
                            onOpenMistakes = { currentScreen = Screen.Mistakes },
                            onOpenFavorites = { currentScreen = Screen.Favorites },
                            onOpenStats = { selectedTab = 1 },
                            onOpenDailyChallenge = {
                                val ids = questions.shuffled().take(10).map { it.id }
                                currentScreen = Screen.Practice(
                                    if (ar) "تحدي اليوم" else "Défi du jour",
                                    ids,
                                    isDailyChallenge = true
                                )
                            },
                            onOpenAchievements = { currentScreen = Screen.Achievements },
                            onOpenGuide = { currentScreen = Screen.LicenseGuide },
                            onOpenTips = { currentScreen = Screen.SuccessTips },
                            onOpenSettings = { currentScreen = Screen.Settings },
                            onToggleLanguage = { vm.setLanguage(if (ar) "fr" else "ar") }
                        )

                        1 -> ResultsStatsTab(vm, questions, examHistory)

                        2 -> SearchScreen(
                            isArabic = ar,
                            questions = questions,
                            signs = signs,
                            onPracticeQuestion = { qId ->
                                currentScreen = Screen.Practice(if (ar) "نتيجة البحث" else "Recherche", listOf(qId))
                            }
                        )

                        3 -> InfoTab(
                            isArabic = ar,
                            onOpenGuide = { currentScreen = Screen.LicenseGuide },
                            onOpenTips = { currentScreen = Screen.SuccessTips }
                        )

                        4 -> AccountTab(
                            vm = vm,
                            activity = activity,
                            onOpenSettings = { currentScreen = Screen.Settings },
                            onOpenAchievements = { currentScreen = Screen.Achievements }
                        )
                    }
                }
            }
        }

        is Screen.LearnCategories -> LearnCategoriesScreen(
            isArabic = ar,
            questions = questions,
            onStartPractice = { title, ids -> currentScreen = Screen.Practice(title, ids) },
            onBack = { currentScreen = Screen.MainTabs }
        )

        is Screen.ExamSetup -> ExamSetupScreen(
            isArabic = ar,
            totalAvailable = questions.size,
            onStartExam = { count, mins, passPct ->
                val ids = questions.shuffled().take(count).map { it.id }
                currentScreen = Screen.ExamSession(ids, mins, passPct)
            },
            onBack = { currentScreen = Screen.MainTabs }
        )

        is Screen.PrioritySection -> PrioritySectionScreen(
            isArabic = ar,
            priorityQuestions = questions.filter { it.categoryId == "priority" },
            onStartPriorityQuiz = { ids ->
                currentScreen = Screen.Practice(if (ar) "أسئلة الأولوية" else "Quiz Priorité", ids)
            },
            onBack = {
                InterstitialAdHelper.showIfAllowed(activity)
                currentScreen = Screen.MainTabs
            }
        )

        is Screen.RoadSigns -> RoadSignsLibraryScreen(
            isArabic = ar,
            signs = signs,
            onBack = {
                InterstitialAdHelper.showIfAllowed(activity)
                currentScreen = Screen.MainTabs
            }
        )

        is Screen.Mistakes -> {
            val list = questions.filter { it.lastWrong }
            QuestionListScreen(
                title = if (ar) "الأسئلة الخاطئة" else "Questions erronées",
                isArabic = ar,
                questions = list,
                onPracticeAll = {
                    if (list.isNotEmpty()) currentScreen = Screen.Practice(if (ar) "مراجعة الأخطاء" else "Erreurs", list.map { it.id })
                },
                onBack = { currentScreen = Screen.MainTabs }
            )
        }

        is Screen.Favorites -> {
            val list = questions.filter { it.isFavorite }
            QuestionListScreen(
                title = if (ar) "المفضلة" else "Favoris",
                isArabic = ar,
                questions = list,
                onPracticeAll = {
                    if (list.isNotEmpty()) currentScreen = Screen.Practice(if (ar) "أسئلتي المحفوظة" else "Favoris", list.map { it.id })
                },
                onBack = { currentScreen = Screen.MainTabs }
            )
        }

        is Screen.Achievements -> AchievementsScreen(
            isArabic = ar,
            xp = vm.xp,
            streak = vm.streak,
            questions = questions,
            examHistory = examHistory,
            onBack = { currentScreen = Screen.MainTabs }
        )

        is Screen.LicenseGuide -> LicenseGuideScreen(isArabic = ar, onBack = { currentScreen = Screen.MainTabs })
        is Screen.SuccessTips -> SuccessTipsScreen(isArabic = ar, onBack = { currentScreen = Screen.MainTabs })
        is Screen.Settings -> SettingsScreen(vm = vm, onBack = { currentScreen = Screen.MainTabs })

        is Screen.Practice -> {
            val sessionQs = remember(screen.questionIds, questions) {
                val map = questions.associateBy { it.id }
                screen.questionIds.mapNotNull { map[it] }
            }
            QuestionPracticeScreen(
                title = screen.title,
                isArabic = ar,
                questions = sessionQs,
                signsByCode = signsByCode,
                isDailyChallenge = screen.isDailyChallenge,
                onToggleFavorite = { vm.toggleFavorite(it) },
                onAnswerRecorded = { id, ok -> vm.recordAnswer(id, ok) },
                onFinishDaily = { vm.completeDailyChallenge(it) },
                onBack = { currentScreen = Screen.MainTabs }
            )
        }

        is Screen.ExamSession -> {
            val examQs = remember(screen.questionIds, questions) {
                val map = questions.associateBy { it.id }
                screen.questionIds.mapNotNull { map[it] }
            }
            ExamSimulatorScreen(
                isArabic = ar,
                questions = examQs,
                signsByCode = signsByCode,
                durationMinutes = screen.durationMinutes,
                onSubmit = { answers, elapsed ->
                    val correct = examQs.count { answers[it.id] == it.correctIndex }
                    vm.saveExamResult(examQs.size, correct, screen.passingPercent, elapsed)
                    currentScreen = Screen.ExamResultView(examQs, answers, screen.passingPercent, elapsed)
                },
                onCancel = { currentScreen = Screen.MainTabs }
            )
        }

        is Screen.ExamResultView -> ExamResultScreen(
            isArabic = ar,
            questions = screen.questions,
            selectedAnswers = screen.selectedAnswers,
            passingPercent = screen.passingPercent,
            durationSeconds = screen.durationSeconds,
            onDone = { currentScreen = Screen.MainTabs }
        )
    }
}

@Composable
private fun SplashScreen(isArabic: Boolean, onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1300)
        onDone()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DzHeaderBlue, Color(0xFF001F5C)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "AutoQUIZ DZ",
                modifier = Modifier.size(136.dp).clip(RoundedCornerShape(28.dp))
            )
            Spacer(Modifier.height(18.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) { append("AutoQUIZ ") }
                    withStyle(SpanStyle(color = Color(0xFF22C55E), fontWeight = FontWeight.ExtraBold)) { append("DZ") }
                },
                fontSize = 30.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (isArabic) "تعلم • تدرب • نجح" else "Apprendre • S'entraîner • Réussir",
                color = Color(0xFFCBD5E1),
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun OnboardingScreen(
    isArabic: Boolean,
    onSelectLanguage: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(DzSurfaceLight).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_app_logo),
            contentDescription = null,
            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(26.dp))
        )
        Spacer(Modifier.height(20.dp))
        Text(
            if (isArabic) "مرحباً بك في AutoQUIZ DZ" else "Bienvenue sur AutoQUIZ DZ",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DzDark,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (isArabic) "500 سؤال تدريبي، أكثر من 185 إشارة مرور جزائرية، قسم خاص بالأولوية والتقاطعات، ومحاكي امتحان بدون إنترنت."
            else "500 questions d'entraînement, plus de 185 panneaux algériens, section Priorité interactive et simulateur d'examen hors ligne.",
            fontSize = 14.sp,
            color = Color(0xFF475569),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(selected = isArabic, onClick = { onSelectLanguage("ar") }, label = { Text("العربية (الافتراضية)") })
            FilterChip(selected = !isArabic, onClick = { onSelectLanguage("fr") }, label = { Text("Français") })
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DzGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (isArabic) "ابدأ الآن" else "Commencer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HomeDesignScreen(
    vm: MainViewModel,
    questions: List<QuestionEntity>,
    signsCount: Int,
    onOpenLearn: () -> Unit,
    onOpenExam: () -> Unit,
    onOpenPriority: () -> Unit,
    onOpenSigns: () -> Unit,
    onOpenMistakes: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenDailyChallenge: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenTips: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleLanguage: () -> Unit
) {
    val ar = vm.isArabic
    val totalAttempts = questions.sumOf { it.timesAnswered }
    val totalCorrect = questions.sumOf { it.timesCorrect }
    val successPct = if (totalAttempts == 0) 0 else (totalCorrect * 100) / totalAttempts
    val mistakesCount = questions.count { it.lastWrong }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DzSurfaceLight),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Blue Header + Seamless Scenic Hero Banner (matches uploaded mockup)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                    .background(Brush.verticalGradient(listOf(DzHeaderBlue, DzHeaderBlueLight)))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.clickable { onToggleLanguage() }) {
                        AlgerianFlagBadge(size = 38.dp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) { append("AutoQUIZ ") }
                                withStyle(SpanStyle(color = Color(0xFF22C55E), fontWeight = FontWeight.ExtraBold)) { append("DZ") }
                            },
                            fontSize = 24.sp
                        )
                        Text(
                            if (ar) "تعلم • تدرب • نجح" else "Apprendre • S'entraîner • Réussir",
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                Image(
                    painter = painterResource(id = R.drawable.hero_banner),
                    contentDescription = "AutoQUIZ DZ Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(145.dp)
                )
            }
        }

        // 2. Row 1: تعلم قانون المرور (Blue) | امتحان تجريبي (Purple)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GradientGridCard(
                    title = if (ar) "تعلم قانون المرور" else "Apprendre le Code",
                    subtitle = if (ar) "${questions.size} سؤال مبسّط وشامل" else "${questions.size} questions complètes",
                    icon = Icons.Filled.MenuBook,
                    colors = listOf(Color(0xFF0EA5E9), Color(0xFF1D4ED8)),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenLearn
                )
                GradientGridCard(
                    title = if (ar) "امتحان تجريبي" else "Examen Blanc",
                    subtitle = if (ar) "اختبر نفسك الآن" else "Testez votre niveau",
                    icon = Icons.Filled.AssignmentTurnedIn,
                    colors = listOf(Color(0xFF7C3AED), Color(0xFF4338CA)),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenExam
                )
            }
        }

        // 3. Row 2: قسم الأولوية والتقاطعات (Teal-Emerald) | مكتبة إشارات المرور (187+) (Green)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GradientGridCard(
                    title = if (ar) "الأولوية والتقاطعات" else "Priorités & Carrefours",
                    subtitle = if (ar) "سيناريوهات تفاعلية وقواعد" else "Scénarios visuels & règles",
                    icon = Icons.Filled.ForkRight,
                    colors = listOf(Color(0xFF0D9488), Color(0xFF0F766E)),
                    badgeText = if (ar) "مهم" else "TOP",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenPriority
                )
                GradientSignsCard(
                    title = if (ar) "مكتبة إشارات المرور" else "Panneaux ($signsCount)",
                    subtitle = if (ar) "$signsCount إشارة مع الشرح" else "$signsCount panneaux expliqués",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSigns
                )
            }
        }

        // 4. Row 3: الأسئلة الخاطئة (Orange) | المفضلة (Pink-Red)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GradientGridCard(
                    title = if (ar) "الأسئلة الخاطئة" else "Mes Erreurs",
                    subtitle = if (ar) "راجع أخطاءك ($mistakesCount)" else "Réviser ($mistakesCount)",
                    icon = Icons.Filled.ErrorOutline,
                    colors = listOf(Color(0xFFFB923C), Color(0xFFEA580C)),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenMistakes
                )
                GradientGridCard(
                    title = if (ar) "المفضلة" else "Favoris",
                    subtitle = if (ar) "أسئلتك المحفوظة" else "Questions sauvées",
                    icon = Icons.Filled.Star,
                    colors = listOf(Color(0xFFF43F5E), Color(0xFFBE123C)),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenFavorites
                )
            }
        }

        // 5. Row 4: إحصائياتي (Cyan) | تحدي اليوم (Green + "جديد" badge)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GradientGridCard(
                    title = if (ar) "إحصائياتي" else "Statistiques",
                    subtitle = if (ar) "تابع تقدمك اليومي" else "Suivez vos progrès",
                    icon = Icons.Filled.BarChart,
                    colors = listOf(Color(0xFF06B6D4), Color(0xFF0284C7)),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenStats
                )
                GradientGridCard(
                    title = if (ar) "تحدي اليوم" else "Défi du Jour",
                    subtitle = if (ar) "10 أسئلة يومياً" else "10 questions / jour",
                    icon = Icons.Filled.EmojiEvents,
                    colors = listOf(Color(0xFF22C55E), Color(0xFF15803D)),
                    badgeText = if (ar) "جديد" else "NEW",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenDailyChallenge
                )
            }
        }

        // 6. Achievements Banner (Purple)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF4338CA))))
                    .clickable { onOpenAchievements() }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WorkspacePremium, null, tint = Color(0xFFFDE047), modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (ar) "الإنجازات والأوسمة" else "Succès & Badges", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(if (ar) "حقق المزيد من الإنجازات • ${vm.xp} XP • سلسلة ${vm.streak} يوم" else "${vm.xp} XP • Série ${vm.streak} j", color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
                }
            }
        }

        // 7. Success Rate & Current Level Card (matches mockup middle banner!)
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0ECFF))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.EmojiEvents, null, tint = DzAmber, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(if (ar) "نسبة نجاحك الحالية" else "Taux de réussite", fontSize = 12.sp, color = DzDark, fontWeight = FontWeight.SemiBold)
                        Text("$successPct%", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = DzDark)
                        LinearProgressIndicator(
                            progress = { (successPct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                            color = DzGreenBright,
                            trackColor = Color.White
                        )
                    }
                    Box(modifier = Modifier.padding(horizontal = 12.dp).width(1.dp).height(46.dp).background(Color(0xFF94A3B8)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.9f)) {
                        Text(if (ar) "المستوى" else "Niveau", fontSize = 12.sp, color = DzDark)
                        Text(vm.prefs.levelTitle(ar), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DzGreen)
                    }
                    Icon(Icons.Filled.Stars, null, tint = DzAmber, modifier = Modifier.size(34.dp))
                }
            }
        }

        // 8. Bottom 2 Cards: دليل رخصة السياقة (Navy) | نصائح للنجاح (Green)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactGradientTile(
                    title = if (ar) "دليل رخصة السياقة" else "Guide Permis DZ",
                    subtitle = if (ar) "كل ما تحتاج معرفته" else "Dossier & étapes",
                    icon = Icons.Filled.Description,
                    colors = listOf(Color(0xFF1E40AF), Color(0xFF1E3A8A)),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenGuide
                )
                CompactGradientTile(
                    title = if (ar) "نصائح للنجاح" else "Conseils Réussite",
                    subtitle = if (ar) "أهم النصائح العملية" else "Astuces pratiques",
                    icon = Icons.Filled.Lightbulb,
                    colors = listOf(Color(0xFF16A34A), Color(0xFF14532D)),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenTips
                )
            }
        }
    }
}

@Composable
private fun GradientGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colors: List<Color>,
    badgeText: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(116.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(colors))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        if (badgeText != null) {
            Surface(
                color = DzRed,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(badgeText, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
            }
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(34.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 1)
                Text(subtitle, color = Color.White.copy(alpha = 0.88f), fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@Composable
private fun GradientSignsCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(116.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF15803D))))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                RoadSignCanvasRaw("oct_red", "text", "STOP", Modifier.size(36.dp))
                RoadSignCanvasRaw("tri_white", "pedestrian", "", Modifier.size(36.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 1)
                Text(subtitle, color = Color.White.copy(alpha = 0.90f), fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CompactGradientTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(colors))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
            Icon(icon, null, tint = Color(0xFFFDE047), modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun LearnCategoriesScreen(
    isArabic: Boolean,
    questions: List<QuestionEntity>,
    onStartPractice: (String, List<Int>) -> Unit,
    onBack: () -> Unit
) {
    var selectedDiff by remember { mutableStateOf("all") }
    val filtered = remember(questions, selectedDiff) {
        if (selectedDiff == "all") questions else questions.filter { it.difficulty == selectedDiff }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "تعلم قانون المرور (${questions.size})" else "Catégories (${questions.size})", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DzHeaderBlue),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                }
            )
        }
    ) { p ->
        LazyColumn(
            modifier = Modifier.padding(p).fillMaxSize().background(DzSurfaceLight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "all" to (if (isArabic) "الكل" else "Tous"),
                        "easy" to (if (isArabic) "سهل" else "Facile"),
                        "medium" to (if (isArabic) "متوسط" else "Moyen"),
                        "hard" to (if (isArabic) "صعب" else "Difficile")
                    ).forEach { (k, lbl) ->
                        FilterChip(selected = selectedDiff == k, onClick = { selectedDiff = k }, label = { Text(lbl, fontSize = 12.sp) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val ids = filtered.shuffled().take(20).map { it.id }
                        if (ids.isNotEmpty()) onStartPractice(if (isArabic) "تدريب عشوائي (20 سؤال)" else "Quiz aléatoire (20)", ids)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DzGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (isArabic) "تدريب عشوائي سريع (20 سؤال)" else "Entraînement aléatoire (20)", fontWeight = FontWeight.Bold)
                }
            }

            items(CatalogMeta.allCategories) { cat ->
                val catQs = filtered.filter { it.categoryId == cat.id }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = catQs.isNotEmpty()) {
                        onStartPractice(cat.name(isArabic), catQs.map { it.id })
                    },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.name(isArabic), fontWeight = FontWeight.Bold, color = DzDark)
                            Text("${catQs.size} ${if (isArabic) "سؤال" else "questions"}", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = DzHeaderBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionPracticeScreen(
    title: String,
    isArabic: Boolean,
    questions: List<QuestionEntity>,
    signsByCode: Map<String, RoadSignEntity>,
    isDailyChallenge: Boolean,
    onToggleFavorite: (QuestionEntity) -> Unit,
    onAnswerRecorded: (Int, Boolean) -> Unit,
    onFinishDaily: (Int) -> Unit,
    onBack: () -> Unit
) {
    if (questions.isEmpty()) {
        onBack()
        return
    }
    var index by remember { mutableIntStateOf(0) }
    var selectedOpt by remember(index) { mutableStateOf<Int?>(null) }
    var sessionCorrect by remember { mutableIntStateOf(0) }
    val q = questions[index]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$title (${index + 1}/${questions.size})", fontSize = 16.sp, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DzHeaderBlue),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                },
                actions = {
                    IconButton(onClick = { onToggleFavorite(q) }) {
                        Icon(
                            if (q.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            null,
                            tint = if (q.isFavorite) Color(0xFFFDA4AF) else Color.White
                        )
                    }
                }
            )
        }
    ) { p ->
        Column(
            modifier = Modifier.padding(p).fillMaxSize().background(DzSurfaceLight).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinearProgressIndicator(
                progress = { (index + 1).toFloat() / questions.size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = DzGreen
            )

            val linkedSign = q.signCode?.let { signsByCode[it] }
            if (linkedSign != null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    RoadSignCanvas(sign = linkedSign, modifier = Modifier.size(100.dp))
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Text(q.text(isArabic), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DzDark, modifier = Modifier.padding(16.dp))
            }

            q.options(isArabic).forEachIndexed { optIdx, text ->
                val answered = selectedOpt != null
                val isCorrectOpt = optIdx == q.correctIndex
                val isChosen = optIdx == selectedOpt
                val bg = when {
                    !answered -> Color.White
                    isCorrectOpt -> DzGreenBright.copy(alpha = 0.16f)
                    isChosen -> DzRed.copy(alpha = 0.16f)
                    else -> Color.White
                }
                val border = when {
                    !answered -> Color(0xFFCBD5E1)
                    isCorrectOpt -> DzGreenBright
                    isChosen -> DzRed
                    else -> Color(0xFFCBD5E1)
                }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !answered) {
                        selectedOpt = optIdx
                        val ok = optIdx == q.correctIndex
                        if (ok) sessionCorrect++
                        onAnswerRecorded(q.id, ok)
                    },
                    colors = CardDefaults.cardColors(containerColor = bg),
                    border = BorderStroke(1.5.dp, border),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text, modifier = Modifier.weight(1f), fontSize = 15.sp, color = DzDark)
                        if (answered && isCorrectOpt) Icon(Icons.Filled.CheckCircle, null, tint = DzGreenBright)
                        else if (answered && isChosen) Icon(Icons.Filled.Cancel, null, tint = DzRed)
                    }
                }
            }

            if (selectedOpt != null) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)), border = BorderStroke(1.dp, DzGreen)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(if (isArabic) "التفسير:" else "Explication :", fontWeight = FontWeight.Bold, color = DzGreen)
                        Text(q.explanation(isArabic), fontSize = 14.sp, color = DzDark)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (index > 0) {
                    OutlinedButton(onClick = { index-- }, modifier = Modifier.weight(1f)) {
                        Text(if (isArabic) "السابق" else "Précédent")
                    }
                }
                Button(
                    onClick = {
                        if (index < questions.lastIndex) index++
                        else {
                            if (isDailyChallenge) onFinishDaily(sessionCorrect)
                            onBack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DzGreen)
                ) {
                    Text(if (index < questions.lastIndex) (if (isArabic) "التالي" else "Suivant") else (if (isArabic) "إنهاء" else "Terminer"))
                }
            }
        }
    }
}

@Composable
private fun ExamSetupScreen(
    isArabic: Boolean,
    totalAvailable: Int,
    onStartExam: (Int, Int, Int) -> Unit,
    onBack: () -> Unit
) {
    val maxQ = totalAvailable.coerceAtLeast(10)
    var count by remember { mutableIntStateOf(minOf(20, maxQ)) }
    var mins by remember { mutableIntStateOf(20) }
    var passPct by remember { mutableIntStateOf(75) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "إعدادات الامتحان التجريبي" else "Simulateur d'examen", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF5B21B6)),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                }
            )
        }
    ) { p ->
        Column(
            modifier = Modifier.padding(p).fillMaxSize().background(DzSurfaceLight).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)), border = BorderStroke(1.dp, DzAmber)) {
                Text(
                    if (isArabic) "ملاحظة: امتحان قانون المرور الرسمي في الجزائر يُجرى أمام ممتحن (CNPC). اضبط عدد الأسئلة والوقت بحرية للتدرب بأفضل شكل."
                    else "Note : l'examen officiel algérien (CNPC) se passe devant un examinateur. Configurez librement le nombre de questions et le temps.",
                    modifier = Modifier.padding(14.dp),
                    fontSize = 13.sp,
                    color = DzDark
                )
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (isArabic) "عدد الأسئلة: $count" else "Questions : $count", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 20, 30, 40).filter { it <= maxQ }.forEach { opt ->
                            FilterChip(selected = count == opt, onClick = { count = opt }, label = { Text("$opt") })
                        }
                    }
                    Text(if (isArabic) "المدة: $mins دقيقة" else "Durée : $mins min", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 15, 20, 30).forEach { opt ->
                            FilterChip(selected = mins == opt, onClick = { mins = opt }, label = { Text("$opt m") })
                        }
                    }
                    Text(if (isArabic) "نسبة النجاح: $passPct%" else "Seuil : $passPct%", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(70, 75, 80, 85).forEach { opt ->
                            FilterChip(selected = passPct == opt, onClick = { passPct = opt }, label = { Text("$opt%") })
                        }
                    }
                }
            }
            Button(
                onClick = { onStartExam(count, mins, passPct) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DzGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (isArabic) "ابدأ الامتحان الآن" else "Démarrer l'examen", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ExamSimulatorScreen(
    isArabic: Boolean,
    questions: List<QuestionEntity>,
    signsByCode: Map<String, RoadSignEntity>,
    durationMinutes: Int,
    onSubmit: (Map<Int, Int>, Int) -> Unit,
    onCancel: () -> Unit
) {
    if (questions.isEmpty()) { onCancel(); return }
    var index by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<Int, Int>() }
    val totalSecs = durationMinutes * 60
    var remaining by remember { mutableIntStateOf(totalSecs) }

    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
        onSubmit(answers.toMap(), totalSecs)
    }

    val q = questions[index]
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(String.format("%02d:%02d", remaining / 60, remaining % 60), fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (remaining < 60) DzRed else DzDark),
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                },
                actions = {
                    TextButton(onClick = { onSubmit(answers.toMap(), totalSecs - remaining) }) {
                        Text(if (isArabic) "تسليم" else "Soumettre", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { p ->
        Column(
            modifier = Modifier.padding(p).fillMaxSize().background(DzSurfaceLight).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(questions) { idx, item ->
                    val answered = answers.containsKey(item.id)
                    Surface(
                        modifier = Modifier.size(34.dp).clickable { index = idx },
                        shape = CircleShape,
                        color = if (idx == index) DzHeaderBlue else if (answered) DzGreenBright.copy(alpha = 0.25f) else Color.White,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${idx + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (idx == index) Color.White else DzDark)
                        }
                    }
                }
            }

            val linkedSign = q.signCode?.let { signsByCode[it] }
            if (linkedSign != null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    RoadSignCanvas(sign = linkedSign, modifier = Modifier.size(94.dp))
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Text(q.text(isArabic), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DzDark, modifier = Modifier.padding(16.dp))
            }

            q.options(isArabic).forEachIndexed { optIdx, text ->
                val chosen = answers[q.id] == optIdx
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { answers[q.id] = optIdx },
                    colors = CardDefaults.cardColors(containerColor = if (chosen) DzHeaderBlue.copy(alpha = 0.12f) else Color.White),
                    border = BorderStroke(1.5.dp, if (chosen) DzHeaderBlue else Color(0xFFCBD5E1)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text, modifier = Modifier.padding(14.dp), fontSize = 15.sp, color = DzDark)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { if (index > 0) index-- }, enabled = index > 0, modifier = Modifier.weight(1f)) {
                    Text(if (isArabic) "السابق" else "Précédent")
                }
                Button(
                    onClick = {
                        if (index < questions.lastIndex) index++
                        else onSubmit(answers.toMap(), totalSecs - remaining)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DzGreen)
                ) {
                    Text(if (index < questions.lastIndex) (if (isArabic) "التالي" else "Suivant") else (if (isArabic) "إنهاء وتسليم" else "Terminer"))
                }
            }
        }
    }
}

@Composable
private fun ExamResultScreen(
    isArabic: Boolean,
    questions: List<QuestionEntity>,
    selectedAnswers: Map<Int, Int>,
    passingPercent: Int,
    durationSeconds: Int,
    onDone: () -> Unit
) {
    val correct = questions.count { selectedAnswers[it.id] == it.correctIndex }
    val pct = if (questions.isEmpty()) 0 else (correct * 100) / questions.size
    val passed = pct >= passingPercent

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "نتيجة الامتحان والتصحيح" else "Résultat et correction", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (passed) DzGreen else DzRed),
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                }
            )
        }
    ) { p ->
        LazyColumn(
            modifier = Modifier.padding(p).fillMaxSize().background(DzSurfaceLight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = if (passed) DzGreen else DzRed), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (passed) (if (isArabic) "🎉 ناجح! أحسنت" else "🎉 Réussi !") else (if (isArabic) "تحتاج لمزيد من التدريب" else "À retravailler"), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("$correct / ${questions.size} ($pct%)", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${durationSeconds / 60}m ${durationSeconds % 60}s", color = Color.White.copy(alpha = 0.9f))
                    }
                }
            }
            itemsIndexed(questions) { idx, q ->
                val userIdx = selectedAnswers[q.id]
                val ok = userIdx == q.correctIndex
                val opts = q.options(isArabic)
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, if (ok) DzGreenBright else DzRed)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${idx + 1}. ${q.text(isArabic)}", fontWeight = FontWeight.Bold, color = DzDark)
                        Text(
                            "${if (isArabic) "إجابتك:" else "Votre réponse :"} ${if (userIdx != null) opts[userIdx] else (if (isArabic) "بدون إجابة" else "Aucune")}",
                            color = if (ok) DzGreen else DzRed,
                            fontSize = 13.sp
                        )
                        if (!ok) {
                            Text("${if (isArabic) "الصحيحة:" else "Correcte :"} ${opts[q.correctIndex]}", color = DzGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Text(q.explanation(isArabic), fontSize = 12.sp, color = Color(0xFF475569))
                    }
                }
            }
            item {
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = DzHeaderBlue)) {
                    Text(if (isArabic) "العودة للرئيسية" else "Retour à l'accueil")
                }
            }
        }
    }
}

@Composable
private fun RoadSignsLibraryScreen(
    isArabic: Boolean,
    signs: List<RoadSignEntity>,
    onBack: () -> Unit
) {
    var selectedCat by remember { mutableStateOf("all") }
    var search by remember { mutableStateOf("") }
    var activeSign by remember { mutableStateOf<RoadSignEntity?>(null) }

    val filtered = remember(signs, selectedCat, search) {
        signs.filter { s ->
            (selectedCat == "all" || s.categoryId == selectedCat) &&
                (search.isBlank() || s.nameAr.contains(search, true) || s.nameFr.contains(search, true) || s.code.contains(search, true))
        }
    }

    if (activeSign != null) {
        val s = activeSign!!
        AlertDialog(
            onDismissRequest = { activeSign = null },
            confirmButton = { TextButton(onClick = { activeSign = null }) { Text(if (isArabic) "إغلاق" else "Fermer") } },
            title = { Text("${s.name(isArabic)} (${s.code})", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoadSignCanvas(sign = s, modifier = Modifier.size(120.dp))
                    Text(s.meaning(isArabic), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Text(s.explanation(isArabic), fontSize = 13.sp)
                    Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(10.dp)) {
                        Text(s.example(isArabic), fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "مكتبة إشارات المرور (${signs.size})" else "Panneaux (${signs.size})", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DzGreen),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                }
            )
        }
    ) { p ->
        LazyColumn(
            modifier = Modifier.padding(p).fillMaxSize().background(DzSurfaceLight),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = DzGreen) },
                    placeholder = { Text(if (isArabic) "ابحث بين ${signs.size} إشارة..." else "Rechercher parmi ${signs.size} panneaux...") },
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = selectedCat == "all", onClick = { selectedCat = "all" }, label = { Text(if (isArabic) "الكل (${signs.size})" else "Tous (${signs.size})") })
                    }
                    items(CatalogMeta.allSignCategories) { c ->
                        val cnt = signs.count { it.categoryId == c.id }
                        FilterChip(selected = selectedCat == c.id, onClick = { selectedCat = c.id }, label = { Text("${c.name(isArabic)} ($cnt)") })
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
                        RoadSignCanvas(sign = sign, modifier = Modifier.size(62.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sign.name(isArabic), fontWeight = FontWeight.Bold, color = DzDark, fontSize = 14.sp)
                            Text(sign.meaning(isArabic), fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionListScreen(
    title: String,
    isArabic: Boolean,
    questions: List<QuestionEntity>,
    onPracticeAll: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$title (${questions.size})", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DzHeaderBlue),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                },
                actions = {
                    if (questions.isNotEmpty()) {
                        TextButton(onClick = onPracticeAll) {
                            Text(if (isArabic) "تدرب الآن" else "Pratiquer", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { p ->
        if (questions.isEmpty()) {
            Box(Modifier.padding(p).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (isArabic) "القائمة فارغة حالياً." else "Liste vide pour le moment.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(p).fillMaxSize().background(DzSurfaceLight), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(questions) { q ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(q.text(isArabic), fontWeight = FontWeight.SemiBold, color = DzDark)
                            Spacer(Modifier.height(4.dp))
                            Text(q.options(isArabic)[q.correctIndex], color = DzGreen, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsStatsTab(vm: MainViewModel, questions: List<QuestionEntity>, examHistory: List<ExamResultEntity>) {
    val ar = vm.isArabic
    val attempts = questions.sumOf { it.timesAnswered }
    val correct = questions.sumOf { it.timesCorrect }
    val pct = if (attempts == 0) 0 else (correct * 100) / attempts

    LazyColumn(modifier = Modifier.fillMaxSize().background(DzSurfaceLight), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(if (ar) "النتائج والإحصائيات" else "Résultats et Statistiques", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DzDark)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = DzHeaderBlue), shape = RoundedCornerShape(18.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$pct%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(if (ar) "النجاح" else "Réussite", color = Color.White.copy(0.8f), fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${vm.xp}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("XP", color = Color.White.copy(0.8f), fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${vm.streak}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(if (ar) "أيام متتالية" else "Série", color = Color.White.copy(0.8f), fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${vm.totalStudySeconds / 60}m", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(if (ar) "الدراسة" else "Temps", color = Color.White.copy(0.8f), fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            Text(if (ar) "سجل الامتحانات التجريبية" else "Historique des examens", fontWeight = FontWeight.Bold, color = DzDark)
        }
        if (examHistory.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                    Text(if (ar) "لم تجتز أي امتحان تجريبي بعد. ابدأ أول امتحان من الرئيسية!" else "Aucun examen terminé pour le moment.", modifier = Modifier.padding(16.dp), color = Color(0xFF64748B))
                }
            }
        } else {
            items(examHistory) { res ->
                val scorePct = if (res.totalQuestions == 0) 0 else (res.correctAnswers * 100) / res.totalQuestions
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${res.correctAnswers}/${res.totalQuestions} ($scorePct%)", fontWeight = FontWeight.Bold)
                        Text(
                            if (res.passed) (if (ar) "ناجح ✓" else "Réussi ✓") else (if (ar) "أعد المحاولة" else "Échoué"),
                            color = if (res.passed) DzGreen else DzRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoTab(isArabic: Boolean, onOpenGuide: () -> Unit, onOpenTips: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(DzSurfaceLight), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(if (isArabic) "معلومات ومصادر قانونية" else "Informations et Sources", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DzDark)
        }
        item {
            Button(onClick = onOpenGuide, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = DzHeaderBlue)) {
                Text(if (isArabic) "دليل رخصة السياقة في الجزائر" else "Guide du Permis Algérien", fontWeight = FontWeight.Bold)
            }
        }
        item {
            Button(onClick = onOpenTips, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = DzGreen)) {
                Text(if (isArabic) "نصائح ذهبية للنجاح في الامتحان" else "Conseils pour réussir", fontWeight = FontWeight.Bold)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Text(
                    if (isArabic) {
                        "المصادر والخصوصية:\n• يعتمد التطبيق على قواعد قانون المرور الجزائري (القانون 01-14 والمرسوم التنفيذي 04-381 واتفاقية فيينا لإشارات المرور).\n• جميع الأسئلة هي أسئلة تدريبية تعليمية ولا تمثل نسخة رسمية مسربة من المركز الوطني لرخص السياقة (CNPC).\n• لا يتطلب التطبيق تسجيلاً ولا يجمع بيانات شخصية؛ تُحفظ نتائجك محلياً على جهازك فقط.\n• يستخدم التطبيق إعلانات Google AdMob لدعم المجانية دون عرض أي إعلان أثناء الامتحان أو أثناء الإجابة."
                    } else {
                        "Sources & Confidentialité :\n• Basé sur la réglementation algérienne (Loi 01-14, Décret exécutif 04-381, Convention de Vienne).\n• Questions d'entraînement éducatives (non officielles CNPC).\n• Aucune inscription requise, progression stockée localement.\n• Annonces AdMob jamais affichées pendant un examen ou une question."
                    },
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = DzDark
                )
            }
        }
    }
}

@Composable
private fun AccountTab(
    vm: MainViewModel,
    activity: Activity,
    onOpenSettings: () -> Unit,
    onOpenAchievements: () -> Unit
) {
    val ar = vm.isArabic
    val ctx = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(DzSurfaceLight).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (ar) "حسابي والإعدادات" else "Mon Compte", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DzDark)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(id = R.drawable.ic_app_logo), contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("AutoQUIZ DZ", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DzDark)
                    Text("${if (ar) "المستوى:" else "Niveau :"} ${vm.prefs.levelTitle(ar)} • ${vm.xp} XP", color = DzGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Button(
            onClick = {
                RewardedAdHelper.show(
                    activity = activity,
                    onRewardEarned = {
                        vm.grantBonusXp(50)
                        Toast.makeText(ctx, "+50 XP!", Toast.LENGTH_SHORT).show()
                    },
                    onUnavailable = {
                        Toast.makeText(ctx, if (ar) "الإعلان غير جاهز حالياً" else "Pub indisponible", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DzGreen)
        ) {
            Icon(Icons.Filled.Star, null)
            Spacer(Modifier.width(8.dp))
            Text(if (ar) "شاهد إعلاناً اختيارياً (+50 XP)" else "Voir une pub (+50 XP)")
        }
        OutlinedButton(onClick = onOpenAchievements, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text(if (ar) "عرض الإنجازات والأوسمة" else "Voir mes badges")
        }
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text(if (ar) "الإعدادات (اللغة، التذكيرات، تحديث الأسئلة)" else "Paramètres (Langue, Rappels, Sync)")
        }
    }
}

@Composable
private fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ar = vm.isArabic
    var jsonUrl by remember { mutableStateOf(vm.prefs.remoteJsonUrl) }
    var showReset by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setNotifications(granted)
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(if (ar) "إعادة ضبط التقدم" else "Réinitialiser") },
            text = { Text(if (ar) "هل تريد حذف جميع الإحصائيات ونتائج الامتحانات؟" else "Effacer toutes vos statistiques ?") },
            confirmButton = {
                TextButton(onClick = { vm.resetAllProgress(); showReset = false }) {
                    Text(if (ar) "تأكيد" else "Confirmer", color = DzRed)
                }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text(if (ar) "إلغاء" else "Annuler") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ar) "الإعدادات" else "Paramètres", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DzHeaderBlue),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                }
            )
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).fillMaxSize().background(DzSurfaceLight).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (ar) "لغة التطبيق" else "Langue", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(selected = ar, onClick = { vm.setLanguage("ar") }, label = { Text("العربية") })
                        FilterChip(selected = !ar, onClick = { vm.setLanguage("fr") }, label = { Text("Français") })
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (ar) "تذكير يومي بالدراسة" else "Rappels quotidiens", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Switch(
                        checked = vm.notificationsEnabled,
                        onCheckedChange = { enable ->
                            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else vm.setNotifications(enable)
                        }
                    )
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (ar) "تحديث الأسئلة عن بعد (JSON v${vm.contentVersion})" else "Mise à jour JSON (v${vm.contentVersion})", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = jsonUrl, onValueChange = { jsonUrl = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(onClick = { vm.syncRemoteContent(jsonUrl) }, enabled = !vm.isSyncing, colors = ButtonDefaults.buttonColors(containerColor = DzGreen)) {
                        Text(if (ar) "تحديث الآن" else "Synchroniser")
                    }
                    if (vm.syncStatus.isNotBlank()) Text(vm.syncStatus, fontSize = 12.sp, color = Color(0xFF475569))
                }
            }
            OutlinedButton(onClick = { showReset = true }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, DzRed)) {
                Text(if (ar) "إعادة ضبط التقدم" else "Réinitialiser la progression", color = DzRed)
            }
        }
    }
}
