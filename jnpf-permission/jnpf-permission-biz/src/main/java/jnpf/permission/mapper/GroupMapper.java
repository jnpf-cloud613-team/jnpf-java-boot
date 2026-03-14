package jnpf.permission.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import jnpf.base.mapper.SuperMapper;
import jnpf.permission.entity.GroupEntity;
import jnpf.permission.model.usergroup.GroupPagination;
import jnpf.util.DateUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 分组管理Mapper
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/3/10 17:56
 */
public interface GroupMapper extends SuperMapper<GroupEntity> {

    default List<GroupEntity> getList(GroupPagination pagination) {
        QueryWrapper<GroupEntity> queryWrapper = new QueryWrapper<>();
        // 定义变量判断是否需要使用修改时间倒序
        boolean flag = false;
        // 判断关键字
        String keyword = pagination.getKeyword();
        if (StringUtil.isNotEmpty(keyword)) {
            flag = true;
            queryWrapper.lambda().and(
                    t -> t.like(GroupEntity::getFullName, keyword)
                            .or().like(GroupEntity::getEnCode, keyword)
                            .or().like(GroupEntity::getDescription, keyword)
            );
        }
        if (pagination.getEnabledMark() != null) {
            queryWrapper.lambda().eq(GroupEntity::getEnabledMark, pagination.getEnabledMark());
        }
        // 获取列表
        queryWrapper.lambda().orderByAsc(GroupEntity::getSortCode).orderByAsc(GroupEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(GroupEntity::getLastModifyTime);
        }
        //不分页
        if (Objects.equals(pagination.getDataType(), 1)) {
            return selectList(queryWrapper);
        }
        //有分页
        Page<GroupEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<GroupEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), iPage.getTotal());
    }

    default List<GroupEntity> list() {
        QueryWrapper<GroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(GroupEntity::getEnabledMark, 1);
        queryWrapper.lambda().orderByAsc(GroupEntity::getSortCode).orderByAsc(GroupEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default Map<String, Object> getGroupMap() {
        QueryWrapper<GroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(GroupEntity::getId, GroupEntity::getFullName);
        return this.selectList(queryWrapper).stream().collect(Collectors.toMap(GroupEntity::getId, GroupEntity::getFullName));
    }

    default Map<String, Object> getGroupEncodeMap(boolean enabledMark) {
        QueryWrapper<GroupEntity> queryWrapper = new QueryWrapper<>();
        if (enabledMark) {
            queryWrapper.lambda().eq(GroupEntity::getEnabledMark, 1);
        }
        queryWrapper.lambda().select(GroupEntity::getId, GroupEntity::getFullName, GroupEntity::getEnCode);
        return this.selectList(queryWrapper).stream().collect(Collectors.toMap(group -> group.getFullName() + "/" + group.getEnCode(), GroupEntity::getId));
    }

    default GroupEntity getInfo(String id) {
        QueryWrapper<GroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(GroupEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default GroupEntity getInfo(String fullName, String enCode) {
        QueryWrapper<GroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(GroupEntity::getFullName, fullName);
        queryWrapper.lambda().eq(GroupEntity::getEnCode, enCode);
        return this.selectOne(queryWrapper);
    }

    default void crete(GroupEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        entity.setCreatorTime(DateUtil.getNowDate());
        entity.setEnabledMark(1);
        this.insert(entity);
    }

    default Boolean update(String id, GroupEntity entity) {
        entity.setId(id);
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        entity.setLastModifyTime(DateUtil.getNowDate());
        entity.setEnabledMark(1);
        int i = this.updateById(entity);
        return i > 0;
    }

    default void delete(GroupEntity entity) {
        this.deleteById(entity.getId());
    }

    default Boolean isExistByFullName(String fullName, String id) {
        QueryWrapper<GroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(GroupEntity::getFullName, fullName);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(GroupEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default Boolean isExistByEnCode(String enCode, String id) {
        if (StringUtil.isEmpty(enCode)) return false;
        QueryWrapper<GroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(GroupEntity::getEnCode, enCode);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(GroupEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default List<GroupEntity> getListByIds(List<String> idList) {
        return this.getListByIds(idList, true);
    }

    default List<GroupEntity> getListByIds(List<String> idList, boolean filterEnabledMark) {
        if (CollUtil.isEmpty(idList)) return Collections.emptyList();
        QueryWrapper<GroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(GroupEntity::getId, idList);
        if (idList.size() > 1000) {
            List<List<String>> lists = Lists.partition(idList, 1000);
            queryWrapper.lambda().and(t -> {
                for (List<String> list : lists) {
                    t.in(GroupEntity::getId, list).or();
                }
            });
        } else {
            queryWrapper.lambda().in(GroupEntity::getId, idList);
        }
        if (filterEnabledMark) {
            queryWrapper.lambda().eq(GroupEntity::getEnabledMark, 1);
        }
        return this.selectList(queryWrapper);
    }
}
