package jnpf.onlinedev.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.ActionResultCode;
import jnpf.base.controller.SuperController;
import jnpf.base.entity.*;
import jnpf.base.model.ColumnDataModel;
import jnpf.base.model.VisualDevJsonModel;
import jnpf.base.model.VisualWebTypeEnum;
import jnpf.base.model.column.ColumnModel;
import jnpf.base.model.export.VisualExportVo;
import jnpf.base.model.flow.DataModel;
import jnpf.base.model.flow.FlowLaunchModel;
import jnpf.base.model.module.PropertyJsonModel;
import jnpf.base.model.online.ExcelImportModel;
import jnpf.base.model.online.ImportExcelFieldModel;
import jnpf.base.model.online.VisualImportModel;
import jnpf.base.model.online.VisualdevModelDataInfoVO;
import jnpf.base.service.*;
import jnpf.base.util.FormExecelUtils;
import jnpf.base.util.VisualUtil;
import jnpf.base.util.VisualUtils;
import jnpf.base.vo.DownloadVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.emnus.ExportModelTypeEnum;
import jnpf.emnus.ModuleTypeEnum;
import jnpf.entity.FileParameter;
import jnpf.exception.DataException;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.trigger.TriggerDataModel;
import jnpf.model.ExcelModel;
import jnpf.model.OnlineDevData;
import jnpf.model.TransferModel;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.FormDataModel;
import jnpf.model.visualjson.UploaderTemplateModel;
import jnpf.model.visualjson.config.HeaderModel;
import jnpf.onlinedev.entity.VisualdevModelDataEntity;
import jnpf.onlinedev.model.*;
import jnpf.onlinedev.service.VisualDevInfoService;
import jnpf.onlinedev.service.VisualDevListService;
import jnpf.onlinedev.service.VisualPersonalService;
import jnpf.onlinedev.service.VisualdevModelDataService;
import jnpf.onlinedev.util.*;
import jnpf.permission.service.UserService;
import jnpf.permissions.PermissionInterfaceImpl;
import jnpf.util.*;
import jnpf.util.context.RequestContext;
import jnpf.util.visiual.JnpfKeyConsts;
import jnpf.workflow.service.TaskApi;
import jnpf.workflow.service.WorkFlowApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 0代码无表开发
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Slf4j
@Tag(name = "0代码无表开发", description = "OnlineDev")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/visualdev/OnlineDev")
public class VisualdevModelDataController extends SuperController<VisualdevModelDataService, VisualdevModelDataEntity> {

    private final VisualdevModelDataService visualdevModelDataService;
    private final VisualdevService visualdevService;
    private final FileExport fileExport;
    private final VisualDevListService visualDevListService;
    private final VisualDevInfoService visualDevInfoService;
    private final VisualdevReleaseService visualdevReleaseService;
    private final OnlineSwapDataUtils onlineSwapDataUtils;
    private final IntegrateUtil integrateUtil;
    private final ModuleService moduleService;
    private final ModuleUseNumService moduleUseNumService;
    private final VisualAliasService aliasService;
    private final WorkFlowApi workFlowApi;
    private final TaskApi taskApi;
    private final VisualPersonalService visualPersonalService;
    private final UserService userService;
    private final SystemService systemService;
    private final OnlineExcelUtil onlineExcelUtil;

