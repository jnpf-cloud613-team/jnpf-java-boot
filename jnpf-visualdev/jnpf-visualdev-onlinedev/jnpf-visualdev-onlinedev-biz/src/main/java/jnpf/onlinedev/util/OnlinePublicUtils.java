package jnpf.onlinedev.util;


import jnpf.base.entity.DataInterfaceEntity;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.entity.VisualdevReleaseEntity;
import jnpf.base.model.ColumnDataModel;
import jnpf.base.model.VisualDevJsonModel;
import jnpf.base.model.VisualWebTypeEnum;
import jnpf.base.model.template.ColumnListField;
import jnpf.constant.MsgCode;
import jnpf.database.model.query.SuperJsonModel;
import jnpf.database.model.query.SuperQueryJsonModel;
import jnpf.exception.WorkFlowException;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.FormDataModel;
import jnpf.model.visualjson.TableModel;
import jnpf.model.visualjson.config.ConfigModel;
import jnpf.onlinedev.model.enums.MultipleControlEnum;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.XSSEscape;
import jnpf.util.visiual.JnpfKeyConsts;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

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
public class OnlinePublicUtils {
    OnlinePublicUtils() {
    }

    /**
     * map key转小写
     *
     * @param requestMap
     * @return
     */
    public static Map<String, Object> mapKeyToLower(Map<String, ?> requestMap) {
        // 非空校验
        if (requestMap.isEmpty()) {
            return Collections.emptyMap();
        }
        // 初始化放转换后数据的Map
        Map<String, Object> responseMap = new HashMap<>(16);
        // 使用迭代器进行循环遍历
        Set<String> requestSet = requestMap.keySet();
        Iterator<String> iterator = requestSet.iterator();
        iterator.forEachRemaining(obj -> {
            // 判断Key对应的Value是否为Map
            if ((requestMap.get(obj) instanceof Map)) {
                // 递归调用，将value中的Map的key转小写
                responseMap.put(obj.toLowerCase(), mapKeyToLower((Map) requestMap.get(obj)));
            } else {
                // 直接将key小写放入responseMap
                responseMap.put(obj.toLowerCase(), requestMap.get(obj));
            }
        });

        return responseMap;
    }

    /**
     * 递归控件
     *
     * @param allFields
     * @param fieLdsModelList
     * @return
     */
    public static void recursionFields(List<FieLdsModel> allFields, List<FieLdsModel> fieLdsModelList) {
        for (FieLdsModel fieLdsModel : fieLdsModelList) {
            ConfigModel config = fieLdsModel.getConfig();
            String jnpfKey = config.getJnpfKey();
            if (JnpfKeyConsts.CHILD_TABLE.equals(jnpfKey)) {
                allFields.add(fieLdsModel);
            } else {
                if (config.getChildren() != null) {
                    recursionFields(allFields, config.getChildren());
                } else {
                    if (jnpfKey != null) {
                        allFields.add(fieLdsModel);
                    }
                }
            }
        }
    }

    /**
     * 判断字符串是否有某个字符存在
     *
     * @param var1 完整字符串
     * @param var2 统计字符
     * @return
     */
    public static boolean getMultiple(String var1, String var2) {
        if (var1 == null) {
            return false;
        }
        return var1.startsWith(var2);
    }

    /**
     * 数据字典处理（从缓存中取出）
     *
     * @param dataList
     * @param swapModel
     * @return
     */
    public static Map<String, Object> getDataMap(List<Map<String, Object>> dataList, FieLdsModel swapModel) {
        String label = swapModel.getProps() != null ? swapModel.getProps().getLabel() : "";
        String value = swapModel.getProps() != null ? swapModel.getProps().getValue() : "";
        Map<String, Object> dataInterfaceMap = new HashMap<>();
        dataList.stream().forEach(data -> dataInterfaceMap.put(String.valueOf(data.get(value)), String.valueOf(data.get(label))));
        return dataInterfaceMap;
    }

