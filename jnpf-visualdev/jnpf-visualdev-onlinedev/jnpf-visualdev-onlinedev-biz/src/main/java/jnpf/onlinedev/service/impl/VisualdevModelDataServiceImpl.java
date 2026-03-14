package jnpf.onlinedev.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.page.PageMethod;
import com.google.common.collect.Lists;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.base.entity.ModuleEntity;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.entity.VisualdevReleaseEntity;
import jnpf.base.mapper.FlowFormDataMapper;
import jnpf.base.model.*;
import jnpf.base.model.flow.DataModel;
import jnpf.base.model.flow.FlowFormDataModel;
import jnpf.base.model.form.ModuleFormModel;
import jnpf.base.model.module.PropertyJsonModel;
import jnpf.base.model.online.VisualdevModelDataInfoVO;
import jnpf.base.service.DbLinkService;
import jnpf.base.service.ModuleService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.VisualdevReleaseService;
import jnpf.base.util.FlowFormDataUtil;
import jnpf.base.util.FormCheckUtils;
import jnpf.base.util.FormPublicUtils;
import jnpf.constant.JnpfConst;
import jnpf.constant.KeyConst;
import jnpf.constant.MsgCode;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.model.query.SuperJsonModel;
import jnpf.database.model.query.SuperQueryJsonModel;
import jnpf.database.util.ConnUtil;
import jnpf.database.util.DynamicDataSourceUtil;
import jnpf.exception.DataException;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.TaskEntity;
import jnpf.model.OnlineDevData;
import jnpf.model.visualjson.*;
import jnpf.model.visualjson.analysis.FormAllModel;
import jnpf.model.visualjson.analysis.RecursionForm;
import jnpf.onlinedev.entity.VisualdevModelDataEntity;
import jnpf.onlinedev.mapper.VisualdevModelDataMapper;
import jnpf.onlinedev.model.*;
import jnpf.onlinedev.model.log.VisualLogForm;
import jnpf.onlinedev.model.online.VisualColumnSearchVO;
import jnpf.onlinedev.service.VisualDevInfoService;
import jnpf.onlinedev.service.VisualDevListService;
import jnpf.onlinedev.service.VisualLogService;
import jnpf.onlinedev.service.VisualdevModelDataService;
import jnpf.onlinedev.util.OnlineProductSqlUtils;
import jnpf.onlinedev.util.OnlinePublicUtils;
import jnpf.onlinedev.util.OnlineSwapDataUtils;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.model.authorize.OnlineDynamicSqlModel;
import jnpf.permission.service.UserService;
import jnpf.permissions.PermissionInterfaceImpl;
import jnpf.util.*;
import jnpf.util.context.RequestContext;
import jnpf.util.visiual.JnpfKeyConsts;
import jnpf.workflow.service.WorkFlowApi;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.dynamic.sql.*;
import org.mybatis.dynamic.sql.delete.DeleteDSL;
import org.mybatis.dynamic.sql.delete.DeleteModel;
import org.mybatis.dynamic.sql.render.RenderingStrategies;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.select.SelectModel;
import org.mybatis.dynamic.sql.select.join.EqualTo;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
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
public class VisualdevModelDataServiceImpl extends SuperServiceImpl<VisualdevModelDataMapper, VisualdevModelDataEntity> implements VisualdevModelDataService {

    private final UserService userApi;
    private final DbLinkService dblinkService;
    private final VisualdevReleaseService visualdevReleaseService;
    private final VisualDevListService visualDevListService;
    private final OnlineSwapDataUtils onlineSwapDataUtils;
    private final FlowFormDataUtil flowFormDataUtil;
    private final FormCheckUtils formCheckUtils;
    private final WorkFlowApi workFlowApi;
    private final ModuleService moduleService;
    private final VisualLogService visualLogService;
    private final VisualDevInfoService visualDevInfoService;
    private final FlowFormDataMapper flowFormDataMapper;


