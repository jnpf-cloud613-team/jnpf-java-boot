package jnpf.base.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableList;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.dbtable.vo.DbFieldVO;
import jnpf.base.model.form.VisualTableModel;
import jnpf.base.util.common.GenerateCommon;
import jnpf.constant.GenerateConstant;
import jnpf.constant.KeyConst;
import jnpf.constant.MsgCode;
import jnpf.database.constant.DbFieldConst;
import jnpf.database.datatype.viewshow.constant.DtViewConst;
import jnpf.database.model.dbfield.DbFieldModel;
import jnpf.database.model.dbtable.DbTableFieldModel;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.source.DbBase;
import jnpf.exception.DataException;
import jnpf.exception.WorkFlowException;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.TableFields;
import jnpf.model.visualjson.TableModel;
import jnpf.model.visualjson.analysis.FormAllModel;
import jnpf.model.visualjson.analysis.FormColumnTableModel;
import jnpf.model.visualjson.analysis.FormEnum;
import jnpf.util.*;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/3/25 9:30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisualDevTableCre {

    private final ServiceBaseUtil serviceUtil;

    private static final String JNPF_TABLE = "_jnpfTable_";

    /**
     * 表单赋值tableName
     *
     * @param jsonArray
     * @param tableModels
     */
    private void fieldsTableName(JSONArray jsonArray, List<TableModel> tableModels, Map<String, String> tableNameList) {
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            String jnpfkey = jsonObject.getJSONObject(GenerateCommon.CONFIG).getString(KeyConst.JNPF_KEY);
            List<String> childrenListAll = ImmutableList.of(
                    FormEnum.CARD.getMessage(),
                    FormEnum.ROW.getMessage(),
                    FormEnum.TAB.getMessage(),
                    FormEnum.COLLAPSE.getMessage(),
                    FormEnum.COLLAPSE_ITEM.getMessage(),
                    FormEnum.TAB_ITEM.getMessage(),
                    FormEnum.TABLE_GRID.getMessage(),
                    FormEnum.TABLE_GRID_TR.getMessage(),
                    FormEnum.TABLE_GRID_TD.getMessage(),
                    FormEnum.STEPS.getMessage(),
                    FormEnum.STEP_ITEM.getMessage());
            if (childrenListAll.contains(jnpfkey) || StringUtil.isEmpty(jnpfkey)) {
                JSONArray childArray = jsonObject.getJSONObject(GenerateCommon.CONFIG).getJSONArray(KeyConst.CHILDREN);
                this.fieldsTableName(childArray, tableModels, tableNameList);
                jsonObject.getJSONObject(GenerateCommon.CONFIG).put(KeyConst.CHILDREN, childArray);
            } else if (FormEnum.TABLE.getMessage().equals(jnpfkey)) {
                JSONArray childrenList = new JSONArray();
                JSONArray children = jsonObject.getJSONObject(GenerateCommon.CONFIG).getJSONArray(KeyConst.CHILDREN);
                String tableModel = tableNameList.get(jsonObject.getString("__vModel__"));
                for (int k = 0; k < children.size(); k++) {
                    JSONObject childrenObject = (JSONObject) children.get(k);
                    this.fieldsModel(childrenObject, tableModels);
                    if (StringUtil.isEmpty(tableModel)) {
                        tableModel = childrenObject.getJSONObject(GenerateCommon.CONFIG).getString(KeyConst.RELATION_TABLE);
                    }
                    childrenList.add(childrenObject);
                }
                jsonObject.getJSONObject(GenerateCommon.CONFIG).put(KeyConst.TABLE_NAME_HUMP, tableModel);
                jsonObject.getJSONObject(GenerateCommon.CONFIG).put(KeyConst.CHILDREN, childrenList);
            } else {
                this.fieldsModel(jsonObject, tableModels);
            }
        }
    }

    /**
     * 赋值table
     *
     * @param jsonObject
     * @param tableModels
     */
    private TableModel fieldsModel(JSONObject jsonObject, List<TableModel> tableModels) {
        String vModel = jsonObject.getString("__vModel__");
        String relationField = StringUtil.isNotEmpty(jsonObject.getString(KeyConst.RELATION_FIELD)) ? jsonObject.getString(KeyConst.RELATION_FIELD) : "";
        String jnpfkey = jsonObject.getJSONObject(GenerateCommon.CONFIG).getString(KeyConst.JNPF_KEY);
        TableModel tableName = tableModels.stream().filter(t -> "1".equals(t.getTypeId())).findFirst().orElse(null);
        if (tableName == null) {
            throw new DataException("主表不存在");
        }
        jsonObject.getJSONObject(GenerateCommon.CONFIG).put(KeyConst.TABLE_NAME_HUMP, tableName.getTable());
        List<TableModel> childTableAll = tableModels.stream().filter(t -> "0".equals(t.getTypeId())).collect(Collectors.toList());
        TableModel childTableaa = childTableAll.stream().filter(t -> t.getFields().stream().filter(k -> k.getField().equals(vModel)).count() > 0).findFirst().orElse(null);
        if (childTableaa != null) {
            jsonObject.getJSONObject(GenerateCommon.CONFIG).put(KeyConst.RELATION_TABLE, childTableaa.getTable());
        }
        if ((FormEnum.RELATION_FORM_ATTR.getMessage().equals(jnpfkey) || FormEnum.POPUP_ATTR.getMessage().equals(jnpfkey))
                && StringUtil.isNotEmpty(relationField)) {
            boolean isSubTable = jsonObject.getJSONObject(GenerateCommon.CONFIG).getBooleanValue("isSubTable");
            String model = relationField.split(JNPF_TABLE)[0];
            jsonObject.put(KeyConst.RELATION_FIELD, model + JNPF_TABLE + tableName.getTable() + (isSubTable ? "0" : "1"));
        }
        if (JnpfKeyConsts.BILLRULE.equals(jnpfkey) && StringUtil.isNotEmpty(relationField)) {
            boolean isSubTable = jsonObject.getJSONObject(GenerateCommon.CONFIG).getBooleanValue("isSubTable");
            String model = relationField.split(JNPF_TABLE)[0];
            jsonObject.put(KeyConst.RELATION_FIELD, model + JNPF_TABLE + tableName.getTable() + (isSubTable ? "0" : "1"));
        }
        return childTableaa;
    }

    /**
     * 验证表名合规性
     *
     * @param tableName
     * @param dbLinkId
     * @return
     * @throws Exception
     */
    public void checkName(String tableName, String dbLinkId) {
        if (!RegexUtils.checkName(tableName)) {
            throw new DataException(MsgCode.VS030.get());
        }
        if (tableName.length() > 30) {
            throw new DataException(MsgCode.VS031.get());
        }
        boolean existTable;
        try {
            existTable = serviceUtil.isExistTable(dbLinkId, tableName);
        } catch (Exception e) {
            throw new DataException(MsgCode.LOG110.get());
        }
        if (existTable) {
            throw new DataException(MsgCode.VS032.get());
        }
        if (GenerateConstant.containKeyword(tableName)) {
            throw new DataException(MsgCode.SYS128.get(tableName));
        }
    }

    /**
     * 创建表
     *
     * @return
     */
    public List<TableModel> tableList(VisualTableModel visualTableModel) throws WorkFlowException {
        JSONArray jsonArray = visualTableModel.getJsonArray();
        List<FormAllModel> formAllModel = visualTableModel.getFormAllModel();
        String table = visualTableModel.getTable();
        String linkId = visualTableModel.getLinkId();
        String fullName = visualTableModel.getFullName();
        int primaryKey = visualTableModel.getPrimaryKey();
        List<TableModel> tableModelList = new LinkedList<>();
        Map<String, String> tableNameList = new HashMap<>();
        DbLinkEntity dbLink = serviceUtil.getDbLink(linkId);
        String type = dbLink.getDbType();
        boolean isUpperCase = (DbBase.DM.equals(type) || DbBase.ORACLE.equals(type));
        boolean isLowerCase = (DbBase.POSTGRE_SQL.equals(type) || DbBase.KINGBASE_ES.equals(type));
        table = tableName(table, isUpperCase, isLowerCase);

        String relationField = tableName(TableFeildsEnum.FID.getField(), isUpperCase, isLowerCase);
        String tableField = tableName(TableFeildsEnum.FOREIGN.getField(), isUpperCase, isLowerCase);
        List<String> checkNameList = new ArrayList<>();
        checkNameList.add(table);
        try {
            List<DbFieldModel> fieldList = new ArrayList<>();
            Map<String, List<DbFieldModel>> tableListAll = new HashMap<>();
            for (FormAllModel model : formAllModel) {
                if (FormEnum.MAST.getMessage().equals(model.getJnpfKey())) {
                    FieLdsModel fieLdsModel = model.getFormColumnModel().getFieLdsModel();
                    this.fieldList(fieLdsModel, table, fieldList);
                } else if (FormEnum.TABLE.getMessage().equals(model.getJnpfKey())) {
                    String tableName = "ct" + RandomUtil.uuId();
                    tableName = tableName(tableName, isUpperCase, isLowerCase);

                    FormColumnTableModel fieLdsModel = model.getChildList();
                    if (StringUtil.isNotEmpty(fieLdsModel.getTableName())) {
                        tableName = fieLdsModel.getTableName();
                        checkName(tableName, linkId);
                        if (checkNameList.contains(tableName)) {
                            throw new DataException(MsgCode.VS032.get());
                        } else {
                            checkNameList.add(tableName);
                        }
                    }
                    List<DbFieldModel> tableList = new ArrayList<>();
                    String tableModel = fieLdsModel.getTableModel();
                    List<FieLdsModel> fieldsList = fieLdsModel.getChildList().stream().map(t -> t.getFieLdsModel()).collect(Collectors.toList());
                    for (FieLdsModel tableFieLdsModel : fieldsList) {
                        this.fieldList(tableFieLdsModel, tableName, tableList);
                    }
                    this.dbTableField(tableList, true, primaryKey, isLowerCase, tableField, relationField);
                    tableNameList.put(tableModel, tableName);
                    tableListAll.put(tableModel, tableList);
                }
            }

            this.dbTableField(fieldList, false, primaryKey, isLowerCase, tableField, relationField);
            fieldList.add(ConcurrencyUtils.getDbFieldModel(TableFeildsEnum.TENANTID, isUpperCase, isLowerCase));
            fieldList.add(ConcurrencyUtils.getDbFieldModel(TableFeildsEnum.FLOWID, isUpperCase, isLowerCase));
            fieldList.add(ConcurrencyUtils.getDbFieldModel(TableFeildsEnum.VERSION, isUpperCase, isLowerCase));
            fieldList.add(ConcurrencyUtils.getDbFieldModel(TableFeildsEnum.FLOWTASKID, isUpperCase, isLowerCase));
            fieldList.add(ConcurrencyUtils.getDbFieldModel(TableFeildsEnum.FLOWSTATE, isUpperCase, isLowerCase));
            if (Boolean.TRUE.equals(visualTableModel.getLogicalDelete())) {//删除标志字段
                fieldList.add(ConcurrencyUtils.getDbFieldModel(TableFeildsEnum.DELETEMARK, isUpperCase, isLowerCase));
                fieldList.add(ConcurrencyUtils.getDbFieldModel(TableFeildsEnum.DELETETIME, isUpperCase, isLowerCase));
                fieldList.add(ConcurrencyUtils.getDbFieldModel(TableFeildsEnum.DELETEUSERID, isUpperCase, isLowerCase));
            }
            List<DbTableFieldModel> dbTableList = new ArrayList<>();
            //创建子表
            for (Map.Entry<String, List<DbFieldModel>> keyItem : tableListAll.entrySet()) {
                String key = keyItem.getKey();
                List<DbFieldModel> datableList = keyItem.getValue();
                String tableName = tableName(tableNameList.get(key), isUpperCase, isLowerCase);
                datableList.add(ConcurrencyUtils.getDbFieldModel(TableFeildsEnum.TENANTID, isUpperCase, isLowerCase));
                if (Boolean.TRUE.equals(visualTableModel.getLogicalDelete())) {//删除标志字段
                    datableList.add(ConcurrencyUtils.getDbFieldModel(TableFeildsEnum.DELETEMARK, isUpperCase, isLowerCase));
                    datableList.add(ConcurrencyUtils.getDbFieldModel(TableFeildsEnum.DELETETIME, isUpperCase, isLowerCase));
                    datableList.add(ConcurrencyUtils.getDbFieldModel(TableFeildsEnum.DELETEUSERID, isUpperCase, isLowerCase));
                }
                DbTableFieldModel dbTable = this.dbTable(linkId, tableName, datableList, true, fullName);
                dbTableList.add(dbTable);
                this.tableModel(tableModelList, datableList, tableName, table, true, tableField, relationField);
            }
            this.tableModel(tableModelList, fieldList, table, table, false, tableField, relationField);
            DbTableFieldModel dbTable = this.dbTable(linkId, table, fieldList, false, fullName);
            dbTableList.add(dbTable);
            serviceUtil.createTable(dbTableList);
            this.fieldsTableName(jsonArray, tableModelList, tableNameList);
            visualTableModel.setTableNameList(tableNameList);
        } catch (DataException e) {
            e.printStackTrace();
            log.error("表新增错误:{}", e.getMessage());
            throw new DataException(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("表新增错误:{}", e.getMessage());
            throw new WorkFlowException(MsgCode.FM013.get(e.getMessage()), e);
        }
        return tableModelList;
    }

    /**
     * 表名
     *
     * @param tableName
     * @param isUpperCase
     * @param isLowerCase
     * @return
     */
    private String tableName(String tableName, boolean isUpperCase, boolean isLowerCase) {
        String resultName;
        if (isUpperCase) {
            resultName = tableName.toUpperCase();
        } else {
            resultName = isLowerCase ? tableName.toLowerCase() : tableName;
        }
        return resultName;
    }

    /**
     * 获取表单字段
     *
     * @param fieLdsModel
     * @param tableList
     */
    private void fieldList(FieLdsModel fieLdsModel, String table, List<DbFieldModel> tableList) {
        String vmodel = fieLdsModel.getVModel();
        String lable = fieLdsModel.getConfig().getLabel();
        String jnpfkey = fieLdsModel.getConfig().getJnpfKey();
        fieLdsModel.getConfig().setTableName(table);
        if (StringUtil.isNotEmpty(vmodel)) {
            DbFieldModel fieldForm = new DbFieldModel();
            fieldForm.setNullSign(DbFieldConst.NULL);
            fieldForm.setDataType(DtViewConst.VARCHAR);
            fieldForm.setLength("255");
            fieldForm.setIsPrimaryKey(false);
            if (StringUtil.isNotEmpty(fieLdsModel.getVModel())) {
                if (JnpfKeyConsts.getTextField().contains(jnpfkey)) {
                    fieldForm.setDataType(DtViewConst.TEXT);
                }
                List<String> date = Arrays.asList(JnpfKeyConsts.MODIFYTIME, JnpfKeyConsts.CREATETIME, JnpfKeyConsts.DATE, JnpfKeyConsts.DATE_CALCULATE);
                if (date.contains(jnpfkey)) {
                    fieldForm.setDataType(DtViewConst.DATE_TIME);
                }
                if (JnpfKeyConsts.NUM_INPUT.equals(jnpfkey) || JnpfKeyConsts.CALCULATE.equals(jnpfkey) || JnpfKeyConsts.SLIDER.equals(jnpfkey)) {
                    fieldForm.setDataType(DtViewConst.DECIMAL);
                    String precision = "15";
                    if (fieLdsModel.getPrecision() != null) {
                        precision = String.valueOf(fieLdsModel.getPrecision());
                    }
                    fieldForm.setLength("38," + precision);
                    //mysql 最大长度65，Oracle和postgresql最大长度38，精度0-最大长度内取值，当前系统默认给15最大
                }

                if (JnpfKeyConsts.RATE.equals(jnpfkey)) {
                    fieldForm.setDataType(DtViewConst.DECIMAL);
                    fieldForm.setLength("38,1");
                }

                if (JnpfKeyConsts.LOCATION.equals(jnpfkey)) {
                    fieldForm.setLength("500");
                }
                fieldForm.setField(vmodel);
                fieldForm.setComment(lable);
                tableList.add(fieldForm);
            }
        }
    }

    /**
     * 创建主外键字段
     *
     * @param tableList
     * @param isforeign
     */
    private void dbTableField(List<DbFieldModel> tableList, boolean isforeign, int primaryKey, boolean isPostgreOrKingbase, String tableField, String relationField) {
        //是否自增 true自增
        boolean autoPrimaryKey = primaryKey == 2;
        String dataType;
        if (autoPrimaryKey) {
            dataType = isPostgreOrKingbase ? DtViewConst.INT : DtViewConst.BIGINT;
        } else {
            dataType = DtViewConst.VARCHAR;
        }
        DbFieldModel tableKey = new DbFieldModel();
        tableKey.setNullSign(DbFieldConst.NOT_NULL);
        tableKey.setDataType(dataType);
        tableKey.setLength(TableFeildsEnum.FID.getLength());
        tableKey.setIsPrimaryKey(true);
        tableKey.setField(relationField);
        tableKey.setIsAutoIncrement(autoPrimaryKey);
        tableKey.setComment(TableFeildsEnum.FID.getComment());
        tableList.add(tableKey);
        if (isforeign) {
            DbFieldModel tableForeignKey = new DbFieldModel();
            tableForeignKey.setNullSign(DbFieldConst.NULL);
            tableForeignKey.setDataType(dataType);
            tableForeignKey.setLength(TableFeildsEnum.FOREIGN.getLength());
            tableForeignKey.setIsPrimaryKey(false);
            tableForeignKey.setField(tableField);
            tableForeignKey.setComment(TableFeildsEnum.FOREIGN.getComment());
            tableList.add(tableForeignKey);
        }
    }

    /**
     * 组装字段list
     *
     * @param tableModelList
     * @param dbtable
     * @param table
     * @param mastTable
     * @param isforeign
     */
    private void tableModel(List<TableModel> tableModelList, List<DbFieldModel> dbtable, String table, String mastTable, boolean isforeign, String tableField, String relationField) {
        TableModel tableModel = new TableModel();
        tableModel.setRelationField(isforeign ? relationField : "");
        tableModel.setRelationTable(isforeign ? mastTable : "");
        tableModel.setTable(table);
        tableModel.setComment(isforeign ? "子表" : "主表");
        tableModel.setTableField(isforeign ? tableField : "");
        tableModel.setTypeId(isforeign ? "0" : "1");
        List<DbFieldVO> voList = dbtable.stream().map(DbFieldVO::new).collect(Collectors.toList());
        tableModel.setFields(JsonUtil.getJsonToList(voList, TableFields.class));
        tableModelList.add(tableModel);
    }

    /**
     * 组装创表字段
     *
     * @param linkId
     * @param tableName
     * @param tableFieldList
     * @param isforeign
     * @return
     */
    private DbTableFieldModel dbTable(String linkId, String tableName, List<DbFieldModel> tableFieldList, boolean isforeign, String fullName) {
        DbTableFieldModel dbTable = new DbTableFieldModel();
        dbTable.setDbLinkId(linkId);
        dbTable.setTable(tableName);
        dbTable.setDbFieldModelList(tableFieldList);
        String s = isforeign ? "子表" : "主表";
        if (fullName.contains("&")) {//自动生成表备注的时候带&符号创建不成功问题
            fullName = fullName.replace("&", " ");
        }
        dbTable.setComment(String.format("%s-%s", fullName, s));
        return dbTable;
    }

    /**
     * 替换列表字段表名
     *
     * @param entity
     * @param tableModels
     * @param modelTableName
     */
    public static void setFieldTable(VisualdevEntity entity, List<TableModel> tableModels, Map<String, String> modelTableName) {
        if (StringUtil.isEmpty(entity.getColumnData())) {
            return;
        }
        Map<String, Object> columnData = JsonUtil.stringToMap(entity.getColumnData());
        if (columnData.get(KeyConst.DEFAULT_COLUMN_LIST) == null) {
            return;
        }
        JSONArray columnDataArr = JsonUtil.getJsonToJsonArray(String.valueOf(columnData.get(KeyConst.DEFAULT_COLUMN_LIST)));
        for (Object o : columnDataArr) {
            JSONObject jsonObject = (JSONObject) o;
            String fieldId = jsonObject.getString("id");
            JSONObject config = jsonObject.getJSONObject(GenerateCommon.CONFIG);
            TableModel mainTableModel = tableModels.stream().filter(t -> "1".equals(t.getTypeId())).findFirst().orElse(null);
            if (mainTableModel == null) {
                throw new DataException("主表不存在");
            }
            String tableName = mainTableModel.getTable();
            String relationTable = "";
            if (fieldId.toLowerCase().startsWith(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                String tableModelName = fieldId.split("-")[0];
                relationTable = modelTableName.get(tableModelName);
            }
            if (StringUtil.isNotEmpty(tableName)) {
                config.put(KeyConst.TABLE_NAME_HUMP, tableName);
            }
            if (StringUtil.isNotEmpty(relationTable)) {
                config.put(KeyConst.RELATION_TABLE, relationTable);
            }
        }
        columnData.put(KeyConst.DEFAULT_COLUMN_LIST, columnDataArr);
        entity.setColumnData(JsonUtil.getObjectToString(columnData));
        //app
        if (StringUtil.isNotEmpty(entity.getAppColumnData())) {
            Map<String, Object> appColumnData = JsonUtil.stringToMap(entity.getAppColumnData());
            appColumnData.put(KeyConst.DEFAULT_COLUMN_LIST, columnDataArr);
            entity.setAppColumnData(JsonUtil.getObjectToString(appColumnData));
        }
    }
}
