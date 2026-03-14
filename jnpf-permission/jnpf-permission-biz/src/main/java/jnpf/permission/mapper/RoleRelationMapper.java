package jnpf.permission.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.google.common.collect.Lists;
import jnpf.base.mapper.SuperMapper;
import jnpf.constant.PermissionConst;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.entity.RoleRelationEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.model.rolerelaiton.*;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色关系
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/2/26 18:13:07
 */
public interface RoleRelationMapper extends SuperMapper<RoleRelationEntity> {

    default List<RoleRelationEntity> getListPage(RoleListPage pagination) {
        String objectType = StringUtil.isNotEmpty(pagination.getPositionId()) ? PermissionConst.POSITION : PermissionConst.ORGANIZE;
        String objectId = StringUtil.isNotEmpty(pagination.getPositionId()) ? pagination.getPositionId() : pagination.getOrganizeId();
        QueryWrapper<RoleRelationEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(RoleRelationEntity::getObjectId, objectId);
        queryWrapper.lambda().eq(RoleRelationEntity::getObjectType, objectType);
        queryWrapper.lambda().orderByDesc(RoleRelationEntity::getCreatorTime).orderByDesc(RoleRelationEntity::getId);
        Page<RoleRelationEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<RoleRelationEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), iPage.getTotal());
    }

    default List<RoleRelationUserVo> getUserPage(RoleRelationPage pagination) {
        MPJLambdaWrapper<RoleRelationEntity> queryWrapper = JoinWrappers.lambda(RoleRelationEntity.class);
        queryWrapper.selectAs(UserEntity::getId, RoleRelationUserVo::getId);
        queryWrapper.selectAs(UserEntity::getAccount, RoleRelationUserVo::getAccount);
        queryWrapper.selectAs(UserEntity::getRealName, RoleRelationUserVo::getRealName);
        queryWrapper.selectAs(UserEntity::getGender, RoleRelationUserVo::getGender);
        queryWrapper.selectAs(UserEntity::getMobilePhone, RoleRelationUserVo::getMobilePhone);
        queryWrapper.selectAs(UserEntity::getEnabledMark, RoleRelationUserVo::getEnabledMark);
        queryWrapper.leftJoin(UserEntity.class, UserEntity::getId, RoleRelationEntity::getObjectId);
        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            queryWrapper.and(
                    t -> t.like(UserEntity::getAccount, pagination.getKeyword())
                            .or().like(UserEntity::getRealName, pagination.getKeyword())
                            .or().like(UserEntity::getMobilePhone, pagination.getKeyword())
            );
        }
        queryWrapper.eq(RoleRelationEntity::getRoleId, pagination.getRoleId());
        queryWrapper.eq(RoleRelationEntity::getObjectType, pagination.getType());

        queryWrapper.orderByDesc(RoleRelationEntity::getCreatorTime).orderByDesc(RoleRelationEntity::getId);
        Page<RoleRelationUserVo> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<RoleRelationUserVo> data = this.selectJoinPage(page, RoleRelationUserVo.class, queryWrapper);
        return pagination.setData(data.getRecords(), page.getTotal());
    }

    default List<RoleRelationOrgVo> getOrgPage(RoleRelationPage pagination) {
        MPJLambdaWrapper<RoleRelationEntity> queryWrapper = JoinWrappers.lambda(RoleRelationEntity.class);
        queryWrapper.leftJoin(OrganizeEntity.class, OrganizeEntity::getId, RoleRelationEntity::getObjectId);
        queryWrapper.selectAs(OrganizeEntity::getId, RoleRelationOrgVo::getId);
        queryWrapper.selectAs(OrganizeEntity::getFullName, RoleRelationOrgVo::getFullName);
        queryWrapper.selectAs(OrganizeEntity::getEnCode, RoleRelationOrgVo::getEnCode);
        queryWrapper.selectAs(OrganizeEntity::getOrgNameTree, RoleRelationOrgVo::getOrgNameTree);
        queryWrapper.selectAs(OrganizeEntity::getDescription, RoleRelationOrgVo::getDescription);

        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            queryWrapper.and(
                    t -> t.like(OrganizeEntity::getEnCode, pagination.getKeyword())
                            .or().like(OrganizeEntity::getFullName, pagination.getKeyword())
            );
        }
        queryWrapper.eq(RoleRelationEntity::getRoleId, pagination.getRoleId());
        queryWrapper.eq(RoleRelationEntity::getObjectType, pagination.getType());
        queryWrapper.isNotNull(OrganizeEntity::getId);

        queryWrapper.orderByDesc(RoleRelationEntity::getCreatorTime).orderByDesc(RoleRelationEntity::getId);
        Page<RoleRelationOrgVo> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<RoleRelationOrgVo> data = this.selectJoinPage(page, RoleRelationOrgVo.class, queryWrapper);
        return pagination.setData(data.getRecords(), page.getTotal());
    }

    default List<RoleRelationOrgVo> getPosPage(RoleRelationPage pagination) {
        MPJLambdaWrapper<RoleRelationEntity> queryWrapper = JoinWrappers.lambda(RoleRelationEntity.class);
        queryWrapper.leftJoin(PositionEntity.class, PositionEntity::getId, RoleRelationEntity::getObjectId);
        queryWrapper.selectAs(PositionEntity::getId, RoleRelationOrgVo::getId);
        queryWrapper.selectAs(PositionEntity::getFullName, RoleRelationOrgVo::getFullName);
        queryWrapper.selectAs(PositionEntity::getEnCode, RoleRelationOrgVo::getEnCode);
        queryWrapper.selectAs(PositionEntity::getDescription, RoleRelationOrgVo::getDescription);
        queryWrapper.selectAs(PositionEntity::getOrganizeId, RoleRelationOrgVo::getOrganizeId);

        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            queryWrapper.and(
                    t -> t.like(PositionEntity::getEnCode, pagination.getKeyword())
                            .or().like(PositionEntity::getFullName, pagination.getKeyword())
            );
        }
        queryWrapper.eq(RoleRelationEntity::getRoleId, pagination.getRoleId());
        queryWrapper.eq(RoleRelationEntity::getObjectType, pagination.getType());
        queryWrapper.isNotNull(PositionEntity::getId);

        queryWrapper.orderByDesc(RoleRelationEntity::getCreatorTime).orderByDesc(RoleRelationEntity::getId);
        Page<RoleRelationOrgVo> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<RoleRelationOrgVo> data = this.selectJoinPage(page, RoleRelationOrgVo.class, queryWrapper);
        return pagination.setData(data.getRecords(), page.getTotal());
    }

    default List<RoleRelationEntity> getListByObjectId(String objectId, String objectType) {
        QueryWrapper<RoleRelationEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(RoleRelationEntity::getObjectId, objectId);
        if (objectType != null) {
            queryWrapper.lambda().eq(RoleRelationEntity::getObjectType, objectType);
        }
        queryWrapper.lambda().orderByAsc(RoleRelationEntity::getSortCode).orderByDesc(RoleRelationEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<RoleRelationEntity> getListByObjectId(List<String> objectId, String objectType) {
        if (CollUtil.isEmpty(objectId)) return Collections.emptyList();
        QueryWrapper<RoleRelationEntity> queryWrapper = new QueryWrapper<>();
        if (objectType != null) {
            queryWrapper.lambda().eq(RoleRelationEntity::getObjectType, objectType);
        }
        List<List<String>> lists = Lists.partition(objectId, 1000);
        queryWrapper.lambda().and(t -> {
            for (List<String> ids : lists) {
                t.in(RoleRelationEntity::getObjectId, ids).or();
            }
        });

        queryWrapper.lambda().orderByAsc(RoleRelationEntity::getSortCode).orderByDesc(RoleRelationEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<RoleRelationEntity> getListByRoleId(String roleId, String objectType) {
        QueryWrapper<RoleRelationEntity> query = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(roleId)) {
            query.lambda().eq(RoleRelationEntity::getRoleId, roleId);
        }
        if (StringUtil.isNotEmpty(objectType)) {
            query.lambda().eq(RoleRelationEntity::getObjectType, objectType);
        }
        query.lambda().orderByAsc(RoleRelationEntity::getSortCode).orderByDesc(RoleRelationEntity::getCreatorTime);
        return this.selectList(query);
    }

    default List<RoleRelationEntity> getListByRoleId(List<String> roleId, String objectType) {
        if (CollUtil.isEmpty(roleId)) return Collections.emptyList();
        QueryWrapper<RoleRelationEntity> query = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(objectType)) {
            query.lambda().eq(RoleRelationEntity::getObjectType, objectType);
        }
        List<List<String>> lists = Lists.partition(roleId, 1000);
        query.lambda().and(t -> {
            for (List<String> ids : lists) {
                t.in(RoleRelationEntity::getRoleId, ids).or();
            }
        });
        query.lambda().orderByAsc(RoleRelationEntity::getSortCode).orderByDesc(RoleRelationEntity::getCreatorTime);
        return this.selectList(query);
    }

    default void delete(RoleRelationForm form) {
        QueryWrapper<RoleRelationEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(RoleRelationEntity::getRoleId, form.getRoleId());
        queryWrapper.lambda().eq(RoleRelationEntity::getObjectType, form.getType());
        queryWrapper.lambda().in(RoleRelationEntity::getObjectId, form.getIds());
        this.deleteByIds(selectList(queryWrapper));
    }

    default void objectAddRoles(AddRolesForm form) {
        String type = form.getType();
        List<String> roleIds = form.getIds();
        List<String> hasRRE = this.getListByObjectId(form.getObjectId(), type).stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
        for (String roleId : roleIds) {
            if (!hasRRE.contains(roleId)) {
                RoleRelationEntity entity = new RoleRelationEntity();
                entity.setId(RandomUtil.uuId());
                entity.setObjectId(form.getObjectId());
                entity.setRoleId(roleId);
                entity.setObjectType(type);
                this.insert(entity);
            }
        }
    }

    default void objectDeleteRoles(AddRolesForm form) {
        QueryWrapper<RoleRelationEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(RoleRelationEntity::getObjectId, form.getObjectId());
        queryWrapper.lambda().eq(RoleRelationEntity::getObjectType, form.getType());
        queryWrapper.lambda().in(RoleRelationEntity::getRoleId, form.getIds());
        this.deleteByIds(selectList(queryWrapper));
    }

    default List<RoleRelationEntity> getListByForm(RoleRelationForm form) {
        QueryWrapper<RoleRelationEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(RoleRelationEntity::getRoleId, form.getRoleId());
        queryWrapper.lambda().eq(RoleRelationEntity::getObjectType, form.getType());
        queryWrapper.lambda().in(RoleRelationEntity::getObjectId, form.getIds());
        return this.selectList(queryWrapper);
    }

    default void deleteAllByObjId(List<String> objId) {
        if (CollUtil.isEmpty(objId)) {
            return;
        }
        QueryWrapper<RoleRelationEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(RoleRelationEntity::getObjectId, objId);
        this.deleteByIds(selectList(queryWrapper));
    }
}
