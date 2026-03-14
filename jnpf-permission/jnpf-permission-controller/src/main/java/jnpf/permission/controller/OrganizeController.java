package jnpf.permission.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.controller.SuperController;
import jnpf.base.service.SysconfigService;
import jnpf.base.util.ExcelTool;
import jnpf.base.vo.DownloadVO;
import jnpf.base.vo.ListVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.KeyConst;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.emnus.OrgTypeEnum;
import jnpf.exception.DataException;
import jnpf.message.service.SynThirdDingTalkService;
import jnpf.message.service.SynThirdQyService;
import jnpf.message.util.SynThirdConsts;
import jnpf.model.ExcelColumnAttr;
import jnpf.model.ExcelImportForm;
import jnpf.model.ExcelImportVO;
import jnpf.model.ExcelModel;
import jnpf.permission.constant.OrgColumnMap;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.model.organize.*;
import jnpf.permission.model.user.mod.UserIdModel;
import jnpf.permission.service.OrganizeService;
import jnpf.permission.service.PositionService;
import jnpf.util.*;
import jnpf.util.treeutil.SumTree;
import jnpf.util.treeutil.newtreeutil.TreeDotUtils;
import jnpf.workflow.service.WorkFlowApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 组织机构
 * 组织架构：公司》部门》岗位》用户
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Tag(name = "组织管理", description = "Organize")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permission/Organize")
@Slf4j
public class OrganizeController extends SuperController<OrganizeService, OrganizeEntity> {

    private final OrganizeService organizeService;
    private final SynThirdQyService synThirdQyService;
    private final SynThirdDingTalkService synThirdDingTalkService;
    private final SysconfigService sysconfigApi;
    private final PositionService positionService;
    private final WorkFlowApi workFlowApi;

