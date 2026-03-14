package jnpf.base.util;

import cn.hutool.core.util.ObjectUtil;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.config.ConfigModel;
import jnpf.util.*;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 在线开发公用
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/7/28
 */
@Slf4j
public class FormPublicUtils {
    FormPublicUtils() {
    }

    /**
     * 递归控件，除子表外控件全部提到同级
     * 取子集，子表不外提
     *
     * @return
     */
    public static void recursionFieldsExceptChild(List<FieLdsModel> allFields, List<FieLdsModel> fieLdsModelList) {
        for (FieLdsModel fieLdsModel : fieLdsModelList) {
            ConfigModel config = fieLdsModel.getConfig();
            String jnpfKey = config.getJnpfKey();
            if (JnpfKeyConsts.CHILD_TABLE.equals(jnpfKey)) {
                allFields.add(fieLdsModel);
            } else {
                if (config.getChildren() != null) {
                    recursionFieldsExceptChild(allFields, config.getChildren());
                } else if (StringUtil.isNotEmpty(jnpfKey)) {
                    allFields.add(fieLdsModel);
                }
            }
        }
    }

    /**
     * 转换时间格式
     *
     * @param time
     * @return
     */
    public static String getTimeFormat(String time) {
        String result;
        switch (time.length()) {
            case 16:
                result = time + ":00";
                break;
            case 19:
                result = time;
                break;
            case 21:
                result = time.substring(0, time.length() - 2);
                break;
            case 10:
                result = time + " 00:00:00";
                break;
            case 8:
                result = "2000-01-01 " + time;
                break;
            case 7:
                result = time + "-01 00:00:00";
                break;
            case 4:
                result = time + "-01-01 00:00:00";
                break;
            default:
                result = "";
                break;
        }
        return result;
    }

    public static String getLastTimeFormat(String time) {
        String result;
        switch (time.length()) {
            case 16:
                result = time + ":00";
                break;
            case 19:
                result = time;
                break;
            case 10:
                result = time + " 23:59:59";
                break;
            case 8:
                result = "2000-01-01 " + time;
                break;
            case 7:
                //获取月份最后一天
                String[] split = time.split("-");
                Calendar cale = Calendar.getInstance();
                cale.set(Calendar.YEAR, Integer.valueOf(split[0]));//赋值年份
                cale.set(Calendar.MONTH, Integer.valueOf(split[1]) - 1);//赋值月份
                int lastDay = cale.getActualMaximum(Calendar.DAY_OF_MONTH);//获取月最大天数
                cale.set(Calendar.DAY_OF_MONTH, lastDay);//设置日历中月份的最大天数
                cale.set(Calendar.HOUR_OF_DAY, 23);
                cale.set(Calendar.SECOND, 59);
                cale.set(Calendar.MINUTE, 59);
                result = DateUtil.daFormatHHMMSS(cale.getTime().getTime());
                break;
            case 4:
                result = time + "-12-31 23:59:59";
                break;
            default:
                result = "";
                break;
        }
        return result;
    }

    /**
     * 判断时间是否在设置范围内
     *
     * @param swapDataVo
     * @param format
     * @param value
     * @param data
     * @param jnpfKey
     * @return
     */
    public static boolean dateTimeCondition(FieLdsModel swapDataVo, String format, Object value, Map<String, Object> data, String jnpfKey) {
        long valueTimeLong;
        //输入值转long
        if (value instanceof String) {
            valueTimeLong = cn.hutool.core.date.DateUtil.parse(String.valueOf(value), format).getTime();
        } else {
            //输入值按格式补全
            String timeFormat = getTimeFormat(String.valueOf(value));
            valueTimeLong = DateUtil.stringToDate(timeFormat).getTime();
        }
        boolean timeHasRangeError = false;
        //开始时间判断
        if (Boolean.TRUE.equals(swapDataVo.getConfig().getStartTimeRule())) {
            String startTimeValue = swapDataVo.getConfig().getStartTimeValue();
            String startTimeType = swapDataVo.getConfig().getStartTimeType();
            String startTimeTarget = swapDataVo.getConfig().getStartTimeTarget();
            String startTimeRelationField = swapDataVo.getConfig().getStartRelationField();
            //根据类型获取开始时间戳
            long startTimeLong = getDateTimeLong(data, jnpfKey, startTimeValue, startTimeType, startTimeTarget, startTimeRelationField, format);
            if (startTimeLong != 0 && valueTimeLong < startTimeLong) {
                timeHasRangeError = true;
            }
        }
        //结束时间判断
        if (Boolean.TRUE.equals(swapDataVo.getConfig().getEndTimeRule())) {
            String endTimeValue = swapDataVo.getConfig().getEndTimeValue();
            String endTimeType = swapDataVo.getConfig().getEndTimeType();
            String endTimeTarget = swapDataVo.getConfig().getEndTimeTarget();
            String endTimeRelationField = swapDataVo.getConfig().getEndRelationField();
            //根据类型获取开始时间戳
            long endTimeLong = getDateTimeLong(data, jnpfKey, endTimeValue, endTimeType, endTimeTarget, endTimeRelationField, format);
            if (endTimeLong != 0 && valueTimeLong > endTimeLong) {
                timeHasRangeError = true;
            }
        }
        return timeHasRangeError;
    }

