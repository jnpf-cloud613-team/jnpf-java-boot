package jnpf.permission.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.collection.CollUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.controller.SuperController;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.permission.entity.GroupEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.entity.UserRelationEntity;
import jnpf.permission.model.user.mod.UserIdModel;
import jnpf.permission.model.usergroup.*;
import jnpf.permission.service.GroupService;
import jnpf.permission.service.UserRelationService;
import jnpf.permission.service.UserService;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 分组管理控制器
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/3/10 17:57
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "分组管理", description = "UserGroupController")
@RequestMapping("/api/permission/Group")
public class GroupController extends SuperController<GroupService, GroupEntity> {

    private final GroupService userGroupService;
    private final UserRelationService userRelationService;
    private final UserService userService;

    @Operation(summary = "获取分组管理列表")
    @GetMapping
    public ActionResult<PageListVO<GroupVO>> list(GroupPagination pagination) {
        pagination.setDataType(1);
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            pagination.setDataType(null);
        }
        List<GroupEntity> list = userGroupService.getList(pagination);
        List<GroupVO> jsonToList = JsonUtil.getJsonToList(list, GroupVO.class);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(jsonToList, paginationVO);
    }

    @Operation(summary = "创建")
    @Parameter(name = "userGroupCrForm", description = "新建模型", required = true)
    @SaCheckPermission(value = {"permission.user"})
    @PostMapping
    public ActionResult<Object> create(@RequestBody @Valid GroupForm form) {
        GroupEntity entity = JsonUtil.getJsonToBean(form, GroupEntity.class);
        // 判断名称是否重复
        if (Boolean.TRUE.equals(userGroupService.isExistByFullName(entity.getFullName(), null))) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (Boolean.TRUE.equals(userGroupService.isExistByEnCode(entity.getEnCode(), null))) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        userGroupService.crete(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    @Operation(summary = "更新")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "userGroupUpForm", description = "修改模型", required = true)
    @SaCheckPermission(value = {"permission.user"})
    @PutMapping("/{id}")
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid GroupForm form) {
        GroupEntity info = userGroupService.getInfo(id);
        if (info == null) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        GroupEntity groupEntity = JsonUtil.getJsonToBean(form, GroupEntity.class);
        // 判断名称是否重复
        if (Boolean.TRUE.equals(userGroupService.isExistByFullName(groupEntity.getFullName(), id))) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (Boolean.TRUE.equals(userGroupService.isExistByEnCode(groupEntity.getEnCode(), id))) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        userGroupService.update(id, groupEntity);
        return ActionResult.success(MsgCode.SU004.get());
    }

    @Operation(summary = "信息")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission(value = {"permission.user"})
    @GetMapping("/{id}")
    public ActionResult<GroupInfoVO> info(@PathVariable("id") String id) {
        GroupEntity entity = userGroupService.getInfo(id);
        GroupInfoVO vo = JsonUtil.getJsonToBean(entity, GroupInfoVO.class);
        return ActionResult.success(vo);
    }

    @Operation(summary = "删除")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission(value = {"permission.user"})
    @DeleteMapping("/{id}")
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        GroupEntity entity = userGroupService.getInfo(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA003.get());
        }
        List<UserRelationEntity> bingUserByRoleList = userRelationService.getListByObjectId(id, PermissionConst.GROUP);
        for (UserRelationEntity item : bingUserByRoleList) {
            userRelationService.removeById(item);
        }
        userGroupService.delete(entity);
        return ActionResult.success(MsgCode.SU003.get());
    }

    //++++++++++++++++++++++++++++++++++动作start++++++++++++++++++++++++++++++

    @Operation(summary = "添加用户")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "userIdModel", description = "参数对象", required = false)
    @SaCheckPermission(value = {"permission.user"})
    @PostMapping("{id}/Actions/AddUser")
    public ActionResult<GroupInfoVO> addUserToGroup(@PathVariable("id") String id, @RequestBody @Valid UserIdModel userIdModel) {
        GroupEntity info = userGroupService.getInfo(id);
        if (info == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        Set<String> userIds = userRelationService.getListByObjectId(id, PermissionConst.GROUP).stream().map(UserRelationEntity::getUserId).collect(Collectors.toSet());
        Set<String> adminIds = userService.getAdminList().stream().map(UserEntity::getId).collect(Collectors.toSet());
        List<UserRelationEntity> listRelation = new ArrayList<>();
        List<String> errList1 = new ArrayList<>();
        for (String userId : userIdModel.getIds()) {
            if (adminIds.contains(userId)) {
                errList1.add("超管不能添加");
                continue;
            }
            if (!userIds.contains(userId)) {
                UserRelationEntity userRelation = new UserRelationEntity();
                userRelation.setObjectId(id);
                userRelation.setUserId(userId);
                userRelation.setObjectType(PermissionConst.GROUP);
                listRelation.add(userRelation);
            }
        }
        if (CollUtil.isNotEmpty(listRelation)) {
            userRelationService.save(listRelation);
        }
        if (!errList1.isEmpty()) {
            return ActionResult.success(MsgCode.DB019.get());
        }
        return ActionResult.success(MsgCode.SU018.get());
    }

    @Operation(summary = "移除用户")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "userIdModel", description = "参数对象", required = false)
    @SaCheckPermission(value = {"permission.user"})
    @PostMapping("{id}/Actions/DeleteUser")
    public ActionResult<GroupInfoVO> deleteUserToGroup(@PathVariable("id") String id, @RequestBody @Valid UserIdModel userIdModel) {
        GroupEntity entity = userGroupService.getInfo(id);
        List<String> ids = userIdModel.getIds();
        if (entity != null && !ids.isEmpty()) {
            List<UserRelationEntity> listByObjectId = userRelationService.getListByObjectId(id, PermissionConst.GROUP);
            for (UserRelationEntity item : listByObjectId) {
                if (ids.contains(item.getUserId())) {
                    userRelationService.removeById(item.getId());
                }
            }
        }
        return ActionResult.success(MsgCode.SU021.get());
    }
    //++++++++++++++++++++++++++++++++++动作end++++++++++++++++++++++++++++++++

    @Operation(summary = "获取分组管理下拉框")
    @GetMapping("/Selector")
    public ActionResult<List<GroupSelectorVO>> selector() {
        List<GroupEntity> data = userGroupService.list();
        List<GroupSelectorVO> list = JsonUtil.getJsonToList(data, GroupSelectorVO.class);
        for (GroupSelectorVO vo : list) {
            vo.setIcon(PermissionConst.GROUP_ICON);
        }
        return ActionResult.success(list);
    }

    @Operation(summary = "自定义范围获取分组下拉框")
    @Parameter(name = "positionConditionModel", description = "岗位选择模型", required = true)
    @PostMapping("/GroupCondition")
    public ActionResult<List<GroupSelectorVO>> positionCondition(@RequestBody UserIdModel idModel) {
        List<GroupEntity> data = userGroupService.getListByIds(idModel.getIds());
        List<GroupSelectorVO> list = JsonUtil.getJsonToList(data, GroupSelectorVO.class);
        for (GroupSelectorVO vo : list) {
            vo.setIcon(PermissionConst.GROUP_ICON);
        }
        return ActionResult.success(list);
    }

}
