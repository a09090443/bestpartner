package tw.zipe.bastpartner.util.time;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.MinguoChronology;
import java.time.chrono.MinguoDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DecimalStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * @author : Gary Tsai
 * @created : @Date 2020/11/9 上午 10:23
 **/
public class DateTimeUtils {
    public static final String UTC_ADD_8 = "+8";

    public static DateTimeFormatter dateTimeFormate1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static DateTimeFormatter dateTimeFormate2 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static DateTimeFormatter dateTimeFormate3 = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static DateTimeFormatter dateTimeFormate4 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    public static DateTimeFormatter dateTimeFormate5 = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    public static DateTimeFormatter dateTimeFormate6 = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    public static DateTimeFormatter dateTimeFormate7 = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static DateTimeFormatter dateTimeFormate8 = DateTimeFormatter.ofPattern("yyyMMdd");
    public static DateTimeFormatter dateTimeFormate9 = DateTimeFormatter.ofPattern("yyyy");
    public static DateTimeFormatter dateTimeFormate10 = DateTimeFormatter.ofPattern("MM");
    public static DateTimeFormatter dateTimeFormate11 = DateTimeFormatter.ofPattern("dd");
    public static DateTimeFormatter dateTimeFormate12 = DateTimeFormatter.ofPattern("yyyyMM");
    public static DateTimeFormatter dateTimeFormate13 = DateTimeFormatter.ofPattern("yyyMM");
    public static DateTimeFormatter dateTimeFormate14 = DateTimeFormatter.ofPattern("yyy/MM");
    public static DateTimeFormatter dateTimeFormate15 = DateTimeFormatter.ofPattern("dd/MM/yy");
    public static DateTimeFormatter dateTimeFormate16 = DateTimeFormatter.ofPattern("HHmmss");
    public static DateTimeFormatter dateTimeFormate17 = DateTimeFormatter.ofPattern("yyy");
    public static DateTimeFormatter dateTimeFormate18 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String getDateNow(DateTimeFormatter formatter) {
        return getDateNow().format(formatter);
    }

    public static LocalDateTime getDateNow() {
        return LocalDateTime.now(ZoneOffset.of(UTC_ADD_8));
    }

    /**
     * 獲取當前年的前幾年/後幾年的日期
     *
     * @param yearsToAddOrSubtract 後幾年傳正整數，前幾年傳負數
     * @param formatter
     */
    public static String getMinusOrPlusYears(long yearsToAddOrSubtract, DateTimeFormatter formatter) {
        String date = !Objects.isNull(formatter) ? getDateNow().plusYears(yearsToAddOrSubtract).format(formatter) :
                LocalDate.now(ZoneOffset.of(UTC_ADD_8)).plusYears(yearsToAddOrSubtract) + "";
        return date;
    }

    /**
     * 獲取當前月的前幾月/後幾月的日期
     *
     * @param monthsToAddOrSubtract 後幾月傳正整數，前幾月傳負數
     * @param formatter
     */
    public static String getMinusOrPlusMonths(long monthsToAddOrSubtract, DateTimeFormatter formatter) {
        String date = !Objects.isNull(formatter) ? getDateNow().plusMonths(monthsToAddOrSubtract).format(formatter) :
                LocalDate.now(ZoneOffset.of(UTC_ADD_8)).plusMonths(monthsToAddOrSubtract) + "";
        return date;
    }

    /**
     * 獲取當前月的前幾月/後幾月的日期
     *
     * @param monthsToAddOrSubtract 後幾月傳正整數，前幾月傳負數
     * @param formatter
     * @param startDate 需計算的起始日期
     */
    public static String getMinusOrPlusMonthsByStartDate(long monthsToAddOrSubtract, DateTimeFormatter formatter, LocalDateTime startDate) {
        String date = !Objects.isNull(formatter) ? startDate.plusMonths(monthsToAddOrSubtract).format(formatter) :
                LocalDate.now(ZoneOffset.of(UTC_ADD_8)).plusMonths(monthsToAddOrSubtract) + "";
        return date;
    }

