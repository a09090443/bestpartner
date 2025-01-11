package tw.zipe.bastpartner.util.time

import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.chrono.MinguoChronology
import java.time.chrono.MinguoDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DecimalStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 增強型日期時間工具類，支援時區和字串轉換
 * @author Gary Tsai (Original)
 * Modified and enhanced for Kotlin with string conversion support
 */
object DateTimeUtils {
    const val UTC_ADD_8 = "+8"

    /**
     * 日期時間格式定義
     * 使用 Map 儲存所有支援的日期時間格式
     */
    private val formatters = mapOf(
        "DATETIME" to "yyyy-MM-dd HH:mm:ss",
        "DATE" to "yyyy-MM-dd",
        "TIME" to "HH:mm:ss",
        "DATETIME_SLASH" to "yyyy/MM/dd HH:mm:ss",
        "DATETIME_COMPACT" to "yyyyMMdd HH:mm:ss",
        "DATE_SLASH" to "yyyy/MM/dd",
        "DATE_COMPACT" to "yyyyMMdd",
        "MINGUO_DATE" to "yyyMMdd",
        "YEAR" to "yyyy",
        "MONTH" to "MM",
        "DAY" to "dd",
        "YEARMONTH" to "yyyyMM",
        "MINGUO_YEARMONTH" to "yyyMM",
        "MINGUO_YEARMONTH_SLASH" to "yyy/MM",
        "DATE_SIMPLE" to "dd/MM/yy",
        "TIME_COMPACT" to "HHmmss",
        "MINGUO_YEAR" to "yyy",
        "DATETIME_FULL" to "yyyyMMddHHmmss",
        "DATETIME_CHINESE" to "yyyy年MM月dd日 HH時mm分ss秒",
        "DATE_CHINESE" to "yyyy年MM月dd日",
        "TIME_CHINESE" to "HH時mm分ss秒",
        "YEARMONTH_CHINESE" to "yyyy年MM月"
    ).mapValues { DateTimeFormatter.ofPattern(it.value) }

    /**
     * 取得指定格式器
     * @param key 格式鍵值
     * @return DateTimeFormatter 日期時間格式器
     */
    fun getFormatter(key: String): DateTimeFormatter = formatters[key]
        ?: throw IllegalArgumentException("未知的格式鍵值: $key")

    // String 轉換擴展函數
    /**
     * 將各種日期時間類型轉換為字串的擴展函數
     */
    fun LocalDateTime.formatToString(
        pattern: String = "DATETIME",
        zoneId: ZoneId = ZoneId.of(UTC_ADD_8)
    ): String = this.atZone(zoneId).format(getFormatter(pattern))

    fun LocalDate.formatToString(
        pattern: String = "DATE",
        zoneId: ZoneId = ZoneId.of(UTC_ADD_8)
    ): String = this.atStartOfDay(zoneId).format(getFormatter(pattern))

    fun LocalTime.formatToString(
        pattern: String = "TIME",
        zoneId: ZoneId = ZoneId.of(UTC_ADD_8)
    ): String = this.atDate(LocalDate.now(zoneId)).format(getFormatter(pattern))

    fun Date.formatToString(
        pattern: String = "DATETIME",
        zoneId: ZoneId = ZoneId.of(UTC_ADD_8)
    ): String = this.toInstant().atZone(zoneId).format(getFormatter(pattern))

    /**
     * 字串解析函數
     */
    fun String.toLocalDateTime(
        pattern: String = "DATETIME",
        zoneId: ZoneId = ZoneId.of(UTC_ADD_8)
    ): LocalDateTime = LocalDateTime.parse(this, getFormatter(pattern))
        .atZone(zoneId)
        .toLocalDateTime()

    fun String.toLocalDate(
        pattern: String = "DATE",
        zoneId: ZoneId = ZoneId.of(UTC_ADD_8)
    ): LocalDate = LocalDate.parse(this, getFormatter(pattern))

    fun String.toLocalTime(
        pattern: String = "TIME",
        zoneId: ZoneId = ZoneId.of(UTC_ADD_8)
    ): LocalTime = LocalTime.parse(this, getFormatter(pattern))

    /**
     * 取得目前日期時間
     * @param zoneId 時區
     * @return LocalDateTime 當前日期時間
     */
    fun getDateTimeNow(zoneId: ZoneId = ZoneId.of(UTC_ADD_8)): LocalDateTime =
        LocalDateTime.now(zoneId)

