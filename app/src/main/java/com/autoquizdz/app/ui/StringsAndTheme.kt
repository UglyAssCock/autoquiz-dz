package com.autoquizdz.app.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

val DzGreen = Color(0xFF006233)
val DzGreenBright = Color(0xFF16A34A)
val DzRed = Color(0xFFD21034)
val DzDark = Color(0xFF0F172A)
val DzSurfaceLight = Color(0xFFF8FAFC)
val DzCardLight = Color(0xFFFFFFFF)
val DzAmber = Color(0xFFF59E0B)
val DzBlue = Color(0xFF1D4ED8)

private val LightColors = lightColorScheme(
    primary = DzGreen,
    onPrimary = Color.White,
    secondary = DzGreenBright,
    onSecondary = Color.White,
    tertiary = DzDark,
    background = DzSurfaceLight,
    onBackground = DzDark,
    surface = DzCardLight,
    onSurface = DzDark,
    error = DzRed
)

private val DarkColors = darkColorScheme(
    primary = DzGreenBright,
    onPrimary = Color.White,
    secondary = DzGreen,
    background = DzDark,
    surface = Color(0xFF1E293B),
    onBackground = Color.White,
    onSurface = Color.White,
    error = DzRed
)

@Composable
fun AutoQuizTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}

/**
 * Strictly flat 2D vector logo (no shadows, no 3D gradients).
 */
@Composable
fun AutoQuizLogo(size: Dp = 64.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val r = w * 0.18f
        drawRoundRect(
            color = DzGreen,
            size = Size(w, h),
            cornerRadius = CornerRadius(r, r)
        )
        val strokeW = w * 0.08f
        val radius = w * 0.30f
        val cx = w / 2f
        val cy = h / 2f
        drawCircle(
            color = Color.White,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = strokeW)
        )
        drawLine(
            color = Color.White,
            start = Offset(cx - radius * 0.9f, cy),
            end = Offset(cx + radius * 0.9f, cy),
            strokeWidth = strokeW * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(cx, cy),
            end = Offset(cx, cy + radius * 0.9f),
            strokeWidth = strokeW * 0.85f,
            cap = StrokeCap.Round
        )
        drawCircle(color = Color.White, radius = w * 0.10f, center = Offset(cx, cy))
        drawCircle(color = DzRed, radius = w * 0.06f, center = Offset(cx, cy))
    }
}

/**
 * Accurate 2D vector Algerian / Vienna Convention Road Signs rendered natively.
 */
