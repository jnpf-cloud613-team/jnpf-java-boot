package jnpf.permission.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.controller.SuperController;
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
import jnpf.permission.constant.RoleColumnMap;
import jnpf.permission.entity.RoleEntity;
import jnpf.permission.entity.RoleRelationEntity;
import jnpf.permission.entity.UserRelationEntity;
import jnpf.permission.model.check.CheckResult;
import jnpf.permission.model.position.PosConModel;
import jnpf.permission.model.role.*;
import jnpf.permission.model.user.mod.UserIdModel;
import jnpf.permission.service.*;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色管理
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Tag(name = "角色管理", description = "Role")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permission/Role")
public class RoleController extends SuperController<RoleService, RoleEntity> {

    private final RoleService roleService;
    private final RoleRelationService roleRelationService;
    private final OrganizeService organizeService;
    private final UserService userService;
    private final UserRelationService userRelationService;

    @Operation(summary = "获取角色列表")
    @SaCheckPermission(value = {"permission.auth", "permission.role"}, mode = SaMode.OR)
    @GetMapping
    public ActionResult<PageListVO<RoleListVO>> list(RolePagination pagination) {
        pagination.setDataType(1);
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            pagination.setDataType(null);
        }
        List<RoleEntity> list = roleService.getList(pagination);
        List<RoleListVO> listVO = new ArrayList<>();
        for (RoleEntity entity : list) {
            // 角色类型展示
            RoleListVO vo = JsonUtil.getJsonToBean(entity, RoleListVO.class);
            if (Objects.equals(entity.getGlobalMark(), 1)) {
                vo.setIsSystem(1);
            }
            listVO.add(vo);
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(listVO, paginationVO);
    }

    @Operation(summary = "新建角色")
    @Parameter(name = "roleCrForm", description = "角色模型", required = true)
    @SaCheckPermission("permission.role")
    @PostMapping
    @DSTransactional
    public ActionResult<String> create(@RequestBody @Valid RoleCrForm roleCrForm) {
        RoleEntity entity = JsonUtil.getJsonToBean(roleCrForm, RoleEntity.class);
        if (Boolean.TRUE.equals(roleService.isExistByFullName(roleCrForm.getFullName(), null, roleCrForm.getType()))) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (Boolean.TRUE.equals(roleService.isExistByEnCode(roleCrForm.getEnCode(), null))) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }

        //约束判断
        PosConModel posConModel = new PosConModel();
        if (PermissionConst.USER.equals(entity.getType()) && Objects.equals(entity.getIsCondition(), 1)) {
            posConModel = JsonUtil.getJsonToBean(entity.getConditionJson(), PosConModel.class);
            posConModel.init();
            String errStr = posConModel.checkCondition(entity.getId());
            if (StringUtil.isNotEmpty(errStr)) {
                return ActionResult.fail(errStr);
            }
            if (posConModel.getPrerequisiteFlag()) {
                for (String item : posConModel.getPrerequisite()) {
                    RoleEntity itemInfo = roleService.getInfo(item);
                    //先决只能1级
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
        roleService.create(entity);
        roleService.linkUpdate(entity.getId(), posConModel);
        return ActionResult.success(MsgCode.SU001.get());
    }

    @Operation(summary = "更新角色")
    @Parameter(name = "roleUpForm", description = "角色模型", required = true)
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("permission.role")
    @PutMapping("/{id}")
    @DSTransactional
    public ActionResult<String> update(@RequestBody @Valid RoleUpForm roleUpForm, @PathVariable("id") String id) throws DataException {
        RoleEntity info = roleService.getInfo(id);
        if (info == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        RoleEntity entity = JsonUtil.getJsonToBean(roleUpForm, RoleEntity.class);
        entity.setId(id);
        if (Boolean.TRUE.equals(roleService.isExistByFullName(roleUpForm.getFullName(), id, roleUpForm.getType()))) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (Boolean.TRUE.equals(roleService.isExistByEnCode(roleUpForm.getEnCode(), id))) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        PosConModel posConModel = new PosConModel();
        //约束判断
        if (PermissionConst.USER.equals(entity.getType()) && Objects.equals(entity.getIsCondition(), 1)) {
            posConModel = JsonUtil.getJsonToBean(entity.getConditionJson(), PosConModel.class);
            posConModel.init();
            String errStr = posConModel.checkCondition(entity.getId());
            if (StringUtil.isNotEmpty(errStr)) {
                return ActionResult.fail(errStr);
            }
            //修改角色的时候，角色有人的话，互斥角色（选择的角色下的人一个都不能重叠），先决角色（选择的角色下的人必须包含当前角色人员）
            Set<String> userIds = roleRelationService.getListByRoleId(entity.getId(), PermissionConst.USER)
                    .stream().map(RoleRelationEntity::getObjectId).collect(Collectors.toSet());
            //获取全部的先决列表。用于判断当前是否开启先决（存在就不能开启）
            List<RoleEntity> list = roleService.getList(true, PermissionConst.USER, null);
            Map<String, RoleEntity> roleMap = list.stream().collect(Collectors.toMap(RoleEntity::getId, t -> t));
            if (posConModel.getMutualExclusionFlag()) {
                for (String item : posConModel.getMutualExclusion()) {
                    Set<String> itemUserIds = roleRelationService.getListByRoleId(item, PermissionConst.USER)
                            .stream().map(RoleRelationEntity::getObjectId).collect(Collectors.toSet());
                    Set<String> commonIds = new HashSet<>(userIds);
                    commonIds.retainAll(itemUserIds);
                    if (!commonIds.isEmpty()) {
                        return ActionResult.fail(MsgCode.SYS137.get());
                    }
                    //当前角色A添加互斥角色B，B里的先决不能有A，如果有会导致添加成功后B角色的先决和互斥冲突
                    RoleEntity itemEntity = roleMap.get(item);
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
                for (RoleEntity t : list) {
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

                //用户冲突--当前角色里的用户要判断先决包含先决的所有角色
                if (CollUtil.isNotEmpty(userIds)) {
                    for (String userId : userIds) {
                        Set<String> roleIds = roleRelationService.getListByObjectId(userId, PermissionConst.USER)
                                .stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toSet());
                        //用户冲突，先决修改失败
                        if (!roleIds.containsAll(posConModel.getPrerequisite())) {
                            return ActionResult.fail(MsgCode.SYS138.get());
                        }
                    }
                }

                //先决只能1级--选中的先决自己不能是先决
                for (String item : posConModel.getPrerequisite()) {
                    RoleEntity itemInfo = roleService.getInfo(item);
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
        boolean flag = roleService.update(id, entity);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        roleService.linkUpdate(id, posConModel);
        return ActionResult.success(MsgCode.SU004.get());
    }


    @Operation(summary = "获取角色信息")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("permission.role")
    @GetMapping("/{id}")
    public ActionResult<RoleInfoVO> getInfo(@PathVariable("id") String id) throws DataException {
        RoleEntity entity = roleService.getInfo(id);
        RoleInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, RoleInfoVO.class);
        //判断当前角色是否已经是先决角色
        Set<String> allPrePos = new HashSet<>();
        List<RoleEntity> list = roleService.getList(false, PermissionConst.USER, 0);
        for (RoleEntity t : list) {
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
        return ActionResult.success(vo);
    }

    @Operation(summary = "删除角色")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("permission.role")
    @DeleteMapping("/{id}")
    public ActionResult<String> delete(@PathVariable("id") String id) {
        RoleEntity entity = roleService.getInfo(id);
        if (entity != null) {
            //刷新角色关联用户
            List<String> userIds = new ArrayList<>();
            if (PermissionConst.USER.equals(entity.getType())) {
                List<RoleRelationEntity> userList = roleRelationService.getListByRoleId(id, PermissionConst.USER);
                userIds.addAll(userList.stream().map(RoleRelationEntity::getObjectId).collect(Collectors.toList()));
            }
            if (PermissionConst.POSITION.equals(entity.getType())) {
                List<RoleRelationEntity> posList = roleRelationService.getListByRoleId(id, PermissionConst.POSITION);
                List<UserRelationEntity> userList = userRelationService.getListByObjectIdAll(posList.stream().map(RoleRelationEntity::getObjectId).collect(Collectors.toList()));
                userIds.addAll(userList.stream().map(UserRelationEntity::getUserId).collect(Collectors.toList()));
            }
            roleService.delete(entity);
            if (CollUtil.isNotEmpty(userIds)) {
                userService.delCurUser(null, userIds);
            }
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    //+++++++++++++++++++++++++++++++++其他接口++++++++++++++++++++++++++++++++++++++++++++++++

    @Operation(summary = "角色下拉框列表")
    @GetMapping("/Selector")
    public ActionResult<ListVO<RoleSelectorVO>> listAll(@RequestParam(name = "type", required = false) String type,
                                                        @RequestParam(name = "isSystem", required = false) Integer isSystem) {
        List<RoleEntity> list = roleService.getList(true, type, isSystem);
        List<RoleSelectorVO> modelList = JsonUtil.getJsonToList(list, RoleSelectorVO.class);
        for (RoleSelectorVO vo : modelList) {
            vo.setIcon(PermissionConst.ROLE_ICON);
        }
        ListVO<RoleSelectorVO> vo = new ListVO<>();
        vo.setList(modelList);
        return ActionResult.success(vo);
    }

    @Operation(summary = "获取角色下拉框(自定义范围)")
    @Parameter(name = "idModel", description = "ids", required = true)
    @PostMapping("/RoleCondition")
    public ActionResult<List<RoleSelectorVO>> roleCondition(@RequestBody UserIdModel idModel) {
        List<RoleEntity> list = roleService.getListByIds(idModel.getIds());
        list = list.stream().filter(t -> !Objects.equals(t.getGlobalMark(), 1)).collect(Collectors.toList());
        List<RoleSelectorVO> modelList = JsonUtil.getJsonToList(list, RoleSelectorVO.class);
        for (RoleSelectorVO vo : modelList) {
            vo.setIcon(PermissionConst.ROLE_ICON);
        }
        return ActionResult.success(modelList);
    }

    @Operation(summary = "模板下载")
    @SaCheckPermission("permission.role")
    @GetMapping("/TemplateDownload")
    public ActionResult<DownloadVO> templateDownload() {
        RoleColumnMap columnMap = new RoleColumnMap();
        String excelName = columnMap.getExcelName();
        Map<String, String> keyMap = columnMap.getColumnByType(0);
        List<ExcelColumnAttr> models = columnMap.getFieldsModel(false);
        List<Map<String, Object>> list = columnMap.getDefaultList();
        Map<String, String[]> optionMap = getOptionMap();
        ExcelModel excelModel = ExcelModel.builder().models(models).selectKey(new ArrayList<>(keyMap.keySet())).optionMap(optionMap).build();
        DownloadVO vo = ExcelTool.getImportTemplate(FileTypeConstant.TEMPORARY, excelName, keyMap, list, excelModel);
        return ActionResult.success(vo);
    }

    /**
     * 上传Excel
     *
     * @return
     */
    @Operation(summary = "上传导入Excel")
    @SaCheckPermission("permission.role")
    @PostMapping("/Uploader")
    public ActionResult<Object> uploader() {
        return ExcelTool.uploader();
    }

    /**
     * 导入预览
     *
     * @return
     */
    @Operation(summary = "导入预览")
    @SaCheckPermission("permission.role")
    @GetMapping("/ImportPreview")
    public ActionResult<Map<String, Object>> importPreview(String fileName) {
        // 导入字段
        RoleColumnMap columnMap = new RoleColumnMap();
        Map<String, String> keyMap = columnMap.getColumnByType(0);
        Map<String, Object> headAndDataMap = ExcelTool.importPreview(FileTypeConstant.TEMPORARY, fileName, keyMap);
        return ActionResult.success(headAndDataMap);
    }


    /**
     * 导出异常报告
     *
     * @return
     */
    @Operation(summary = "导出异常报告")
    @SaCheckPermission("permission.role")
    @PostMapping("/ExportExceptionData")
    public ActionResult<DownloadVO> exportExceptionData(@RequestBody ExcelImportForm visualImportModel) {
        List<Map<String, Object>> dataList = visualImportModel.getList();
        RoleColumnMap columnMap = new RoleColumnMap();
        String excelName = columnMap.getExcelName();
        Map<String, String> keyMap = columnMap.getColumnByType(0);
        List<ExcelColumnAttr> models = columnMap.getFieldsModel(true);
        ExcelModel excelModel = ExcelModel.builder().optionMap(getOptionMap()).models(models).build();
        DownloadVO vo = ExcelTool.exportExceptionReport(FileTypeConstant.TEMPORARY, excelName, keyMap, dataList, excelModel);
        return ActionResult.success(vo);
    }

    /**
     * 导入数据
     *
     * @return
     */
    @Operation(summary = "导入数据")
    @SaCheckPermission("permission.role")
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
        List<RoleEntity> addList = new ArrayList<>();
        List<Map<String, Object>> failList = new ArrayList<>();
        Map<String, List<List<String>>> addOrganizeIdMap = new HashMap<>();
        // 对数据做校验
        this.validateImportData(listData, addList, addOrganizeIdMap, failList);

        //正常数据插入
        for (RoleEntity each : addList) {
            roleService.create(each);
        }
        ExcelImportVO importModel = new ExcelImportVO();
        importModel.setSnum(addList.size());
        importModel.setFnum(failList.size());
        importModel.setResultType(!failList.isEmpty() ? 1 : 0);
        importModel.setFailResult(failList);
        importModel.setHeaderRow(headerRow);
        return ActionResult.success(importModel);
    }

    private void validateImportData(List<Map<String, Object>> listData, List<RoleEntity> addList, Map<String, List<List<String>>> addOrganizeIdMap, List<Map<String, Object>> failList) {
        RoleColumnMap columnMap = new RoleColumnMap();
        Map<String, String> keyMap = columnMap.getColumnByType(0);
        QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().isNull(RoleEntity::getDeleteMark);
        queryWrapper.lambda().orderByAsc(RoleEntity::getSortCode).orderByDesc(RoleEntity::getCreatorTime);
        List<RoleEntity> allPositionList = roleService.list(queryWrapper);
        Map<String, Object> allOrgsTreeName = organizeService.getAllOrgsTreeName();
        for (int i = 0, len = listData.size(); i < len; i++) {
            Map<String, Object> eachMap = listData.get(i);
            Map<String, Object> realMap = JsonUtil.getJsonToBean(eachMap, Map.class);
            StringJoiner errInfo = new StringJoiner(",");
            int globalMark = "组织".equals(eachMap.get(KeyConst.GLOBAL_MARK)) ? 0 : 1;
            List<List<String>> organizeIdList = null;

            for (Map.Entry<String, String> columnItem : keyMap.entrySet()) {
                String column = columnItem.getKey();
                String columnName = columnItem.getValue();
                Object valueObj = eachMap.get(column);
                String value = valueObj == null ? null : String.valueOf(valueObj);
                switch (column) {
                    case "fullName":
                        if (StringUtils.isEmpty(value)) {
                            errInfo.add(columnName + "不能为空");
                            break;
                        }
                        if (50 < value.length()) {
                            errInfo.add(columnName + "值超出最多输入字符限制");
                        }
                        //值不能含有特殊符号
                        if (!RegexUtils.checkSpecoalSymbols(value)) {
                            errInfo.add(columnName + "值不能含有特殊符号");
                        }
                        //库里重复
                        int finalGlobalMark = globalMark;
                        long fullNameCount = allPositionList.stream().filter(t -> t.getFullName().equals(value) && t.getGlobalMark().equals(finalGlobalMark)).count();
                        if (fullNameCount > 0) {
                            errInfo.add(columnName + "值已存在");
                            break;
                        }
                        //表格内重复
                        fullNameCount = addList.stream().filter(t -> t.getFullName().equals(value) && t.getGlobalMark().equals(finalGlobalMark)).count();
                        if (fullNameCount > 0) {
                            errInfo.add(columnName + "值已存在");
                            break;
                        }
                        break;
                    case "enCode":
                        if (StringUtils.isEmpty(value)) {
                            errInfo.add(columnName + "不能为空");
                            break;
                        }
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
                            break;
                        }
                        //表格内重复
                        enCodeCount = addList.stream().filter(t -> t.getEnCode().equals(value)).count();
                        if (enCodeCount > 0) {
                            errInfo.add(columnName + "值已存在");
                            break;
                        }
                        break;
                    case KeyConst.GLOBAL_MARK:
                        if (StringUtil.isEmpty(value)) {
                            errInfo.add(columnName + "不能为空");
                            break;
                        }
                        if ("全局".equals(value)) {
                            globalMark = 1;
                        } else if ("组织".equals(value)) {
                            globalMark = 0;
                        } else {
                            errInfo.add(columnName + "值不正确");
                            break;
                        }
                        realMap.put(KeyConst.GLOBAL_MARK, globalMark);
                        break;
                    case KeyConst.ORGANIZE_ID:
                        if (globalMark == 0 && StringUtil.isEmpty(value)) {
                            errInfo.add("所属组织" + "不能为空");
                            break;
                        }
                        if (globalMark == 1) {
                            realMap.put("organizeId", null);
                            break;
                        }
                        if (StringUtil.isEmpty(value)) {
                            break;
                        }
                        if (StringUtils.isNotEmpty(value)){
                            CheckResult organizeIdCheckResult = this.checkOrganizes(value, allOrgsTreeName);
                            if (!organizeIdCheckResult.isPass()) {
                                errInfo.add(organizeIdCheckResult.getErrorMsg());
                                break;
                            }
                            organizeIdList = (List<List<String>>) organizeIdCheckResult.getValue();
                        }

                        break;
                    case "sortCode":
                        if (StringUtil.isEmpty(value)) {
                            realMap.put("sortCode", 0);
                            break;
                        }
                        long numValue;
                        try {
                            numValue = Long.parseLong(value);
                        } catch (Exception e) {
                            errInfo.add(columnName + "值不正确");
                            break;
                        }
                        if (numValue < 0) {
                            errInfo.add(columnName + "值不能小于0");
                            break;
                        }
                        if (numValue > 1000000) {
                            errInfo.add(columnName + "值不能大于999999");
                            break;
                        }
                        break;
                    case KeyConst.ENABLED_MARK:
                        if (StringUtil.isEmpty(value)) {
                            errInfo.add(columnName + "不能为空");
                            break;
                        }
                        if ("启用".equals(value)) {
                            realMap.put(KeyConst.ENABLED_MARK, 1);
                        } else if ("禁用".equals(value)) {
                            realMap.put(KeyConst.ENABLED_MARK, 0);
                        } else {
                            errInfo.add(columnName + "值不正确");
                        }
                        break;
                    default:
                        break;
                }

            }
            if (errInfo.length() == 0) {
                RoleEntity roleEntity = JsonUtil.getJsonToBean(realMap, RoleEntity.class);
                roleEntity.setCreatorTime(new Date());
                roleEntity.setId(RandomUtil.uuId());
                addList.add(roleEntity);
                if (organizeIdList != null && !organizeIdList.isEmpty() && roleEntity.getGlobalMark() == 0) {
                    addOrganizeIdMap.put(roleEntity.getId(), organizeIdList);
                }
            } else {
                eachMap.put("errorsInfo", errInfo.toString());
                failList.add(eachMap);
            }
        }
    }

    /**
     * 导出Excel
     *
     * @return
     */
    @Operation(summary = "导出Excel")
    @SaCheckPermission("permission.role")
    @GetMapping("/ExportData")
    public ActionResult<Object> exportData(RolePagination pagination){
        if (StringUtil.isEmpty(pagination.getSelectKey())) {
            return ActionResult.fail(MsgCode.IMP011.get());
        }
        List<Map<String, Object>> realList = new ArrayList<>();
        String[] keys = !StringUtil.isEmpty(pagination.getSelectKey()) ? pagination.getSelectKey() : new String[0];
        RoleColumnMap posColumnMap = new RoleColumnMap();
        String excelName = posColumnMap.getExcelName();
        List<ExcelColumnAttr> models = posColumnMap.getFieldsModel(false);
        Map<String, String> keyMap = posColumnMap.getColumnByType(null);
        Map<String, String[]> optionMap = new HashMap<>();
        ExcelModel excelModel = ExcelModel.builder().selectKey(Arrays.asList(keys)).models(models).optionMap(optionMap).build();
        DownloadVO vo = ExcelTool.creatModelExcel(FileTypeConstant.TEMPORARY, excelName, keyMap, realList, excelModel);
        return ActionResult.success(vo);
    }

    private CheckResult checkOrganizes(String organizeNames, Map<String, Object> allOrgsTreeName) {
        String[] organizeNameArray = organizeNames.split(",");
        List<String> errorOrganizeNameList = new ArrayList<>();
        List<List<String>> successOrganizeIdList = new ArrayList<>();
        for (String organizeName : organizeNameArray) {
            boolean find = false;
            for (Map.Entry<String, Object> item : allOrgsTreeName.entrySet()) {
                String key = item.getKey();
                Object o = item.getValue();
                if (organizeName.equals(o.toString())) {
                    find = true;
                    successOrganizeIdList.add(Arrays.asList(key.split(",")));
                    break;
                }
            }
            if (!find) {
                errorOrganizeNameList.add(organizeName);
            }
        }
        if (errorOrganizeNameList.isEmpty()) {
            return new CheckResult(true, null, successOrganizeIdList);
        } else if (organizeNameArray.length == 1) {
            return new CheckResult(false, "找不到该所属组织", null);
        } else {
            return new CheckResult(false, "找不到该所属组织（" + String.join("、", errorOrganizeNameList) + "）", null);
        }
    }

    /**
     * 获取下拉框
     *
     * @return
     */
    private Map<String, String[]> getOptionMap() {
        Map<String, String[]> optionMap = new HashMap<>();
        //角色类型
        optionMap.put(KeyConst.GLOBAL_MARK, new String[]{"全局", "组织"});
        optionMap.put(KeyConst.ENABLED_MARK, new String[]{"启用", "禁用"});
        return optionMap;
    }
}