    /**
     * 獲取目前日的前幾日/後幾日的日期
     *
     * @param daysToAddOrSubtract 後幾日傳正整數，前幾日傳負數
     * @param formatter
     */
    public static String getMinusOrPlusDays(long daysToAddOrSubtract, DateTimeFormatter formatter) {
        String date = !Objects.isNull(formatter) ? getDateNow().plusDays(daysToAddOrSubtract).format(formatter) :
                LocalDate.now(ZoneOffset.of(UTC_ADD_8)).plusDays(daysToAddOrSubtract) + "";
        return date;
    }

    /**
     * 獲取目前星期的前幾星期/後幾星期的日期
     *
     * @param weeksToAddOrSubtract 後幾星期傳正整數，前幾星期傳負數
     * @param formatter
     */
    public static String getMinusOrPlusWeeks(long weeksToAddOrSubtract, DateTimeFormatter formatter) {
        String date = !Objects.isNull(formatter) ? getDateNow().plusWeeks(weeksToAddOrSubtract).format(formatter) :
                LocalDate.now(ZoneOffset.of(UTC_ADD_8)).plusWeeks(weeksToAddOrSubtract) + "";
        return date;
    }

    /**
     * 獲取當前小時的前幾小時/後幾小時的日期
     *
     * @param hoursToAddOrSubtract 後幾小時傳正整數，前幾小時傳負數
     * @param formatter
     */
    public static String getMinusOrPlusHours(long hoursToAddOrSubtract, DateTimeFormatter formatter) {
        String date = !Objects.isNull(formatter) ? getDateNow().plusHours(hoursToAddOrSubtract).format(formatter) :
                LocalTime.now(ZoneOffset.of(UTC_ADD_8)).plusHours(hoursToAddOrSubtract).format(formatter);
        return date;
    }

    /**
     * 獲取當前分鐘的前幾分鐘/後幾分鐘的日期
     *
     * @param minutesToAddOrSubtract 後幾分鐘傳正整數，前幾分鐘傳負數
     * @param formatter
     */
    public static String getMinusOrPlusMinutes(long minutesToAddOrSubtract, DateTimeFormatter formatter) {
        String date = !Objects.isNull(formatter) ? getDateNow().plusMinutes(minutesToAddOrSubtract).format(formatter) :
                LocalTime.now(ZoneOffset.of(UTC_ADD_8)).plusMinutes(minutesToAddOrSubtract).format(formatter);
        return date;
    }

    /**
     * 獲取當前秒的前幾秒/後幾秒的日期
     *
     * @param secondsToAddOrSubtract 後幾秒傳正整數，前幾秒傳負數
     * @param formatter
     */
    public static String getMinusOrPlusSeconds(long secondsToAddOrSubtract, DateTimeFormatter formatter) {
        String date = !Objects.isNull(formatter) ? getDateNow().plusSeconds(secondsToAddOrSubtract).format(formatter) :
                LocalTime.now(ZoneOffset.of(UTC_ADD_8)).plusSeconds(secondsToAddOrSubtract).format(formatter);
        return date;
    }