    /**
     * 根据类型获取时间戳
     *
     * @param data
     * @param jnpfKey
     * @param timeValue
     * @param timeType
     * @param timeTarget
     * @return
     */
    public static long getDateTimeLong(Map<String, Object> data, String jnpfKey, String timeValue, String timeType, String timeTarget,
                                       String stringimeRelationField, String format) {
        if (StringUtil.isEmpty(timeValue)) {
            timeValue = "0";
        }
        long startTimeLong = 0;
        //当前格式的当前时间戳
        long timestampInMillis = new Date().getTime();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            String s = sdf.format(new Date());
            timestampInMillis = sdf.parse(s).getTime();
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
        }
        switch (timeType) {
            case "1"://特定时间
                if (JnpfKeyConsts.DATE.equals(jnpfKey)) {
                    startTimeLong = Long.parseLong(timeValue);
                } else {
                    String newDateStr = DateUtil.daFormat(new Date()) + " " + timeValue + (timeValue.length() > 6 ? "" : ":00");
                    startTimeLong = DateUtil.stringToDate(newDateStr).getTime();
                }
                break;
            case "2"://表单字段
                startTimeLong = setFormFiled(data, stringimeRelationField, startTimeLong);
                break;
            case "3"://填写当前时间
                startTimeLong = timestampInMillis;
                break;
            case "4"://当前时间前
                Calendar caledel = Calendar.getInstance();
                caledel.setTimeInMillis(timestampInMillis);
                if (JnpfKeyConsts.DATE.equals(jnpfKey)) {
                    getDate1(timeValue, timeTarget, caledel);
                } else {
                    getTime1(timeValue, timeTarget, caledel);
                }
                startTimeLong = caledel.getTime().getTime();
                break;
            case "5"://当前时间后
                Calendar cale = Calendar.getInstance();
                cale.setTimeInMillis(timestampInMillis);
                if (JnpfKeyConsts.DATE.equals(jnpfKey)) {
                    getDate2(timeValue, timeTarget, cale);
                } else {
                    getTime2(timeValue, timeTarget, cale);
                }
                startTimeLong = cale.getTime().getTime();
                break;
            default:
                break;
        }
        return startTimeLong;
    }

    private static long setFormFiled(Map<String, Object> data, String stringimeRelationField, long startTimeLong) {
        if (stringimeRelationField != null) {
            String fieldValue = "";
            if (stringimeRelationField.toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {//子
                String[] split = stringimeRelationField.split("-");
                fieldValue = data.get(split[1]) != null ? data.get(split[1]).toString() : "";
            } else {//主副
                Map<String, Object> mainAndMast = data.get("mainAndMast") != null ? JsonUtil.entityToMap(data.get("mainAndMast")) : data;
                fieldValue = mainAndMast.get(stringimeRelationField) != null ? mainAndMast.get(stringimeRelationField).toString() : "";
            }
            if (StringUtil.isNotEmpty(fieldValue)) {
                String timeFormat = getTimeFormat(fieldValue);
                startTimeLong = cn.hutool.core.date.DateUtil.parse(timeFormat, "yyyy-MM-dd HH:mm:ss").getTime();
            }
        }
        return startTimeLong;
    }

    private static void getDate1(String timeValue, String timeTarget, Calendar caledel) {
        switch (timeTarget) {
            case "1"://年
                caledel.set(Calendar.YEAR, caledel.get(Calendar.YEAR) - Integer.valueOf(timeValue));//赋值年份
                break;
            case "2"://月
                caledel.set(Calendar.MONTH, caledel.get(Calendar.MONTH) - Integer.valueOf(timeValue));
                break;
            case "3"://日
                caledel.set(Calendar.DAY_OF_MONTH, caledel.get(Calendar.DAY_OF_MONTH) - Integer.valueOf(timeValue));
                break;
            default:
                break;
        }
    }

    private static void getTime1(String timeValue, String timeTarget, Calendar caledel) {
        switch (timeTarget) {
            case "1"://时
                caledel.set(Calendar.HOUR_OF_DAY, caledel.get(Calendar.HOUR_OF_DAY) - Integer.valueOf(timeValue));
                break;
            case "2"://分
                caledel.set(Calendar.MINUTE, caledel.get(Calendar.MINUTE) - Integer.valueOf(timeValue));
                break;
            case "3"://秒
                caledel.set(Calendar.SECOND, caledel.get(Calendar.SECOND) - Integer.valueOf(timeValue));
                break;
            default:
                break;
        }
    }

    private static void getDate2(String timeValue, String timeTarget, Calendar cale) {
        switch (timeTarget) {
            case "1"://年
                cale.set(Calendar.YEAR, cale.get(Calendar.YEAR) + Integer.valueOf(timeValue));//赋值年份
                break;
            case "2"://月
                cale.set(Calendar.MONTH, cale.get(Calendar.MONTH) + Integer.valueOf(timeValue));
                break;
            case "3"://日
                cale.set(Calendar.DAY_OF_MONTH, cale.get(Calendar.DAY_OF_MONTH) + Integer.valueOf(timeValue));
                break;
            default:
                break;
        }
    }

    private static void getTime2(String timeValue, String timeTarget, Calendar cale) {
        switch (timeTarget) {
            case "1"://时
                cale.set(Calendar.HOUR_OF_DAY, cale.get(Calendar.HOUR_OF_DAY) + Integer.valueOf(timeValue));
                break;
            case "2"://分
                cale.set(Calendar.MINUTE, cale.get(Calendar.MINUTE) + Integer.valueOf(timeValue));
                break;
            case "3"://秒
                cale.set(Calendar.SECOND, cale.get(Calendar.SECOND) + Integer.valueOf(timeValue));
                break;
            default:
                break;
        }
    }

    /**
     * 字符串转数组
     *
     * @param value 值
     * @return
     */
    public static Object getDataConversion(Object value) {
        if (value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Double || value instanceof BigDecimal)
            return value;
        return getDataConversion(null, value, false, "/");
    }

    /**
     * 字符串转数组
     *
     * @param redis 转换对象
     * @param value 值
     * @return
     */
    public static Object getDataConversion(Map<String, Object> redis, Object value, boolean isMultiple, String separator) {
        Object dataValue = value;
        boolean iszhuanhuan = redis != null;
        try {
            List<List<Object>> list = (List<List<Object>>) (List<?>) JsonUtil.getJsonToList(String.valueOf(value), List.class);
            dataValue = list;
            if (iszhuanhuan) {
                //一级分隔符
                StringJoiner joiner = new StringJoiner(",");
                for (List<Object> listChild : list) {
                    StringJoiner aa = new StringJoiner(separator);
                    for (Object object : listChild) {
                        String value1 = redis.get(String.valueOf(object)) != null ? String.valueOf(redis.get(String.valueOf(object))) : "";
                        if (StringUtil.isNotEmpty(value1)) {
                            aa.add(value1);
                        }
                    }
                    joiner.add(aa.toString());
                }
                dataValue = joiner.toString();
            }
        } catch (Exception e) {
            try {
                List<String> list = JsonUtil.getJsonToList(String.valueOf(value), String.class);
                dataValue = list;
                if (iszhuanhuan) {
                    if (isMultiple) {//一级分隔符
                        separator = ",";
                    }
                    StringJoiner joiner = new StringJoiner(separator);
                    for (Object listChild : list) {
                        String value1 = redis.get(String.valueOf(listChild)) != null ? String.valueOf(redis.get(String.valueOf(listChild))) : "";
                        if (StringUtil.isNotEmpty(value1)) {
                            joiner.add(value1);
                        }
                    }
                    dataValue = joiner.toString();
                }
            } catch (Exception e1) {
                dataValue = String.valueOf(value);
                if (iszhuanhuan) {
                    dataValue = redis.get(String.valueOf(value)) != null ? String.valueOf(redis.get(String.valueOf(value))) : "";
                }
            }
        }
        return dataValue;
    }

    /**
     * 给列表数据添加id
     *
     * @param dataList
     * @param key
     */
    public static List<Map<String, Object>> addIdToList(List<Map<String, Object>> dataList, String key) {
        return dataList.stream().map(data -> {
            data.put(FlowFormConstant.ID, data.get(key));
            String flwoId = null;
            if (data.get(TableFeildsEnum.FLOWID.getField()) != null) {
                flwoId = String.valueOf(data.get(TableFeildsEnum.FLOWID.getField()));
            }
            if (data.get(TableFeildsEnum.FLOWID.getField().toUpperCase()) != null) {
                flwoId = String.valueOf(data.get(TableFeildsEnum.FLOWID.getField().toUpperCase()));
            }
            data.put(FlowFormConstant.FLOWID, flwoId);
            String flowTaskId = null;
            if (data.get(TableFeildsEnum.FLOWTASKID.getField()) != null) {
                flowTaskId = String.valueOf(data.get(TableFeildsEnum.FLOWTASKID.getField()));
            }
            if (data.get(TableFeildsEnum.FLOWTASKID.getField().toUpperCase()) != null) {
                flowTaskId = String.valueOf(data.get(TableFeildsEnum.FLOWTASKID.getField().toUpperCase()));
            }
            data.put(FlowFormConstant.FLOWTASKID, flowTaskId);

            return data;
        }).collect(Collectors.toList());
    }

    /**
     * 关联表单获取原字段数据（数据类型也要转换）
     *
     * @param dataMap
     * @param jnpfKey
     * @param obj
     * @param vModel
     */
    public static void relationGetJnpfId(Map<String, Object> dataMap, String jnpfKey, Object obj, String vModel) {
        String vModeljnpfId = vModel + "_jnpfId";
        switch (jnpfKey) {
            case JnpfKeyConsts.CREATETIME:
            case JnpfKeyConsts.MODIFYTIME:
            case JnpfKeyConsts.DATE:
            case JnpfKeyConsts.DATE_CALCULATE:
                Long dateTime = DateTimeFormatConstant.getDateObjToLong(dataMap.get(vModel));
                dataMap.put(vModeljnpfId, dateTime != null ? dateTime : dataMap.get(vModel));
                break;
            case JnpfKeyConsts.CHILD_TABLE:
                break;
            case JnpfKeyConsts.SWITCH:
            case JnpfKeyConsts.SLIDER:
            case JnpfKeyConsts.RATE:
            case JnpfKeyConsts.CALCULATE:
            case JnpfKeyConsts.NUM_INPUT:
                dataMap.put(vModeljnpfId, ObjectUtil.isNotEmpty(obj) ? new BigDecimal(String.valueOf(obj)) : dataMap.get(vModel));
                break;
            default:
                dataMap.put(vModeljnpfId, obj);
                break;
        }
        if (JnpfKeyConsts.getArraysKey().contains(jnpfKey) && obj != null) {
            String o = String.valueOf(obj);
            try {
                if(o.contains("[")) {
                    List<List<Object>> jsonToList = (List<List<Object>>) (List<?>) JsonUtil.getJsonToList(o, List.class);
                    List<Object> res = new ArrayList<>();
                    for (List<Object> listChild : jsonToList) {
                        List<Object> res2 = new ArrayList<>();
                        for (Object object : listChild) {
                            if (object != null && StringUtil.isNotEmpty(String.valueOf(object))) {
                                res2.add(object);
                            }
                        }
                        res.add(res2);
                    }
                    dataMap.put(vModeljnpfId, res);
                }
            } catch (Exception e) {
                try {
                    if(o.contains("{")) {
                        List<Object> jsonToList = JsonUtil.getJsonToList(o, Object.class);
                        dataMap.put(vModeljnpfId, jsonToList);
                    }
                } catch (Exception e1) {
                    // 尝试转换其他结果, 忽略所有错误
                }
            }
        }
    }
}