    @Operation(summary = "获取数据列表")
    @Parameter(name = "modelId", description = "模板id")
    @PostMapping("/{modelId}/List")
    public ActionResult<PageListVO<Map<String, Object>>> list(@PathVariable("modelId") String modelId, @RequestBody PaginationModel paginationModel) throws WorkFlowException {
        StpUtil.checkPermission(modelId);
        paginationModel.setSystemCode(RequestContext.getAppCode());

        VisualdevReleaseEntity visualdevEntity = visualdevReleaseService.getById(modelId);
        VisualDevJsonModel visualJsonModel = OnlinePublicUtils.getVisualJsonModel(visualdevEntity);

        //判断请求客户端来源
        if (!RequestContext.isOrignPc()) {
            visualJsonModel.setColumnData(visualJsonModel.getAppColumnData());
        }
        ColumnDataModel columnDataModel = visualJsonModel.getColumnData();
        assert columnDataModel != null;
        List<Map<String, Object>> realList;
        if (VisualWebTypeEnum.FORM.getType().equals(visualdevEntity.getWebType())) {
            realList = new ArrayList<>();
        } else if (VisualWebTypeEnum.DATA_VIEW.getType().equals(visualdevEntity.getWebType())) {//
            //数据视图的接口数据获取、
            realList = onlineSwapDataUtils.getInterfaceData(visualdevEntity, paginationModel, columnDataModel);
        } else {
            this.setFlowVersion(visualJsonModel, paginationModel);
            realList = visualDevListService.getDataList(visualJsonModel, paginationModel);
        }

        //判断数据是否分组
        if (OnlineDevData.COLUMNTYPE_THREE.equals(columnDataModel.getType()) && StringUtil.isEmpty(paginationModel.getExtraQueryJson())) {
            realList = OnlineDevListUtils.groupData(realList, columnDataModel);
        }
        //树形列表
        if (OnlineDevData.COLUMNTYPE_FIVE.equals(columnDataModel.getType()) && StringUtil.isEmpty(paginationModel.getExtraQueryJson())) {
            realList = OnlineDevListUtils.treeListData(realList, columnDataModel);
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(paginationModel, PaginationVO.class);

        return ActionResult.page(realList, paginationVO);
    }

    private void setFlowVersion(VisualDevJsonModel visualDevJsonModel, PaginationModel paginationModel) {
        ColumnDataModel columnDataModel = visualDevJsonModel.getColumnData();
        //是否流程
        boolean enableFlow = false;
        //是否数据管理（有菜单则不是数据管理）
        if (ObjectUtil.isNotEmpty(paginationModel.getMenuId())) {
            //菜单id
            String menuId = paginationModel.getMenuId();
            if (Boolean.TRUE.equals(columnDataModel.getUseColumnPermission())) {
                visualDevJsonModel.setNeedPermission(true);
                List<String> pList = new ArrayList<>();
                Map<String, Object> pMap = PermissionInterfaceImpl.getColumnMap();
                if (pMap.get(menuId) != null) {
                    pList = JsonUtil.getJsonToList(pMap.get(menuId), ColumnModel.class).stream()
                            .map(ColumnModel::getEnCode).collect(Collectors.toList());
                }
                visualDevJsonModel.setPermissionList(pList);
            }
            //菜单判断是否启用流程-添加流程状态
            ModuleEntity info = moduleService.getInfo(menuId);
            //流程菜单
            enableFlow = Objects.equals(info.getType(), 9);
            visualDevJsonModel.setEnableFlow(enableFlow);
            if (enableFlow) {
                PropertyJsonModel model = JsonUtil.getJsonToBean(info.getPropertyJson(), PropertyJsonModel.class);
                List<String> flowVersionIds = workFlowApi.getFlowIdsByTemplateId(model.getModuleId());
                visualDevJsonModel.setFlowId(model.getModuleId());
                visualDevJsonModel.setFlowVersionIds(flowVersionIds);
            }
        }
    }


    @Operation(summary = "树形异步查询子列表接口")
    @Parameter(name = "modelId", description = "模板id")
    @Parameter(name = "id", description = "数据id")
    @PostMapping("/{modelId}/List/{id}")
    public ActionResult<PageListVO<Map<String, Object>>> listTree(@PathVariable("modelId") String modelId, @RequestBody PaginationModel paginationModel, @PathVariable("id") String id) throws WorkFlowException {
        StpUtil.checkPermission(modelId);

        VisualdevReleaseEntity visualdevEntity = visualdevReleaseService.getById(modelId);
        VisualDevJsonModel visualJsonModel = OnlinePublicUtils.getVisualJsonModel(visualdevEntity);
        //判断请求客户端来源
        if (!RequestContext.isOrignPc()) {
            visualJsonModel.setColumnData(visualJsonModel.getAppColumnData());
        }

        this.setFlowVersion(visualJsonModel, paginationModel);
        List<Map<String, Object>> realList = visualDevListService.getDataList(visualJsonModel, paginationModel);
        ColumnDataModel columnDataModel = visualJsonModel.getColumnData();
        assert columnDataModel != null;
        String parentField = columnDataModel.getParentField() + "_id";

        List<Map<String, Object>> collect = realList.stream().filter(item -> id.equals(item.get(parentField))).collect(Collectors.toList());
        PaginationVO paginationVO = JsonUtil.getJsonToBean(paginationModel, PaginationVO.class);
        return ActionResult.page(collect, paginationVO);
    }

    @Operation(summary = "获取列表表单配置JSON")
    @Parameter(name = "modelId", description = "模板id")
    @Parameter(name = "type", description = "类型0-草稿，1-发布")
    @GetMapping("/{modelId}/Config")
    public ActionResult<Object> getData(@PathVariable("modelId") String modelId,
                                        @RequestParam(value = "type", required = false) String type,
                                        @RequestParam(value = "personal", required = false) Integer personal,
                                        @RequestParam(value = "menuId", required = false) String menuId) {
        StpUtil.checkPermissionOr(modelId, "onlineDev.formDesign", "onlineDev.flowEngine", "generator.webForm", "generator.flowForm");

        //app调用应用的在线开发功能，记录该菜单点击次数
        if (!RequestContext.isOrignPc()) {
            moduleUseNumService.insertOrUpdateUseNum(menuId);
        }

        VisualdevEntity entity;
        //线上版本
        if ("0".equals(type)) {
            entity = visualdevService.getInfo(modelId);
        } else {
            VisualdevReleaseEntity releaseEntity = visualdevReleaseService.getById(modelId);
            entity = JsonUtil.getJsonToBean(releaseEntity, VisualdevEntity.class);
        }
        if (entity == null) {
            return ActionResult.fail(MsgCode.VS412.get());
        }
        String s = VisualUtil.checkPublishVisualModel(entity, MsgCode.VS005.get());
        if (s != null) {
            return ActionResult.fail(s);
        }

        DataInfoVO vo = JsonUtil.getJsonToBean(entity, DataInfoVO.class);
        if (Objects.equals(entity.getType(), 1) && !VisualWebTypeEnum.DATA_VIEW.getType().equals(entity.getWebType())) {
            vo.setPropsValueList(visualdevService.storedFieldList(entity));
        }

        //页面初始化获取个性化配置
        if (Objects.equals(1, personal)) {
            visualPersonalService.setDataInfoVO(menuId, vo);
        }
        return ActionResult.success(vo);
    }

    @Operation(summary = "获取表单配置JSON")
    @Parameter(name = "modelId", description = "模板id")
    @GetMapping("/{modelId}/FormData")
    public ActionResult<ColumnDataInfoVO> getFormData(@PathVariable("modelId") String modelId) {
        StpUtil.checkPermission(modelId);

        VisualdevEntity entity = visualdevService.getInfo(modelId);
        return ActionResult.success(entity.getFormData());
    }

    @Operation(summary = "获取数据信息")
    @Parameter(name = "modelId", description = "模板id")
    @GetMapping("/{modelId}/{id}")
    public ActionResult<Object> info(@PathVariable("id") String id,
                                     @PathVariable("modelId") String modelId,
                                     @RequestParam(name = "menuId", required = false) String menuId) throws DataException {
        StpUtil.checkPermission(modelId);
        VisualdevEntity visualdevEntity = visualdevService.getReleaseInfo(modelId);
        VisualdevModelDataInfoVO editDataInfo = visualDevInfoService.getEditDataInfo(id, visualdevEntity, OnlineInfoModel.builder().menuId(menuId).build());
        return ActionResult.success(editDataInfo);
    }

    @Operation(summary = "获取数据信息(带转换数据)")
    @Parameter(name = "modelId", description = "模板id")
    @Parameter(name = "id", description = "数据id")
    @Parameter(name = "propsValue", description = "存储字段（非必传）")
    @PostMapping("/{modelId}/DataChange")
    public ActionResult<Object> infoWithDataChange(@PathVariable("modelId") String modelId,
                                                   @RequestBody VisualInfoParam visualInfoParam) {
        StpUtil.checkPermission(modelId);
        String id = visualInfoParam.getId() instanceof String ? (String) visualInfoParam.getId() : JsonUtil.getObjectToString(visualInfoParam.getId());
        String propsValue = StringUtil.isNotEmpty(visualInfoParam.getPropsValue()) && visualInfoParam.getPropsValue().contains(JnpfConst.FIELD_SUFFIX_JNPFID) ?
                visualInfoParam.getPropsValue().split(JnpfConst.FIELD_SUFFIX_JNPFID)[0] : visualInfoParam.getPropsValue();

        VisualdevEntity visualdevEntity = visualdevService.getReleaseInfo(modelId);
        String columnData = RequestContext.isOrignPc() ? visualdevEntity.getColumnData() : visualdevEntity.getAppColumnData();
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(columnData, ColumnDataModel.class);
        assert columnDataModel != null;
        if (Boolean.TRUE.equals(columnDataModel.getUseBtnPermission())) {
            StpUtil.checkPermission(modelId + "::" + PermissionConst.BTN_DETAIL);
        }

        VisualdevModelDataInfoVO vo = visualDevInfoService.getDetailsDataInfo(id, visualdevEntity,
                OnlineInfoModel.builder().needRlationFiled(true).needSwap(true).propsValue(propsValue).menuId(visualInfoParam.getMenuId()).build());
        return ActionResult.success(vo);
    }

    @Operation(summary = "添加数据")
    @Parameter(name = "modelId", description = "模板id")
    @Parameter(name = "visualdevModelDataCrForm", description = "功能数据创建表单")
    @PostMapping("/{modelId}")
    public ActionResult<Object> create(@PathVariable("modelId") String modelId, @RequestBody VisualdevModelDataCrForm visualdevModelDataCrForm) throws WorkFlowException {
        StpUtil.checkPermission(modelId);

        String menuId = visualdevModelDataCrForm.getMenuId();
        VisualdevEntity visualdevEntity = visualdevService.getReleaseInfo(modelId);
        String columnData = RequestContext.isOrignPc() ? visualdevEntity.getColumnData() : visualdevEntity.getAppColumnData();
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(columnData, ColumnDataModel.class);
        assert columnDataModel != null;
        if (Boolean.TRUE.equals(columnDataModel.getUseBtnPermission())) {
            StpUtil.checkPermission(modelId + "::" + PermissionConst.BTN_ADD);
        }
        Map<String, Object> map = JsonUtil.stringToMap(visualdevModelDataCrForm.getData());
        DataModel dataModel = visualdevModelDataService.visualCreate(VisualParamModel.builder().visualdevEntity(visualdevEntity).data(map).menuId(menuId).build());
        AsyncExecuteModel model = new AsyncExecuteModel();
        model.setModelId(modelId);
        model.setTrigger(1);
        model.setDataId(ImmutableList.of(dataModel.getMainId()));
        model.setUserInfo(UserProvider.getUser());
        integrateUtil.asyncExecute(model);

        return ActionResult.success(MsgCode.SU001.get());
    }


    @Operation(summary = "修改数据")
    @Parameter(name = "modelId", description = "模板id")
    @Parameter(name = "id", description = "数据id")
    @Parameter(name = "visualdevModelDataUpForm", description = "功能数据修改表单")
    @PutMapping("/{modelId}/{id}")
    public ActionResult<Object> update(@PathVariable("id") String id, @PathVariable("modelId") String modelId, @RequestBody VisualdevModelDataUpForm visualdevModelDataUpForm) throws WorkFlowException {
        StpUtil.checkPermission(modelId);
        String menuId = visualdevModelDataUpForm.getMenuId();
        Map<String, Object> data = JsonUtil.stringToMap(visualdevModelDataUpForm.getData());
        VisualdevEntity visualdevEntity = visualdevService.getReleaseInfo(modelId);
        String columnData = RequestContext.isOrignPc() ? visualdevEntity.getColumnData() : visualdevEntity.getAppColumnData();
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(columnData, ColumnDataModel.class);
        assert columnDataModel != null;
        if (Boolean.TRUE.equals(columnDataModel.getUseBtnPermission())) {
            StpUtil.checkPermission(modelId + "::" + PermissionConst.BTN_EDIT);
        }
        DataModel dataModel = visualdevModelDataService.visualUpdate(VisualParamModel.builder().visualdevEntity(visualdevEntity).data(data).menuId(menuId).id(id).build());
        AsyncExecuteModel model = new AsyncExecuteModel();
        model.setModelId(modelId);
        model.setTrigger(2);
        model.setDataId(ImmutableList.of(id));
        model.setDataModel(dataModel);
        model.setUserInfo(UserProvider.getUser());
        integrateUtil.asyncExecute(model);
        return ActionResult.success(MsgCode.SU004.get());
    }

    //接口废弃全部走批量接口
    @Operation(summary = "删除数据")
    @Parameter(name = "modelId", description = "模板id")
    @Parameter(name = "id", description = "数据id")
    @DeleteMapping("/{modelId}/{id}")
    public ActionResult<Object> delete(@PathVariable("id") String id, @PathVariable("modelId") String modelId) throws Exception {
        StpUtil.checkPermission(modelId);

        VisualdevEntity visualdevEntity = visualdevService.getReleaseInfo(modelId);
        String columnData = RequestContext.isOrignPc() ? visualdevEntity.getColumnData() : visualdevEntity.getAppColumnData();
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(columnData, ColumnDataModel.class);
        assert columnDataModel != null;
        if (Boolean.TRUE.equals(columnDataModel.getUseBtnPermission())) {
            StpUtil.checkPermissionOr(modelId + "::" + PermissionConst.BTN_REMOVE, modelId + "::" + PermissionConst.BTN_BATCHREMOVE);
        }

        VisualDevJsonModel visualJsonModel = OnlinePublicUtils.getVisualJsonModel(visualdevEntity);

        //判断请求客户端来源
        if (!RequestContext.isOrignPc()) {
            visualJsonModel.setColumnData(visualJsonModel.getAppColumnData());
        }

        if (!StringUtil.isEmpty(visualdevEntity.getVisualTables()) && !OnlineDevData.TABLE_CONST.equals(visualdevEntity.getVisualTables())) {
            //树形递归删除
            if (OnlineDevData.COLUMNTYPE_FIVE.equals(visualJsonModel.getColumnData().getType())) {
                try {
                    ActionResult<PageListVO<Map<String, Object>>> listTreeAction = listTree(modelId, new PaginationModel(), id);
                    if (listTreeAction != null && listTreeAction.getCode() == 200 && listTreeAction.getData() instanceof Object) {
                        Map<String, Object> map = JsonUtil.getJsonToBean(listTreeAction.getData(), Map.class);
                        List<Map<String, Object>> list = JsonUtil.getJsonToListMap(map.get("list").toString());
                        if (!list.isEmpty()) {
                            for (Map<String, Object> item : list) {
                                this.delete(item.get("id").toString(), modelId);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("子数据删除异常:{}", e.getMessage());
                }
            }
            visualDevInfoService.getEditDataInfo(id, visualdevEntity, OnlineInfoModel.builder().build());

            boolean result = visualdevModelDataService.tableDelete(id, visualJsonModel);
            if (result) {
                AsyncExecuteModel model = new AsyncExecuteModel();
                model.setModelId(modelId);
                model.setTrigger(3);
                model.setDataId(ImmutableList.of(id));
                model.setUserInfo(UserProvider.getUser());
                integrateUtil.asyncExecute(model);
                return ActionResult.success(MsgCode.SU003.get());
            } else {
                return ActionResult.fail(MsgCode.FA003.get());
            }
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    @Operation(summary = "批量删除数据")
    @Parameter(name = "modelId", description = "模板id")
    @Parameter(name = "idsVo", description = "批量处理参数")
    @PostMapping("/batchDelete/{modelId}")
    public ActionResult<Object> batchDelete(@RequestBody BatchRemoveIdsVo idsVo, @PathVariable("modelId") String modelId) throws SQLException {
        StpUtil.checkPermission(modelId);

        VisualdevEntity visualdevEntity = visualdevService.getReleaseInfo(modelId);
        String columnData = RequestContext.isOrignPc() ? visualdevEntity.getColumnData() : visualdevEntity.getAppColumnData();
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(columnData, ColumnDataModel.class);
        assert columnDataModel != null;
        if (Boolean.TRUE.equals(columnDataModel.getUseBtnPermission())) {
            StpUtil.checkPermissionOr(modelId + "::" + PermissionConst.BTN_REMOVE, modelId + "::" + PermissionConst.BTN_BATCHREMOVE);
        }
        VisualDevJsonModel visualJsonModel = OnlinePublicUtils.getVisualJsonModel(visualdevEntity);

        //判断请求客户端来源
        if (!RequestContext.isOrignPc()) {
            visualJsonModel.setColumnData(visualJsonModel.getAppColumnData());
        }

        List<String> idsList = new ArrayList<>();
        List<String> idsVoList = Arrays.asList(idsVo.getIds());
        String errMess = "";

        List<Map<String, Object>> dataMap = new ArrayList<>();
        for (String id : idsVoList) {
            VisualdevModelDataInfoVO editDataInfo = visualDevInfoService.getEditDataInfo(id, visualdevEntity, OnlineInfoModel.builder().build());
            Map<String, Object> map = JsonUtil.stringToMap(editDataInfo.getData());
            dataMap.add(map);

            if (StringUtils.isNotBlank(idsVo.getFlowId())) {
                TaskEntity taskEntity = workFlowApi.getInfoSubmit(map.get(FlowFormConstant.FLOWTASKID).toString(), TaskEntity::getId,
                        TaskEntity::getParentId, TaskEntity::getFullName, TaskEntity::getStatus);
                if (taskEntity != null) {
                    try {
                        workFlowApi.delete(taskEntity);
                        idsList.add(id);
                    } catch (Exception e) {
                        errMess = e.getMessage();
                    }
                } else {
                    idsList.add(id);
                }
            } else {
                idsList.add(id);
            }
        }

        if (idsList.isEmpty()) {
            return ActionResult.fail(errMess);
        }

        if (!StringUtil.isEmpty(visualdevEntity.getVisualTables()) && !OnlineDevData.TABLE_CONST.equals(visualdevEntity.getVisualTables())) {
            AsyncExecuteModel model = new AsyncExecuteModel();
            model.setModelId(modelId);
            model.setTrigger(3);
            model.setDataId(idsList);
            model.setUserInfo(UserProvider.getUser());
            List<TriggerDataModel> triggerDataModels = integrateUtil.asyncDelExecute(model);
            ActionResult<Object> result = visualdevModelDataService.tableDeleteMore(idsList, visualJsonModel);
            if (!triggerDataModels.isEmpty()) {
                model.setDataMap(dataMap);
                integrateUtil.asyncExecute(model);
            }
            return result;
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    @Operation(summary = "导入数据")
    @Parameter(name = "modelId", description = "模板id")
    @Parameter(name = "visualImportModel", description = "导入参数")
    @PostMapping("{modelId}/ImportData")
    public ActionResult<ExcelImportModel> imports(@PathVariable("modelId") String modelId, @RequestBody VisualImportModel visualImportModel) throws Exception {
        StpUtil.checkPermission(modelId);

        VisualdevEntity visualdevEntity = visualdevService.getReleaseInfo(modelId);
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(visualdevEntity.getColumnData(), ColumnDataModel.class);
        assert columnDataModel != null;
        if (Boolean.TRUE.equals(columnDataModel.getUseBtnPermission())) {
            StpUtil.checkPermission(modelId + "::" + PermissionConst.BTN_UPLOAD);
        }
        VisualDevJsonModel visualJsonModel = OnlinePublicUtils.getVisualJsonModel(visualdevEntity);
        FormDataModel formData = visualJsonModel.getFormData();
        List<FieLdsModel> fieldsModelList = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
        List<FieLdsModel> allFieLds = new ArrayList<>();
        VisualUtils.recursionFields(fieldsModelList, allFieLds);
        visualJsonModel.setFormListModels(allFieLds);
        visualJsonModel.setFlowId(visualImportModel.getFlowId());
        //复杂表头数据 还原成普通数据
        List<Map<String, Object>> listData = new ArrayList<>();
        List<Map<String, Object>> headerRow = new ArrayList<>();
        if (visualImportModel.isType()) {
            ActionResult<Map<String, Object>> result = importPreview(modelId, visualImportModel.getFileName());
            if (result == null) {
                throw new DataException(MsgCode.FA018.get());
            }
            if (result.getCode() != 200) {
                return ActionResult.fail(result.getMsg());
            }
            if (result.getData() instanceof Map) {
                Map<String, Object> data = result.getData();
                listData = (List<Map<String, Object>>) data.get("dataRow");
                headerRow = (List<Map<String, Object>>) data.get("headerRow");
            }
        } else {
            listData = visualImportModel.getList();
        }
        List<Map<String, Object>> mapList = VisualUtils.complexImportsDataOnline(listData, visualdevEntity);
        ExcelImportModel excelData = onlineExcelUtil.createExcelData(mapList, visualJsonModel, visualdevEntity);

        //复杂表头-表头和数据处理
        List<HeaderModel> complexHeaderList = columnDataModel.getComplexHeaderList();
        if (!Objects.equals(columnDataModel.getType(), 3) && !Objects.equals(columnDataModel.getType(), 5)) {
            List<Map<String, Object>> mapList1 = VisualUtils.complexHeaderDataHandel(excelData.getFailResult(), complexHeaderList, false);
            excelData.setFailResult(mapList1);
        }
        excelData.setHeaderRow(headerRow);
        return ActionResult.success(excelData);
    }

    @Operation(summary = "导出")
    @Parameter(name = "modelId", description = "模板id")
    @Parameter(name = "paginationModelExport", description = "导出参数")
    @PostMapping("/{modelId}/Actions/ExportData")
    public ActionResult<Object> export(@PathVariable("modelId") String modelId, @RequestBody PaginationModelExport paginationModelExport) throws ParseException, IOException, SQLException, DataException {
        StpUtil.checkPermission(modelId);
        paginationModelExport.setSystemCode(RequestContext.getAppCode());

        ModuleEntity menuInfo = moduleService.getInfo(paginationModelExport.getMenuId());
        VisualdevEntity visualdevEntity = visualdevService.getReleaseInfo(modelId);
        if (visualdevEntity == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        String excelName = "";
        if (menuInfo != null) {
            excelName = menuInfo.getFullName();
        } else {
            excelName = visualdevEntity.getFullName();
        }

        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(visualdevEntity.getColumnData(), ColumnDataModel.class);
        assert columnDataModel != null;
        if (Boolean.TRUE.equals(columnDataModel.getUseBtnPermission())) {
            StpUtil.checkPermission(modelId + "::" + PermissionConst.BTN_DOWNLOAD);
        }

        VisualDevJsonModel visualJsonModel = OnlinePublicUtils.getVisualJsonModel(visualdevEntity);
        //判断请求客户端来源
        if (!RequestContext.isOrignPc()) {
            visualJsonModel.setColumnData(visualJsonModel.getAppColumnData());
        }

        String[] keys = paginationModelExport.getSelectKey();
        List<String> selectKey = Arrays.asList(paginationModelExport.getSelectKey());
        List<Object> selectIds = Arrays.asList(paginationModelExport.getSelectIds());
        //关键字过滤
        List<Map<String, Object>> realList;
        DownloadVO vo;

        if (VisualWebTypeEnum.DATA_VIEW.getType().equals(visualdevEntity.getWebType())) {//视图查询数据
            VisualdevReleaseEntity visualdevREntity = JsonUtil.getJsonToBean(visualdevEntity, VisualdevReleaseEntity.class);
            realList = onlineSwapDataUtils.getInterfaceData(visualdevREntity, paginationModelExport, visualJsonModel.getColumnData());
            if ("2".equals(paginationModelExport.getDataType()) && StringUtils.isBlank(columnDataModel.getViewKey())) {
                ActionResult.fail(MsgCode.VS029.get());
            }
            realList = "2".equals(paginationModelExport.getDataType()) ? realList.stream().filter(t -> selectIds.contains(t.get(columnDataModel.getViewKey()))).collect(Collectors.toList()) : realList;
            vo = VisualUtils.createModelExcelApiData(visualdevEntity.getColumnData(), realList, Arrays.asList(keys), "表单信息", excelName, new ExcelModel());
        } else {
            ExcelModel excelModel = onlineSwapDataUtils.getDefaultValue(visualdevEntity.getFormData(), selectKey);
            realList = visualdevModelDataService.exportData(keys, paginationModelExport, visualJsonModel);
            realList = "2".equals(paginationModelExport.getDataType()) ? realList.stream().filter(t -> selectIds.contains(t.get("id"))).collect(Collectors.toList()) : realList;
            vo = VisualUtils.createModelExcel(visualdevEntity, realList, Arrays.asList(keys), "表单信息", excelName, excelModel);
        }
        return ActionResult.success(vo);
    }

    @Operation(summary = "功能导出")
    @Parameter(name = "modelId", description = "模板id")
    @PostMapping("/{modelId}/Actions/Export")
    @SaCheckPermission("onlineDev.formDesign")
    public ActionResult<Object> exportData(@PathVariable("modelId") String modelId) {
        VisualdevEntity visualdevEntity = visualdevService.getReleaseInfo(modelId);
        VisualExportVo vo = JsonUtil.getJsonToBean(visualdevEntity, VisualExportVo.class);
        List<VisualAliasEntity> list = aliasService.getList(visualdevEntity.getId());
        vo.setAliasListJson(JsonUtil.getObjectToString(list));
        vo.setModelType(ExportModelTypeEnum.DESIGN.getMessage());
        DownloadVO downloadVO = fileExport.exportFile(vo, FileTypeConstant.TEMPORARY, visualdevEntity.getFullName(), ModuleTypeEnum.VISUAL_DEV.getTableName());
        return ActionResult.success(downloadVO);
    }

    @Operation(summary = "功能导入")
    @PostMapping(value = "/Actions/Import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SaCheckPermission("onlineDev.formDesign")
    public ActionResult<Object> importData(@RequestParam("type") Integer type, @RequestPart("file") MultipartFile multipartFile) {
        SystemEntity sysInfo = systemService.getInfoByEnCode(RequestContext.getAppCode());
        if (sysInfo == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        //判断是否为.json结尾
        if (FileUtil.existsSuffix(multipartFile, ModuleTypeEnum.VISUAL_DEV.getTableName())) {
            return ActionResult.fail(MsgCode.IMP002.get());
        }
        //获取文件内容
        String fileContent = FileUtil.getFileContent(multipartFile);
        VisualExportVo vo = JsonUtil.getJsonToBean(fileContent, VisualExportVo.class);

        VisualdevEntity visualdevEntity = JsonUtil.getJsonToBean(vo, VisualdevEntity.class);
        if (!sysInfo.getId().equals(visualdevEntity.getSystemId())) {
            visualdevEntity.setId(RandomUtil.uuId());
            visualdevService.setAutoEnCode(visualdevEntity);
            visualdevEntity.setSystemId(sysInfo.getId());
        }
        StringJoiner errList = new StringJoiner("、");
        String copyNum = UUID.randomUUID().toString().substring(0, 5);
        if (visualdevService.getInfo(visualdevEntity.getId()) != null) {
            if (Objects.equals(type, 0)) {
                errList.add("ID");
            } else {
                visualdevEntity.setId(RandomUtil.uuId());
            }
        }
        if (visualdevService.getObjByEncode(visualdevEntity.getEnCode(), visualdevEntity.getType())) {
            if (Objects.equals(type, 0)) {
                errList.add(MsgCode.IMP009.get());
            } else {
                visualdevEntity.setEnCode(visualdevEntity.getEnCode() + copyNum);
            }
        }
        if (visualdevService.getCountByName(visualdevEntity.getFullName(), visualdevEntity.getType(), sysInfo.getId())) {
            if (Objects.equals(type, 0)) {
                errList.add(MsgCode.IMP008.get());
            } else {
                visualdevEntity.setFullName(visualdevEntity.getFullName() + ".副本" + copyNum);
            }
        }
        if (Objects.equals(type, 0) && errList.length() > 0) {
            return ActionResult.fail(errList + MsgCode.IMP007.get());
        }

        if (visualdevEntity.getId() != null) {
            visualdevService.setIgnoreLogicDelete().removeById(visualdevEntity.getId());
            visualdevService.clearIgnoreLogicDelete();
        }
        if (Objects.equals(visualdevEntity.getType(), 1)) {
            visualdevEntity.setDbLinkId("0");
        }
        visualdevEntity.setCreatorTime(DateUtil.getNowDate());
        visualdevEntity.setCreatorUserId(UserProvider.getUser().getUserId());
        visualdevEntity.setLastModifyTime(null);
        visualdevEntity.setLastModifyUserId(null);
        visualdevEntity.setState(0);
        visualdevService.save(visualdevEntity);
        if (StringUtil.isNotEmpty(vo.getAliasListJson())) {
            List<VisualAliasEntity> jsonToList = JsonUtil.getJsonToList(vo.getAliasListJson(), VisualAliasEntity.class);
            for (VisualAliasEntity aliasEntity : jsonToList) {
                aliasService.copyEntity(aliasEntity, visualdevEntity.getId());
            }
        }
        return ActionResult.success(MsgCode.IMP001.get());
    }

    @Operation(summary = "模板下载")
    @Parameter(name = "modelId", description = "模板id")
    @Parameter(name = "menuId", description = "菜单id")
    @GetMapping("/{modelId}/TemplateDownload")
    public ActionResult<DownloadVO> templateDownload(@PathVariable("modelId") String modelId,
                                                     @RequestParam(value = "menuId", required = false) String menuId) {
        StpUtil.checkPermission(modelId);

        ModuleEntity menuInfo = moduleService.getInfo(menuId);
        VisualdevEntity visualdevEntity = visualdevService.getReleaseInfo(modelId);
        if (visualdevEntity == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        String menuFullName = "";
        if (menuInfo != null) {
            menuFullName = menuInfo.getFullName();
        } else {
            menuFullName = visualdevEntity.getFullName();
        }
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(visualdevEntity.getColumnData(), ColumnDataModel.class);
        assert columnDataModel != null;
        if (Boolean.TRUE.equals(columnDataModel.getUseBtnPermission())) {
            StpUtil.checkPermission(modelId + "::" + PermissionConst.BTN_DOWNLOAD);
        }
        UploaderTemplateModel uploaderTemplateModel = JsonUtil.getJsonToBean(columnDataModel.getUploaderTemplateJson(), UploaderTemplateModel.class);
        List<String> selectKey = uploaderTemplateModel.getSelectKey();
        ExcelModel excelModel = onlineSwapDataUtils.getDefaultValue(visualdevEntity.getFormData(), selectKey);
        List<Map<String, Object>> dataList = new ArrayList<>();
        dataList.add(excelModel.getDataMap());
        DownloadVO vo = VisualUtils.createModelExcel(visualdevEntity, dataList, selectKey, "导入模板", menuFullName + "导入模板", excelModel);
        return ActionResult.success(vo);
    }

    @Operation(summary = "上传文件")
    @PostMapping("/Uploader")
    public ActionResult<Object> uploader() {
        List<MultipartFile> list = UpUtil.getFileAll();
        MultipartFile file = list.get(0);
        if (file != null) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && (originalFilename.endsWith(".xlsx") || originalFilename.endsWith(".xls"))) {
                String fileName = XSSEscape.escape(RandomUtil.uuId() + "." + UpUtil.getFileType(file));
                //上传文件
                FileInfo fileInfo = FileUploadUtils.uploadFile(new FileParameter(FileTypeConstant.TEMPORARY, fileName), file);
                DownloadVO vo = DownloadVO.builder().build();
                vo.setName(fileInfo.getFilename());
                return ActionResult.success(vo);
            }
        }
        return ActionResult.fail(MsgCode.ETD110.get());
    }

    @Operation(summary = "导入预览")
    @Parameter(name = "modelId", description = "模板id")
    @Parameter(name = "fileName", description = "文件名")
    @GetMapping("/{modelId}/ImportPreview")
    public ActionResult<Map<String, Object>> importPreview(@PathVariable("modelId") String modelId, String fileName) throws
            Exception {
        StpUtil.checkPermission(modelId);

        Map<String, Object> previewMap = null;
        try {
            VisualdevReleaseEntity entity = visualdevReleaseService.getById(modelId);
            ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(entity.getColumnData(), ColumnDataModel.class);
            if (columnDataModel == null) {
                throw new DataException("列表配置异常");
            }
            if (Boolean.TRUE.equals(columnDataModel.getUseBtnPermission())) {
                StpUtil.checkPermission(modelId + "::" + PermissionConst.BTN_UPLOAD);
            }
            FormDataModel formDataModel = JsonUtil.getJsonToBean(entity.getFormData(), FormDataModel.class);
            UploaderTemplateModel uploaderTemplateModel = JsonUtil.getJsonToBean(columnDataModel.getUploaderTemplateJson(), UploaderTemplateModel.class);
            List<FieLdsModel> fieLdsModels = JsonUtil.getJsonToList(formDataModel.getFields(), FieLdsModel.class);
            List<FieLdsModel> allFields = new ArrayList<>();
            OnlinePublicUtils.recursionFormFields(allFields, fieLdsModels);
            List<String> selectKey = uploaderTemplateModel.getSelectKey();
            File temporary = FileUploadUtils.downloadFileToLocal(new FileParameter(FileTypeConstant.TEMPORARY, fileName));
            //判断有无子表
            String tablefield = selectKey.stream().filter(s -> s.toLowerCase().startsWith(JnpfKeyConsts.CHILD_TABLE_PREFIX)).findFirst().orElse(null);
            //判断有没有复杂表头
            boolean hasComplex = false;
            for (HeaderModel item : columnDataModel.getComplexHeaderList()) {
                if (item.getChildColumns() != null && !item.getChildColumns().isEmpty()) {
                    List<String> childColumns = new ArrayList<>(item.getChildColumns());
                    childColumns.retainAll(selectKey);
                    if (!childColumns.isEmpty()) {
                        hasComplex = true;
                    }
                }
            }
            //有子表需要取第二行的表头
            Integer i = tablefield != null || hasComplex ? 2 : 1;
            //读取excel中数据
            InputStream inputStream = ExcelUtil.solveOrginTitle(temporary, i);
            List<Map<String, Object>> excelDataList = ExcelUtil.getMapByInputStream(inputStream, 0, i);
            //数据超过100条
            if (excelDataList != null && excelDataList.size() > 1000) {
                return ActionResult.fail(MsgCode.ETD117.get());
            }
            ExcelUtil.imoportExcelToMap(temporary, i, excelDataList);
            //列表字段
            List<Map<String, Object>> columns = getColumns(selectKey, allFields);

            List<Map<String, Object>> results = FormExecelUtils.dataMergeChildTable(excelDataList, selectKey);

            previewMap = new HashMap<>();
            //复杂表头-表头和数据处理
            List<HeaderModel> complexHeaderList = columnDataModel.getComplexHeaderList();
            if (!Objects.equals(columnDataModel.getType(), 3) && !Objects.equals(columnDataModel.getType(), 5)) {
                columns = VisualUtils.complexHeaderHandelOnline(columns, complexHeaderList);
            }
            previewMap.put("dataRow", results);
            previewMap.put("headerRow", columns);
        } catch (Exception e) {
            e.printStackTrace();
            return ActionResult.fail(MsgCode.VS407.get());
        }
        return ActionResult.success(previewMap);
    }

    private static @NotNull List<Map<String, Object>> getColumns
            (List<String> selectKey, List<FieLdsModel> allFields) {
        List<Map<String, Object>> columns = new ArrayList<>();
        List<ImportExcelFieldModel> chiImList = new ArrayList<>();
        List<ImportExcelFieldModel> allImList = new ArrayList<>();
        selectKey.stream().forEach(s -> {
            String requiredStr = "";
            ImportExcelFieldModel importExcel = new ImportExcelFieldModel();
            if (s.toLowerCase().startsWith(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                String table = s.substring(0, s.indexOf("-"));
                String field = s.substring(s.indexOf("-") + 1);
                FieLdsModel fieLdsModel = allFields.stream().filter(t -> t.getVModel().equals(table)).findFirst().orElse(null);
                List<FieLdsModel> children = fieLdsModel.getConfig().getChildren();
                FieLdsModel fieLdsModel1 = children.stream().filter(t -> t.getVModel().equals(field)).findFirst().orElse(null);
                requiredStr = fieLdsModel1.getConfig().isRequired() ? "*" : "";
                importExcel.setField(field);
                importExcel.setTableField(table);
                importExcel.setFullName(requiredStr + fieLdsModel1.getConfig().getLabel());
                importExcel.setJnpfKey(fieLdsModel1.getConfig().getJnpfKey());
                chiImList.add(importExcel);
            } else {
                FieLdsModel fieLdsModel = allFields.stream().filter(t -> t.getVModel().equals(s)).findFirst().orElse(null);
                requiredStr = fieLdsModel.getConfig().isRequired() ? "*" : "";
                importExcel.setField(s);
                importExcel.setFullName(requiredStr + fieLdsModel.getConfig().getLabel());
                importExcel.setJnpfKey(fieLdsModel.getConfig().getJnpfKey());
                allImList.add(importExcel);
            }
        });
        Map<String, List<ImportExcelFieldModel>> groups = chiImList.stream().collect(Collectors.groupingBy(ImportExcelFieldModel::getTableField, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<ImportExcelFieldModel>> entry : groups.entrySet()) {
            ImportExcelFieldModel importExcel = new ImportExcelFieldModel();

            List<ImportExcelFieldModel> value = entry.getValue();
            FieLdsModel fieLdsModel = allFields.stream().filter(f -> entry.getKey().equals(f.getVModel())).findFirst().orElse(null);
            assert fieLdsModel != null;
            String tableName = fieLdsModel.getConfig().getLabel();
            importExcel.setField(entry.getKey());
            importExcel.setFullName(tableName);
            importExcel.setJnpfKey("table");
            importExcel.setChildren(value);
            allImList.add(importExcel);
        }

        for (ImportExcelFieldModel importExcel : allImList) {
            Map<String, Object> selectMap = new HashMap<>(16);
            selectMap.put("id", importExcel.getField());
            selectMap.put("fullName", importExcel.getFullName());
            selectMap.put("jnpfKey", importExcel.getJnpfKey());
            if (importExcel.getChildren() != null) {
                List<ImportExcelFieldModel> children = importExcel.getChildren();
                List<Map<String, Object>> childMapList = new ArrayList<>();
                for (ImportExcelFieldModel childIm : children) {
                    Map<String, Object> childMap = new HashMap<>(16);
                    childMap.put("id", childIm.getField());
                    childMap.put("fullName", childIm.getFullName());
                    childMap.put("jnpfKey", childIm.getJnpfKey());
                    childMapList.add(childMap);
                }
                selectMap.put("children", childMapList);
            }
            columns.add(selectMap);
        }
        return columns;
    }

    @Operation(summary = "导出异常报告")
    @Parameter(name = "modelId", description = "模板id")
    @Parameter(name = "visualImportModel", description = "导出参数")
    @PostMapping("/{modelId}/ImportExceptionData")
    public ActionResult<DownloadVO> importExceptionData(@PathVariable("modelId") String
                                                                modelId, @RequestBody VisualImportModel visualImportModel) {
        StpUtil.checkPermission(modelId);
        String menuFullName = "";
        if (StringUtil.isNotEmpty(visualImportModel.getMenuId())) {
            ModuleEntity menuInfo = moduleService.getInfo(visualImportModel.getMenuId());
            if (menuInfo != null && StringUtil.isNotEmpty(menuInfo.getFullName())) {
                menuFullName = menuInfo.getFullName();
            }
        }
        VisualdevEntity visualdevEntity = visualdevService.getReleaseInfo(modelId);
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(visualdevEntity.getColumnData(), ColumnDataModel.class);
        assert columnDataModel != null;
        if (Boolean.TRUE.equals(columnDataModel.getUseBtnPermission())) {
            StpUtil.checkPermission(modelId + "::" + PermissionConst.BTN_DOWNLOAD);
        }
        UploaderTemplateModel uploaderTemplateModel = JsonUtil.getJsonToBean(columnDataModel.getUploaderTemplateJson(), UploaderTemplateModel.class);
        List<String> selectKey = uploaderTemplateModel.getSelectKey();
        ExcelModel excelModel = onlineSwapDataUtils.getDefaultValue(visualdevEntity.getFormData(), selectKey);
        DownloadVO vo = VisualUtils.createModelExcel(visualdevEntity, visualImportModel.getList(), selectKey, "错误报告", menuFullName + "错误报告", excelModel);
        return ActionResult.success(vo);
    }

    @Operation(summary = "自定义按钮发起审批")
    @Parameter(name = "modelId", description = "模板id")
    @Parameter(name = "visualImportModel", description = "导出参数")
    @PostMapping("/{modelId}/actionLaunchFlow")
    public ActionResult<DownloadVO> actionLaunchFlow(@PathVariable("modelId") String
                                                             modelId, @RequestBody FlowLaunchModel model) {
        StpUtil.checkPermission(modelId);

        VisualdevEntity visualdevEntity = visualdevService.getReleaseInfo(modelId);
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(visualdevEntity.getColumnData(), ColumnDataModel.class);
        assert columnDataModel != null;
        if (Boolean.TRUE.equals(columnDataModel.getUseBtnPermission())) {
            StpUtil.checkPermission(modelId + "::" + model.getBtnCode());
        }
        //用户列表
        List<String> userList = new ArrayList<>();
        if (Objects.equals(1, model.getCurrentUser())) {
            userList.add(UserProvider.getUser().getUserId());
        }
        if (Objects.equals(1, model.getCustomUser()) && CollUtil.isNotEmpty(model.getInitiator())) {
            userList.addAll(userService.getUserIdList(model.getInitiator()));
            userList = new ArrayList<>(new HashSet<>(userList));
        }
        //数据列表
        List<Map<String, Object>> formDataList = new ArrayList<>();
        List<List<TransferModel>> dataList = model.getDataList();
        if (CollUtil.isNotEmpty(dataList)) {
            transforData(dataList, formDataList);
        }
        FlowModel flowModel = new FlowModel();
        flowModel.setTemplateId(model.getTemplate());
        flowModel.setUserIds(userList);
        flowModel.setFormDataList(formDataList);
        flowModel.setHasPermission(model.getHasPermission());
        ActionResult<Object> actionResult = taskApi.launchFlow(flowModel);
        if (actionResult == null || !ActionResultCode.SUCCESS.getCode().equals(actionResult.getCode())) {
            return ActionResult.fail(actionResult == null ? "" : actionResult.getMsg());
        }
        return ActionResult.success(actionResult.getMsg());
    }

    //转换数据
    private static void transforData
    (List<List<TransferModel>> dataList, List<Map<String, Object>> formDataList) {
        for (List<TransferModel> itemList : dataList) {
            if (CollUtil.isNotEmpty(itemList)) {
                Map<String, Object> map = new HashMap<>();
                Map<String, List<Map<String, Object>>> tableList = new HashMap<>();
                for (TransferModel transferModel : itemList) {
                    String[] table = transferModel.getTargetField().split("-");
                    String[] value = transferModel.getSourceValue().split("-");
                    List<Map<String, Object>> list = tableList.get(table[0]) != null ? tableList.get(table[0]) : new ArrayList<>();
                    if (table.length > 1) {
                        if (value.length > 1) {
                            if (transferModel.getDefaultValue() instanceof List) {
                                List<Object> data = (List<Object>) transferModel.getDefaultValue();
                                int num = data.size() - list.size();
                                for (int i = 0; i < num; i++) {
                                    list.add(new HashMap<>());
                                }
                                for (int i = 0; i < data.size(); i++) {
                                    Map<String, Object> objectMap = list.get(i);
                                    objectMap.put(table[1], data.get(i));
                                }
                            }
                        } else {
                            if (list.isEmpty()) {
                                list.add(new HashMap<>());
                            }
                            for (Map<String, Object> objectMap : list) {
                                objectMap.put(table[1], transferModel.getDefaultValue());
                            }
                        }
                        tableList.put(table[0], list);
                    }
                    map.put(transferModel.getTargetField(), transferModel.getDefaultValue());
                }
                map.putAll(tableList);
                formDataList.add(map);
            }
        }
    }

    @Operation(summary = "根据菜单获取功能配置(流程是直接通过菜单确定表单)")
    @Parameter(name = "menuId", description = "菜单id")
    @GetMapping("/Config")
    public ActionResult<Object> getConfigByMenu(@RequestParam(value = "menuId", required = false) String menuId,
                                                @RequestParam(value = "systemId", required = false) String systemId) {
        StpUtil.checkPermissionOr(menuId, "onlineDev.formDesign", "generator.webForm", "generator.flowForm");
        //app调用应用的在线开发功能，记录该菜单点击次数
        if (!RequestContext.isOrignPc()) {
            moduleUseNumService.insertOrUpdateUseNum(menuId);
        }

        VisualdevReleaseEntity releaseEntity = null;
        ModuleEntity info = moduleService.getInfo(menuId);

        if (info != null && StringUtil.isNotEmpty(info.getPropertyJson())) {
            PropertyJsonModel propertyJsonModel = JsonUtil.getJsonToBean(info.getPropertyJson(), PropertyJsonModel.class);
            String modelId = propertyJsonModel.getModuleId();
            releaseEntity = visualdevReleaseService.getById(modelId);
        }
        if (releaseEntity == null || (StringUtil.isNotEmpty(systemId) && !Objects.equals(info.getSystemId(), systemId))) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        VisualdevEntity entity = JsonUtil.getJsonToBean(releaseEntity, VisualdevEntity.class);

        String s = VisualUtil.checkPublishVisualModel(entity, MsgCode.VS005.get());
        if (s != null) {
            return ActionResult.fail(s);
        }
        DataInfoVO vo = JsonUtil.getJsonToBean(entity, DataInfoVO.class);
        return ActionResult.success(vo);
    }

    @Operation(summary = "关联查询数据")
    @Parameter(name = "id", description = "功能id")
    @PostMapping("/RelationQuery")
    public ActionResult<PageListVO<Map<String, Object>>> relationQuery(@RequestBody RelationQuery query) {
        VisualdevEntity entity = visualdevService.getReleaseInfo(query.getModelId());
        List<Map<String, Object>> realList = visualdevModelDataService.relationQuery(entity, query);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(query, PaginationVO.class);
        return ActionResult.page(realList, paginationVO);
    }
}
