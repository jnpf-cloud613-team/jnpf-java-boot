package jnpf.permission.service.impl;

import cn.hutool.core.collection.CollUtil;
import jnpf.base.ActionResult;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.permission.entity.*;
import jnpf.permission.mapper.*;
import jnpf.permission.model.position.PosConModel;
import jnpf.permission.model.rolerelaiton.*;
import jnpf.permission.service.RoleRelationService;
import jnpf.permission.util.UserUtil;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

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
@Service
@RequiredArgsConstructor
public class RoleRelationServiceImpl extends SuperServiceImpl<RoleRelationMapper, RoleRelationEntity> implements RoleRelationService {

    private final UserUtil userUtil;
    private final UserMapper userMapper;
    private final PositionMapper positionMapper;
    private final UserRelationMapper userRelationMapper;
    private final RoleMapper roleMapper;

    @Override
    public List<RoleRelationEntity> getListPage(RoleListPage pagination) {
        return this.baseMapper.getListPage(pagination);
    }

    @Override
    public List<RoleRelationUserVo> getUserPage(RoleRelationPage pagination) {
        return this.baseMapper.getUserPage(pagination);
    }

    @Override
    public List<RoleRelationOrgVo> getOrgPage(RoleRelationPage pagination) {
        return this.baseMapper.getOrgPage(pagination);
    }

    @Override
    public List<RoleRelationOrgVo> getPosPage(RoleRelationPage pagination) {
        return this.baseMapper.getPosPage(pagination);
    }

    @Override
    public List<RoleRelationEntity> getListByObjectId(String objectId, String objectType) {
        return this.baseMapper.getListByObjectId(objectId, objectType);
    }

    @Override
    public List<RoleRelationEntity> getListByObjectId(List<String> objectId, String objectType) {
        return this.baseMapper.getListByObjectId(objectId, objectType);
    }

    @Override
    public List<RoleRelationEntity> getListByRoleId(String roleId, String objectType) {
        return this.baseMapper.getListByRoleId(roleId, objectType);
    }

    @Override
    public List<RoleRelationEntity> getListByRoleId(List<String> roleId, String objectType) {
        return this.baseMapper.getListByRoleId(roleId, objectType);
    }

    @Override
    public ActionResult<Object> roleAddObjectIds(RoleRelationForm form) {
        if (CollUtil.isEmpty(form.getIds())) {
            return ActionResult.fail(MsgCode.SYS134.get());
        }
        String roleId = form.getRoleId();
        RoleEntity info = roleMapper.getInfo(roleId);
        if (info == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }

        String type = form.getType();
        List<String> ids = new ArrayList<>(form.getIds());
        List<String> errList1 = new ArrayList<>();
        List<String> objectIds = new ArrayList<>();

        //角色-移除数据库已有数据。
        List<RoleRelationEntity> listByRoleId = this.getListByRoleId(roleId, type);
        Set<String> roleSet = listByRoleId.stream().map(RoleRelationEntity::getObjectId).collect(Collectors.toSet());
        Set<String> adminIds = userMapper.getAdminList().stream().map(UserEntity::getId).collect(Collectors.toSet());
        ids = ids.stream().filter(t -> !roleSet.contains(t)).collect(Collectors.toList());

        //用户角色约束判断
        PosConModel posConModel = new PosConModel();
        if (Objects.equals(info.getIsCondition(), 1)) {
            posConModel = JsonUtil.getJsonToBean(info.getConditionJson(), PosConModel.class);
            posConModel.init();
        }
        //用户基数限制
        if (posConModel.getNumFlag() && (!ids.isEmpty() && posConModel.getUserNum() <= roleSet.size())) {
            return ActionResult.fail(MsgCode.SYS135.get(MsgCode.PS006.get()));
        }

        for (String objectId : ids) {
            if (adminIds.contains(objectId)) {
                errList1.add("超管不能添加");
            } else {
                List<String> errList2 = checkRole(objectId, type, roleId, posConModel);
                if (errList2.isEmpty()) {
                    //达到用户基数限制跳出循环
                    if (posConModel.getNumFlag() && posConModel.getUserNum() <= (objectIds.size() + roleSet.size())) {
                        errList1.add(MsgCode.SYS135.get(MsgCode.PS006.get()));
                        break;
                    }
                    objectIds.add(objectId);
                    setEntity(objectId, roleId, type);
                } else {
                    errList1.addAll(errList2);
                }
            }

        }
        //修改关系-相关用户踢下线
        this.delCurUser(type, objectIds);

        if (CollUtil.isNotEmpty(errList1) && CollUtil.isNotEmpty(objectIds)) {
            return ActionResult.success(MsgCode.SYS139.get());
        } else if (CollUtil.isNotEmpty(errList1)) {
            return ActionResult.fail(MsgCode.DB019.get());
        }
        return ActionResult.success(MsgCode.SU018.get());
    }

