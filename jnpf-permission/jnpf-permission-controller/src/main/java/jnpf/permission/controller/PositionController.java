package jnpf.permission.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.collection.CollUtil;
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
import jnpf.exception.DataException;
import jnpf.model.ExcelColumnAttr;
import jnpf.model.ExcelImportForm;
import jnpf.model.ExcelImportVO;
import jnpf.model.ExcelModel;
import jnpf.permission.constant.PosColumnMap;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.entity.UserRelationEntity;
import jnpf.permission.model.permission.PermissionModel;
import jnpf.permission.model.position.*;
import jnpf.permission.model.user.mod.UserIdModel;
import jnpf.permission.service.OrganizeService;
import jnpf.permission.service.PositionService;
import jnpf.permission.service.UserRelationService;
import jnpf.permission.service.UserService;
import jnpf.util.*;
import jnpf.util.treeutil.SumTree;
import jnpf.util.treeutil.newtreeutil.TreeDotUtils;
import jnpf.workflow.service.WorkFlowApi;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 岗位信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Tag(name = "岗位管理", description = "Position")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permission/Position")
public class PositionController extends SuperController<PositionService, PositionEntity> {

    private final UserService userService;
    private final PositionService positionService;
    private final OrganizeService organizeService;
    private final SysconfigService sysconfigApi;
    private final UserRelationService userRelationService;
    private final WorkFlowApi workFlowApi;

