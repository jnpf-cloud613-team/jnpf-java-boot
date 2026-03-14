package jnpf.base.util.dataset;

import jnpf.base.model.dataset.*;
import jnpf.constant.DsKeyConst;
import jnpf.database.model.query.SuperQueryJsonModel;
import jnpf.database.source.DbBase;
import jnpf.database.util.DbTypeUtil;
import jnpf.emnus.DsJoinTypeEnum;
import jnpf.emnus.SearchMethodEnum;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.util.DateUtil;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

/**
 * 配置式工具类
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/9/9 17:34:11
 */
public class DataSetConfigUtil {

    DataSetConfigUtil(){

    }
    //字段别名
    public static final String FIELD_RENAME = "%s.%s AS %s ";
    //普通查询
    public static final String SELECT_FROM = "SELECT %s FROM %s";
    //最基础的查询别名
    public static final String SELECT_UNIT = "(SELECT %s FROM %s) %s";
    //基础的上级查询
    public static final String SELECT_UPPER = "(%s) %s";
    public static final String SELECT_BRACKET = "(%s)";

    public static final String SIGN_EMPTY = "";
    public static final String SIGN_BLANK = " ";
    public static final String SIGN_COMMA = ",";
    public static final String SIGN_EQUALS = " = ";
    public static final String SIGN_ON = " ON ";
    public static final String SIGN_AND = " AND ";
    public static final String SIGN_WHERE = " WHERE ";
    public static final String SIGN_OR = " OR ";
    public static final String SIGN_UNDERLINE = "_";
    public static final String SIGN_BAR = "-";
    public static final String SIGN_PER = "%";
    public static final String SIGN_BETWEEN = " BETWEEN ";
    public static final String SIGN_NULL = " IS NULL ";
    public static final String SIGN_NOT_NULL = " IS NOT NULL ";
    public static final String SIGN_NEQ = " <> ";
    public static final String SIGN_GREATER = " > ";
    public static final String SIGN_LESS = " < ";
    public static final String SIGN_GREATER_EQ = " >= ";
    public static final String SIGN_LESS_EQ = " <= ";
    public static final String SIGN_LIKE = " LIKE ";
    public static final String SIGN_NOT_LIKE = " NOT LIKE ";
    public static final String SIGN_JNPFNULLLIST = "jnpfNullList";
    public static final String SIGN_QUESTION = "?";
    public static final String SIGN_TAMESTAMP = "TO_TIMESTAMP(?,'yyyy-mm-dd hh24:mi:ss')";


    /**
     * 递归获取全部字段
     *
     * @param configList
     * @param list
     */
    public static void getAllFields(List<DsConfigModel> configList, List<DsConfigFields> list) {
        if (configList == null || CollectionUtils.isEmpty(configList)) return;
        for (DsConfigModel item : configList) {
            if (item.getFieldList() != null && !item.getFieldList().isEmpty()) {
                list.addAll(item.getFieldList());
            }
            getAllFields(item.getChildren(), list);
        }
    }

    /**
     * 单表查询字段别名
     *
     * @param list
     * @return
     */
    public static String getTableFieldsAlias(List<DsConfigFields> list, boolean onlyField) {
        if (list == null || CollectionUtils.isEmpty(list)) return SIGN_EMPTY;
        StringJoiner result = new StringJoiner(SIGN_COMMA);
        for (DsConfigFields item : list) {
            String tableAlias = item.getTable();
            String fieldAlias = item.getFieldAlias();
            String field = item.getField();
            if (StringUtil.isNotEmpty(fieldAlias)) {
                if (onlyField) {
                    //只有别名字段
                    result.add(fieldAlias);
                } else {
                    //表名.字段 AS 别名
                    result.add(String.format(FIELD_RENAME, tableAlias, field, fieldAlias));
                }
            } else {
                //只有原字段
                result.add(field);
            }
        }
        return result.toString();
    }

    /**
     * 解析json
     *
     * @param configListJson
     * @return
     */
    public static List<DsConfigModel> analyzeJson(String configListJson, String dbType) {
        List<DsConfigModel> configList = JsonUtil.getJsonToList(configListJson, DsConfigModel.class);
        if (CollectionUtils.isEmpty(configList)) {
            return new ArrayList<>();
        }
        //单个表不别名
        if (CollectionUtils.isEmpty(configList.get(0).getChildren())) {
            return configList;
        }

        int startIndex = 0;

        //别名小写
        if (DbTypeUtil.needToLowerCase(dbType)) {
            startIndex = 32;
        }
        analyzeDsConfigModel(configList, startIndex);
        return configList;
    }

