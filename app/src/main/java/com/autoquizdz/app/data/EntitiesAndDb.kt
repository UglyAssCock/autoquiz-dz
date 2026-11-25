package com.autoquizdz.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: Int,
    val categoryId: String,
    val difficulty: String, // "easy", "medium", "hard"
    val questionAr: String,
    val questionFr: String,
    val optionAAr: String,
    val optionBAr: String,
    val optionCAr: String,
    val optionDAr: String,
    val optionAFr: String,
    val optionBFr: String,
    val optionCFr: String,
    val optionDFr: String,
    val correctIndex: Int, // 0..3
    val explanationAr: String,
    val explanationFr: String,
    val signCode: String? = null,
    val isOfficialVerified: Boolean = false,
    val isFavorite: Boolean = false,
    val timesAnswered: Int = 0,
    val timesCorrect: Int = 0,
    val lastWrong: Boolean = false
) {
    fun options(isArabic: Boolean): List<String> =
        if (isArabic) listOf(optionAAr, optionBAr, optionCAr, optionDAr)
        else listOf(optionAFr, optionBFr, optionCFr, optionDFr)

    fun text(isArabic: Boolean): String = if (isArabic) questionAr else questionFr
    fun explanation(isArabic: Boolean): String = if (isArabic) explanationAr else explanationFr
}

@Entity(tableName = "road_signs")
data class RoadSignEntity(
    @PrimaryKey val code: String,
    val categoryId: String,
    val nameAr: String,
    val nameFr: String,
    val meaningAr: String,
    val meaningFr: String,
    val explanationAr: String,
    val explanationFr: String,
    val exampleAr: String,
    val exampleFr: String
) {
    fun name(isArabic: Boolean) = if (isArabic) nameAr else nameFr
    fun meaning(isArabic: Boolean) = if (isArabic) meaningAr else meaningFr
    fun explanation(isArabic: Boolean) = if (isArabic) explanationAr else explanationFr
    fun example(isArabic: Boolean) = if (isArabic) exampleAr else exampleFr
}

@Entity(tableName = "exam_results")
data class ExamResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val totalQuestions: Int,
    val correctAnswers: Int,
    val passingPercent: Int,
    val passed: Boolean,
    val durationSeconds: Int
)

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY id ASC")
    fun observeAll(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions ORDER BY id ASC")
    suspend fun getAll(): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<QuestionEntity>)

    @Query("UPDATE questions SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Int, favorite: Boolean)

    @Query(
        "UPDATE questions SET timesAnswered = timesAnswered + 1, " +
            "timesCorrect = timesCorrect + :correctDelta, " +
            "lastWrong = :isWrong WHERE id = :id"
    )
    suspend fun recordAnswer(id: Int, correctDelta: Int, isWrong: Boolean)

    @Query("UPDATE questions SET timesAnswered = 0, timesCorrect = 0, lastWrong = 0, isFavorite = 0")
    suspend fun resetAllStats()
}

@Dao
interface RoadSignDao {
    @Query("SELECT * FROM road_signs ORDER BY categoryId, code")
    fun observeAll(): Flow<List<RoadSignEntity>>

    @Query("SELECT COUNT(*) FROM road_signs")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RoadSignEntity>)
}

@Dao
interface ExamResultDao {
    @Query("SELECT * FROM exam_results ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ExamResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: ExamResultEntity)

    @Query("DELETE FROM exam_results")
    suspend fun clearAll()
}

@Database(
    entities = [QuestionEntity::class, RoadSignEntity::class, ExamResultEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun roadSignDao(): RoadSignDao
    abstract fun examResultDao(): ExamResultDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autoquiz_dz.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}

class UserPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("autoquiz_dz_prefs", Context.MODE_PRIVATE)

    var language: String
        get() = prefs.getString("language", "ar") ?: "ar"
        set(value) = prefs.edit().putString("language", value).apply()

    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(value) = prefs.edit().putBoolean("onboarding_done", value).apply()

    var xp: Int
        get() = prefs.getInt("xp", 0)
        set(value) = prefs.edit().putInt("xp", value).apply()

    var streak: Int
        get() = prefs.getInt("streak", 0)
        set(value) = prefs.edit().putInt("streak", value).apply()

    var lastStudyDate: String
        get() = prefs.getString("last_study_date", "") ?: ""
        set(value) = prefs.edit().putString("last_study_date", value).apply()

    var dailyChallengeDate: String
        get() = prefs.getString("daily_challenge_date", "") ?: ""
        set(value) = prefs.edit().putString("daily_challenge_date", value).apply()

    var totalStudySeconds: Int
        get() = prefs.getInt("study_seconds", 0)
        set(value) = prefs.edit().putInt("study_seconds", value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", false)
        set(value) = prefs.edit().putBoolean("notifications_enabled", value).apply()

    var contentVersion: Int
        get() = prefs.getInt("content_version", 1)
        set(value) = prefs.edit().putInt("content_version", value).apply()

    var remoteJsonUrl: String
        get() = prefs.getString(
            "remote_json_url",
            "https://raw.githubusercontent.com/YOUR_GITHUB_USERNAME/autoquiz-dz/main/content/questions_sample.json"
        ) ?: ""
        set(value) = prefs.edit().putString("remote_json_url", value).apply()

    fun todayKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private fun yesterdayKey(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    fun recordStudyActivity(earnedXp: Int, secondsSpent: Int) {
        val today = todayKey()
        val last = lastStudyDate
        if (last != today) {
            streak = if (last == yesterdayKey()) streak + 1 else 1
            lastStudyDate = today
        }
        xp += earnedXp
        totalStudySeconds += secondsSpent
    }

    fun currentLevel(): Int = (xp / 150) + 1

    fun resetProgress() {
        prefs.edit()
            .putInt("xp", 0)
            .putInt("streak", 0)
            .putString("last_study_date", "")
            .putString("daily_challenge_date", "")
            .putInt("study_seconds", 0)
            .apply()
    }
}
