package jnpf.permission.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.permission.entity.*;
import jnpf.permission.model.standing.*;
import jnpf.permission.model.usergroup.GroupInfoVO;
import jnpf.permission.service.*;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 身份管理控制器
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/3/4 18:25:30
 */
@Tag(name = "身份管理控制器", description = "Standing")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permission/Standing")
@Slf4j
public class StandingController {
    private final StandingService standingService;
    private final AuthorizeService authorizeService;
    private final UserService userService;
    private final UserRelationService userRelationService;
    private final RoleRelationService roleRelationService;
    private final RoleService roleService;
    private final OrganizeService organizeService;

    @Operation(summary = "列表")
    @SaCheckPermission(value = {"permission.identity"})
    @GetMapping
    public ActionResult<PageListVO<StandingVO>> list(StandingPagination pagination) {
        List<StandingEntity> list = standingService.getList(pagination);
        List<StandingVO> listVo = JsonUtil.getJsonToList(list, StandingVO.class);
        for (StandingVO item : listVo) {
            item.setIsSystem(Objects.equals(item.getIsSystem(), 1) ? 1 : 0);
        }
        return ActionResult.page(listVo, null);
    }

    @Operation(summary = "创建")
    @Parameter(name = "StandingForm", description = "新建模型", required = true)
    @SaCheckPermission(value = {"permission.identity"})
    @PostMapping
    public ActionResult<String> create(@RequestBody @Valid StandingForm form) {
        StandingEntity entity = JsonUtil.getJsonToBean(form, StandingEntity.class);
        // 判断名称是否重复
        if (Boolean.TRUE.equals(standingService.isExistByFullName(entity.getFullName(), null))) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (Boolean.TRUE.equals(standingService.isExistByEnCode(entity.getEnCode(), null))) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        standingService.crete(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    @Operation(summary = "更新")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "StandingForm", description = "修改模型", required = true)
    @SaCheckPermission(value = {"permission.identity"})
    @PutMapping("/{id}")
    public ActionResult<String> update(@PathVariable("id") String id, @RequestBody @Valid StandingForm form) {
        StandingEntity info = standingService.getInfo(id);
        if (info == null) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        StandingEntity entity = JsonUtil.getJsonToBean(form, StandingEntity.class);
        // 判断名称是否重复
        if (Boolean.TRUE.equals(standingService.isExistByFullName(entity.getFullName(), id))) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (Boolean.TRUE.equals(standingService.isExistByEnCode(entity.getEnCode(), id))) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        standingService.update(id, entity);
        return ActionResult.success(MsgCode.SU004.get());
    }

    @Operation(summary = "信息")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission(value = {"permission.identity"})
    @GetMapping("/{id}")
    public ActionResult<StandingInfoVO> info(@PathVariable("id") String id) {
        StandingEntity entity = standingService.getInfo(id);
        StandingInfoVO vo = JsonUtil.getJsonToBean(entity, StandingInfoVO.class);
        return ActionResult.success(vo);
    }

    @Operation(summary = "删除")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission(value = {"permission.identity"})
    @DeleteMapping("/{id}")
    public ActionResult<String> delete(@PathVariable("id") String id) {
        StandingEntity entity = standingService.getInfo(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA003.get());
        }
        standingService.delete(entity);
        return ActionResult.success(MsgCode.SU003.get());
    }

    //++++++++++++++++++++++++++++++++动作start++++++++++++++++++++++++++++++++++++++