    /**
     * 递归解析json
     *
     * @param configList
     * @param letterNum
     */
    private static void analyzeDsConfigModel(List<DsConfigModel> configList, int letterNum) {
        if (CollectionUtils.isNotEmpty(configList)) {
            for (DsConfigModel configModel : configList) {
                String tableLetter = getLetter(letterNum) + SIGN_UNDERLINE;
                String tableAlias = tableLetter + configModel.getTable();
                configModel.setTableAlias(tableAlias);
                //子表配置
                List<DsConfigModel> children = configModel.getChildren();
                if (CollectionUtils.isNotEmpty(children)) {
                    letterNum++;
                    analyzeDsConfigModel(children, letterNum);
                }
                //字段别名设置
                List<DsConfigFields> fieldList = configModel.getFieldList();
                if (CollectionUtils.isNotEmpty(fieldList)) {
                    for (DsConfigFields field : fieldList) {
                        field.setFieldAlias(tableLetter + field.getField());
                        field.setTable(configModel.getTable());
                    }
                }
                letterNum++;
            }
        }
    }

    /**
     * 组装sql
     *
     * @param configList
     * @return
     */
    public static String assembleSql(List<DsConfigModel> configList, DsParamModel dsParamModel) {
        String sql = "";
        if (CollectionUtils.isEmpty(configList)) {
            return null;
        }
        List<DsConfigFields> listDF = new ArrayList<>();
        DataSetConfigUtil.getAllFields(configList, listDF);

        String fieldsAlias = getTableFieldsAlias(listDF, true);

        DsConfigModel configModel = configList.get(0);

        StringBuilder result = new StringBuilder();
        //递归组装
        if (CollectionUtils.isNotEmpty(configModel.getChildren())) {
            StringBuilder childStr = new StringBuilder();
            DataSetConfigUtil.recurAssSql(configModel, childStr, dsParamModel);
            result.append(childStr);
        } else {
            String where = DataSetConfigUtil.recurAssWhere(configModel, dsParamModel, false);
            String tableAndWhere = "";
            if (!where.equals("()")) {
                tableAndWhere = configModel.getTable() + (StringUtil.isNotEmpty(where) ? SIGN_WHERE + where : "");
                result.append(String.format(SELECT_FROM, fieldsAlias, tableAndWhere));
            } else {
                result.append(String.format(SELECT_FROM, fieldsAlias, configModel.getTable()));
            }

        }
        sql = result.toString();
        //组装最外层过滤
        if (StringUtil.isNotEmpty(dsParamModel.getFilterConfigJson())) {
            DsConfigModel relationModel = JsonUtil.getJsonToBean(dsParamModel.getFilterConfigJson(), DsConfigModel.class);
            if (CollectionUtils.isNotEmpty(relationModel.getRuleList())) {
                StringBuilder filterResult = new StringBuilder();
                String filterTable = StringUtil.isNotEmpty(configModel.getTableAlias()) ? configModel.getTableAlias() : configModel.getTable();
                filterResult.append(String.format(SELECT_FROM, fieldsAlias, String.format(SELECT_UPPER, sql, filterTable)));
                relationModel.setTable(configModel.getTable());
                relationModel.setFieldList(configModel.getFieldList());
                relationModel.setChildren(configModel.getChildren());
                String recurAssWhere = recurAssWhere(relationModel, dsParamModel, true);
                if (StringUtil.isNotEmpty(recurAssWhere)) {
                    filterResult.append(SIGN_WHERE);
                    filterResult.append(recurAssWhere);
                }
                sql = filterResult.toString();
            }
        }
        return sql;
    }

