package jnpf.base.util.dataset;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import jnpf.base.entity.ProvinceEntity;
import jnpf.base.model.dataset.DataSetConfig;
import jnpf.base.model.dataset.DataSetSwapModel;
import jnpf.base.service.ProvinceService;
import jnpf.util.*;
import jnpf.util.visiual.DataTypeConst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据集转换工具
 *
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/7/15 11:08:03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSetSwapUtil {

    private final DataSetExecutor dataSetExecutor;

    private final ProvinceService provinceService;

    //缓存系统权限数据, 组织、部门、岗位、分组、角色、用户
    public static final boolean NEEDCACHE_SYS = true;

    public static final String S_S_S ="%s-%s-%s";

    /**
     * 数据转换
     *
     * @param printId
     * @param convertConfig
     * @param map
     */
    public void swapData(String printId, String convertConfig, Map<String, Object> map) {
        List<DataSetSwapModel> modelList = JsonUtil.getJsonToList(convertConfig, DataSetSwapModel.class);
        modelList = CollectionUtils.isEmpty(modelList) ? new ArrayList<>() : modelList;
        Map<String, Object> localCache = new ConcurrentHashMap<>();
        //初始化缓存
        dataSetExecutor.executorRedis(localCache, printId, modelList, map);
        swapData(localCache, printId, modelList, map);
    }

    private void swapData(Map<String, Object> localCache, String printId, List<DataSetSwapModel> swapList, Map<String, Object> map) {

        Map<String, Object> orgMap = (Map<String, Object>) localCache.get(CacheKeyUtil.SYS_ORG_TREE);
        Map<String, Object> depMap = (Map<String, Object>) localCache.get(CacheKeyUtil.SYS_DEP);
        Map<String, Object> posMap = (Map<String, Object>) localCache.get(CacheKeyUtil.SYS_POS);
        Map<String, Object> userMap = (Map<String, Object>) localCache.get(CacheKeyUtil.SYS_USER);
        Map<String, Object> roleMap = (Map<String, Object>) localCache.get(CacheKeyUtil.SYS_ROLE);
        Map<String, Object> groupMap = (Map<String, Object>) localCache.get(CacheKeyUtil.SYS_GROUP);
        String tenantId = Optional.ofNullable(TenantHolder.getDatasourceId()).orElse("");

        if (MapUtils.isNotEmpty(map)) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                if (Objects.isNull(map.get(key))) continue;
                List<Map<String, Object>> list = (List<Map<String, Object>>) map.get(key);
                DataClob.swapClob(list);
                for (Map<String, Object> item : list) {
                    for (DataSetSwapModel model : swapList) {
                        String field = model.getField();
                        String type = model.getType();
                        if(StringUtils.isNotEmpty(field) || StringUtils.isNotEmpty(type)) {
                            DataSetConfig config = model.getConfig();

                            String[] fields = field.split("\\.");
                            if (fields.length == 2) {
                                String dataSetName = fields[0];
                                String filedKey = fields[1];
                                Object value = item.get(filedKey);
                                if (!key.equals(dataSetName) || value == null || value.toString().trim().isEmpty()) continue;

                                String resultStr = "";
                                String redisKey;
                                switch (type) {
                                    case DataSetConstant.KEY_ORG:
                                        item.put(filedKey, getListDataSwap(orgMap, value, true));
                                        break;
                                    case DataSetConstant.KEY_DEP:
                                        item.put(filedKey, getListDataSwap(depMap, value, false));
                                        break;
                                    case DataSetConstant.KEY_POS:
                                        item.put(filedKey, getListDataSwap(posMap, value, false));
                                        break;
                                    case DataSetConstant.KEY_USER:
                                        item.put(filedKey, getListDataSwap(userMap, value, false));
                                        break;
                                    case DataSetConstant.KEY_ROLE:
                                        item.put(filedKey, getListDataSwap(roleMap, value, false));
                                        break;
                                    case DataSetConstant.KEY_GROUP:
                                        item.put(filedKey, getListDataSwap(groupMap, value, false));
                                        break;
                                    case DataSetConstant.KEY_DATE:
                                        item.put(filedKey, getDateOrTime(value,config,false));
                                        break;
                                    case DataSetConstant.KEY_TIME:
                                        item.put(filedKey, getDateOrTime(value,config,true));
                                        break;
                                    case DataSetConstant.KEY_SELECT:
                                        if (DataTypeConst.STATIC.equals(config.getDataType())) {
                                            redisKey = String.format(S_S_S, printId, field, DataTypeConst.STATIC);
                                            if (localCache.containsKey(redisKey)) {
                                                Map<String, Object> selectMap = (Map<String, Object>) localCache.get(redisKey);
                                                resultStr = getListDataSwap(selectMap, value, false);
                                                item.put(filedKey, resultStr);
                                            }
                                        }
                                        if (DataTypeConst.DICTIONARY.equals(config.getDataType())) {
                                            redisKey = String.format(S_S_S, tenantId, DataTypeConst.DICTIONARY, model.getConfig().getDictionaryType());
                                            if (localCache.containsKey(redisKey)) {
                                                List<Map<String, Object>> options = (List<Map<String, Object>>) localCache.get(redisKey);
                                                String propsValue = config.getPropsValue();
                                                Map<String, Object> selectMap = new HashMap<>();
                                                options.forEach(o -> selectMap.put(String.valueOf(o.get(propsValue)), o.get("fullName")));
                                                resultStr = getListDataSwap(selectMap, value, false);
                                                item.put(filedKey, resultStr);
                                            }
                                        }
                                        if (DataTypeConst.DYNAMIC.equals(config.getDataType())) {
                                            redisKey = String.format(S_S_S, tenantId, DataTypeConst.DYNAMIC, model.getConfig().getPropsUrl());
                                            if (localCache.containsKey(redisKey)) {
                                                List<Map<String, Object>> options = (List<Map<String, Object>>) localCache.get(redisKey);
                                                String propsValue = config.getPropsValue();
                                                String propsLabel = config.getPropsLabel();
                                                Map<String, Object> selectMap = new HashMap<>();
                                                options.forEach(o -> selectMap.put(String.valueOf(o.get(propsValue)), o.get(propsLabel)));
                                                resultStr = getListDataSwap(selectMap, value, false);
                                                item.put(filedKey, resultStr);
                                            }
                                        }
                                        break;
                                    case DataSetConstant.KEY_NUMBER:
                                        item.put(filedKey,getNumber(value,config));
                                        break;
                                    case DataSetConstant.KEY_ADDRESS:
                                        item.put(filedKey, getAddressStr(value));
                                        break;
                                    case DataSetConstant.KEY_USERS:
                                        try {
                                            List<String> dataNoSwapInMethod = getDataNoSwapInMethod(value);
                                            StringJoiner valueJoin = new StringJoiner(",");
                                            for (String data : dataNoSwapInMethod) {
                                                String id = data.contains("--") ? data.substring(0, data.lastIndexOf("--")) : data;
                                                String selecttype = data.contains("--") ? data.substring(data.lastIndexOf("--") + 2) : "";
                                                Map<String, Object> cacheMap;
                                                switch (selecttype) {
                                                    case "role":
                                                        cacheMap = roleMap;
                                                        break;
                                                    case "position":
                                                        cacheMap = posMap;
                                                        break;
                                                    case "company":
                                                    case "department":
                                                        cacheMap = depMap;
                                                        break;
                                                    case "group":
                                                        cacheMap = groupMap;
                                                        break;
                                                    case "user":
                                                    default:
                                                        cacheMap = userMap;
                                                        break;
                                                }
                                                valueJoin.add(Optional.ofNullable(cacheMap.get(id)).orElse("").toString());
                                            }
                                            item.put(filedKey, valueJoin.toString());
                                        } catch (Exception e) {
                                            log.error(e.getMessage());
                                        }
                                        break;
                                    case DataSetConstant.KEY_LOCATION:
                                        try {
                                            Map omap = JsonUtil.stringToMap(String.valueOf(value));
                                            resultStr = omap.get("fullAddress") != null ? omap.get("fullAddress").toString() : "";
                                            item.put(filedKey, resultStr);
                                        } catch (Exception e) {
                                            log.error(e.getMessage());
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }

                            }



                    }
                }

            }
        }

    }

    /**
     * 多级控件转换
     *
     * @param redis
     * @param value
     * @param isOrg 是否组织
     * @return
     */
    public static String getListDataSwap(Map<String, Object> redis, Object value, Boolean isOrg) {
        if (MapUtils.isEmpty(redis)) {
            return String.valueOf(value);
        }
        Object dataValue;
        try {
            List<List> list = JsonUtil.getJsonToList(String.valueOf(value), List.class);
            StringJoiner joiner = new StringJoiner(",");
            for (List listChild : list) {
                if (Boolean.TRUE.equals(isOrg)) {
                    String join = StringUtils.join(listChild, ",");
                    String value1 = redis.get(String.valueOf(join)) != null ? String.valueOf(redis.get(String.valueOf(join))) : "";
                    joiner.add(value1);
                } else {
                    StringJoiner aa = new StringJoiner("/");
                    for (Object object : listChild) {
                        String value1 = redis.get(String.valueOf(object)) != null ? String.valueOf(redis.get(String.valueOf(object))) : "";
                        if (StringUtil.isNotEmpty(value1)) {
                            aa.add(value1);
                        }
                    }
                    joiner.add(aa.toString());
                }
            }
            dataValue = joiner.toString();
        } catch (Exception e) {
            try {
                List<String> list = JsonUtil.getJsonToList(String.valueOf(value), String.class);
                String separator = ",";
                StringJoiner joiner = new StringJoiner(separator);
                if (Boolean.TRUE.equals(isOrg)) {
                    if (CollectionUtils.isNotEmpty(list)) {
                        for (String string : list) {
                            String value1 = redis.get(String.valueOf(string)) != null ? String.valueOf(redis.get(String.valueOf(string))) : "";
                            joiner.add(value1);
                        }
                    }


                } else {
                    for (Object listChild : list) {
                        String value1 = redis.get(String.valueOf(listChild)) != null ? String.valueOf(redis.get(String.valueOf(listChild))) : "";
                        if (StringUtil.isNotEmpty(value1)) {
                            joiner.add(value1);
                        }
                    }
                }
                dataValue = joiner.toString();
            } catch (Exception e1) {
                dataValue = redis.get(String.valueOf(value)) != null ? String.valueOf(redis.get(String.valueOf(value))) : "";
                if (Boolean.TRUE.equals(isOrg) && StringUtil.isEmpty(dataValue.toString())) {
                    for (Map.Entry<String, Object> entry : redis.entrySet()) {
                        String k = entry.getKey();
                        if (k.endsWith(String.valueOf(value))) {
                            dataValue = String.valueOf(redis.get(k));
                        }
                    }
                }
            }
        }
        return StringUtil.isNotEmpty(dataValue.toString()) ? dataValue.toString() : value.toString();
    }

    /**
     * 日期和时间控件转换
     *
     * @param value
     * @return
     */
    public static String getDateOrTime(Object value, DataSetConfig config, Boolean isTime) {
        List<Object> list = new ArrayList<>();
        if (value instanceof List) {
            list.addAll((List<Object>) value);
        } else {
            list.add(value);
        }
        StringJoiner joiner = new StringJoiner("~");
        for (Object object : list) {
            if (Boolean.TRUE.equals(isTime)) {
                joiner.add(object + "");
            } else {
                Long time = getDateObjToLong(object);
                joiner.add(DateUtil.dateToString(new Date(time), config.getFormat()));
            }
        }
        return joiner.toString();
    }

    /**
     * 日期和时间控件转换
     *
     * @param value
     * @return
     */
    public static String getNumber(Object value, DataSetConfig config) {
        List<Object> list = new ArrayList<>();
        if (value instanceof List) {
            list.addAll((List<Object>) value);
        } else {
            list.add(value);
        }
        StringJoiner joiner = new StringJoiner("~");
        for (Object object : list) {
            String resultStr = "";
            try {
                BigDecimal bd = new BigDecimal(String.valueOf(object));
                resultStr = bd.toPlainString();
                Integer precision = config.getPrecision();
                if (precision == 0 && resultStr.contains(".")) {
                    resultStr = resultStr.split("\\.")[0];
                } else if (precision > 0) {
                    String formatZ = "000000000000000";
                    String format = formatZ.substring(0, precision);
                    DecimalFormat decimalFormat = new DecimalFormat("0." + format);
                    resultStr = decimalFormat.format(bd);
                }

                if (config.isThousands()) {
                    resultStr = thousandsFormat(resultStr);
                }
                joiner.add(resultStr);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return joiner.toString();
    }

    /**
     * 时间转换兼容
     *
     * @param dateObj
     * @return
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

    /**
     * 获取地址名称
     *
     * @param value
     * @return
     */
    private String getAddressStr(Object value) {
        String addressStr = String.valueOf(value);
        try {
            List<List> list = JsonUtil.getJsonToList(String.valueOf(value), List.class);
            StringJoiner joiner1 = new StringJoiner(",");
            List<String> proDataS = new ArrayList<>();
            list.forEach(proDataS::addAll);
            Map<String, String> provinceNames = provinceService.getProList(proDataS).stream().collect(Collectors.toMap(ProvinceEntity::getId, ProvinceEntity::getFullName
                    , (k1, k2) -> k2
                    , () -> new LinkedHashMap<>(proDataS.size(), 1.0F)));

            for (List<String> addressList : list) {
                StringJoiner joiner2 = new StringJoiner("/");
                for (String addressId : addressList) {
                    String name = provinceNames.getOrDefault(addressId, "");
                    joiner2.add(name);
                }
                joiner1.add(joiner2.toString());
            }
            addressStr = joiner1.toString();
        } catch (Exception e) {
            try {
                List<String> addressList = JsonUtil.getJsonToList(String.valueOf(value), String.class);
                Map<String, String> provinceNames = provinceService.getProList(addressList).stream().collect(Collectors.toMap(ProvinceEntity::getId, ProvinceEntity::getFullName
                        , (k1, k2) -> k2
                        , () -> new LinkedHashMap<>(addressList.size(), 1.0F)));
                StringJoiner joiner2 = new StringJoiner("/");
                for (String addressId : addressList) {
                    String name = provinceNames.getOrDefault(addressId, "");
                    joiner2.add(name);
                }
                addressStr = joiner2.toString();
            } catch (Exception e1) {
                List<String> addressList = new ArrayList<>();
                addressList.add(String.valueOf(value));
                Map<String, String> provinceNames = provinceService.getProList(addressList).stream().collect(Collectors.toMap(ProvinceEntity::getId, ProvinceEntity::getFullName
                        , (k1, k2) -> k2
                        , () -> new LinkedHashMap<>(addressList.size(), 1.0F)));
                if (MapUtils.isNotEmpty(provinceNames)) {
                    addressStr = provinceNames.get(String.valueOf(value));
                }
            }
        }
        return addressStr;
    }

    /**
     * 千位符展示
     *
     * @param str
     * @return
     */
    public static String thousandsFormat(String str) {
        String regex = "(\\d)(?=(\\d{3})+$)";
        String replacement = "$1,";
        if (str.contains(".")) {
            String[] arr = str.split("\\.");
            arr[0] = arr[0].replaceAll(regex, replacement);
            return String.join(".", arr);
        }
        return str.replaceAll(regex, replacement);
    }

    /**
     * 用户组件-获取数据
     *
     * @param modelData
     * @return
     */
    public static List<String> getDataNoSwapInMethod(Object modelData) {
        List<String> dataValueList = new ArrayList<>();
        if (String.valueOf(modelData).startsWith("[")) {
            dataValueList = JsonUtil.getJsonToList(String.valueOf(modelData), String.class);
        } else {
            String[] modelDatas = String.valueOf(modelData).split(",");
            dataValueList.addAll(Arrays.asList(modelDatas));
        }
        return dataValueList;
    }


}