    /**
     * 格式化目前日期時間
     * @param formatter 格式器
     * @param zoneId 時區
     * @return String 格式化後的日期時間字串
     */
    fun getFormattedDateTime(
        pattern: String = "DATETIME",
        zoneId: ZoneId = ZoneId.of(UTC_ADD_8)
    ): String = getDateTimeNow(zoneId).formatToString(pattern, zoneId)

    /**
     * 日期時間計算擴展函數
     */
    private fun LocalDateTime.adjustTime(
        amount: Long,
        unit: ChronoUnit,
        zoneId: ZoneId = ZoneId.of(UTC_ADD_8)
    ): LocalDateTime = this.plus(amount, unit).atZone(zoneId).toLocalDateTime()

    /**
     * 計算年份增減
     * @param yearsToAddOrSubtract 要增加或減少的年數
     * @param pattern 輸出格式
     * @param zoneId 時區
     * @return String 計算後的日期時間字串
     */
    fun getMinusOrPlusYears(
        yearsToAddOrSubtract: Long,
        pattern: String? = null,
        zoneId: ZoneId = ZoneId.of(UTC_ADD_8)
    ): String = when (pattern) {
        null -> getDateTimeNow(zoneId).adjustTime(yearsToAddOrSubtract, ChronoUnit.YEARS, zoneId).toString()
        else -> getDateTimeNow(zoneId).adjustTime(yearsToAddOrSubtract, ChronoUnit.YEARS, zoneId)
            .formatToString(pattern, zoneId)
    }

    /**
     * 日期類型轉換擴展函數
     */
    fun Date.toLocalDateTime(zoneId: ZoneId = ZoneId.of(UTC_ADD_8)): LocalDateTime =
        this.toInstant().atZone(zoneId).toLocalDateTime()

    fun Date.toLocalDate(zoneId: ZoneId = ZoneId.of(UTC_ADD_8)): LocalDate =
        this.toInstant().atZone(zoneId).toLocalDate()

    fun Date.toLocalTime(zoneId: ZoneId = ZoneId.of(UTC_ADD_8)): LocalTime =
        this.toInstant().atZone(zoneId).toLocalTime()

    /**
     * LocalDateTime 轉換為 Date
     */
    fun LocalDateTime.toDate(zoneId: ZoneId = ZoneId.of(UTC_ADD_8)): Date =
        Date.from(this.atZone(zoneId).toInstant())

    /**
     * LocalDate 轉換為 Date
     */
    fun LocalDate.toDate(zoneId: ZoneId = ZoneId.of(UTC_ADD_8)): Date =
        Date.from(this.atStartOfDay(zoneId).toInstant())

    /**
     * LocalTime 轉換為 Date
     */
    fun LocalTime.toDate(): Date = Calendar.getInstance().apply {
        clear()
        set(0, 0, 0, this@toDate.hour, this@toDate.minute, this@toDate.second)
    }.time

    /**
     * 民國年份相關函數
     */
    fun getMinguoYear(zoneId: ZoneId = ZoneId.of(UTC_ADD_8)): String =
        MinguoDate.from(LocalDate.now(zoneId)).format(getFormatter("MINGUO_YEAR"))

    /**
     * 西元年轉民國年
     * @param dateString 西元年日期字串 (yyyyMM 格式)
     * @param zoneId 時區
     * @return String 民國年日期字串
     */
    fun transferADDateToMinguoDate(
        dateString: String,
        zoneId: ZoneId = ZoneId.of(UTC_ADD_8)
    ): String = YearMonth.parse(dateString, getFormatter("YEARMONTH"))
        .atDay(1)
        .let { MinguoDate.from(it).format(getFormatter("MINGUO_YEARMONTH")) }

    /**
     * 民國年轉西元年
     * @param dateString 民國年日期字串 (yyyMMdd 格式)
     * @return String 西元年日期字串
     */
    fun transferMinguoDateToADDate(dateString: String): String {
        val chrono = MinguoChronology.INSTANCE
        val formatter = DateTimeFormatterBuilder()
            .parseLenient()
            .appendPattern("yyyMMdd")
            .toFormatter()
            .withChronology(chrono)
            .withDecimalStyle(DecimalStyle.of(Locale.getDefault()))

        return LocalDate.from(chrono.date(formatter.parse(dateString)))
            .format(getFormatter("DATE"))
    }