    /**
     * 递归控件
     *
     * @return
     */
    public static void recursionFormFields(List<FieLdsModel> allFields, List<FieLdsModel> fieLdsModelList) {
        for (FieLdsModel fieLdsModel : fieLdsModelList) {
            ConfigModel config = fieLdsModel.getConfig();
            String jnpfKey = config.getJnpfKey();
            if (JnpfKeyConsts.CHILD_TABLE.equals(jnpfKey)) {
                allFields.add(fieLdsModel);
            } else {
                if (config.getChildren() != null) {
                    recursionFormFields(allFields, config.getChildren());
                } else {
                    allFields.add(fieLdsModel);
                }
            }
        }
    }

    /**
     * 递归控件(取出所有子集)
     *
     * @return
     */
    public static void recursionFormChildFields(List<FieLdsModel> allFields, List<FieLdsModel> fieLdsModelList) {
        for (FieLdsModel fieLdsModel : fieLdsModelList) {
            ConfigModel config = fieLdsModel.getConfig();
            String jnpfKey = config.getJnpfKey();
            if (JnpfKeyConsts.CHILD_TABLE.equals(jnpfKey)) {
                String childVmodel = fieLdsModel.getVModel();
                String childLabel = fieLdsModel.getConfig().getLabel();
                for (FieLdsModel child : Optional.ofNullable(fieLdsModel.getConfig().getChildren()).orElse(new ArrayList<>())) {
                    if (child.getVModel() != null) {
                        child.setVModel(childVmodel + "-" + child.getVModel());
                        child.getConfig().setLabel(childLabel + "-" + child.getConfig().getLabel());
                        allFields.add(child);
                    }
                }
            } else {
                if (config.getChildren() != null) {
                    recursionFormChildFields(allFields, config.getChildren());
                } else {
                    if (StringUtil.isNotEmpty(fieLdsModel.getVModel())) {
                        allFields.add(fieLdsModel);
                    }
                }
            }
        }
    }

    /**
     * @param redisMap   缓存集合
     * @param modelData  数据
     * @param isMultiple 是否多选
     * @return
     */
    public static String getDataInMethod(Map<String, Object> redisMap, Object modelData, boolean isMultiple) {
        if (redisMap == null || redisMap.isEmpty()) {
            return modelData.toString();
        }
        String separator = isMultiple ? ";" : "/";
        String s2;
        if (OnlinePublicUtils.getMultiple(String.valueOf(modelData), MultipleControlEnum.MULTIPLE_JSON_TWO.getMultipleChar())) {
            String[][] data = JsonUtil.getJsonToBean(String.valueOf(modelData), String[][].class);
            List<String> addList = new ArrayList<>();
            for (String[] AddressData : data) {
                List<String> adList = new ArrayList<>();
                for (String s : AddressData) {
                    adList.add(String.valueOf(redisMap.get(s)));
                }
                addList.add(String.join("/", adList));
            }
            s2 = String.join(";", addList);
        } else if (OnlinePublicUtils.getMultiple(String.valueOf(modelData), MultipleControlEnum.MULTIPLE_JSON_ONE.getMultipleChar())) {
            List<String> modelDataList = JsonUtil.getJsonToList(String.valueOf(modelData), String.class);
            modelDataList = modelDataList.stream().map(s -> String.valueOf(redisMap.get(s))).collect(Collectors.toList());
            s2 = String.join(separator, modelDataList);
        } else {
            String[] modelDatas = String.valueOf(modelData).split(",");
            StringBuilder dynamicData = new StringBuilder();
            for (int i = 0; i < modelDatas.length; i++) {
                modelDatas[i] = String.valueOf(Objects.nonNull(redisMap.get(modelDatas[i])) ? redisMap.get(modelDatas[i]) : "");
                dynamicData.append(modelDatas[i]);
                dynamicData.append(separator);
            }
            s2 = dynamicData.deleteCharAt(dynamicData.length() - 1).toString();
        }
        return StringUtil.isEmpty(s2) ? modelData.toString() : s2;
    }

