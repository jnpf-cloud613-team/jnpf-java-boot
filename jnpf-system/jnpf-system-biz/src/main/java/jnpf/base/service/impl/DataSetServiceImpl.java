package jnpf.base.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.google.common.collect.ImmutableList;
import jnpf.base.ActionResult;
import jnpf.base.ActionResultCode;
import jnpf.base.UserInfo;
import jnpf.base.entity.DataInterfaceEntity;
import jnpf.base.entity.DataSetEntity;
import jnpf.base.mapper.DataSetMapper;
import jnpf.base.model.datainterface.*;
import jnpf.base.model.dataset.*;
import jnpf.base.service.DataInterfaceService;
import jnpf.base.service.DataSetService;
import jnpf.base.service.DbLinkService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.util.DataInterfaceParamUtil;
import jnpf.base.util.dataset.DataSetConfigUtil;
import jnpf.base.util.result.ResultStrategy;
import jnpf.constant.DataInterfaceVarConst;
import jnpf.constant.DbSensitiveConstant;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.database.model.dbfield.DbFieldModel;
import jnpf.database.model.dbfield.JdbcColumnModel;
import jnpf.database.model.dto.PrepSqlDTO;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.source.DbBase;
import jnpf.database.sql.util.SqlFastUtil;
import jnpf.database.util.ConnUtil;
import jnpf.database.util.DbTypeUtil;
import jnpf.database.util.DynamicDataSourceUtil;
import jnpf.database.util.JdbcUtil;
import jnpf.emnus.DsJoinTypeEnum;
import jnpf.emnus.SearchMethodEnum;
import jnpf.exception.DataException;
import jnpf.model.SystemParamModel;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.config.ConfigModel;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.entity.UserRelationEntity;
import jnpf.permission.service.OrganizeService;
import jnpf.permission.service.PositionService;
import jnpf.permission.service.UserRelationService;
import jnpf.permission.service.UserService;
import jnpf.util.*;
import jnpf.util.treeutil.SumTree;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import oracle.sql.TIMESTAMP;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.stereotype.Service;

import java.io.Reader;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * 数据集合
 *
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/5/6 14:07:11
 */
@Service
@RequiredArgsConstructor
public class DataSetServiceImpl extends SuperServiceImpl<DataSetMapper, DataSetEntity> implements DataSetService {

    public static final String STRUCT ="struct";
    
     final List<ResultStrategy> resultStrategyList;
    
    private final OrganizeService organizeApi;
    
    private final UserService userApi;
    
    private final UserRelationService userRelationApi;
    
    private final DbLinkService dbLinkService;
    
    private final DataInterfaceService dataInterfaceService;
    
    private final PositionService positionApi;

