package jnpf.base.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import jnpf.base.UserInfo;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.mapper.FlowFormDataMapper;
import jnpf.base.model.ColumnDataModel;
import jnpf.base.model.VisualConst;
import jnpf.base.model.VisualDevJsonModel;
import jnpf.base.model.flow.DataModel;
import jnpf.base.model.form.ModuleFormModel;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.MsgCode;
import jnpf.database.model.dbtable.JdbcTableModel;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.util.ConnUtil;
import jnpf.database.util.DbTypeUtil;
import jnpf.database.util.DynamicDataSourceUtil;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.exception.DataException;
import jnpf.exception.WorkFlowException;
import jnpf.model.visualjson.*;
import jnpf.model.visualjson.analysis.*;
import jnpf.onlinedev.model.OnlineInfoModel;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permissions.PermissionInterfaceImpl;
import jnpf.util.*;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.mybatis.dynamic.sql.AndOrCriteriaGroup;
import org.mybatis.dynamic.sql.BasicColumn;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.SqlTable;
import org.mybatis.dynamic.sql.delete.render.DeleteStatementProvider;
import org.mybatis.dynamic.sql.insert.GeneralInsertDSL;
import org.mybatis.dynamic.sql.insert.render.GeneralInsertStatementProvider;
import org.mybatis.dynamic.sql.render.RenderingStrategies;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.select.SelectModel;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.mybatis.dynamic.sql.update.UpdateModel;
import org.mybatis.dynamic.sql.update.render.UpdateStatementProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlowFormDataUtil {

    private final ConfigValueUtil configValueUtil;
    private final ServiceBaseUtil serviceUtil;
    private final FlowFormDataMapper flowFormDataMapper;
    private final FormInfoUtils formInfoUtils;
    private final VisualBillUtil visualBillUtil;

    /**
     * 从数据库取表主键
     *
     * @param conn
     * @param tableName 表名
     * @return
     */
    public String getTableKey(Connection conn, String tableName) throws SQLException {
        String pKeyName = JdbcTableModel.getPrimaryExculde(conn, tableName, configValueUtil.getMultiTenantColumn());
        if (StringUtil.isEmpty(pKeyName)) {
            pKeyName = TableFeildsEnum.FID.getField();
        }
        String databaseProductName = conn.getMetaData().getDatabaseProductName().trim();
        if (DbTypeUtil.needToUpperCase(databaseProductName)) {
            pKeyName = pKeyName.toUpperCase();
        }
        return pKeyName;
    }

    /**
     * 从配置取主键
     *
     * @param tableModel
     * @return String
     */
    public String getKey(TableModel tableModel, String databaseProductName) {
        boolean toUpperCase = false;
        if (StringUtil.isNotEmpty(databaseProductName) && DbTypeUtil.needToUpperCase(databaseProductName)) {
            toUpperCase = !toUpperCase;
        }
        String pKeyName = toUpperCase ? TableFeildsEnum.FID.getField().toUpperCase() : TableFeildsEnum.FID.getField();
        if (tableModel != null && tableModel.getFields() != null) {
            TableFields tableFields = tableModel.getFields().stream().filter(t -> Objects.equals(t.getPrimaryKey(), 1)
                    && !t.getField().toLowerCase().contains(TableFeildsEnum.TENANTID.getField())).findFirst().orElse(null);
            pKeyName = Objects.nonNull(tableFields) ? tableFields.getField() : pKeyName;
        }
        return pKeyName;
    }

    /**
     * 获取流程任务id原字段 flowTaskId
     *
     * @param tableModel
     * @param databaseProductName
     * @return
     */
    public String getFlowTaskId(TableModel tableModel, String databaseProductName) {
        boolean toUpperCase = false;
        if (StringUtil.isNotEmpty(databaseProductName) && DbTypeUtil.needToUpperCase(databaseProductName)) {
            toUpperCase = !toUpperCase;
        }
        String pKeyName = toUpperCase ? TableFeildsEnum.FLOWTASKID.getField().toUpperCase() : TableFeildsEnum.FLOWTASKID.getField();
        if (tableModel != null && tableModel.getFields() != null) {
            TableFields tableFields = tableModel.getFields().stream().filter(t ->
                    t.getField().toLowerCase().contains(TableFeildsEnum.FLOWTASKID.getField())).findFirst().orElse(null);
            pKeyName = Objects.nonNull(tableFields) ? tableFields.getField() : pKeyName;
        }
        return pKeyName;
    }

    //---------------------------------------------信息---------------------------------------------

    /**
     * 获取编辑页数据
     *
     * @param visualdevEntity
     * @param id
     * @return
     */
    public Map<String, Object> getEditDataInfo(VisualdevEntity visualdevEntity, String id, OnlineInfoModel infoModel) {
        Map<String, Object> allDataMap = new HashMap<>();

        FormDataModel formData = JsonUtil.getJsonToBean(visualdevEntity.getFormData(), FormDataModel.class);
        boolean logicalDelete = formData.getLogicalDelete();

        //权限参数
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(visualdevEntity.getColumnData(), ColumnDataModel.class);
        boolean needP = false;
        List<String> formPerList = new ArrayList<>();
        if (columnDataModel != null && StringUtil.isNotEmpty(infoModel.getMenuId())) {
            needP = columnDataModel.getUseFormPermission();
            Map<String, Object> pMap = PermissionInterfaceImpl.getFormMap();
            if (pMap.get(infoModel.getMenuId()) != null) {
                formPerList = JsonUtil.getJsonToList(pMap.get(infoModel.getMenuId()), ModuleFormModel.class).stream()
                        .map(ModuleFormModel::getEnCode).collect(Collectors.toList());
            }
        }

        Object mainId = id;
        boolean autoIncrement = Objects.equals(formData.getPrimaryKeyPolicy(), 2);
        if (autoIncrement) {
            mainId = Long.parseLong(id);
        }
        //是否开启并发锁
        String version = "";
        if (Boolean.TRUE.equals(formData.getConcurrencyLock())) {
            //查询
            version = TableFeildsEnum.VERSION.getField();
        }

        List<FieLdsModel> list = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
        List<TableModel> tableModelList = JsonUtil.getJsonToList(visualdevEntity.getVisualTables(), TableModel.class);
        RecursionForm recursionForm = new RecursionForm();
        recursionForm.setList(list);
        recursionForm.setTableModelList(tableModelList);
        List<FormAllModel> formAllModel = new ArrayList<>();
        FormCloumnUtil.recursionForm(recursionForm, formAllModel);
        //form的属性
        List<FormAllModel> mast = formAllModel.stream().filter(t -> FormEnum.MAST.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        List<FormAllModel> table = formAllModel.stream().filter(t -> FormEnum.TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        List<FormAllModel> mastTable = formAllModel.stream().filter(t -> FormEnum.MAST_TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());

        TableModel mainTable = tableModelList.stream().filter(t -> t.getTypeId().equals("1")).findFirst().orElse(null);

        DbLinkEntity linkEntity = serviceUtil.getDbLink(visualdevEntity.getDbLinkId());
        try {
            DynamicDataSourceUtil.switchToDataSource(linkEntity);
            @Cleanup Connection conn = ConnUtil.getConnOrDefault(linkEntity);
            String dbType = conn.getMetaData().getDatabaseProductName();
            String pKeyName = this.getKey(mainTable, dbType);
            SqlTable mainSqlTable = SqlTable.of(mainTable.getTable());

            String flowTaskId = this.getFlowTaskId(mainTable, dbType);

            List<AndOrCriteriaGroup> groupList = new ArrayList<>();
            groupList.add(SqlBuilder.or(mainSqlTable.column(flowTaskId), SqlBuilder.isEqualTo(mainId.toString())));

            SelectStatementProvider render = SqlBuilder.select(mainSqlTable.allColumns())
                    .from(mainSqlTable)
                    .where(mainSqlTable.column(pKeyName), SqlBuilder.isEqualTo(mainId), groupList)
                    .build().render(RenderingStrategies.MYBATIS3);
            Map<String, Object> mainAllMap = Optional.ofNullable(flowFormDataMapper.selectOneMappedRow(render)).orElse(new HashMap<>());
            if (mainAllMap.size() == 0) {
                return new HashMap<>();
            }
            //主表
            List<String> mainTableFields = mast.stream().filter(m -> StringUtil.isNotEmpty(m.getFormColumnModel().getFieLdsModel().getVModel()))
                    .map(s -> s.getFormColumnModel().getFieLdsModel().getVModel()).collect(Collectors.toList());
            //开启权限移除字段
            if (needP) {
                if (CollUtil.isEmpty(formPerList)) {
                    mainTableFields = Collections.emptyList();
                } else {
                    List<String> newList = new ArrayList<>();
                    for (String item : mainTableFields) {
                        if (formPerList.contains(item)) {
                            newList.add(item);
                        }
                    }
                    mainTableFields = newList;
                }
            }
            if (StringUtil.isNotEmpty(version)) {
                mainTableFields.add(version);
            }
            mainTableFields.add(pKeyName);
            mainTableFields.add(flowTaskId);
            List<BasicColumn> mainTableBasicColumn = mainTableFields.stream().map(m -> SqlTable.of(mainTable.getTable()).column(m)).collect(Collectors.toList());
            SelectStatementProvider mainRender = SqlBuilder.select(mainTableBasicColumn)
                    .from(mainSqlTable)
                    .where(mainSqlTable.column(pKeyName), SqlBuilder.isEqualTo(mainId), groupList)
                    .build().render(RenderingStrategies.MYBATIS3);
            Map<String, Object> mainMap = flowFormDataMapper.selectOneMappedRow(mainRender);
            if (ObjectUtil.isNotEmpty(mainMap)) {
                //转换主表里的数据
                List<FieLdsModel> mainFieldList = mast.stream().filter(m -> StringUtil.isNotEmpty(m.getFormColumnModel().getFieLdsModel().getVModel()))
                        .map(t -> t.getFormColumnModel().getFieLdsModel()).collect(Collectors.toList());
                mainMap = formInfoUtils.swapDataInfoType(mainFieldList, mainMap);
                allDataMap.putAll(mainMap);
                allDataMap.put(FlowFormConstant.FLOWTASKID, mainMap.get(flowTaskId));
            }

            //副表
            Map<String, List<FormMastTableModel>> groupByTableNames = mastTable.stream().map(mt -> mt.getFormMastTableModel()).collect(Collectors.groupingBy(ma -> ma.getTable()));
            Iterator<Map.Entry<String, List<FormMastTableModel>>> entryIterator = groupByTableNames.entrySet().iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<String, List<FormMastTableModel>> next = entryIterator.next();
                String childTableName = next.getKey();
                List<FormMastTableModel> childMastTableList = next.getValue();
                //开启权限移除字段
                if (needP) {
                    if (CollUtil.isEmpty(formPerList)) {
                        childMastTableList = Collections.emptyList();
                    } else {
                        List<FormMastTableModel> newList = new ArrayList<>();
                        for (FormMastTableModel item : childMastTableList) {
                            if (formPerList.contains(item.getVModel())) {
                                newList.add(item);
                            }
                        }
                        childMastTableList = newList;
                    }
                }
                TableModel childTableModel = tableModelList.stream().filter(t -> t.getTable().equals(childTableName)).findFirst().orElse(null);
                SqlTable mastSqlTable = SqlTable.of(childTableName);
                List<BasicColumn> mastTableBasicColumn = childMastTableList.stream().filter(m -> StringUtil.isNotEmpty(m.getField()))
                        .map(m -> mastSqlTable.column(m.getField())).collect(Collectors.toList());
                //添加副表关联字段，不然数据会空没有字段名称
                mastTableBasicColumn.add(mastSqlTable.column(childTableModel.getTableField()));
                //主表主键
                String mainField = childTableModel.getRelationField();
                Object mainValue = new CaseInsensitiveMap<>(mainAllMap).get(mainField);
                //子表外键
                String childFoIdFiled = childTableModel.getTableField();
                //外键字段是否varchar转换
                TableFields fogIdField = childTableModel.getFields().stream().filter(t -> t.getField().equals(childFoIdFiled)).findFirst().orElse(null);
                boolean fogIdTypeString = Objects.nonNull(fogIdField) && fogIdField.getDataType().toLowerCase().contains("varchar");
                if (fogIdTypeString) {
                    mainValue = mainValue.toString();
                }

                QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder mastWhere = SqlBuilder.select(mastTableBasicColumn).from(mastSqlTable).where();
                mastWhere.and(mastSqlTable.column(childFoIdFiled), SqlBuilder.isEqualTo(mainValue));
                //逻辑删除不展示
                if (logicalDelete) {
                    mastWhere.and(mastSqlTable.column(TableFeildsEnum.DELETEMARK.getField()), SqlBuilder.isNull());
                }
                SelectStatementProvider mastRender = mastWhere.build().render(RenderingStrategies.MYBATIS3);
                List<Map<String, Object>> childMapList = flowFormDataMapper.selectManyMappedRows(mastRender);
                if (CollUtil.isNotEmpty(childMapList)) {
                    Map<String, Object> soloDataMap = childMapList.get(0);
                    Map<String, Object> renameKeyMap = new HashMap<>();
                    for (Map.Entry<String, Object> entry : soloDataMap.entrySet()) {
                        FormMastTableModel model = childMastTableList.stream().filter(child -> child.getField().equalsIgnoreCase(String.valueOf(entry.getKey()))).findFirst().orElse(null);
                        if (model != null) renameKeyMap.put(model.getVModel(), entry.getValue());
                    }
                    List<FieLdsModel> columnChildFields = childMastTableList.stream().map(cl -> cl.getMastTable().getFieLdsModel()).collect(Collectors.toList());
                    renameKeyMap = formInfoUtils.swapDataInfoType(columnChildFields, renameKeyMap);
                    allDataMap.putAll(renameKeyMap);
                }
            }

            //设计子表
            boolean finalNeedP = needP;
            List<String> finalFormPerList = formPerList;
            table.stream().map(t -> t.getChildList()).forEach(
                    t1 -> {
                        try {
                            String childTableName = t1.getTableName();
                            TableModel tableModel = tableModelList.stream().filter(tm -> tm.getTable().equals(childTableName)).findFirst().orElse(null);
                            SqlTable childSqlTable = SqlTable.of(childTableName);
                            List<FormColumnModel> chilFieldList = t1.getChildList().stream().filter(t2 -> StringUtil.isNotEmpty(t2.getFieLdsModel().getVModel())).collect(Collectors.toList());
                            String tableModelName = t1.getTableModel();
                            //开启权限移除字段
                            if (finalNeedP) {
                                if (CollUtil.isEmpty(finalFormPerList)) {
                                    chilFieldList = Collections.emptyList();
                                } else {
                                    List<FormColumnModel> newList = new ArrayList<>();
                                    for (FormColumnModel item : chilFieldList) {
                                        if (finalFormPerList.contains(tableModelName + "-" + item.getFieLdsModel().getVModel())) {
                                            newList.add(item);
                                        }
                                    }
                                    chilFieldList = newList;
                                }
                            }
                            List<BasicColumn> childFields = chilFieldList.stream().map(t2 -> childSqlTable.column(t2.getFieLdsModel().getVModel())).collect(Collectors.toList());
                            childFields.add(childSqlTable.column(tableModel.getTableField()));
                            String childKeyName = this.getKey(tableModel, dbType);
                            childFields.add(childSqlTable.column(childKeyName));
                            //主表主键
                            String mainField = tableModel.getRelationField();
                            Object mainValue = new CaseInsensitiveMap<>(mainAllMap).get(mainField);
                            //子表外键
                            String childFoIdFiled = tableModel.getTableField();
                            TableFields fogIdField = tableModel.getFields().stream().filter(t -> t.getField().equals(childFoIdFiled)).findFirst().orElse(null);
                            if (VisualConst.DB_INT_ALL.contains(fogIdField.getField().toLowerCase())) {
                                mainValue = Long.parseLong(String.valueOf(mainValue));
                            }

                            QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder childWhere = SqlBuilder.select(childFields).from(childSqlTable).where();
                            childWhere.and(childSqlTable.column(childFoIdFiled), SqlBuilder.isEqualTo(mainValue));
                            //逻辑删除不展示
                            if (logicalDelete) {
                                childWhere.and(childSqlTable.column(TableFeildsEnum.DELETEMARK.getField()), SqlBuilder.isNull());
                            }
                            SelectStatementProvider childRender = childWhere.build().render(RenderingStrategies.MYBATIS3);
                            List<Map<String, Object>> childMapList = flowFormDataMapper.selectManyMappedRows(childRender);
                            if (ObjectUtil.isNotEmpty(childMapList)) {
                                List<FieLdsModel> childFieldModels = t1.getChildList().stream().map(t2 -> t2.getFieLdsModel()).collect(Collectors.toList());
                                childMapList = childMapList.stream().map(c1 -> {
                                    try {
                                        return formInfoUtils.swapDataInfoType(childFieldModels, c1);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    return c1;
                                }).collect(Collectors.toList());
                                Map<String, Object> childMap = new HashMap<>(1);
                                childMap.put(t1.getTableModel(), childMapList);
                                allDataMap.putAll(childMap);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
            );
            for (Map.Entry<String, Object> keyItem : allDataMap.entrySet()) {
                if (pKeyName.equalsIgnoreCase(keyItem.getKey())) {
                    allDataMap.put("id", keyItem.getValue());
                    break;
                }
            }
        } catch (Exception e) {
            log.error("编辑页面数据：" + e.getMessage(), e);
        } finally {
            DynamicDataSourceUtil.clearSwitchDataSource();
        }
        return allDataMap;
    }

    //---------------------------------------------新增---------------------------------------------

    /**
     * 新增数据处理
     **/
    @DSTransactional
    public Map<String, Object> create(DataModel dataModel) throws WorkFlowException {
        try {
            //处理好的数据
            return this.createDataList(dataModel);
        } catch (DataException dataException) {
            throw new WorkFlowException(dataException.getMessage());
        } catch (Exception e) {
            //close
            e.printStackTrace();
            log.error("新增异常：{}", e.getMessage());
            throw new WorkFlowException(MsgCode.FA028.get(), e.getMessage());
        }
    }

    /**
     * 新增数据
     **/
    public Map<String, Object> createDataList(DataModel dataModel) throws SQLException, DataException {
        //处理好的数据
        Map<String, Object> result = new HashMap<>(16);
        List<TableModel> tableModelList = dataModel.getTableModelList();
        List<FormAllModel> formAllModel = dataModel.getFormAllModel();
        //有表数据处理
        if (!tableModelList.isEmpty()) {
            DbLinkEntity link = dataModel.getLink();
            DynamicDataSourceUtil.switchToDataSource(link);
            try {
                @Cleanup Connection conn = ConnUtil.getConnOrDefault(link);
                String dbType = conn.getMetaData().getDatabaseProductName().trim();
                dataModel.setDbType(dbType);
                //主表
                this.createMast(formAllModel, dataModel, result);
                //子表
                this.createTable(formAllModel, dataModel, result);
                //副表
                this.createMastTable(formAllModel, dataModel, result);
            } finally {
                DynamicDataSourceUtil.clearSwitchDataSource();
            }
        } else {
            //无表数据处理
            result = this.createAll(dataModel, formAllModel);
        }
        return result;
    }

    /**
     * 子表数据
     **/
    private void createTable(List<FormAllModel> formAllModel, DataModel dataModel, Map<String, Object> result) throws SQLException {
        log.info("结果数据:" + result.size());
        List<TableModel> tableModelList = dataModel.getTableModelList();
        Map<String, Object> dataNewMap = dataModel.getDataNewMap();
        boolean autoIncrement = Objects.equals(dataModel.getPrimaryKeyPolicy(), 2);
        String flowId = "";
        if (dataNewMap.get(FlowFormConstant.FLOWID) != null && StringUtil.isNotEmpty(dataNewMap.get(FlowFormConstant.FLOWID).toString())) {
            flowId = dataNewMap.get(FlowFormConstant.FLOWID).toString();
        }

        UserEntity userEntity = dataModel.getUserEntity();
        //子表
        List<FormAllModel> tableForm = formAllModel.stream().filter(t -> FormEnum.TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());

        for (FormAllModel itemModel : tableForm) {
            FormColumnTableModel childModel = itemModel.getChildList();
            if (Boolean.TRUE.equals(childModel.getDisabled())) continue;
            String key = childModel.getTableModel();
            String tableName = childModel.getTableName();
            //子表数据
            List<Map<String, Object>> chidList = dataNewMap.get(key) != null ? (List<Map<String, Object>>) dataNewMap.get(key) : new ArrayList<>();
            List<FormColumnModel> formColumnModels = childModel.getChildList().stream().filter(g -> StringUtil.isNotEmpty(g.getFieLdsModel().getVModel())).collect(Collectors.toList());
            if (Objects.equals(childModel.getIsNumLimit(), true) && chidList.size() > childModel.getNumLimit()) {
                throw new DataException(MsgCode.VS033.get(childModel.getLabel()));
            }

            //子表主键
            TableModel tableModel = tableModelList.stream().filter(k -> k.getTable().equals(tableName)).findFirst().orElse(null);
            String table = tableModel.getTable();
            //子表主键
            String childKeyName = this.getKey(tableModel, dataModel.getDbType());

            //子表外键字段
            String foreignKey = tableModel.getTableField();
            TableFields foreignField = tableModel.getFields().stream().filter(t -> t.getField().equalsIgnoreCase(foreignKey)).findFirst().orElse(null);
            if (foreignField == null) {
                throw new SQLException(MsgCode.COD001.get());
            }
            //主表关联字段
            Object foreignValue = dataNewMap.get(tableModel.getRelationField());
            if (VisualConst.DB_INT_ALL.contains(foreignField.getDataType())) {
                foreignValue = Long.parseLong(String.valueOf(foreignValue));
            }

            SqlTable sqlTable = SqlTable.of(table);
            for (Map<String, Object> objectMap : chidList) {
                if (StringUtil.isNotEmpty(flowId)) {
                    objectMap.put(FlowFormConstant.FLOWID, flowId);
                }
                GeneralInsertDSL generalInsertDSL = SqlBuilder.insertInto(sqlTable).set(sqlTable.column(foreignKey)).toValue(foreignValue);
                for (FormColumnModel column : formColumnModels) {
                    FieLdsModel fieLdsModel = column.getFieLdsModel();
                    String childKey = fieLdsModel.getVModel();
                    Object data = objectMap.get(childKey);
                    String fieldKey = fieLdsModel.getConfig().getParentVModel() + "-" + childKey;
                    //表单字段和外键字段重复不写入（写入外键值） //流程表单权限
                    if (Objects.equals(foreignKey, childKey)
                            || (CollUtil.isNotEmpty(dataModel.getFlowFormOperates()) && !isHasOperate(dataModel, fieldKey))
                            || (dataModel.getNeedPermission() && (CollUtil.isEmpty(dataModel.getFormPerList()) || !dataModel.getFormPerList().contains(fieldKey)))) {
                        continue;
                    }

                    //处理系统自动生成
                    data = this.create(fieLdsModel, data, true, userEntity, dataModel.isLinkOpen());
                    data = visualBillUtil.getBillNumber(dataModel.getVisualId(), fieLdsModel, objectMap, data);
                    getInsertDSL(sqlTable, generalInsertDSL, childKey, data);
                }
                if (!autoIncrement) {
                    generalInsertDSL = generalInsertDSL.set(sqlTable.column(childKeyName)).toValue(RandomUtil.uuId());
                }
                //租户信息
                addTenantId(generalInsertDSL, sqlTable);

                GeneralInsertStatementProvider insertRender = generalInsertDSL.build().render(RenderingStrategies.MYBATIS3);
                flowFormDataMapper.generalInsert(insertRender);
            }
        }
    }

    /**
     * 副表数据
     **/
    private void createMastTable(List<FormAllModel> formAllModel, DataModel dataModel, Map<String, Object> result) throws SQLException {
        List<TableModel> tableModelList = dataModel.getTableModelList();
        Map<String, Object> dataNewMap = dataModel.getDataNewMap();
        Integer primaryKeyPolicy = dataModel.getPrimaryKeyPolicy();

        UserEntity userEntity = dataModel.getUserEntity();
        //副表
        Map<String, List<FormAllModel>> mastTableAll = formAllModel.stream().filter(t -> FormEnum.MAST_TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.groupingBy(e -> e.getFormMastTableModel().getTable()));
        for (Map.Entry<String, List<FormAllModel>> keyItem : mastTableAll.entrySet()) {
            String key = keyItem.getKey();
            List<FormAllModel> masTableList = keyItem.getValue();
            Optional<TableModel> first = tableModelList.stream().filter(t -> t.getTable().equals(key)).findFirst();
            if (!first.isPresent()) {
                throw new SQLException(MsgCode.COD001.get());
            }
            TableModel tableModel = first.get();
            String tableModelTable = tableModel.getTable();
            String childKeyName = this.getKey(tableModel, dataModel.getDbType());

            //副表外键字段
            String foreignKey = tableModel.getTableField();
            TableFields foreignField = tableModel.getFields().stream().filter(t -> t.getField().equalsIgnoreCase(foreignKey)).findFirst().orElse(null);
            if (foreignField == null) {
                throw new SQLException(MsgCode.COD001.get());
            }
            //主表关联字段
            Object foreignValue = dataNewMap.get(tableModel.getRelationField());
            if (VisualConst.DB_INT_ALL.contains(foreignField.getDataType())) {
                foreignValue = Long.parseLong(String.valueOf(foreignValue));
            }

            SqlTable sqlTable = SqlTable.of(tableModelTable);
            GeneralInsertDSL generalInsertDSL = SqlBuilder.insertInto(sqlTable).set(sqlTable.column(foreignKey)).toValue(foreignValue);

            for (FormAllModel model : masTableList) {
                FormMastTableModel formMastTableModel = model.getFormMastTableModel();
                FormColumnModel mastTable = formMastTableModel.getMastTable();
                FieLdsModel fieLdsModel = mastTable.getFieLdsModel();
                String mostTableKey = fieLdsModel.getVModel();
                String field = formMastTableModel.getField();
                Object data = dataNewMap.get(mostTableKey);
                //表单字段和外键字段重复不写入（写入外键值）                 //流程表单权限
                if (StringUtil.isEmpty(mostTableKey) || Objects.equals(foreignKey, field)
                        || (CollUtil.isNotEmpty(dataModel.getFlowFormOperates()) && !isHasOperate(dataModel, mostTableKey))
                        || (dataModel.getNeedPermission() && (CollUtil.isEmpty(dataModel.getFormPerList()) || !dataModel.getFormPerList().contains(mostTableKey)))) {
                    continue;
                }

                //处理系统自动生成
                data = this.create(fieLdsModel, data, true, userEntity, dataModel.isLinkOpen());
                data = visualBillUtil.getBillNumber(dataModel.getVisualId(), fieLdsModel, dataNewMap, data);
                //返回值
                result.put(mostTableKey, data);
                getInsertDSL(sqlTable, generalInsertDSL, field, data);
            }
            //sql主键
            if (primaryKeyPolicy == 1) {
                generalInsertDSL = generalInsertDSL.set(sqlTable.column(childKeyName)).toValue(RandomUtil.uuId());
            }
            //租户信息
            addTenantId(generalInsertDSL, sqlTable);

            GeneralInsertStatementProvider insertRender = generalInsertDSL.build().render(RenderingStrategies.MYBATIS3);
            flowFormDataMapper.generalInsert(insertRender);
        }
    }

    /**
     * 主表数据
     **/
    private void createMast(List<FormAllModel> formAllModel, DataModel dataModel, Map<String, Object> result) throws SQLException {
        log.info("结果数据:" + result.size());
        List<TableModel> tableModelList = dataModel.getTableModelList();
        Map<String, Object> dataNewMap = dataModel.getDataNewMap();
        Object mainId = dataModel.getMainId();
        boolean autoIncrement = Objects.equals(dataModel.getPrimaryKeyPolicy(), 2);
        if (autoIncrement) {
            mainId = Long.parseLong(dataModel.getMainId());
        }
        UserEntity userEntity = dataModel.getUserEntity();
        Optional<TableModel> first = tableModelList.stream().filter(t -> "1".equals(t.getTypeId())).findFirst();
        if (!first.isPresent()) {
            throw new SQLException(MsgCode.COD001.get());
        }
        TableModel tableModel = first.get();
        String mastTableName = tableModel.getTable();
        List<FormAllModel> mastForm = formAllModel.stream().filter(t -> FormEnum.MAST.getMessage().equals(t.getJnpfKey())).filter(t -> StringUtil.isNotEmpty(t.getFormColumnModel().getFieLdsModel().getVModel())).collect(Collectors.toList());
        //新增字段
        String keyName = this.getKey(tableModel, dataModel.getDbType());

        SqlTable sqlTable = SqlTable.of(mastTableName);
        String flowTaskId = this.getFlowTaskId(tableModel, dataModel.getDbType());
        GeneralInsertDSL generalInsertDSL = SqlBuilder.insertInto(sqlTable).set(sqlTable.column(keyName)).toValue(mainId)
                .set(sqlTable.column(flowTaskId)).toValue(mainId.toString());
        if (autoIncrement) {
            generalInsertDSL = SqlBuilder.insertInto(sqlTable).set(sqlTable.column(flowTaskId)).toValue(mainId.toString());
        }

        for (FormAllModel model : mastForm) {
            FieLdsModel fieLdsModel = model.getFormColumnModel().getFieLdsModel();
            String field = fieLdsModel.getVModel();
            Object data = dataNewMap.get(field);
            data = data instanceof List ? JsonUtil.getObjectToString(data) : data;
            //流程表单权限
            if ((CollUtil.isNotEmpty(dataModel.getFlowFormOperates()) && !isHasOperate(dataModel, field))
                    || (dataModel.getNeedPermission() && (CollUtil.isEmpty(dataModel.getFormPerList()) || !dataModel.getFormPerList().contains(field)))) {
                continue;
            }
            //处理系统自动生成
            data = this.create(fieLdsModel, data, true, userEntity, dataModel.isLinkOpen());
            data = visualBillUtil.getBillNumber(dataModel.getVisualId(), fieLdsModel, dataNewMap, data);
            if (JnpfKeyConsts.BILLRULE.equals(fieLdsModel.getConfig().getJnpfKey())) dataNewMap.put(field, data);
            getInsertDSL(sqlTable, generalInsertDSL, field, data);
        }
        //判断是否开启锁
        if (Boolean.TRUE.equals(dataModel.getConcurrencyLock())) {
            generalInsertDSL = generalInsertDSL.set(sqlTable.column(TableFeildsEnum.VERSION.getField())).toValue(dataNewMap.get(TableFeildsEnum.VERSION.getField()));
        }
        //添加流程引擎信息
        if (dataNewMap.get(FlowFormConstant.FLOWID) != null && StringUtil.isNotEmpty(dataNewMap.get(FlowFormConstant.FLOWID).toString())) {
            generalInsertDSL = generalInsertDSL.set(sqlTable.column(TableFeildsEnum.FLOWID.getField())).toValue(dataNewMap.get(FlowFormConstant.FLOWID))
                    .set(sqlTable.column(TableFeildsEnum.FLOWSTATE.getField())).toValue(0);
        }
        //租户信息
        addTenantId(generalInsertDSL, sqlTable);

        GeneralInsertStatementProvider insertRender = generalInsertDSL.build().render(RenderingStrategies.MYBATIS3);
        flowFormDataMapper.generalInsert(insertRender);
        //设置实际主键id
        getRealMainId(dataModel, tableModel, keyName, autoIncrement, mastTableName, mainId);
        dataNewMap.put(keyName, dataModel.getMainId());
    }

    /**
     * 获取实际主键id
     *
     * @param dataModel
     * @param realIdKey     主表主键
     * @param autoIncrement 自增设置id
     * @param mastTableName
     * @throws SQLException
     */
    private void getRealMainId(DataModel dataModel, TableModel tableModel, String realIdKey, boolean autoIncrement, String mastTableName, Object flowTaskId) {
        if (autoIncrement) {
            String flowTaskKey = this.getFlowTaskId(tableModel, dataModel.getDbType());
            List<String> mastFile = new ArrayList<>();
            mastFile.add(flowTaskKey);
            mastFile.add(realIdKey);
            SqlTable mastSqlTable = SqlTable.of(mastTableName);
            SelectStatementProvider mastRender = SqlBuilder.select(mastFile.stream().map(mastSqlTable::column).collect(Collectors.toList()))
                    .from(mastSqlTable)
                    .where(mastSqlTable.column(flowTaskKey), SqlBuilder.isEqualTo(flowTaskId.toString()))
                    .build().render(RenderingStrategies.MYBATIS3);
            Map<String, Object> map = flowFormDataMapper.selectOneMappedRow(mastRender);
            dataModel.setMainId(map.get(realIdKey) != null ? map.get(realIdKey).toString() : dataModel.getMainId());
        }
    }

    /**
     * 新增系统赋值
     **/
    private Object create(FieLdsModel fieLdsModel, Object dataValue, boolean isTable, UserEntity userEntity, boolean isLink) {
        UserInfo user = UserProvider.getUser();
        String jnpfKey = fieLdsModel.getConfig().getJnpfKey();
        String rule = fieLdsModel.getConfig().getRule();
        String format = DateTimeFormatConstant.getFormat(fieLdsModel.getFormat());
        Object value = dataValue;
        //外链跳过系统参数生成
        if (isLinkSkip(isLink, jnpfKey)) return null;
        switch (jnpfKey) {
            case JnpfKeyConsts.CREATEUSER:
                value = userEntity.getId();
                break;
            case JnpfKeyConsts.CREATETIME:
                value = new Date();
                break;
            case JnpfKeyConsts.CURRORGANIZE:
            case JnpfKeyConsts.CURRDEPT:
                if (user != null && CollUtil.isNotEmpty(user.getOrganizeIds())) {
                    value = JsonUtil.getObjectToString(user.getOrganizeIds());
                }
                break;
            case JnpfKeyConsts.MODIFYTIME:
                value = null;
                break;
            case JnpfKeyConsts.MODIFYUSER:
                value = null;
                break;
            case JnpfKeyConsts.CURRPOSITION:
                if (user != null && CollUtil.isNotEmpty(user.getPositionIds())) {
                    value = JsonUtil.getObjectToString(user.getPositionIds());
                }
                break;
            case JnpfKeyConsts.BILLRULE:
                if (fieLdsModel.getConfig().getRuleType() == null || Objects.equals(fieLdsModel.getConfig().getRuleType(), 1)) {
                    try {
                        value = serviceUtil.getBillNumber(rule);
                    } catch (Exception e) {
                        value = null;
                    }
                }
                break;
            case JnpfKeyConsts.DATE:
            case JnpfKeyConsts.DATE_CALCULATE:
                value = getDateValue(dataValue, isTable, format, value);
                if (value == null) return null;
                break;
            case JnpfKeyConsts.NUM_INPUT:
            case JnpfKeyConsts.CALCULATE:
                value = getIntValue(dataValue, isTable, value);
                break;
            default:
                if (isTable) {
                    value = this.valueToNull(value);
                }
                break;
        }
        return value;
    }

    private static boolean isLinkSkip(boolean isLink, String jnpfKey) {
        if (isLink) {
            List<String> systemAttList = new ArrayList<>();
            systemAttList.add(JnpfKeyConsts.CREATEUSER);
            systemAttList.add(JnpfKeyConsts.CURRORGANIZE);
            systemAttList.add(JnpfKeyConsts.CURRPOSITION);
            systemAttList.add(JnpfKeyConsts.CURRDEPT);
            systemAttList.add(JnpfKeyConsts.MODIFYUSER);
            if (systemAttList.contains(jnpfKey)) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable Object getDateValue(Object dataValue, boolean isTable, String format, Object value) {
        if (isTable) {
            if (dataValue == null || "".equals(dataValue)) {
                return null;
            }
            if (dataValue instanceof String) {
                try {
                    SimpleDateFormat formatter = new SimpleDateFormat(format);
                    value = formatter.parse(dataValue.toString());
                } catch (ParseException var3) {
                    return null;
                }
            } else {
                value = new Date(Long.valueOf(String.valueOf(dataValue)));
            }
        }
        return value;
    }

    /**
     * 无表插入数据
     **/
    private Map<String, Object> createAll(DataModel dataModel, List<FormAllModel> formAllModel) {
        Map<String, Object> dataNewMap = dataModel.getDataNewMap();
        UserEntity userEntity = dataModel.getUserEntity();
        //处理好的数据
        Map<String, Object> result = new HashMap<>(16);
        List<FormAllModel> mastForm = formAllModel.stream().filter(t -> FormEnum.MAST.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        List<FormAllModel> tableForm = formAllModel.stream().filter(t -> FormEnum.TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        for (Map.Entry<String, Object> keyItem : dataNewMap.entrySet()) {
            String key = keyItem.getKey();
            FormAllModel model = mastForm.stream().filter(t -> key.equals(t.getFormColumnModel().getFieLdsModel().getVModel())).findFirst().orElse(null);
            if (model != null) {
                FieLdsModel fieLdsModel = model.getFormColumnModel().getFieLdsModel();
                Object data = keyItem.getValue();
                //处理系统自动生成
                data = this.create(fieLdsModel, data, false, userEntity, dataModel.isLinkOpen());
                result.put(key, data);
            } else {
                FormAllModel childModel = tableForm.stream().filter(t -> key.equals(t.getChildList().getTableModel())).findFirst().orElse(null);
                if (childModel != null) {
                    //子表主键
                    List<FormColumnModel> childList = childModel.getChildList().getChildList();
                    List<Map<String, Object>> childDataMap = (List<Map<String, Object>>) keyItem.getValue();
                    //子表处理的数据
                    List<Map<String, Object>> childResult = new ArrayList<>();
                    for (Map<String, Object> objectMap : childDataMap) {
                        //子表单体处理的数据
                        Map<String, Object> childOneResult = new HashMap<>(16);
                        for (Map.Entry<String, Object> childItem : objectMap.entrySet()) {
                            String childKey = childItem.getKey();
                            Object data = childItem.getValue();
                            FormColumnModel columnModel = childList.stream().filter(t -> childKey.equals(t.getFieLdsModel().getVModel())).findFirst().orElse(null);
                            if (columnModel != null) {
                                FieLdsModel fieLdsModel = columnModel.getFieLdsModel();

                                //处理系统自动生成
                                data = this.create(fieLdsModel, data, false, userEntity, dataModel.isLinkOpen());
                                childOneResult.put(childKey, data);
                            }
                        }
                        childResult.add(childOneResult);
                    }
                    result.put(key, childResult);
                }
            }
        }
        return result;
    }

    //--------------------------------------------修改 ----------------------------------------------------

    /**
     * 修改数据处理
     **/
    @DSTransactional
    public Map<String, Object> update(DataModel dataModel) throws WorkFlowException {
        try {
            //处理好的数据
            return this.updateDataList(dataModel);
        } catch (DataException dataException) {
            throw new WorkFlowException(dataException.getMessage());
        } catch (Exception e) {
            //close
            e.printStackTrace();
            log.error("修改异常：{}", e.getMessage());
            throw new WorkFlowException(MsgCode.FA029.get(), e.getMessage());
        }
    }

    /**
     * 修改数据
     **/
    public Map<String, Object> updateDataList(DataModel dataModel) throws SQLException, DataException {
        //处理好的数据
        Map<String, Object> result = new HashMap<>(16);
        List<TableModel> tableModelList = dataModel.getTableModelList();
        List<FormAllModel> formAllModel = dataModel.getFormAllModel();
        //有表数据处理
        if (!tableModelList.isEmpty()) {
            DbLinkEntity link = dataModel.getLink();
            DynamicDataSourceUtil.switchToDataSource(link);
            try {
                //系统数据
                @Cleanup Connection conn = ConnUtil.getConnOrDefault(link);
                String dbType = conn.getMetaData().getDatabaseProductName().trim();
                dataModel.setDbType(dbType);
                conn.setAutoCommit(false);
                //主表
                this.updateMast(formAllModel, dataModel);
                //子表
                this.updateTable(formAllModel, dataModel);
                //副表
                this.updateMastTable(formAllModel, dataModel);
            } finally {
                DynamicDataSourceUtil.clearSwitchDataSource();
            }
        } else {
            //无表数据处理
            result = this.updateAll(dataModel, formAllModel);
        }
        return result;
    }

    /**
     * 子表数据
     **/
    private void updateTable(List<FormAllModel> formAllModel, DataModel dataModel) throws SQLException {
        boolean onlyUpdate = dataModel.getOnlyUpdate();
        boolean logicalDelete = dataModel.getLogicalDelete();
        List<TableModel> tableModelList = dataModel.getTableModelList();
        Map<String, Object> dataNewMap = dataModel.getDataNewMap();
        Map<String, Object> oldMainData = dataModel.getOldMainData();
        if (MapUtils.isEmpty(oldMainData)) {
            return;
        }
        boolean autoIncrement = Objects.equals(dataModel.getPrimaryKeyPolicy(), 2);
        String flowId = "";
        if (dataNewMap.get(FlowFormConstant.FLOWID) != null && StringUtil.isNotEmpty(dataNewMap.get(FlowFormConstant.FLOWID).toString())) {
            flowId = dataNewMap.get(FlowFormConstant.FLOWID).toString();
        }

        //子表
        List<FormAllModel> tableForm = formAllModel.stream().filter(t -> FormEnum.TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());

        for (FormAllModel itemModel : tableForm) {
            FormColumnTableModel childModel = itemModel.getChildList();
            if (Boolean.TRUE.equals(childModel.getDisabled())) continue;
            String key = childModel.getTableModel();
            String tableName = childModel.getTableName();
            //子表数据
            List<Map<String, Object>> chidList = dataNewMap.get(key) != null ? (List<Map<String, Object>>) dataNewMap.get(key) : new ArrayList<>();
            List<FormColumnModel> formColumnModels = childModel.getChildList().stream().filter(g -> StringUtil.isNotEmpty(g.getFieLdsModel().getVModel())).collect(Collectors.toList());
            if (Objects.equals(childModel.getIsNumLimit(), true) && chidList.size() > childModel.getNumLimit()) {
                throw new DataException(MsgCode.VS033.get(childModel.getLabel()));
            }

            //子表主键
            TableModel tableModel = tableModelList.stream().filter(k -> k.getTable().equals(tableName)).findFirst().orElse(null);
            String table = tableModel.getTable();
            SqlTable sqlTable = SqlTable.of(table);
            String childPrimary = this.getKey(tableModel, dataModel.getDbType());

            //子表表外键字段
            String foreignKey = tableModel.getTableField();
            TableFields foreignField = tableModel.getFields().stream().filter(t -> t.getField().equalsIgnoreCase(foreignKey)).findFirst().orElse(null);
            if (foreignField == null) {
                throw new SQLException(MsgCode.COD001.get());
            }
            //主表关联字段
            Object foreignValue = dataNewMap.get(tableModel.getRelationField());
            Object oldForeignValue = oldMainData.get(tableModel.getRelationField());
            if (VisualConst.DB_INT_ALL.contains(foreignField.getDataType())) {
                foreignValue = Long.parseLong(String.valueOf(foreignValue));
                oldForeignValue = Long.parseLong(String.valueOf(oldForeignValue));
            }

            //查询旧的子表数据
            SelectStatementProvider render = SqlBuilder.select(sqlTable.column(childPrimary))
                    .from(sqlTable)
                    .where(sqlTable.column(foreignKey), SqlBuilder.isEqualTo(oldForeignValue))
                    .build().render(RenderingStrategies.MYBATIS3);
            List<Object> childIdList = flowFormDataMapper.selectManyMappedRows(render).stream().map(t -> new CaseInsensitiveMap<>(t).get(childPrimary)).collect(Collectors.toList());
            List<Object> formDataIdList = chidList.stream().filter(t -> new CaseInsensitiveMap<>(t).containsKey(childPrimary)).map(t -> new CaseInsensitiveMap<>(t).get(childPrimary)).collect(Collectors.toList());
            List<Object> deleteList = childIdList.stream().filter(t -> {
                List<String> jsonToList = JsonUtil.getJsonToList(formDataIdList, String.class);
                return !jsonToList.contains(String.valueOf(t));
            }).collect(Collectors.toList());

            if (CollUtil.isNotEmpty(deleteList) && !onlyUpdate) {//删除子表id数据
                //是否开启逻辑删除
                if (logicalDelete) {
                    UpdateDSL<UpdateModel> updateModelUpdateDSL = SqlBuilder.update(sqlTable);
                    updateModelUpdateDSL.set(sqlTable.column(TableFeildsEnum.DELETEMARK.getField())).equalTo(1);
                    updateModelUpdateDSL.set(sqlTable.column(TableFeildsEnum.DELETETIME.getField())).equalTo(new Date());
                    updateModelUpdateDSL.set(sqlTable.column(TableFeildsEnum.DELETEUSERID.getField())).equalTo(UserProvider.getUser().getUserId());
                    UpdateStatementProvider updateRender = updateModelUpdateDSL
                            .where(sqlTable.column(childPrimary), SqlBuilder.isIn(deleteList))
                            .build().render(RenderingStrategies.MYBATIS3);
                    flowFormDataMapper.update(updateRender);
                } else {
                    DeleteStatementProvider deleteRender = SqlBuilder.deleteFrom(sqlTable)
                            .where(sqlTable.column(childPrimary), SqlBuilder.isIn(deleteList))
                            .build().render(RenderingStrategies.MYBATIS3);
                    flowFormDataMapper.delete(deleteRender);
                }
            }

            for (Map<String, Object> objectMap : chidList) {
                objectMap = new CaseInsensitiveMap<>(objectMap);
                boolean isUpdate = false;
                if (CollUtil.isNotEmpty(childIdList)) {
                    List<String> jsonToList = JsonUtil.getJsonToList(childIdList, String.class);
                    if (objectMap.get(childPrimary) != null && jsonToList.contains(String.valueOf(objectMap.get(childPrimary)))) {
                        isUpdate = true;
                    }
                }
                if (StringUtil.isNotEmpty(flowId)) {
                    objectMap.put(FlowFormConstant.FLOWID, flowId);
                }
                GeneralInsertDSL generalInsertDSL = SqlBuilder.insertInto(sqlTable).set(sqlTable.column(foreignKey)).toValue(foreignValue);
                UpdateDSL<UpdateModel> updateModelUpdateDSL = SqlBuilder.update(sqlTable).set(sqlTable.column(foreignKey)).equalTo(foreignValue);
                for (FormColumnModel column : formColumnModels) {
                    FieLdsModel fieLdsModel = column.getFieLdsModel();
                    String childKey = fieLdsModel.getVModel();
                    String jnpfkey = fieLdsModel.getConfig().getJnpfKey();
                    Object data = objectMap.get(childKey);
                    String fieldKey = fieLdsModel.getConfig().getParentVModel() + "-" + childKey;
                    //流程表单权限
                    if (foreignKey.equalsIgnoreCase(childKey)
                            || (CollUtil.isNotEmpty(dataModel.getFlowFormOperates()) && !isHasOperate(dataModel, fieldKey))
                            || (dataModel.getNeedPermission() && (CollUtil.isEmpty(dataModel.getFormPerList()) || !dataModel.getFormPerList().contains(fieldKey)))
                            || (isUpdate && (JnpfKeyConsts.CURRORGANIZE.equals(jnpfkey) || JnpfKeyConsts.CURRPOSITION.equals(jnpfkey)
                            || JnpfKeyConsts.CREATETIME.equals(jnpfkey) || JnpfKeyConsts.CREATEUSER.equals(jnpfkey)))
                            || (!isUpdate && (JnpfKeyConsts.MODIFYUSER.equals(jnpfkey) || JnpfKeyConsts.MODIFYTIME.equals(jnpfkey)))) {
                        continue;
                    }

                    //处理系统自动生成
                    data = this.update(fieLdsModel, data, true, objectMap.get(childPrimary) == null);
                    data = visualBillUtil.getBillNumber(dataModel.getVisualId(), fieLdsModel, objectMap, data);
                    getInsertDSL(sqlTable, generalInsertDSL, childKey, data);
                    getUpdateDSL(sqlTable, updateModelUpdateDSL, childKey, data);
                }

                if (isUpdate) {//修改
                    updateModelUpdateDSL.where(sqlTable.column(childPrimary), SqlBuilder.isEqualTo(objectMap.get(childPrimary)));
                    UpdateStatementProvider render1 = updateModelUpdateDSL.build().render(RenderingStrategies.MYBATIS3);
                    flowFormDataMapper.update(render1);
                } else {//新增
                    //添加主键值和外键值
                    if (!autoIncrement) {
                        generalInsertDSL = generalInsertDSL.set(sqlTable.column(childPrimary)).toValue(RandomUtil.uuId());
                    }
                    //租户信息
                    addTenantId(generalInsertDSL, sqlTable);
                    GeneralInsertStatementProvider insertRender = generalInsertDSL.build().render(RenderingStrategies.MYBATIS3);
                    flowFormDataMapper.generalInsert(insertRender);
                }
            }
        }
    }

    /**
     * 副表数据
     **/
    private void updateMastTable(List<FormAllModel> formAllModel, DataModel dataModel) throws SQLException {
        List<TableModel> tableModelList = dataModel.getTableModelList();
        Map<String, Object> dataNewMap = dataModel.getDataNewMap();
        Map<String, Object> oldMainData = dataModel.getOldMainData();
        if (MapUtils.isEmpty(oldMainData)) {
            return;
        }

        //副表
        Map<String, List<FormAllModel>> mastTableAll = formAllModel.stream().filter(t -> FormEnum.MAST_TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.groupingBy(e -> e.getFormMastTableModel().getTable()));
        for (Map.Entry<String, List<FormAllModel>> keyItem : mastTableAll.entrySet()) {
            String key = keyItem.getKey();
            List<FormAllModel> masTableList = keyItem.getValue();
            //副表
            Optional<TableModel> first = tableModelList.stream().filter(t -> t.getTable().equals(key)).findFirst();
            if (!first.isPresent()) {
                throw new SQLException(MsgCode.COD001.get());
            }
            TableModel tableModel = first.get();
            String tableModelTable = tableModel.getTable();
            SqlTable sqlTable = SqlTable.of(tableModelTable);

            //副表外键字段
            String foreignKey = tableModel.getTableField();
            TableFields foreignField = tableModel.getFields().stream().filter(t -> t.getField().equalsIgnoreCase(foreignKey)).findFirst().orElse(null);
            if (foreignField == null) {
                throw new SQLException(MsgCode.COD001.get());
            }
            //主表关联字段
            Object foreignValue = dataNewMap.get(tableModel.getRelationField());
            Object oldForeignValue = oldMainData.get(tableModel.getRelationField());
            if (VisualConst.DB_INT_ALL.contains(foreignField.getDataType())) {
                foreignValue = Long.parseLong(String.valueOf(foreignValue));
                oldForeignValue = Long.parseLong(String.valueOf(oldForeignValue));
            }

            UpdateDSL<UpdateModel> updateModelUpdateDSL = SqlBuilder.update(sqlTable).set(sqlTable.column(foreignKey)).equalTo(foreignValue);

            for (FormAllModel model : masTableList) {
                FormMastTableModel formMastTableModel = model.getFormMastTableModel();
                FormColumnModel mastTable = formMastTableModel.getMastTable();
                FieLdsModel fieLdsModel = mastTable.getFieLdsModel();
                String mostTableKey = fieLdsModel.getVModel();
                String jnpfkey = fieLdsModel.getConfig().getJnpfKey();
                Object data = dataNewMap.get(mostTableKey);
                String field = formMastTableModel.getField();
                //外键值写入不覆盖  //流程表单权限
                if (foreignKey.equalsIgnoreCase(field) || JnpfKeyConsts.CURRORGANIZE.equals(jnpfkey) || JnpfKeyConsts.CURRPOSITION.equals(jnpfkey)
                        || JnpfKeyConsts.CREATETIME.equals(jnpfkey) || JnpfKeyConsts.CREATEUSER.equals(jnpfkey)
                        || (CollUtil.isNotEmpty(dataModel.getFlowFormOperates()) && !isHasOperate(dataModel, mostTableKey))
                        || (dataModel.getNeedPermission() && (CollUtil.isEmpty(dataModel.getFormPerList()) || !dataModel.getFormPerList().contains(mostTableKey)))) {
                    continue;
                }

                //处理系统自动生成
                data = this.update(fieLdsModel, data, true, false);
                data = visualBillUtil.getBillNumber(dataModel.getVisualId(), fieLdsModel, dataNewMap, data);
                getUpdateDSL(sqlTable, updateModelUpdateDSL, field, data);
            }

            UpdateStatementProvider updateStatementProvider = updateModelUpdateDSL
                    .where(sqlTable.column(foreignKey), SqlBuilder.isEqualTo(oldForeignValue))
                    .build().render(RenderingStrategies.MYBATIS3);
            flowFormDataMapper.update(updateStatementProvider);
        }
    }

    /**
     * 主表数据
     **/
    private void updateMast(List<FormAllModel> formAllModel, DataModel dataModel) throws SQLException {
        List<TableModel> tableModelList = dataModel.getTableModelList();
        Map<String, Object> dataNewMap = dataModel.getDataNewMap();
        List<FormAllModel> mastForm = formAllModel.stream().filter(t -> FormEnum.MAST.getMessage().equals(t.getJnpfKey())).filter(
                t -> StringUtil.isNotEmpty(t.getFormColumnModel().getFieLdsModel().getVModel())).collect(Collectors.toList());

        Optional<TableModel> first = tableModelList.stream().filter(t -> "1".equals(t.getTypeId())).findFirst();
        if (!first.isPresent()) {
            throw new SQLException(MsgCode.COD001.get());
        }
        TableModel tableModel = first.get();
        String mastTableName = tableModel.getTable();
        SqlTable sqlTable = SqlTable.of(mastTableName);
        TableFields keyFieldModel = tableModel.getFields().stream().filter(t -> Objects.equals(t.getPrimaryKey(), 1)
                && !t.getField().toLowerCase().contains(TableFeildsEnum.TENANTID.getField())).findFirst().orElse(null);
        if (keyFieldModel == null) {
            throw new SQLException(MsgCode.COD001.get());
        }
        String keyName = keyFieldModel.getField();

        //设置实际主键id
        Object mainId = dataModel.getMainId();
        if (VisualConst.DB_INT_ALL.contains(keyFieldModel.getDataType().toLowerCase())) {
            mainId = Long.parseLong(dataModel.getMainId());
        }
        dataNewMap.put(keyName, mainId);

        //查询旧的主表数据（用于确定修改时外键字段信息）
        SelectStatementProvider render = SqlBuilder.select(sqlTable.allColumns())
                .from(sqlTable)
                .where(sqlTable.column(keyName), SqlBuilder.isEqualTo(mainId))
                .build().render(RenderingStrategies.MYBATIS3);
        List<Map<String, Object>> maps = flowFormDataMapper.selectManyMappedRows(render);
        if (CollUtil.isEmpty(maps)) {
            return;
        }
        dataModel.setOldMainData(maps.get(0));

        UpdateDSL<UpdateModel> updateModelUpdateDSL = SqlBuilder.update(sqlTable);
        int num = 0;

        for (FormAllModel model : mastForm) {
            FieLdsModel fieLdsModel = model.getFormColumnModel().getFieLdsModel();
            String jnpfkey = fieLdsModel.getConfig().getJnpfKey();
            String field = fieLdsModel.getVModel();
            Object data = dataNewMap.get(field);

            //流程表单权限
            if ((CollUtil.isNotEmpty(dataModel.getFlowFormOperates()) && !isHasOperate(dataModel, field))
                    || (dataModel.getNeedPermission() && (CollUtil.isEmpty(dataModel.getFormPerList()) || !dataModel.getFormPerList().contains(field)))
                    || JnpfKeyConsts.CURRORGANIZE.equals(jnpfkey) || JnpfKeyConsts.CURRPOSITION.equals(jnpfkey)
                    || JnpfKeyConsts.CREATETIME.equals(jnpfkey) || JnpfKeyConsts.CREATEUSER.equals(jnpfkey)) {
                continue;
            }

            //处理系统自动生成
            data = this.update(fieLdsModel, data, true, false);
            data = visualBillUtil.getBillNumber(dataModel.getVisualId(), fieLdsModel, dataNewMap, data);
            getUpdateDSL(sqlTable, updateModelUpdateDSL, field, data);
            num++;
        }
        //判断是否开启锁
        if (Boolean.TRUE.equals(dataModel.getConcurrencyLock())) {
            updateModelUpdateDSL = updateModelUpdateDSL.set(sqlTable.column(TableFeildsEnum.VERSION.getField())).equalTo(dataNewMap.get(TableFeildsEnum.VERSION.getField()));
        }

        //添加流程引擎信息
        if (dataNewMap.get(FlowFormConstant.FLOWID) != null && StringUtil.isNotEmpty(dataNewMap.get(FlowFormConstant.FLOWID).toString())) {
            updateModelUpdateDSL = updateModelUpdateDSL.set(sqlTable.column(TableFeildsEnum.FLOWID.getField())).equalTo(dataNewMap.get(FlowFormConstant.FLOWID));
        }

        UpdateStatementProvider updateStatementProvider = updateModelUpdateDSL
                .where(sqlTable.column(keyName), SqlBuilder.isEqualTo(mainId))
                .build().render(RenderingStrategies.MYBATIS3);
        if (num > 0) {
            flowFormDataMapper.update(updateStatementProvider);
        }
    }

    /**
     * 修改无表数据
     **/
    private Map<String, Object> updateAll(DataModel dataModel, List<FormAllModel> formAllModel) {
        Map<String, Object> dataNewMap = dataModel.getDataNewMap();
        //处理好的数据
        Map<String, Object> result = new HashMap<>(16);
        //系统数据
        List<FormAllModel> mastForm = formAllModel.stream().filter(t -> FormEnum.MAST.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        List<FormAllModel> tableForm = formAllModel.stream().filter(t -> FormEnum.TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        for (Map.Entry<String, Object> keyItem : dataNewMap.entrySet()) {
            String key = keyItem.getKey();
            FormAllModel model = mastForm.stream().filter(t -> key.equals(t.getFormColumnModel().getFieLdsModel().getVModel())).findFirst().orElse(null);
            if (model != null) {
                FieLdsModel fieLdsModel = model.getFormColumnModel().getFieLdsModel();
                Object data = keyItem.getValue();
                //处理系统自动生成
                data = this.update(fieLdsModel, data, false, false);
                result.put(key, data);
            } else {
                FormAllModel childModel = tableForm.stream().filter(t -> key.equals(t.getChildList().getTableModel())).findFirst().orElse(null);
                if (childModel != null) {
                    List<Map<String, Object>> childDataMap = (List<Map<String, Object>>) keyItem.getValue();
                    //子表处理的数据
                    List<Map<String, Object>> childResult = new ArrayList<>();
                    for (Map<String, Object> objectMap : childDataMap) {
                        //子表单体处理的数据
                        Map<String, Object> childOneResult = new HashMap<>(16);
                        for (Map.Entry<String, Object> childItem : objectMap.entrySet()) {
                            String childKey = childItem.getKey();
                            Object data = childItem.getValue();
                            FormColumnModel columnModel = childModel.getChildList().getChildList().stream().filter(t -> childKey.equals(t.getFieLdsModel().getVModel())).findFirst().orElse(null);
                            if (columnModel != null) {
                                FieLdsModel fieLdsModel = columnModel.getFieLdsModel();
                                data = this.update(fieLdsModel, data, false, false);
                                childOneResult.put(childKey, data);
                            }
                        }
                        childResult.add(childOneResult);
                    }
                    result.put(key, childResult);
                }
            }
        }
        return result;
    }

    /**
     * childNeedAdd子表id为空需要生成单据规则（除子表外其他都是false）
     * 修改系统赋值
     **/
    private Object update(FieLdsModel fieLdsModel, Object dataValue, boolean isTable, boolean childNeedAdd) {
        String jnpfKey = fieLdsModel.getConfig().getJnpfKey();
        String rule = fieLdsModel.getConfig().getRule();
        String format = DateTimeFormatConstant.getFormat(fieLdsModel.getFormat());
        UserInfo user = UserProvider.getUser();
        Object value = dataValue;
        switch (jnpfKey) {
            case JnpfKeyConsts.CREATEUSER:
                value = user.getUserId();
                break;
            case JnpfKeyConsts.CREATETIME:
                value = new Date();
                break;
            case JnpfKeyConsts.CURRORGANIZE:
            case JnpfKeyConsts.CURRDEPT:
                if (user != null && CollUtil.isNotEmpty(user.getOrganizeIds())) {
                    value = JsonUtil.getObjectToString(user.getOrganizeIds());
                }
                break;
            case JnpfKeyConsts.MODIFYTIME:
                if (!childNeedAdd) {
                    value = new Date();
                }
                break;
            case JnpfKeyConsts.MODIFYUSER:
                if (!childNeedAdd) {
                    value = user.getUserId();
                }
                break;
            case JnpfKeyConsts.CURRPOSITION:
                if (user != null && CollUtil.isNotEmpty(user.getPositionIds())) {
                    value = JsonUtil.getObjectToString(user.getPositionIds());
                }
                break;
            case JnpfKeyConsts.BILLRULE:
                if (Objects.equals(fieLdsModel.getConfig().getRuleType(), 1) && (childNeedAdd || ObjectUtil.isEmpty(dataValue))) {
                    try {
                        value = serviceUtil.getBillNumber(rule);
                    } catch (Exception e) {
                        value = null;
                    }
                }
                break;
            case JnpfKeyConsts.DATE:
            case JnpfKeyConsts.DATE_CALCULATE:
                value = getDateValue(dataValue, isTable, format, value);
                if (value == null) return null;
                break;
            case JnpfKeyConsts.NUM_INPUT:
            case JnpfKeyConsts.CALCULATE:
                value = getIntValue(dataValue, isTable, value);
                break;
            default:
                if (isTable) {
                    value = this.valueToNull(value);
                }
                break;
        }
        return value;
    }

    private static Object getIntValue(Object dataValue, boolean isTable, Object value) {
        if (isTable) {
            try {
                value = new BigDecimal(String.valueOf(dataValue));
            } catch (Exception e) {
                log.error("在线数字控件异常：" + e.getMessage(), e);
            }
        }
        return value;
    }

    /**
     * 删除有表单条数据
     *
     * @param id
     * @param visualDevJsonModel
     * @return
     * @throws SQLException
     * @throws DataException
     */
    @DSTransactional
    public boolean deleteTable(String id, VisualDevJsonModel visualDevJsonModel, DbLinkEntity linkEntity) throws SQLException {
        boolean logicalDelete = visualDevJsonModel.getFormData().getLogicalDelete();
        Integer primaryKeyPolicy = visualDevJsonModel.getFormData().getPrimaryKeyPolicy();
        List<TableModel> tableModels = visualDevJsonModel.getVisualTables();

        List<FieLdsModel> list = JsonUtil.getJsonToList(visualDevJsonModel.getFormData().getFields(), FieLdsModel.class);
        RecursionForm recursionForm = new RecursionForm(list, tableModels);
        List<FormAllModel> formAllModel = new ArrayList<>();
        FormCloumnUtil.recursionForm(recursionForm, formAllModel);
        List<FormAllModel> tableForm = formAllModel.stream().filter(t -> FormEnum.TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        Map<String, Boolean> disableMap = new HashMap<>();
        for (FormAllModel allModel : tableForm) {
            disableMap.put(allModel.getChildList().getTableName(), allModel.getChildList().getDisabled());
        }
        //主表
        TableModel mainTableModel = tableModels.stream().filter(t -> "1".equals(t.getTypeId())).findFirst().orElse(null);
        String mainTable = mainTableModel.getTable();
        DynamicDataSourceUtil.switchToDataSource(linkEntity);
        try {
            @Cleanup Connection conn = ConnUtil.getConnOrDefault(linkEntity);
            String dbType = conn.getMetaData().getDatabaseProductName().trim();
            String pKeyName = this.getKey(mainTableModel, dbType);

            String flowTaskId = this.getFlowTaskId(mainTableModel, dbType);
            List<AndOrCriteriaGroup> groupList = new ArrayList<>();
            groupList.add(SqlBuilder.or(SqlTable.of(mainTable).column(flowTaskId), SqlBuilder.isEqualTo(id)));

            boolean autoIncrement = Objects.equals(primaryKeyPolicy, 2);
            Object selectId = id;
            if (autoIncrement) {
                selectId = Long.parseLong(id);
            }
            SelectStatementProvider queryMain = SqlBuilder.select(SqlTable.of(mainTable).allColumns())
                    .from(SqlTable.of(mainTable))
                    .where(SqlTable.of(mainTable).column(pKeyName), SqlBuilder.isEqualTo(selectId), groupList)
                    .build().render(RenderingStrategies.MYBATIS3);
            Map<String, Object> mainMap = flowFormDataMapper.selectOneMappedRow(queryMain);

            if (MapUtils.isNotEmpty(mainMap)) {
                Object realId = mainMap.get(pKeyName);
                //是否开启逻辑删除
                if (logicalDelete) {
                    SqlTable sqlt = SqlTable.of(mainTable);
                    UpdateDSL<UpdateModel> updateModelUpdateDSL = SqlBuilder.update(sqlt);
                    updateModelUpdateDSL.set(sqlt.column(TableFeildsEnum.DELETEMARK.getField())).equalTo(1);
                    updateModelUpdateDSL.set(sqlt.column(TableFeildsEnum.DELETETIME.getField())).equalTo(new Date());
                    updateModelUpdateDSL.set(sqlt.column(TableFeildsEnum.DELETEUSERID.getField())).equalTo(UserProvider.getUser().getUserId());
                    UpdateStatementProvider mainUpdate = updateModelUpdateDSL
                            .where(SqlTable.of(mainTable).column(pKeyName), SqlBuilder.isEqualTo(realId))
                            .build().render(RenderingStrategies.MYBATIS3);
                    flowFormDataMapper.update(mainUpdate);
                } else {
                    DeleteStatementProvider mainDelete = SqlBuilder.deleteFrom(SqlTable.of(mainTable))
                            .where(SqlTable.of(mainTable).column(pKeyName), SqlBuilder.isEqualTo(realId))
                            .build().render(RenderingStrategies.MYBATIS3);
                    flowFormDataMapper.delete(mainDelete);
                }

                if (tableModels.size() > 1) {
                    //去除主表
                    tableModels.remove(mainTableModel);
                    for (TableModel table : tableModels) {
                        if (CollUtil.isNotEmpty(disableMap) && disableMap.get(table.getTable()) != null && Boolean.TRUE.equals(disableMap.get(table.getTable()))) {
                            continue;
                        }
                        //主表关联-字段值
                        Object relationFieldValue = mainMap.get(table.getRelationField());
                        //子表字段-key
                        String tableField = table.getTableField();

                        if (logicalDelete) {
                            SqlTable sqlt = SqlTable.of(table.getTable());
                            UpdateDSL<UpdateModel> updateModelUpdateDSL = SqlBuilder.update(sqlt);
                            updateModelUpdateDSL.set(sqlt.column(TableFeildsEnum.DELETEMARK.getField())).equalTo(1);
                            updateModelUpdateDSL.set(sqlt.column(TableFeildsEnum.DELETETIME.getField())).equalTo(new Date());
                            updateModelUpdateDSL.set(sqlt.column(TableFeildsEnum.DELETEUSERID.getField())).equalTo(UserProvider.getUser().getUserId());
                            UpdateStatementProvider mainUpdate = updateModelUpdateDSL
                                    .where(SqlTable.of(table.getTable()).column(tableField), SqlBuilder.isEqualTo(relationFieldValue))
                                    .build().render(RenderingStrategies.MYBATIS3);
                            flowFormDataMapper.update(mainUpdate);
                        } else {
                            DeleteStatementProvider childDeleteProvider = SqlBuilder.deleteFrom(SqlTable.of(table.getTable()))
                                    .where(SqlTable.of(table.getTable()).column(tableField), SqlBuilder.isEqualTo(relationFieldValue))
                                    .build().render(RenderingStrategies.MYBATIS3);
                            flowFormDataMapper.delete(childDeleteProvider);
                        }
                    }
                }
            }
        } finally {
            DynamicDataSourceUtil.clearSwitchDataSource();
        }
        return true;
    }

    /**
     * 获取锁字段信息
     *
     * @param table
     * @param linkEntity
     * @param dataMap
     * @param id
     * @return
     */
    public Boolean getVersion(String table, DbLinkEntity linkEntity, Map<String, Object> dataMap, Object id) {
        boolean canUpdate = true;
        try {
            DynamicDataSourceUtil.switchToDataSource(linkEntity);
            @Cleanup Connection conn = ConnUtil.getConnOrDefault(linkEntity);
            String pKeyName = this.getTableKey(conn, table);
            SqlTable sqlTable = SqlTable.of(table);
            SelectStatementProvider render = SqlBuilder.select(sqlTable.column(TableFeildsEnum.VERSION.getField()))
                    .from(sqlTable)
                    .where(sqlTable.column(pKeyName), SqlBuilder.isEqualTo(id))
                    .and(sqlTable.column(TableFeildsEnum.VERSION.getField()), SqlBuilder.isEqualTo(dataMap.get(TableFeildsEnum.VERSION.getField())))
                    .build().render(RenderingStrategies.MYBATIS3);
            int count = flowFormDataMapper.selectManyMappedRows(render).size();
            canUpdate = count > 0;
        } catch (DataException | SQLException e) {
            log.error("切换数据源异常");
        } finally {
            DynamicDataSourceUtil.clearSwitchDataSource();
        }
        return canUpdate;
    }

    /**
     * 添加sql语句
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2023/3/29
     */
    private GeneralInsertDSL getInsertDSL(SqlTable sqlTable, GeneralInsertDSL generalInsertDSL, String field, Object data) {
        if (data == null || StringUtils.isEmpty(data.toString())) {
            generalInsertDSL = generalInsertDSL.set(sqlTable.column(field)).toNull();
        } else {
            generalInsertDSL = generalInsertDSL.set(sqlTable.column(field)).toValue(data);
        }
        return generalInsertDSL;
    }

    private UpdateDSL<UpdateModel> getUpdateDSL(SqlTable sqlTable, UpdateDSL<UpdateModel> updateModelUpdateDSL, String field, Object data) {
        if (data == null || StringUtils.isEmpty(data.toString())) {
            updateModelUpdateDSL = updateModelUpdateDSL.set(sqlTable.column(field)).equalToNull();
        } else {
            updateModelUpdateDSL = updateModelUpdateDSL.set(sqlTable.column(field)).equalTo(data);
        }
        return updateModelUpdateDSL;
    }

    /**
     * 判断数据为空或空数组转换成null
     *
     * @param value
     * @return
     */
    private Object valueToNull(Object value) {
        if (value instanceof List || value instanceof String[][]) {
            List<Object> l = (List) value;
            if (!l.isEmpty()) {
                value = JsonUtil.getObjectToString(value);
            } else {
                value = null;
            }
        } else if (value instanceof CharSequence && (StringUtils.isEmpty((CharSequence) value) || "[]".equals(value))) {
            value = null;
        }
        return value;
    }

    /**
     * 获取当前组织完整路径
     *
     * @param orgId
     * @return
     */
    public String getCurrentOrgIds(String orgId, String showLevel) {
        String orgIds = null;
        OrganizeEntity organizeEntity = serviceUtil.getOrganizeInfo(orgId);
        if (organizeEntity != null && StringUtil.isNotEmpty(organizeEntity.getOrganizeIdTree())) {
            String[] split = organizeEntity.getOrganizeIdTree().split(",");
            orgIds = split.length > 0 ? JsonUtil.getObjectToString(Arrays.asList(split)) : null;
        }
        if (!"all".equals(showLevel) && organizeEntity != null && "company".equals(organizeEntity.getCategory())) {
            orgIds = null;
        }
        return orgIds;
    }

    /**
     * 是否有流程表单权限
     *
     * @param dataModel
     * @param field
     * @return false 没有权限(需要跳过)
     */
    private boolean isHasOperate(DataModel dataModel, String field) {
        boolean hasOperate = true;
        for (Map<String, Object> item : dataModel.getFlowFormOperates()) {
            if (field.equals(item.get("id")) && (item.get("write") == null || "false".equals(item.get("write").toString()))) {
                hasOperate = false;
            }
        }
        return hasOperate;
    }

    /**
     * 添加租户信息
     *
     * @param generalInsertDSL
     * @param sqlTable
     */
    private void addTenantId(GeneralInsertDSL generalInsertDSL, SqlTable sqlTable) {
        //租户信息
        String tenantId = TenantDataSourceUtil.getTenantColumn();
        generalInsertDSL.set(sqlTable.column(TableFeildsEnum.TENANTID.getField())).toValue(tenantId);
    }

    public void saveState(VisualdevEntity visualdevEntity, String flowTaskId, Integer flowState) {
        List<TableModel> tableModels = JsonUtil.getJsonToList(visualdevEntity.getVisualTables(), TableModel.class);
        TableModel mainT = tableModels.stream().filter(t -> t.getTypeId().equals("1")).findFirst().orElse(null);
        if (mainT == null) {
            throw new DataException("主表不存在");
        }
        DbLinkEntity linkEntity = serviceUtil.getDbLink(visualdevEntity.getDbLinkId());
        try {
            DynamicDataSourceUtil.switchToDataSource(linkEntity);
            SqlTable sqlt = SqlTable.of(mainT.getTable());
            UpdateDSL<UpdateModel> updateModelUpdateDSL = SqlBuilder.update(sqlt);
            updateModelUpdateDSL.set(sqlt.column(TableFeildsEnum.FLOWSTATE.getField())).equalTo(flowState);
            UpdateStatementProvider mainUpdate = updateModelUpdateDSL
                    .where(SqlTable.of(mainT.getTable()).column(TableFeildsEnum.FLOWTASKID.getField()), SqlBuilder.isEqualTo(flowTaskId))
                    .build().render(RenderingStrategies.MYBATIS3);
            flowFormDataMapper.update(mainUpdate);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            DynamicDataSourceUtil.clearSwitchDataSource();
        }
    }
}