    @Override
    public List<VisualdevModelDataEntity> getList(String modelId) {
        QueryWrapper<VisualdevModelDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualdevModelDataEntity::getVisualDevId, modelId);
        return this.list(queryWrapper);
    }

    /**
     * 表单字段
     *
     * @param id
     * @param filterType 过滤类型，0或者不传为默认过滤子表和关联表单，1-弹窗配置需要过滤掉的类型
     * @return
     */
    @Override
    public List<FormDataField> fieldList(String id, Integer filterType) {
        VisualdevReleaseEntity entity = visualdevReleaseService.getById(id);
        FormDataModel formData = JsonUtil.getJsonToBean(entity.getFormData(), FormDataModel.class);

        List<FieLdsModel> fieLdsModelList = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
        List<FieLdsModel> mainFieldModelList = new ArrayList<>();

        OnlinePublicUtils.recursionFields(mainFieldModelList, fieLdsModelList);
        //过滤掉无法传递的组件
        List<String> notInList = new ArrayList<>();
        notInList.add(JnpfKeyConsts.RELATIONFORM);
        notInList.add(JnpfKeyConsts.CHILD_TABLE);
        if (Objects.equals(filterType, 1)) {
            notInList.add("link");
            notInList.add("button");
            notInList.add("JNPFText");
            notInList.add("alert");
            notInList.add(JnpfKeyConsts.POPUPSELECT);
            notInList.add(JnpfKeyConsts.QR_CODE);
            notInList.add(JnpfKeyConsts.BARCODE);
            notInList.add(JnpfKeyConsts.CREATEUSER);
            notInList.add(JnpfKeyConsts.CREATETIME);
            notInList.add(JnpfKeyConsts.UPLOADIMG);
            notInList.add(JnpfKeyConsts.UPLOADFZ);
            notInList.add(JnpfKeyConsts.MODIFYUSER);
            notInList.add(JnpfKeyConsts.MODIFYTIME);

            notInList.add(JnpfKeyConsts.CURRORGANIZE);
            notInList.add(JnpfKeyConsts.CURRPOSITION);
            notInList.add(JnpfKeyConsts.IFRAME);
            notInList.add(JnpfKeyConsts.RELATIONFORM_ATTR);
            notInList.add(JnpfKeyConsts.POPUPSELECT_ATTR);
        }

        return mainFieldModelList.stream()
                .filter(fieLdsModel ->
                        !"".equals(fieLdsModel.getVModel())
                                && StringUtil.isNotEmpty(fieLdsModel.getVModel())
                                && !notInList.contains(fieLdsModel.getConfig().getJnpfKey()))
                .map(fieLdsModel -> {
                    FormDataField formDataField = new FormDataField();
                    formDataField.setLabel(fieLdsModel.getConfig().getLabel());
                    formDataField.setVModel(fieLdsModel.getVModel());
                    return formDataField;
                }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getPageList(VisualdevEntity entity, PaginationModel paginationModel) {
        VisualDevJsonModel visualJsonModel = OnlinePublicUtils.getVisualJsonModel(entity);
        //判断请求客户端来源
        if (!RequestContext.isOrignPc()) {
            visualJsonModel.setColumnData(visualJsonModel.getAppColumnData());
        }
        return visualDevListService.getRelationFormList(visualJsonModel, paginationModel);
    }

    @Override
    public List<Map<String, Object>> exportData(String[] keys, PaginationModelExport paginationModelExport, VisualDevJsonModel visualDevJsonModel) {
        PaginationModel paginationModel = new PaginationModel();
        BeanUtil.copyProperties(paginationModelExport, paginationModel);
        List<String> keyList = Arrays.asList(keys);
        List<Map<String, Object>> noSwapDataList = new ArrayList<>();
        ColumnDataModel columnDataModel = visualDevJsonModel.getColumnData();
        List<VisualColumnSearchVO> searchVOList;
        List<TableModel> visualTables = visualDevJsonModel.getVisualTables();
        //解析控件
        FormDataModel formDataModel = visualDevJsonModel.getFormData();
        List<FieLdsModel> fieLdsModels = JsonUtil.getJsonToList(formDataModel.getFields(), FieLdsModel.class);
        RecursionForm recursionForm = new RecursionForm(fieLdsModels, visualTables);
        List<FormAllModel> formAllModel = new ArrayList<>();
        FormCloumnUtil.recursionForm(recursionForm, formAllModel);
        //封装查询条件
        if (StringUtil.isNotEmpty(paginationModel.getQueryJson())) {
            Map<String, Object> keyJsonMap = JsonUtil.stringToMap(paginationModel.getQueryJson());
            searchVOList = JsonUtil.getJsonToList(columnDataModel.getSearchList(), VisualColumnSearchVO.class);
            searchVOList = searchVOList.stream().map(searchVO -> {
                searchVO.setValue(keyJsonMap.get(searchVO.getId()));
                return searchVO;
            }).filter(vo -> vo.getValue() != null && StringUtil.isNotEmpty(String.valueOf(vo.getValue()))).collect(Collectors.toList());
            //左侧树查询
            boolean b = false;
            if (columnDataModel.getTreeRelation() != null) {
                b = keyJsonMap.keySet().stream().anyMatch(t -> t.equalsIgnoreCase(String.valueOf(columnDataModel.getTreeRelation())));
            }
            if (b && keyJsonMap.size() > searchVOList.size()) {
                String relation = columnDataModel.getTreeRelation();
                VisualColumnSearchVO vo = new VisualColumnSearchVO();
                vo.setSearchType("1");
                vo.setVModel(relation);
                vo.setValue(keyJsonMap.get(relation));
                searchVOList.add(vo);
            }
        }
        //菜单id
        String menuId = paginationModel.getMenuId();
        //菜单判断是否启用流程-添加流程状态
        ModuleEntity info = moduleService.getInfo(menuId);
        if (info != null) {
            //流程菜单
            boolean enableFlow = Objects.equals(info.getType(), 9);
            visualDevJsonModel.setEnableFlow(enableFlow);
            if (enableFlow) {
                PropertyJsonModel model = JsonUtil.getJsonToBean(info.getPropertyJson(), PropertyJsonModel.class);
                List<String> flowVersionIds = workFlowApi.getFlowIdsByTemplateId(model.getModuleId());
                visualDevJsonModel.setFlowId(model.getModuleId());
                visualDevJsonModel.setFlowVersionIds(flowVersionIds);
            }
        }

        if (visualDevJsonModel.getVisualTables().size() > 0) {
            //当前用户信息
            UserInfo userInfo = UserProvider.getUser();
            //封装搜索数据
            OnlineProductSqlUtils.queryList(formAllModel, visualDevJsonModel, paginationModel);
            noSwapDataList = visualDevListService.getListWithTable(visualDevJsonModel, paginationModel, userInfo, keyList);
            //数据转换
            List<FieLdsModel> fields = new ArrayList<>();
            OnlinePublicUtils.recursionFields(fields, fieLdsModels);
            noSwapDataList = onlineSwapDataUtils.getSwapList(noSwapDataList, fields, visualDevJsonModel.getId(), false);
        }
        return noSwapDataList;
    }


    @Override
    public VisualdevModelDataEntity getInfo(String id) {
        QueryWrapper<VisualdevModelDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualdevModelDataEntity::getId, id);
        return this.getOne(queryWrapper);
    }

    @Override
    public DataModel visualCreate(VisualParamModel visualParamModel) throws WorkFlowException {
        VisualdevEntity visualdevEntity = visualParamModel.getVisualdevEntity();
        Map<String, Object> map = visualParamModel.getData();
        boolean isLink = visualParamModel.getIsLink();
        boolean isUpload = visualParamModel.getIsUpload();
        FormDataModel formData = JsonUtil.getJsonToBean(visualdevEntity.getFormData(), FormDataModel.class);
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(visualdevEntity.getColumnData(), ColumnDataModel.class);
        boolean needP = false;
        List<String> formPerList = new ArrayList<>();
        if (columnDataModel != null && StringUtil.isNotEmpty(visualParamModel.getMenuId())) {
            needP = columnDataModel.getUseFormPermission();
            Map<String, Object> pMap = PermissionInterfaceImpl.getFormMap();
            if (pMap.get(visualParamModel.getMenuId()) != null) {
                formPerList = JsonUtil.getJsonToList(pMap.get(visualParamModel.getMenuId()), ModuleFormModel.class).stream()
                        .map(ModuleFormModel::getEnCode).collect(Collectors.toList());
            }
        }
        List<FieLdsModel> list = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
        List<TableModel> tableModels = JsonUtil.getJsonToList(visualdevEntity.getVisualTables(), TableModel.class);
        DbLinkEntity linkEntity = StringUtil.isNotEmpty(visualdevEntity.getDbLinkId()) ? dblinkService.getInfo(visualdevEntity.getDbLinkId()) : null;

        //递归遍历模板
        RecursionForm recursionForm = new RecursionForm(list, tableModels);
        List<FormAllModel> formAllModel = new ArrayList<>();
        FormCloumnUtil.recursionForm(recursionForm, formAllModel);
        //是否开启并发锁
        boolean concurrency = false;
        Integer primaryKeyPolicy = formData.getPrimaryKeyPolicy();
        if (Boolean.TRUE.equals(formData.getConcurrencyLock())) {
            //初始化version值
            map.put(TableFeildsEnum.VERSION.getField(), 0);
            concurrency = true;
        }
        OnlineSwapDataUtils.swapDatetime(list, map);
        //单行唯一校验
        if (!isUpload) {
            CheckFormModel checkFormModel = CheckFormModel.builder().formFieldList(list).dataMap(map).linkEntity(linkEntity).tableModelList(tableModels)
                    .visualdevEntity(visualdevEntity).id(null).isLink(isLink).build();
            String b = formCheckUtils.checkForm(checkFormModel);
            if (StringUtil.isNotEmpty(b)) {
                throw new WorkFlowException(b);
            }
        }
        String mainId = RandomUtil.uuId();
        UserInfo userInfo = UserProvider.getUser();
        UserEntity info = userApi.getInfo(userInfo.getUserId());
        DataModel dataModel = DataModel.builder().visualId(visualdevEntity.getId())
                .dataNewMap(map).fieLdsModelList(list).tableModelList(tableModels).formAllModel(formAllModel)
                .mainId(mainId).link(linkEntity).userEntity(info).concurrencyLock(concurrency)
                .primaryKeyPolicy(primaryKeyPolicy).linkOpen(isLink)
                .needPermission(needP).formPerList(formPerList)
                .build();
        flowFormDataUtil.create(dataModel);

        //数据日志
        if (formData.isDataLog() && !isUpload && !isLink) {
            visualLogService.createEventLog(VisualLogForm.builder().modelId(visualdevEntity.getId()).dataId(dataModel.getMainId()).newData(map).type(0).build());
        }
        return dataModel;
    }

    @Override
    public DataModel visualUpdate(VisualParamModel visualParamModel) throws WorkFlowException {
        VisualdevEntity visualdevEntity = visualParamModel.getVisualdevEntity();
        Map<String, Object> map = visualParamModel.getData();
        String id = visualParamModel.getId();
        boolean isUpload = visualParamModel.getIsUpload();
        boolean onlyUpdate = visualParamModel.getOnlyUpdate();
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(visualdevEntity.getColumnData(), ColumnDataModel.class);
        FormDataModel formData = JsonUtil.getJsonToBean(visualdevEntity.getFormData(), FormDataModel.class);
        boolean needP = false;
        List<String> formPerList = new ArrayList<>();
        if (columnDataModel != null && StringUtil.isNotEmpty(visualParamModel.getMenuId())) {
            needP = columnDataModel.getUseFormPermission();
            Map<String, Object> pMap = PermissionInterfaceImpl.getFormMap();
            if (pMap.get(visualParamModel.getMenuId()) != null) {
                formPerList = JsonUtil.getJsonToList(pMap.get(visualParamModel.getMenuId()), ModuleFormModel.class).stream()
                        .map(ModuleFormModel::getEnCode).collect(Collectors.toList());
            }
        }
        List<FieLdsModel> list = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
        boolean inlineEdit = columnDataModel.getType() != null && columnDataModel.getType() == 4;
        if (inlineEdit && RequestContext.isOrignPc()) {
            list = JsonUtil.getJsonToList(columnDataModel.getColumnList(), FieLdsModel.class);
            list = list.stream().filter(f -> !f.getId().toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)).collect(Collectors.toList());
        }
        List<TableModel> tableModels = JsonUtil.getJsonToList(visualdevEntity.getVisualTables(), TableModel.class);
        TableModel mainT = tableModels.stream().filter(t -> t.getTypeId().equals("1")).findFirst().orElse(null);
        DbLinkEntity linkEntity = StringUtil.isNotEmpty(visualdevEntity.getDbLinkId()) ? dblinkService.getInfo(visualdevEntity.getDbLinkId()) : null;

        //递归遍历模板
        RecursionForm recursionForm = new RecursionForm(list, tableModels);
        List<FormAllModel> formAllModel = new ArrayList<>();
        FormCloumnUtil.recursionForm(recursionForm, formAllModel);
        Integer primaryKeyPolicy = formData.getPrimaryKeyPolicy();
        //是否开启并发锁
        boolean isConcurrencyLock = isConcurrencyLock(formData, map, id, primaryKeyPolicy, mainT, linkEntity);

        OnlineSwapDataUtils.swapDatetime(list, map);
        //单行唯一校验
        if (!isUpload) {
            CheckFormModel checkFormModel = CheckFormModel.builder().formFieldList(list).dataMap(map).linkEntity(linkEntity).tableModelList(tableModels)
                    .visualdevEntity(visualdevEntity).id(id).build();
            String b = formCheckUtils.checkForm(checkFormModel);
            if (StringUtil.isNotEmpty(b)) {
                throw new WorkFlowException(b);
            }
        }

        //数据日志
        VisualdevModelDataInfoVO resOld = visualDevInfoService.getDetailsDataInfo(id, visualdevEntity,
                OnlineInfoModel.builder().needRlationFiled(true).needSwap(false).formAllModel(formAllModel).build());
        Map<String, Object> oldData = JsonUtil.stringToMap(resOld.getData());

        UserInfo userInfo = UserProvider.getUser();
        UserEntity info = userApi.getInfo(userInfo.getUserId());
        DataModel dataModel = DataModel.builder().visualId(visualdevEntity.getId())
                .dataNewMap(map).fieLdsModelList(list).tableModelList(tableModels).formAllModel(formAllModel)
                .mainId(id).link(linkEntity).userEntity(info).concurrencyLock(isConcurrencyLock)
                .primaryKeyPolicy(primaryKeyPolicy).onlyUpdate(onlyUpdate).logicalDelete(formData.getLogicalDelete())
                .needPermission(needP).formPerList(formPerList)
                .build();
        flowFormDataUtil.update(dataModel);
        VisualdevModelDataInfoVO res = visualDevInfoService.getDetailsDataInfo(id, visualdevEntity,
                OnlineInfoModel.builder().needRlationFiled(true).needSwap(false).formAllModel(formAllModel).build());
        Map<String, Object> newData = JsonUtil.stringToMap(res.getData());
        VisualLogForm form = VisualLogForm.builder().modelId(visualdevEntity.getId()).dataId(id).oldData(oldData).newData(newData).type(1).build();
        //处理变更字段信息-任务流程用
        List<VisualLogModel> listLog = new ArrayList<>();
        visualLogService.addLog(form, listLog);
        dataModel.setListLog(listLog);
        //数据日志
        if (formData.isDataLog() && !isUpload) {
            form.setListLog(listLog);
            visualLogService.createEventLog(form);
        }
        return dataModel;
    }

    private boolean isConcurrencyLock(FormDataModel formData, Map<String, Object> map, String id, Integer primaryKeyPolicy, TableModel mainT, DbLinkEntity linkEntity) throws WorkFlowException {
        boolean isConcurrencyLock = false;
        if (Boolean.TRUE.equals(formData.getConcurrencyLock())) {
            if (map.get(TableFeildsEnum.VERSION.getField()) == null) {
                map.put(TableFeildsEnum.VERSION.getField(), 0);
            } else {
                boolean version = true;
                try {
                    Object realId = id;
                    if (Objects.equals(primaryKeyPolicy, 2)) {
                        realId = Long.parseLong(id);
                    }
                    version = flowFormDataUtil.getVersion(mainT.getTable(), linkEntity, map, realId);
                } catch (Exception e) {
                    throw new WorkFlowException(e.getMessage(), e);
                }
                if (!version) {
                    throw new WorkFlowException(MsgCode.VS405.get());
                } else {
                    Integer vs = Integer.valueOf(String.valueOf(map.get(TableFeildsEnum.VERSION.getField())));
                    map.put(TableFeildsEnum.VERSION.getField(), vs + 1);
                }
            }
            isConcurrencyLock = true;
        }
        return isConcurrencyLock;
    }

    @Override
    public void visualDelete(VisualdevEntity visualdevEntity, List<Map<String, Object>> data) throws WorkFlowException {
        VisualDevJsonModel visualJsonModel = OnlinePublicUtils.getVisualJsonModel(visualdevEntity);
        //判断请求客户端来源
        if (!RequestContext.isOrignPc()) {
            visualJsonModel.setColumnData(visualJsonModel.getAppColumnData());
        }
        List<String> idsList = new ArrayList<>();
        StringJoiner errMess = new StringJoiner(",");
        for (Map<String, Object> map : data) {
            String id = String.valueOf(map.get(FlowFormConstant.ID));
            TaskEntity taskEntity = workFlowApi.getInfoSubmit(String.valueOf(map.get(FlowFormConstant.FLOWTASKID)), TaskEntity::getId,
                    TaskEntity::getParentId, TaskEntity::getFullName, TaskEntity::getStatus);
            if (taskEntity != null) {
                try {
                    workFlowApi.delete(taskEntity);
                    idsList.add(id);
                } catch (Exception e) {
                    errMess.add(e.getMessage());
                }
            } else {
                idsList.add(id);
            }
        }
        if (idsList.isEmpty()) {
            throw new WorkFlowException(errMess.toString());
        }
        if (!StringUtil.isEmpty(visualdevEntity.getVisualTables()) && !OnlineDevData.TABLE_CONST.equals(visualdevEntity.getVisualTables())) {
            for (String id : idsList) {
                try {
                    tableDelete(id, visualJsonModel);
                } catch (Exception e) {
                    throw new WorkFlowException(e.getMessage(), e);
                }
            }
        }
    }

    @DSTransactional(rollbackFor = Exception.class)
    @Override
    public void delete(VisualdevModelDataEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
        }
    }

    @Override
    public boolean tableDelete(String id, VisualDevJsonModel visualDevJsonModel) throws SQLException {
        DbLinkEntity linkEntity = dblinkService.getInfo(visualDevJsonModel.getDbLinkId());
        VisualDevJsonModel model = BeanUtil.copyProperties(visualDevJsonModel, VisualDevJsonModel.class);
        return flowFormDataUtil.deleteTable(id, model, linkEntity);
    }

    @Override
    public ActionResult<Object> tableDeleteMore(List<String> ids, VisualDevJsonModel visualDevJsonModel) throws SQLException {
        List<String> dataInfoVOList = new ArrayList<>();
        for (String id : ids) {
            boolean isDel = tableDelete(id, visualDevJsonModel);
            if (isDel) {
                dataInfoVOList.add(id);
            }
        }
        visualDevJsonModel.setDataIdList(dataInfoVOList);
        return ActionResult.success(MsgCode.SU003.get());
    }

    @Override
    @DSTransactional
    public void deleteByTableName(FlowFormDataModel model) throws WorkFlowException, SQLException {
        String tableName = model.getTableName();
        List<Map<String, Object>> ruleList = model.getRuleList();
        String realTableName = tableName;
        VisualdevReleaseEntity entity = visualdevReleaseService.getById(model.getFormId());
        List<TableModel> tableModels = JsonUtil.getJsonToList(entity.getVisualTables(), TableModel.class);
        FormDataModel formData = JsonUtil.getJsonToBean(entity.getFormData(), FormDataModel.class);
        DbLinkEntity linkEntity = StringUtil.isNotEmpty(entity.getDbLinkId()) ? dblinkService.getInfo(entity.getDbLinkId()) : null;
        List<FieLdsModel> fieLdsModels = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
        List<FieLdsModel> fields = new ArrayList<>();
        OnlinePublicUtils.recursionFields(fields, fieLdsModels);
        if (StringUtil.isNotEmpty(tableName) && tableName.toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
            FieLdsModel fieLdsModel = fields.stream().filter(t -> t.getVModel().equalsIgnoreCase(tableName)).findFirst().orElse(null);
            if (fieLdsModel != null) {
                realTableName = fieLdsModel.getConfig().getTableName();
            }
        }
        String finalRealTableName = realTableName;
        TableModel tableModel = tableModels.stream().filter(t -> t.getTable().equals(finalRealTableName)).findFirst().orElse(null);
        if (tableModel == null) {
            throw new DataException("表不存在");
        }
        List<OnlineDynamicSqlModel> sqlModelList = new ArrayList<>();
        OnlineDynamicSqlModel sqlModel = new OnlineDynamicSqlModel();
        SqlTable sqlTable = SqlTable.of(tableModel.getTable());
        sqlModel.setSqlTable(sqlTable);
        sqlModel.setTableName(tableModel.getTable());
        sqlModelList.add(sqlModel);
        SuperJsonModel ruleJsonModel = null;
        //组装查询条件
        if (ObjectUtil.isNotEmpty(ruleList)) {
            ruleJsonModel = new SuperJsonModel();
            ruleJsonModel.setMatchLogic(model.getRuleMatchLogic());
            List<SuperQueryJsonModel> superQueryJsonModelList = new ArrayList<>();
            for (Object obj : ruleList) {
                SuperQueryJsonModel ruleQueryModel = JsonUtil.getJsonToBean(obj, SuperQueryJsonModel.class);
                List<FieLdsModel> groups = ruleQueryModel.getGroups();
                if (ObjectUtil.isNotEmpty(groups)) {
                    groups.stream().forEach(group -> {
                        if (group.getId().toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                            group.setVModel(group.getId().split("-")[1]);
                        }
                    });
                }
                superQueryJsonModelList.add(ruleQueryModel);
            }
            ruleJsonModel.setConditionList(superQueryJsonModelList);
        }
        DynamicDataSourceUtil.switchToDataSource(linkEntity);
        try {
            @Cleanup Connection conn = ConnUtil.getConnOrDefault(linkEntity);
            String dbType = conn.getMetaData().getDatabaseProductName().trim();
            String pkeyId = flowFormDataUtil.getKey(tableModel, dbType);
            List<Object> idStringList = new ArrayList<>();
            if (ObjectUtil.isNotEmpty(ruleJsonModel)) {
                QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder where = SqlBuilder.selectDistinct(sqlTable.column(pkeyId)).from(sqlTable).where();
                OnlineProductSqlUtils.getSuperSql(where, ruleJsonModel, sqlModelList, dbType, null, false);
                List<Map<String, Object>> dataList = flowFormDataMapper.selectManyMappedRows(where.build().render(RenderingStrategies.MYBATIS3));
                idStringList = dataList.stream().map(m -> m.get(pkeyId)).distinct().collect(Collectors.toList());
                if (ObjectUtil.isEmpty(idStringList)) {
                    idStringList.add("nodata");
                }
            }
            DeleteDSL<DeleteModel>.DeleteWhereBuilder whereD = SqlBuilder.deleteFrom(SqlTable.of(tableModel.getTable())).where();
            if (ObjectUtil.isNotEmpty(idStringList)) {
                whereD.and(sqlTable.column(pkeyId), SqlBuilder.isIn(idStringList));
            }
            flowFormDataMapper.delete(whereD.build().render(RenderingStrategies.MYBATIS3));
        } catch (Exception e) {
            e.printStackTrace();
            throw new WorkFlowException(MsgCode.FA103.get(), e.getMessage());
        } finally {
            DynamicDataSourceUtil.clearSwitchDataSource();
        }
    }

    @Override
    public List<Map<String, Object>> relationQuery(VisualdevEntity entity, RelationQuery query) {
        VisualDevJsonModel visualDevJsonModel = OnlinePublicUtils.getVisualJsonModel(entity);
        FormDataModel formData = visualDevJsonModel.getFormData();
        boolean logicalDelete = visualDevJsonModel.getFormData().getLogicalDelete();
        List<String> collect = StringUtil.isNotEmpty(query.getColumnOptions()) ? Arrays.asList(query.getColumnOptions().split(",")) : new ArrayList<>();
        List<FieLdsModel> fieLdsModels = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
        List<FieLdsModel> allFields = new ArrayList<>();
        List<TableModel> tableModelList = JsonUtil.getJsonToList(visualDevJsonModel.getVisualTables(), TableModel.class);
        OnlinePublicUtils.recursionFields(allFields, fieLdsModels);
        List<Map<String, Object>> resList = new ArrayList<>();
        List<Map<String, Object>> dataList;
        //判断有无表
        if (visualDevJsonModel.getVisualTables().size() > 0) {
            try {
                //主表
                TableModel mainTable = tableModelList.stream().filter(t -> t.getTypeId().equals("1")).findFirst().orElse(null);
                DbLinkEntity linkEntity = dblinkService.getInfo(visualDevJsonModel.getDbLinkId());
                List<OnlineDynamicSqlModel> sqlModelList = new ArrayList<>();
                //根据表字段创建sqltable
                for (TableModel model : tableModelList) {
                    OnlineDynamicSqlModel sqlModel = new OnlineDynamicSqlModel();
                    sqlModel.setSqlTable(SqlTable.of(model.getTable()));
                    sqlModel.setTableName(model.getTable());
                    if (model.getTypeId().equals("1")) {
                        sqlModel.setMain(true);
                    } else {
                        sqlModel.setForeign(model.getTableField());
                        sqlModel.setRelationKey(model.getRelationField());
                        sqlModel.setMain(false);
                    }
                    sqlModelList.add(sqlModel);
                }
                //判断是否分页
                boolean isPage = query.getPageSize() != null && query.getPageSize() < 1000;
                //获取表单主表副表全字段
                List<String> allFieldsKey = new ArrayList<>();
                for (FieLdsModel item : allFields) {
                    if (StringUtil.isNotEmpty(item.getVModel()) && collect.contains(item.getVModel())) {
                        allFieldsKey.add(item.getVModel());
                    }
                }
                OnlineProductSqlUtils.getColumnListSql(sqlModelList, visualDevJsonModel, allFieldsKey, linkEntity);
                //高级查询
                RecursionForm recursionForm = new RecursionForm(fieLdsModels, tableModelList);
                List<FormAllModel> formAllModel = new ArrayList<>();
                FormCloumnUtil.recursionForm(recursionForm, formAllModel);
                PaginationModel paginationModel = JsonUtil.getJsonToBean(query, PaginationModel.class);
                OnlineProductSqlUtils.queryList(formAllModel, visualDevJsonModel, paginationModel);
                SuperJsonModel superQuery = visualDevJsonModel.getSuperQuery();
                List<SuperJsonModel> listquery = Arrays.asList(superQuery);
                DynamicDataSourceUtil.switchToDataSource(linkEntity);
                @Cleanup Connection connection = ConnUtil.getConnOrDefault(linkEntity);
                String databaseProductName = connection.getMetaData().getDatabaseProductName().trim();

                String pkeyId = flowFormDataUtil.getKey(mainTable, databaseProductName);
                visualDevJsonModel.setPkeyId(pkeyId);
                List<FieLdsModel> jsonToList = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
                //递归处理控件
                List<FieLdsModel> allFieLds = new ArrayList<>();
                OnlinePublicUtils.recursionFields(allFieLds, jsonToList);
                Map<String, String> tableFieldAndTableName = new HashMap<>(8);
                Map<String, String> tableNameAndTableField = new HashMap<>(8);
                allFields.stream().filter(f -> f.getConfig().getJnpfKey().equals(JnpfKeyConsts.CHILD_TABLE)).forEach(f -> {
                    tableFieldAndTableName.put(f.getVModel(), f.getConfig().getTableName());
                    tableNameAndTableField.put(f.getConfig().getTableName(), f.getVModel());
                });
                //取出所有有用到的表-进行关联过滤
                List<String> allTableName = OnlinePublicUtils.getAllTableName(new ArrayList<>(), listquery, tableFieldAndTableName);
                for (String str : collect) {
                    if (str.toLowerCase().startsWith(JnpfConst.SIDE_MARK_PRE)) {
                        allTableName.add(str.split(JnpfConst.SIDE_MARK)[0].substring(5));
                    } else if (str.toLowerCase().startsWith(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                        allTableName.add(tableFieldAndTableName.get(str.split("-")[0]));
                    }
                }
                //主表
                OnlineDynamicSqlModel mainSqlModel = sqlModelList.stream().filter(OnlineDynamicSqlModel::isMain).findFirst().orElse(null);
                //非主表
                List<OnlineDynamicSqlModel> dycList = sqlModelList.stream().filter(dyc -> !dyc.isMain()).collect(Collectors.toList());

                List<BasicColumn> sqlColumns = new ArrayList<>();
                Map<String, String> aliasMap = new HashMap<>();
                boolean isOracle = databaseProductName.equalsIgnoreCase("oracle");
                boolean isDm = databaseProductName.equalsIgnoreCase("DM DBMS");
                boolean isUp = databaseProductName.equalsIgnoreCase("oracle") || databaseProductName.equalsIgnoreCase("DM DBMS");
                String flowStateKey = isUp ? TableFeildsEnum.FLOWSTATE.getField().toUpperCase() : TableFeildsEnum.FLOWSTATE.getField();
                String flowIdKey = isUp ? TableFeildsEnum.FLOWID.getField().toUpperCase() : TableFeildsEnum.FLOWID.getField();
                String deleteMarkKey = isUp ? TableFeildsEnum.DELETEMARK.getField().toUpperCase() : TableFeildsEnum.DELETEMARK.getField();
                for (OnlineDynamicSqlModel dynamicSqlModel : sqlModelList) {
                    List<BasicColumn> basicColumns = Optional.ofNullable(dynamicSqlModel.getColumns()).orElse(new ArrayList<>());
                    //达梦或者oracle 别名太长转换-底下有方法进行还原
                    if (isOracle || isDm) {
                        for (int i = 0; i < basicColumns.size(); i++) {
                            BasicColumn item = basicColumns.get(i);
                            String alias = item.alias().orElse(null);
                            if (StringUtil.isNotEmpty(alias)) {
                                String aliasNewName = "A" + RandomUtil.uuId();
                                aliasMap.put(aliasNewName, alias);
                                basicColumns.set(i, item.as(aliasNewName));
                            }
                        }
                    }
                    sqlColumns.addAll(basicColumns);
                }
                if (visualDevJsonModel.isEnableFlow()) {
                    sqlColumns.add(mainSqlModel.getSqlTable().column(flowStateKey).as(FlowFormConstant.FLOW_STATE));
                }
                QueryExpressionDSL<SelectModel> from = SqlBuilder.select(sqlColumns).from(mainSqlModel.getSqlTable());
                QueryExpressionDSL<SelectModel> subFrom = SqlBuilder.select(mainSqlModel.getSqlTable().column(pkeyId)).from(mainSqlModel.getSqlTable());

                // 构造table和table下字段的分组
                Map<String, List<String>> tableFieldGroup = new HashMap<>(8);
                allFieLds.forEach(f -> tableFieldGroup.computeIfAbsent(f.getConfig().getTableName(), k -> new ArrayList<>()).add(
                        "table".equals(f.getConfig().getType()) ? f.getConfig().getTableName() : f.getVModel()));
                Map<String, SqlTable> subSqlTableMap = new HashMap<>();
                if (dycList.size() > 0) {
                    for (OnlineDynamicSqlModel sqlModel : dycList) {
                        if (!allTableName.contains(sqlModel.getTableName())) continue;
                        String relationKey = sqlModel.getRelationKey();

                        boolean relationKeyTypeString = false;
                        for (TableModel item : tableModelList) {
                            if (item.getTable().equals(sqlModel.getTableName())) {
                                String foreign = sqlModel.getForeign();
                                TableFields thisfield = item.getFields().stream().filter(t -> t.getField().equals(foreign)).findFirst().orElse(null);
                                if (thisfield != null) {
                                    relationKeyTypeString = thisfield.getDataType().toLowerCase().contains(KeyConst.VARCHAR);
                                }
                            }
                        }
                        //postgresql自增  int和varchar无法对比-添加以下判断
                        if (Objects.equals(formData.getPrimaryKeyPolicy(), 2) && "PostgreSQL".equalsIgnoreCase(databaseProductName) && relationKeyTypeString) {
                            relationKey += "::varchar";
                        }
                        //移除子表的外联--用于查询主副表字段数据
                        if (!tableNameAndTableField.containsKey(sqlModel.getTableName())) {
                            from.leftJoin(sqlModel.getSqlTable())
                                    .on(sqlModel.getSqlTable().column(sqlModel.getForeign()), new EqualTo(mainSqlModel.getSqlTable().column(relationKey)));
                        }

                        //用于查询拼接各条件之后的主表id列表（和统计数量）
                        subFrom.leftJoin(sqlModel.getSqlTable())
                                .on(sqlModel.getSqlTable().column(sqlModel.getForeign()), new EqualTo(mainSqlModel.getSqlTable().column(relationKey)));
                        String tableName = sqlModel.getTableName();
                        List<String> fieldList = tableFieldGroup.get(tableName);
                        if (fieldList != null) {
                            fieldList.forEach(fieldKey -> subSqlTableMap.put(fieldKey, sqlModel.getSqlTable()));
                        }
                    }
                }

                QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder where = from.where();
                QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder subWhere = subFrom.where();

                //逻辑删除不展示
                if (logicalDelete) {
                    subWhere.and(mainSqlModel.getSqlTable().column(deleteMarkKey), SqlBuilder.isNull());
                }
                //非流程数据
                subWhere.and(mainSqlModel.getSqlTable().column(flowIdKey), SqlBuilder.isNull());

                //高级查询
                OnlineProductSqlUtils.getSuperSql(subWhere, superQuery, sqlModelList, databaseProductName, null, false);

                //统计语句
                BasicColumn countColumn;
                if (dycList.size() > 0) {
                    countColumn = SqlBuilder.countDistinct(mainSqlModel.getSqlTable().column(pkeyId));
                } else {
                    countColumn = SqlBuilder.count(mainSqlModel.getSqlTable().column(pkeyId));
                }
                QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder countSelect = SqlBuilder
                        .select(countColumn)
                        .from(subWhere, "tmp")
                        .where();
                SelectStatementProvider renderCount = countSelect.build().render(RenderingStrategies.MYBATIS3);
                long count = flowFormDataMapper.count(renderCount);
                if (count == 0) {
                    paginationModel.setTotal(0);
                    return new ArrayList<>();
                }

                //排序 -- 是流程先按状态排序
                List<SortSpecification> sidxList = new ArrayList<>();
                if (StringUtil.isNotEmpty(paginationModel.getSidx())) {
                    String[] split = paginationModel.getSidx().split(",");
                    for (String sidx : split) {
                        //目前只支持主表排序
                        if (sidx.toLowerCase().contains(JnpfConst.SIDE_MARK) || sidx.toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                            continue;
                        }
                        SortSpecification sortSpecification;
                        if (sidx.startsWith("-")) {
                            sortSpecification = SqlBuilder.sortColumn(mainTable.getTable(), SqlTable.of(mainTable.getTable()).column(sidx.substring(1))).descending();
                        } else {
                            sortSpecification = SqlBuilder.sortColumn(mainTable.getTable(), SqlTable.of(mainTable.getTable()).column(sidx));
                        }
                        sidxList.add(sortSpecification);
                    }
                } else {
                    sidxList.add(SqlBuilder.sortColumn(mainTable.getTable(), SqlTable.of(mainTable.getTable()).column(pkeyId)).descending());
                }
                where.orderBy(sidxList);

                if (isPage) {
                    PageMethod.startPage((int) paginationModel.getCurrentPage(), (int) paginationModel.getPageSize(), false);
                }

                //查询主副表数据
                where.and(SqlTable.of(mainTable.getTable()).column(pkeyId), SqlBuilder.isIn(subWhere));
                SelectStatementProvider render = where.build().render(RenderingStrategies.MYBATIS3);
                dataList = flowFormDataMapper.selectManyMappedRows(render);

                String finalPkeyId = pkeyId;
                List<Object> idStringList = dataList.stream().map(m -> m.get(finalPkeyId)).distinct().collect(Collectors.toList());
                if (!idStringList.isEmpty()) {
                    //处理子表
                    List<String> tableFields = collect.stream().filter(c -> c.toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)).collect(Collectors.toList());
                    List<TableModel> childTableModels = tableModelList.stream().filter(t -> t.getTypeId().equals("0")).collect(Collectors.toList());
                    Map<String, List<String>> tableMap = tableFields.stream().collect(Collectors.groupingBy(t -> t.substring(0, t.lastIndexOf("-"))));
                    for (TableModel tableModel : childTableModels) {
                        String table = tableModel.getTable();
                        if (!allTableName.contains(table)) continue;
                        String tableField = tableNameAndTableField.get(table);

                        String childPrimKey = flowFormDataUtil.getKey(tableModel, databaseProductName);
                        String fogID = tableModel.getTableField();
                        String mainPrimKey = tableModel.getRelationField();
                        //外键值集合（通过主表关联键查询）
                        List<Object> fogValueList = dataList.stream().map(m -> m.get(mainPrimKey)).distinct().collect(Collectors.toList());
                        //外键是字符串 - 转换主表数据string
                        TableFields fogIdField = tableModel.getFields().stream().filter(t -> t.getField().equals(fogID)).findFirst().orElse(null);
                        boolean fogIdTypeString = Objects.nonNull(fogIdField) && fogIdField.getDataType().toLowerCase().contains(KeyConst.VARCHAR);
                        if (fogIdTypeString) {
                            idStringList = idStringList.stream().map(Object::toString).collect(Collectors.toList());
                        }

                        List<String> childFields = tableMap.get(tableField);
                        if (childFields != null) {
                            OnlineDynamicSqlModel onlineDynamicSqlModel = sqlModelList.stream().filter(s -> s.getTableName().equalsIgnoreCase(table)).findFirst().orElse(null);
                            if (onlineDynamicSqlModel == null) {
                                throw new DataException("表不存在");
                            }
                            SqlTable childSqlTable = onlineDynamicSqlModel.getSqlTable();
                            List<BasicColumn> childSqlColumns = new ArrayList<>();
                            for (String c : childFields) {
                                String childF = c.substring(c.lastIndexOf("-") + 1);
                                SqlColumn<Object> column = childSqlTable.column(childF);
                                childSqlColumns.add(column);
                            }
                            childSqlColumns.add(childSqlTable.column(childPrimKey));
                            childSqlColumns.add(childSqlTable.column(fogID));
                            //查子表数据字段，不去重
                            QueryExpressionDSL<SelectModel> childFrom = SqlBuilder.select(childSqlColumns).from(mainSqlModel.getSqlTable());
                            if (dycList.size() > 0) {
                                for (OnlineDynamicSqlModel sqlModel : dycList) {
                                    String relationKey = sqlModel.getRelationKey();
                                    boolean relationKeyTypeString = false;
                                    for (TableModel item : tableModelList) {
                                        if (item.getTable().equals(sqlModel.getTableName())) {
                                            String foreign = sqlModel.getForeign();
                                            TableFields thisfield = item.getFields().stream().filter(t -> t.getField().equals(foreign)).findFirst().orElse(null);
                                            if (thisfield != null) {
                                                relationKeyTypeString = thisfield.getDataType().toLowerCase().contains(KeyConst.VARCHAR);
                                            }
                                        }
                                    }
                                    //postgresql自增  int和varchar无法对比-添加以下判断
                                    if (Objects.equals(formData.getPrimaryKeyPolicy(), 2) && "PostgreSQL".equalsIgnoreCase(databaseProductName) && relationKeyTypeString) {
                                        relationKey += "::varchar";
                                    }
                                    //用于查询拼接各条件之后的主表id列表（和统计数量）
                                    childFrom.leftJoin(sqlModel.getSqlTable())
                                            .on(sqlModel.getSqlTable().column(sqlModel.getForeign()), new EqualTo(mainSqlModel.getSqlTable().column(relationKey)));
                                }
                            }
                            QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder childWhere = childFrom.where();

                            if (fogValueList.size() > 1000) {
                                List<List<Object>> lists = Lists.partition(fogValueList, 1000);
                                List<AndOrCriteriaGroup> groupListAll = new ArrayList<>();
                                for (List<Object> item : lists) {
                                    groupListAll.add(SqlBuilder.or(childSqlTable.column(fogID), SqlBuilder.isIn(item)));
                                }
                                groupListAll.remove(0);
                                childWhere.and(childSqlTable.column(fogID), SqlBuilder.isIn(lists.get(0)), groupListAll);
                            } else {
                                childWhere.and(childSqlTable.column(fogID), SqlBuilder.isIn(fogValueList));
                            }

                            //逻辑删除不展示
                            if (logicalDelete) {
                                childWhere.and(childSqlTable.column(deleteMarkKey), SqlBuilder.isNull());
                            }
                            //高级查询
                            OnlineProductSqlUtils.getSuperSql(childWhere, superQuery, sqlModelList, databaseProductName, null, false);

                            SelectStatementProvider childRender = childWhere.build().render(RenderingStrategies.MYBATIS3);
                            List<Map<String, Object>> mapList = flowFormDataMapper.selectManyMappedRows(childRender);
                            //连表去重，distinct去不掉
                            List<Object> dictinctKey = new ArrayList<>();
                            List<Map<String, Object>> newList = new ArrayList<>();
                            for (Map<String, Object> item : mapList) {
                                Object o = item.get(childPrimKey);
                                if (!dictinctKey.contains(o)) {
                                    newList.add(item);
                                    dictinctKey.add(o);
                                }
                                item.put(FlowFormConstant.ID, o);
                            }
                            Map<String, List<Map<String, Object>>> idMap = newList.stream().collect(Collectors.groupingBy(m -> m.get(fogID).toString()));

                            for (Map<String, Object> m : dataList) {
                                if (ObjectUtil.isNotEmpty(m.get(mainPrimKey))) {
                                    String s = m.get(mainPrimKey).toString();
                                    Map<String, Object> valueMap = new HashMap<>();
                                    valueMap.put(tableField, idMap.get(s));
                                    m.putAll(valueMap);
                                }
                            }
                        }
                    }
                } else {
                    return new ArrayList<>();
                }

                //添加id属性
                dataList = FormPublicUtils.addIdToList(dataList, pkeyId);

                //别名key还原
                VisualDevListServiceImpl.setAliasKey(dataList, aliasMap);

                query.setTotal(count);
                resList = dataList;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                DynamicDataSourceUtil.clearSwitchDataSource();
            }
        }
        if (resList.isEmpty()) {
            return new ArrayList<>();
        }
        resList = onlineSwapDataUtils.getSwapList(resList, allFields, visualDevJsonModel.getId(), false);
        return resList;
    }
}
