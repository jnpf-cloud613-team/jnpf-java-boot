package jnpf.permission.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jnpf.base.UserInfo;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.entity.UserRelationEntity;
import jnpf.permission.mapper.*;
import jnpf.permission.model.position.PosistionCurrentModel;
import jnpf.permission.model.user.page.UserPagination;
import jnpf.permission.model.user.vo.UserListVO;
import jnpf.permission.model.userrelation.UserRelationForm;
import jnpf.permission.service.UserRelationService;
import jnpf.permission.util.UserUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
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
public class UserRelationServiceImpl extends SuperServiceImpl<UserRelationMapper, UserRelationEntity> implements UserRelationService {

    private final UserUtil userUtil;
    private final UserMapper userMapper;
    private final PositionMapper positionMapper;
    private final OrganizeMapper organizeMapper;
    private final RoleRelationMapper roleRelationMapper;

    @Override
    public List<UserListVO> getListPage(UserPagination pagination) {
        String objectType = StringUtil.isNotEmpty(pagination.getPositionId()) ? PermissionConst.POSITION : PermissionConst.ORGANIZE;
        String objectId = StringUtil.isNotEmpty(pagination.getPositionId()) ? pagination.getPositionId() : pagination.getOrganizeId();
        //是否显示子孙组织用户
        List<String> orgIds = new ArrayList<>();
        if (Objects.equals(pagination.getShowSubOrganize(), 1)) {
            List<OrganizeEntity> allChild = organizeMapper.getAllChild(pagination.getOrganizeId());
            orgIds.addAll(allChild.stream().map(OrganizeEntity::getId).collect(Collectors.toList()));
        }

        MPJLambdaWrapper<UserRelationEntity> queryWrapper = JoinWrappers.lambda(UserRelationEntity.class);
        queryWrapper.selectAs(UserEntity::getId, UserListVO::getId);
        queryWrapper.selectAs(UserEntity::getAccount, UserListVO::getAccount);
        queryWrapper.selectAs(UserEntity::getRealName, UserListVO::getRealName);
        queryWrapper.selectAs(UserEntity::getGender, UserListVO::getGender);
        queryWrapper.selectAs(UserEntity::getMobilePhone, UserListVO::getMobilePhone);
        queryWrapper.selectAs(UserEntity::getEnabledMark, UserListVO::getEnabledMark);
        queryWrapper.selectAs(UserEntity::getUnlockTime, UserListVO::getUnlockTime);
        queryWrapper.selectMax(UserRelationEntity::getId, UserListVO::getMaxRelationId);

        queryWrapper.leftJoin(UserEntity.class, UserEntity::getId, UserRelationEntity::getUserId);
        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            queryWrapper.and(
                    t -> t.like(UserEntity::getAccount, pagination.getKeyword())
                            .or().like(UserEntity::getRealName, pagination.getKeyword())
                            .or().like(UserEntity::getMobilePhone, pagination.getKeyword())
            );
        }
        if (pagination.getEnabledMark() != null) {
            queryWrapper.eq(UserEntity::getEnabledMark, pagination.getEnabledMark());
        }

        if (!orgIds.isEmpty()) {
            queryWrapper.in(UserRelationEntity::getObjectId, orgIds);
        } else {
            queryWrapper.eq(UserRelationEntity::getObjectId, objectId);
        }

        queryWrapper.eq(UserRelationEntity::getObjectType, objectType);

        queryWrapper.orderByDesc("maxRelationId");