    public static List<String> getDataNoSwapInMethod(Object modelData) {
        List<String> dataValueList = new ArrayList<>();
        if (OnlinePublicUtils.getMultiple(String.valueOf(modelData), MultipleControlEnum.MULTIPLE_JSON_ONE.getMultipleChar())) {
            List<String> modelDataList = JsonUtil.getJsonToList(String.valueOf(modelData), String.class);
            dataValueList = modelDataList;
        } else {
            dataValueList.addAll(Arrays.asList(String.valueOf(modelData).split(",")));
        }
        return dataValueList;
    }

    public static VisualDevJsonModel getVisualJsonModel(VisualdevEntity entity) {
        VisualDevJsonModel jsonModel = new VisualDevJsonModel();
        if (entity.getColumnData() != null) {
            jsonModel.setColumnData(JsonUtil.getJsonToBean(entity.getColumnData(), ColumnDataModel.class));
        }
        if (entity.getAppColumnData() != null) {
            jsonModel.setAppColumnData(JsonUtil.getJsonToBean(entity.getAppColumnData(), ColumnDataModel.class));
        }
        FormDataModel formDataModel = JsonUtil.getJsonToBean(entity.getFormData(), FormDataModel.class);
        jsonModel.setFormData(formDataModel);
        if (!VisualWebTypeEnum.DATA_VIEW.getType().equals(entity.getWebType())) {
            jsonModel.setFormListModels(JsonUtil.getJsonToList(formDataModel.getFields(), FieLdsModel.class));
        }
        jsonModel.setVisualTables(JsonUtil.getJsonToList(entity.getVisualTables(), TableModel.class));
        jsonModel.setId(entity.getId());
        jsonModel.setDbLinkId(entity.getDbLinkId());
        jsonModel.setFullName(entity.getFullName());
        jsonModel.setType(entity.getType());
        jsonModel.setWebType(entity.getWebType());
        return jsonModel;
    }

    public static VisualDevJsonModel getVisualJsonModel(VisualdevReleaseEntity entity) throws WorkFlowException {
        if (entity == null) throw new WorkFlowException(MsgCode.VS412.get());
        VisualDevJsonModel jsonModel = new VisualDevJsonModel();
        if (entity.getColumnData() != null) {
            jsonModel.setColumnData(JsonUtil.getJsonToBean(entity.getColumnData(), ColumnDataModel.class));
        }
        if (entity.getAppColumnData() != null) {
            jsonModel.setAppColumnData(JsonUtil.getJsonToBean(entity.getAppColumnData(), ColumnDataModel.class));
        }
        FormDataModel formDataModel = JsonUtil.getJsonToBean(entity.getFormData(), FormDataModel.class);
        jsonModel.setFormData(formDataModel);
        if (!VisualWebTypeEnum.DATA_VIEW.getType().equals(entity.getWebType())) {
            jsonModel.setFormListModels(JsonUtil.getJsonToList(formDataModel.getFields(), FieLdsModel.class));
        }
        jsonModel.setVisualTables(JsonUtil.getJsonToList(entity.getVisualTables(), TableModel.class));
        jsonModel.setId(entity.getId());
        jsonModel.setDbLinkId(entity.getDbLinkId());
        jsonModel.setFullName(entity.getFullName());
        jsonModel.setType(entity.getType());
        jsonModel.setWebType(entity.getWebType());
        return jsonModel;
    }

    /**
     * 视图sql条件拼接
     *
     * @param info
     * @param queryCondition
     * @param parameterMap
     * @return
     */
    public static void getViewQuerySql(DataInterfaceEntity info, List<FieLdsModel> queryCondition,
                                       Map<String, String> parameterMap, Map<String, Object> extraMap) {
        if (!Objects.equals(info.getType(), 1) || queryCondition.isEmpty()) {
            return;
        }

        List<String> conditions = new ArrayList<>();
        List<String> values = new ArrayList<>();
        // 处理查询条件
        for (FieLdsModel item : queryCondition) {
            String condition = buildSearchCondition(item, values);
            if (StringUtil.isNotEmpty(condition)) {
                conditions.add(condition);
            }
        }

        // 处理额外条件
        if (MapUtils.isNotEmpty(extraMap)) {
            for (Map.Entry<String, Object> entry : extraMap.entrySet()) {
                String fieldName = "t." + entry.getKey();
                Object fieldValue = entry.getValue();
                if (fieldValue != null && StringUtil.isNotEmpty(fieldValue.toString())) {
                    values.add(XSSEscape.escape(fieldValue.toString()));
                    conditions.add(fieldName + " = ?");
                }
            }
        }

        // 构建最终SQL
        if (!conditions.isEmpty()) {
            String searchSqlStr = String.join(" and ", conditions);
            parameterMap.put("searchSqlStr", searchSqlStr);
            parameterMap.put("searchValues", JsonUtil.getObjectToString(values));
        }
    }

