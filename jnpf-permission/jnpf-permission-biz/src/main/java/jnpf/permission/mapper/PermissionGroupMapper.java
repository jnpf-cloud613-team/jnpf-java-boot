package jnpf.permission.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.mapper.SuperMapper;
import jnpf.permission.entity.PermissionGroupEntity;
import jnpf.permission.model.permissiongroup.PaginationPermissionGroup;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public interface PermissionGroupMapper extends SuperMapper<PermissionGroupEntity> {

    default List<PermissionGroupEntity> list(PaginationPermissionGroup pagination) {
        boolean flag = false;
        QueryWrapper<PermissionGroupEntity> queryWrapper = new QueryWrapper<>();
        String keyword = pagination.getKeyword();
        if (StringUtil.isNotEmpty(keyword)) {
            flag = true;
            queryWrapper.lambda().and(
                    t -> t.like(PermissionGroupEntity::getFullName, keyword)
                            .or().like(PermissionGroupEntity::getEnCode, keyword)
                            .or().like(PermissionGroupEntity::getDescription, keyword)
            );
        }
        if (pagination.getEnabledMark() != null) {
            queryWrapper.lambda().eq(PermissionGroupEntity::getEnabledMark, pagination.getEnabledMark());
        }
        queryWrapper.lambda().orderByAsc(PermissionGroupEntity::getSortCode).orderByDesc(PermissionGroupEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(PermissionGroupEntity::getLastModifyTime);
        }
        Page<PermissionGroupEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<PermissionGroupEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), iPage.getTotal());
    }

    default List<PermissionGroupEntity> list(boolean filterEnabledMark, List<String> ids) {
        if (ids != null && ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<PermissionGroupEntity> queryWrapper = new QueryWrapper<>();
        if (filterEnabledMark) {
            queryWrapper.lambda().eq(PermissionGroupEntity::getEnabledMark, 1);
        }
        if (ids != null && !ids.isEmpty()) {
            queryWrapper.lambda().in(PermissionGroupEntity::getId, ids);
        }
        return this.selectList(queryWrapper);
    }

    default PermissionGroupEntity info(String id) {
        return this.selectById(id);
    }

    default boolean create(PermissionGroupEntity entity) {
        entity.setId(RandomUtil.uuId());
        int i = this.insert(entity);
        return i > 0;
    }

    default boolean update(String id, PermissionGroupEntity entity) {
        entity.setId(id);
        int i = this.updateById(entity);
        return i > 0;
    }

    default boolean delete(PermissionGroupEntity entity) {
        int i = this.deleteById(entity);
        return i > 0;
    }

    default boolean isExistByFullName(String id, PermissionGroupEntity entity) {
        QueryWrapper<PermissionGroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PermissionGroupEntity::getFullName, entity.getFullName());
        if (StringUtil.isNotEmpty(id)) {
            queryWrapper.lambda().ne(PermissionGroupEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByEnCode(String enCode, String id) {
        QueryWrapper<PermissionGroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PermissionGroupEntity::getEnCode, enCode);
        if (StringUtil.isNotEmpty(id)) {
            queryWrapper.lambda().ne(PermissionGroupEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    /**
     * 通过ids获取权限组列表
     *
     * @param ids
     * @return
     */
    default List<PermissionGroupEntity> list(List<String> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<PermissionGroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(PermissionGroupEntity::getId, ids);
        return this.selectList(queryWrapper);
    }

    /**
     * 获取权限成员
     *
     * @param id 主键
     * @return
     */
    default PermissionGroupEntity permissionMember(String id) {
        QueryWrapper<PermissionGroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PermissionGroupEntity::getId, id);
        queryWrapper.lambda().select(PermissionGroupEntity::getId, PermissionGroupEntity::getPermissionMember);
        return this.selectOne(queryWrapper);
    }
}
