package jnpf.permission.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.google.common.collect.Lists;
import jnpf.base.Pagination;
import jnpf.base.mapper.SuperMapper;
import jnpf.permission.entity.RoleEntity;
import jnpf.permission.model.position.PosConModel;
import jnpf.permission.model.role.RolePagination;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import org.apache.ibatis.annotations.Param;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统角色
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
public interface RoleMapper extends SuperMapper<RoleEntity> {

    /**
     * 通过组织id获取用户信息
     *
     * @param orgIdList
     * @return
     */
    List<String> query(@Param("orgIdList") List<String> orgIdList, @Param("keyword") String keyword, @Param("globalMark") Integer globalMark, @Param("enabledMark") Integer enabledMark);

    /**
     * 通过组织id获取用户信息
     *
     * @param
     * @param orgIdList
     * @return
     */
    Long count(@Param("orgIdList") List<String> orgIdList, @Param("keyword") String keyword, @Param("globalMark") Integer globalMark, @Param("enabledMark") Integer enabledMark);


    default List<RoleEntity> getList(RolePagination pagination) {
        boolean flag = false;
        QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            flag = true;
            queryWrapper.lambda().and(
                    t -> t.like(RoleEntity::getFullName, pagination.getKeyword())
                            .or().like(RoleEntity::getEnCode, pagination.getKeyword())
            );
        }
        if (StringUtil.isNotEmpty(pagination.getType())) {
            queryWrapper.lambda().eq(RoleEntity::getType, pagination.getType());
        }
        if (pagination.getEnabledMark() != null) {
            queryWrapper.lambda().eq(RoleEntity::getEnabledMark, pagination.getEnabledMark());
        }