        queryWrapper.groupBy(UserEntity::getId, UserEntity::getAccount, UserEntity::getRealName, UserEntity::getGender, UserEntity::getMobilePhone,
                UserEntity::getEnabledMark, UserEntity::getUnlockTime);
        Page<UserListVO> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<UserListVO> data = this.selectJoinListPage(page, UserListVO.class, queryWrapper);
        return pagination.setData(data.getRecords(), page.getTotal());
    }

    @Override
    public List<UserRelationEntity> getListByUserId(String userId) {
        return this.baseMapper.getListByUserId(userId);
    }

    @Override
    public List<UserRelationEntity> getListByUserIdAndObjType(String userId, String objectType) {
        return this.baseMapper.getListByUserIdAndObjType(userId, objectType);
    }

    @Override
    public List<UserRelationEntity> getListByUserIdAll(List<String> userId) {
        return this.baseMapper.getListByUserIdAll(userId);
    }

    @Override
    public List<UserRelationEntity> getListByObjectId(String objectId) {
        return this.baseMapper.getListByObjectId(objectId);
    }

    @Override
    public List<UserRelationEntity> getListByObjectType(String objectType) {
        return this.baseMapper.getListByObjectType(objectType);
    }

    @Override
    public List<UserRelationEntity> getListByObjectId(String objectId, String objectType) {
        return this.baseMapper.getListByObjectId(objectId, objectType);
    }

    @Override
    public List<UserRelationEntity> getListByObjectIdAll(List<String> objectId) {
        return this.baseMapper.getListByObjectIdAll(objectId);
    }

    @Override
    public void deleteAllByObjId(String objId) {
        this.baseMapper.deleteAllByObjId(objId);
    }

    @Override
    public void deleteAllByUserId(List<String> userId) {
        this.baseMapper.deleteAllByUserId(userId);
    }


    @Override
    public UserRelationEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public void save(String objectId, List<UserRelationEntity> entitys) {
        this.baseMapper.save(objectId, entitys);
    }

    @Override
    public void save(List<UserRelationEntity> list) {
        this.baseMapper.save(list);
    }

    @Override
    @DSTransactional
    public void delete(UserRelationForm form) {
        List<String> userIds = form.getUserIds();
        String type = form.getObjectType();
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        QueryWrapper<UserRelationEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserRelationEntity::getObjectId, form.getObjectId());
        queryWrapper.lambda().eq(UserRelationEntity::getObjectType, type);
        queryWrapper.lambda().in(UserRelationEntity::getUserId, userIds);
        List<UserRelationEntity> list = this.list(queryWrapper);
        if (CollUtil.isEmpty(list)) {
            return;
        }

        if (PermissionConst.POSITION.equals(type)) {
            PositionEntity info = positionMapper.getInfo(form.getObjectId());
            //如果责任人被移除则清空责任人
            if (StringUtil.isNotEmpty(info.getDutyUser()) && userIds.contains(info.getDutyUser())) {
                info.setDutyUser(null);
                positionMapper.updateById(info);
            }
            //岗位时顺便移除组织关系
            for (String userId : userIds) {
                queryWrapper = new QueryWrapper<>();
                queryWrapper.lambda().eq(UserRelationEntity::getObjectId, info.getOrganizeId());
                queryWrapper.lambda().eq(UserRelationEntity::getObjectType, PermissionConst.ORGANIZE);
                queryWrapper.lambda().in(UserRelationEntity::getUserId, userId);
                List<UserRelationEntity> orgList = this.list(queryWrapper);
                if (CollUtil.isNotEmpty(orgList)) {
                    list.add(orgList.get(0));
                }
            }
        }
        for (UserRelationEntity item : list) {
            this.removeById(item);
        }

        if (PermissionConst.POSITION.equals(type)) {
            userUtil.delCurUser(MsgCode.PS010.get(), form.getUserIds());
        }
    }

    @Override
    public List<UserRelationEntity> getRelationByUserIds(List<String> userIds) {
        return this.baseMapper.getRelationByUserIds(userIds);
    }

    @Override
    public List<UserRelationEntity> getListByObjectType(String userId, String objectType) {
        return this.baseMapper.getListByObjectType(userId, objectType);
    }

    @Override
    public List<UserRelationEntity> getAllOrgRelationByUserId(String userId) {
        return this.getListByObjectType(userId, PermissionConst.ORGANIZE);
    }

    @Override
    public List<PosistionCurrentModel> getObjectVoList() {
        UserInfo user = UserProvider.getUser();
        String userId = user.getUserId();
        String majorPosId = user.getPositionId();
        // 岗位遵循一对多关系
        List<String> ids = new ArrayList<>();
        this.getListByObjectType(userId, PermissionConst.POSITION).forEach(r -> ids.add(r.getObjectId()));
        List<PositionEntity> positionList = positionMapper.getListByIds(ids);
        if (!positionList.isEmpty()) {
            List<PosistionCurrentModel> voList = new ArrayList<>();
            for (PositionEntity p : positionList) {
                PosistionCurrentModel model = new PosistionCurrentModel();
                OrganizeEntity orgInfo = organizeMapper.getInfo(p.getOrganizeId());
                model.setId(p.getId());
                model.setPositionId(p.getId());
                model.setFullName(p.getFullName());
                model.setOrganizeId(orgInfo.getId());
                model.setOrgTreeName(orgInfo.getOrgNameTree());

                //上级岗位
                if (StringUtil.isNotEmpty(p.getParentId())) {
                    PositionEntity pPosition = positionMapper.getInfo(p.getParentId());
                    if (pPosition != null) {
                        model.setParentId(pPosition.getId());
                        model.setParentName(pPosition.getFullName());
                        UserEntity dutyUser = userMapper.getInfo(pPosition.getDutyUser());
                        if (dutyUser != null) {
                            model.setManagerId(dutyUser.getId());
                            model.setManagerName(dutyUser.getRealName());
                        }
                    }
                } else {
                    //没有上级岗位的时候--岗位为空，上级责任人去组织上级责任岗位的责任人
                    OrganizeEntity pOrgInfo = organizeMapper.getInfo(orgInfo.getParentId());
                    if (pOrgInfo != null) {
                        PositionEntity dutyPos = positionMapper.getInfo(pOrgInfo.getDutyPosition());
                        if (dutyPos != null) {
                            UserEntity dutyUser = userMapper.getInfo(dutyPos.getDutyUser());
                            if (dutyUser != null) {
                                model.setManagerId(dutyUser.getId());
                                model.setManagerName(dutyUser.getRealName());
                            }
                        }
                    }
                }
                model.setManagerId(p.getId());
                model.setIsDefault(p.getId().equals(majorPosId));
                voList.add(model);
            }
            return voList;
        }
        return Collections.emptyList();
    }

    @Override
    public Boolean existByObj(String objectType, String objectId) {
        return this.baseMapper.existByObj(objectType, objectId);
    }

    @Override
    public Boolean existByObj(String objectType, List<String> objectId) {
        return this.baseMapper.existByObj(objectType, objectId);
    }

    @Override
    public List<UserRelationEntity> getListByRoleId(String roleId) {
        List<UserRelationEntity> list = new ArrayList<>();
        roleRelationMapper.getListByRoleId(roleId, PermissionConst.ORGANIZE).forEach(o -> {
            QueryWrapper<UserRelationEntity> query = new QueryWrapper<>();
            query.lambda()
                    .eq(UserRelationEntity::getObjectType, PermissionConst.ORGANIZE)
                    .eq(UserRelationEntity::getObjectId, o.getObjectId());
            list.addAll(this.baseMapper.selectList(query));
        });
        return list;
    }

    @Override
    public List<UserRelationEntity> getListByUserId(String userId, String objectType) {
        return this.baseMapper.getListByUserId(userId, objectType);
    }

    @Override
    public List<UserRelationEntity> getListByOrgId(List<String> orgIdList) {
        return this.baseMapper.getListByOrgId(orgIdList);
    }

    @Override
    public List<UserEntity> getUserProgeny(List<String> idList, String enableMark) {
        return userUtil.getUserProgeny(idList, enableMark);
    }

    @Override
    public List<UserEntity> getUserAndSub(List<String> idList, String enableMark) {
        return userUtil.getUserAndSub(idList);
    }

    @Override
    public void updateOrgToNew(List<String> positionIds, String oldOrgId, String newOrgId) {
        List<UserRelationEntity> userPos = this.baseMapper.getListByObjectIdAll(positionIds);
        Map<String, List<UserRelationEntity>> userPosMap = userPos.stream().collect(Collectors.groupingBy(UserRelationEntity::getUserId));
        List<UserRelationEntity> userOrg = this.baseMapper.getListByObjectId(oldOrgId, PermissionConst.ORGANIZE);
        Map<String, List<UserRelationEntity>> userOrgMap = userOrg.stream().collect(Collectors.groupingBy(UserRelationEntity::getUserId));



        for (Map.Entry<String, List<UserRelationEntity>> entry : userPosMap.entrySet()) {
            String userId = entry.getKey();
            List<UserRelationEntity> upos = userPosMap.get(userId);
            List<UserRelationEntity> uorg = userOrgMap.get(userId);
            List<UserRelationEntity> updateList = new ArrayList<>();
            List<UserRelationEntity> insertList = new ArrayList<>();
            if (userOrgMap.get(userId) != null) {
                if (uorg.size() >= upos.size()) {
                    updateList.addAll(uorg.subList(0, upos.size()));
                } else {
                    updateList.addAll(uorg);
                    int n = upos.size() - uorg.size();
                    for (int i = 0; i < n; i++) {
                        UserRelationEntity urel = new UserRelationEntity();
                        urel.setId(RandomUtil.uuId());
                        urel.setUserId(userId);
                        urel.setObjectId(newOrgId);
                        urel.setObjectType(PermissionConst.ORGANIZE);
                        insertList.add(urel);
                    }
                }
            } else {
                UserRelationEntity urel = new UserRelationEntity();
                urel.setId(RandomUtil.uuId());
                urel.setUserId(userId);
                urel.setObjectId(newOrgId);
                urel.setObjectType(PermissionConst.ORGANIZE);
                insertList.add(urel);
            }
            for (UserRelationEntity item : updateList) {
                item.setObjectId(newOrgId);
                this.baseMapper.updateById(item);
            }
            for (UserRelationEntity item : insertList) {
                this.baseMapper.insert(item);
            }
        }
    }

    @Override
    public void removeOrgRelation(List<UserRelationEntity> userRelationEntities, String userId) {
        this.baseMapper.deleteByIds(userRelationEntities);
        List<String> collect = userRelationEntities.stream()
                .map(UserRelationEntity::getObjectId).collect(Collectors.toList());

        List<PositionEntity> listByOrganizeId = positionMapper.getListByOrganizeId(collect, false);
        this.baseMapper.deleteByPosIdAndUserId(listByOrganizeId.stream()
                .map(PositionEntity::getId).collect(Collectors.toList()), userId);

    }
}