    /**
     * 构建查询条件 - 合并所有搜索类型的处理逻辑
     */
    private static String buildSearchCondition(FieLdsModel item, List<String> values) {
        if (item == null) {
            return null;
        }

        String fieldName = "t." + item.getVModel();
        String escapedValue;

        switch (item.getSearchType()) {
            case 1: // 等于
                Object fieldValue = item.getFieldValue();
                if (fieldValue == null || StringUtil.isEmpty(fieldValue.toString())) {
                    return null;
                }
                escapedValue = XSSEscape.escape(fieldValue.toString());
                values.add(escapedValue);
                return fieldName + " = ?";

            case 2: // 模糊查询
                fieldValue = item.getFieldValue();
                if (fieldValue == null || StringUtil.isEmpty(fieldValue.toString())) {
                    return null;
                }
                escapedValue = XSSEscape.escape(fieldValue.toString());
                values.add("%" + escapedValue + "%");
                return fieldName + " like ?";

            case 3: // BETWEEN
                Object valueOne = item.getFieldValueOne();
                Object valueTwo = item.getFieldValueTwo();
                if (valueOne == null || valueTwo == null) {
                    return null;
                }
                String escapedOne = XSSEscape.escape(String.valueOf(valueOne));
                String escapedTwo = XSSEscape.escape(String.valueOf(valueTwo));
                values.add(escapedOne);
                values.add(escapedTwo);
                return fieldName + " between ? and ?";

            case 4: // 包含（多个值的模糊查询）
                List<String> dataList = item.getDataList();
                if (dataList == null || dataList.isEmpty()) {
                    return null;
                }

                List<String> likeConditions = new ArrayList<>();
                for (String value : dataList) {
                    if (StringUtil.isNotEmpty(value)) {
                        escapedValue = XSSEscape.escape(value);
                        values.add("%" + escapedValue + "%");
                        likeConditions.add(fieldName + " like ?");
                    }
                }

                if (likeConditions.isEmpty()) {
                    return null;
                }
                return "(" + String.join(" or ", likeConditions) + ")";

            default:
                return null;
        }
    }

    /**
     * 视图非sql条件过滤
     *
     * @param info
     * @param queryCondition
     * @param dataRes
     * @return
     */
    public static List<Map<String, Object>> getViewQueryNotSql(DataInterfaceEntity info, List<FieLdsModel> queryCondition, List<Map<String, Object>> dataRes, Map<String, Object> extraMap) {
        //是否包含页签参数
        boolean hasExtra = false;
        String key = "";
        if (MapUtils.isNotEmpty(extraMap)) {
            hasExtra = true;
            List<String> keyList = new ArrayList<>(extraMap.keySet());
            key = keyList.get(0);
        }
        List<Map<String, Object>> dataInterfaceList = new ArrayList<>();
        if (!Objects.equals(info.getType(), 1) && !queryCondition.isEmpty()) {
            for (Map<String, Object> map : dataRes) {
                if (OnlinePublicUtils.mapCompar(queryCondition, map)) {
                    if (hasExtra) {
                        if (Objects.equals(map.get(key), extraMap.get(key))) dataInterfaceList.add(map);
                    } else {
                        dataInterfaceList.add(map);
                    }
                }
            }
        } else {
            for (Map<String, Object> map : dataRes) {
                if (hasExtra) {
                    if (Objects.equals(map.get(key), extraMap.get(key))) dataInterfaceList.add(map);
                } else {
                    dataInterfaceList.add(map);
                }
            }
        }
        return dataInterfaceList;
    }