        //不分页
        if (Objects.equals(pagination.getDataType(), 1)) {
            //排序
            queryWrapper.lambda().orderByAsc(RoleEntity::getGlobalMark).orderByAsc(RoleEntity::getSortCode).orderByAsc(RoleEntity::getCreatorTime);
            if (flag) {
                queryWrapper.lambda().orderByDesc(RoleEntity::getLastModifyTime);
            }
            return this.selectList(queryWrapper);
        }
        //分页
        long count = this.selectCount(queryWrapper);
        //排序
        queryWrapper.lambda().orderByAsc(RoleEntity::getGlobalMark).orderByAsc(RoleEntity::getSortCode).orderByAsc(RoleEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(RoleEntity::getLastModifyTime);
        }
        Page<RoleEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize(), count, false);
        page.setOptimizeCountSql(false);
        IPage<RoleEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), page.getTotal());
    }

    default Boolean isExistByFullName(String fullName, String id, String type) {
        QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(RoleEntity::getFullName, fullName);
        queryWrapper.lambda().eq(RoleEntity::getType, type);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(RoleEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default Boolean isExistByEnCode(String enCode, String id) {
        if (StringUtil.isEmpty(enCode)) return false;
        QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(RoleEntity::getEnCode, enCode);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(RoleEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default void create(RoleEntity entity) {
        if (StringUtil.isEmpty(entity.getId())) {
            entity.setId(RandomUtil.uuId());
        }
        entity.setGlobalMark(2);
        entity.setEnabledMark(1);
        entity.setCreatorTime(new Date());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        this.insert(entity);
    }

    default Boolean update(String id, RoleEntity entity) {
        entity.setId(id);
        entity.setEnabledMark(1);
        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        return SqlHelper.retBool(this.updateById(entity));
    }

    default RoleEntity getInfo(String id) {
        return this.selectById(id);
    }

    default RoleEntity getByEnCode(String enCode) {
        if (StringUtil.isEmpty(enCode)) return null;
        QueryWrapper<RoleEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(RoleEntity::getEnCode, enCode);
        return this.selectOne(wrapper);
    }

    default List<RoleEntity> getList(boolean filterEnabledMark, String type, Integer isSystem) {
        QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
        if (filterEnabledMark) {
            queryWrapper.lambda().eq(RoleEntity::getEnabledMark, 1);
        }
        if (StringUtil.isNotEmpty(type)) {
            queryWrapper.lambda().eq(RoleEntity::getType, type);
        }
        if (Objects.nonNull(isSystem)) {
            if (Objects.equals(isSystem, 1)) {
                queryWrapper.lambda().eq(RoleEntity::getGlobalMark, 1);
            } else {
                queryWrapper.lambda().ne(RoleEntity::getGlobalMark, 1);
            }

        }
        queryWrapper.lambda().orderByAsc(RoleEntity::getSortCode).orderByAsc(RoleEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<RoleEntity> getListByIds(List<String> id, String keyword, boolean filterEnabledMark) {
        if (CollUtil.isEmpty(id)) return Collections.emptyList();
        List<RoleEntity> roleList = new ArrayList<>();
        if (!id.isEmpty()) {
            QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(RoleEntity::getId, id);
            if (filterEnabledMark) {
                queryWrapper.lambda().eq(RoleEntity::getEnabledMark, 1);
            }
            if (StringUtil.isNotEmpty(keyword)) {
                queryWrapper.lambda().and(
                        t -> t.like(RoleEntity::getFullName, keyword)
                                .or().like(RoleEntity::getEnCode, keyword)
                );
            }
            roleList = this.selectList(queryWrapper);
        }
        return roleList;
    }

    default List<RoleEntity> getListByIds(List<String> idList) {
        if (CollUtil.isEmpty(idList)) return Collections.emptyList();
        QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
        List<List<String>> lists = Lists.partition(idList, 1000);
        for (List<String> list : lists) {
            queryWrapper.lambda().in(RoleEntity::getId, list);
        }
        return this.selectList(queryWrapper);
    }

    default List<RoleEntity> getListByIds(Pagination pagination, List<String> idList) {
        if (CollUtil.isEmpty(idList)) return Collections.emptyList();
        QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
        List<List<String>> lists = Lists.partition(idList, 1000);
        for (List<String> list : lists) {
            queryWrapper.lambda().in(RoleEntity::getId, list);
        }
        if (StringUtil.isNotEmpty(pagination.getKeyword()) && StringUtil.isNotEmpty(pagination.getKeyword())) {
            queryWrapper.lambda().and(
                    t -> t.like(RoleEntity::getFullName, pagination.getKeyword())
                            .or().like(RoleEntity::getEnCode, pagination.getKeyword())
            );
        }
        Page<RoleEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<RoleEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), page.getTotal());
    }

    default Map<String, Object> getRoleMap() {
        QueryWrapper<RoleEntity> roleWrapper = new QueryWrapper<>();
        roleWrapper.lambda().select(RoleEntity::getFullName, RoleEntity::getId);
        List<RoleEntity> list = this.selectList(roleWrapper);
        return list.stream().collect(Collectors.toMap(RoleEntity::getId, RoleEntity::getFullName));
    }

    default Map<String, Object> getRoleNameAndIdMap() {
        return getRoleNameAndIdMap(false);
    }

    default Map<String, Object> getRoleNameAndIdMap(boolean enabledMark) {
        QueryWrapper<RoleEntity> roleWrapper = new QueryWrapper<>();
        if (enabledMark) {
            roleWrapper.lambda().eq(RoleEntity::getEnabledMark, 1);
        }
        List<RoleEntity> list = this.selectList(roleWrapper);
        Map<String, Object> roleNameMap = new HashMap<>();
        list.stream().forEach(role -> roleNameMap.put(role.getFullName() + "/" + role.getEnCode(), role.getId()));
        return roleNameMap;
    }

    default RoleEntity getInfoByFullName(String fullName) {
        QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(RoleEntity::getFullName, fullName);
        return this.selectOne(queryWrapper);
    }

    default List<RoleEntity> getList(List<String> idList, Pagination pagination, boolean filterEnabledMark) {
        if (!idList.isEmpty()) {
            QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(RoleEntity::getId, idList);
            if (filterEnabledMark) {
                queryWrapper.lambda().eq(RoleEntity::getEnabledMark, 1);
            }
            if (StringUtil.isNotEmpty(pagination.getKeyword())) {
                queryWrapper.lambda().and(
                        t -> t.like(RoleEntity::getFullName, pagination.getKeyword())
                                .or().like(RoleEntity::getEnCode, pagination.getKeyword())
                );
            }
            Page<RoleEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
            IPage<RoleEntity> iPage = this.selectPage(page, queryWrapper);
            return pagination.setData(iPage.getRecords(), iPage.getTotal());
        }
        return Collections.emptyList();
    }

    default void linkUpdate(String id, PosConModel posConModel) {
        //联动修改互斥对象
        List<String> muEList = new ArrayList<>();
        if (posConModel.getMutualExclusionFlag()) {
            muEList.addAll(posConModel.getMutualExclusion());
        }
        //muEList 互斥对象。除了这个列表外其他角色里不能包含该互斥
        QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().like(RoleEntity::getConditionJson, id);
        if (CollUtil.isNotEmpty(muEList)) {
            queryWrapper.lambda().or().in(RoleEntity::getId, muEList);
        }

        List<RoleEntity> list = this.selectList(queryWrapper);
        for (RoleEntity item : list) {
            if (muEList.contains(item.getId())) {
                //添加
                item.setIsCondition(1);
                PosConModel psModel = StringUtil.isEmpty(item.getConditionJson()) ? new PosConModel() : JsonUtil.getJsonToBean(item.getConditionJson(), PosConModel.class);
                List<Integer> constraintType = psModel.getConstraintType() == null ? new ArrayList<>() : psModel.getConstraintType();
                if (!constraintType.contains(0)) {
                    constraintType.add(0);
                    psModel.setConstraintType(constraintType);
                }
                List<String> mutualExclusion = psModel.getMutualExclusion() == null ? new ArrayList<>() : psModel.getMutualExclusion();
                if (!mutualExclusion.contains(id)) {
                    mutualExclusion.add(id);
                    psModel.setMutualExclusion(mutualExclusion);
                    item.setConditionJson(JsonUtil.getObjectToString(psModel));
                }
                this.update(item.getId(), item);
            } else {
                //移除
                if (Objects.equals(item.getIsCondition(), 1)) {
                    PosConModel psModel = JsonUtil.getJsonToBean(item.getConditionJson(), PosConModel.class);
                    psModel.init();
                    if (psModel.getMutualExclusionFlag()) {
                        List<String> mutualExclusion = psModel.getMutualExclusion();
                        if (mutualExclusion.contains(id)) {
                            mutualExclusion.remove(id);
                            if (mutualExclusion.isEmpty()) {
                                List<Integer> constraintType = psModel.getConstraintType();
                                constraintType.remove(Integer.valueOf(0));
                                psModel.setConstraintType(constraintType);
                                if (constraintType.isEmpty()) {
                                    item.setIsCondition(0);
                                }
                            }
                            item.setConditionJson(JsonUtil.getObjectToString(psModel));
                            this.update(item.getId(), item);
                        }
                    }
                }
            }
        }
    }
}