    @Operation(summary = "添加岗位或用户角色")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "userIdModel", description = "参数对象", required = false)
    @SaCheckPermission(value = {"permission.user", "permission.identity"}, mode = SaMode.OR)
    @PostMapping("{id}/Actions/AddObject")
    public ActionResult<GroupInfoVO> addObject(@PathVariable("id") String id, @RequestBody @Valid StandingActionForm model) {
        //AuthorizeEntity  objectType:standing身份绑定类型itemType：role/position
        StandingEntity info = standingService.getInfo(id);
        List<String> ids = model.getIds();
        List<String> authorizeList = authorizeService.getListByObjectAndItem(id, model.getType())
                .stream().map(AuthorizeEntity::getObjectId).collect(Collectors.toList());
        if (info != null && !ids.isEmpty()) {
            for (String thisId : ids) {
                if (!authorizeList.contains(thisId)) {
                    AuthorizeEntity authorize = new AuthorizeEntity();
                    authorize.setObjectId(thisId);
                    authorize.setObjectType(model.getType());
                    authorize.setItemId(id);
                    authorize.setItemType(PermissionConst.STAND);
                    authorizeService.save(authorize);
                }
            }
        }

        List<AuthorizeEntity> standList = authorizeService.getAuthorizeByItem(PermissionConst.STAND, id);
        List<String> posIds = standList.stream().filter(t -> PermissionConst.POSITION.equals(t.getObjectType())).map(AuthorizeEntity::getObjectId).collect(Collectors.toList());
        List<String> roleIds = standList.stream().filter(t -> PermissionConst.ROLE.equals(t.getObjectType())).map(AuthorizeEntity::getObjectId).collect(Collectors.toList());
        Set<String> userIds = new HashSet<>();
        userIds.addAll(userRelationService.getListByObjectIdAll(posIds).stream().map(UserRelationEntity::getUserId).collect(Collectors.toList()));
        userIds.addAll(roleRelationService.getListByRoleId(roleIds, PermissionConst.USER).stream().map(RoleRelationEntity::getObjectId).collect(Collectors.toList()));
        userService.delCurUser(MsgCode.PS010.get(), new ArrayList<>(userIds));
        return ActionResult.success(MsgCode.SU018.get());
    }

    @Operation(summary = "移除岗位或用户角色")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "userIdModel", description = "参数对象", required = false)
    @SaCheckPermission(value = {"permission.user", "permission.identity"}, mode = SaMode.OR)
    @PostMapping("{id}/Actions/DeleteObject")
    public ActionResult<GroupInfoVO> deleteObject(@PathVariable("id") String id, @RequestBody @Valid StandingActionForm model) {
        StandingEntity info = standingService.getInfo(id);
        String notRemove = "";
        List<String> codeList = Arrays.asList(PermissionConst.MANAGER_CODE, PermissionConst.DEVELOPER_CODE, PermissionConst.USER_CODE);
        if (codeList.contains(info.getEnCode())) {
            RoleEntity byEnCode = roleService.getByEnCode(info.getEnCode());
            notRemove = byEnCode != null ? byEnCode.getId() : "";
        }

        List<String> ids = model.getIds();
        if (info != null && !ids.isEmpty()) {
            //获取身份下的岗位或者用户角色(itemType)
            List<AuthorizeEntity> listByObjectId = authorizeService.getListByObjectAndItem(id, model.getType());
            for (AuthorizeEntity item : listByObjectId) {
                if (ids.contains(item.getObjectId()) && !Objects.equals(notRemove, item.getObjectId())) {
                    authorizeService.removeById(item);
                }
            }
        }
        List<AuthorizeEntity> standList = authorizeService.getAuthorizeByItem(PermissionConst.STAND, id);
        List<String> posIds = standList.stream().filter(t -> PermissionConst.POSITION.equals(t.getObjectType())).map(AuthorizeEntity::getObjectId).collect(Collectors.toList());
        List<String> roleIds = standList.stream().filter(t -> PermissionConst.ROLE.equals(t.getObjectType())).map(AuthorizeEntity::getObjectId).collect(Collectors.toList());
        if (PermissionConst.POSITION.equals(model.getType())) {
            posIds.addAll(ids);
        } else {
            roleIds.addAll(ids);
        }
        Set<String> userIds = new HashSet<>();
        userIds.addAll(userRelationService.getListByObjectIdAll(posIds).stream().map(UserRelationEntity::getUserId).collect(Collectors.toList()));
        userIds.addAll(roleRelationService.getListByRoleId(roleIds, PermissionConst.USER).stream().map(RoleRelationEntity::getObjectId).collect(Collectors.toList()));
        userService.delCurUser(MsgCode.PS010.get(), new ArrayList<>(userIds));
        return ActionResult.success(MsgCode.SU021.get());
    }

    //++++++++++++++++++++++++++++++++动作end++++++++++++++++++++++++++++++++++++++++

    @Operation(summary = "列表")
    @SaCheckPermission(value = {"permission.identity"})
    @GetMapping("/list")
    public ActionResult<PageListVO<StandingVO>> lists(StandingPagination pagination) {
        StandingEntity info = standingService.getInfo(pagination.getId());
        List<StandingVO> list = new ArrayList<>();
        //岗位、用户角色
        if (PermissionConst.POSITION.equals(pagination.getType())) {
            Map<String, Object> allOrgsTreeName = organizeService.getAllOrgsTreeName();
            List<PositionEntity> listByIds = standingService.getPosPage(pagination);
            for (PositionEntity pe : listByIds) {
                StandingVO vo = JsonUtil.getJsonToBean(pe, StandingVO.class);
                vo.setFullName(allOrgsTreeName.get(pe.getOrganizeId()) + "/" + pe.getFullName());
                list.add(vo);
            }
        } else {
            List<RoleEntity> listByIds = standingService.getRolePage(pagination);
            list = JsonUtil.getJsonToList(listByIds, StandingVO.class);
            for (StandingVO vo : list) {
                vo.setIsSystem(Objects.equals(vo.getGlobalMark(), 1) ? 1 : 0);
                if (Objects.equals(info.getIsSystem(), 1) && info.getEnCode().equals(vo.getEnCode())) {
                    vo.setDisable(1);
                }
            }
        }
        PaginationVO jsonToBean = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(list, jsonToBean);
    }

}