    @Override
    public List<DataSetEntity> getList(DataSetPagination pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public void create(List<DataSetForm> listSet, String objectType, String objectId) {
        this.baseMapper.create(listSet, objectType, objectId);
    }

    @FunctionalInterface
    private interface MultiConsumer<T, S, U> {
        void accept(T t, S s, U u);
    }

    @Override
    @DSTransactional
    public SumTree<TableTreeModel> getTabFieldStruct(DataSetEntity item) throws SQLException {
        //配置式
        if (Objects.equals(item.getType(), 3)) {
            return getInterfaceFields(item);
        }
        //配置式
        if (Objects.equals(item.getType(), 2)) {
            return getConfigureSQL(item);
        }
        //SQL语句
        TableTreeModel printTable = new TableTreeModel();
        if (StringUtil.isNotEmpty(ParameterUtil.checkContainsSensitive(item.getDataConfigJson(), DbSensitiveConstant.PRINT_SENSITIVE))) {
            throw new DataException(MsgCode.SYS047.get());
        }
        MultiConsumer<DataSetEntity, List<List<JdbcColumnModel>>, DbLinkEntity> consumer = (dataSetEntity, dataList, dbLinkEntity) -> {
            Set<String> tableNameSet = new HashSet<>();
            String parentId = dataSetEntity.getId();
            String headTable = dataSetEntity.getFullName();
            printTable.setId(parentId);
            printTable.setChildren(treeSetField(tableNameSet, dbLinkEntity, dataList.get(0), parentId));
            printTable.setFullName(headTable);
            printTable.setParentId(STRUCT);
        };
        sqlCommon(item, consumer, new HashMap<>(), true);
        return printTable;
    }

    public List<SumTree<TableTreeModel>> treeSetField(Set<String> tableNameSet, DbLinkEntity dbLinkEntity, List<JdbcColumnModel> dbJdbcModelList, String parentId) {
        List<SumTree<TableTreeModel>> list = new ArrayList<>();
        for (Map<String, String> mapOne : getFieldMap(dbLinkEntity, dbJdbcModelList, tableNameSet)) {
            TableTreeModel fieldModel = new TableTreeModel();
            fieldModel.setId(mapOne.get("field"));
            fieldModel.setFullName(mapOne.get("fieldName"));
            fieldModel.setLabel(mapOne.get("comment"));
            fieldModel.setParentId(parentId);
            list.add(fieldModel);
        }
        return list;
    }

    private List<Map<String, String>> getFieldMap(DbLinkEntity dbLinkEntity, List<JdbcColumnModel> dbJdbcModelList, Set<String> tableNameSet) {
        List<Map<String, String>> mapList = new ArrayList<>();
        Map<String, List<DbFieldModel>> tableFiledsMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(dbJdbcModelList)) {
            // 部分数据库，无法从元数据中查出表、字段注释，比如Oracle
            try {
                for (JdbcColumnModel jdbcColumnModel : dbJdbcModelList) {
                    if (StringUtils.isNotBlank(jdbcColumnModel.getTable()) && !tableFiledsMap.containsKey(jdbcColumnModel.getTable())) {
                        tableFiledsMap.put(jdbcColumnModel.getTable(), SqlFastUtil.getFieldList(dbLinkEntity, jdbcColumnModel.getTable()));
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        for (JdbcColumnModel model : dbJdbcModelList) {
            // 获取表名
            Map<String, String> map = new HashMap<>();
            String tableInfo = model.getTable();
            String fieldInfo = model.getField();
            String comment = "";
            List<DbFieldModel> dbFieldModels = tableFiledsMap.get(tableInfo);
            if (CollectionUtils.isNotEmpty(dbFieldModels)) {
                String columnComment = "";
                for (DbFieldModel column : dbFieldModels) {
                    if (column.getField().equalsIgnoreCase(fieldInfo)) {
                        columnComment = column.getFieldName();
                    }
                }
                fieldInfo = fieldInfo + " (" + columnComment + ")";
                comment = columnComment;
            }
            tableNameSet.add(tableInfo); // 表名
            map.put("fieldName", fieldInfo);// 表字段
            map.put("field", model.getLabel());
            map.put("comment", comment);
            mapList.add(map);
        }
        return mapList;
    }

    public void sqlCommon(DataSetEntity dataSetEntity, MultiConsumer<DataSetEntity, List<List<JdbcColumnModel>>, DbLinkEntity> consumer, Map<String, Object> params, boolean isFieldStruct) throws SQLException {
        DbLinkEntity dbLinkEntity = dbLinkService.getResource(dataSetEntity.getDbLinkId());
        //转换json
        List<Object> values = new ArrayList<>();
        String sql = replaceSql(dataSetEntity.getDataConfigJson(), dataSetEntity.getParameterJson(), params, values);

        List<List<JdbcColumnModel>> dataList;
        String addition;
        try {
            if (Boolean.TRUE.equals(DbTypeUtil.checkOracle(dbLinkEntity))) {
                addition = "SELECT major.* FROM\n" +
                        "\t(SELECT 1 from dual) temp\n" +
                        "LEFT JOIN \n" +
                        " \t({sql}) major\n" +
                        "ON \n" +
                        "\t1 = 1";
            } else {
                addition = "SELECT major.* FROM\n" +
                        "\t(SELECT 1 AS tempColumn) AS temp\n" +
                        "LEFT JOIN \n" +
                        " \t({sql}) AS major\n" +
                        "ON \n" +
                        "\t1 = 1";
            }
            boolean isFunction = StringUtil.isNotEmpty(sql) && sql.toLowerCase().startsWith("call");//判断是否存储过程
            //sqlserver获取结构方法里面不能有orderby
            if (Boolean.TRUE.equals(isFieldStruct && DbTypeUtil.checkSQLServer(dbLinkEntity)) && sql.toLowerCase().contains("order")) {
                sql = sql.substring(0, sql.toLowerCase().lastIndexOf("order"));
            }
            //isFieldStruct 获取结构时才走sql替换
            sql = isFunction || !isFieldStruct ? sql : addition.replace("{sql}", sql);
            dataList = JdbcUtil.queryJdbcColumns(new PrepSqlDTO(sql, values).withConn(dbLinkEntity)).get();
            if (dataList.isEmpty()) {
                dataList = (JdbcUtil.queryJdbcColumns(new PrepSqlDTO(sql, values).withConn(dbLinkEntity)).setIsValue(false).get());
            }
        } catch (Exception e) {
            throw new DataException(MsgCode.PRI007.get());
        }
        if (dataList.isEmpty()) {
            throw new DataException(MsgCode.PRI004.get());
        }
        consumer.accept(dataSetEntity, dataList, dbLinkEntity);
    }


    @SneakyThrows
    @Override
    @DSTransactional
    public Map<String, Object> getDataMapOrList(DataSetEntity entity, Map<String, Object> params, String formId, boolean outIsMap) {
        Map<String, Object> printDataMap = new HashMap<>();
        DataSetForm dataSetForm = JsonUtil.getJsonToBean(entity, DataSetForm.class);
        dataSetForm.setFormId(formId);
        //数据接口
        if (Objects.equals(entity.getType(), 3)) {
            try {
                dataSetForm.setNoPage(true);
                DataSetViewInfo previewDataInterface = getPreviewDataInterface(dataSetForm);
                if (outIsMap) {
                    printDataMap.put(entity.getFullName(), previewDataInterface.getPreviewData().get(0));
                } else {
                    printDataMap.put(entity.getFullName(), previewDataInterface.getPreviewData());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return printDataMap;
        }
        //配置式 获取数据
        if (Objects.equals(entity.getType(), 2)) {
            dataSetForm.setNoPage(true);
            List<Map<String, Object>> previewData = getPreviewData(dataSetForm).getPreviewData();
            if (outIsMap) {
                printDataMap.put(entity.getFullName(), previewData.get(0));
            } else {
                printDataMap.put(entity.getFullName(), previewData);
            }
            return printDataMap;
        }
        //sql语句获取数据
        try {
            MultiConsumer<DataSetEntity, List<List<JdbcColumnModel>>, DbLinkEntity> consumer = (dataSetEntity, dataList, dbLinkEntity) -> {
                List<Map<String, Object>> lists = swapData(dataList);
                if (outIsMap) {
                    printDataMap.put(dataSetEntity.getFullName(), lists.get(0));
                } else {
                    printDataMap.put(dataSetEntity.getFullName(), lists);
                }
            };
            params.put(DataInterfaceVarConst.FORM_ID, formId);
            sqlCommon(entity, consumer, params, false);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return printDataMap;
    }

    @Override
    public Map<String, Object> getDataList(DataSetQuery query) {

        Map<String, Object> dataMapOrList = new HashMap<>();
        List<FieLdsModel> queryList = StringUtil.isNotEmpty(query.getQueryList()) ? JsonUtil.getJsonToList(query.getQueryList(), FieLdsModel.class) : new ArrayList<>();
        List<DataSetEntity> dataSetList = getList(new DataSetPagination(query.getType(), query.getId()));
        Map<String, Object> queryMap = query.getMap() != null ? query.getMap() : new HashMap<>();
        Map<String, Map<String, FieLdsModel>> queryData = new HashMap<>();
        for (String key : queryMap.keySet()) {
            String vModel = key.replace("-", ".");
            String[] name = key.split("-");
            String model = name[0];
            Map<String, FieLdsModel> fieLdsModelMap = queryData.get(model) != null ? queryData.get(model) : new HashMap<>();
            FieLdsModel fieLdsModel = queryList.stream().filter(t -> Objects.equals(t.getVModel(), vModel)).findFirst().orElse(null);
            if (fieLdsModel != null) {
                fieLdsModelMap.put(key, fieLdsModel);
            }
            queryData.put(model, fieLdsModelMap);
        }
        for (DataSetEntity item : dataSetList) {
            Map<String, Object> map = new HashMap<>();
            Map<String, FieLdsModel> fieLdMap = queryData.get(item.getFullName()) != null ? queryData.get(item.getFullName()) : new HashMap<>();
            boolean isInterfaceSql = true;
            boolean ss = Objects.equals(item.getType(), 2);
            String dataJsJson = "";
            if (Objects.equals(item.getType(), 2)) {
                DataSetForm dataSetForm = JsonUtil.getJsonToBean(item, DataSetForm.class);
                dataSetForm.setFormId(query.getFormId());
                String previewSqlText = getPreviewData(dataSetForm).getPreviewSqlText();
                item.setDataConfigJson(previewSqlText);
                item.setType(1);
            }
            if (Objects.equals(item.getType(), 3)) {
                DataInterfaceEntity info = dataInterfaceService.getInfo(item.getInterfaceId());
                isInterfaceSql = info != null && Objects.equals(info.getType(), 1);
                if (info != null && Objects.equals(info.getType(), 1)) {
                    if (StringUtil.isNotEmpty(item.getParameterJson())) {
                        List<DataInterfaceModel> jsonToList = JsonUtil.getJsonToList(item.getParameterJson(), DataInterfaceModel.class);
                        Map<String, String> dataMap = new HashMap<>();
                        dataInterfaceService.paramSourceTypeReplaceValue(jsonToList, dataMap);
                        map.putAll(dataMap);
                    }
                    DataConfigJsonModel configJsonModel = JsonUtil.getJsonToBean(info.getDataConfigJson(), DataConfigJsonModel.class);
                    SqlDateModel countSqlDateModel = JsonUtil.getJsonToBean(configJsonModel.getSqlData(), SqlDateModel.class);
                    item.setDbLinkId(countSqlDateModel.getDbLinkId());
                    item.setDataConfigJson(countSqlDateModel.getSql());
                    item.setType(1);
                    dataJsJson = info.getDataJsJson();
                }
            }

            //sql语句

            if (Objects.equals(item.getType(), 1) && !fieLdMap.isEmpty()) {
                    int num = 0;
                    String sql = item.getDataConfigJson();
                    DataSetEntity queryEntity = new DataSetEntity();
                    queryEntity.setDataConfigJson("select * from (" + sql + ") t where 1=1 ");
                    queryEntity.setDbLinkId(item.getDbLinkId());
                    for (Map.Entry<String, FieLdsModel> entry : fieLdMap.entrySet()) {
                        String key = entry.getKey();
                        FieLdsModel fieLdsModel = fieLdMap.get(key);
                        if (fieLdsModel != null) {
                            String[] name = key.split("-");
                            String model = name.length > 1 ? name[1] : name[0];
                            keyJson(fieLdsModel, model, queryMap.get(key));
                            num = value(fieLdsModel, queryEntity, model, num, map);
                        }
                    }
                    item.setDataConfigJson(queryEntity.getDataConfigJson());
                }


            Map<String, Object> dataList = new HashMap<>();

            Map<String, Object> dataMapOrList1 = getDataMapOrList(item, map, null, false);

            for (Map.Entry<String, Object> entry : dataMapOrList1.entrySet()) {
                String key = entry.getKey();
                try {
                    List<Map<String, Object>> data = new ArrayList<>();
                    if (StringUtil.isNotEmpty(dataJsJson)) {
                        Object object = JScriptUtil.callJs(dataJsJson, dataMapOrList1.get(key));
                        data.addAll((List<Map<String, Object>>) object);
                    } else {
                        data.addAll((List<Map<String, Object>>) dataMapOrList1.get(key));
                    }
                    List<FieLdsModel> queryCondition = new ArrayList<>(fieLdMap.values());
                    List<Map<String, Object>> interfaceData = new ArrayList<>();
                    if (!queryCondition.isEmpty() && !isInterfaceSql) {
                        for (Map<String, Object> dataMap : data) {
                            boolean hasExtra = mapCompar(queryCondition, dataMap);
                            if (hasExtra) {
                                interfaceData.add(dataMap);
                            }
                        }
                        dataList.put(key, interfaceData);
                    } else if (ss) {
                        //处理条件
                        DataSetForm dataSetForm = JsonUtil.getJsonToBean(item, DataSetForm.class);
                        data = getFilterResult(dataSetForm, data);
                        dataList.put(key, data);
                    } else {
                        dataList.put(key, data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            dataMapOrList.putAll(dataList);
        }
        return dataMapOrList;
    }

    private List<Map<String, Object>> swapData(List<List<JdbcColumnModel>> dbJdbcModelList) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (List<JdbcColumnModel> mods : dbJdbcModelList) {
            Map<String, Object> map = new HashMap<>(16);
            for (JdbcColumnModel mod : mods) {
                Object value = mod.getValue();
                if (value != null) {
                    value = swapColumn(value);
                } else {
                    value = "";
                }
                map.put(mod.getLabel(), value);
            }
            mapList.add(map);
        }
        return mapList;
    }

    private String replaceSql(String sql, String paramJson, Map<String, Object> params, List<Object> values) {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            map.put(key, params.get(key));
        }
        // 系统内置参数替换
        Map<Double, DataInterfaceMarkModel> systemParameter = dataInterfaceService.systemParameterOne(sql, UserProvider.getUser());
        if (sql.contains(DataInterfaceVarConst.FORM_ID)) {
            DataInterfaceParamUtil.getParamModel(systemParameter, sql, DataInterfaceVarConst.FORM_ID, params.get(DataInterfaceVarConst.FORM_ID));
        }
        // 自定义参数替换
        sql = dataInterfaceService.customizationParameter(paramJson, sql, map, systemParameter);
        // 参数替换为占位符
        sql = dataInterfaceService.getHandleArraysSql(sql, values, systemParameter);
        return sql;
    }

    private Object swapColumn(Object obj) {

        if (obj instanceof Clob) {
            Clob clob = (Clob) obj;
            StringBuilder sb = new StringBuilder();
            // 获取CLOB字段的内容长度
            int length = 0;
            // 以流的形式读取CLOB字段的内容
            try (Reader reader = clob.getCharacterStream()) {
                length = (int) clob.length();
                char[] buffer = new char[length];
                int bytesRead;
                // 逐个字符读取并添加到字符串构建器中
                while ((bytesRead = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, bytesRead);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (StringUtil.isNotEmpty(sb.toString())) {
                return sb.toString();
            }
        } else if (obj instanceof LocalDateTime || obj instanceof Timestamp || obj instanceof TIMESTAMP) {
            LocalDateTime dateTime = null;
            if (obj instanceof LocalDateTime) {
                dateTime = (LocalDateTime) obj;
            } else if (obj instanceof Timestamp) {
                dateTime = ((Timestamp) obj).toLocalDateTime();
            } else {
                dateTime = LocalDateTimeUtil.of(cn.hutool.core.date.DateUtil.parse(obj.toString()));
            }
            return dateTime != null ? new Date(DateUtil.localDateTime2Millis(dateTime)) : obj;
        }
        return obj;
    }

    private void keyJson(FieLdsModel fieLdsModel, String key, Object object) {
        convertUserSelectData(fieLdsModel);
        //封装查询
        ConfigModel config = fieLdsModel.getConfig();
        Integer searchType = fieLdsModel.getSearchType();
        config.setJnpfKey(fieLdsModel.getType());
        String jnpfKey = config.getJnpfKey();
        //模糊搜索
        boolean isMultiple = fieLdsModel.getSearchMultiple() || fieLdsModel.getMultiple();
        List<String> type = JnpfKeyConsts.SelectIgnore;
        //文本框搜索
        List<String> base = JnpfKeyConsts.BaseSelect;
        if (isMultiple || type.contains(jnpfKey)) {
            if (object instanceof String) {
                object = ImmutableList.of(object);
            }
            searchType = 4;
        }
        if (base.contains(jnpfKey) && searchType == 3) {
            searchType = 2;
        }
        String string = searchType == 2 ? SearchMethodEnum.LIKE.getSymbol() : SearchMethodEnum.INCLUDED.getSymbol();
        String s = searchType == 3 ? SearchMethodEnum.BETWEEN.getSymbol() : string;
        String symbol = searchType == 1 ? SearchMethodEnum.EQUAL.getSymbol() : s;
        fieLdsModel.setSymbol(symbol);
        if (object instanceof List) {
            fieLdsModel.setFieldValue(JsonUtil.getObjectToString(object));
        } else {
            fieLdsModel.setFieldValue(String.valueOf(object));
        }

        //封装数据
        List<String> dateControl = JnpfKeyConsts.DateSelect;
        dateControl.add("date");
        List<String> numControl = JnpfKeyConsts.NumSelect;
        List<String> dataList = new ArrayList<>();
        String value = fieLdsModel.getFieldValue();
        Object fieldValue = fieLdsModel.getFieldValue();
        Object fieldValueTwo = fieLdsModel.getFieldValue();
        if (fieLdsModel.getFieldValue() == null) {
            fieldValue = "";
        }
        List<String> controlList = new ArrayList<>();
        controlList.addAll(numControl);
        controlList.addAll(dateControl);
        controlList.add(JnpfKeyConsts.TIME);
        controlList.add("time");
        //处理数据
        if (controlList.contains(jnpfKey) && StringUtil.isNotEmpty(value)
                && !SearchMethodEnum.LIKE.getSymbol().equals(fieLdsModel.getSymbol())) {
            int num = 0;
            List<String> data = new ArrayList<>();
            try {
                data.addAll(JsonUtil.getJsonToList(value, String.class));
            } catch (Exception e) {
                data.add(value);
                data.add(value);
            }
            String valueOne = data.get(0);
            String valueTwo = data.get(1);
            //数字
            if (numControl.contains(jnpfKey)) {
                fieldValue = valueOne != null ? new BigDecimal(valueOne) : valueOne;
                fieldValueTwo = valueTwo != null ? new BigDecimal(valueTwo) : valueTwo;
                num++;
            }
            //日期
            if (dateControl.contains(jnpfKey)) {
                fieldValue = new Date();
                fieldValueTwo = new Date();
                if (ObjectUtil.isNotEmpty(valueOne)) {
                    fieldValue = new Date(Long.parseLong(valueOne));
                }
                if (ObjectUtil.isNotEmpty(valueTwo)) {
                    fieldValueTwo = new Date(Long.parseLong(valueTwo));
                }
                num++;
            }
            if (num == 0) {
                fieldValue = valueOne;
                fieldValueTwo = valueTwo;
            }
        }
        try {
            List<List<String>> list = JsonUtil.getJsonToBean(value, List.class);
            Set<String> dataAll = new HashSet<>();
            for (List<String> strings : list) {
                List<String> list1 = new ArrayList<>(strings);
                dataAll.add(JSON.toJSONString(list1));
            }
            dataList = new ArrayList<>(dataAll);
        } catch (Exception e) {
            try {
                Set<String> dataAll = new HashSet<>();
                List<String> list = JsonUtil.getJsonToList(value, String.class);
                List<String> mast = new ArrayList<>() ;
                mast.add(JnpfKeyConsts.CASCADER);
                mast.add(JnpfKeyConsts.ADDRESS);
                if (mast.contains(jnpfKey)) {
                    dataAll.add(JSON.toJSONString(list));
                } else {
                    dataAll.addAll(list);
                }
                dataList.addAll(new ArrayList<>(dataAll));
            } catch (Exception e1) {
                dataList.add(value);
            }
        }

        switch (jnpfKey) {
            case JnpfKeyConsts.POSSELECT:
                //包含子岗位
                if (Objects.equals(fieLdsModel.getSelectRange(), "2")) {
                    List<PositionEntity> childList = positionApi.getListByParentIds(dataList);
                    dataList.addAll(childList.stream().map(PositionEntity::getId).collect(Collectors.toList()));
                    //包含子孙岗位
                } else if (Objects.equals(fieLdsModel.getSelectRange(), "3")) {
                    List<PositionEntity> childList = positionApi.getProgeny(dataList, 1);
                    dataList.addAll(childList.stream().map(PositionEntity::getId).collect(Collectors.toList()));
                }
                break;
            case JnpfKeyConsts.COMSELECT:
                //包含子组织
                if (Objects.equals(fieLdsModel.getSelectRange(), "2")) {
                    List<OrganizeEntity> childList = organizeApi.getListByParentIds(dataList);
                    dataList.addAll(childList.stream().map(OrganizeEntity::getId).collect(Collectors.toList()));
                    //包含子孙组织
                } else if (Objects.equals(fieLdsModel.getSelectRange(), "3")) {
                    List<OrganizeEntity> childList = organizeApi.getProgeny(dataList, 1);
                    dataList.addAll(childList.stream().map(OrganizeEntity::getId).collect(Collectors.toList()));
                }
                break;
            case JnpfKeyConsts.USERSELECT:
                //包含当前用户及下属
                if (CollectionUtils.isNotEmpty(dataList)) {
                    List<String> posIds = userRelationApi.getListByUserIdAll(dataList).stream()
                            .filter(t -> PermissionConst.POSITION.equals(t.getObjectType()))
                            .map(UserRelationEntity::getObjectId).collect(Collectors.toList());
                    if (Objects.equals(fieLdsModel.getSelectRange(), "2")) {
                        List<UserEntity> childList = userRelationApi.getUserAndSub(posIds, null);
                        dataList.addAll(childList.stream().map(UserEntity::getId).collect(Collectors.toList()));
                        //包含子孙用户
                    } else if (Objects.equals(fieLdsModel.getSelectRange(), "3")) {
                        List<UserEntity> childList = userRelationApi.getUserProgeny(posIds, null);
                        dataList.addAll(childList.stream().map(UserEntity::getId).collect(Collectors.toList()));
                    }
                }
                break;
            default:
                break;
        }
        if (dataList.isEmpty()) {
            dataList.add("jnpfNullList");
        }
        fieLdsModel.setVModel(key);
        fieLdsModel.setFieldValueOne(fieldValue);
        fieLdsModel.setFieldValueTwo(fieldValueTwo);
        fieLdsModel.setDataList(dataList);
    }

    private void convertUserSelectData(FieLdsModel fieLdsModel) {
        List<String> symbolList = ImmutableList.of(SearchMethodEnum.EQUAL.getSymbol(), SearchMethodEnum.NOT_EQUAL.getSymbol());
        String jnpfKey = fieLdsModel.getConfig().getJnpfKey();
        String fieldValue = fieLdsModel.getFieldValue();
        String symbol = fieLdsModel.getSymbol();
        if (StringUtil.isNotEmpty(fieldValue) && JnpfKeyConsts.CUSTOMUSERSELECT.equals(jnpfKey) && !symbolList.contains(symbol)) {
                List<String> values = new ArrayList<>();
                try {
                    values = JsonUtil.getJsonToList(fieldValue, String.class);
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
                List<String> dataValues = new ArrayList<>(values);
                for (String userVal : values) {
                    String userValue = userVal.substring(0, userVal.indexOf("--"));
                    UserEntity userEntity = userApi.getInfo(userValue);
                    if (userEntity != null) {
                        dataValues.add(userValue);
                        //在用户关系表中取出
                        List<UserRelationEntity> groupRel = Optional.ofNullable(userRelationApi.getListByUserId(userValue, PermissionConst.GROUP)).orElse(new ArrayList<>());
                        List<UserRelationEntity> posRel = Optional.ofNullable(userRelationApi.getListByUserId(userValue, PermissionConst.POSITION)).orElse(new ArrayList<>());
                        List<UserRelationEntity> roleRel = Optional.ofNullable(userRelationApi.getListByUserId(userValue, PermissionConst.ROLE)).orElse(new ArrayList<>());
                        if (!groupRel.isEmpty()) {
                            for (UserRelationEntity split : groupRel) {
                                dataValues.add(split.getObjectId());
                            }
                        }
                        if (StringUtil.isNotEmpty(userEntity.getOrganizeId())) {
                            //向上递归 查出所有上级组织
                            List<String> allUpOrgIDs = new ArrayList<>();
                            organizeApi.upWardRecursion(allUpOrgIDs, userEntity.getOrganizeId());
                            dataValues.addAll(allUpOrgIDs);
                        }
                        if (!posRel.isEmpty()) {
                            for (UserRelationEntity split : posRel) {
                                dataValues.add(split.getObjectId());
                            }
                        }
                        if (!roleRel.isEmpty()) {
                            for (UserRelationEntity split : roleRel) {
                                dataValues.add(split.getObjectId());
                            }
                        }
                    }
                }
                fieLdsModel.setFieldValue(JsonUtil.getObjectToString(dataValues));
            }

    }

    private Integer value(FieLdsModel fieLdsModel, DataSetEntity queryEntity, String key, int num, Map<String, Object> map) {
        try {
            DbLinkEntity linkEntity = dbLinkService.getResource(queryEntity.getDbLinkId());
            @Cleanup Connection connection = ConnUtil.getConnOrDefault(linkEntity);
            String dbType = connection.getMetaData().getDatabaseProductName().trim();
            boolean isSqlServer = "Microsoft SQL Server".equalsIgnoreCase(dbType);
            SearchMethodEnum symbol = SearchMethodEnum.getSearchMethod(fieLdsModel.getSymbol());
            StringBuilder querySql = new StringBuilder(queryEntity.getDataConfigJson());
            String sql = "{" + key + num + "} ";
            Object fieldValue = fieLdsModel.getFieldValueOne();
            Object fieldValueTwo = fieLdsModel.getFieldValueTwo();
            switch (symbol) {
                case IS_NULL:
                    querySql.append("AND ").append(key).append(" IS NULL ");
                    break;
                case IS_NOT_NULL:
                    querySql.append("AND ").append(key).append(" IS NOT NULL ");
                    break;
                case EQUAL:
                    querySql.append("AND ").append(key).append(" = ").append(sql);
                    map.put(key + num, fieldValue);
                    num++;
                    break;
                case NOT_EQUAL:
                    querySql.append("AND ").append(key).append(" <> ").append(sql);
                    map.put(key + num, fieldValue);
                    num++;
                    break;
                case GREATER_THAN:
                    querySql.append("AND ").append(key).append(" > ").append(sql);
                    map.put(key + num, fieldValue);
                    num++;
                    break;
                case LESS_THAN:
                    querySql.append("AND ").append(key).append(" < ").append(sql);
                    map.put(key + num, fieldValue);
                    num++;
                    break;
                case GREATER_THAN_OR_EQUAL:
                    querySql.append("AND ").append(key).append(" >= ").append(sql);
                    map.put(key + num, fieldValue);
                    num++;
                    break;
                case LESS_THAN_OR_EQUAL:
                    querySql.append("AND ").append(key).append(" <= ").append(sql);
                    map.put(key + num, fieldValue);
                    num++;
                    break;
                case LIKE:
                    querySql.append("AND ").append(key).append(" LIKE ").append(sql);
                    if (isSqlServer) {
                        fieldValue = String.valueOf(fieldValue).replace("\\[", "[[]");
                    }
                    map.put(key + num, "%" + fieldValue + "%");
                    num++;
                    break;
                case NOT_LIKE:
                    querySql.append("AND ").append(key).append(" NOT LIKE ").append(sql);
                    if (isSqlServer) {
                        fieldValue = String.valueOf(fieldValue).replace("\\[", "[[]");
                    }
                    map.put(key + num, "%" + fieldValue + "%");
                    num++;
                    break;
                case BETWEEN:
                    querySql.append("AND ").append(key).append(" BETWEEN ").append(sql);
                    map.put(key + num, fieldValue);
                    num++;
                    String two = "{" + key + num + "} ";
                    querySql.append(" AND ").append(two);
                    map.put(key + num, fieldValueTwo);
                    num++;
                    break;
                case INCLUDED:
                case NOT_INCLUDED:
                    List<String> dataList = fieLdsModel.getDataList();
                    querySql.append("AND  ( ");
                    for (int i = 0; i < dataList.size(); i++) {
                        String value = dataList.get(i);
                        if (isSqlServer) {
                            value = String.valueOf(value).replace("\\[", "[[]");
                        }
                        boolean isLast = i == dataList.size() - 1;
                        if (symbol == SearchMethodEnum.INCLUDED) {
                            querySql.append(key).append(" LIKE {").append(key).append(num).append("} ");
                            querySql.append(isLast ? "" : " OR ");
                            map.put(key + num, "%" + value + "%");
                        } else {
                            querySql.append(key).append(" NOT LIKE {").append(key).append(num).append("} ");
                            querySql.append(isLast ? "" : " AND ");
                            map.put(key + num, "%" + value + "%");
                        }
                        num++;
                    }
                    querySql.append(" ) ");
                    break;
                default:
                    break;
            }
            queryEntity.setDataConfigJson(querySql.toString());
        } catch (Exception e) {
            e.getMessage();
        }
        return num;
    }

    /**
     * 获取配置式字段
     *
     * @param item
     * @return
     */
    private TableTreeModel getConfigureSQL(DataSetEntity item) {
        TableTreeModel printTable = new TableTreeModel();
        String parentId = item.getId();
        String headTable = item.getFullName();

        String dbType = null;
        try {
            DbLinkEntity linkEntity = dbLinkService.getResource(item.getDbLinkId());
            @Cleanup Connection connection = ConnUtil.getConnOrDefault(linkEntity);
            dbType = DbTypeUtil.getDbEncodeByProductName(connection.getMetaData().getDatabaseProductName().trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<DsConfigModel> dsConfigModels = DataSetConfigUtil.analyzeJson(item.getVisualConfigJson(), dbType);

        List<DsConfigFields> listDF = new ArrayList<>();
        DataSetConfigUtil.getAllFields(dsConfigModels, listDF);

        List<SumTree<TableTreeModel>> list = new ArrayList<>();
        for (DsConfigFields configFields : listDF) {
            TableTreeModel fieldModel = new TableTreeModel();
            String fieldAlias = configFields.getFieldAlias();
            String filed;
            String fieldName;
            if (StringUtil.isNotEmpty(fieldAlias)) {
                filed = fieldAlias;
                fieldName = configFields.getFieldName().replace(configFields.getField(), fieldAlias);
            } else {
                filed = configFields.getField();
                fieldName = configFields.getFieldName();
            }
            fieldModel.setId(filed);
            fieldModel.setFullName(fieldName);
            fieldModel.setLabel(fieldName);
            list.add(fieldModel);
        }
        printTable.setId(parentId);
        printTable.setChildren(list);
        printTable.setFullName(headTable);
        printTable.setParentId(STRUCT);
        return printTable;
    }

    @Override
    public DataSetViewInfo getPreviewData(DataSetForm dataSetForm) {
        DataSetViewInfo dataSetViewInfo = new DataSetViewInfo();
        Map<String, String> systemParam = userApi.getSystemFieldValue(new SystemParamModel());
        //系统参数添加当前表单id
        systemParam.put(DataInterfaceVarConst.FORM_ID, ObjectUtil.isNotEmpty(dataSetForm.getFormId()) ? dataSetForm.getFormId() : DataInterfaceVarConst.FORM_ID);
        try {
            DbLinkEntity linkEntity = dbLinkService.getResource(dataSetForm.getDbLinkId());
            DynamicDataSourceUtil.switchToDataSource(linkEntity);

            @Cleanup Connection connection = ConnUtil.getConnOrDefault(linkEntity);
            String dbType = DbTypeUtil.getDbEncodeByProductName(connection.getMetaData().getDatabaseProductName().trim());
            List<Object> values = new ArrayList<>();
            List<DsConfigModel> dsConfigModels = DataSetConfigUtil.analyzeJson(dataSetForm.getVisualConfigJson(), dbType);

            //字段配置
            List<DsConfigFields> listDF = new ArrayList<>();
            DataSetConfigUtil.getAllFields(dsConfigModels, listDF);
            List<Map<String, String>> previewColumns = new ArrayList<>();
            for (DsConfigFields configFields : listDF) {
                Map<String, String> map = new HashMap<>();
                String fieldAlias = configFields.getFieldAlias();
                String filed;
                String fieldName;
                if (StringUtil.isNotEmpty(fieldAlias)) {
                    filed = fieldAlias;
                    fieldName = configFields.getFieldName().replace(configFields.getField(), fieldAlias);
                } else {
                    filed = configFields.getField();
                    fieldName = configFields.getFieldName();
                }
                map.put("title", filed);
                map.put("label", fieldName);
                previewColumns.add(map);
            }

            String sql = DataSetConfigUtil.assembleSql(dsConfigModels,
                    DsParamModel.builder().dbType(dbType).values(values).systemParam(systemParam).filterConfigJson(dataSetForm.getFilterConfigJson()).build());

            if (DbBase.MYSQL.equals(dbType) && sql.contains(DsJoinTypeEnum.FULL_JOIN.getCode())) {
                throw new DataException(MsgCode.SYS129.get());
            }

            Object[] valueArr = new Object[values.size()];
            for (int i = 0; i < values.size(); i++) {
                if (values.get(i) instanceof String) {
                    valueArr[i] = "'" + values.get(i) + "'";
                } else {
                    valueArr[i] = values.get(i);
                }
            }
            String replace = sql.replace("?", "%s");
            String sqlValue = String.format(replace, valueArr);
            String formatSql = SqlFormatter.format(sqlValue);


            String objectToString = JsonUtil.getObjectToStringAsDate(JdbcUtil.queryList(new PrepSqlDTO(sql, values).withConn(linkEntity, null)).setIsAlias(true).get());
            List<Map<String, Object>> data = JsonUtil.getJsonToListMap(objectToString);
            //结果集筛选
            data = getFilterResult(dataSetForm, data);
            List<Map<String, Object>> collect ;
            if (data.size() > 20) {
                collect = data.stream().limit(20).collect(Collectors.toList());
            } else {
                collect = new ArrayList<>(data);
            }
            dataSetViewInfo.setPreviewSqlText(formatSql);
            dataSetViewInfo.setPreviewData(collect);
            dataSetViewInfo.setPreviewColumns(previewColumns);
        } catch (DataException dataE) {
            throw new DataException(dataE.getMessage());
        } catch (SQLSyntaxErrorException | PersistenceException sqlE) {
            throw new DataException(MsgCode.PRI007.get());
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            DynamicDataSourceUtil.clearSwitchDataSource();
        }
        return dataSetViewInfo;
    }

    /**
     * 结果集筛选
     *
     * @param dataSetForm 数据配置
     * @param data        数据
     * @return 返回最终数据
     */
    private List<Map<String, Object>> getFilterResult(DataSetForm dataSetForm, List<Map<String, Object>> data) {
        if (StringUtil.isNotEmpty(dataSetForm.getFilterConfigJson())) {
            String filterConfigJson = dataSetForm.getFilterConfigJson();
            DataFormModel jsonToBean = JsonUtil.getJsonToBean(filterConfigJson, DataFormModel.class);
            if (jsonToBean != null && jsonToBean.getResultFilter() != null) {
                data = resultStrategyList.stream()
                        .filter(it -> it.getChoice().equals(jsonToBean.getResultFilter().toString()))
                        .collect(Collectors.toList())
                        .get(0)
                        .getResults(data, jsonToBean);

            }
        }
        return data;
    }

    /**
     * 获取系统参数
     *
     * @return
     */
    public Map<String, Object> getSystemParam(String str, String formId) {
        Map<String, Object> map = new HashMap<>();
        UserInfo userInfo = UserProvider.getUser();

        map.put(DataInterfaceVarConst.FORM_ID, ObjectUtil.isNotEmpty(formId) ? formId : DataInterfaceVarConst.FORM_ID);

        //当前用户
        String userId = userInfo.getUserId();
        map.put(DataInterfaceVarConst.USER, userId);

        //当前用户及下属
        List<String> subOrganizeIds = new ArrayList<>();
        if (null != userInfo.getSubordinateIds() && !userInfo.getSubordinateIds().isEmpty()) {
            subOrganizeIds = userInfo.getSubordinateIds();
        }
        subOrganizeIds.add(userInfo.getUserId());
        map.put(DataInterfaceVarConst.USERANDSUB, subOrganizeIds);

        //当前组织
        String orgId = userInfo.getOrganizeId();
        if (StringUtil.isNotEmpty(userInfo.getDepartmentId())) {
            orgId = userInfo.getDepartmentId();
        }
        map.put(DataInterfaceVarConst.ORG, orgId);

        //当前组织及子组织
        if (str.contains(DataInterfaceVarConst.ORGANDSUB)) {
            List<String> underOrganizations = organizeApi.getUnderOrganizations(orgId, false);
            underOrganizations.add(orgId);
            map.put(DataInterfaceVarConst.ORGANDSUB, underOrganizations);
        }
        return map;
    }

    /**
     * 数据接口获取字段列表
     *
     * @param item
     * @return
     */
    private TableTreeModel getInterfaceFields(DataSetEntity item) {
        TableTreeModel printTable = new TableTreeModel();
        String parentId = item.getId();
        String headTable = item.getFullName();

        DataInterfaceEntity info = dataInterfaceService.getInfo(item.getInterfaceId());
        if (info == null) {
            throw new DataException(MsgCode.FM001.get());
        }
        String fieldJson = info.getFieldJson();

        if (StringUtil.isEmpty(fieldJson) || "[]".equals(fieldJson)) {
            throw new DataException(MsgCode.SYS133.get());
        }

        List<FieldModel> fieldList = JsonUtil.getJsonToList(fieldJson, FieldModel.class);
        List<SumTree<TableTreeModel>> list = new ArrayList<>();
        for (FieldModel configFields : fieldList) {
            TableTreeModel fieldModel = new TableTreeModel();
            fieldModel.setId(configFields.getDefaultValue());
            fieldModel.setFullName(configFields.getDefaultValue() + "(" + configFields.getField() + ")");
            fieldModel.setLabel(configFields.getField());
            list.add(fieldModel);
        }
        printTable.setId(parentId);
        printTable.setChildren(list);
        printTable.setFullName(headTable);
        printTable.setParentId(STRUCT);
        return printTable;
    }

    @Override
    public DataSetViewInfo getPreviewDataInterface(DataSetForm dataSetForm) {
        DataSetViewInfo dataSetViewInfo = new DataSetViewInfo();
        DataInterfaceEntity info = dataInterfaceService.getInfo(dataSetForm.getInterfaceId());
        if (info == null) {
            throw new DataException(MsgCode.FM001.get());
        }
        String fieldJson = info.getFieldJson();

        if (StringUtil.isEmpty(fieldJson) || "[]".equals(fieldJson)) {
            throw new DataException(MsgCode.SYS133.get());
        }
        List<FieldModel> fieldList = JsonUtil.getJsonToList(fieldJson, FieldModel.class);
        List<Map<String, String>> previewColumns = new ArrayList<>();
        for (FieldModel configFields : fieldList) {
            Map<String, String> map = new HashMap<>();
            map.put("title", configFields.getDefaultValue());
            map.put("label", configFields.getField());
            previewColumns.add(map);
        }
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            HashMap<String, String> paramMap = new HashMap<>(16);
            if (StringUtil.isNotEmpty(dataSetForm.getParameterJson())) {
                List<DataInterfaceModel> jsonToList = JsonUtil.getJsonToList(dataSetForm.getParameterJson(), DataInterfaceModel.class);
                jsonToList.stream().forEach(t -> {
                    if (Objects.equals(t.getSourceType(), 4) && DataInterfaceVarConst.FORM_ID.equals(t.getRelationField())) {
                        t.setDefaultValue(dataSetForm.getFormId());
                    }
                });
                dataInterfaceService.paramSourceTypeReplaceValue(jsonToList, paramMap);
            }
            ActionResult<Object> actionResult = dataInterfaceService.infoToId(dataSetForm.getInterfaceId(), null, paramMap);
            if (ActionResultCode.SUCCESS.getCode().equals(actionResult.getCode())) {
                data = (List<Map<String, Object>>) actionResult.getData();
            }
            if (!dataSetForm.isNoPage() && data.size() > 20) {
                    data = new ArrayList<>(data.subList(0, 20));
                }

        } catch (Exception e) {
            e.printStackTrace();
            throw new DataException(MsgCode.FA037.get());
        }
        dataSetViewInfo.setPreviewData(data);
        dataSetViewInfo.setPreviewColumns(previewColumns);
        return dataSetViewInfo;
    }

    public static boolean mapCompar(List<FieLdsModel> searchList, Map<String, Object> hashMap2) {
        boolean isChange = false;
        for (FieLdsModel item : searchList) {
            String realValue = hashMap2.get(item.getVModel()) == null ? "" : (String) hashMap2.get(item.getVModel());
            switch (item.getSearchType()) {
                case 2:
                    if (realValue.contains(item.getFieldValue())) {
                        isChange = true;
                    }
                    break;
                case 3://between
                    List<String> longList = new ArrayList<>();
                    longList.add(JnpfKeyConsts.NUM_INPUT);
                    longList.add(JnpfKeyConsts.DATE);
                    longList.add(JnpfKeyConsts.DATE_CALCULATE);
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
                        isChange = value.contains(realValue);
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


}
