package com.autoquizdz.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class CategoryDef(
    val id: String,
    val nameAr: String,
    val nameFr: String
) {
    fun name(isArabic: Boolean) = if (isArabic) nameAr else nameFr
}

data class SignCategoryDef(
    val id: String,
    val nameAr: String,
    val nameFr: String
) {
    fun name(isArabic: Boolean) = if (isArabic) nameAr else nameFr
}

object SeedData {

    val allCategories = listOf(
        CategoryDef("road_signs", "إشارات المرور", "Panneaux de signalisation"),
        CategoryDef("priority", "قواعد الأولوية", "Règles de priorité"),
        CategoryDef("traffic_lights", "الإشارات الضوئية", "Feux de signalisation"),
        CategoryDef("road_markings", "العلامات الأرضية", "Marquage au sol"),
        CategoryDef("speed_limits", "حدود السرعة", "Limitations de vitesse"),
        CategoryDef("overtaking", "التجاوز", "Dépassement"),
        CategoryDef("parking", "التوقف", "Stationnement"),
        CategoryDef("stopping", "الوقوف", "Arrêt"),
        CategoryDef("roundabouts", "المحاور الدورانية", "Ronds-points"),
        CategoryDef("intersections", "تقاطعات الطرق", "Intersections"),
        CategoryDef("pedestrians", "الراجلون", "Piétons"),
        CategoryDef("motorcycles", "الدراجات النارية", "Motocycles"),
        CategoryDef("highway", "الطريق السيار", "Autoroute"),
        CategoryDef("safety_distances", "مسافات الأمان", "Distances de sécurité"),
        CategoryDef("weather", "الأحوال الجوية", "Conditions météorologiques"),
        CategoryDef("night_driving", "السياقة الليلية", "Conduite de nuit"),
        CategoryDef("vehicle_safety", "سلامة المركبة", "Sécurité du véhicule"),
        CategoryDef("driver_resp", "مسؤوليات السائق", "Responsabilités du conducteur"),
        CategoryDef("first_aid", "الإسعافات الأولية", "Premiers secours"),
        CategoryDef("accidents", "حوادث المرور", "Accidents de la route"),
        CategoryDef("dangerous_situations", "الوضعيات الخطيرة", "Situations dangereuses")
    )

    val allSignCategories = listOf(
        SignCategoryDef("warning", "إشارات الخطر", "Panneaux de danger"),
        SignCategoryDef("prohibition", "إشارات المنع", "Panneaux d'interdiction"),
        SignCategoryDef("mandatory", "إشارات الإجبار", "Panneaux d'obligation"),
        SignCategoryDef("information", "إشارات الإرشاد", "Panneaux d'indication"),
        SignCategoryDef("priority", "إشارات الأولوية", "Panneaux de priorité"),
        SignCategoryDef("temporary", "الإشارات المؤقتة", "Signalisation temporaire"),
        SignCategoryDef("markings", "العلامات الأرضية", "Marquage au sol"),
        SignCategoryDef("lights", "الإشارات الضوئية", "Feux tricolores")
    )

