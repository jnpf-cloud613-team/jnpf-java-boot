package jnpf.base.util;

import cn.hutool.core.util.ObjectUtil;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.mapper.FlowFormDataMapper;
import jnpf.base.model.CheckFormModel;
import jnpf.base.model.ColumnDataModel;
import jnpf.base.model.VisualConst;
import jnpf.base.model.form.FormCheckModel;
import jnpf.base.model.online.ImportDataModel;
import jnpf.base.model.online.ImportFormCheckUniqueModel;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.util.ConnUtil;
import jnpf.database.util.DynamicDataSourceUtil;
import jnpf.exception.DataException;
import jnpf.i18n.util.I18nUtil;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.FormDataModel;
import jnpf.model.visualjson.TableFields;
import jnpf.model.visualjson.TableModel;
import jnpf.model.visualjson.config.ConfigModel;
import jnpf.onlinedev.model.VisualErrInfo;
import jnpf.util.FlowFormConstant;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.TableFeildsEnum;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.AndOrCriteriaGroup;
import org.mybatis.dynamic.sql.BasicColumn;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.SqlTable;
import org.mybatis.dynamic.sql.render.RenderingStrategies;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.select.SelectModel;
import org.mybatis.dynamic.sql.select.join.EqualTo;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 验证表单数据
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2022/5/25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FormCheckUtils {
    public static final List<String> LINK_ALLOW_KEY = Collections.unmodifiableList(Arrays.asList("alert", "card", "collapse", "collapseItem", "colorPicker", "datePicker", "divider", "editor",
            "groupTitle", "iframe", "input", "inputNumber", "link", "location", "rate", "row", "slider", "stepItem", "steps", "switch", "tab", "tabItem", "table", "tableGrid",
            "tableGridTd", "tableGridTr", "text", "textarea", "timePicker"));
    private final FlowFormDataUtil flowDataUtil;
    private final FlowFormDataMapper flowFormDataMapper;

    public String checkForm(CheckFormModel checkFormModel) {
        List<FieLdsModel> formFieldList = checkFormModel.getFormFieldList();
        Map<String, Object> dataMap = checkFormModel.getDataMap();
        DbLinkEntity linkEntity = checkFormModel.getLinkEntity();
        List<TableModel> tableModelList = checkFormModel.getTableModelList();
        VisualdevEntity visualdevEntity = checkFormModel.getVisualdevEntity();
        String id = checkFormModel.getId();
        boolean isTransfer = checkFormModel.getIsTransfer();

        FormDataModel formData = JsonUtil.getJsonToBean(visualdevEntity.getFormData(), FormDataModel.class);
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(visualdevEntity.getColumnData(), ColumnDataModel.class);
        boolean inlineEdit = columnDataModel != null && columnDataModel.getType() != null && columnDataModel.getType() == 4;
        boolean logicalDelete = formData.getLogicalDelete();

        List<FieLdsModel> fields = new ArrayList<>();
        FormPublicUtils.recursionFieldsExceptChild(fields, formFieldList);
        try {
            //切换数据源
            DynamicDataSourceUtil.switchToDataSource(linkEntity);
            @Cleanup Connection conn = ConnUtil.getConnOrDefault(linkEntity);
            String dbType = conn.getMetaData().getDatabaseProductName().trim();
            //业务主键判断
            VisualErrInfo visualErrInfo = this.checkBusinessKey(fields, dataMap, tableModelList, formData, id);
            if (ObjectUtil.isNotEmpty(visualErrInfo) && StringUtil.isNotEmpty(visualErrInfo.getErrMsg())) {
                return visualErrInfo.getErrMsg();
            }
            //唯一和非空判断
            List<FormCheckModel> formCheckModels = new ArrayList<>();
            for (FieLdsModel fieLdsModel : fields) {
                ConfigModel config = fieLdsModel.getConfig();
                if (Boolean.TRUE.equals(checkFormModel.getIsLink()) && !LINK_ALLOW_KEY.contains(config.getJnpfKey())) {
                    continue;
                }
                Object o = dataMap.get(fieLdsModel.getVModel());
                if (config.isRequired() && !inlineEdit && !isTransfer && (ObjectUtil.isEmpty(o) || StringUtil.isEmpty(o.toString().trim()))) {
                    return I18nUtil.getMessageStr(config.getLabelI18nCode(), config.getLabel()) + MsgCode.VS015.get();
                }
                if (JnpfKeyConsts.COM_INPUT.equals(config.getJnpfKey()) && Boolean.TRUE.equals(config.getUnique())) {
                    o = ObjectUtil.isEmpty(o) ? null : String.valueOf(o).trim();
                    dataMap.put(fieLdsModel.getVModel(), o);
                    String tableName = fieLdsModel.getConfig().getTableName();
                    //验证唯一
                    SqlTable sqlTable = SqlTable.of(tableName);
                    String key = flowDataUtil.getTableKey(conn, tableName);
                    String vModelThis = fieLdsModel.getVModel();

                    String foriegKey = "";
                    String columnName = "";
                    boolean isMain = true;
                    TableModel mainTableModel = new TableModel();
                    TableModel tableModel = new TableModel();
                    for (TableModel item : tableModelList) {
                        if (Objects.equals(item.getTypeId(), "1")) {
                            mainTableModel = item;
                        }

                        if (StringUtil.isNotEmpty(fieLdsModel.getConfig().getRelationTable())) {
                            //子表判断
                            if (fieLdsModel.getConfig().getRelationTable().equals(item.getTable())) {
                                tableModel = item;
                            }
                        } else {
                            //主副表判断
                            if (fieLdsModel.getConfig().getTableName().equals(item.getTable())) {
                                tableModel = item;
                            }
                        }
                    }

                    if (tableModel != null) {
                        String fieldName = vModelThis;
                        if (!"1".equals(tableModel.getTypeId()) && vModelThis.contains(JnpfConst.SIDE_MARK)) {
                            fieldName = vModelThis.split(JnpfConst.SIDE_MARK)[1];
                            isMain = false;
                            foriegKey = tableModel.getTableField();
                        }
                        String finalFieldName = fieldName;
                        TableFields tableFields = tableModel.getFields().stream().filter(t -> t.getField().equals(finalFieldName)).findFirst().orElse(null);
                        if (tableFields != null) {
                            columnName = StringUtil.isNotEmpty(tableFields.getField()) ? tableFields.getField() : fieldName;
                        }
                    }

                    List<BasicColumn> selectKey = new ArrayList<>();
                    selectKey.add(sqlTable.column(columnName));
                    selectKey.add(sqlTable.column(key));
                    if (StringUtil.isNotEmpty(foriegKey)) {
                        String finalForiegKey = foriegKey;
                        TableFields tableFields = tableModel.getFields().stream().filter(t -> t.getField().equals(finalForiegKey)).findFirst().orElse(null);
                        if (tableFields != null) {
                            foriegKey = StringUtil.isNotEmpty(tableFields.getField()) ? tableFields.getField() : finalForiegKey;
                        }
                        selectKey.add(sqlTable.column(foriegKey));
                    }

                    SqlTable sqlMainTable = SqlTable.of(mainTableModel.getTable());
                    String taskIdField = TableFeildsEnum.FLOWTASKID.getField();
                    if (dbType.contains("Oracle") || dbType.contains("DM DBMS")) {
                        taskIdField = TableFeildsEnum.FLOWTASKID.getField().toUpperCase();
                    }
                    if (dataMap.get(FlowFormConstant.FLOWID) != null && StringUtil.isNotEmpty(dataMap.get(FlowFormConstant.FLOWID).toString())) {
                        selectKey.add(sqlMainTable.column(taskIdField));
                    }
                    QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder where;
                    //是否主表
                    if (isMain) {
                        where = SqlBuilder
                                .select(selectKey)
                                .from(sqlTable)
                                .where();
                    } else {
                        where = SqlBuilder
                                .select(selectKey)
                                .from(sqlMainTable)
                                .leftJoin(sqlTable)
                                .on(sqlTable.column(tableModel.getTableField()), new EqualTo(sqlMainTable.column(tableModel.getRelationField())))
                                .where();
                    }
                    if (ObjectUtil.isEmpty(o)) {
                        where.and(sqlTable.column(columnName), SqlBuilder.isNull());
                    } else {
                        where.and(sqlTable.column(columnName), SqlBuilder.isEqualTo(o.toString()));
                    }
                    if (StringUtils.isNotEmpty(id)) {
                        TableFields mainKeyModel = mainTableModel.getFields().stream().filter(t ->
                                Objects.equals(t.getPrimaryKey(), 1) && !t.getField().equalsIgnoreCase(TableFeildsEnum.TENANTID.getField())).findFirst().orElse(null);
                        Object realId = id;
                        if (VisualConst.DB_INT_ALL.contains(mainKeyModel.getDataType().toLowerCase())) {
                            realId = Long.parseLong(id);
                        }
                        where.and(sqlTable.column(mainKeyModel.getField()), SqlBuilder.isNotEqualTo(realId));
                    }
                    if (logicalDelete) {
                        where.and(sqlMainTable.column(TableFeildsEnum.DELETEMARK.getField()), SqlBuilder.isNull());
                    }
                    //是流程
                    if (dataMap.get(FlowFormConstant.FLOWID) != null && StringUtil.isNotEmpty(dataMap.get(FlowFormConstant.FLOWID).toString())) {
                        where.and(sqlMainTable.column(TableFeildsEnum.FLOWID.getField()), SqlBuilder.isEqualTo(dataMap.get(FlowFormConstant.FLOWID)));
                    } else {
                        where.and(sqlMainTable.column(TableFeildsEnum.FLOWID.getField()), SqlBuilder.isNull());
                    }

                    SelectStatementProvider render = where.build().render(RenderingStrategies.MYBATIS3);
                    FormCheckModel formCheckModel = new FormCheckModel();
                    formCheckModel.setLabel(I18nUtil.getMessageStr(fieLdsModel.getConfig().getLabelI18nCode(), fieLdsModel.getConfig().getLabel()));
                    formCheckModel.setStatementProvider(render);
                    formCheckModels.add(formCheckModel);
                }
            }

            //主副表数据库判重
            for (FormCheckModel formCheckModel : formCheckModels) {
                int count = flowFormDataMapper.selectManyMappedRows(formCheckModel.getStatementProvider()).size();
                if (count > 0) {
                    return formCheckModel.getLabel() + MsgCode.EXIST103.get();
                }
            }

            //子表当前表单数据判重
            List<FieLdsModel> childFieldList = fields.stream().filter(f -> JnpfKeyConsts.CHILD_TABLE.equals(f.getConfig().getJnpfKey())).collect(Collectors.toList());
            for (FieLdsModel fieLdsModel : childFieldList) {
                List<Map<String, Object>> childMapList = (List) dataMap.get(fieLdsModel.getVModel());
                if (ObjectUtil.isEmpty(childMapList)) continue;
                String tableName = I18nUtil.getMessageStr(fieLdsModel.getConfig().getLabelI18nCode(), fieLdsModel.getConfig().getLabel());
                for (FieLdsModel childField : fieLdsModel.getConfig().getChildren()) {
                    ConfigModel config = childField.getConfig();
                    if (Boolean.TRUE.equals(checkFormModel.getIsLink()) && !LINK_ALLOW_KEY.contains(config.getJnpfKey())) {
                        continue;
                    }
                    //判断为空
                    if (config.isRequired() && !inlineEdit && !isTransfer) {
                        for (Map<String, Object> item : childMapList) {
                            Object o = item.get(childField.getVModel());
                            if (ObjectUtil.isEmpty(o) || StringUtil.isEmpty(o.toString().trim())) {
                                return tableName + "-" + I18nUtil.getMessageStr(config.getLabelI18nCode(), config.getLabel()) + MsgCode.VS015.get();
                            }
                        }
                    }
                    //判断唯一
                    if (JnpfKeyConsts.COM_INPUT.equals(config.getJnpfKey()) && Boolean.TRUE.equals(config.getUnique())) {
                        List<String> childValues = childMapList.stream().filter(childTbMap -> childTbMap.get(childField.getVModel()) != null)
                                .map(childTbMap -> String.valueOf(childTbMap.get(childField.getVModel())).trim()).collect(Collectors.toList());
                        if (!childValues.isEmpty()) {
                            HashSet<String> child = new HashSet<>(childValues);
                            if (child.size() != childValues.size()) {
                                return tableName + "-" + I18nUtil.getMessageStr(config.getLabelI18nCode(), config.getLabel()) + MsgCode.EXIST103.get();
                            }
                        }
                    }
                }
            }

            if (conn != null) {
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DynamicDataSourceUtil.clearSwitchDataSource();
        }
        return "";
    }

    public String getCount(Object id, SqlTable sqlTable, TableModel tableModel, DbLinkEntity linkEntity) {
        try {
            DynamicDataSourceUtil.switchToDataSource(linkEntity);
            @Cleanup Connection conn = ConnUtil.getConnOrDefault(linkEntity);
            String dbType = conn.getMetaData().getDatabaseProductName().trim();
            String key = flowDataUtil.getKey(tableModel, dbType);
            String flowTaskId = flowDataUtil.getFlowTaskId(tableModel, dbType);
            List<AndOrCriteriaGroup> groupList = new ArrayList<>();
            groupList.add(SqlBuilder.or(sqlTable.column(flowTaskId), SqlBuilder.isEqualTo(id.toString())));

            SelectStatementProvider countRender = SqlBuilder
                    .select(sqlTable.column(key))
                    .from(sqlTable)
                    .where(sqlTable.column(key), SqlBuilder.isEqualTo(id), groupList)
                    .build().render(RenderingStrategies.MYBATIS3);
            Map<String, Object> map = flowFormDataMapper.selectOneMappedRow(countRender);
            if (MapUtils.isNotEmpty(map) && Objects.nonNull(map.get(key))) {
                return map.get(key).toString();
            }
        } catch (DataException e) {
            log.error(e.getMessage(), e);
        } catch (SQLException sqlException) {
            log.error(sqlException.getMessage(), sqlException);
        } finally {
            DynamicDataSourceUtil.clearSwitchDataSource();
        }
        return null;
    }

    /**
     * 业务主键 判断
     *
     * @param fields
     * @param data
     * @param tableList
     * @param formData
     * @param id
     * @return
     */
    public VisualErrInfo checkBusinessKey(List<FieLdsModel> fields, Map<String, Object> data, List<TableModel> tableList, FormDataModel formData, String id) {
        boolean logicalDelete = formData.getLogicalDelete();
        if (formData.isUseBusinessKey()) {
            List<String> businessKeyList = Arrays.asList(formData.getBusinessKeyList());
            if (CollectionUtils.isEmpty(businessKeyList)) return null;
            TableModel mainTable = tableList.stream().filter(t -> t.getTypeId().equals("1")).findFirst().orElse(null);
            TableFields mainFields = mainTable.getFields().stream().filter(t -> Objects.equals(t.getPrimaryKey(), 1)
                    && !t.getField().equalsIgnoreCase(TableFeildsEnum.TENANTID.getField())).findFirst().orElse(null);

            SqlTable sqlTable = SqlTable.of(mainTable.getTable());
            String mainKey = mainFields.getField();
            String taskIdField = TableFeildsEnum.FLOWTASKID.getField();
            TableFields taskIdFielde = mainTable.getFields().stream().filter(t -> TableFeildsEnum.FLOWTASKID.getField().equalsIgnoreCase(t.getField())).findFirst().orElse(null);
            if (ObjectUtil.isNotEmpty(taskIdFielde)) {
                taskIdField = taskIdFielde.getField();
            }
            List<BasicColumn> selectKey = new ArrayList<>();
            selectKey.add(sqlTable.column(mainKey));
            if (data.get(FlowFormConstant.FLOWID) != null && StringUtil.isNotEmpty(data.get(FlowFormConstant.FLOWID).toString())) {
                selectKey.add(sqlTable.column(taskIdField));
            }
            QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder where = SqlBuilder.select(selectKey).from(sqlTable).where();

            for (FieLdsModel fieLdsModel : fields) {
                setEachCondition(data, fieLdsModel, businessKeyList, where, sqlTable);
            }
            //更新跳过本条数据
            if (StringUtils.isNotEmpty(id)) {
                Object realId = id;
                if (VisualConst.DB_INT_ALL.contains(mainFields.getDataType().toLowerCase())) {
                    realId = Long.parseLong(id);
                }
                where.and(sqlTable.column(mainKey), SqlBuilder.isNotEqualTo(realId));
            }
            //流程数据过滤
            if (data.get(FlowFormConstant.FLOWID) != null && StringUtil.isNotEmpty(data.get(FlowFormConstant.FLOWID).toString())) {
                where.and(sqlTable.column(TableFeildsEnum.FLOWID.getField()), SqlBuilder.isEqualTo(data.get(FlowFormConstant.FLOWID)));
            } else {
                where.and(sqlTable.column(TableFeildsEnum.FLOWID.getField()), SqlBuilder.isNull());
            }
            //逻辑删除
            if (logicalDelete) {
                where.and(sqlTable.column(TableFeildsEnum.DELETEMARK.getField()), SqlBuilder.isNull());
            }
            SelectStatementProvider render = where.build().render(RenderingStrategies.MYBATIS3);
            List<Map<String, Object>> maps;
            try {
                maps = flowFormDataMapper.selectManyMappedRows(render);
            } catch (Exception e) {
                return null;
            }
            int count = maps.size();
            if (count > 0) {
                VisualErrInfo visualErrInfo = VisualErrInfo.builder().errMsg(formData.getBusinessKeyTip()).build();
                if (ObjectUtil.isNotEmpty(maps.get(0).get(mainKey))) {
                    visualErrInfo.setId(maps.get(0).get(mainKey).toString());
                }
                if (ObjectUtil.isNotEmpty(maps.get(0).get(taskIdField))) {
                    visualErrInfo.setFlowTaskId(maps.get(0).get(taskIdField).toString());
                }
                return visualErrInfo;
            }
        }
        return null;
    }

    private static void setEachCondition(Map<String, Object> data, FieLdsModel fieLdsModel, List<String> businessKeyList, QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder where, SqlTable sqlTable) {
        String fieldName = fieLdsModel.getVModel();
        Object o = data.get(fieldName);
        if (businessKeyList.contains(fieldName)) {
            if (ObjectUtil.isEmpty(o)) {
                where.and(sqlTable.column(fieldName), SqlBuilder.isNull());
            } else {
                o = o instanceof List ? JsonUtil.getObjectToString(o) : o;
                if (JnpfKeyConsts.DateSelect.contains(fieLdsModel.getConfig().getJnpfKey())) {
                    try {
                        o = new Date((Long) o);
                    } catch (Exception e) {
                        try {
                            String format = fieLdsModel.getFormat();
                            SimpleDateFormat sdf = new SimpleDateFormat(format);
                            o = sdf.parse(String.valueOf(o));
                        } catch (Exception ex) {
                            log.error(ex.getMessage(), ex);
                        }
                    }
                }
                where.and(sqlTable.column(fieldName), SqlBuilder.isEqualTo(o));
            }
        }
    }

    /**
     * 判断excel表内业务主键是否重复
     *
     * @param formData
     * @param resultMap
     * @param uniqueModel
     * @return
     */
    public String checkBusinessKeyExcel(FormDataModel formData, Map<String, Object> resultMap, ImportFormCheckUniqueModel uniqueModel) {
        List<String> businessKeyList = Arrays.asList(formData.getBusinessKeyList());
        List<Map<String, Object>> successList = uniqueModel.getImportDataModel().stream().map(ImportDataModel::getResultData).collect(Collectors.toList());
        if (com.baomidou.mybatisplus.core.toolkit.CollectionUtils.isNotEmpty(successList)) {
            boolean exists = false;
            for (int uniqueIndex = 0; uniqueIndex < successList.size(); uniqueIndex++) {
                Map<String, Object> indexMap = successList.get(uniqueIndex);
                boolean hasB = false;
                for (String fieldsKey : businessKeyList) {
                    if ((resultMap.get(fieldsKey) == null && indexMap.get(fieldsKey) == null)
                            || (resultMap.get(fieldsKey) != null && resultMap.get(fieldsKey).equals(indexMap.get(fieldsKey)))) {
                        hasB = true;
                    } else {
                        //有一个没匹配上就跳出循环
                        hasB = false;
                        break;
                    }
                }
                if (hasB) {
                    exists = true;
                    if (uniqueModel.isUpdate()) {
                        uniqueModel.getImportDataModel().get(uniqueIndex).setResultData(resultMap);
                    }
                    break;
                }
            }
            if (exists) {
                return formData.getBusinessKeyTip();
            }
        }
        return null;
    }
}