    private void setEntity(String objectId, String roleId, String type) {
        RoleRelationEntity entity = new RoleRelationEntity();
        entity.setId(RandomUtil.uuId());
        entity.setObjectId(objectId);
        entity.setRoleId(roleId);
        entity.setObjectType(type);
        this.save(entity);
    }

    private @NotNull List<String> checkRole(String objectId, String type, String roleId, PosConModel posConModel) {
        List<String> errList2 = new ArrayList<>();

        //角色约束判断
        if (PermissionConst.USER.equals(type)) {
            List<RoleRelationEntity> userRoleList = this.getListByObjectId(objectId, PermissionConst.USER);
            List<String> thisUserRole = userRoleList.stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
            List<RoleEntity> roleList = roleMapper.getListByIds(thisUserRole);
            //用户现有角色和当前互斥
            for (RoleEntity role : roleList) {
                if (Objects.equals(role.getIsCondition(), 1)) {
                    PosConModel conModelP = JsonUtil.getJsonToBean(role.getConditionJson(), PosConModel.class);
                    conModelP.init();
                    if (conModelP.getMutualExclusionFlag() && conModelP.getMutualExclusion().contains(roleId)) {
                        errList2.add(MsgCode.SYS137.get());
                    }
                }
            }
            //互斥
            if (posConModel.getMutualExclusionFlag() && posConModel.getMutualExclusion().stream().anyMatch(thisUserRole::contains)) {
                errList2.add(MsgCode.SYS137.get());
            }
            //先决
            if (posConModel.getPrerequisiteFlag() && !thisUserRole.containsAll(posConModel.getPrerequisite())) {
                errList2.add(MsgCode.SYS138.get());
            }
        }
        return errList2;
    }

    @Override
    public void delete(RoleRelationForm form) {
        List<RoleRelationEntity> list = this.baseMapper.getListByForm(form);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        for (RoleRelationEntity item : list) {
            this.removeById(item);
        }
        //一次移除只能是用类型数据
        List<String> objectIds = list.stream().map(RoleRelationEntity::getObjectId).collect(Collectors.toList());
        //修改关系-相关用户踢下线
        this.delCurUser(form.getType(), objectIds);
    }

    @Override
    public void objectAddRoles(AddRolesForm form) {
        this.baseMapper.objectAddRoles(form);
        this.delCurUser(form.getType(), Arrays.asList(form.getObjectId()));
    }

    public void objectDeleteRoles(AddRolesForm form) {
        this.baseMapper.objectDeleteRoles(form);
        this.delCurUser(form.getType(), Arrays.asList(form.getObjectId()));
    }

    private void delCurUser(String type, List<String> objectIds) {
        //修改关系-相关用户踢下线
        List<String> userIds = new ArrayList<>();
        if (PermissionConst.USER.equals(type)) {
            userIds.addAll(objectIds);
        }
        if (PermissionConst.POSITION.equals(type)) {
            List<PositionEntity> listByOrgIds = positionMapper.getListByIds(objectIds);
            List<String> positionIds = listByOrgIds.stream().map(PositionEntity::getId).collect(Collectors.toList());
            List<UserRelationEntity> listByObjectIdAll = userRelationMapper.getListByObjectIdAll(positionIds);
            userIds.addAll(listByObjectIdAll.stream().map(UserRelationEntity::getUserId).collect(Collectors.toList()));
        }
        if (CollUtil.isNotEmpty(userIds)) {
            userUtil.delCurUser(MsgCode.PS010.get(), userIds);
        }
    }
}