    /**
     * 递归组装
     *
     * @param configModel
     * @param result
     * @return
     */
    private static void recurAssSql(DsConfigModel configModel, StringBuilder result, DsParamModel dsParamModel) {
        List<DsConfigModel> children = configModel.getChildren();
        //递归获取全部字段
        List<DsConfigFields> listDF = new ArrayList<>();
        ArrayList<DsConfigModel> dsConfigModels = new ArrayList<>();
        dsConfigModels.add(configModel);
        DataSetConfigUtil.getAllFields(dsConfigModels, listDF);
        String tableAlias = configModel.getTableAlias();
        String table = configModel.getTable();

        List<DsConfigFields> fieldList = configModel.getFieldList();
        String mainFields = DataSetConfigUtil.getTableFieldsAlias(fieldList, false);

        //当前查询条件
        String where = DataSetConfigUtil.recurAssWhere(configModel, dsParamModel, false);
        String tableAndWhere = table + (StringUtil.isNotEmpty(where) ? SIGN_WHERE + where : "");
        //查询当前表语句 - 有子表往里塞，没有直接抛出
        String tableSelect = String.format(SELECT_UNIT, mainFields, tableAndWhere, tableAlias);
        if (CollectionUtils.isNotEmpty(children)) {
            //查询结果集
            String allFields = DataSetConfigUtil.getTableFieldsAlias(listDF, true);
            StringBuilder childBuilder = new StringBuilder();

            //查询主表字段
            childBuilder.append(tableSelect);
            //查询连表
            for (DsConfigModel item : children) {
                DsRelationConfig relationConfig = item.getRelationConfig();
                List<DsConfigFields> fieldItem = item.getFieldList();
                String codeByType = DsJoinTypeEnum.getCodeByType(relationConfig.getType());
                childBuilder.append(SIGN_BLANK);
                childBuilder.append(codeByType);
                StringBuilder itemBuilder = new StringBuilder();
                DataSetConfigUtil.recurAssSql(item, itemBuilder, dsParamModel);
                if (CollectionUtils.isNotEmpty(item.getChildren())) {
                    childBuilder.append(String.format(SELECT_UPPER, itemBuilder, item.getTableAlias()));
                } else {
                    childBuilder.append(itemBuilder);
                }
                //关联关系拼接-必须有关联关系
                List<DsRelationModel> relationList = relationConfig.getRelationList();
                childBuilder.append(SIGN_ON);
                StringJoiner relationJoiner = new StringJoiner(SIGN_AND);
                for (DsRelationModel relationModel : relationList) {
                    DsConfigFields pFields = fieldList.stream().filter(t -> t.getField().equals(relationModel.getPField())).findFirst().orElse(null);
                    DsConfigFields cFields = fieldItem.stream().filter(t -> t.getField().equals(relationModel.getField())).findFirst().orElse(null);
                    if (null!=cFields&&null!=pFields){
                        relationJoiner.add(pFields.getFieldAlias() + SIGN_EQUALS + cFields.getFieldAlias());
                    }

                }
                childBuilder.append(relationJoiner);
            }
            //拼接所有表的关联关系
            StringJoiner relationWhereJoiner = new StringJoiner(SIGN_AND);
            for (DsConfigModel item : children) {
                DsRelationConfig relationConfig = item.getRelationConfig();
                //连表条件拼接
                DsConfigModel relationModel = new DsConfigModel();
                relationModel.setTable(table);
                relationModel.setFieldList(fieldList);
                relationModel.setChildren(children);
                relationModel.setMatchLogic(relationConfig.getMatchLogic());
                relationModel.setRuleList(relationConfig.getRuleList());
                String relationWhere = DataSetConfigUtil.recurAssWhere(relationModel, dsParamModel, true);
                if (StringUtil.isNotEmpty(relationWhere)) {
                    relationWhereJoiner.add(relationWhere);
                }
            }
            if (relationWhereJoiner.length() > 0) {
                childBuilder.append(SIGN_WHERE);
                childBuilder.append(relationWhereJoiner);
            }
            result.append(String.format(SELECT_FROM, allFields, childBuilder));
        } else {
            result.append(tableSelect);
        }

    }

