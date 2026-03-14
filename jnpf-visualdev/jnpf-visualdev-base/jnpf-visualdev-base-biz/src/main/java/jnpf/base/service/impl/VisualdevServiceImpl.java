package jnpf.base.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import jnpf.base.entity.VisualAliasEntity;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.entity.VisualdevReleaseEntity;
import jnpf.base.mapper.FlowFormDataMapper;
import jnpf.base.mapper.VisualAliasMapper;
import jnpf.base.mapper.VisualdevMapper;
import jnpf.base.mapper.VisualdevReleaseMapper;
import jnpf.base.model.PaginationVisualdev;
import jnpf.base.model.dbtable.vo.DbFieldVO;
import jnpf.base.model.export.VisualExportVo;
import jnpf.base.model.form.VisualTableModel;
import jnpf.base.service.DbLinkService;
import jnpf.base.service.DbTableService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.VisualdevService;
import jnpf.base.util.ConcurrencyUtils;
import jnpf.base.util.VisualDevTableCre;
import jnpf.base.util.VisualUtils;
import jnpf.constant.CodeConst;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.database.constant.DbAliasConst;
import jnpf.database.model.dbfield.DbFieldModel;
import jnpf.database.model.dbfield.base.DbFieldModelBase;
import jnpf.database.model.dbtable.DbTableFieldModel;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.util.ConnUtil;
import jnpf.database.util.DataSourceUtil;
import jnpf.database.util.DynamicDataSourceUtil;
import jnpf.exception.DataException;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.TaskEntity;
import jnpf.model.OnlineDevData;
import jnpf.model.visualjson.*;
import jnpf.model.visualjson.analysis.FormAllModel;
import jnpf.model.visualjson.analysis.FormEnum;
import jnpf.model.visualjson.analysis.FormMastTableModel;
import jnpf.model.visualjson.analysis.RecursionForm;
import jnpf.permission.service.CodeNumService;
import jnpf.util.*;
import jnpf.util.visiual.JnpfKeyConsts;
import jnpf.workflow.service.WorkFlowApi;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.SqlTable;
import org.mybatis.dynamic.sql.render.RenderingStrategies;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.select.SelectModel;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.mybatis.dynamic.sql.update.UpdateModel;
import org.mybatis.dynamic.sql.update.render.UpdateStatementProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/3/16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisualdevServiceImpl extends SuperServiceImpl<VisualdevMapper, VisualdevEntity> implements VisualdevService {

    private final CodeNumService codeNumService;
    private final VisualDevTableCre visualDevTableCreUtil;
    private final ConcurrencyUtils concurrencyUtils;
    private final DbTableService dbTableService;
    private final DbLinkService dblinkService;
    private final DataSourceUtil dataSourceUtil;
    private final WorkFlowApi taskApi;
    private final VisualdevReleaseMapper visualdevReleaseMapper;
    private final FlowFormDataMapper flowFormDataMapper;
    private final VisualAliasMapper visualAliasMapper;

    private static final String FIELDS = "fields";

    @Override
    public List<VisualdevEntity> getList(PaginationVisualdev paginationVisualdev) {
        return this.baseMapper.getList(paginationVisualdev);
    }

    @Override
    public List<VisualdevEntity> getPageList(PaginationVisualdev paginationVisualdev) {
        return visualdevReleaseMapper.getPageList(paginationVisualdev);
    }

    @Override
    public List<VisualdevEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public VisualdevEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public VisualdevEntity getReleaseInfo(String id) {
        VisualdevReleaseEntity visualdevReleaseEntity = visualdevReleaseMapper.selectById(id);
        VisualdevEntity visualdevEntity = null;
        if (visualdevReleaseEntity != null) {
            visualdevEntity = JsonUtil.getJsonToBean(visualdevReleaseEntity, VisualdevEntity.class);
        }
        if (visualdevEntity == null) {
            visualdevEntity = getById(id);
        }
        return visualdevEntity;
    }

    @Override
    @SneakyThrows
    public Boolean create(VisualdevEntity entity) {
        if (StringUtil.isEmpty(entity.getId())) {
            entity.setId(RandomUtil.uuId());
        }
        if (OnlineDevData.FORM_TYPE_DEV.equals(entity.getType())) {
            FormDataModel formDataModel = JsonUtil.getJsonToBean(entity.getFormData(), FormDataModel.class);
            if (formDataModel != null) {
                //是否开启安全锁
                int primaryKeyPolicy = formDataModel.getPrimaryKeyPolicy();

                //判断是否要创表
                List<TableModel> tableModels = JsonUtil.getJsonToList(entity.getVisualTables(), TableModel.class);
                //有表
                if (!tableModels.isEmpty()) {

                    for (TableModel tableModel : tableModels) {
                        boolean isAutoIncre = this.getPrimaryDbField(entity.getDbLinkId(), tableModel.getTable());
                        // 1:雪花ID 2:自增ID
                        if (primaryKeyPolicy == 1 && isAutoIncre) {
                            throw new WorkFlowException(MsgCode.VS022.get(tableModel.getTable()));
                        } else if (primaryKeyPolicy == 2 && !isAutoIncre) {
                            throw new WorkFlowException(MsgCode.VS023.get(tableModel.getTable()));
                        }
                    }

                    try {
                        String tableJsonMap = addDbFileds(entity.getDbLinkId(), tableModels, formDataModel);
                        if (StringUtil.isNotEmpty(tableJsonMap)) {
                            entity.setVisualTables(tableJsonMap);
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        }

        if (StringUtil.isEmpty(entity.getEnCode())) {
            setAutoEnCode(entity);
        }
        entity.setEnabledMark(0);
        entity.setState(0);
        entity.setCreatorTime(new Date());
        entity.setCreatorUserId(UserProvider.getLoginUserId());
        entity.setLastModifyTime(null);
        entity.setLastModifyUserId(null);
        this.setIgnoreLogicDelete().removeById(entity.getId());
        boolean result = this.setIgnoreLogicDelete().saveOrUpdate(entity);
        this.clearIgnoreLogicDelete();
        return result;
    }

    /**
     * 数据库表，添加字段
     *
     * @param dbLinkId      数据链接id
     * @param visualTables  表列表
     * @param formDataModel 表单属性
     * @throws Exception
     */
    private String addDbFileds(String dbLinkId, List<TableModel> visualTables, FormDataModel formDataModel) throws Exception {
        if (CollectionUtils.isEmpty(visualTables)) return null;

        List<Map<String, Object>> tableJsonMap = new ArrayList<>();
        boolean concurrencyLock = formDataModel.getConcurrencyLock();
        boolean logicalDelete = formDataModel.getLogicalDelete();
        //在各个表创建多租户字段强
        for (TableModel tableModel : visualTables) {
            boolean isMainTable = tableModel.getTypeId().equals("1");
            List<DbFieldModel> dbFieldModelList = dbTableService.getFieldList(dbLinkId, tableModel.getTable());
            List<DbFieldModelBase> fieldList = JsonUtil.getJsonToList(dbFieldModelList, DbFieldModelBase.class);
            List<DbFieldModel> addList = new ArrayList<>();
            DbLinkEntity dbLink = dblinkService.getInfo(dbLinkId);
            String type = dbLink != null ? dbLink.getDbType() : dataSourceUtil.getDbType();
            concurrencyUtils.createTenantId(fieldList, type, addList);
            if (logicalDelete) {
                concurrencyUtils.creDeleteMark(fieldList, type, addList);
            }
            if (isMainTable && concurrencyLock) {
                concurrencyUtils.createVersion(fieldList, type, addList);
            }
            if (isMainTable) {
                //流程字段强制生成
                concurrencyUtils.createFlowEngine(fieldList, type, addList);
                concurrencyUtils.createFlowTaskId(fieldList, type, addList);
                concurrencyUtils.createFlowState(fieldList, type, addList);
            }
            concurrencyUtils.addFileds(tableModel.getTable(), dbLinkId, addList);
            List<DbFieldVO> voList = new ArrayList<>();
            List<DbFieldModelBase> listAll = new ArrayList<>();
            listAll.addAll(fieldList);
            listAll.addAll(addList);
            for (DbFieldModelBase item : listAll) {
                DbFieldVO tableFields = new DbFieldVO();
                tableFields.setField(item.getField());
                tableFields.setFieldName(item.getComment());
                tableFields.setDataType(item.getDataType());
                tableFields.setDataLength(item.getLength());
                tableFields.setPrimaryKey(DbAliasConst.PRIMARY_KEY.getNum(item.getIsPrimaryKey()));
                tableFields.setAllowNull(DbAliasConst.ALLOW_NULL.getNum(item.getNullSign()));
                tableFields.setAutoIncrement(DbAliasConst.AUTO_INCREMENT.getNum(item.getIsAutoIncrement()));
                tableFields.setIdentity(DbAliasConst.AUTO_INCREMENT.getNum(item.getIsAutoIncrement()));
                voList.add(tableFields);
            }
            Map<String, Object> stringObjectMap = JsonUtil.entityToMap(tableModel);
            stringObjectMap.put(FIELDS, voList);
            tableJsonMap.add(stringObjectMap);
        }
        return JsonUtil.getObjectToString(tableJsonMap);
    }

    @Override
    public boolean update(String id, VisualdevEntity entity) throws WorkFlowException, SQLException {
        entity.setId(id);
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        entity.setLastModifyTime(DateUtil.getNowDate());
        if (StringUtil.isEmpty(entity.getEnCode())) {
            setAutoEnCode(entity);
        }
        if (OnlineDevData.FORM_TYPE_DEV.equals(entity.getType())) {
            //代码生成修改时就要生成字段
            FormDataModel formDataModel = JsonUtil.getJsonToBean(entity.getFormData(), FormDataModel.class);
            if (formDataModel != null) {
                int primaryKeyPolicy = formDataModel.getPrimaryKeyPolicy();
                //判断是否要创表
                List<TableModel> visualTables = JsonUtil.getJsonToList(entity.getVisualTables(), TableModel.class);
                //有表
                if (!visualTables.isEmpty()) {
                    try {
                        //字段强制生成
                        String tableJsonMap = addDbFileds(entity.getDbLinkId(), visualTables, formDataModel);
                        if (StringUtil.isNotEmpty(tableJsonMap)) {
                            entity.setVisualTables(tableJsonMap);
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                    //判断自增是否匹配
                    concurrencyUtils.checkAutoIncrement(primaryKeyPolicy, entity.getDbLinkId(), visualTables);
                }
            }
        }
        return this.updateById(entity);
    }

    @Override
    public boolean getObjByEncode(String encode, Integer type) {
        return this.baseMapper.getObjByEncode(encode, type);
    }

    @Override
    public void setAutoEnCode(VisualdevEntity entity) {
        if (Objects.equals(1, entity.getType())) {
            entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.ZXBD), code -> this.getObjByEncode(code, entity.getType())));
        } else {
            entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.BDHC), code -> this.getObjByEncode(code, entity.getType())));
        }
    }

    @Override
    public boolean getCountByName(String name, Integer type, String systemId) {
        return this.baseMapper.getCountByName(name, type, systemId);
    }

    @Override
    public void createTable(VisualdevEntity entity) throws WorkFlowException, SQLException {
        FormDataModel formDataModel = JsonUtil.getJsonToBean(entity.getFormData(), FormDataModel.class);
        //是否开启安全锁
        Boolean concurrencyLock = formDataModel.getConcurrencyLock();
        int primaryKeyPolicy = formDataModel.getPrimaryKeyPolicy();
        Boolean logicalDelete = formDataModel.getLogicalDelete();
        String dbLinkId = entity.getDbLinkId();

        Map<String, Object> formMap = JsonUtil.stringToMap(entity.getFormData());
        List<FieLdsModel> list = JsonUtil.getJsonToList(formMap.get(FIELDS), FieLdsModel.class);
        JSONArray formJsonArray = JsonUtil.getJsonToJsonArray(String.valueOf(formMap.get(FIELDS)));
        List<TableModel> visualTables = JsonUtil.getJsonToList(entity.getVisualTables(), TableModel.class);

        List<FormAllModel> formAllModel = new ArrayList<>();
        RecursionForm recursionForm = new RecursionForm();
        recursionForm.setTableModelList(visualTables);
        recursionForm.setList(list);
        FormCloumnUtil.recursionForm(recursionForm, formAllModel);

        String tableName = "mt" + RandomUtil.uuId();
        if (StringUtil.isNotEmpty(formDataModel.getTableName())) {
            tableName = formDataModel.getTableName();
            visualDevTableCreUtil.checkName(tableName, dbLinkId);
        }

        VisualTableModel model = VisualTableModel.builder()
                .jsonArray(formJsonArray)
                .formAllModel(formAllModel)
                .table(tableName)
                .linkId(dbLinkId)
                .fullName(entity.getFullName())
                .concurrency(concurrencyLock)
                .primaryKey(primaryKeyPolicy)
                .logicalDelete(logicalDelete)
                .build();
        List<TableModel> tableModelList = visualDevTableCreUtil.tableList(model);
        formMap.put(FIELDS, formJsonArray);
        //更新
        entity.setFormData(JsonUtil.getObjectToString(formMap));
        entity.setVisualTables(JsonUtil.getObjectToString(tableModelList));

        //生成表的时候列表字段信息回填
        VisualDevTableCre.setFieldTable(entity, tableModelList, model.getTableNameList());
    }

    @Override
    public boolean getPrimaryDbField(String linkId, String table) {
        DbTableFieldModel dbTableModel;
        try {
            dbTableModel = dbTableService.getDbTableModel(linkId, table);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new DataException(MsgCode.LOG110.get());
        }
        List<DbFieldModel> data = dbTableModel.getDbFieldModelList();
        DbFieldModel dbFieldModel = data.stream().filter(DbFieldModel::getIsPrimaryKey).findFirst().orElse(null);
        if (dbFieldModel != null) {
            return dbFieldModel.getIsAutoIncrement() != null && dbFieldModel.getIsAutoIncrement();
        } else {
            return false;
        }
    }

    @Override
    public List<VisualdevEntity> selectorList(String systemId) {
        return this.baseMapper.selectorList(systemId);
    }

    @Override
    public List<TableFields> storedFieldList(VisualdevEntity entity) {
        List<TableFields> resultList = new ArrayList<>();
        if (entity != null) {
            // 是否存在关联数据库
            try {
                DbLinkEntity linkEntity = null;
                if (StringUtil.isNotEmpty(entity.getDbLinkId())) {
                    linkEntity = dblinkService.getInfo(entity.getDbLinkId());
                }
                //获取主表
                List<TableModel> listTable = JsonUtil.getJsonToList(entity.getVisualTables(), TableModel.class);
                String mainTable = listTable.stream().filter(t -> "1".equals(t.getTypeId())).findFirst().orElse(new TableModel()).getTable();
                //获取主键
                String pKeyName = VisualUtils.getpKey(linkEntity, mainTable);
                resultList.add(new TableFields(pKeyName, "表单主键"));
                FormDataModel formData = JsonUtil.getJsonToBean(entity.getFormData(), FormDataModel.class);
                List<FieLdsModel> list = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
                List<FormAllModel> formAllModel = new ArrayList<>();
                RecursionForm recursionForm = new RecursionForm();
                recursionForm.setTableModelList(JsonUtil.getJsonToList(entity.getVisualTables(), TableModel.class));
                recursionForm.setList(list);
                FormCloumnUtil.recursionForm(recursionForm, formAllModel);
                List<FormAllModel> mast = formAllModel.stream().filter(t -> FormEnum.MAST.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
                //列表子表数据
                List<FormAllModel> mastTable = formAllModel.stream().filter(t -> FormEnum.MAST_TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
                for (FormAllModel item : mast) {
                    FieLdsModel fieLdsModel = item.getFormColumnModel().getFieLdsModel();
                    addField(fieLdsModel.getVModel(), fieLdsModel, resultList);
                }
                for (FormAllModel item : mastTable) {
                    FormMastTableModel formMastTableModel = item.getFormMastTableModel();
                    FieLdsModel fieLdsModel = formMastTableModel.getMastTable().getFieLdsModel();
                    addField(formMastTableModel.getVModel(), fieLdsModel, resultList);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return resultList;
    }

    /**
     * 添加字段（统一过滤条件）
     *
     * @param realVmodel
     * @param fieLdsModel
     * @param resultList
     */
    private static void addField(String realVmodel, FieLdsModel fieLdsModel, List<TableFields> resultList) {
        List<String> list = Arrays.asList(JnpfKeyConsts.COM_INPUT, JnpfKeyConsts.BILLRULE);
        if (StringUtil.isNotEmpty(fieLdsModel.getVModel()) && list.contains(fieLdsModel.getConfig().getJnpfKey())) {
            resultList.add(new TableFields(realVmodel + JnpfConst.FIELD_SUFFIX_JNPFID, fieLdsModel.getConfig().getLabel()));
        }
    }

    @Override
    @Async
    public void initFlowState(VisualdevEntity entity) {
        DbLinkEntity linkEntity = dblinkService.getInfo(entity.getDbLinkId());
        List<TableModel> visualTables = JsonUtil.getJsonToList(entity.getVisualTables(), TableModel.class);
        TableModel mainTable = visualTables.stream().filter(t -> "1".equals(t.getTypeId())).findFirst().orElse(null);
        if (mainTable == null) {
            return;
        }
        SqlTable sqlTable = SqlTable.of(mainTable.getTable());
        String flowTaskIdKey = TableFeildsEnum.FLOWTASKID.getField();
        List<String> flowTaskIdList = new ArrayList<>();
        try {
            DynamicDataSourceUtil.switchToDataSource(linkEntity);
            @Cleanup Connection connection = ConnUtil.getConnOrDefault(linkEntity);
            String databaseProductName = connection.getMetaData().getDatabaseProductName().trim();
            boolean isUpdate = databaseProductName.equalsIgnoreCase("oracle") || databaseProductName.equalsIgnoreCase("DM DBMS");

            flowTaskIdKey = isUpdate ? TableFeildsEnum.FLOWTASKID.getField().toUpperCase() : TableFeildsEnum.FLOWTASKID.getField();
            QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder where = SqlBuilder.select(sqlTable.column(flowTaskIdKey)).from(sqlTable)
                    .where(sqlTable.column(TableFeildsEnum.FLOWSTATE.getField()), SqlBuilder.isNull());
            SelectStatementProvider render = where.build().render(RenderingStrategies.MYBATIS3);
            List<Map<String, Object>> dataList = flowFormDataMapper.selectManyMappedRows(render);
            if (dataList.isEmpty()) {
                return;
            }
            //将state为空的全部修改成0
            String finalFlowTaskIdKey = flowTaskIdKey;
            flowTaskIdList = dataList.stream().map(t -> String.valueOf(t.get(finalFlowTaskIdKey))).collect(Collectors.toList());
            List<List<String>> flowTaskIds = Lists.partition(flowTaskIdList, 1000);
            UpdateDSL<UpdateModel> updateModelUpdateDSL = SqlBuilder.update(sqlTable);
            updateModelUpdateDSL.set(sqlTable.column(TableFeildsEnum.FLOWSTATE.getField())).equalTo(0);
            UpdateDSL<UpdateModel>.UpdateWhereBuilder where1 = updateModelUpdateDSL.where();
            for (List<String> item : flowTaskIds) {
                where1.or(sqlTable.column(flowTaskIdKey), SqlBuilder.isIn(item));
            }
            UpdateStatementProvider updateSP = where1.build().render(RenderingStrategies.MYBATIS3);
            flowFormDataMapper.update(updateSP);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new DataException(e.getMessage());
        } finally {
            DynamicDataSourceUtil.clearSwitchDataSource();
        }
        List<TaskEntity> tasks = taskApi.getInfosSubmit(flowTaskIdList.toArray(new String[]{}), TaskEntity::getStatus, TaskEntity::getId);
        try {
            DynamicDataSourceUtil.switchToDataSource(linkEntity);
            //根据不同的流程状态分组修改
            Map<Integer, List<TaskEntity>> collect = tasks.stream().collect(Collectors.groupingBy(TaskEntity::getStatus));
            for (Map.Entry<Integer, List<TaskEntity>> item : collect.entrySet()) {
                Integer status = item.getKey();
                List<TaskEntity> taskList = item.getValue();
                if (!taskList.isEmpty()) {
                    List<String> itemFlowTaskIds = taskList.stream().map(TaskEntity::getId).collect(Collectors.toList());
                    if (itemFlowTaskIds.isEmpty()) {
                        continue;
                    }
                    UpdateDSL<UpdateModel> updateDsl = SqlBuilder.update(sqlTable);
                    updateDsl.set(sqlTable.column(TableFeildsEnum.FLOWSTATE.getField())).equalTo(status);
                    UpdateDSL<UpdateModel>.UpdateWhereBuilder where2 = updateDsl.where();
                    List<List<String>> lists = Lists.partition(itemFlowTaskIds, 1000);
                    for (List<String> stateItem : lists) {
                        where2.or(sqlTable.column(flowTaskIdKey), SqlBuilder.isIn(stateItem));
                    }
                    UpdateStatementProvider updateSP1 = where2.build().render(RenderingStrategies.MYBATIS3);
                    flowFormDataMapper.update(updateSP1);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            DynamicDataSourceUtil.clearSwitchDataSource();
        }
    }

    @Override
    public List<VisualdevEntity> getListBySystem(PaginationVisualdev paginationVisualdev) {
        return visualdevReleaseMapper.getListBySystem(paginationVisualdev);
    }

    @Override
    public List<VisualExportVo> getExportList(String systemId) {
        List<VisualdevEntity> list = this.baseMapper.getListBySystemId(systemId);
        List<VisualExportVo> voList = new ArrayList<>();
        for (VisualdevEntity item : list) {
            VisualExportVo vo = JsonUtil.getJsonToBean(item, VisualExportVo.class);
            List<VisualAliasEntity> aliasEntityList = visualAliasMapper.getList(item.getId());
            vo.setAliasListJson(JsonUtil.getObjectToString(aliasEntityList));
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public void deleteBySystemId(String systemId) {
        List<VisualdevEntity> visList = this.baseMapper.getListBySystemId(systemId);
        for (VisualdevEntity entity : visList) {
            List<VisualAliasEntity> list = visualAliasMapper.getList(entity.getId());
            visualAliasMapper.deleteByIds(list);
            this.baseMapper.deleteById(entity);
        }
    }
}