    // Practice questions based on Algerian traffic rules (Décret exécutif 04-381 & Loi 01-14).
    // Clearly flagged as practice questions (isOfficialVerified = false) unless officially certified.
    fun initialQuestions(): List<QuestionEntity> = listOf(
        QuestionEntity(
            id = 1,
            categoryId = "road_signs",
            difficulty = "easy",
            questionAr = "ماذا تعني هذه الإشارة الحمراء ثمانية الأضلاع (قف)؟",
            questionFr = "Que signifie ce panneau octogonal rouge (STOP) ?",
            optionAAr = "التخفيض من السرعة فقط دون توقف",
            optionBAr = "الوقوف الإجباري التام وترك الأولوية لليمين واليسار",
            optionCAr = "ممنوع التجاوز في هذا المقطع",
            optionDAr = "أولوية المرور للمركبة القادمة من الأمام",
            optionAFr = "Ralentir sans obligation d'arrêt",
            optionBFr = "Arrêt obligatoire absolu et céder le passage à gauche et à droite",
            optionCFr = "Interdiction de dépasser sur ce tronçon",
            optionDFr = "Priorité au véhicule venant en face",
            correctIndex = 1,
            explanationAr = "إشارة قف (STOP) تفرض الوقوف التام عند خط الوقوف أو حد التقاطع وإفساح الطريق للمركبات القادمة من اليمين واليسار قبل الانطلاق.",
            explanationFr = "Le panneau STOP impose un arrêt absolu à la ligne d'arrêt ou à la limite de l'intersection, en cédant le passage aux véhicules venant de gauche et de droite.",
            signCode = "STOP"
        ),
        QuestionEntity(
            id = 2,
            categoryId = "road_signs",
            difficulty = "easy",
            questionAr = "ماذا تعني الإشارة المثلثة ذات الإطار الأحمر والخلفية الصفراء؟",
            questionFr = "Que signifie un panneau triangulaire à bordure rouge et fond jaune ?",
            optionAAr = "إشارة إرشاد دائمة",
            optionBAr = "إشارة خطر مؤقت (مثل أشغال عمومية)",
            optionCAr = "إشارة طريق سيار",
            optionDAr = "إشارة نهاية المنع",
            optionAFr = "Un panneau d'indication permanent",
            optionBFr = "Un panneau de danger temporaire (ex. travaux)",
            optionCFr = "Un panneau d'autoroute",
            optionDFr = "Un panneau de fin d'interdiction",
            correctIndex = 1,
            explanationAr = "في قانون المرور الجزائري، الخلفية الصفراء في إشارات الخطر تشير إلى خطر مؤقت مثل الأشغال العمومية ويجب تخفيض السرعة.",
            explanationFr = "Dans le code de la route algérien, le fond jaune signale un danger temporaire (travaux, chaussée rétrécie) nécessitant de ralentir.",
            signCode = "TEMP_WORKS"
        ),
        QuestionEntity(
            id = 3,
            categoryId = "priority",
            difficulty = "easy",
            questionAr = "عند تقاطع طرق بدون أي إشارة مرور أو شرطي، لمن تكون الأولوية في الجزائر؟",
            questionFr = "À une intersection sans signalisation ni agent, qui a la priorité en Algérie ?",
            optionAAr = "للمركبة القادمة من اليسار",
            optionBAr = "للمركبة الأكبر حجماً",
            optionCAr = "للمركبة القادمة من اليمين",
            optionDAr = "للمركبة الأسرع",
            optionAFr = "Le véhicule venant de gauche",
            optionBFr = "Le véhicule le plus volumineux",
            optionCFr = "Le véhicule venant de droite",
            optionDFr = "Le véhicule le plus rapide",
            correctIndex = 2,
            explanationAr = "القاعدة العامة عند غياب الإشارات في التقاطعات هي الأولوية لليمين (المادة 13 وما يليها من المرسوم التنفيذي 04-381).",
            explanationFr = "En l'absence de signalisation à une intersection, la règle générale est la priorité à droite (Décret exécutif 04-381).",
            signCode = "PRIO_RIGHT"
        ),
        QuestionEntity(
            id = 4,
            categoryId = "priority",
            difficulty = "medium",
            questionAr = "عندما ترى إشارة المعين الأصفر (طريق ذو أولوية)، ماذا يعني ذلك؟",
            questionFr = "Lorsque vous rencontrez le losange jaune (route prioritaire), que cela signifie-t-il ?",
            optionAAr = "أنت في طريق مضمون الأولوية في التقاطعات القادمة مع بقاء الحذر",
            optionBAr = "يجب ترك الأولوية لليمين في كل تقاطع",
            optionCAr = "ممنوع الوقوف على طول الطريق",
            optionDAr = "الطريق ينتهي قريباً",
            optionAFr = "Vous circulez sur une route prioritaire aux prochaines intersections tout en restant prudent",
            optionBFr = "Vous devez céder la priorité à droite à chaque intersection",
            optionCFr = "Le stationnement est interdit sur toute la route",
            optionDFr = "La route se termine bientôt",
            correctIndex = 0,
            explanationAr = "إشارة المعين الأصفر تؤكد أنك تسير في طريق ذي أولوية حتى ظهور إشارة نهاية الطريق ذي الأولوية، مع ضرورة التزام الحذر.",
            explanationFr = "Le losange jaune indique que vous êtes sur une route prioritaire jusqu'au panneau de fin de priorité, tout en restant vigilant.",
            signCode = "PRIO_ROAD"
        ),
        QuestionEntity(
            id = 5,
            categoryId = "traffic_lights",
            difficulty = "easy",
            questionAr = "ماذا يجب أن تفعل عند اشتعال الضوء البرتقالي (الأصفر) الثابت؟",
            questionFr = "Que devez-vous faire lorsque le feu orange (jaune) fixe s'allume ?",
            optionAAr = "زيادة السرعة للمرور قبل الضوء الأحمر",
            optionBAr = "التوقف إلا إذا كانت المسافة قصيرة جداً بحيث يشكل التوقف خطراً",
            optionCAr = "المرور دائماً دون التوقف",
            optionDAr = "تشغيل أضواء الطوارئ والعبور",
            optionAFr = "Accélérer pour passer avant le feu rouge",
            optionBFr = "S'arrêter, sauf si l'arrêt brusque présente un danger",
            optionCFr = "Passer systématiquement sans s'arrêter",
            optionDFr = "Allumer les feux de détresse et traverser",
            correctIndex = 1,
            explanationAr = "الضوء الأصفر الثابت يعلن عن اشتعال الضوء الأحمر ويفرض التوقف، إلا إذا لم يكن بالإمكان التوقف بأمان.",
            explanationFr = "Le feu orange fixe annonce le feu rouge et impose l'arrêt, sauf si la distance ne permet pas de s'arrêter en sécurité.",
            signCode = "TRAFFIC_LIGHTS"
        ),
        QuestionEntity(
            id = 6,
            categoryId = "road_markings",
            difficulty = "easy",
            questionAr = "هل يسمح بتجاوز الخط الأبيض المتواصل المرسوم وسط الطريق؟",
            questionFr = "Est-il permis de franchir une ligne blanche continue au centre de la chaussée ?",
            optionAAr = "نعم، إذا كانت الطريق فارغة",
            optionBAr = "نعم، لتجاوز شاحنة بطيئة فقط",
            optionCAr = "لا، يمنع قطعياً تجاوزه أو السير فوقه",
            optionDAr = "نعم، في الليل فقط",
            optionAFr = "Oui, si la route est dégagée",
            optionBFr = "Oui, uniquement pour dépasser un camion lent",
            optionCFr = "Non, il est strictement interdit de la franchir ou de la chevaucher",
            optionDFr = "Oui, uniquement la nuit",
            correctIndex = 2,
            explanationAr = "الخط الأبيض المتواصل يُعامل كجدار افتراضي يمنع تجاوزه أو الدوس عليه في جميع الظروف.",
            explanationFr = "La ligne blanche continue équivaut à un mur virtuel : il est strictement interdit de la franchir ou de la chevaucher.",
            signCode = "SOLID_LINE"
        ),
        QuestionEntity(
            id = 7,
            categoryId = "speed_limits",
            difficulty = "easy",
            questionAr = "ما هي السرعة القصوى القانونية داخل التجمعات السكانية في الجزائر (في الطقس الجاف دون إشارة مخالفة)؟",
            questionFr = "Quelle est la vitesse maximale autorisée en agglomération en Algérie (temps sec, sans panneau contraire) ?",
            optionAAr = "40 كلم/سا",
            optionBAr = "50 كلم/سا",
            optionCAr = "60 كلم/سا",
            optionDAr = "70 كلم/سا",
            optionAFr = "40 km/h",
            optionBFr = "50 km/h",
            optionCFr = "60 km/h",
            optionDFr = "70 km/h",
            correctIndex = 1,
            explanationAr = "طبقاً للمرسوم التنفيذي 04-381، تحدد السرعة القصوى داخل التجمعات السكانية بـ 50 كلم/سا في الطقس العادي (وتخفض إلى 40 كلم/سا عند المطر).",
            explanationFr = "Selon le décret exécutif 04-381, la limite en agglomération est de 50 km/h par temps sec (et 40 km/h par temps de pluie).",
            signCode = "LIMIT_50"
        ),
        QuestionEntity(
            id = 8,
            categoryId = "speed_limits",
            difficulty = "medium",
            questionAr = "ما هي السرعة القصوى المسموح بها لحامل رخصة السياقة الاختبارية (الجدد) في الجزائر؟",
            questionFr = "Quelle est la vitesse maximale autorisée pour un conducteur titulaire d'un permis probatoire en Algérie ?",
            optionAAr = "60 كلم/سا",
            optionBAr = "80 كلم/سا",
            optionCAr = "100 كلم/سا",
            optionDAr = "120 كلم/سا",
            optionAFr = "60 km/h",
            optionBFr = "80 km/h",
            optionCFr = "100 km/h",
            optionDFr = "120 km/h",
            correctIndex = 1,
            explanationAr = "يلزم حامل رخصة السياقة الاختبارية (خلال السنتين الأوليين) بعدم تجاوز سرعة 80 كلم/سا حتى على الطريق السيار، مع وضع شارة 80.",
            explanationFr = "Le titulaire du permis probatoire (2 premières années) ne doit pas dépasser 80 km/h même sur autoroute et doit apposer le disque 80.",
            signCode = "LIMIT_80"
        ),
        QuestionEntity(
            id = 9,
            categoryId = "overtaking",
            difficulty = "medium",
            questionAr = "من أي جهة يتم التجاوز عادةً في قانون المرور الجزائري؟",
            questionFr = "De quel côté s'effectue normalement le dépassement selon le code de la route algérien ?",
            optionAAr = "من جهة اليمين دائماً",
            optionBAr = "من جهة اليسار، باستثناء حالات خاصة (مثل مركبة تنعطف يساراً)",
            optionCAr = "من أي جهة متاحة",
            optionDAr = "فقط عبر شريط التوقف الاستعجالي",
            optionAFr = "Toujours par la droite",
            optionBFr = "Par la gauche, sauf exceptions (ex. véhicule tournant à gauche)",
            optionCFr = "De n'importe quel côté libre",
            optionDFr = "Uniquement par la bande d'arrêt d'urgence",
            correctIndex = 1,
            explanationAr = "التجاوز يتم من اليسار بعد التأكد من خلو الطريق وتشغيل الغماز، ويستثنى التجاوز من اليمين إذا كانت المركبة الأمامية قد أشارت للانعطاف يساراً وتوفرت المساحة الآمنة.",
            explanationFr = "Le dépassement se fait par la gauche après vérification et clignotant ; par la droite uniquement si le véhicule devant indique clairement tourner à gauche avec espace suffisant.",
            signCode = "NO_OVERTAKE"
        ),
        QuestionEntity(
            id = 10,
            categoryId = "parking",
            difficulty = "easy",
            questionAr = "ماذا تعني الدائرة الزرقاء المحاطة بإطار أحمر وبها خط مائل أحمر واحد؟",
            questionFr = "Que signifie le disque bleu cerclé de rouge barré d'une seule diagonale rouge ?",
            optionAAr = "ممنوع الوقوف والتوقف معاً",
            optionBAr = "ممنوع التوقف (يسمح بالوقوف القصير لصعود أو نزول الركاب)",
            optionCAr = "موقف سيارات مجاني",
            optionDAr = "اتجاه ممنوع",
            optionAFr = "Arrêt et stationnement interdits",
            optionBFr = "Stationnement interdit (l'arrêt bref reste autorisé)",
            optionCFr = "Parking gratuit",
            optionDFr = "Sens interdit",
            correctIndex = 1,
            explanationAr = "خط مائل واحد يعني منع التوقف (Stationnement) فقط، بينما يبقى الوقوف المؤقت (Arrêt) لإنزال أو إركاب شخص مع بقاء السائق قرب المركبة مسموحاً.",
            explanationFr = "Une seule barre rouge interdit le stationnement, mais l'arrêt bref (montée/descente de passagers, conducteur à proximité) reste autorisé.",
            signCode = "NO_PARKING"
        ),
        QuestionEntity(
            id = 11,
            categoryId = "stopping",
            difficulty = "easy",
            questionAr = "ماذا تعني الدائرة الزرقاء المحاطة بإطار أحمر وبها علامة X حمراء؟",
            questionFr = "Que signifie le panneau bleu cerclé de rouge barré d'une croix rouge (X) ?",
            optionAAr = "ممنوع التوقف فقط",
            optionBAr = "ممنوع الوقوف والتوقف معاً",
            optionCAr = "تقاطع طرق خطر",
            optionDAr = "نهاية منطقة التوقف",
            optionAFr = "Stationnement interdit uniquement",
            optionBFr = "Arrêt et stationnement strictement interdits",
            optionCFr = "Intersection dangereuse",
            optionDFr = "Fin de zone de stationnement",
            correctIndex = 1,
            explanationAr = "علامة X الحمراء تمنع كلاً من الوقوف (حتى لثوانٍ) والتوقف على جانب الطريق المعني بالإشارة.",
            explanationFr = "La croix rouge interdit à la fois l'arrêt (même bref) et le stationnement du côté où le panneau est implanté.",
            signCode = "NO_STOP_PARK"
        ),
        QuestionEntity(
            id = 12,
            categoryId = "roundabouts",
            difficulty = "medium",
            questionAr = "عند الدخول إلى محور دوراني (Rond-point) مسبوق بإشارة إفساح الطريق، لمن تكون الأولوية؟",
            questionFr = "À l'entrée d'un rond-point précédé d'un panneau Cédez le passage, qui a la priorité ?",
            optionAAr = "للمركبات التي تدخل المحور",
            optionBAr = "للمركبات الموجودة داخل المحور والقادمة من اليسار",
            optionCAr = "للمركبات القادمة من اليمين",
            optionDAr = "للدراجات فقط",
            optionAFr = "Les véhicules qui entrent dans le rond-point",
            optionBFr = "Les véhicules déjà engagés dans l'anneau venant de gauche",
            optionCFr = "Les véhicules venant de droite",
            optionDFr = "Les cyclistes uniquement",
            correctIndex = 1,
            explanationAr = "في المحور الدوراني المزود بإشارة إفساح الطريق، يجب ترك الأولوية للمركبات التي تسير فعلياً داخل الدوار (القادمة من يسارك).",
            explanationFr = "Dans un carrefour à sens giratoire avec Cédez le passage, la priorité appartient aux usagers déjà engagés dans l'anneau (venant de gauche).",
            signCode = "ROUNDABOUT_WARN"
        ),
        QuestionEntity(
            id = 13,
            categoryId = "intersections",
            difficulty = "medium",
            questionAr = "عند الوصول إلى تقاطع بإشارة إفساح الطريق (المثلث المقلوب) وكانت الطريق الرئيسية خالية تماماً، هل الوقوف التام إجباري؟",
            questionFr = "À une intersection avec panneau Cédez le passage (triangle inversé), si la voie est totalement libre, l'arrêt absolu est-il obligatoire ?",
            optionAAr = "نعم، يجب التوقف التام دائماً مثل إشارة قف",
            optionBAr = "لا، يكفي التخفيض من السرعة والتأكد من خلو الطريق ثم العبور بحذر",
            optionCAr = "يجب تشغيل المنبه الصوتي قبل العبور",
            optionDAr = "يمنع الدوران إلى اليسار",
            optionAFr = "Oui, l'arrêt absolu est toujours obligatoire comme au STOP",
            optionBFr = "Non, il faut ralentir, vérifier que la voie est libre puis passer prudemment",
            optionCFr = "Il faut obligatoirement klaxonner avant de passer",
            optionDFr = "Il est interdit de tourner à gauche",
            correctIndex = 1,
            explanationAr = "الفرق الأساسي بين إفساح الطريق وإشارة قف هو أن إفساح الطريق لا يفرض التوقف التام إذا كانت الطريق خالية، لكنه يفرض التمهل والاستعداد للتوقف.",
            explanationFr = "Contrairement au STOP, le Cédez le passage n'impose pas l'arrêt complet si aucun véhicule prioritaire n'arrive, mais impose de ralentir et d'être prêt à s'arrêter.",
            signCode = "YIELD"
        ),
        QuestionEntity(
            id = 14,
            categoryId = "pedestrians",
            difficulty = "easy",
            questionAr = "عندما يشرع راجل في عبور ممر الراجلين، ما هو واجب السائق؟",
            questionFr = "Lorsqu'un piéton s'engage sur un passage piéton, quelle est l'obligation du conducteur ?",
            optionAAr = "استعمال المنبه الصوتي لينتظر الراجل",
            optionBAr = "التوقف التام وترك الأولوية للراجل ليعبر بأمان",
            optionCAr = "تجاوز المركبة المتوقفة أمام الممر",
            optionDAr = "تخفيض السرعة قليلاً والمرور بسرعة",
            optionAFr = "Klaxonner pour faire attendre le piéton",
            optionBFr = "S'arrêter et céder le passage au piéton en toute sécurité",
            optionCFr = "Dépasser le véhicule arrêté devant le passage",
            optionDFr = "Ralentir légèrement et passer rapidement",
            correctIndex = 1,
            explanationAr = "الراجل الذي يشرع في العبور على ممر الراجلين له أولوية مطلقة، كما يُمنع تجاوز أي مركبة توقفت لتركه يمر.",
            explanationFr = "Le piéton engagé sur un passage protégé est prioritaire ; il est également interdit de dépasser un véhicule arrêté pour le laisser traverser.",
            signCode = "PEDESTRIAN"
        ),
        QuestionEntity(
            id = 15,
            categoryId = "motorcycles",
            difficulty = "medium",
            questionAr = "ما هي المسافة الجانبية الدنيا الموصى بها عند تجاوز دراجة نارية أو هوائية خارج التجمعات السكانية؟",
            questionFr = "Quelle distance latérale minimale est recommandée pour dépasser un deux-roues hors agglomération ?",
            optionAAr = "0.5 متر",
            optionBAr = "1.5 متر على الأقل",
            optionCAr = "0.3 متر",
            optionDAr = "لا توجد مسافة محددة",
            optionAFr = "0,5 mètre",
            optionBFr = "Au moins 1,5 mètre",
            optionCFr = "0,3 mètre",
            optionDFr = "Aucune distance particulière",
            correctIndex = 1,
            explanationAr = "لضمان سلامة مستعملي الدراجات وتفادي أثر تيار الهواء، يجب ترك مسافة جانبية لا تقل عن 1 متر داخل المدينة و1.5 متر خارج التجمعات السكانية.",
            explanationFr = "Pour protéger les deux-roues des écarts et du souffle d'air, il faut laisser au moins 1 m en ville et 1,5 m hors agglomération."
        ),
        QuestionEntity(
            id = 16,
            categoryId = "highway",
            difficulty = "medium",
            questionAr = "ما هي السرعة القصوى القانونية على الطريق السيار في الجزائر في الطقس الجاف (للسائقين غير المبتدئين)؟",
            questionFr = "Quelle est la vitesse maximale légale sur autoroute en Algérie par temps sec (conducteurs confirmés) ?",
            optionAAr = "100 كلم/سا",
            optionBAr = "110 كلم/سا",
            optionCAr = "120 كلم/سا",
            optionDAr = "130 كلم/سا",
            optionAFr = "100 km/h",
            optionBFr = "110 km/h",
            optionCFr = "120 km/h",
            optionDFr = "130 km/h",
            correctIndex = 2,
            explanationAr = "السرعة القصوى على الطريق السيار في الجزائر هي 120 كلم/سا في الطقس الجاف وتخفض إلى 100 كلم/سا أثناء المطر (و80 كلم/سا للرخصة الاختبارية).",
            explanationFr = "Sur autoroute en Algérie, la limite est de 120 km/h par temps sec et 100 km/h par temps de pluie (80 km/h pour le permis probatoire)."
        ),
        QuestionEntity(
            id = 17,
            categoryId = "highway",
            difficulty = "easy",
            questionAr = "متى يُسمح بالتوقف على شريط التوقف الاستعجالي في الطريق السيار؟",
            questionFr = "Quand est-il permis de s'arrêter sur la bande d'arrêt d'urgence d'une autoroute ?",
            optionAAr = "للرد على مكالمة هاتفية",
            optionBAr = "للراحة أو تناول الطعام",
            optionCAr = "فقط في حالة عطب ميكانيكي أو طارئ صحي خطير",
            optionDAr = "لتجاوز الازدحام المروري",
            optionAFr = "Pour répondre à un appel téléphonique",
            optionBFr = "Pour se reposer ou manger",
            optionCFr = "Uniquement en cas de panne ou d'urgence grave",
            optionDFr = "Pour contourner un embouteillage",
            correctIndex = 2,
            explanationAr = "شريط التوقف الاستعجالي مخصص حصرياً لحالات العطب أو الطوارئ القصوى ومرور مركبات الإسعاف والأمن.",
            explanationFr = "La bande d'arrêt d'urgence est strictement réservée aux pannes, urgences graves et véhicules de secours."
        ),
        QuestionEntity(
            id = 18,
            categoryId = "safety_distances",
            difficulty = "medium",
            questionAr = "ما هي القاعدة الزمنية العملية لتقدير مسافة الأمان الدنيا خلف المركبة التي أمامك في الطقس الجاف؟",
            questionFr = "Quelle est la règle temporelle pratique pour évaluer la distance de sécurité minimale par temps sec ?",
            optionAAr = "نصف ثانية",
            optionBAr = "ثانيتان على الأقل",
            optionCAr = "خمس ثوانٍ دائماً",
            optionDAr = "طول سيارة واحدة فقط",
            optionAFr = "Une demi-seconde",
            optionBFr = "Au moins 2 secondes",
            optionCFr = "Toujours 5 secondes",
            optionDFr = "Juste la longueur d'une voiture",
            correctIndex = 1,
            explanationAr = "ترك فاصل زمني لا يقل عن ثانيتين يمنح السائق وقتاً كافياً لرد الفعل والفرملة، ويجب مضاعفتها عند المطر أو الضباب.",
            explanationFr = "Un intervalle minimal de 2 secondes laisse le temps de réagir et de freiner ; il doit être augmenté sous la pluie ou le brouillard."
        ),
        QuestionEntity(
            id = 19,
            categoryId = "weather",
            difficulty = "medium",
            questionAr = "أثناء هطول المطر على طريق عادي خارج التجمعات السكانية في الجزائر، إلى كم تنخفض السرعة القصوى (من 80 كلم/سا)؟",
            questionFr = "Par temps de pluie sur route ordinaire hors agglomération en Algérie, à combien descend la limite (normalement 80 km/h) ?",
            optionAAr = "70 كلم/سا",
            optionBAr = "60 كلم/سا",
            optionCAr = "50 كلم/سا",
            optionDAr = "تبقى 80 كلم/سا",
            optionAFr = "70 km/h",
            optionBFr = "60 km/h",
            optionCFr = "50 km/h",
            optionDFr = "Elle reste à 80 km/h",
            correctIndex = 1,
            explanationAr = "وفق المرسوم التنفيذي 04-381، تنخفض السرعة القصوى عند المطر من 80 إلى 60 كلم/سا خارج التجمعات السكانية، ومن 120 إلى 100 كلم/سا على الطريق السيار.",
            explanationFr = "Selon le décret 04-381, par temps de pluie la limite passe de 80 à 60 km/h hors agglomération, et de 120 à 100 km/h sur autoroute."
        ),
        QuestionEntity(
            id = 20,
            categoryId = "night_driving",
            difficulty = "easy",
            questionAr = "عندما تقترب من مركبة قادمة في الاتجاه المعاكس ليلاً خارج المدينة، أي أضواء يجب استعمالها؟",
            questionFr = "La nuit hors agglomération, à l'approche d'un véhicule venant en sens inverse, quels feux utiliser ?",
            optionAAr = "أضواء الطريق (الفار)",
            optionBAr = "أضواء التقابل (الكود)",
            optionCAr = "أضواء الوضعية فقط (الفيوز)",
            optionDAr = "إطفاء جميع الأضواء",
            optionAFr = "Les feux de route (pleins phares)",
            optionBFr = "Les feux de croisement (codes)",
            optionCFr = "Les feux de position seuls",
            optionDFr = "Éteindre tous les feux",
            correctIndex = 1,
            explanationAr = "يجب التحول من أضواء الطريق إلى أضواء التقابل (الكود) لتفادي إبهار السائق المقابل والحفاظ على رؤية آمنة.",
            explanationFr = "Il faut passer aux feux de croisement afin de ne pas éblouir le conducteur venant en face tout en conservant une visibilité suffisante."
        ),
        QuestionEntity(
            id = 21,
            categoryId = "vehicle_safety",
            difficulty = "easy",
            questionAr = "من الملزم بوضع حزام الأمان داخل المركبة في قانون المرور الجزائري؟",
            questionFr = "Qui doit porter la ceinture de sécurité dans un véhicule selon la réglementation algérienne ?",
            optionAAr = "السائق فقط",
            optionBAr = "السائق والراكب الأمامي فقط",
            optionCAr = "السائق وجميع الركاب في المقاعد المجهزة بأحزمة أمان",
            optionDAr = "فقط خارج المدينة",
            optionAFr = "Le conducteur uniquement",
            optionBFr = "Le conducteur et le passager avant uniquement",
            optionCFr = "Le conducteur et tous les passagers des places équipées",
            optionDFr = "Uniquement hors agglomération",
            correctIndex = 2,
            explanationAr = "وضع حزام الأمان إجباري للسائق والركاب في المقاعد الأمامية والخلفية المجهزة بها داخل وخارج التجمعات السكانية.",
            explanationFr = "Le port de la ceinture est obligatoire pour le conducteur et les passagers avant et arrière équipés, en ville comme hors agglomération."
        ),
        QuestionEntity(
            id = 22,
            categoryId = "driver_resp",
            difficulty = "easy",
            questionAr = "هل يُسمح باستعمال الهاتف النقال باليد أثناء السياقة؟",
            questionFr = "Est-il autorisé de tenir un téléphone portable en main en conduisant ?",
            optionAAr = "نعم، في الطرق السريعة فقط",
            optionBAr = "نعم، عند التوقف في الضوء الأحمر فقط",
            optionCAr = "لا، يُمنع استعمال الهاتف باليد أثناء السياقة لأنه يضاعف خطر الحوادث",
            optionDAr = "نعم، للرسائل القصيرة فقط",
            optionAFr = "Oui, sur autoroute uniquement",
            optionBFr = "Oui, au feu rouge uniquement",
            optionCFr = "Non, l'usage du téléphone tenu en main est interdit en circulation",
            optionDFr = "Oui, pour les messages courts uniquement",
            correctIndex = 2,
            explanationAr = "استعمال الهاتف باليد أثناء السياقة مخالفة خطيرة في قانون المرور الجزائري ويشتت تركيز السائق بشكل كبير.",
            explanationFr = "Tenir un téléphone en main en conduisant est une infraction grave en Algérie qui multiplie fortement le risque d'accident."
        ),
        QuestionEntity(
            id = 23,
            categoryId = "first_aid",
            difficulty = "hard",
            questionAr = "عند وقوع حادث مرور به مصابون، ما هو الترتيب الصحيح للخطوات الأساسية (PAS)؟",
            questionFr = "Face à un accident avec blessés, quel est l'ordre correct des gestes essentiels (PAS) ?",
            optionAAr = "الإسعاف ثم الإبلاغ ثم الحماية",
            optionBAr = "الحماية (تأمين المكان) ثم الإبلاغ (الحماية المدنية 14) ثم الإسعاف",
            optionCAr = "نقل المصاب فوراً بسيارتك قبل أي شيء",
            optionDAr = "الإبلاغ فقط والمغادرة",
            optionAFr = "Secourir, puis Alerter, puis Protéger",
            optionBFr = "Protéger le lieu, puis Alerter (Protection Civile 14), puis Secourir",
            optionCFr = "Déplacer immédiatement le blessé dans votre voiture",
            optionDFr = "Alerter uniquement puis quitter les lieux",
            correctIndex = 1,
            explanationAr = "القاعدة العالمية المعتمدة هي: 1- الحماية (مثلث الخطر وسترة الأمان لتفادي حادث ثانٍ)، 2- الإبلاغ (الحماية المدنية 14 / الدرك 1055 / الشرطة 1548)، 3- الإسعاف دون تحريك المصاب إلا عند خطر فوري كالحريق.",
            explanationFr = "La règle PAS impose : 1) Protéger la zone (triangle, gilet) pour éviter un suraccident, 2) Alerter (Protection Civile 14, Gendarmerie 1055, Police 1548), 3) Secourir sans déplacer un blessé sauf danger immédiat."
        ),
        QuestionEntity(
            id = 24,
            categoryId = "accidents",
            difficulty = "medium",
            questionAr = "في حالة حادث مادي بسيط بدون إصابات، ما هي الوثيقة التي يُنصح بملئها مع الطرف الآخر؟",
            questionFr = "En cas d'accident matériel léger sans blessé, quel document est recommandé de remplir avec l'autre conducteur ?",
            optionAAr = "شهادة طبية",
            optionBAr = "المعاينة الودية للحادث (Constat amiable)",
            optionCAr = "محضر بيع المركبة",
            optionDAr = "لا حاجة لأي وثيقة",
            optionAFr = "Un certificat médical",
            optionBFr = "Le constat amiable d'accident",
            optionCFr = "Un acte de vente du véhicule",
            optionDFr = "Aucun document n'est nécessaire",
            correctIndex = 1,
            explanationAr = "المعاينة الودية (Constat amiable) تُسهّل إجراءات التعويض لدى شركة التأمين ويجب ملؤها بدقة وتوقيعها من الطرفين ثم إيداعها ضمن الآجال القانونية.",
            explanationFr = "Le constat amiable facilite l'indemnisation auprès de l'assurance ; il doit être rempli avec précision, signé par les deux conducteurs et transmis dans les délais légaux."
        ),
        QuestionEntity(
            id = 25,
            categoryId = "dangerous_situations",
            difficulty = "hard",
            questionAr = "ماذا تفعل إذا بدأت سيارتك بالانزلاق المائي (Aquaplaning) فوق بركة ماء؟",
            questionFr = "Que devez-vous faire si votre véhicule subit un aquaplaning sur une flaque d'eau ?",
            optionAAr = "الضغط بقوة ومفاجأة على الفرامل وتدوير المقود بسرعة",
            optionBAr = "رفع القدم عن دواسة البنزين بهدوء، الحفاظ على استقامة المقود، وتجنب الفرملة المفاجئة",
            optionCAr = "زيادة السرعة للخروج من الماء",
            optionDAr = "شد فرامل اليد فوراً",
            optionAFr = "Freiner brutalement et braquer fortement le volant",
            optionBFr = "Lever doucement le pied de l'accélérateur, garder le volant droit et éviter de freiner brusquement",
            optionCFr = "Accélérer pour sortir de l'eau",
            optionDFr = "Tirer le frein à main immédiatement",
            correctIndex = 1,
            explanationAr = "أثناء الانزلاق المائي تفقد العجلات التصاقها بالطريق؛ الفرملة القوية أو تدوير المقود يؤدي لفقدان السيطرة. الحل هو رفع القدم عن البنزين بهدوء حتى تعود العجلات للتماسك.",
            explanationFr = "En aquaplaning, les pneus perdent le contact avec la route : freiner fort ou braquer fait déraper. Il faut lever le pied doucement et maintenir le volant droit jusqu'au retour de l'adhérence."
        ),
        QuestionEntity(
            id = 26,
            categoryId = "road_signs",
            difficulty = "easy",
            questionAr = "ماذا تعني الإشارة الدائرية الزرقاء ذات السهم الأبيض نحو اليمين؟",
            questionFr = "Que signifie le panneau circulaire bleu avec une flèche blanche vers la droite ?",
            optionAAr = "ممنوع الدوران نحو اليمين",
            optionBAr = "اتجاه إجباري نحو اليمين",
            optionCAr = "منعرج خطير نحو اليمين",
            optionDAr = "طريق ذو أولوية نحو اليمين",
            optionAFr = "Interdiction de tourner à droite",
            optionBFr = "Direction obligatoire à droite",
            optionCFr = "Virage dangereux à droite",
            optionDFr = "Route prioritaire à droite",
            correctIndex = 1,
            explanationAr = "الإشارات الدائرية الزرقاء هي إشارات إجبار؛ السهم نحو اليمين يفرض على السائق الاتجاه يميناً في التقاطع.",
            explanationFr = "Les panneaux ronds bleus sont des panneaux d'obligation ; la flèche blanche impose de tourner à droite.",
            signCode = "MANDATORY_RIGHT"
        ),
        QuestionEntity(
            id = 27,
            categoryId = "road_signs",
            difficulty = "easy",
            questionAr = "ماذا تعني الدائرة الحمراء التي يتوسطها مستطيل أبيض أفقي؟",
            questionFr = "Que signifie le disque rouge barré d'un rectangle blanc horizontal ?",
            optionAAr = "قف",
            optionBAr = "اتجاه ممنوع (ممنوع الدخول)",
            optionCAr = "ممنوع التجاوز",
            optionDAr = "نهاية الطريق السيار",
            optionAFr = "STOP",
            optionBFr = "Sens interdit",
            optionCFr = "Interdiction de dépasser",
            optionDFr = "Fin d'autoroute",
            correctIndex = 1,
            explanationAr = "إشارة اتجاه ممنوع تمنع دخول أي مركبة في هذا الاتجاه لأن الطريق ذو اتجاه واحد معاكس.",
            explanationFr = "Le panneau Sens interdit interdit à tout véhicule de s'engager dans cette voie à sens unique inverse.",
            signCode = "NO_ENTRY"
        )
    )