    /**
     * 组装where条件
     *
     * @param configModel
     * @param dsParamModel
     * @param fieldAlias
     * @return
     */
    public static String recurAssWhere(DsConfigModel configModel, DsParamModel dsParamModel, boolean fieldAlias) {
        List<DsConfigFields> fieldList = configModel.getFieldList();
        Map<String, List<DsConfigFields>> fieldsMap = new HashMap<>();
        fieldsMap.put(configModel.getTable(), fieldList);
        List<DsConfigModel> children = configModel.getChildren();
        for (DsConfigModel child : children) {
            fieldsMap.put(child.getTable(), child.getFieldList());
        }
        String matchLogic = SearchMethodEnum.AND.getSymbol().equals(configModel.getMatchLogic()) ? SIGN_AND : SIGN_OR;
        List<SuperQueryJsonModel> ruleList = configModel.getRuleList();
        StringJoiner matchJoiner = new StringJoiner(matchLogic);
        int m = 0;
        if (CollectionUtils.isNotEmpty(ruleList)) {
            for (SuperQueryJsonModel ruleModel : ruleList) {
                String logic = SearchMethodEnum.AND.getSymbol().equals(ruleModel.getLogic()) ? SIGN_AND : SIGN_OR;
                StringJoiner logicJoiner = new StringJoiner(logic);
                List<FieLdsModel> groups = ruleModel.getGroups();
                int n = 0;
                for (FieLdsModel fieLdsModel : groups) {
                    DsConfigFields configFields = null;
                    String thisField;
                    if (fieLdsModel.getField().contains(SIGN_BAR)) {
                        String thisTable = fieLdsModel.getField().split(SIGN_BAR)[0];
                        thisField = fieLdsModel.getField().split(SIGN_BAR)[1];
                        List<DsConfigFields> dsConfigFields = fieldsMap.get(thisTable);
                        if (CollectionUtils.isNotEmpty(dsConfigFields)) {
                            configFields = dsConfigFields.stream().filter(t -> t.getField().equals(thisField)).findFirst().orElse(null);
                        }
                    } else {
                        thisField = fieLdsModel.getField();
                        configFields = fieldList.stream().filter(t -> t.getField().equals(thisField)).findFirst().orElse(null);
                    }
                    if (configFields != null && StringUtil.isNotEmpty(configFields.getFieldAlias())) {
                        fieLdsModel.setFieldAlias(configFields.getFieldAlias());
                    } else {
                        fieLdsModel.setFieldAlias(thisField);
                    }
                    String condition = getCondition(fieLdsModel, dsParamModel, fieldAlias);
                    logicJoiner.add(condition);
                    n++;
                }
                if (logicJoiner.length() > 0) {
                    String logicSql = logicJoiner.toString();
                    if (n > 1) {
                        logicSql = String.format(SELECT_BRACKET, logicSql);
                    }
                    matchJoiner.add(logicSql);
                    m++;
                }

            }
        }
        if (matchJoiner.length() > 0) {
            String matchSql = matchJoiner.toString();
            if (m > 1) {
                matchSql = String.format(SELECT_BRACKET, matchSql);
            }
            return matchSql;
        }
        return "";
    }

    /**
     * 拼接条件
     *
     * @param fieLdsModel
     * @param dsParamModel
     * @param fieldAlias
     * @return
     */
    public static String getCondition(FieLdsModel fieLdsModel, DsParamModel dsParamModel, boolean fieldAlias) {
        List<Object> values = dsParamModel.getValues();
        String dbType = dsParamModel.getDbType();
        boolean isOracleOrPostgre = DbBase.ORACLE.equalsIgnoreCase(dbType) || DbBase.POSTGRE_SQL.equalsIgnoreCase(dbType);
        boolean isSqlServer = DbBase.SQL_SERVER.equalsIgnoreCase(dbType);
        String field = fieldAlias ? fieLdsModel.getFieldAlias() : fieLdsModel.getField();
        String dataType = fieLdsModel.getDataType();
        SearchMethodEnum symbol = SearchMethodEnum.getSearchMethod(fieLdsModel.getSymbol());

        String mark = SIGN_QUESTION;
        if (DsKeyConst.getDateSelect().contains(dataType) && isOracleOrPostgre) {
            mark = SIGN_TAMESTAMP;
        }
        //获取条件值
        swapValue(fieLdsModel, dsParamModel);
        Object fieldValueOne = fieLdsModel.getFieldValueOne();
        Object fieldValueTwo = fieLdsModel.getFieldValueTwo();
        if (isSqlServer && fieldValueOne instanceof String) {
            fieldValueOne = String.valueOf(fieldValueOne).replace("\\[", "[[]");
        }

        StringBuilder sqlBuilder = new StringBuilder();
        switch (symbol) {
            case IS_NULL:
                sqlBuilder.append(field).append(SIGN_NULL);
                break;
            case IS_NOT_NULL:
                sqlBuilder.append(field).append(SIGN_NOT_NULL);
                break;
            case EQUAL:
                sqlBuilder.append(field).append(SIGN_EQUALS).append(mark);
                values.add(fieldValueOne);
                break;
            case NOT_EQUAL:
                sqlBuilder.append(field).append(SIGN_NEQ).append(mark);
                values.add(fieldValueOne);
                break;
            case GREATER_THAN:
                sqlBuilder.append(field).append(SIGN_GREATER).append(mark);
                values.add(fieldValueOne);
                break;
            case LESS_THAN:
                sqlBuilder.append(field).append(SIGN_LESS).append(mark);
                values.add(fieldValueOne);
                break;
            case GREATER_THAN_OR_EQUAL:
                sqlBuilder.append(field).append(SIGN_GREATER_EQ).append(mark);
                values.add(fieldValueOne);
                break;
            case LESS_THAN_OR_EQUAL:
                sqlBuilder.append(field).append(SIGN_LESS_EQ).append(mark);
                values.add(fieldValueOne);
                break;
            case LIKE:
                sqlBuilder.append(field).append(SIGN_LIKE).append(mark);
                values.add(SIGN_PER + fieldValueOne + SIGN_PER);
                break;
            case NOT_LIKE:
                sqlBuilder.append(field).append(SIGN_NOT_LIKE).append(mark);
                values.add(SIGN_PER + fieldValueOne + SIGN_PER);
                break;
            case BETWEEN:
                sqlBuilder.append(field).append(SIGN_BETWEEN).append(mark).append(SIGN_AND).append(mark);
                values.add(fieldValueOne);
                values.add(fieldValueTwo);
                break;
            case INCLUDED:
            case NOT_INCLUDED:
                List<String> dataList = new ArrayList<>();
                if (fieldValueOne != null){
                    if (fieldValueOne instanceof List) {
                        dataList.addAll((List) fieldValueOne);
                    } else {
                        try {
                            dataList.addAll(JsonUtil.getJsonToList(fieldValueOne.toString(), String.class));
                        } catch (Exception e) {
                            dataList.add(fieldValueOne.toString());
                        }
                    }
                }

                if (dataList.isEmpty()) {
                    dataList.add(SIGN_JNPFNULLLIST);
                }
                StringJoiner included = new StringJoiner(SIGN_OR);
                StringJoiner notIncluded = new StringJoiner(SIGN_AND);
                for (String s : dataList) {
                    if (symbol == SearchMethodEnum.INCLUDED) {
                        included.add(field + SIGN_LIKE + mark);
                        values.add(SIGN_PER + s + SIGN_PER);
                    } else {
                        notIncluded.add(field + SIGN_NOT_LIKE + mark);
                        values.add(SIGN_PER + s + SIGN_PER);
                    }
                }
                if (SearchMethodEnum.INCLUDED.equals(symbol)) {
                    sqlBuilder.append(String.format(SELECT_BRACKET, included));
                } else {
                    sqlBuilder.append(String.format(SELECT_BRACKET, notIncluded));
                }

                break;
            default:
                break;
        }
        return sqlBuilder.toString();
    }

