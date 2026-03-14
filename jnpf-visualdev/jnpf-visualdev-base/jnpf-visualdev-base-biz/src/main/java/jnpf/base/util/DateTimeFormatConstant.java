package jnpf.base.util;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import jnpf.util.DateUtil;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 时间格式 常量
 */
public class DateTimeFormatConstant {
    DateTimeFormatConstant() {
    }

    public static final String YEAR = "yyyy";
    public static final String YEAR_MONTH = "yyyy-MM";
    public static final String YEAR_MONTH_DATE = "yyyy-MM-dd";
    public static final String YEAR_MONTH_DHM = "yyyy-MM-dd HH:mm";
    public static final String YEAR_MONTH_DHMS = "yyyy-MM-dd HH:mm:ss";
    public static final String HOUR_MINUTE = "HH:mm";
    public static final String HOUR_MINUTE_SECOND = "HH:mm:ss";

    /**
     * 时间格式忽略大小写
     *
     * @param otherFormat
     * @return 返回固定格式
     */
    public static String getFormat(String otherFormat) {
        if (YEAR.equalsIgnoreCase(otherFormat)) {
            return YEAR;
        }
        if (YEAR_MONTH.equalsIgnoreCase(otherFormat)) {
            return YEAR_MONTH;
        }
        if (YEAR_MONTH_DATE.equalsIgnoreCase(otherFormat)) {
            return YEAR_MONTH_DATE;
        }
        if (YEAR_MONTH_DHM.equalsIgnoreCase(otherFormat)) {
            return YEAR_MONTH_DHM;
        }
        if (YEAR_MONTH_DHMS.equalsIgnoreCase(otherFormat)) {
            return YEAR_MONTH_DHMS;
        }
        if (HOUR_MINUTE.equalsIgnoreCase(otherFormat)) {
            return HOUR_MINUTE;
        }
        if (HOUR_MINUTE_SECOND.equalsIgnoreCase(otherFormat)) {
            return HOUR_MINUTE_SECOND;
        }
        return otherFormat;
    }

    /**
     * 数据库查询时间字段-转换成long
     * 不同数据库查询结果的对象不同
     *
     * @param dateObj
     * @return long
     */
    public static Long getDateObjToLong(Object dateObj) {
        LocalDateTime dateTime = null;
        if (ObjectUtil.isNotEmpty(dateObj)) {
            if (dateObj instanceof LocalDateTime) {
                dateTime = (LocalDateTime) dateObj;
            } else if (dateObj instanceof Timestamp) {
                dateTime = ((Timestamp) dateObj).toLocalDateTime();
            } else if (dateObj instanceof Long) {
                dateTime = LocalDateTimeUtil.of(new Date(Long.parseLong(dateObj.toString())));
            } else {
                dateTime = LocalDateTimeUtil.of(cn.hutool.core.date.DateUtil.parse(dateObj.toString()));
            }
        }
        return dateTime != null ? DateUtil.localDateTime2Millis(dateTime) : null;
    }

}