@Composable
fun RoadSignCanvas(signCode: String, modifier: Modifier = Modifier.size(88.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val m = w * 0.08f

        fun drawTriangleSign(fillColor: Color) {
            val path = Path().apply {
                moveTo(cx, m)
                lineTo(w - m, h - m)
                lineTo(m, h - m)
                close()
            }
            drawPath(path, color = fillColor)
            drawPath(path, color = DzRed, style = Stroke(width = w * 0.10f))
        }

        fun drawTextCenter(text: String, textColor: Int, sizeRatio: Float, yOffsetRatio: Float = 0.08f) {
            val paint = Paint().apply {
                color = textColor
                textSize = w * sizeRatio
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(text, cx, cy + w * yOffsetRatio, paint)
        }

        when (signCode) {
            "STOP" -> {
                val radius = w * 0.44f
                val oct = Path().apply {
                    for (i in 0 until 8) {
                        val angle = Math.toRadians((22.5 + i * 45.0))
                        val x = cx + (radius * cos(angle)).toFloat()
                        val y = cy + (radius * sin(angle)).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(oct, color = DzRed)
                drawPath(oct, color = Color.White, style = Stroke(width = w * 0.045f))
                drawTextCenter("STOP", android.graphics.Color.WHITE, 0.24f, 0.09f)
            }

            "YIELD" -> {
                val path = Path().apply {
                    moveTo(m, m)
                    lineTo(w - m, m)
                    lineTo(cx, h - m)
                    close()
                }
                drawPath(path, color = Color.White)
                drawPath(path, color = DzRed, style = Stroke(width = w * 0.10f))
            }

            "PRIO_ROAD" -> {
                val outer = Path().apply {
                    moveTo(cx, m)
                    lineTo(w - m, cy)
                    lineTo(cx, h - m)
                    lineTo(m, cy)
                    close()
                }
                val innerM = w * 0.20f
                val inner = Path().apply {
                    moveTo(cx, innerM)
                    lineTo(w - innerM, cy)
                    lineTo(cx, h - innerM)
                    lineTo(innerM, cy)
                    close()
                }
                drawPath(outer, color = Color.White)
                drawPath(outer, color = Color.Black, style = Stroke(width = w * 0.04f))
                drawPath(inner, color = DzAmber)
                drawPath(inner, color = Color.Black, style = Stroke(width = w * 0.025f))
            }

            "PRIO_RIGHT" -> {
                drawTriangleSign(Color.White)
                val s = w * 0.12f
                val yMid = h * 0.65f
                drawLine(Color.Black, Offset(cx - s, yMid - s), Offset(cx + s, yMid + s), strokeWidth = w * 0.07f)
                drawLine(Color.Black, Offset(cx + s, yMid - s), Offset(cx - s, yMid + s), strokeWidth = w * 0.07f)
            }

            "CURVE_RIGHT" -> {
                drawTriangleSign(Color.White)
                val p = Path().apply {
                    moveTo(cx - w * 0.08f, h * 0.78f)
                    lineTo(cx - w * 0.08f, h * 0.56f)
                    lineTo(cx + w * 0.12f, h * 0.44f)
                }
                drawPath(p, color = Color.Black, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
            }

            "PEDESTRIAN" -> {
                drawTriangleSign(Color.White)
                drawCircle(Color.Black, radius = w * 0.055f, center = Offset(cx, h * 0.48f))
                drawLine(Color.Black, Offset(cx, h * 0.54f), Offset(cx, h * 0.70f), strokeWidth = w * 0.055f)
                drawLine(Color.Black, Offset(cx - w * 0.16f, h * 0.78f), Offset(cx + w * 0.16f, h * 0.78f), strokeWidth = w * 0.05f)
            }

            "ROUNDABOUT_WARN" -> {
                drawTriangleSign(Color.White)
                drawCircle(
                    color = Color.Black,
                    radius = w * 0.15f,
                    center = Offset(cx, h * 0.64f),
                    style = Stroke(width = w * 0.06f)
                )
            }

            "TEMP_WORKS" -> {
                drawTriangleSign(Color(0xFFFACC15))
                drawCircle(Color.Black, radius = w * 0.055f, center = Offset(cx - w * 0.04f, h * 0.49f))
                drawLine(Color.Black, Offset(cx - w * 0.04f, h * 0.56f), Offset(cx + w * 0.12f, h * 0.76f), strokeWidth = w * 0.06f)
                drawLine(Color.Black, Offset(cx - w * 0.16f, h * 0.78f), Offset(cx + w * 0.18f, h * 0.78f), strokeWidth = w * 0.06f)
            }

            "NO_ENTRY" -> {
                drawCircle(color = DzRed, radius = w * 0.43f, center = Offset(cx, cy))
                drawRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.18f, cy - h * 0.09f),
                    size = Size(w * 0.64f, h * 0.18f)
                )
            }

            "LIMIT_50" -> {
                drawCircle(color = Color.White, radius = w * 0.43f, center = Offset(cx, cy))
                drawCircle(color = DzRed, radius = w * 0.43f, center = Offset(cx, cy), style = Stroke(width = w * 0.10f))
                drawTextCenter("50", android.graphics.Color.BLACK, 0.34f, 0.12f)
            }

            "LIMIT_80" -> {
                drawCircle(color = Color.White, radius = w * 0.43f, center = Offset(cx, cy))
                drawCircle(color = DzRed, radius = w * 0.43f, center = Offset(cx, cy), style = Stroke(width = w * 0.10f))
                drawTextCenter("80", android.graphics.Color.BLACK, 0.34f, 0.12f)
            }

            "NO_OVERTAKE" -> {
                drawCircle(color = Color.White, radius = w * 0.43f, center = Offset(cx, cy))
                drawCircle(color = DzRed, radius = w * 0.43f, center = Offset(cx, cy), style = Stroke(width = w * 0.10f))
                drawRoundRect(
                    color = DzRed,
                    topLeft = Offset(w * 0.22f, h * 0.38f),
                    size = Size(w * 0.22f, h * 0.24f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(w * 0.56f, h * 0.38f),
                    size = Size(w * 0.22f, h * 0.24f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }

            "NO_PARKING" -> {
                drawCircle(color = DzBlue, radius = w * 0.43f, center = Offset(cx, cy))
                drawCircle(color = DzRed, radius = w * 0.43f, center = Offset(cx, cy), style = Stroke(width = w * 0.11f))
                drawLine(DzRed, Offset(w * 0.24f, h * 0.24f), Offset(w * 0.76f, h * 0.76f), strokeWidth = w * 0.10f)
            }

            "NO_STOP_PARK" -> {
                drawCircle(color = DzBlue, radius = w * 0.43f, center = Offset(cx, cy))
                drawCircle(color = DzRed, radius = w * 0.43f, center = Offset(cx, cy), style = Stroke(width = w * 0.11f))
                drawLine(DzRed, Offset(w * 0.24f, h * 0.24f), Offset(w * 0.76f, h * 0.76f), strokeWidth = w * 0.10f)
                drawLine(DzRed, Offset(w * 0.76f, h * 0.24f), Offset(w * 0.24f, h * 0.76f), strokeWidth = w * 0.10f)
            }

            "MANDATORY_RIGHT" -> {
                drawCircle(color = DzBlue, radius = w * 0.43f, center = Offset(cx, cy))
                drawLine(Color.White, Offset(w * 0.24f, cy), Offset(w * 0.72f, cy), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
                drawLine(Color.White, Offset(w * 0.55f, h * 0.33f), Offset(w * 0.74f, cy), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
                drawLine(Color.White, Offset(w * 0.55f, h * 0.67f), Offset(w * 0.74f, cy), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
            }

            "MANDATORY_ROUNDABOUT" -> {
                drawCircle(color = DzBlue, radius = w * 0.43f, center = Offset(cx, cy))
                drawCircle(color = Color.White, radius = w * 0.24f, center = Offset(cx, cy), style = Stroke(width = w * 0.09f))
            }

            "PARKING_INFO" -> {
                drawRoundRect(color = DzBlue, topLeft = Offset(m, m), size = Size(w - 2 * m, h - 2 * m), cornerRadius = CornerRadius(12f, 12f))
                drawRoundRect(color = Color.White, topLeft = Offset(m, m), size = Size(w - 2 * m, h - 2 * m), cornerRadius = CornerRadius(12f, 12f), style = Stroke(width = w * 0.04f))
                drawTextCenter("P", android.graphics.Color.WHITE, 0.48f, 0.17f)
            }

            "HOSPITAL_INFO" -> {
                drawRoundRect(color = DzBlue, topLeft = Offset(m, m), size = Size(w - 2 * m, h - 2 * m), cornerRadius = CornerRadius(12f, 12f))
                drawRect(color = Color.White, topLeft = Offset(w * 0.24f, h * 0.24f), size = Size(w * 0.52f, h * 0.52f))
                drawLine(DzRed, Offset(cx, h * 0.30f), Offset(cx, h * 0.70f), strokeWidth = w * 0.12f)
                drawLine(DzRed, Offset(w * 0.30f, cy), Offset(w * 0.70f, cy), strokeWidth = w * 0.12f)
            }

            "SOLID_LINE" -> {
                drawRoundRect(color = Color(0xFF334155), topLeft = Offset(m, m), size = Size(w - 2 * m, h - 2 * m), cornerRadius = CornerRadius(10f, 10f))
                drawLine(Color.White, Offset(cx, m * 1.6f), Offset(cx, h - m * 1.6f), strokeWidth = w * 0.09f)
            }

            "DASHED_LINE" -> {
                drawRoundRect(color = Color(0xFF334155), topLeft = Offset(m, m), size = Size(w - 2 * m, h - 2 * m), cornerRadius = CornerRadius(10f, 10f))
                drawLine(Color.White, Offset(cx, h * 0.18f), Offset(cx, h * 0.34f), strokeWidth = w * 0.09f)
                drawLine(Color.White, Offset(cx, h * 0.44f), Offset(cx, h * 0.60f), strokeWidth = w * 0.09f)
                drawLine(Color.White, Offset(cx, h * 0.70f), Offset(cx, h * 0.84f), strokeWidth = w * 0.09f)
            }

            "TRAFFIC_LIGHTS" -> {
                drawRoundRect(color = Color(0xFF1E293B), topLeft = Offset(w * 0.30f, m), size = Size(w * 0.40f, h - 2 * m), cornerRadius = CornerRadius(16f, 16f))
                drawCircle(color = DzRed, radius = w * 0.10f, center = Offset(cx, h * 0.26f))
                drawCircle(color = DzAmber, radius = w * 0.10f, center = Offset(cx, cy))
                drawCircle(color = DzGreenBright, radius = w * 0.10f, center = Offset(cx, h * 0.74f))
            }

            else -> {
                drawCircle(color = DzGreen, radius = w * 0.40f, center = Offset(cx, cy))
                drawTextCenter("DZ", android.graphics.Color.WHITE, 0.30f, 0.11f)
            }
        }
    }
}

class AppStrings(val isArabic: Boolean) {
    val appName = "AutoQUIZ DZ"
    val subtitle = if (isArabic) "التحضير لقانون المرور الجزائري" else "Préparation au Code de la Route Algérien"
    val practiceBadge = if (isArabic) "سؤال تدريبي (غير رسمي)" else "Question d'entraînement"
    val navHome = if (isArabic) "الرئيسية" else "Accueil"
    val navLearn = if (isArabic) "تعلّم" else "Apprendre"
    val navExam = if (isArabic) "امتحان" else "Examen"
    val navSigns = if (isArabic) "الإشارات" else "Panneaux"
    val navProfile = if (isArabic) "ملفي" else "Profil"

    val chooseLang = if (isArabic) "اختر لغة التطبيق" else "Choisissez la langue"
    val startNow = if (isArabic) "ابدأ التعلم الآن" else "Commencer maintenant"
    val onboardingTitle = if (isArabic) "مرحباً بك في AutoQUIZ DZ" else "Bienvenue sur AutoQUIZ DZ"
    val onboardingDesc = if (isArabic) {
        "تدرّب على قانون المرور الجزائري بدون إنترنت، تعلّم إشارات المرور، راجع أخطاءك، وجرّب محاكي الامتحان."
    } else {
        "Préparez le code de la route algérien hors ligne : panneaux, exercices par thème, révision des erreurs et simulateur d'examen."
    }

    val continueLearning = if (isArabic) "متابعة التعلم" else "Continuer l'apprentissage"
    val startExam = if (isArabic) "ابدأ الامتحان" else "Démarrer l'examen"
    val dailyChallenge = if (isArabic) "التحدي اليومي" else "Défi quotidien"
    val roadSigns = if (isArabic) "مكتبة إشارات المرور" else "Bibliothèque des panneaux"
    val categories = if (isArabic) "أقسام التدريب" else "Catégories"
    val recentMistakes = if (isArabic) "أخطائي الأخيرة" else "Mes erreurs récentes"
    val favorites = if (isArabic) "المفضلة" else "Favoris"
    val streakLabel = if (isArabic) "أيام متتالية" else "Série (jours)"
    val completedLabel = if (isArabic) "أسئلة منجزة" else "Questions faites"
    val successRate = if (isArabic) "نسبة النجاح" else "Taux de réussite"
    val levelLabel = if (isArabic) "المستوى" else "Niveau"
    val randomPractice = if (isArabic) "تدريب عشوائي" else "Entraînement aléatoire"

    val allDifficulty = if (isArabic) "الكل" else "Tous"
    val easy = if (isArabic) "سهل" else "Facile"
    val medium = if (isArabic) "متوسط" else "Moyen"
    val hard = if (isArabic) "صعب" else "Difficile"

    val next = if (isArabic) "التالي" else "Suivant"
    val previous = if (isArabic) "السابق" else "Précédent"
    val finish = if (isArabic) "إنهاء" else "Terminer"
    val submitExam = if (isArabic) "تسليم الامتحان" else "Soumettre l'examen"
    val explanationTitle = if (isArabic) "الشرح:" else "Explication :"
    val noQuestions = if (isArabic) "لا توجد أسئلة مطابقة حالياً." else "Aucune question correspondante pour le moment."

    val examConfigTitle = if (isArabic) "إعدادات محاكي الامتحان" else "Configuration du simulateur"
    val examDisclaimer = if (isArabic) {
        "ملاحظة هامة: في الجزائر يُجرى امتحان قانون المرور رسمياً أمام الممتحِن (CNPC) وقد تختلف الصيغة حسب الولاية. لذلك يمكنك ضبط عدد الأسئلة والوقت ونسبة النجاح بحرية."
    } else {
        "Note importante : en Algérie, l'examen théorique officiel est encadré par les examinateurs (CNPC) et le format peut varier. Configurez librement le nombre de questions, le temps et le seuil de réussite."
    }
    val numQuestions = if (isArabic) "عدد الأسئلة" else "Nombre de questions"
    val timeMinutes = if (isArabic) "المدة (دقائق)" else "Durée (minutes)"
    val passScore = if (isArabic) "نسبة النجاح المطلوبة" else "Seuil de réussite"
    val passedText = if (isArabic) "ناجح! أحسنت" else "Réussi ! Bravo"
    val failedText = if (isArabic) "تحتاج لمزيد من التدريب" else "Encore un peu d'entraînement"
    val detailedCorrection = if (isArabic) "التصحيح المفصل" else "Correction détaillée"
    val yourAnswer = if (isArabic) "إجابتك:" else "Votre réponse :"
    val correctAnswer = if (isArabic) "الإجابة الصحيحة:" else "Bonne réponse :"
    val unanswered = if (isArabic) "بدون إجابة" else "Sans réponse"

    val meaning = if (isArabic) "المعنى" else "Signification"
    val example = if (isArabic) "مثال تطبيقي" else "Exemple d'utilisation"

    val progressTitle = if (isArabic) "لوحة التقدم والإحصائيات" else "Tableau de progression"
    val weakCategories = if (isArabic) "الأقسام التي تحتاج مراجعة" else "Catégories à renforcer"
    val examHistory = if (isArabic) "سجل الامتحانات" else "Historique des examens"
    val badgesTitle = if (isArabic) "الإنجازات والشارات" else "Badges et records"
    val studyTime = if (isArabic) "وقت الدراسة" else "Temps d'étude"
    val bonusXpBtn = if (isArabic) "شاهد إعلاناً اختيارياً (+50 XP)" else "Voir une pub optionnelle (+50 XP)"

    val settings = if (isArabic) "الإعدادات" else "Paramètres"
    val notifications = if (isArabic) "تذكيرات الدراسة اليومية" else "Rappels quotidiens"
    val contentSync = if (isArabic) "تحديث بنك الأسئلة (JSON)" else "Mettre à jour les questions (JSON)"
    val syncNow = if (isArabic) "تحقق من التحديثات الآن" else "Vérifier les mises à jour"
    val resetProgress = if (isArabic) "إعادة ضبط التقدم" else "Réinitialiser la progression"
    val resetConfirm = if (isArabic) "هل أنت متأكد من حذف جميع الإحصائيات والنتائج؟" else "Voulez-vous vraiment effacer toute votre progression ?"
    val confirm = if (isArabic) "تأكيد" else "Confirmer"
    val cancel = if (isArabic) "إلغاء" else "Annuler"

    val aboutTitle = if (isArabic) "حول التطبيق والمصادر والخصوصية" else "À propos, Sources et Confidentialité"
    val aboutBody = if (isArabic) {
        """
        AutoQUIZ DZ هو تطبيق تعليمي تدريبي مستقل مخصص للمقبلين على اجتياز امتحان قانون المرور في الجزائر.

        المصادر المرجعية الجزائرية:
        • القانون رقم 01-14 المؤرخ في 19 أوت 2001 المتعلق بتنظيم حركة المرور عبر الطرق وسلامتها وأمنها (المعدل والمتمم).
        • المرسوم التنفيذي رقم 04-381 المؤرخ في 28 نوفمبر 2004 المحدد لقواعد حركة المرور عبر الطرق.
        • إشارات المرور المعتمدة وفق اتفاقية فيينا والمطبقة في الجزائر (المندوبية الوطنية للأمن في الطرق DNSR).

        إخلاء مسؤولية هام:
        جميع الأسئلة المضمنة هي أسئلة تدريبية تعليمية وليست تسريباً أو نسخاً رسمياً لامتحانات المركز الوطني لرخص السياقة (CNPC). يرجى دائماً التحقق من القوانين المحدثة والغرامات لدى مدرسة تعليم سياقة معتمدة.

        سياسة الخصوصية:
        • لا يطلب التطبيق إنشاء حساب ولا يجمع أي بيانات شخصية حساسة.
        • تُحفظ نتائجك وتقدمك محلياً داخل جهازك فقط.
        • يستخدم التطبيق إعلانات Google AdMob لدعم مجانية التطبيق وفق سياسات Google Play، ولا تُعرض الإعلانات أبداً أثناء الامتحان أو أثناء الإجابة.
        """.trimIndent()
    } else {
        """
        AutoQUIZ DZ est une application éducative indépendante dédiée à la préparation du code de la route en Algérie.

        Sources réglementaires algériennes :
        • Loi n° 01-14 du 19 août 2001 relative à l'organisation, la sécurité et la police de la circulation routière (modifiée et complétée).
        • Décret exécutif n° 04-381 du 28 novembre 2004 fixant les règles de la circulation routière.
        • Signalisation routière conforme à la Convention de Vienne appliquée en Algérie (DNSR).

        Avertissement important :
        Les questions proposées sont des questions d'entraînement et ne constituent pas un examen officiel du CNPC. Vérifiez toujours les règlements et barèmes en vigueur auprès d'une auto-école agréée.

        Politique de confidentialité :
        • Aucune inscription requise, aucune collecte de données personnelles sensibles.
        • Vos progrès et scores sont stockés localement sur votre appareil.
        • Des annonces Google AdMob financent la gratuité de l'application sans jamais interrompre un examen ni une question en cours.
        """.trimIndent()
    }
}