    @Operation(summary = "获取岗位列表")
    @GetMapping
    public ActionResult<PageListVO<PositionListVO>> list(PositionPagination pagination) {
        pagination.setDataType(1);
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            pagination.setDataType(null);
        }
        List<PositionEntity> data = positionService.getList(pagination);
        List<PositionTreeModel> list = JsonUtil.getJsonToList(data, PositionTreeModel.class);
        String dutyPost = "";
        if (StringUtil.isNotEmpty(pagination.getOrganizeId())) {
            OrganizeEntity org = organizeService.getInfo(pagination.getOrganizeId());
            dutyPost = org.getDutyPosition();
        }
        for (PositionTreeModel item : list) {
            if (StringUtil.isNotEmpty(dutyPost) && Objects.equals(dutyPost, item.getId())) {
                item.setIsDutyPosition(1);
            }
            //可设置责任岗位
            if (StringUtil.isEmpty(item.getParentId()) && !Objects.equals(item.getIsDutyPosition(), 1)) {
                item.setAllowDuty(1);
            }
            item.setIcon(PermissionConst.POSITION_ICON);
        }
        List<SumTree<PositionTreeModel>> trees = TreeDotUtils.convertListToTreeDot(list);
        List<PositionListVO> voList = JsonUtil.getJsonToList(trees, PositionListVO.class);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(voList, paginationVO);
    }

    @Operation(summary = "新建岗位")
    @Parameter(name = "positionCrForm", description = "实体对象", required = true)
    @SaCheckPermission("permission.organize")
    @PostMapping
    public ActionResult<Object> create(@RequestBody @Valid PositionCrForm positionCrForm) {
        PositionEntity entity = JsonUtil.getJsonToBean(positionCrForm, PositionEntity.class);
        if (positionService.isExistByFullName(entity, false)) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (Boolean.TRUE.equals(positionService.isExistByEnCode(entity.getEnCode(), null))) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        if (!positionService.checkLevel(entity)) {
            return ActionResult.fail(MsgCode.PS038.get(sysconfigApi.getSysInfo().getPositionLevel()));
        }
        PosConModel posConModel = new PosConModel();
        //约束判断
        if (Objects.equals(entity.getIsCondition(), 1)) {
            posConModel = JsonUtil.getJsonToBean(entity.getConditionJson(), PosConModel.class);
            posConModel.init();
            String errStr = posConModel.checkCondition(entity.getId());
            if (StringUtil.isNotEmpty(errStr)) {
                return ActionResult.fail(errStr);
            }
            if (posConModel.getPrerequisiteFlag()) {
                for (String item : posConModel.getPrerequisite()) {
                    PositionEntity itemInfo = positionService.getInfo(item);
                    //先决只能1级（选中的先决岗位，这个岗位不能有先决条件，有的多就变成多级了）
                    if (Objects.equals(itemInfo.getIsCondition(), 1)) {
                        PosConModel itemModel = JsonUtil.getJsonToBean(itemInfo.getConditionJson(), PosConModel.class);
                        itemModel.init();
                        if (itemModel.getPrerequisiteFlag()) {
                            return ActionResult.fail(MsgCode.SYS143.get());
                        }
                    }
                }
            }
        }
        positionService.create(entity);
        positionService.linkUpdate(entity.getId(), posConModel);
        return ActionResult.success(MsgCode.SU001.get());
    }

    @Operation(summary = "修改岗位")
    @Parameter(name = "id", description = "主键值", required = true)
    @Parameter(name = "positionUpForm", description = "实体对象", required = true)
    @SaCheckPermission("permission.organize")
    @PutMapping("/{id}")
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid PositionUpForm positionUpForm) {
        PositionEntity info = positionService.getInfo(id);
        PositionEntity entity = JsonUtil.getJsonToBean(positionUpForm, PositionEntity.class);
        entity.setId(id);
        if (info == null) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        PositionEntity parentInfo = positionService.getInfo(entity.getParentId());
        //不能放到自己下级内
        if (id.equals(entity.getParentId()) || (parentInfo != null && parentInfo.getPositionIdTree() != null && parentInfo.getPositionIdTree().contains(id))) {
            return ActionResult.fail(MsgCode.SYS146.get(MsgCode.PS004.get()));
        }
        //验证流程是否逐级审批
        if (!Objects.equals(info.getOrganizeId(), positionUpForm.getOrganizeId()) || !Objects.equals(info.getParentId(), positionUpForm.getParentId())) {
            List<String> stepList = workFlowApi.getStepList();
            if (!Objects.equals(info.getOrganizeId(), positionUpForm.getOrganizeId())) {
                if (stepList.contains(positionUpForm.getOrganizeId())) {
                    return ActionResult.fail(MsgCode.PS041.get());
                }
            } else {
                if (stepList.contains(id)) {
                    return ActionResult.fail(MsgCode.PS041.get());
                }
            }
        }

        if (positionService.isExistByFullName(entity, true)) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (Boolean.TRUE.equals(positionService.isExistByEnCode(entity.getEnCode(), id))) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        if (!positionService.checkLevel(entity)) {
            return ActionResult.fail(MsgCode.PS038.get(sysconfigApi.getSysInfo().getPositionLevel()));
        }
        //约束判断
        PosConModel posConModel = new PosConModel();
        if (Objects.equals(entity.getIsCondition(), 1)) {
            posConModel = JsonUtil.getJsonToBean(entity.getConditionJson(), PosConModel.class);
            posConModel.init();
            String errStr = posConModel.checkCondition(entity.getId());
            if (StringUtil.isNotEmpty(errStr)) {
                return ActionResult.fail(errStr);
            }
            //修改岗位的时候，岗位有人的话，互斥岗位（选择的岗位下的人一个都不能重叠），先决岗位（选择的岗位下的人必须包含当前岗位人员）
            Set<String> userIds = userRelationService.getListByObjectId(entity.getId(), PermissionConst.POSITION)
                    .stream().map(UserRelationEntity::getUserId).collect(Collectors.toSet());
            //获取全部岗位的先决岗位列表。用于判断当前岗位是否开启先决（存在就不能开启）
            List<PositionEntity> list = positionService.getList(false);
            Map<String, PositionEntity> posMap = list.stream().collect(Collectors.toMap(t -> t.getId(), t -> t));

            if (posConModel.getMutualExclusionFlag()) {
                for (String item : posConModel.getMutualExclusion()) {
                    Set<String> itemUserIds = userRelationService.getListByObjectId(item, PermissionConst.POSITION)
                            .stream().map(UserRelationEntity::getUserId).collect(Collectors.toSet());
                    Set<String> commonIds = new HashSet<>(userIds);
                    commonIds.retainAll(itemUserIds);
                    //用户冲突，互斥修改失败
                    if (!commonIds.isEmpty()) {
                        return ActionResult.fail(MsgCode.SYS137.get());
                    }
                    //当前岗位A添加互斥岗位B，B里的先决不能有A，如果有会导致添加成功后B岗位的先决和互斥冲突
                    PositionEntity itemEntity = posMap.get(item);
                    if (itemEntity != null && StringUtil.isNotEmpty(itemEntity.getConditionJson())) {
                        PosConModel tm = JsonUtil.getJsonToBean(itemEntity.getConditionJson(), PosConModel.class);
                        tm.init();
                        if (tm.getPrerequisite() != null && tm.getPrerequisite().contains(id)) {
                            return ActionResult.fail(MsgCode.SYS142.get());
                        }
                    }
                }
            }
            if (posConModel.getPrerequisiteFlag()) {
                Set<String> allPrePos = new HashSet<>();
                for (PositionEntity t : list) {
                    if (Objects.equals(t.getIsCondition(), 1)) {
                        PosConModel tm = JsonUtil.getJsonToBean(t.getConditionJson(), PosConModel.class);
                        tm.init();
                        if (tm.getPrerequisiteFlag()) {
                            allPrePos.addAll(tm.getPrerequisite());
                        }
                    }
                }
                if (allPrePos.contains(id)) {
                    return ActionResult.fail(MsgCode.SYS143.get());
                }

                //用户冲突--当前岗位里有用户就要判断先决包含先决的所有岗位
                if (CollUtil.isNotEmpty(userIds)) {
                    for (String userId : userIds) {
                        Set<String> posIds = userRelationService.getListByUserId(userId, PermissionConst.POSITION)
                                .stream().map(UserRelationEntity::getObjectId).collect(Collectors.toSet());
                        //用户冲突，先决修改失败
                        if (!posIds.containsAll(posConModel.getPrerequisite())) {
                            return ActionResult.fail(MsgCode.SYS138.get());
                        }
                    }
                }

                //先决只能1级（选中的先决岗位，这个岗位不能有先决条件，有的多就变成多级了）
                for (String item : posConModel.getPrerequisite()) {
                    PositionEntity itemInfo = positionService.getInfo(item);
                    if (Objects.equals(itemInfo.getIsCondition(), 1)) {
                        PosConModel itemModel = JsonUtil.getJsonToBean(itemInfo.getConditionJson(), PosConModel.class);
                        itemModel.init();
                        if (itemModel.getPrerequisiteFlag()) {
                            return ActionResult.fail(MsgCode.SYS143.get());
                        }
                    }
                }
            }
        }

        boolean flag = positionService.update(id, entity);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        positionService.linkUpdate(id, posConModel);
        // 得到所有子组织或部门id
        if (!Objects.equals(info.getParentId(), entity.getParentId())) {
            List<PositionEntity> allChild = positionService.getAllChild(id);
            allChild.forEach(t -> {
                if (!t.getId().equals(id)) {
                    String[] split = t.getPositionIdTree().split(id);
                    t.setPositionIdTree(entity.getPositionIdTree() + split[1]);
                    t.setOrganizeId(entity.getOrganizeId());
                    positionService.update(t.getId(), t);
                }
            });
            //将岗位绑定的同数量的组织调整成新组织
            List<String> positionIds = allChild.stream().map(PositionEntity::getId).collect(Collectors.toList());
            userRelationService.updateOrgToNew(positionIds, info.getOrganizeId(), positionUpForm.getOrganizeId());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    @Operation(summary = "获取岗位信息")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("permission.organize")
    @GetMapping("/{id}")
    public ActionResult<PositionInfoVO> getInfo(@PathVariable("id") String id) throws DataException {
        PositionEntity entity = positionService.getInfo(id);
        PositionInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, PositionInfoVO.class);
        Map<String, Object> orgMap = organizeService.getOrgMap();
        List<PositionEntity> list = positionService.getList(false);
        Set<String> allPrePos = new HashSet<>();
        for (PositionEntity t : list) {
            if (Objects.equals(t.getIsCondition(), 1)) {
                PosConModel tm = JsonUtil.getJsonToBean(t.getConditionJson(), PosConModel.class);
                tm.init();
                if (tm.getPrerequisiteFlag()) {
                    allPrePos.addAll(tm.getPrerequisite());
                }
            }
        }
        if (allPrePos.contains(id)) {
            vo.setIsPrePosition(true);
        }
        if (orgMap.containsKey(vo.getOrganizeId())) {
            vo.setOrganizeName(orgMap.get(vo.getOrganizeId()).toString());
        }
        return ActionResult.success(vo);
    }

    @Operation(summary = "删除岗位")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("permission.organize")
    @DeleteMapping("/{id}")
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        PositionEntity entity = positionService.getInfo(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA003.get());
        }
        //删除子孙岗位
        List<PositionEntity> allChild = positionService.getAllChild(id);
        //岗位下有用户不能删除，没有顺便删除岗位角色
        if (Boolean.TRUE.equals(userRelationService
                .existByObj(PermissionConst.POSITION, allChild.stream().map(PositionEntity::getId)
                        .collect(Collectors.toList())))) {
            return ActionResult.fail(MsgCode.PS040.get(MsgCode.PS004.get()));
        }
        for (PositionEntity item : allChild) {
            positionService.delete(item);
        }
        return ActionResult.success(MsgCode.SU003.get());
    }

    //++++++++++++++++++++++++++++++++++++++++++动作start+++++++++++++++++++++++++

    @Operation(summary = "设为责任岗位")
    @Parameter(name = "id", description = "主键值", required = true)
    @Parameter(name = KeyConst.ORGANIZE_ID, description = "组织id", required = true)
    @SaCheckPermission("permission.organize")
    @PostMapping("{id}/Actions/DutyPosition")
    public ActionResult<PositionInfoVO> setDutyPostion(@PathVariable("id") String id, @RequestParam(KeyConst.ORGANIZE_ID) String organizeId) {
        OrganizeEntity org = organizeService.getInfo(organizeId);
        PositionEntity position = positionService.getInfo(id);
        if (org == null || position == null) {
            return ActionResult.fail(MsgCode.FA007.get());
        }
        org.setDutyPosition(id);
        organizeService.updateById(org);
        return ActionResult.success(MsgCode.SU005.get());
    }

    @Operation(summary = "设为责任人")
    @Parameter(name = "id", description = "主键值", required = true)
    @Parameter(name = "userId", description = "用户id", required = true)
    @SaCheckPermission("permission.organize")
    @PostMapping("{id}/Actions/DutyUser")
    public ActionResult<PositionInfoVO> setDutyUser(@PathVariable("id") String id, @RequestParam("userId") String userId) {
        UserEntity user = userService.getInfo(userId);
        PositionEntity position = positionService.getInfo(id);
        if (user == null || position == null) {
            return ActionResult.fail(MsgCode.FA007.get());
        }
        position.setDutyUser(userId);
        positionService.updateById(position);
        return ActionResult.success(MsgCode.SU005.get());
    }
    //+++++++++++++++++动作end+++++++++++++控件接口start+++++++++++++++++++++++++++++++

    @Operation(summary = "岗位组件搜索")
    @GetMapping("/Selector")
    public ActionResult<PageListVO<PositionListVO>> selector(PositionPagination pagination) {
        pagination.setDataType(1);
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            pagination.setDataType(null);
        }
        List<PositionEntity> data = positionService.getList(pagination);
        Map<String, Object> allOrgsTreeName = organizeService.getAllOrgsTreeName();
        List<PositionListVO> list = JsonUtil.getJsonToList(data, PositionListVO.class);
        for (PositionListVO item : list) {
            item.setIcon(PermissionConst.POSITION_ICON);
            item.setOrgNameTree(allOrgsTreeName.get(item.getOrganizeId()) + "/" + item.getFullName());
            if (StringUtil.isNotEmpty(pagination.getKeyword())) {
                item.setFullName(item.getOrgNameTree());
            }
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }

    @Operation(summary = "岗位组件树形")
    @GetMapping("/SelectorTree")
    public ActionResult<PageListVO<PositionListVO>> selectorTree(PositionPagination pagination) {
        pagination.setDataType(1);
        List<PositionEntity> data = positionService.getList(pagination);
        Map<String, Object> allOrgsTreeName = organizeService.getAllOrgsTreeName();
        List<PositionTreeModel> list = JsonUtil.getJsonToList(data, PositionTreeModel.class);
        for (PositionTreeModel item : list) {
            item.setIcon(PermissionConst.POSITION_ICON);
            item.setOrgNameTree(allOrgsTreeName.get(item.getOrganizeId()) + "/" + item.getFullName());
        }
        List<SumTree<PositionTreeModel>> trees = TreeDotUtils.convertListToTreeDot(list);
        List<PositionListVO> listVO = JsonUtil.getJsonToList(trees, PositionListVO.class);
        return ActionResult.page(listVO, null);
    }

    @Operation(summary = "自定义范围回显")
    @Parameter(name = "userIdModel", description = "id", required = true)
    @PostMapping("/SelectedList")
    public ActionResult<ListVO<PositionListVO>> selectedList(@RequestBody UserIdModel userIdModel) {
        List<PositionListVO> list = positionService.selectedList(userIdModel.getIds());
        ListVO<PositionListVO> listVO = new ListVO<>();
        listVO.setList(list);
        return ActionResult.success(listVO);
    }

    @Operation(summary = "自定义范围下拉")
    @Parameter(name = "positionConditionModel", description = "岗位选择模型", required = true)
    @PostMapping("/PositionCondition")
    public ActionResult<ListVO<PositionListVO>> positionCondition(@RequestBody UserIdModel userIdModel) {
        List<PositionEntity> list = positionService.positionCondition(userIdModel.getIds());
        Map<String, Object> allOrgsTreeName = organizeService.getAllOrgsTreeName();
        List<PositionTreeModel> modelList = JsonUtil.getJsonToList(list, PositionTreeModel.class);
        for (PositionTreeModel item : modelList) {
            item.setIcon(PermissionConst.POSITION_ICON);
            item.setFullName(allOrgsTreeName.get(item.getOrganizeId()) + "/" + item.getFullName());
            item.setOrgNameTree(item.getFullName());
        }
        List<SumTree<PositionTreeModel>> trees = TreeDotUtils.convertListToTreeDot(modelList);
        List<PositionListVO> voList = JsonUtil.getJsonToList(trees, PositionListVO.class);
        ListVO<PositionListVO> listVO = new ListVO<>();
        listVO.setList(voList);
        return ActionResult.success(listVO);
    }

    //++++++++++++++++++++++++++++++++++++++++++控件接口end+++++++++++++++++++++++++++

    @Operation(summary = "列表")
    @GetMapping("/All")
    public ActionResult<ListVO<PositionListAllVO>> listAll() {
        List<PositionEntity> list = positionService.getList(true);
        List<PositionListAllVO> vos = JsonUtil.getJsonToList(list, PositionListAllVO.class);
        ListVO<PositionListAllVO> vo = new ListVO<>();
        vo.setList(vos);
        return ActionResult.success(vo);
    }

    @Operation(summary = "通过组织id获取岗位列表")
    @Parameter(name = KeyConst.ORGANIZE_ID, description = "主键值", required = true)
    @SaCheckPermission("permission.organize")
    @GetMapping("/getList/{organizeId}")
    public ActionResult<List<PositionVo>> getListByOrganizeId(@PathVariable(KeyConst.ORGANIZE_ID) String organizeId) {
        List<PositionEntity> list = positionService.getListByOrganizeId(Collections.singletonList(organizeId), false);
        List<PositionVo> jsonToList = JsonUtil.getJsonToList(list, PositionVo.class);
        return ActionResult.success(jsonToList);
    }

    @Operation(summary = "获取岗位列表通过组织id数组")
    @Parameter(name = "organizeIds", description = "组织id数组", required = true)
    @SaCheckPermission("permission.organize")
    @PostMapping("/getListByOrgIds")
    public ActionResult<ListVO<PermissionModel>> getListByOrganizeIds(@RequestBody @Valid Map<String, List<String>> organizeIds) {
        List<PermissionModel> positionModelAll = new LinkedList<>();
        if (organizeIds.get("organizeIds") != null) {
            List<String> ids = organizeIds.get("organizeIds");
            positionModelAll = positionService.getListByOrganizeIds(ids, false, true);
        }
        ListVO<PermissionModel> vo = new ListVO<>();
        vo.setList(positionModelAll);
        return ActionResult.success(vo);
    }


    @Operation(summary = "模板下载")
    @SaCheckPermission("permission.organize")
    @GetMapping("/TemplateDownload")
    public ActionResult<DownloadVO> templateDownload() {
        PosColumnMap columnMap = new PosColumnMap();
        String excelName = columnMap.getExcelName();
        Map<String, String> keyMap = columnMap.getColumnByType(0);
        List<ExcelColumnAttr> models = columnMap.getFieldsModel(false);
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
        PosColumnMap columnMap = new PosColumnMap();
        Map<String, String> keyMap = columnMap.getColumnByType(0);
        Map<String, Object> headAndDataMap = ExcelTool.importPreview(FileTypeConstant.TEMPORARY, fileName, keyMap);
        return ActionResult.success(headAndDataMap);
    }

    @Operation(summary = "导出异常报告")
    @SaCheckPermission("permission.organize")
    @PostMapping("/ExportExceptionData")
    public ActionResult<DownloadVO> exportExceptionData(@RequestBody ExcelImportForm visualImportModel) {
        List<Map<String, Object>> dataList = visualImportModel.getList();
        PosColumnMap columnMap = new PosColumnMap();
        String excelName = columnMap.getExcelName();
        Map<String, String> keyMap = columnMap.getColumnByType(0);
        List<ExcelColumnAttr> models = columnMap.getFieldsModel(true);
        ExcelModel excelModel = ExcelModel.builder().optionMap(getOptionMap()).models(models).build();
        DownloadVO vo = ExcelTool.exportExceptionReport(FileTypeConstant.TEMPORARY, excelName, keyMap, dataList, excelModel);
        return ActionResult.success(vo);
    }

    @Operation(summary = "导入数据")
    @SaCheckPermission("permission.organize")
    @PostMapping("/ImportData")
    public ActionResult<ExcelImportVO> importData(@RequestBody ExcelImportForm visualImportModel) {
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
        List<PositionEntity> addList = new ArrayList<>();
        List<Map<String, Object>> failList = new ArrayList<>();
        // 对数据做校验
        this.validateImportData(listData, addList, failList);

        //正常数据插入
        for (PositionEntity each : addList) {
            positionService.create(each);
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
    public ActionResult<Object> exportData(PositionPagination pagination){
        pagination.setDefaultMark(0);
        pagination.setDataType(1);
        List<PositionEntity> dataList = positionService.getList(pagination);
        //组织部门
        Map<String, String> orgMap = organizeService.getList(false)
                .stream().collect(Collectors.toMap(OrganizeEntity::getId, t -> t.getFullName() + "/" + t.getEnCode()));
        Map<String, String> posMap = positionService.getList(false)
                .stream().collect(Collectors.toMap(PositionEntity::getId, t -> t.getFullName() + "/" + t.getEnCode()));

        List<Map<String, Object>> realList = new ArrayList<>();
        for (PositionEntity entity : dataList) {
            Map<String, Object> positionMap = JsonUtil.entityToMap(entity);
            //组织
            String orgName = "";
            if (orgMap.containsKey(entity.getOrganizeId())) {
                orgName = orgMap.get(entity.getOrganizeId());
            }
            String parentName = "";
            if (posMap.containsKey(entity.getParentId())) {
                parentName = posMap.get(entity.getParentId());
            }
            positionMap.put(KeyConst.ORGANIZE_ID, orgName);
            positionMap.put(KeyConst.PARENT_ID, parentName);
            positionMap.put(KeyConst.IS_CONDITION, Objects.equals(entity.getIsCondition(), 1) ? "开" : "关");
            if (Objects.equals(entity.getIsCondition(), 1)) {
                PosConModel posConModel = JsonUtil.getJsonToBean(entity.getConditionJson(), PosConModel.class);
                posConModel.init();
                StringJoiner sj = new StringJoiner(",");
                if (posConModel.getMutualExclusionFlag()) {
                    sj.add(PosColumnMap.CONSTRAINT_TYPE.get(0));
                    StringJoiner me = new StringJoiner(",");
                    if (CollUtil.isNotEmpty(posConModel.getMutualExclusion())) {
                        for (String s : posConModel.getMutualExclusion()) {
                            if (posMap.get(s) != null) {
                                me.add(posMap.get(s));
                            }
                        }
                    }
                    positionMap.put("mutualExclusion", me.toString());
                }
                if (posConModel.getNumFlag()) {
                    sj.add(PosColumnMap.CONSTRAINT_TYPE.get(1));
                    positionMap.put("userNum", posConModel.getUserNum());
                    positionMap.put("permissionNum", posConModel.getPermissionNum());

                }
                if (posConModel.getPrerequisiteFlag()) {
                    sj.add(PosColumnMap.CONSTRAINT_TYPE.get(2));
                    StringJoiner me = new StringJoiner(",");
                    if (CollUtil.isNotEmpty(posConModel.getPrerequisite())) {
                        for (String s : posConModel.getPrerequisite()) {
                            if (posMap.get(s) != null) {
                                me.add(posMap.get(s));
                            }
                        }
                    }
                    positionMap.put("prerequisite", me.toString());
                }
                positionMap.put("constraintType", sj.toString());
            }
            realList.add(positionMap);
        }

        PosColumnMap posColumnMap = new PosColumnMap();
        String excelName = posColumnMap.getExcelName();
        List<ExcelColumnAttr> models = posColumnMap.getFieldsModel(false);
        Map<String, String> keyMap = posColumnMap.getColumnByType(null);
        String[] keys = keyMap.keySet().toArray(new String[0]);
        ExcelModel excelModel = ExcelModel.builder().selectKey(Arrays.asList(keys)).models(models).optionMap(null).build();
        DownloadVO vo = ExcelTool.creatModelExcel(FileTypeConstant.TEMPORARY, excelName, keyMap, realList, excelModel);
        return ActionResult.success(vo);
    }

    private void validateImportData(List<Map<String, Object>> listData, List<PositionEntity> addList, List<Map<String, Object>> failList) {
        PosColumnMap columnMap = new PosColumnMap();
        Map<String, String> keyMap = columnMap.getColumnByType(0);
        List<PositionEntity> allPositionList = positionService.getList(false);
        Map<String, Object> nameCodeMap = organizeService.getOrgEncodeAndName(null);
        Integer posLevel = sysconfigApi.getSysInfo().getPositionLevel();

        //数据库所有部门map
        Map<String, String> allPosMap = allPositionList.stream().collect(Collectors.toMap(t -> t.getFullName() + "/" + t.getEnCode(), PositionEntity::getId));
        //新增成功的所有部门map
        Map<String, String> allAddPosMap = addList.stream().collect(Collectors.toMap(t -> t.getFullName() + "/" + t.getEnCode(), PositionEntity::getId));
        for (int i = 0, len = listData.size(); i < len; i++) {
            Map<String, Object> eachMap = listData.get(i);
            Map<String, Object> realMap = JsonUtil.getJsonToBean(eachMap, Map.class);
            StringJoiner errInfo = new StringJoiner(",");

            //所属组织判断，不存在后续不判断
            if (eachMap.get(KeyConst.ORGANIZE_ID) == null || StringUtil.isEmpty(eachMap.get(KeyConst.ORGANIZE_ID).toString())) {
                errInfo.add("所属组织不能为空");
                eachMap.put(KeyConst.ERRORS_INFO, errInfo.toString());
                failList.add(eachMap);
            } else {
                String organizeNameCode = eachMap.get(KeyConst.ORGANIZE_ID).toString();
                if (!nameCodeMap.containsKey(organizeNameCode)) {
                    errInfo.add("找不到所属组织");
                    eachMap.put(KeyConst.ERRORS_INFO, errInfo.toString());
                    failList.add(eachMap);
                    continue;
                }
                String organizeId = nameCodeMap.get(organizeNameCode).toString();
                realMap.put(KeyConst.ORGANIZE_ID, organizeId);
                String parentPosTree = "";

                boolean isCondition = false;
                boolean hasExclusion = false;
                boolean hasNum = false;
                boolean hasPrerequisite = false;
                List<String> extList = new ArrayList<>();
                for (Map.Entry<String, String> keyItem : keyMap.entrySet()) {
                    String column = keyItem.getKey();
                    String columnName = keyItem.getValue();
                    Object valueObj = eachMap.get(column);
                    String value = valueObj == null ? null : String.valueOf(valueObj);

                    switch (column) {
                        case KeyConst.PARENT_ID:
                            if (StringUtil.isEmpty(value)) {
                                break;
                            }
                            String pPosId = "";
                            //数据库找父级
                            PositionEntity pPost = allPositionList.stream().filter(t -> t.getOrganizeId().equals(organizeId)
                                    && (t.getFullName() + "/" + t.getEnCode()).equals(value)).findFirst().orElse(null);
                            if (pPost == null) {
                                //excel中找到父级
                                pPost = addList.stream().filter(t -> t.getOrganizeId().equals(organizeId) && StringUtil.isNotEmpty(t.getEnCode())
                                        && (t.getFullName() + "/" + t.getEnCode()).equals(value)).findFirst().orElse(null);
                            }
                            if (pPost == null) {
                                errInfo.add(columnName + "不存在");
                                break;
                            }
                            pPosId = pPost.getId();
                            parentPosTree = pPost.getPositionIdTree();
                            //层级限制
                            if (parentPosTree.split(",").length >= posLevel) {
                                errInfo.add(MsgCode.PS038.get(posLevel));
                                break;
                            }
                            realMap.put(KeyConst.PARENT_ID, pPosId);
                            break;
                        case "fullName":
                            if (StringUtils.isEmpty(value)) {
                                errInfo.add(columnName + "不能为空");

                            }else {
                                checkFullName(addList, value, errInfo, columnName, organizeId, allPositionList);
                            }

                            break;
                        case "enCode":
                            if (StringUtils.isNotEmpty(value)) {
                                checkEnCode(addList, value, errInfo, columnName, allPositionList);
                            }

                            break;
                        case "isCondition":
                            //岗位约束开关
                            if (StringUtil.isEmpty(value)) {
                                realMap.put(column, 0);
                                break;
                            }
                            if ("开".equals(value)) {
                                isCondition = true;
                            }
                            realMap.put(column, isCondition ? 1 : 0);
                            break;
                        case "constraintType":
                            //岗位约束类型
                            if (isCondition) {
                                if (StringUtil.isEmpty(value)) {
                                    errInfo.add(columnName + "不能为空");
                                    break;
                                }
                                List<String> split = Arrays.asList(value.split(","));
                                Map<Integer, String> typeMap = PosColumnMap.CONSTRAINT_TYPE;
                                List<Integer> constraintTypeList = new ArrayList<>();
                                for (Map.Entry<Integer, String> typeItem : typeMap.entrySet()) {
                                    Integer typeKey = typeItem.getKey();
                                    String itemValue = typeItem.getValue();
                                    if (split.contains(itemValue)) {
                                        if (Objects.equals(typeKey, 0)) {
                                            hasExclusion = true;
                                        }
                                        if (Objects.equals(typeKey, 1)) {
                                            hasNum = true;
                                        }
                                        if (Objects.equals(typeKey, 2)) {
                                            hasPrerequisite = true;
                                        }
                                        constraintTypeList.add(typeKey);
                                    }
                                }
                                if (CollUtil.isEmpty(constraintTypeList)) {
                                    errInfo.add(columnName + "值不正确");
                                    break;
                                }
                                realMap.put(column, constraintTypeList);
                            }
                            break;
                        case "mutualExclusion":
                            //岗位约束互斥
                            if (hasExclusion) {
                                if (StringUtil.isEmpty(value)) {
                                    errInfo.add(columnName + "不能为空");
                                    break;
                                }
                                List<String> exList = Arrays.asList(value.split(","));
                                List<String> allExList = new ArrayList<>();
                                for (String item : exList) {
                                    if (StringUtil.isNotEmpty(allPosMap.get(item))) {
                                        allExList.add(allPosMap.get(item));
                                    } else if (StringUtil.isNotEmpty(allAddPosMap.get(item))) {
                                        allExList.add(allAddPosMap.get(item));
                                    }
                                }
                                if (CollUtil.isEmpty(allExList) || exList.size() != allExList.size()) {
                                    errInfo.add(columnName + "值不正确");
                                    break;
                                }
                                extList = new ArrayList<>(allExList);
                                realMap.put(column, allExList);
                            } else {
                                realMap.put(column, new ArrayList<>());
                            }
                            break;
                        case "userNum":
                        case "permissionNum":
                            //岗位约束基数
                            checkExcNum(hasNum, value, errInfo, columnName, realMap, column);
                            break;
                        case "prerequisite":
                            //岗位约束先决
                            if (hasPrerequisite) {
                                if (StringUtil.isEmpty(value)) {
                                    errInfo.add(columnName + "不能为空");
                                    break;
                                }
                                List<String> preList = Arrays.asList(value.split(","));
                                List<String> allPreList = new ArrayList<>();
                                for (String item : preList) {
                                    if (StringUtil.isNotEmpty(allPosMap.get(item))) {
                                        allPreList.add(allPosMap.get(item));
                                    } else if (StringUtil.isNotEmpty(allAddPosMap.get(item))) {
                                        allPreList.add(allAddPosMap.get(item));
                                    }
                                }
                                if (CollUtil.isEmpty(allPreList) || preList.size() != allPreList.size()) {
                                    errInfo.add(columnName + "值不正确");
                                    break;
                                }
                                //互斥，先决取交集
                                extList.retainAll(allPreList);
                                if (!extList.isEmpty()) {
                                    errInfo.add("互斥和先决对象不能是同一个对象");
                                    break;
                                }
                                for (String item : allPreList) {
                                    PositionEntity positionEntity = allPositionList.stream().filter(t -> t.getId().equals(item)).findFirst().orElse(null);
                                    if (positionEntity == null) {
                                        positionEntity = addList.stream().filter(t -> t.getId().equals(item)).findFirst().orElse(null);
                                    }
                                    if (Objects.equals(positionEntity.getIsCondition(), 1)) {
                                        PosConModel posConModel = JsonUtil.getJsonToBean(positionEntity.getConditionJson(), PosConModel.class);
                                        posConModel.init();
                                        if (posConModel.getPrerequisiteFlag()) {
                                            errInfo.add("先决约束冲突，先决限制1级");
                                            break;
                                        }
                                    }

                                }
                                realMap.put(column, allPreList);
                            } else {
                                realMap.put(column, new ArrayList<>());
                            }
                            break;

                        case "sortCode":
                            checkSort(value, realMap, errInfo, columnName, column);
                            break;
                        default:
                            break;
                    }

                }
                if (errInfo.length() == 0) {
                    PositionEntity positionEntity = JsonUtil.getJsonToBean(realMap, PositionEntity.class);
                    String id = RandomUtil.uuId();
                    positionEntity.setId(id);
                    positionEntity.setCreatorTime(new Date());
                    positionEntity.setPositionIdTree(StringUtil.isNotEmpty(parentPosTree) ? parentPosTree + "," + id : id);
                    if (isCondition) {
                        PosConModel posConModel = JsonUtil.getJsonToBean(realMap, PosConModel.class);
                        positionEntity.setConditionJson(JsonUtil.getObjectToString(posConModel));
                    }
                    addList.add(positionEntity);
                } else {
                    eachMap.put(KeyConst.ERRORS_INFO, errInfo.toString());
                    failList.add(eachMap);
                }

            }
        }
    }

    private static void checkSort(String value, Map<String, Object> realMap, StringJoiner errInfo, String columnName, String column) {
        if (StringUtil.isEmpty(value)) {
            realMap.put("sortCode", 0);
            return;
        }
        Long numValue = 0l;
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
            return;
        }
        realMap.put(column, numValue);
    }

    private static void checkExcNum(boolean hasNum, String value, StringJoiner errInfo, String columnName, Map<String, Object> realMap, String column) {
        if (hasNum) {
            if (StringUtil.isEmpty(value)) {
                errInfo.add(columnName + "不能为空");
                return;
            }
            Integer userNum;
            try {
                userNum = Integer.parseInt(value);
            } catch (Exception e) {
                userNum = -1;
            }
            if (userNum <= 0) {
                errInfo.add(columnName + "值不正确");
                return;
            }
            realMap.put(column, userNum);
        } else {
            realMap.put(column, 1);
        }
    }

    private static void checkEnCode(List<PositionEntity> addList, String value, StringJoiner errInfo, String columnName, List<PositionEntity> allPositionList) {
        if (StringUtil.isNotEmpty(value)) {
            if (50 < value.length()) {
                errInfo.add(columnName + "值超出最多输入字符限制");
            }
            if (!RegexUtils.checkEnCode(value)) {
                errInfo.add(columnName + "只能输入英文、数字和小数点且小数点不能放在首尾");
            }
            //库里重复
            long enCodeCount = allPositionList.stream().filter(t -> t.getEnCode().equals(value)).count();
            if (enCodeCount > 0) {
                errInfo.add(columnName + "值已存在");
                return;
            }
            //表格内重复
            enCodeCount = addList.stream().filter(t -> t.getEnCode().equals(value)).count();
            if (enCodeCount > 0) {
                errInfo.add(columnName + "值已存在");
            }
        }
    }

    private static void checkFullName(List<PositionEntity> addList, String value, StringJoiner errInfo, String columnName, String organizeId, List<PositionEntity> allPositionList) {
        if (StringUtils.isEmpty(value)) {
            errInfo.add(columnName + "不能为空");
            return;
        }
        if (50 < value.length()) {
            errInfo.add(columnName + "值超出最多输入字符限制");
        }
        //值不能含有特殊符号
        if (!RegexUtils.checkSpecoalSymbols(value)) {
            errInfo.add(columnName + "值不能含有特殊符号");
        }
        //库里重复
        long fullNameCount = allPositionList.stream().filter(t -> t.getOrganizeId().equals(organizeId) && t.getFullName().equals(value)).count();
        if (fullNameCount > 0) {
            errInfo.add(columnName + "值已存在");
            return;
        }
        //表格内重复
        fullNameCount = addList.stream().filter(t -> t.getOrganizeId().equals(organizeId) && t.getFullName().equals(value)).count();
        if (fullNameCount > 0) {
            errInfo.add(columnName + "值已存在");
        }
    }

    /**
     * 获取下拉框
     *
     * @return
     */
    private Map<String, String[]> getOptionMap() {
        Map<String, String[]> optionMap = new HashMap<>();
        //约束开关
        optionMap.put("isCondition", new String[]{"开", "关"});
        return optionMap;
    }
}