    fun initialRoadSigns(): List<RoadSignEntity> = listOf(
        RoadSignEntity(
            code = "CURVE_RIGHT",
            categoryId = "warning",
            nameAr = "منعرج خطير نحو اليمين",
            nameFr = "Virage dangereux à droite",
            meaningAr = "تنبيه بوجود منعرج حاد إلى اليمين.",
            meaningFr = "Annonce un virage serré vers la droite.",
            explanationAr = "توضع هذه الإشارة قبل المنعرج بمسافة كافية (حوالي 50م داخل المدينة و150م خارجها) ليتمكن السائق من تخفيض السرعة.",
            explanationFr = "Implanté avant le virage (environ 50 m en ville, 150 m hors agglomération) pour permettre de ralentir.",
            exampleAr = "خفّض سرعتك قبل دخول المنعرج والتزم أقصى اليمين.",
            exampleFr = "Ralentissez avant d'aborder le virage et serrez à droite."
        ),
        RoadSignEntity(
            code = "PEDESTRIAN",
            categoryId = "warning",
            nameAr = "انتباه ممر راجلين",
            nameFr = "Passage pour piétons",
            meaningAr = "تحذير من الاقتراب من ممر مخصص لعبور الراجلين.",
            meaningFr = "Signale l'approche d'un passage piéton.",
            explanationAr = "ينبه السائق لاحتمال عبور المشاة ويفرض الاستعداد للتوقف وترك الأولوية لهم.",
            explanationFr = "Avertit de la présence possible de piétons et impose de se préparer à s'arrêter.",
            exampleAr = "قرب المدارس أو الأسواق، خفّض السرعة وراقب الأرصفة.",
            exampleFr = "Près des écoles ou marchés, ralentissez et observez les trottoirs."
        ),
        RoadSignEntity(
            code = "ROUNDABOUT_WARN",
            categoryId = "warning",
            nameAr = "محور دوراني",
            nameFr = "Carrefour à sens giratoire",
            meaningAr = "تنبيه بالاقتراب من تقاطع ذي اتجاه دوراني.",
            meaningFr = "Annonce un rond-point à sens giratoire.",
            explanationAr = "يدل على وجوب تخفيض السرعة والدوران عكس عقارب الساعة مع احترام إشارة الأولوية عند المدخل.",
            explanationFr = "Indique qu'il faut ralentir, contourner l'îlot par la droite et respecter la priorité à l'entrée.",
            exampleAr = "راقب المركبات القادمة من يسارك داخل الدوار قبل الدخول.",
            exampleFr = "Vérifiez les véhicules venant de gauche dans l'anneau avant de vous engager."
        ),
        RoadSignEntity(
            code = "PRIO_RIGHT",
            categoryId = "warning",
            nameAr = "تقاطع طرق (الأولوية لليمين)",
            nameFr = "Intersection (priorité à droite)",
            meaningAr = "تقاطع طرق تُطبق فيه قاعدة الأولوية لليمين.",
            meaningFr = "Intersection où la priorité à droite s'applique.",
            explanationAr = "عند رؤية علامة X السوداء داخل المثلث الأحمر، يجب ترك الأولوية للمركبات القادمة من اليمين.",
            explanationFr = "La croix noire dans le triangle rouge rappelle de céder le passage aux véhicules venant de droite.",
            exampleAr = "تمهل عند التقاطع وتأكد من خلو الطريق على يمينك.",
            exampleFr = "Ralentissez à l'intersection et vérifiez la voie de droite."
        ),
        RoadSignEntity(
            code = "NO_ENTRY",
            categoryId = "prohibition",
            nameAr = "اتجاه ممنوع",
            nameFr = "Sens interdit",
            meaningAr = "يمنع دخول جميع المركبات في هذا الطريق.",
            meaningFr = "Accès interdit à tous les véhicules dans ce sens.",
            explanationAr = "الطريق مخصص للسير في الاتجاه المعاكس فقط؛ الدخول إليه مخالفة خطيرة قد تتسبب في اصطدام مباشر.",
            explanationFr = "La voie est à sens unique inverse ; s'y engager est dangereux et interdit.",
            exampleAr = "مخرج شارع ذي اتجاه واحد في وسط المدينة.",
            exampleFr = "Sortie d'une rue à sens unique en centre-ville."
        ),
        RoadSignEntity(
            code = "LIMIT_50",
            categoryId = "prohibition",
            nameAr = "السرعة القصوى 50 كلم/سا",
            nameFr = "Vitesse limitée à 50 km/h",
            meaningAr = "يمنع السير بسرعة تفوق 50 كلم/سا.",
            meaningFr = "Interdiction de dépasser 50 km/h.",
            explanationAr = "تطبق هذه السرعة من موضع الإشارة وحتى نهاية التجمع السكاني أو ظهور إشارة سرعة جديدة.",
            explanationFr = "S'applique à partir du panneau jusqu'à la fin de zone ou un nouveau panneau.",
            exampleAr = "مداخل المدن والأحياء السكنية في الجزائر.",
            exampleFr = "Entrées de villes et zones urbaines en Algérie."
        ),
        RoadSignEntity(
            code = "LIMIT_80",
            categoryId = "prohibition",
            nameAr = "السرعة القصوى 80 كلم/سا",
            nameFr = "Vitesse limitée à 80 km/h",
            meaningAr = "يمنع تجاوز سرعة 80 كلم/سا.",
            meaningFr = "Interdiction de dépasser 80 km/h.",
            explanationAr = "السرعة القانونية المعتادة على الطرق الوطنية خارج التجمعات السكانية، وكذلك الحد الأقصى لحاملي الرخصة الاختبارية.",
            explanationFr = "Limite usuelle sur routes nationales hors agglomération et plafond pour le permis probatoire.",
            exampleAr = "الطرق الوطنية بين الولايات في الظروف العادية.",
            exampleFr = "Routes nationales inter-wilayas par temps sec."
        ),
        RoadSignEntity(
            code = "NO_OVERTAKE",
            categoryId = "prohibition",
            nameAr = "ممنوع التجاوز",
            nameFr = "Interdiction de dépasser",
            meaningAr = "يمنع تجاوز المركبات ذات المحرك (عدا الدراجات بدون عربة جانبية).",
            meaningFr = "Interdit de dépasser les véhicules à moteur (sauf deux-roues sans side-car).",
            explanationAr = "توضع في المقاطع التي تكون فيها الرؤية محدودة كالمنعرجات والمرتفعات.",
            explanationFr = "Implanté dans les zones à visibilité réduite (sommets de côte, virages).",
            exampleAr = "طريق جبلي ضيق ذو اتجاهين.",
            exampleFr = "Route de montagne étroite à double sens."
        ),
        RoadSignEntity(
            code = "NO_PARKING",
            categoryId = "prohibition",
            nameAr = "ممنوع التوقف",
            nameFr = "Stationnement interdit",
            meaningAr = "يمنع ركن السيارة، ويسمح بالوقوف القصير فقط.",
            meaningFr = "Stationnement interdit, arrêt bref autorisé.",
            explanationAr = "يطبق المنع بعد الإشارة وحتى التقاطع القادم على نفس جانب الطريق.",
            explanationFr = "L'interdiction vaut à partir du panneau jusqu'à la prochaine intersection du même côté.",
            exampleAr = "أمام مداخل العمارات أو الشوارع المزدحمة.",
            exampleFr = "Devant les accès d'immeubles ou avenues chargées."
        ),
        RoadSignEntity(
            code = "NO_STOP_PARK",
            categoryId = "prohibition",
            nameAr = "ممنوع الوقوف والتوقف",
            nameFr = "Arrêt et stationnement interdits",
            meaningAr = "يمنع التوقف والوقوف حتى لثوانٍ معدودة.",
            meaningFr = "Interdiction totale de s'arrêter et de stationner.",
            explanationAr = "توضع في الأماكن الحساسة حيث يعرقل أي توقف حركة المرور أو يحجب الرؤية.",
            explanationFr = "Utilisé dans les zones sensibles où tout arrêt gêne la circulation ou la visibilité.",
            exampleAr = "قرب التقاطعات، الجسور، أو محطات الحافلات.",
            exampleFr = "Près des intersections, ponts ou arrêts de bus."
        ),
        RoadSignEntity(
            code = "MANDATORY_RIGHT",
            categoryId = "mandatory",
            nameAr = "اتجاه إجباري نحو اليمين",
            nameFr = "Direction obligatoire à droite",
            meaningAr = "يجب الاتجاه نحو اليمين في التقاطع.",
            meaningFr = "Obligation de tourner à droite à l'intersection.",
            explanationAr = "إشارة إجبار دائرية زرقاء تمنع مواصلة السير للأمام أو الدوران يساراً.",
            explanationFr = "Panneau rond bleu d'obligation interdisant d'aller tout droit ou à gauche.",
            exampleAr = "تقاطع مع شارع ذي اتجاه واحد نحو اليمين.",
            exampleFr = "Intersection avec une rue à sens unique vers la droite."
        ),
        RoadSignEntity(
            code = "MANDATORY_ROUNDABOUT",
            categoryId = "mandatory",
            nameAr = "دوران إجباري",
            nameFr = "Sens giratoire obligatoire",
            meaningAr = "يجب الالتفاف حول المحور في اتجاه الأسهم.",
            meaningFr = "Obligation de contourner l'îlot dans le sens des flèches.",
            explanationAr = "تؤكد أن السير داخل الدوار يتم دائماً عكس عقارب الساعة.",
            explanationFr = "Confirme que la circulation dans le rond-point se fait dans le sens antihoraire.",
            exampleAr = "مثبتة وسط الجزيرة الوسطى للمحور الدوراني.",
            exampleFr = "Placé sur le terre-plein central du rond-point."
        ),
        RoadSignEntity(
            code = "PARKING_INFO",
            categoryId = "information",
            nameAr = "موقف سيارات",
            nameFr = "Parking",
            meaningAr = "إرشاد لمكان مخصص لركن المركبات.",
            meaningFr = "Indique un emplacement aménagé pour le stationnement.",
            explanationAr = "إشارة مربعة زرقاء تدل على وجود حظيرة أو مساحة مسموح فيها بالتوقف.",
            explanationFr = "Panneau carré bleu signalant une aire de stationnement autorisée.",
            exampleAr = "قرب المراكز التجارية أو المؤسسات العمومية.",
            exampleFr = "Près des centres commerciaux ou administrations."
        ),
        RoadSignEntity(
            code = "HOSPITAL_INFO",
            categoryId = "information",
            nameAr = "مستشفى",
            nameFr = "Hôpital",
            meaningAr = "وجود مؤسسة استشفائية قريبة.",
            meaningFr = "Présence d'un établissement hospitalier à proximité.",
            explanationAr = "يدعو السائق إلى الهدوء، عدم استعمال المنبه الصوتي، والانتباه لسيارات الإسعاف.",
            explanationFr = "Incite à éviter le klaxon et à faciliter le passage des ambulances.",
            exampleAr = "الطرق المؤدية إلى المستشفيات والعيادات.",
            exampleFr = "Voies d'accès aux hôpitaux et cliniques."
        ),
        RoadSignEntity(
            code = "STOP",
            categoryId = "priority",
            nameAr = "قف (STOP)",
            nameFr = "STOP",
            meaningAr = "الوقوف التام الإجباري وترك الأولوية.",
            meaningFr = "Arrêt absolu obligatoire et céder le passage.",
            explanationAr = "شكلها الثماني المميز يسمح بالتعرف عليها حتى من الخلف أو تحت الثلج؛ الوقوف التام إلزامي دائماً.",
            explanationFr = "Sa forme octogonale unique permet de la reconnaître sous tous les angles ; l'arrêt complet est toujours obligatoire.",
            exampleAr = "خروج من طريق فرعي نحو طريق رئيسي مزدحم.",
            exampleFr = "Débouché d'une voie secondaire sur un axe principal."
        ),
        RoadSignEntity(
            code = "YIELD",
            categoryId = "priority",
            nameAr = "إفساح الطريق",
            nameFr = "Cédez le passage",
            meaningAr = "ترك الأولوية للمركبات في الطريق الرئيسي دون إلزامية التوقف إذا كان خالياً.",
            meaningFr = "Céder la priorité sans arrêt obligatoire si la voie est libre.",
            explanationAr = "مثلث مقلوب برأسه للأسفل، يفرض التمهل الشديد والتوقف عند وجود مركبات قادمة.",
            explanationFr = "Triangle pointe en bas imposant de ralentir et de s'arrêter si un véhicule approche.",
            exampleAr = "مداخل المحاور الدورانية ومسالك الاندماج.",
            exampleFr = "Entrées de ronds-points et voies d'insertion."
        ),
        RoadSignEntity(
            code = "PRIO_ROAD",
            categoryId = "priority",
            nameAr = "طريق ذو أولوية",
            nameFr = "Route à caractère prioritaire",
            meaningAr = "لديك الأولوية في التقاطعات القادمة.",
            meaningFr = "Vous bénéficiez de la priorité aux prochaines intersections.",
            explanationAr = "معين أصفر بإطار أبيض، يظل سارياً حتى ظهور إشارة نهاية الأولوية المشطوبة.",
            explanationFr = "Losange jaune encadré de blanc, valable jusqu'au panneau de fin de priorité barré.",
            exampleAr = "الطرق الوطنية الرئيسية خارج التجمعات السكانية.",
            exampleFr = "Grands axes nationaux hors agglomération."
        ),
        RoadSignEntity(
            code = "TEMP_WORKS",
            categoryId = "temporary",
            nameAr = "أشغال عمومية (مؤقت)",
            nameFr = "Travaux (temporaire)",
            meaningAr = "وجود ورشة أشغال على الطريق أو حافته.",
            meaningFr = "Présence d'un chantier sur ou au bord de la chaussée.",
            explanationAr = "الخلفية الصفراء تدل على طابع مؤقت؛ خفّض السرعة وانتبه للعمال والآليات وضيق المسلك.",
            explanationFr = "Le fond jaune indique un danger temporaire ; réduisez la vitesse et restez attentif aux ouvriers.",
            exampleAr = "صيانة الطريق أو حفر قنوات على الطريق الوطني.",
            exampleFr = "Entretien routier ou travaux de chaussée."
        ),
        RoadSignEntity(
            code = "SOLID_LINE",
            categoryId = "markings",
            nameAr = "خط أبيض متواصل",
            nameFr = "Ligne blanche continue",
            meaningAr = "يمنع تجاوزه أو الدوس عليه أو الدوران للخلف فوقه.",
            meaningFr = "Interdiction de la franchir, de la chevaucher ou de faire demi-tour.",
            explanationAr = "يفصل بين اتجاهي السير في المناطق الخطيرة حيث تكون الرؤية غير كافية للتجاوز.",
            explanationFr = "Sépare les sens de circulation dans les zones où la visibilité interdit le dépassement.",
            exampleAr = "قبل المنعرجات، القمم، وتقاطعات الطرق.",
            exampleFr = "À l'approche des virages, sommets et carrefours."
        ),
        RoadSignEntity(
            code = "DASHED_LINE",
            categoryId = "markings",
            nameAr = "خط أبيض متقطع",
            nameFr = "Ligne blanche discontinue",
            meaningAr = "يسمح بالتجاوز إذا كانت الرؤية والطريق آمنين.",
            meaningFr = "Autorise le dépassement si la visibilité et la sécurité le permettent.",
            explanationAr = "يرسم خطوطاً متقطعة تسمح بعبورها للتجاوز ثم العودة فوراً إلى المسلك الأيمن.",
            explanationFr = "Permet de franchir l'axe pour dépasser avant de se rabattre immédiatement à droite.",
            exampleAr = "المقاطع المستقيمة ذات الرؤية الواضحة.",
            exampleFr = "Lignes droites à bonne visibilité."
        ),
        RoadSignEntity(
            code = "TRAFFIC_LIGHTS",
            categoryId = "lights",
            nameAr = "الإشارات الضوئية الثلاثية",
            nameFr = "Feux tricolores",
            meaningAr = "الأحمر للوقوف، الأصفر للاستعداد للتوقف، والأخضر للمرور بحذر.",
            meaningFr = "Rouge : arrêt, Orange : préparer l'arrêt, Vert : passage prudent.",
            explanationAr = "تنظم حركة المرور في التقاطعات؛ وفي حال عطلها أو انطفائها تطبق إشارات الأولوية المثبتة أو قاعدة الأولوية لليمين.",
            explanationFr = "Régulent la circulation aux carrefours ; s'ils sont éteints ou clignotants jaunes, suivez les panneaux ou la priorité à droite.",
            exampleAr = "تقاطعات الشوارع الرئيسية داخل المدن.",
            exampleFr = "Carrefours principaux en milieu urbain."
        )
    )
}