    /**
     * 判断两个map有相同key-value
     *
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2023/1/5
     */
    public static boolean mapCompar(List<FieLdsModel> searchList, Map<String, Object> hashMap2) {
        boolean isChange = false;
        for (FieLdsModel item : searchList) {
            String realValue = hashMap2.get(item.getVModel()) == null ? "" : (String) hashMap2.get(item.getVModel());
            switch (item.getSearchType()) {
                case 2:
                    if (realValue.indexOf(item.getFieldValue()) >= 0) {
                        isChange = true;
                    }
                    break;
                case 3://between
                    List<String> longList = Arrays.asList(JnpfKeyConsts.NUM_INPUT, JnpfKeyConsts.DATE, JnpfKeyConsts.DATE_CALCULATE);
                    if (longList.contains(item.getConfig().getJnpfKey())) {
                        Long valueLong = Long.parseLong(realValue);
                        Long valueLongOne = (Long) item.getFieldValueOne();
                        Long valueLongTwo = (Long) item.getFieldValueTwo();
                        if (valueLong >= valueLongOne && valueLong <= valueLongTwo) {
                            isChange = true;
                        }
                    } else {
                        String valueLongOne = (String) item.getFieldValueOne();
                        String valueLongTwo = (String) item.getFieldValueTwo();
                        if (realValue.compareTo(valueLongOne) >= 0 && realValue.compareTo(valueLongTwo) <= 0) {
                            isChange = true;
                        }
                    }
                    break;
                case 4://包含
                    List<String> dataList = item.getDataList();
                    for (String value : dataList) {
                        isChange = value.indexOf(realValue) >= 0;
                    }
                    if (isChange) {
                        return true;
                    }
                    break;
                default://1,其他条件都按等于查询
                    isChange = item.getFieldValue().equals(realValue);
                    break;
            }
        }
        return isChange;
    }

    /**
     * 获取所有有使用的表
     *
     * @return
     */
    public static List<String> getAllTableName(List<ColumnListField> modelList, List<SuperJsonModel> listQuery, Map<String, String> fieldTableMap) {
        List<String> list = new ArrayList<>();

        for (ColumnListField item : modelList) {
            String table = StringUtil.isNotEmpty(item.getConfig().getRelationTable()) ?
                    item.getConfig().getRelationTable() : item.getConfig().getTableName();
            if (StringUtils.isBlank(table)) {
                table = fieldTableMap.get(item.getConfig().getParentVModel());
            }
            list.add(table);
        }
        for (SuperJsonModel superJsonModel : listQuery) {
            if (superJsonModel.getConditionList() != null && !superJsonModel.getConditionList().isEmpty()) {
                List<SuperQueryJsonModel> conditionList = superJsonModel.getConditionList();
                for (SuperQueryJsonModel sqj : conditionList) {
                    if (sqj.getGroups() != null && !sqj.getGroups().isEmpty()) {
                        List<FieLdsModel> groups = sqj.getGroups();
                        for (FieLdsModel item : groups) {
                            String table = StringUtil.isNotEmpty(item.getConfig().getRelationTable()) ?
                                    item.getConfig().getRelationTable() : item.getConfig().getTableName();
                            list.add(table);
                        }
                    }
                }
            }
        }
        return list.stream().distinct().collect(Collectors.toList());
    }


    /**
     * 递归控件(取出所有子集=-字段不变)
     *
     * @return
     */
    public static void getAllFields(List<FieLdsModel> allFields, List<FieLdsModel> fieLdsModelList) {
        for (FieLdsModel fieLdsModel : fieLdsModelList) {
            ConfigModel config = fieLdsModel.getConfig();
            String jnpfKey = config.getJnpfKey();
            if (JnpfKeyConsts.CHILD_TABLE.equals(jnpfKey)) {
                allFields.addAll(config.getChildren());
            } else {
                if (config.getChildren() != null) {
                    getAllFields(allFields, config.getChildren());
                } else {
                    if (StringUtil.isNotEmpty(fieLdsModel.getVModel())) {
                        allFields.add(fieLdsModel);
                    }
                }
            }
        }
    }
}

