package jnpf.permission.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.common.collect.Lists;
import jnpf.base.mapper.SuperMapper;
import jnpf.constant.PermissionConst;
import jnpf.permission.entity.UserRelationEntity;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * 用户关系
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
public interface UserRelationMapper extends SuperMapper<UserRelationEntity> {

    default List<UserRelationEntity> getListByUserId(String userId) {
        return getListByUserIdAll(Collections.singletonList(userId));
    }

    default List<UserRelationEntity> getListByUserIdAndObjType(String userId, String objectType) {
        QueryWrapper<UserRelationEntity> query = new QueryWrapper<>();
        query.lambda().in(UserRelationEntity::getUserId, userId);
        query.lambda().in(UserRelationEntity::getObjectType, objectType);
        query.lambda().orderByAsc(UserRelationEntity::getSortCode).orderByDesc(UserRelationEntity::getCreatorTime);
        return this.selectList(query);
    }

    default List<UserRelationEntity> getListByUserIdAll(List<String> userId) {
        if (!userId.isEmpty()) {
            QueryWrapper<UserRelationEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(UserRelationEntity::getUserId, userId);
            return this.selectList(queryWrapper);
        }
        return new ArrayList<>();
    }

    default List<UserRelationEntity> getListByObjectId(String objectId) {
        return getListByObjectId(objectId, null);
    }

    default List<UserRelationEntity> getListByObjectType(String objectType) {
        QueryWrapper<UserRelationEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserRelationEntity::getObjectType, objectType);
        return this.selectList(queryWrapper);
    }

    default List<UserRelationEntity> getListByObjectId(String objectId, String objectType) {
        QueryWrapper<UserRelationEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserRelationEntity::getObjectId, objectId);
        if (objectType != null) {
            queryWrapper.lambda().eq(UserRelationEntity::getObjectType, objectType);
        }
        queryWrapper.lambda().orderByAsc(UserRelationEntity::getSortCode).orderByDesc(UserRelationEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<UserRelationEntity> getListByObjectIdAll(List<String> objectId) {
        List<UserRelationEntity> list = new ArrayList<>();
        if (!objectId.isEmpty()) {
            QueryWrapper<UserRelationEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(UserRelationEntity::getObjectId, objectId);
            list = this.selectList(queryWrapper);
        }
        return list;
    }

    default void deleteAllByObjId(String objId) {
        QueryWrapper<UserRelationEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserRelationEntity::getObjectId, objId);
        this.deleteByIds(selectList(queryWrapper));
    }

    default void deleteAllByUserId(List<String> userId) {
        if (CollUtil.isEmpty(userId)) {
            return;
        }
        QueryWrapper<UserRelationEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(UserRelationEntity::getUserId, userId);
        this.deleteByIds(selectList(queryWrapper));
    }

    default UserRelationEntity getInfo(String id) {
        return this.selectById(id);
    }

    default void save(String objectId, List<UserRelationEntity> entitys) {
        List<UserRelationEntity> existList = this.getListByObjectId(objectId);
        List<UserRelationEntity> relationList = new ArrayList<>();
        for (int i = 0; i < entitys.size(); i++) {
            UserRelationEntity entity = entitys.get(i);
            entity.setId(RandomUtil.uuId());
            entity.setSortCode(Long.parseLong(i + ""));
            entity.setCreatorUserId(UserProvider.getUser().getUserId());
            if (existList.stream().filter(t -> t.getUserId().equals(entity.getUserId())).count() == 0) {
                relationList.add(entity);
            }
        }
        for (UserRelationEntity entity : relationList) {
            this.insert(entity);
        }
    }

    default void save(List<UserRelationEntity> list) {
        for (UserRelationEntity entity : list) {
            this.insert(entity);
        }
    }


    default List<UserRelationEntity> getRelationByUserIds(List<String> userIds) {
        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<UserRelationEntity> query = new QueryWrapper<>();
        query.lambda().in(UserRelationEntity::getUserId, userIds);
        return this.selectList(query);
    }

    default List<UserRelationEntity> getListByObjectType(String userId, String objectType) {
        QueryWrapper<UserRelationEntity> query = new QueryWrapper<>();
        query.lambda().eq(UserRelationEntity::getUserId, userId);
        if (StringUtil.isNotEmpty(objectType)) {
            query.lambda().eq(UserRelationEntity::getObjectType, objectType);
        }
        query.lambda().orderByAsc(UserRelationEntity::getSortCode).orderByDesc(UserRelationEntity::getCreatorTime);
        return this.selectList(query);
    }

    default List<UserRelationEntity> getAllOrgRelationByUserId(String userId) {
        return this.getListByObjectType(userId, PermissionConst.ORGANIZE);
    }

    default Boolean existByObj(String objectType, String objectId) {
        return existByObj(objectType, Arrays.asList(objectId));
    }

    default Boolean existByObj(String objectType, List<String> objectId) {
        if (CollUtil.isEmpty(objectId)) return false;
        QueryWrapper<UserRelationEntity> query = new QueryWrapper<>();
        query.lambda()
                .eq(UserRelationEntity::getObjectType, objectType)
                .in(UserRelationEntity::getObjectId, objectId);
        return this.selectCount(query) > 0;
    }

    default List<UserRelationEntity> getListByUserId(String userId, String objectType) {
        QueryWrapper<UserRelationEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserRelationEntity::getUserId, userId);
        queryWrapper.lambda().eq(UserRelationEntity::getObjectType, objectType);
        return this.selectList(queryWrapper);
    }

    default List<UserRelationEntity> getListByOrgId(List<String> orgIdList) {
        if (CollUtil.isNotEmpty(orgIdList)) {
            QueryWrapper<UserRelationEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(UserRelationEntity::getObjectType, PermissionConst.ORGANIZE);
            List<List<String>> lists = Lists.partition(orgIdList, 1000);
            queryWrapper.lambda().and(t -> {
                for (List<String> ids : lists) {
                    t.or().in(UserRelationEntity::getObjectId, ids);
                }
            });
            return this.selectList(queryWrapper);
        }
        return new ArrayList<>();
    }

    default void deleteByPosIdAndUserId(@NotNull List<String> collect, String userId){
        LambdaUpdateWrapper<UserRelationEntity> wrapper = new LambdaUpdateWrapper<>();
        if (CollUtil.isNotEmpty(collect)) {
            wrapper.in(UserRelationEntity::getObjectId, collect);
        }

        wrapper.eq(UserRelationEntity::getUserId, userId);
        wrapper.eq(UserRelationEntity::getObjectType, PermissionConst.POSITION);
        List<UserRelationEntity> userRelationEntities = this.selectList(wrapper);
        this.deleteByIds(userRelationEntities);
    }
}