data class ContentSyncResult(
    val success: Boolean,
    val version: Int,
    val questionsAddedOrUpdated: Int,
    val signsAddedOrUpdated: Int,
    val message: String
)

object ContentSyncManager {
    suspend fun syncFromUrl(
        db: AppDatabase,
        prefs: UserPrefs,
        urlString: String
    ): ContentSyncResult = withContext(Dispatchers.IO) {
        try {
            if (urlString.isBlank() || urlString.contains("YOUR_GITHUB_USERNAME")) {
                return@withContext ContentSyncResult(
                    success = false,
                    version = prefs.contentVersion,
                    questionsAddedOrUpdated = 0,
                    signsAddedOrUpdated = 0,
                    message = "Please set your real GitHub raw JSON URL in Settings first."
                )
            }
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
            }
            if (conn.responseCode != 200) {
                return@withContext ContentSyncResult(
                    false, prefs.contentVersion, 0, 0, "HTTP ${conn.responseCode}"
                )
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            val remoteVersion = root.optInt("version", prefs.contentVersion)

            val parsedQuestions = mutableListOf<QuestionEntity>()
            val qArr = root.optJSONArray("questions")
            if (qArr != null) {
                for (i in 0 until qArr.length()) {
                    val o = qArr.getJSONObject(i)
                    parsedQuestions.add(
                        QuestionEntity(
                            id = o.getInt("id"),
                            categoryId = o.getString("categoryId"),
                            difficulty = o.optString("difficulty", "medium"),
                            questionAr = o.getString("questionAr"),
                            questionFr = o.getString("questionFr"),
                            optionAAr = o.getString("optionAAr"),
                            optionBAr = o.getString("optionBAr"),
                            optionCAr = o.getString("optionCAr"),
                            optionDAr = o.getString("optionDAr"),
                            optionAFr = o.getString("optionAFr"),
                            optionBFr = o.getString("optionBFr"),
                            optionCFr = o.getString("optionCFr"),
                            optionDFr = o.getString("optionDFr"),
                            correctIndex = o.getInt("correctIndex"),
                            explanationAr = o.getString("explanationAr"),
                            explanationFr = o.getString("explanationFr"),
                            signCode = o.optString("signCode").takeIf { it.isNotBlank() && it != "null" },
                            isOfficialVerified = o.optBoolean("isOfficialVerified", false)
                        )
                    )
                }
            }

            val parsedSigns = mutableListOf<RoadSignEntity>()
            val sArr = root.optJSONArray("roadSigns")
            if (sArr != null) {
                for (i in 0 until sArr.length()) {
                    val o = sArr.getJSONObject(i)
                    parsedSigns.add(
                        RoadSignEntity(
                            code = o.getString("code"),
                            categoryId = o.getString("categoryId"),
                            nameAr = o.getString("nameAr"),
                            nameFr = o.getString("nameFr"),
                            meaningAr = o.getString("meaningAr"),
                            meaningFr = o.getString("meaningFr"),
                            explanationAr = o.getString("explanationAr"),
                            explanationFr = o.getString("explanationFr"),
                            exampleAr = o.getString("exampleAr"),
                            exampleFr = o.getString("exampleFr")
                        )
                    )
                }
            }

            if (parsedQuestions.isNotEmpty()) db.questionDao().upsertAll(parsedQuestions)
            if (parsedSigns.isNotEmpty()) db.roadSignDao().upsertAll(parsedSigns)
            prefs.contentVersion = remoteVersion

            ContentSyncResult(
                success = true,
                version = remoteVersion,
                questionsAddedOrUpdated = parsedQuestions.size,
                signsAddedOrUpdated = parsedSigns.size,
                message = "OK v$remoteVersion"
            )
        } catch (e: Exception) {
            ContentSyncResult(
                success = false,
                version = prefs.contentVersion,
                questionsAddedOrUpdated = 0,
                signsAddedOrUpdated = 0,
                message = e.localizedMessage ?: "Network error"
            )
        }
    }
}
