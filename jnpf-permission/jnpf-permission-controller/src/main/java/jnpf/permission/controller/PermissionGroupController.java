package jnpf.permission.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.vo.ListVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.model.FlowWorkModel;
import jnpf.permission.entity.*;
import jnpf.permission.model.permissiongroup.PaginationPermissionGroup;
import jnpf.permission.model.permissiongroup.PermissionGroupListVO;
import jnpf.permission.model.permissiongroup.PermissionGroupModel;
import jnpf.permission.model.permissiongroup.ViewPermissionsModel;
import jnpf.permission.model.user.UserIdListVo;
import jnpf.permission.model.user.mod.UserIdModel;
import jnpf.permission.model.user.vo.BaseInfoVo;
import jnpf.permission.service.*;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "操作权限", description = "Authorize")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permission/PermissionGroup")
public class PermissionGroupController {

    private final PermissionGroupService permissionGroupService;
    private final UserService userService;
    private final AuthorizeService authorizeService;
    private final OrganizeService organizeService;
    private final PositionService positionService;
    private final RoleService roleService;
    private final GroupService groupService;

    @Operation(summary = "列表")
    @SaCheckPermission("permission.authGroup")
    @GetMapping
    public ActionResult<PageListVO<PermissionGroupListVO>> list(PaginationPermissionGroup pagination) {
        List<PermissionGroupEntity> data = permissionGroupService.list(pagination);
        List<PermissionGroupListVO> list = JsonUtil.getJsonToList(data, PermissionGroupListVO.class);
        list.forEach(t -> {
            String permissionMember = t.getPermissionMember();
            if (StringUtil.isEmpty(permissionMember)) {
                t.setPermissionMember("");
                return;
            }
            List<String> fullNameByIds = userService.getFullNameByIds(Arrays.asList(permissionMember.split(",")));
            StringJoiner stringJoiner = new StringJoiner(",");
            fullNameByIds.forEach(stringJoiner::add);
            t.setPermissionMember(stringJoiner.toString());
        });
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }

    @Operation(summary = "权限成员")
    @SaCheckPermission("permission.authGroup")
    @Parameter(name = "id", description = "主键", required = true)
    @GetMapping("/PermissionMember/{id}")
    public ActionResult<ListVO<BaseInfoVo>> permissionMember(@PathVariable("id") String id) {
        PermissionGroupEntity entity = permissionGroupService.permissionMember(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA003.get());
        }
        ListVO<BaseInfoVo> listVO = new ListVO<>();
        List<BaseInfoVo> list = new ArrayList<>();
        if (StringUtil.isEmpty(entity.getPermissionMember())) {
            listVO.setList(list);
            return ActionResult.success(listVO);
        }
        List<String> ids = Arrays.asList(entity.getPermissionMember().split(","));
        list = userService.selectedByIds(ids);
        listVO.setList(list);
        return ActionResult.success(listVO);
    }

    /**
     * 保存权限成员
     *
     * @param id          主键
     * @param userIdModel 用户id模型
     * @return
     */
    @Operation(summary = "保存权限成员")
    @SaCheckPermission("permission.authGroup")
            @Parameter(name = "id", description = "主键", required = true)
            @Parameter(name = "userIdModel", description = "用户id模型", required = true)
    @PostMapping("/PermissionMember/{id}")
    public ActionResult<ListVO<UserIdListVo>> savePermissionMember(@PathVariable("id") String id, @RequestBody UserIdModel userIdModel) {
        PermissionGroupEntity entity = permissionGroupService.info(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA003.get());
        }
        StringJoiner stringJoiner = new StringJoiner(",");
        List<String> userId = userIdModel.getIds();
        if (userId.isEmpty()) {
            ActionResult.success(MsgCode.SU002.get());
        }
        userId.forEach(stringJoiner::add);
        entity.setPermissionMember(stringJoiner.toString());
        permissionGroupService.update(id, entity);
        //根据ids获取上级，赋予新的权限
        authorizeService.setPermissionGroup(id, PermissionConst.PERMISSION_GROUP);
        return ActionResult.success(MsgCode.SU002.get());
    }

    /**
     * 详情
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "详情")
    @SaCheckPermission("permission.authGroup")
    @Parameter(name = "id", description = "主键", required = true)
    @GetMapping("/{id}")
    public ActionResult<PermissionGroupModel> info(@PathVariable("id") String id) {
        PermissionGroupEntity entity = permissionGroupService.info(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA003.get());
        }
        PermissionGroupModel model = JsonUtil.getJsonToBean(entity, PermissionGroupModel.class);
        return ActionResult.success(model);
    }

    @Operation(summary = "新建")
    @SaCheckPermission("permission.authGroup")
    @Parameter(name = "id", description = "模型", required = true)
    @PostMapping
    public ActionResult<String> crete(@RequestBody PermissionGroupModel model) {
        PermissionGroupEntity entity = JsonUtil.getJsonToBean(model, PermissionGroupEntity.class);
        if (permissionGroupService.isExistByFullName(null, entity)) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (StringUtil.isNotEmpty(entity.getEnCode()) && permissionGroupService.isExistByEnCode(entity.getEnCode(), null)) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        permissionGroupService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    @Operation(summary = "修改")
    @SaCheckPermission("permission.authGroup")
            @Parameter(name = "id", description = "主键", required = true)
            @Parameter(name = "model", description = "模型", required = true)
    @PutMapping("/{id}")
    public ActionResult<String> update(@PathVariable("id") String id, @RequestBody PermissionGroupModel model) {
        PermissionGroupEntity permissionGroupEntity = permissionGroupService.info(id);
        PermissionGroupEntity entity = JsonUtil.getJsonToBean(model, PermissionGroupEntity.class);
        if (permissionGroupService.isExistByFullName(id, entity)) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (StringUtil.isNotEmpty(entity.getEnCode()) && permissionGroupService.isExistByEnCode(entity.getEnCode(), id)) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        if (permissionGroupEntity.getEnabledMark() == 1 && entity.getEnabledMark() == 0) {
            userService.delCurRoleUser(null, Collections.singletonList(id));
        }
        permissionGroupService.update(id, entity);
        return ActionResult.success(MsgCode.SU004.get());
    }

    @Operation(summary = "删除")
    @SaCheckPermission("permission.authGroup")
    @Parameter(name = "id", description = "主键", required = true)
    @DeleteMapping("/{id}")
    public ActionResult<String> delete(@PathVariable("id") String id) {
        PermissionGroupEntity entity = permissionGroupService.info(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA003.get());
        }
        userService.delCurRoleUser(null, Collections.singletonList(id));
        permissionGroupService.delete(entity);
        List<AuthorizeEntity> listByObjectId = authorizeService.getListByObjectId(id, null);
        authorizeService.removeByIds(listByObjectId);
        return ActionResult.success(MsgCode.SU003.get());
    }

    @Operation(summary = "复制")
    @SaCheckPermission("permission.authGroup")
    @Parameter(name = "id", description = "主键", required = true)
    @PostMapping("/{id}/Actions/Copy")
    public ActionResult<PermissionGroupModel> actionsCopy(@PathVariable("id") String id) {
        PermissionGroupEntity info = permissionGroupService.info(id);
        if (info == null) {
            return ActionResult.fail(MsgCode.FA003.get());
        }
        PermissionGroupEntity entity = JsonUtil.getJsonToBean(info, PermissionGroupEntity.class);
        String copyNum = UUID.randomUUID().toString().substring(0, 5);
        String idNew = RandomUtil.uuId();
        entity.setId(idNew);
        entity.setFullName(entity.getFullName() + ".副本" + copyNum);
        entity.setEnCode(entity.getEnCode() + copyNum);
        entity.setCreatorTime(DateUtil.getNowDate());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        entity.setLastModifyTime(null);
        entity.setLastModifyUserId(null);
        entity.setPermissionMember(null);//清空授权对象
        if (entity.getEnCode().length() > 50 || entity.getFullName().length() > 50) {
            return ActionResult.fail(MsgCode.PRI006.get());
        }
        permissionGroupService.save(entity);
        //复制权限信息
        List<AuthorizeEntity> listByObjectId = authorizeService.getListByObjectId(id, null);
        for (AuthorizeEntity item : listByObjectId) {
            AuthorizeEntity itemEntity = JsonUtil.getJsonToBean(item, AuthorizeEntity.class);
            itemEntity.setId(RandomUtil.uuId());
            itemEntity.setObjectId(idNew);
            itemEntity.setCreatorTime(DateUtil.getNowDate());
            itemEntity.setCreatorUserId(UserProvider.getUser().getUserId());
            authorizeService.save(itemEntity);
        }
        return ActionResult.success(MsgCode.SU007.get());
    }

    /**
     * 获取菜单权限返回权限组
     *
     * @param model 模型
     * @return ignore
     */
    @Operation(summary = "获取菜单权限返回权限组")
            @Parameter(name = "id", description = "主键", required = true)
    @GetMapping("/getPermissionGroup")
    public ActionResult<Map<String, Object>> getPermissionGroup(ViewPermissionsModel model) {
        String objectType = model.getObjectType();
        String id = model.getId();
        if (checkDataById(id, objectType)) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        Map<String, Object> map = new HashMap<>(2);
        int type = 0; // 0未开启权限，1有
        List<FlowWorkModel> list;
        List<PermissionGroupEntity> permissionGroupByUserId = permissionGroupService.getPermissionGroupByObjectId(id, objectType);
        list = JsonUtil.getJsonToList(permissionGroupByUserId, FlowWorkModel.class);
        list.forEach(t -> t.setIcon("icon-ym icon-ym-authGroup"));
        if (!list.isEmpty()) {
            type = 1;
        } else {
            type = 2;
        }
        map.put("list", list);
        map.put("type", type);
        return ActionResult.success(map);
    }


    /**
     * 验证对象数据是否存在
     *
     * @param id
     * @param objectType
     * @return
     */
    private boolean checkDataById(String id, String objectType) {
        if (PermissionConst.COMPANY.equals(objectType) || PermissionConst.DEPARTMENT.equals(objectType)) {
            // 获取当前菜单开启了哪些权限
            OrganizeEntity entity = organizeService.getInfo(id);
            if (entity == null) {
                return true;
            }
        } else if ("position".equals(objectType)) {
            PositionEntity entity = positionService.getInfo(id);
            if (entity == null) {
                return true;
            }
        } else if ("user".equals(objectType)) {
            UserEntity entity = userService.getInfo(id);
            if (entity == null) {
                return true;
            }
        } else if ("role".equals(objectType)) {
            RoleEntity entity = roleService.getInfo(id);
            if (entity == null) {
                return true;
            }
        } else if ("group".equals(objectType)) {
            GroupEntity entity = groupService.getInfo(id);
            if (entity == null) {
                return true;
            }
        } else {
            return true;
        }
        return false;
    }

}
