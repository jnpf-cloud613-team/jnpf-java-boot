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
import jnpf.message.service.SynThirdDingTalkService;
import jnpf.message.service.SynThirdQyService;
import jnpf.permission.entity.GroupEntity;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.entity.UserRelationEntity;
import jnpf.permission.model.position.PosConModel;
import jnpf.permission.model.user.page.UserPagination;
import jnpf.permission.model.user.vo.UserListVO;
import jnpf.permission.model.userrelation.UserRelationForm;
import jnpf.permission.service.GroupService;
import jnpf.permission.service.PositionService;
import jnpf.permission.service.UserRelationService;
import jnpf.permission.service.UserService;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.ThreadPoolExecutorUtil;
import jnpf.util.enums.DictionaryDataEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户关系
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Tag(name = "用户关系", description = "UserRelation")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permission/UserRelation")
@Log
public class UserRelationController extends SuperController<UserRelationService, UserRelationEntity> {

    private final UserRelationService userRelationService;
    private final SynThirdDingTalkService synThirdDingTalkService;
    private final UserService userService;
    private final PositionService positionService;
    private final GroupService groupService;
    private final SynThirdQyService synThirdQyService;
    private final DictionaryDataService dictionaryDataApi;

    @Operation(summary = "获取组织岗位绑定用户列表")
    @Parameter(name = "objectId", description = "对象主键", required = true)
    @Parameter(name = "type", description = "类型：organize,position", required = true)
    @SaCheckPermission(value = {"permission.organize"}, mode = SaMode.OR)
    @GetMapping("/userList")
    public ActionResult<PageListVO<UserListVO>> getRoleList(UserPagination pagination) {
        List<UserListVO> relationList = userRelationService.getListPage(pagination);
        Map<String, String> positionMap = positionService.getPosFullNameMap();
        List<DictionaryDataEntity> dataServiceList4 = dictionaryDataApi.getListByTypeDataCode(DictionaryDataEnum.SEX_TYPE.getDictionaryTypeId());
        Map<String, String> genderMap = dataServiceList4.stream().collect(Collectors.toMap(DictionaryDataEntity::getEnCode, DictionaryDataEntity::getFullName));
        //责任人（有岗位信息时添加属性）
        String dutyUser = "";
        if (StringUtil.isNotEmpty(pagination.getPositionId())) {
            PositionEntity info = positionService.getInfo(pagination.getPositionId());
            if (info != null) dutyUser = info.getDutyUser();
        }
        for (UserListVO userListVO : relationList) {
            // 时间小于当前时间则判断已解锁
            if (userListVO.getEnabledMark() != null && userListVO.getEnabledMark() != 0) {
                if (Objects.nonNull(userListVO.getUnlockTime()) && userListVO.getUnlockTime().getTime() > System.currentTimeMillis()) {
                    userListVO.setEnabledMark(2);
                } else if (Objects.nonNull(userListVO.getUnlockTime()) && userListVO.getUnlockTime().getTime() < System.currentTimeMillis()) {
                    userListVO.setEnabledMark(1);
                }
            }
            StringJoiner positionJoiner = new StringJoiner(",");
            StringJoiner organizeJoiner = new StringJoiner(",");
            List<UserRelationEntity> allPostion = userRelationService.getListByObjectType(userListVO.getId(), PermissionConst.POSITION);
            if (CollUtil.isNotEmpty(allPostion)) {
                for (UserRelationEntity item : allPostion) {
                    String posName = positionMap.get(item.getObjectId());
                    if (posName != null) {
                        positionJoiner.add(posName);
                        organizeJoiner.add(posName.substring(0, posName.lastIndexOf("/")));
                    }

                }
            }
            userListVO.setGender(genderMap.get(userListVO.getGender()));
            userListVO.setPosition(positionJoiner.toString());
            userListVO.setOrganize(organizeJoiner.toString());
            if (StringUtil.isNotEmpty(dutyUser) && dutyUser.equals(userListVO.getId())) {
                userListVO.setIsDutyUser(1);
            }
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(relationList, paginationVO);
    }


    @Operation(summary = "岗位/分组添加用户")
    @Parameter(name = "UserRelationForm", description = "用户关联表单")
    @SaCheckPermission(value = {"permission.auth", "permission.organize", "permission.user"}, mode = SaMode.OR)
    @PostMapping
    public ActionResult<String> save(@RequestBody UserRelationForm form) {
        if (CollUtil.isEmpty(form.getUserIds())) {
            return ActionResult.fail(MsgCode.SYS134.get());
        }
        if (Objects.equals(form.getObjectType(), PermissionConst.GROUP)) {
            return groupAddUser(form);
        } else {
            form.setUserIds(userService.getRelUserEnable(form.getUserIds()));
            return positionAddUser(form);
        }
    }

    //分组添加用户
    private ActionResult<String> groupAddUser(UserRelationForm form) {
        GroupEntity info = groupService.getInfo(form.getObjectId());
        if (info == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        Set<String> adminIds = userService.getAdminList().stream().map(UserEntity::getId).collect(Collectors.toSet());
        Set<String> userIds = userRelationService.getListByObjectId(form.getObjectId(), PermissionConst.GROUP).stream().map(UserRelationEntity::getUserId).collect(Collectors.toSet());
        List<UserRelationEntity> listRelation = new ArrayList<>();
        for (String userId : form.getUserIds()) {
            if (!userIds.contains(userId) && !adminIds.contains(userId)) {
                UserRelationEntity userRelation = new UserRelationEntity();
                userRelation.setObjectId(form.getObjectId());
                userRelation.setUserId(userId);
                userRelation.setObjectType(form.getObjectType());
                listRelation.add(userRelation);
            }
        }
        if (CollUtil.isNotEmpty(listRelation)) {
            userRelationService.save(listRelation);
        }
        return ActionResult.success(MsgCode.SU018.get());
    }

    //岗位添加用户
    private ActionResult<String> positionAddUser(UserRelationForm form) {
        String posId = form.getObjectId();
        PositionEntity info = positionService.getInfo(posId);
        if (info == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        List<String> errList1 = new ArrayList<>();
        List<PositionEntity> allPos = positionService.getList(false);
        List<String> userIds = new ArrayList<>(form.getUserIds());

        //岗位-移除数据库已有数据  和  超管用户。
        List<UserRelationEntity> listByObjectId = userRelationService.getListByObjectId(info.getId(), PermissionConst.POSITION);
        Set<String> posUsers = listByObjectId.stream().map(UserRelationEntity::getUserId).collect(Collectors.toSet());
        Set<String> adminIds = userService.getAdminList().stream().map(UserEntity::getId).collect(Collectors.toSet());
        userIds = userIds.stream().filter(t -> !posUsers.contains(t)).collect(Collectors.toList());

        //约束判断
        PosConModel conModel = new PosConModel();
        if (Objects.equals(info.getIsCondition(), 1)) {
            conModel = JsonUtil.getJsonToBean(info.getConditionJson(), PosConModel.class);
            conModel.init();
        }
        //用户基数限制
        if (conModel.getNumFlag() && !userIds.isEmpty() && conModel.getUserNum() <= posUsers.size()) {
            return ActionResult.fail(MsgCode.SYS135.get(MsgCode.PS004.get()));
        }
        List<String> ids = new ArrayList<>();
        List<UserRelationEntity> listRelation = new ArrayList<>();

        for (String userId : userIds) {
            if (adminIds.contains(userId)) {
                errList1.add("超管不能添加");
            } else {
                List<String> errList2 = checkPosition(userId, allPos, posId, conModel);
                if (errList2.isEmpty()) {
                    //添加数量超出权限基数后跳出循环
                    if (conModel.getNumFlag() && conModel.getUserNum() <= (ids.size() + posUsers.size())) {
                        errList1.add(MsgCode.SYS135.get(MsgCode.PS004.get()));
                        break;
                    }
                    setEntity(form, userId, ids, posId, info, listRelation);
                } else {
                    errList1.addAll(errList2);
                }
            }
        }
        if (CollUtil.isNotEmpty(ids)) {
            synThird(form, ids, info);
            userRelationService.save(listRelation);
            userService.delCurUser(MsgCode.PS010.get(), ids);
        }

        if (CollUtil.isNotEmpty(errList1) && CollUtil.isNotEmpty(ids)) {
            return ActionResult.success(MsgCode.SYS139.get());
        } else if (CollUtil.isNotEmpty(errList1)) {
            return ActionResult.fail(MsgCode.DB019.get());
        }
        return ActionResult.success(MsgCode.SU018.get());
    }

    private @NotNull List<String> checkPosition(String userId, List<PositionEntity> allPos, String posId, PosConModel conModel) {
        List<String> errList2 = new ArrayList<>();
        List<String> thisUserPos = userRelationService.getListByUserId(userId, PermissionConst.POSITION).stream()
                .map(UserRelationEntity::getObjectId).collect(Collectors.toList());
        //现有角色和当前这个角色互斥
        List<PositionEntity> posList = allPos.stream().filter(t -> thisUserPos.contains(t.getId())).collect(Collectors.toList());
        for (PositionEntity p : posList) {
            if (Objects.equals(p.getIsCondition(), 1)) {
                PosConModel conModelP = JsonUtil.getJsonToBean(p.getConditionJson(), PosConModel.class);
                conModelP.init();
                if (conModelP.getMutualExclusionFlag() && conModelP.getMutualExclusion().contains(posId)) {
                    errList2.add(MsgCode.SYS137.get());
                }
            }
        }
        //互斥
        if (conModel.getMutualExclusionFlag() && conModel.getMutualExclusion().stream().anyMatch(thisUserPos::contains)) {
            errList2.add(MsgCode.SYS137.get());
        }
        //先决
        if (conModel.getPrerequisiteFlag() && !new HashSet<>(thisUserPos).containsAll(conModel.getPrerequisite())) {
            errList2.add(MsgCode.SYS138.get());
        }
        return errList2;
    }

    private static void setEntity(UserRelationForm form, String userId, List<String> ids, String posId, PositionEntity info, List<UserRelationEntity> listRelation) {
        ids.add(userId);
        UserRelationEntity userRelation = new UserRelationEntity();
        userRelation.setObjectId(posId);
        userRelation.setUserId(userId);
        userRelation.setObjectType(form.getObjectType());
        //岗位添加组织关系方便使用
        UserRelationEntity userOrgRelation = new UserRelationEntity();
        userOrgRelation.setObjectId(info.getOrganizeId());
        userOrgRelation.setUserId(userId);
        userOrgRelation.setObjectType(PermissionConst.ORGANIZE);
        listRelation.add(userRelation);
        listRelation.add(userOrgRelation);
    }

    //第三方同步
    private void synThird(UserRelationForm form, List<String> ids, PositionEntity info) {
        ThreadPoolExecutorUtil.getExecutor().execute(() -> {
            try {

                List<UserEntity> userList = userService.getUserList(ids);
                for (UserEntity entity : userList) {
                    entity.setOrganizeId(info.getOrganizeId());
                }//创建组织后判断是否需要同步到企业微信
                synThirdQyService.createUserSysToQy(false, userList, "", form.getObjectId());
                //创建组织后判断是否需要同步到钉钉
                synThirdDingTalkService.createUserSysToDing(false, userList, "", form.getObjectId());


            } catch (Exception e) {
                log.config("创建组织后同步失败到企业微信或钉钉失败，异常：" + e.getMessage());
            }
        });
    }

    @Operation(summary = "岗位/分组移除用户绑定")
    @Parameter(name = "RoleRelationForm", description = "表单数据")
    @SaCheckPermission(value = {"permission.auth", "permission.organize", "permission.role"}, mode = SaMode.OR)
    @PostMapping("/delete")
    public ActionResult<Object> delete(@RequestBody UserRelationForm form) {
        userRelationService.delete(form);
        ThreadPoolExecutorUtil.getExecutor().execute(() -> {
            try {

                List<UserEntity> userList = userService.getUserList(form.getUserIds());
                List<String> collect = userList.stream().map(UserEntity::getId)
                        .collect(Collectors.toList());
                //创建组织后判断是否需要同步到企业微信
                synThirdQyService.deleteUserSysToQy(false, collect, "", form.getObjectId());
                //创建组织后判断是否需要同步到钉钉
                synThirdDingTalkService.deleteUserSysToDing(false, collect, "", form.getObjectId());


            } catch (Exception e) {
                log.config("创建组织后同步失败到企业微信或钉钉失败，异常：" + e.getMessage());
            }
        });
        return ActionResult.success(MsgCode.SU021.get());
    }
}