    /**
     * 值转换
     *
     * @param fieLdsModel
     * @param dsParamModel
     */
    public static void swapValue(FieLdsModel fieLdsModel, DsParamModel dsParamModel) {
        Map<String, String> systemParam = dsParamModel.getSystemParam();
        String fieldValue = fieLdsModel.getFieldValue();
        String dataType = fieLdsModel.getDataType();

        String valueOne = null;
        String valueTwo = null;
        Object fieldValueOne = fieldValue;
        Object fieldValueTwo = fieldValue;
        String fieldValueType = fieLdsModel.getFieldValueType();
        List<String> data = new ArrayList<>();
        if (DsKeyConst.getBetweenSelect().contains(dataType)) {
            try {
                data.addAll(JsonUtil.getJsonToList(fieldValue, String.class));
                if (data.isEmpty()) return;
                if (data.size() == 1) data.add(data.get(0));
            } catch (Exception e) {
                data.add(fieldValue);
                data.add(fieldValue);
            }
            valueOne = data.get(0);
            valueTwo = data.get(1);
        }
        switch (dataType) {
            case DsKeyConst.DOUBLE:
                fieldValueOne = valueOne != null ? Double.valueOf(valueOne) : valueOne;
                fieldValueTwo = valueTwo != null ? Double.valueOf(valueTwo) : valueTwo;
                break;
            case DsKeyConst.BIGINT:
                fieldValueOne = valueOne != null ? Long.valueOf(valueOne) : valueOne;
                fieldValueTwo = valueTwo != null ? Long.valueOf(valueTwo) : valueTwo;
                break;
            case DsKeyConst.DATE:
            case DsKeyConst.TIME:
                if (valueOne != null) {
                    fieldValueOne = DateUtil.dateFormat(new Date(Long.valueOf(valueOne)));
                }

                if (valueTwo != null) {
                    fieldValueTwo = DateUtil.dateFormat(new Date(Long.valueOf(valueTwo)));
                }
                break;
            default:
                //TEXT 1-自定义，2-系统参数
                if (Objects.equals(fieldValueType, "2")) {
                    fieldValueOne = systemParam.get(fieldValue);
                }
                break;
        }
        fieLdsModel.setFieldValueOne(fieldValueOne);
        fieLdsModel.setFieldValueTwo(fieldValueTwo);
    }

    /**
     * asc码获取字母
     *
     * @param letterNum
     * @return
     */
    public static String getLetter(int letterNum) {
        char letter = (char) (65 + letterNum);
        return String.valueOf(letter);
    }
}
