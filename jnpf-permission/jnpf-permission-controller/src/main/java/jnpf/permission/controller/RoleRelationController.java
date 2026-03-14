package jnpf.permission.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.collection.CollUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.controller.SuperController;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.permission.entity.RoleEntity;
import jnpf.permission.entity.RoleRelationEntity;
import jnpf.permission.entity.UserRelationEntity;
import jnpf.permission.model.role.RoleListVO;
import jnpf.permission.model.rolerelaiton.*;
import jnpf.permission.service.*;
import jnpf.util.JsonUtil;
import jnpf.util.enums.DictionaryDataEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * 角色关系
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/3/5 18:14:07
 */
@Tag(name = "角色关系", description = "RoleRelation")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permission/RoleRelation")
public class RoleRelationController extends SuperController<UserRelationService, UserRelationEntity> {

    private final RoleRelationService roleRelationService;
    private final UserService userService;
    private final OrganizeService organizeService;
    private final PositionService positionService;
    private final RoleService roleService;
    private final UserRelationService userRelationService;
    private final DictionaryDataService dictionaryDataApi;

    @Operation(summary = "获取角色绑定信息列表")
    @Parameter(name = "roleId", description = "角色id", required = true)
    @Parameter(name = "type", description = "类型：user,organize,position", required = true)
    @SaCheckPermission(value = {"permission.auth", "permission.organize", "permission.role"}, mode = SaMode.OR)
    @GetMapping
    public ActionResult getList(RoleRelationPage pagination) {
        String type = pagination.getType();
        if (PermissionConst.USER.equals(type)) {
            List<RoleRelationUserVo> userList = roleRelationService.getUserPage(pagination);
            Map<String, String> positionMap = positionService.getPosFullNameMap();
            List<DictionaryDataEntity> dataServiceList4 = dictionaryDataApi.getListByTypeDataCode(DictionaryDataEnum.SEX_TYPE.getDictionaryTypeId());
            Map<String, String> genderMap = dataServiceList4.stream().collect(Collectors.toMap(DictionaryDataEntity::getEnCode, DictionaryDataEntity::getFullName));
            for (RoleRelationUserVo userVo : userList) {
                StringJoiner positionJoiner = new StringJoiner(",");
                StringJoiner organizeJoiner = new StringJoiner(",");
                List<UserRelationEntity> allPostion = userRelationService.getListByObjectType(userVo.getId(), PermissionConst.POSITION);
                if (CollUtil.isNotEmpty(allPostion)) {
                    for (UserRelationEntity item : allPostion) {
                        String posName = positionMap.get(item.getObjectId());
                        if (posName != null) {
                            positionJoiner.add(posName);
                            organizeJoiner.add(posName.substring(0, posName.lastIndexOf("/")));
                        }
                    }
                }
                userVo.setGender(genderMap.get(userVo.getGender()));
                userVo.setPosition(positionJoiner.toString());
            }
            PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
            return ActionResult.page(userList, paginationVO);
        }
        List<RoleRelationOrgVo> list = new ArrayList<>();
        if (PermissionConst.ORGANIZE.equals(type)) {
            list = roleRelationService.getOrgPage(pagination);
            list.forEach(t -> t.setFullName(t.getOrgNameTree()));
        }
        if (PermissionConst.POSITION.equals(type)) {
            list = roleRelationService.getPosPage(pagination);
            Map<String, Object> allOrgsTreeName = organizeService.getAllOrgsTreeName();
            list.forEach(t -> {
                t.setOrgNameTree(allOrgsTreeName.get(t.getOrganizeId()) + "/" + t.getFullName());
                t.setFullName(t.getOrgNameTree());
            });
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }


    @Operation(summary = "获取组织岗位绑定角色列表")
    @Parameter(name = "objectId", description = "对象主键", required = true)
    @Parameter(name = "type", description = "类型：organize,position", required = true)
    @SaCheckPermission(value = {"permission.auth", "permission.organize", "permission.role"}, mode = SaMode.OR)
    @GetMapping("/roleList")
    public ActionResult<PageListVO<RoleListVO>> getRoleList(RoleListPage pagination) {
        List<RoleRelationEntity> relationList = roleRelationService.getListPage(pagination);
        List<String> roleList = relationList.stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
        List<RoleListVO> listRes = new ArrayList<>();
        if (CollUtil.isNotEmpty(roleList)) {
            Map<String, RoleEntity> roleMap = roleService.getList(roleList, pagination, true).stream().collect(Collectors.toMap(RoleEntity::getId, t -> t));
            for (RoleRelationEntity item : relationList) {
                RoleEntity roleEntity = roleMap.get(item.getRoleId());
                if (roleEntity != null) {
                    RoleListVO vo = JsonUtil.getJsonToBean(roleEntity, RoleListVO.class);
                    listRes.add(vo);
                }
            }
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(listRes, paginationVO);
    }

    @Operation(summary = "角色绑定数据")
    @Parameter(name = "RoleRelationForm", description = "表单数据")
    @SaCheckPermission(value = {"permission.auth", "permission.organize", "permission.role"}, mode = SaMode.OR)
    @PostMapping
    public ActionResult<Object> roleAddObjectIds(@RequestBody RoleRelationForm form) {
        if (CollUtil.isNotEmpty(form.getIds()) && PermissionConst.USER.equals(form.getType())) {
            form.setIds(userService.getRelUserEnable(form.getIds()));
        }
        return roleRelationService.roleAddObjectIds(form);
    }

    @Operation(summary = "角色移除绑定")
    @Parameter(name = "RoleRelationForm", description = "表单数据")
    @SaCheckPermission(value = {"permission.auth", "permission.organize", "permission.role"}, mode = SaMode.OR)
    @PostMapping("/delete")
    public ActionResult<Object> delete(@RequestBody RoleRelationForm form) {
        roleRelationService.delete(form);
        return ActionResult.success(MsgCode.SU021.get());
    }


    @Operation(summary = "组织/岗位添加角色")
    @Parameter(name = "RoleRelationForm", description = "表单数据")
    @SaCheckPermission(value = {"permission.auth", "permission.organize", "permission.role"}, mode = SaMode.OR)
    @PostMapping("/addRoles")
    public ActionResult<Object> addRoles(@RequestBody AddRolesForm form) {
        roleRelationService.objectAddRoles(form);
        return ActionResult.success(MsgCode.SU002.get());
    }

    @Operation(summary = "组织/岗位移除角色")
    @Parameter(name = "RoleRelationForm", description = "表单数据")
    @SaCheckPermission(value = {"permission.auth", "permission.organize", "permission.role"}, mode = SaMode.OR)
    @PostMapping("/deleteRoles")
    public ActionResult<Object> deleteRoles(@RequestBody AddRolesForm form) {
        roleRelationService.objectDeleteRoles(form);
        return ActionResult.success(MsgCode.SU021.get());
    }
}