    @Operation(summary = "获取组织列表")
    @SaCheckPermission(value = {"permission.organize", "permission.user", "permission.role",
            "integrationCenter.dingTalk", "integrationCenter.weCom"}, mode = SaMode.OR)
    @Parameter(name = "id", description = "主键", required = true)
    @GetMapping("/AsyncList/{id}")
    public ActionResult<PageListVO<OrganizeListVO>> getList(@PathVariable("id") String id, OrganizePagination pagination) {
        pagination.setParentId(id);
        pagination.setDataType(1);
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            pagination.setParentId(null);
            pagination.setDataType(null);
        }
        List<OrganizeEntity> list = organizeService.getList(pagination);
        Map<String, Object> allOrgsTreeName = organizeService.getAllOrgsTreeName();
        List<OrganizeListVO> listVO = JsonUtil.getJsonToList(list, OrganizeListVO.class);
        for (OrganizeListVO item : listVO) {
            if (PermissionConst.DEPARTMENT.equals(item.getCategory())) {
                item.setIcon(PermissionConst.DEPARTMENT_ICON);
            } else {
                item.setIcon(PermissionConst.COMPANY_ICON);
            }
            if (StringUtil.isNotEmpty(item.getParentId()) && Objects.nonNull(allOrgsTreeName.get(item.getParentId()))) {
                item.setParentName(allOrgsTreeName.get(item.getParentId()).toString());
            }
            item.setHasChildren(true);
            String[] orgs = item.getOrganizeIdTree().split(",");
            item.setOrganizeIds(Arrays.asList(orgs));

        }
        ListVO<OrganizeListVO> vo = new ListVO<>();
        vo.setList(listVO);
        if (Objects.equals(pagination.getDataType(), 1)) {
            ActionResult.page(listVO, null);
        }
        PaginationVO jsonToBean = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(listVO, jsonToBean);
    }

    @Operation(summary = "新建组织")
    @Parameter(name = "organizeCrForm", description = "组织模型", required = true)
    @SaCheckPermission(value = {"permission.organize"})
    @PostMapping
    public ActionResult<Object> create(@RequestBody @Valid OrganizeCrForm organizeCrForm) {
        OrganizeEntity entity = JsonUtil.getJsonToBean(organizeCrForm, OrganizeEntity.class);
        if (organizeService.isExistByFullName(entity, false, false)) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (organizeService.isExistByEnCode(entity.getEnCode(), null)) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        //验证层级
        if (!organizeService.checkLevel(entity)) {
            return ActionResult.fail(MsgCode.PS036.get(sysconfigApi.getSysInfo().getOrgLevel()));
        }
        organizeService.create(entity);
        ThreadPoolExecutorUtil.getExecutor().execute(() -> {
            try {
                //创建组织后判断是否需要同步到企业微信
                synThirdQyService.unifyDepartmentSysToQy(false, entity, "", SynThirdConsts.CREAT_DEP);
                //创建组织后判断是否需要同步到钉钉
                synThirdDingTalkService.unifyDepartmentSysToDing(false, entity, "", SynThirdConsts.CREAT_DEP);
            } catch (Exception e) {
                log.error("创建组织后同步失败到企业微信或钉钉失败，异常：{}", e.getMessage());
            }
        });
        return ActionResult.success(MsgCode.SU001.get());
    }

    @Operation(summary = "更新组织")
    @Parameter(name = "id", description = "主键值", required = true)
    @Parameter(name = "organizeUpForm", description = "组织模型", required = true)
    @SaCheckPermission(value = {"permission.organize"})
    @PutMapping("/{id}")
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid OrganizeUpForm organizeUpForm) {
        List<OrganizeEntity> synList = new ArrayList<>();
        OrganizeEntity entity = JsonUtil.getJsonToBean(organizeUpForm, OrganizeEntity.class);
        entity.setId(id);
        OrganizeEntity info = organizeService.getInfo(id);
        if (info == null) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        OrganizeEntity parentInfo = organizeService.getInfo(organizeUpForm.getParentId());
        //不能放到自己下级组织内
        if (id.equals(entity.getParentId()) || (parentInfo != null && parentInfo.getOrganizeIdTree() != null && parentInfo.getOrganizeIdTree().contains(id))) {
            return ActionResult.fail(MsgCode.SYS146.get(MsgCode.PS003.get()));
        }
        if (!Objects.equals(info.getParentId(), organizeUpForm.getParentId())) {
            List<String> stepList = workFlowApi.getStepList();
            if (stepList.contains(id)) {
                return ActionResult.fail(MsgCode.OA029.get());
            }
        }

        entity.setId(id);
        if (organizeService.isExistByFullName(entity, false, true)) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (organizeService.isExistByEnCode(entity.getEnCode(), id)) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        //验证层级
        if (!organizeService.checkLevel(entity)) {
            return ActionResult.fail(MsgCode.PS036.get(sysconfigApi.getSysInfo().getOrgLevel()));
        }
        //验证下级类型
        if (!Objects.equals(info.getCategory(), entity.getCategory()) && !organizeService.checkOrgType(entity)) {
            return ActionResult.fail(MsgCode.PS037.get());
        }
        boolean flag = organizeService.update(id, entity);
        synList.add(entity);

        // 父级id或者组织名称变化则更新下级所有树形信息
        if (!Objects.equals(entity.getParentId(), info.getParentId()) || !Objects.equals(entity.getFullName(), info.getFullName())) {
            List<String> underOrganizations = organizeService.getUnderOrganizations(id, false);
            underOrganizations.forEach(t -> {
                OrganizeEntity info1 = organizeService.getInfo(t);
                if (StringUtil.isNotEmpty(info1.getOrganizeIdTree())) {
                    organizeService.setOrgTreeIdAndName(info1);
                    organizeService.update(info1.getId(), info1);
                    synList.add(info1);
                }
            });
        }
        ThreadPoolExecutorUtil.getExecutor().execute(() ->
                synList.forEach(this::syncDepartment)
        );
        if (!flag) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    // 单独定义同步方法
    private void syncDepartment(OrganizeEntity t) {
        try {
            //修改组织后判断是否需要同步到企业微信
            synThirdQyService.unifyDepartmentSysToQy(false, t, "", SynThirdConsts.UPDATE_DEP);
            //修改组织后判断是否需要同步到钉钉
            synThirdDingTalkService.unifyDepartmentSysToDing(false, t, "", SynThirdConsts.UPDATE_DEP);
        } catch (Exception e) {
            log.error("修改组织后同步失败到企业微信或钉钉失败，异常：{}", e.getMessage());
        }
    }

    @Operation(summary = "获取组织信息")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission(value = {"permission.organize"})
    @GetMapping("/{id}")
    public ActionResult<OrganizeInfoVO> info(@PathVariable("id") String id) throws DataException {
        OrganizeEntity entity = organizeService.getInfo(id);
        OrganizeInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, OrganizeInfoVO.class);
        if (StringUtil.isNotEmpty(entity.getParentId())) {
            OrganizeEntity pInfo = organizeService.getInfo(entity.getParentId());
            if (pInfo != null) {
                vo.setParentName(pInfo.getFullName());
                vo.setParentCategory(pInfo.getCategory());
            }
        }
        if (StringUtil.isNotEmpty(entity.getOrganizeIdTree())) {
            String replace = entity.getOrganizeIdTree().replace(entity.getId(), "");
            if (StringUtil.isNotEmpty(replace) && !",".equals(replace)) {
                vo.setOrganizeIdTree(Arrays.asList(replace.split(",")));
            } else {
                vo.setOrganizeIdTree(Collections.singletonList("-1"));
            }
        }
        return ActionResult.success(vo);
    }

    @Operation(summary = "删除组织")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission(value = {"permission.organize"})
    @DeleteMapping("/{id}")
    public ActionResult<String> delete(@PathVariable("id") String orgId) {
        List<OrganizeEntity> organizeEntities = organizeService.delete(orgId);
        ThreadPoolExecutorUtil.getExecutor().execute(() -> {
            try {
                //删除部门后判断是否需要同步到企业微信
                synThirdQyService.unifyDepartmentSysToQy(false, organizeEntities, "", SynThirdConsts.DELETE_DEP);
                //删除部门后判断是否需要同步到钉钉
                synThirdDingTalkService.unifyDepartmentSysToDing(false, organizeEntities, "", SynThirdConsts.DELETE_DEP);
            } catch (Exception e) {
                log.error("删除部门后同步失败到企业微信或钉钉失败，异常：" + e.getMessage());
            }
        });
        return ActionResult.success(MsgCode.SU003.get());
    }

    //+++++++++++++++++++++++++++++++++++其他接口+++++++++++++++++++++++++++++

    @Operation(summary = "获取组织列表")
    @Parameter(name = "id", description = "主键", required = true)
    @GetMapping("/Selector")
    public ActionResult<PageListVO<OrganizeSelectorVO>> getList(OrganizePagination pagination) {
        pagination.setDataType(1);
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            pagination.setParentId(null);
            pagination.setDataType(null);
        }
        List<OrganizeEntity> list = organizeService.getList(pagination);
        Map<String, Object> allOrgsTreeName = organizeService.getAllOrgsTreeName();
        List<OrganizeSelectorVO> listVO = JsonUtil.getJsonToList(list, OrganizeSelectorVO.class);
        for (OrganizeSelectorVO item : listVO) {
            if (PermissionConst.DEPARTMENT.equals(item.getCategory())) {
                item.setIcon(PermissionConst.DEPARTMENT_ICON);
            } else {
                item.setIcon(PermissionConst.COMPANY_ICON);
            }
            if (StringUtil.isNotEmpty(pagination.getKeyword())) {
                item.setFullName(item.getOrgNameTree());
            } else {
                item.setHasChildren(true);
            }
            if (StringUtil.isNotEmpty(item.getParentId()) && Objects.nonNull(allOrgsTreeName.get(item.getParentId()))) {
                item.setParentName(allOrgsTreeName.get(item.getParentId()).toString());
            }
            String[] orgs = item.getOrganizeIdTree().split(",");
            item.setOrganizeIds(Arrays.asList(orgs));

        }
        PaginationVO jsonToBean = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(listVO, jsonToBean);
    }

    @Operation(summary = "自定义范围回显")
    @Parameter(name = "userIdModel", description = "参数模型")
    @PostMapping("/SelectedList")
    public ActionResult<ListVO<OrganizeListVO>> selectedList(@RequestBody UserIdModel userIdModel) {
        List<OrganizeListVO> list = organizeService.selectedList(userIdModel.getIds());
        ListVO<OrganizeListVO> listVO = new ListVO<>();
        listVO.setList(list);
        return ActionResult.success(listVO);
    }

    @Operation(summary = "自定义范围下拉")
    @Parameter(name = "userIdModel", description = "参数模型")
    @PostMapping("/OrganizeCondition")
    public ActionResult<ListVO<OrganizeListVO>> organizeCondition(@RequestBody UserIdModel userIdModel) {
        List<OrganizeEntity> list = organizeService.organizeCondition(userIdModel.getIds());
        List<OrganizeModel> modelList = JsonUtil.getJsonToList(list, OrganizeModel.class);
        for (OrganizeModel item : modelList) {
            if (PermissionConst.DEPARTMENT.equals(item.getCategory())) {
                item.setIcon(PermissionConst.DEPARTMENT_ICON);
            } else {
                item.setIcon(PermissionConst.COMPANY_ICON);
            }
        }
        List<SumTree<OrganizeModel>> trees = TreeDotUtils.convertListToTreeDot(modelList);
        List<OrganizeListVO> voList = JsonUtil.getJsonToList(trees, OrganizeListVO.class);
        for (OrganizeListVO item : voList) {
            item.setFullName(item.getOrgNameTree());
        }
        ListVO<OrganizeListVO> listVO = new ListVO<>();
        listVO.setList(voList);
        return ActionResult.success(listVO);
    }


    @Operation(summary = "组织异步带岗位")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "type", description = "类型：organize、position", required = true)
    @GetMapping("/AsyncPosList")
    public ActionResult<PageListVO<OrganizePositonVO>> asyncPosList(@RequestParam("id") String id, @RequestParam(name = "type") String type) {
        List<OrganizePositonVO> listVO = new ArrayList<>();
        //不是组织就是岗位
        type = StringUtil.isNotEmpty(type) && PermissionConst.ORGANIZE.equals(type) ? PermissionConst.ORGANIZE : PermissionConst.POSITION;
        Map<String, Object> allOrgsTreeName = organizeService.getAllOrgsTreeName();

        if (PermissionConst.ORGANIZE.equals(type)) {
            OrganizeEntity info = organizeService.getInfo(id);
            //添加岗位
            if (!Objects.equals(id, "0")) {
                String dutyPosition = info.getDutyPosition();
                List<PositionEntity> posList = positionService.getListByOrgIdOneLevel(Collections.singletonList(id));
                List<OrganizePositonVO> posListVo = JsonUtil.getJsonToList(posList, OrganizePositonVO.class);
                posListVo.forEach(item -> {
                    item.setType(PermissionConst.POSITION);
                    item.setIcon(PermissionConst.POSITION_ICON);
                    item.setHasChildren(true);
                    item.setOrganize(String.valueOf(allOrgsTreeName.get(item.getOrganizeId())));
                    item.setOrgNameTree(item.getOrganize() + "/" + item.getFullName());
                    if (StringUtil.isNotEmpty(dutyPosition) && dutyPosition.equals(item.getId())) {
                        item.setIsDutyPosition(1);
                    }
                });
                listVO.addAll(posListVo);
            }
            //添加组织
            OrganizePagination pagination = new OrganizePagination();
            pagination.setDataType(1);
            pagination.setParentId(id);
            List<OrganizeEntity> orgList = organizeService.getList(pagination);
            List<OrganizePositonVO> listOrgVO = JsonUtil.getJsonToList(orgList, OrganizePositonVO.class);
            for (OrganizePositonVO item : listOrgVO) {
                item.setType(PermissionConst.ORGANIZE);
                if (PermissionConst.DEPARTMENT.equals(item.getCategory())) {
                    item.setIcon(PermissionConst.DEPARTMENT_ICON);
                } else {
                    item.setIcon(PermissionConst.COMPANY_ICON);
                }
                item.setHasChildren(true);
            }
            listVO.addAll(listOrgVO);
        } else {
            List<PositionEntity> posList = positionService.getByParentId(id);
            listVO.addAll(JsonUtil.getJsonToList(posList, OrganizePositonVO.class));
            for (OrganizePositonVO item : listVO) {
                item.setType(PermissionConst.POSITION);
                item.setIcon(PermissionConst.POSITION_ICON);
                item.setHasChildren(true);
                item.setOrganize(String.valueOf(allOrgsTreeName.get(item.getOrganizeId())));
                item.setOrgNameTree(item.getOrganize() + "/" + item.getFullName());
            }
        }

        return ActionResult.page(listVO, null);
    }

    // -------------------导入导出--------------------

    @Operation(summary = "模板下载")
    @SaCheckPermission("permission.organize")
    @GetMapping("/TemplateDownload")
    public ActionResult<DownloadVO> templateDownload() {
        OrgColumnMap columnMap = new OrgColumnMap();
        String excelName = columnMap.getExcelName();
        Map<String, String> keyMap = columnMap.getColumnByType(0);
        List<ExcelColumnAttr> models = columnMap.getFieldsModel(false, 0);
        List<Map<String, Object>> list = columnMap.getDefaultList();
        Map<String, String[]> optionMap = getOptionMap();
        ExcelModel excelModel = ExcelModel.builder().models(models).selectKey(new ArrayList<>(keyMap.keySet())).optionMap(optionMap).build();
        DownloadVO vo = ExcelTool.getImportTemplate(FileTypeConstant.TEMPORARY, excelName, keyMap, list, excelModel);
        return ActionResult.success(vo);
    }


    @Operation(summary = "上传导入Excel")
    @SaCheckPermission("permission.organize")
    @PostMapping("/Uploader")
    public ActionResult<Object> uploader() {
        return ExcelTool.uploader();
    }

    @Operation(summary = "导入预览")
    @SaCheckPermission("permission.organize")
    @GetMapping("/ImportPreview")
    public ActionResult<Map<String, Object>> importPreview(String fileName) {
        // 导入字段
        OrgColumnMap columnMap = new OrgColumnMap();
        Map<String, String> keyMap = columnMap.getColumnByType(0);
        Map<String, Object> headAndDataMap = ExcelTool.importPreview(FileTypeConstant.TEMPORARY, fileName, keyMap);
        return ActionResult.success(headAndDataMap);
    }

    @Operation(summary = "导出异常报告")
    @SaCheckPermission("permission.organize")
    @PostMapping("/ExportExceptionData")
    public ActionResult<DownloadVO> exportExceptionData(@RequestBody ExcelImportForm visualImportModel) {
        List<Map<String, Object>> dataList = visualImportModel.getList();
        OrgColumnMap columnMap = new OrgColumnMap();
        String excelName = columnMap.getExcelName();
        Map<String, String> keyMap = columnMap.getColumnByType(0);
        List<ExcelColumnAttr> models = columnMap.getFieldsModel(true, 0);
        ExcelModel excelModel = ExcelModel.builder().optionMap(getOptionMap()).models(models).build();
        DownloadVO vo = ExcelTool.exportExceptionReport(FileTypeConstant.TEMPORARY, excelName, keyMap, dataList, excelModel);
        return ActionResult.success(vo);
    }

    @Operation(summary = "导入数据")
    @SaCheckPermission("permission.organize")
    @PostMapping("/ImportData")
    public ActionResult<ExcelImportVO> importData(@RequestBody ExcelImportForm visualImportModel){
        List<Map<String, Object>> listData = new ArrayList<>();
        List<Map<String, Object>> headerRow = new ArrayList<>();
        if (visualImportModel.isType()) {
            ActionResult<Map<String, Object>> result = importPreview(visualImportModel.getFileName());
            if (result == null) {
                return ActionResult.fail(MsgCode.FA018.get());
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
        List<OrganizeEntity> addList = new ArrayList<>();
        List<Map<String, Object>> failList = new ArrayList<>();
        // 对数据做校验
        this.validateImportData(listData, addList, failList);

        //正常数据插入
        for (OrganizeEntity each : addList) {
            organizeService.create(each);
        }
        ExcelImportVO importModel = new ExcelImportVO();
        importModel.setSnum(addList.size());
        importModel.setFnum(failList.size());
        importModel.setResultType(!failList.isEmpty() ? 1 : 0);
        importModel.setFailResult(failList);
        importModel.setHeaderRow(headerRow);
        return ActionResult.success(importModel);
    }

    @Operation(summary = "导出Excel")
    @SaCheckPermission("permission.organize")
    @GetMapping("/ExportData")
    public ActionResult<Object> exportData(OrganizePagination pagination) {
        //获取当前用户的所有可见组织
        pagination.setDataType(1);
        List<OrganizeEntity> list = organizeService.getList(pagination);
        List<OrganizeEntity> allOrg = organizeService.getList(false);
        Map<String, String> idNameMap = allOrg.stream().collect(Collectors.toMap(OrganizeEntity::getId,
                e -> e.getFullName() + "/" + e.getEnCode(), (existing, replacement) -> existing));

        List<Map<String, Object>> realList = JsonUtil.getJsonToListMap(JsonUtil.getObjectToString(list));
        for (Map<String, Object> item : realList) {
            if (item.get(KeyConst.PARENT_ID) != null && !"-1".equals(item.get(KeyConst.PARENT_ID).toString())) {
                item.put(KeyConst.PARENT_ID, idNameMap.get(item.get(KeyConst.PARENT_ID)));
            } else {
                item.put(KeyConst.PARENT_ID, "");
            }
            item.put(KeyConst.CATEGORY, OrgTypeEnum.get(item.get(KeyConst.CATEGORY).toString()).getName());
        }

        //获取字段
        OrgColumnMap columnMap = new OrgColumnMap();
        String excelName = columnMap.getExcelName();
        Map<String, String> keyMap = columnMap.getColumnByType(0);
        List<ExcelColumnAttr> models = columnMap.getFieldsModel(true, 0);
        ExcelModel excelModel = ExcelModel.builder().models(models).selectKey(new ArrayList<>(keyMap.keySet())).build();
        DownloadVO vo = ExcelTool.creatModelExcel(FileTypeConstant.TEMPORARY, excelName, keyMap, realList, excelModel);
        return ActionResult.success(vo);
    }

    /**
     * 字段验证
     *
     * @param listData
     * @param addList
     * @param failList
     */
    private void validateImportData(List<Map<String, Object>> listData, List<OrganizeEntity> addList, List<Map<String, Object>> failList) {
        OrgColumnMap columnMap = new OrgColumnMap();
        Map<String, String> keyMap = columnMap.getColumnByType(0);
        Integer orgLevel = sysconfigApi.getSysInfo().getOrgLevel();

        List<OrganizeEntity> allOrg = organizeService.getList(false);
        Map<String, String> nameCodeMap = createNameCodeMap(allOrg);

        for (int i = 0, len = listData.size(); i < len; i++) {
            processSingleRecord(listData.get(i), keyMap, orgLevel, allOrg, addList, failList, nameCodeMap);
        }
    }

    private Map<String, String> createNameCodeMap(List<OrganizeEntity> allOrg) {
        return allOrg.stream().collect(Collectors.toMap(
                t -> t.getFullName() + "/" + t.getEnCode(),
                OrganizeEntity::getId
        ));
    }

    //处理单条记录
    private void processSingleRecord(Map<String, Object> eachMap, Map<String, String> keyMap, Integer orgLevel,
                                     List<OrganizeEntity> allOrg, List<OrganizeEntity> addList,
                                     List<Map<String, Object>> failList, Map<String, String> nameCodeMap) {
        Map<String, Object> realMap = JsonUtil.getJsonToBean(eachMap, Map.class);
        StringJoiner errInfo = new StringJoiner(",");

        ParentOrgInfo parentInfo = processParentOrg(eachMap, allOrg, addList, orgLevel, nameCodeMap, errInfo);
        if (errInfo.length() > 0) {
            handleValidationError(eachMap, errInfo.toString(), failList);
            return;
        }

        updateRealMapWithParentInfo(realMap, parentInfo);
        validateColumns(eachMap, realMap, keyMap, parentInfo, allOrg, addList, errInfo);

        if (errInfo.length() == 0) {
            addValidOrganize(realMap, addList, nameCodeMap);
        } else {
            handleValidationError(eachMap, errInfo.toString(), failList);
        }
    }

    //处理父级组织信息
    private ParentOrgInfo processParentOrg(Map<String, Object> eachMap, List<OrganizeEntity> allOrg,
                                           List<OrganizeEntity> addList, Integer orgLevel,
                                           Map<String, String> nameCodeMap, StringJoiner errInfo) {
        ParentOrgInfo parentInfo = new ParentOrgInfo();

        if (eachMap.get(KeyConst.PARENT_ID) != null) {
            String parentName = eachMap.get(KeyConst.PARENT_ID).toString();
            String parentId = nameCodeMap.get(parentName);

            if (StringUtil.isEmpty(parentId)) {
                errInfo.add("找不到上级组织");
                return parentInfo;
            }

            OrganizeEntity parentOrg = findParentOrg(allOrg, addList, parentId);
            if (parentOrg != null) {
                parentInfo.parentId = parentId;
                parentInfo.childType = OrgTypeEnum.get(parentOrg.getCategory()).getChildType();
                parentInfo.organizeIdTree = buildOrganizeIdTree(parentOrg, addList);

                if (parentInfo.organizeIdTree.split(",").length > orgLevel) {
                    errInfo.add(MsgCode.PS036.get(orgLevel));
                }
            }
        } else {
            parentInfo.parentId = "-1";
            parentInfo.childType = Arrays.asList(OrgTypeEnum.GROUP.getCode(), OrgTypeEnum.COMPANY.getCode(),
                    OrgTypeEnum.AGENCY.getCode(), OrgTypeEnum.OFFICE.getCode(),
                    OrgTypeEnum.UNIT.getCode());
        }

        return parentInfo;
    }

    private OrganizeEntity findParentOrg(List<OrganizeEntity> allOrg, List<OrganizeEntity> addList, String parentId) {
        OrganizeEntity parentOrg = allOrg.stream()
                .filter(t -> t.getId().equals(parentId))
                .findFirst()
                .orElse(null);

        if (parentOrg == null) {
            parentOrg = addList.stream()
                    .filter(t -> t.getId().equals(parentId))
                    .findFirst()
                    .orElse(null);
        }

        return parentOrg;
    }

    private String buildOrganizeIdTree(OrganizeEntity parentOrg, List<OrganizeEntity> addList) {
        if (addList.contains(parentOrg)) {
            return parentOrg.getOrganizeIdTree() + "," + parentOrg.getId();
        }
        return parentOrg.getOrganizeIdTree();
    }

    private void updateRealMapWithParentInfo(Map<String, Object> realMap, ParentOrgInfo parentInfo) {
        realMap.put(KeyConst.PARENT_ID, parentInfo.parentId);
        realMap.put("organizeIdTree", parentInfo.organizeIdTree);
    }

    //验证各个字段
    private void validateColumns(Map<String, Object> eachMap, Map<String, Object> realMap,
                                 Map<String, String> keyMap, ParentOrgInfo parentInfo,
                                 List<OrganizeEntity> allOrg, List<OrganizeEntity> addList,
                                 StringJoiner errInfo) {
        for (Map.Entry<String, String> item : keyMap.entrySet()) {
            String column = item.getKey();
            String columnName = item.getValue();
            Object valueObj = eachMap.get(column);
            String value = valueObj == null ? null : String.valueOf(valueObj);

            switch (column) {
                case KeyConst.CATEGORY:
                    validateCategory(eachMap, realMap, parentInfo, errInfo, columnName);
                    break;
                case "fullName":
                    if (StringUtils.isEmpty(value)) {
                        errInfo.add(columnName + "不能为空");
                    }else {
                        validateFullName(value, realMap, parentInfo, allOrg, addList, errInfo, columnName);
                    }

                    break;
                case "enCode":
                    if (StringUtils.isNotEmpty(value)) {
                        validateEnCode(value, allOrg, addList, errInfo, columnName);
                    }
                    break;
                case "sortCode":
                    validateSortCode(value, realMap, errInfo, columnName);
                    break;
                default:
                    break;
            }
        }
    }

    private void validateCategory(Map<String, Object> eachMap, Map<String, Object> realMap,
                                  ParentOrgInfo parentInfo, StringJoiner errInfo, String columnName) {
        String category = "";
        if (eachMap.get(KeyConst.CATEGORY) != null && StringUtil.isNotEmpty(eachMap.get(KeyConst.CATEGORY).toString())) {
            category = OrgTypeEnum.getByName(eachMap.get(KeyConst.CATEGORY).toString()).getCode();
        } else {
            errInfo.add(columnName + "不能为空");
            return;
        }

        if (!parentInfo.childType.contains(category)) {
            errInfo.add("组织类型异常，上级组织不允许添加该组织类型");
            return;
        }

        realMap.put(KeyConst.CATEGORY, category);
    }

    private void validateFullName(String value, Map<String, Object> realMap, ParentOrgInfo parentInfo,
                                  List<OrganizeEntity> allOrg, List<OrganizeEntity> addList,
                                  StringJoiner errInfo, String columnName) {
        if (StringUtils.isEmpty(value)) {
            errInfo.add(columnName + "不能为空");
            return;
        }

        if (50 < value.length()) {
            errInfo.add(columnName + "值超出最多输入字符限制");
            return;
        }

        long fullNameCount = allOrg.stream()
                .filter(t -> t.getParentId().equals(parentInfo.parentId) && t.getFullName().equals(value))
                .count();
        if (fullNameCount > 0) {
            errInfo.add(columnName + "值已存在");
            return;
        }

        fullNameCount = addList.stream()
                .filter(t -> t.getParentId().equals(parentInfo.parentId) && t.getFullName().equals(value))
                .count();
        if (fullNameCount > 0) {
            errInfo.add(columnName + "值已存在");
            return;
        }

        realMap.put("fullName", value);
    }

    private void validateEnCode(String value, List<OrganizeEntity> allOrg, List<OrganizeEntity> addList,
                                StringJoiner errInfo, String columnName) {
        if (StringUtil.isEmpty(value)) {
            return;
        }

        if (value.length() > 50) {
            errInfo.add(columnName + "值超出最多输入字符限制");
            return;
        }

        if (!RegexUtils.checkEnCode(value)) {
            errInfo.add(columnName + "只能输入英文、数字和小数点且小数点不能放在首尾");
            return;
        }

        long enCodeCount = allOrg.stream().filter(t -> t.getEnCode().equals(value)).count();
        if (enCodeCount > 0) {
            errInfo.add(columnName + "值已存在");
            return;
        }

        enCodeCount = addList.stream().filter(t -> value.equals(t.getEnCode())).count();
        if (enCodeCount > 0) {
            errInfo.add(columnName + "值已存在");
        }
    }

    private void validateSortCode(String value, Map<String, Object> realMap, StringJoiner errInfo, String columnName) {
        if (StringUtil.isEmpty(value)) {
            realMap.put("sortCode", 0);
            return;
        }

        long numValue;
        try {
            numValue = Long.parseLong(value);
        } catch (Exception e) {
            errInfo.add(columnName + "值不正确");
            return;
        }

        if (numValue < 0) {
            errInfo.add(columnName + "值不能小于0");
            return;
        }

        if (numValue > 1000000) {
            errInfo.add(columnName + "值不能大于999999");
        }
    }

    private void addValidOrganize(Map<String, Object> realMap, List<OrganizeEntity> addList, Map<String, String> nameCodeMap) {
        OrganizeEntity organizeEntity = JsonUtil.getJsonToBean(realMap, OrganizeEntity.class);
        organizeEntity.setCreatorTime(new Date());
        String uuid = RandomUtil.uuId();
        organizeEntity.setId(uuid);

        if (StringUtil.isNotEmpty(organizeEntity.getEnCode())) {
            nameCodeMap.put(organizeEntity.getFullName() + "/" + organizeEntity.getEnCode(), organizeEntity.getId());
        }

        addList.add(organizeEntity);
    }

    private void handleValidationError(Map<String, Object> eachMap, String errorInfo, List<Map<String, Object>> failList) {
        eachMap.put("errorsInfo", errorInfo);
        failList.add(eachMap);
    }

    // 内部类用于传递父级组织信息
    private static class ParentOrgInfo {
        String parentId;
        List<String> childType;
        String organizeIdTree = "";
    }

    /**
     * 获取下拉框
     *
     * @return
     */
    private Map<String, String[]> getOptionMap() {
        Map<String, String[]> optionMap = new HashMap<>();
        //类型
        String[] typeMap = Arrays.stream(OrgTypeEnum.values()).map(OrgTypeEnum::getName).toArray(String[]::new);
        optionMap.put(KeyConst.CATEGORY, typeMap);
        return optionMap;
    }

}