    /**
     * 日期時間差異計算
     * @param date1 第一個日期時間
     * @param date2 第二個日期時間
     * @param unit 計算單位
     * @return Long 時間差值
     */
    fun getDateTimeDifference(
        date1: LocalDateTime,
        date2: LocalDateTime,
        unit: ChronoUnit
    ): Long = unit.between(date1, date2)

    /**
     * 計算小時差
     * @param date1 第一個日期時間
     * @param date2 第二個日期時間
     * @return Double 小時差值（帶小數）
     */
    fun getDateTimeDifferenceInHours(
        date1: LocalDateTime,
        date2: LocalDateTime
    ): Double = Duration.between(date1, date2)
        .seconds
        .toBigDecimal()
        .divide(BigDecimal(3600), 2, BigDecimal.ROUND_HALF_UP)
        .toDouble()

    /**
     * 取得時間段的開始
     * @param unit 時間單位（日、月、年）
     * @return LocalDateTime 時間段的開始時間
     */
    fun LocalDateTime.startOf(unit: ChronoUnit): LocalDateTime = when(unit) {
        ChronoUnit.DAYS -> this.with(LocalTime.MIN)
        ChronoUnit.MONTHS -> this.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN)
        ChronoUnit.YEARS -> this.with(TemporalAdjusters.firstDayOfYear()).with(LocalTime.MIN)
        else -> throw IllegalArgumentException("不支援的時間單位: $unit")
    }

    /**
     * 取得時間段的結束
     * @param unit 時間單位（日、月、年）
     * @return LocalDateTime 時間段的結束時間
     */
    fun LocalDateTime.endOf(unit: ChronoUnit): LocalDateTime = when(unit) {
        ChronoUnit.DAYS -> this.with(LocalTime.MAX)
        ChronoUnit.MONTHS -> this.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX)
        ChronoUnit.YEARS -> this.with(TemporalAdjusters.lastDayOfYear()).with(LocalTime.MAX)
        else -> throw IllegalArgumentException("不支援的時間單位: $unit")
    }

    /**
     * 取得中文格式的日期時間字串
     * @param dateTime 日期時間
     * @param zoneId 時區
     * @return String 中文格式的日期時間字串
     */
    fun getChineseDateTime(
        dateTime: LocalDateTime = getDateTimeNow(),
        zoneId: ZoneId = ZoneId.of(UTC_ADD_8)
    ): String = dateTime.formatToString("DATETIME_CHINESE", zoneId)

    /**
     * 取得中文格式的日期字串
     * @param date 日期
     * @param zoneId 時區
     * @return String 中文格式的日期字串
     */
    fun getChineseDate(
        date: LocalDate = LocalDate.now(ZoneId.of(UTC_ADD_8)),
        zoneId: ZoneId = ZoneId.of(UTC_ADD_8)
    ): String = date.formatToString("DATE_CHINESE", zoneId)

    /**
     * 取得中文格式的時間字串
     * @param time 時間
     * @param zoneId 時區
     * @return String 中文格式的時間字串
     */
    fun getChineseTime(
        time: LocalTime = LocalTime.now(ZoneId.of(UTC_ADD_8)),
        zoneId: ZoneId = ZoneId.of(UTC_ADD_8)
    ): String = time.formatToString("TIME_CHINESE", zoneId)

    /**
     * 示範用法
     */
    fun examples() {
        // 基本日期時間轉換
        val now = getDateTimeNow()
        println("標準格式：${now.formatToString()}")  // 2025-01-11 14:30:00
        println("中文格式：${getChineseDateTime(now)}")  // 2025年01月11日 14時30分00秒

        // 字串解析
        val dateStr = "2025-01-11 14:30:00"
        val parsedDateTime = dateStr.toLocalDateTime()

        // 時區轉換
        val tokyoZone = ZoneId.of("Asia/Tokyo")
        println("東京時間：${now.formatToString(zoneId = tokyoZone)}")

        // 日期計算
        val nextYear = getMinusOrPlusYears(1, "DATE_CHINESE")
        println("明年：$nextYear")  // 2026年01月11日
    }
}
