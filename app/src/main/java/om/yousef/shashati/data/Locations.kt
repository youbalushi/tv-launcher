package om.yousef.shashati.data

data class Wilaya(val name: String, val lat: Double, val lng: Double)
data class Governorate(val name: String, val wilayat: List<Wilaya>)

/**
 * Governorates and wilayat of Oman with approximate coordinates of each wilaya centre.
 * Small differences are corrected with the per-prayer minute offsets in settings.
 */
object Locations {
    val governorates = listOf(
        Governorate("مسقط", listOf(
            Wilaya("مسقط", 23.6139, 58.5922),
            Wilaya("مطرح", 23.6178, 58.5658),
            Wilaya("بوشر", 23.5539, 58.3989),
            Wilaya("السيب", 23.6703, 58.1891),
            Wilaya("العامرات", 23.5230, 58.4900),
            Wilaya("قريات", 23.2636, 58.9184),
        )),
        Governorate("ظفار", listOf(
            Wilaya("صلالة", 17.0151, 54.0924),
            Wilaya("طاقة", 17.0389, 54.4022),
            Wilaya("مرباط", 16.9934, 54.6933),
            Wilaya("رخيوت", 16.7420, 53.4170),
            Wilaya("ثمريت", 17.6650, 54.0280),
            Wilaya("ضلكوت", 16.7058, 53.2483),
            Wilaya("المزيونة", 17.8414, 52.6608),
            Wilaya("مقشن", 19.5570, 54.8840),
            Wilaya("شليم وجزر الحلانيات", 18.1100, 55.6000),
            Wilaya("سدح", 17.0510, 55.0720),
        )),
        Governorate("مسندم", listOf(
            Wilaya("خصب", 26.1799, 56.2477),
            Wilaya("بخا", 26.1384, 56.1488),
            Wilaya("دبا", 25.6300, 56.2650),
            Wilaya("مدحاء", 25.2836, 56.3256),
        )),
        Governorate("البريمي", listOf(
            Wilaya("البريمي", 24.2500, 55.7933),
            Wilaya("محضة", 24.4053, 55.9672),
            Wilaya("السنينة", 23.6600, 56.0200),
        )),
        Governorate("الداخلية", listOf(
            Wilaya("نزوى", 22.9333, 57.5333),
            Wilaya("بهلا", 22.9667, 57.3000),
            Wilaya("منح", 22.7950, 57.6200),
            Wilaya("الحمراء", 23.1167, 57.2833),
            Wilaya("أدم", 22.3833, 57.5167),
            Wilaya("إزكي", 22.9333, 57.7667),
            Wilaya("سمائل", 23.3033, 57.9781),
            Wilaya("بدبد", 23.4083, 58.1250),
            Wilaya("الجبل الأخضر", 23.0700, 57.6700),
        )),
        Governorate("شمال الباطنة", listOf(
            Wilaya("صحار", 24.3470, 56.7090),
            Wilaya("شناص", 24.7425, 56.4650),
            Wilaya("لوى", 24.5300, 56.5700),
            Wilaya("صحم", 24.1722, 56.8886),
            Wilaya("الخابورة", 23.9714, 57.0969),
            Wilaya("السويق", 23.8494, 57.4386),
        )),
        Governorate("جنوب الباطنة", listOf(
            Wilaya("الرستاق", 23.3908, 57.4244),
            Wilaya("العوابي", 23.3000, 57.5300),
            Wilaya("نخل", 23.3950, 57.8290),
            Wilaya("وادي المعاول", 23.4500, 57.7900),
            Wilaya("بركاء", 23.6787, 57.8860),
            Wilaya("المصنعة", 23.7458, 57.6247),
        )),
        Governorate("جنوب الشرقية", listOf(
            Wilaya("صور", 22.5667, 59.5289),
            Wilaya("الكامل والوافي", 22.2100, 59.2000),
            Wilaya("جعلان بني بو حسن", 22.1000, 59.2800),
            Wilaya("جعلان بني بو علي", 22.0167, 59.3333),
            Wilaya("مصيرة", 20.6800, 58.8700),
        )),
        Governorate("شمال الشرقية", listOf(
            Wilaya("إبراء", 22.6906, 58.5334),
            Wilaya("المضيبي", 22.5700, 58.1200),
            Wilaya("بدية", 22.4500, 58.8000),
            Wilaya("القابل", 22.5800, 58.6700),
            Wilaya("وادي بني خالد", 22.6300, 59.0800),
            Wilaya("دماء والطائيين", 23.0500, 58.6000),
        )),
        Governorate("الظاهرة", listOf(
            Wilaya("عبري", 23.2257, 56.5157),
            Wilaya("ينقل", 23.5869, 56.5397),
            Wilaya("ضنك", 23.5530, 56.2600),
        )),
        Governorate("الوسطى", listOf(
            Wilaya("هيماء", 19.9592, 56.2756),
            Wilaya("محوت", 20.5500, 57.9000),
            Wilaya("الدقم", 19.6617, 57.7042),
            Wilaya("الجازر", 19.0700, 57.5500),
        )),
    )

    fun current(): Wilaya {
        val g = governorates.getOrElse(Prefs.govIndex) { governorates[0] }
        return g.wilayat.getOrElse(Prefs.wilIndex) { g.wilayat[0] }
    }
}