    /**
     * Date類型轉 LocalDateTime、LocalDate、LocalTime
     *
     * @param date
     * @param type 1:LocalDateTime; 2:LocalDate; 3: LocalTime
     */
    public static Object DateToJava8Date(Date date, Integer type) {
        Instant instant = date.toInstant();
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, zone);
        Object java8Date = null;
        switch (type) {
            case 1:
                java8Date = dateTime;
                break;
            case 2:
                java8Date = dateTime.toLocalDate();
                break;
            case 3:
                java8Date = dateTime.toLocalTime();
                break;
        }
        return java8Date;
    }

    /**
     * LocalDateTime、LocalDate、LocalTime類型轉Date
     *
     * @param java8Date LocalDateTime、LocalDate、LocalTime
     */
    public static Date Java8DateToDate(Object java8Date) {
        ZoneId zone = ZoneId.of(UTC_ADD_8);
        Instant instant = null;
        Date date = null;
        if (java8Date instanceof LocalDateTime) {
            instant = ((LocalDateTime) java8Date).atZone(zone).toInstant();
        }
        if (java8Date instanceof LocalDate) {
            instant = ((LocalDate) java8Date).atStartOfDay().atZone(zone).toInstant();
        }
        if (java8Date instanceof LocalTime) {
            LocalDateTime localDateTime = LocalDateTime.of(LocalDate.now(ZoneOffset.of(UTC_ADD_8)), LocalTime.now(ZoneOffset.of(UTC_ADD_8)));
            instant = localDateTime.atZone(zone).toInstant();
        }
        return instant == null ? null : Date.from(instant);
    }

    /**
     * 將指定字串格式轉為日期時間
     *
     * @param date
     * @param dateTimeFormatter
     * @return
     */
    public static LocalDateTime getDateTime(String date, DateTimeFormatter dateTimeFormatter) {
        return LocalDateTime.parse(date, dateTimeFormatter);
    }

    /**
     * 將指定字串格式轉為日期
     *
     * @param date
     * @param dateTimeFormatter
     * @return
     */
    public static LocalDate getDate(String date, DateTimeFormatter dateTimeFormatter) {
        return LocalDate.parse(date, dateTimeFormatter);
    }

    /**
     * Date 和 LocalDateTime 互轉
     *
     * @param date
     * @return
     */
    public static LocalDateTime dateToDateTime(Date date) {
        Instant instant = date.toInstant();
        return LocalDateTime.ofInstant(instant, ZoneId.of(UTC_ADD_8));
    }

    /**
     * date 轉為 LocalDate
     *
     * @param date
     * @return
     */
    public static LocalDate dateToLocalDate(Date date) {
        Instant instant = date.toInstant();
        return instant.atZone(ZoneId.of(UTC_ADD_8)).toLocalDate();
    }

    /**
     * LocalDate 轉為 Date
     *
     * @param localDate
     * @return
     */
    public static Date localDateToDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.of(UTC_ADD_8)).toInstant());
    }

    /**
     * Date 轉為 DateTime
     *
     * @param date
     * @return
     */
    public static LocalTime dateToLocalTime(Date date) {
        Instant instant = date.toInstant();
        return instant.atZone(ZoneId.of(UTC_ADD_8)).toLocalTime();
    }

    /**
     * 獲取當前時間之後的指定日期的最小時間
     *
     * @param date
     * @return
     */
    public static LocalDateTime afterXDateTimeMIN(Date date, int after) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.plusDays(after);
        localDateTime = localDateTime.with(LocalTime.MIN);
        return localDateTime;
    }

    /**
     * 獲取當前時間之後的指定日期的最大時間
     *
     * @param date
     * @return
     */
    public static LocalDateTime afterXDateTimeMAX(Date date, int after) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.plusDays(after);
        localDateTime = localDateTime.with(LocalTime.MAX);
        return localDateTime;
    }

    /**
     * 獲取當前時間之前的指定日期的最小時間
     *
     * @param date
     * @return
     */
    public static Date beforeXDateTimeMIN(Date date, int before) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.minusDays(before);
        localDateTime = localDateTime.with(LocalTime.MIN);
        return Date.from(localDateTime.atZone(ZoneId.of(UTC_ADD_8)).toInstant());
    }

    /**
     * 獲取當前時間之前的指定日期的最大時間
     *
     * @param date
     * @return
     */
    public static Date beforeXDateTimeMAX(Date date, int before) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.minusDays(before);
        localDateTime = localDateTime.with(LocalTime.MAX);
        return Date.from(localDateTime.atZone(ZoneId.of(UTC_ADD_8)).toInstant());
    }

    /**
     * 獲取本月的第一天 00:00:00
     *
     * @return
     */
    public static LocalDateTime currentFirstDayOfMonth() {
        LocalDateTime localDateTime = LocalDateTime.now();
        localDateTime = localDateTime.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        return localDateTime;
    }

    /**
     * 獲取指定時間月份的最後一天
     *
     * @return
     */
    public static LocalDateTime currentXDayOfMonth(Date date) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
        return localDateTime;
    }

    /**
     * 指定 date 的月份的第一天
     *
     * @return
     */
    public static LocalDateTime beforeXFirstDayOfMonth(Date date) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        return localDateTime;
    }

    /**
     * 獲取前幾個月的1號0點 00:00:00
     *
     * @param localDateTime
     * @param month
     * @return
     */
    public static LocalDateTime preXDayOfMonthMIN(LocalDateTime localDateTime, int month) {
        localDateTime = localDateTime.minusMonths(month);
        localDateTime = localDateTime.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        return localDateTime;
    }

    /**
     * 獲取前幾個月的1號0點 00:00:00
     *
     * @param date
     * @param month
     * @return
     */
    public static LocalDateTime preXDayOfMonthMIN(Date date, int month) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.minusMonths(month);
        localDateTime = localDateTime.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        return localDateTime;
    }

    /**
     * 獲取前幾個月的最後一天 23：59：59
     *
     * @return
     */
    public static LocalDateTime preXDayOfMonthMAX(int month) {
        LocalDateTime localDateTime = LocalDateTime.now();
        localDateTime = localDateTime.minusMonths(month);
        localDateTime = localDateTime.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
        return localDateTime;
    }

    /**
     * 獲取指定時間幾個月的最後一天 23：59：59
     *
     * @return
     */
    public static LocalDateTime preXDayOfMonthMAX(Date date, int month) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.minusMonths(month);
        localDateTime = localDateTime.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
        return localDateTime;
    }

    /**
     * 兩個日期相差多少個月
     *
     * @param date1
     * @param date2
     * @return
     */
    public static Long getUntilMonth(Date date1, Date date2) {
        LocalDate localDate1 = dateToLocalDate(date1);
        LocalDate localDate2 = dateToLocalDate(date2);
        return ChronoUnit.MONTHS.between(localDate1, localDate2);
    }

    /**
     * 兩個日期相差多少天
     *
     * @param date1
     * @param date2
     * @return
     */
    public static Long getUntilDay(Date date1, Date date2) {
        LocalDate localDate1 = dateToLocalDate(date1);
        LocalDate localDate2 = dateToLocalDate(date2);
        return ChronoUnit.DAYS.between(localDate1, localDate2);
    }

    /**
     * 兩個日期相差多少小時
     *
     * @param date1
     * @param date2
     * @return
     */
    public static Long getUntilHours(Date date1, Date date2) {
        LocalDateTime localDate1 = dateToDateTime(date1);
        LocalDateTime localDate2 = dateToDateTime(date2);
        Long senonds = Duration.between(localDate1, localDate2).get(ChronoUnit.SECONDS);
        return senonds / 3600;
    }

    /**
     * 兩個日期相差多少小時 double 約等於
     *
     * @param date1
     * @param date2
     * @return
     */
    public static double getUntilHoursByDouble(Date date1, Date date2) {
        LocalDateTime localDate1 = dateToDateTime(date1);
        LocalDateTime localDate2 = dateToDateTime(date2);
        Long seconds = Duration.between(localDate1, localDate2).get(ChronoUnit.SECONDS);
        BigDecimal secondss = BigDecimal.valueOf(seconds);
        BigDecimal hours = secondss.divide(BigDecimal.valueOf(3600), 2, BigDecimal.ROUND_HALF_UP);
        return hours.doubleValue();
    }

    /**
     * 兩個日期相差多少秒
     *
     * @param date1
     * @param date2
     * @return
     */
    public static Long getUntilSecond(Date date1, Date date2) {
        LocalDateTime localDate1 = dateToDateTime(date1);
        LocalDateTime localDate2 = dateToDateTime(date2);
        return Duration.between(localDate1, localDate2).get(ChronoUnit.SECONDS);
    }

    /**
     * 取得最大時間 23：59：59
     *
     * @param date
     * @return
     */
    public static Date currentMax(Date date) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.with(LocalTime.MAX);
        return Date.from(localDateTime.atZone(ZoneId.of(UTC_ADD_8)).toInstant());
    }

    /**
     * 取得最小時間 00:00:00
     *
     * @param date
     * @return
     */
    public static Date currentMin(Date date) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.with(LocalTime.MIN);
        return Date.from(localDateTime.atZone(ZoneId.of(UTC_ADD_8)).toInstant());
    }

    public static String getMinguoYear(){
        return MinguoDate.from(LocalDate.now(ZoneOffset.of(UTC_ADD_8))).format(dateTimeFormate17);
    }
    /**
     * Transfer AD date to minguo date.
     * 西元年 yyyyMM 轉 民國年 yyyMM
     *
     * @param dateString the String dateString
     * @return the string
     */
    public static String transferADDateToMinguoDate(String dateString) {
        YearMonth localDate = YearMonth.parse(dateString, dateTimeFormate12);
        return MinguoDate.from(localDate.atDay(1)).format(dateTimeFormate13);
    }

    /**
     * 每年一月會取上一年的民國年份
     *
     * @return
     */
    public static String getMinguoYearIfJanuary(){
        if (getDateNow().getMonthValue() == 1) {
            LocalDate localDate = LocalDate.parse(getMinusOrPlusYears(-1, dateTimeFormate7), dateTimeFormate7);
            return MinguoDate.from(localDate).format(dateTimeFormate17);
        } else {
            return getMinguoYear();
        }
    }
    /**
     * Transfer minguo date to AD date.
     * 民國年 yyyMMdd 轉 西元年 yyyy-MM-dd
     *
     * @param dateString the String dateString
     * @return the string
     */
    public static String transferMinguoDateToADDate(String dateString) {
        Chronology chrono = MinguoChronology.INSTANCE;
        DateTimeFormatter df = new DateTimeFormatterBuilder().parseLenient()
                .appendPattern("yyyMMdd")
                .toFormatter()
                .withChronology(chrono)
                .withDecimalStyle(DecimalStyle.of(Locale.getDefault()));
        ChronoLocalDate chDate = chrono.date(df.parse(dateString));
        return LocalDate.from(chDate).format(dateTimeFormate2);
    }

    /**
     * 當月份為一月時，系統自動會給予前一年份
     *
     * @return
     */
    public static String getLastYearOnJanuary() {
        if (getDateNow().getMonthValue() == 1) {
            return getMinusOrPlusYears(-1, dateTimeFormate9);
        } else {
            return String.valueOf(getDateNow().getYear());
        }
    }

    /**
     * 取得單月最小日期時間，如傳入為雙月，自動回傳前一個月最小日期時間
     *
     * @param localDateTime
     * @return
     */
    public static LocalDateTime firstMinDateOfSingleMonth(LocalDateTime localDateTime) {
        int month = localDateTime.getMonthValue();
        if (month % 2 == 0) {
            localDateTime = DateTimeUtils.preXDayOfMonthMIN(localDateTime, 1);
        } else {
            localDateTime = DateTimeUtils.preXDayOfMonthMIN(localDateTime, 0);
        }
        return localDateTime;
    }

    public static Date localTimeToDate(LocalTime localTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        //assuming year/month/date information is not important
        calendar.set(0, 0, 0, localTime.getHour(), localTime.getMinute(), localTime.getSecond());
        return calendar.getTime();
    }

    public static Date localDateTimeToDate(LocalDateTime localDateTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(localDateTime.getYear(), localDateTime.getMonthValue()-1, localDateTime.getDayOfMonth(),
                localDateTime.getHour(), localDateTime.getMinute(), localDateTime.getSecond());
        return calendar.getTime();
    }
    /**
     * 傳入日期取得前期雙月份
     * 如:傳入 202105 取得 03,04
     *    傳入 202106 取得 03,04
     *    傳入 202112 取得 09,10
     *
     * @param dateTime
     * @return
     */
    public static ArrayList<String> getPreviousMonths(LocalDateTime dateTime) {
        ArrayList<String> values = new ArrayList<>();
        if (dateTime.getMonthValue() % 2 == 0) {
            values.add(DateTimeUtils.getMinusOrPlusMonthsByStartDate(-2, DateTimeUtils.dateTimeFormate10, dateTime));
            values.add(DateTimeUtils.getMinusOrPlusMonthsByStartDate(-3, DateTimeUtils.dateTimeFormate10, dateTime));
        } else {
            values.add(DateTimeUtils.getMinusOrPlusMonthsByStartDate(-1, DateTimeUtils.dateTimeFormate10, dateTime));
            values.add(DateTimeUtils.getMinusOrPlusMonthsByStartDate(-2, DateTimeUtils.dateTimeFormate10, dateTime));
        }
        return values;
    }

    public static void main(String[] args) {
        System.out.println(DateTimeUtils.getMinusOrPlusMonths(-1, DateTimeUtils.dateTimeFormate14));
    }

}
