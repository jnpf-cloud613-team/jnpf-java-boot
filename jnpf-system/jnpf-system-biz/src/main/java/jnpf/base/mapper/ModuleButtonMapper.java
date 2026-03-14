package jnpf.base.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import jnpf.base.Pagination;
import jnpf.base.entity.ModuleButtonEntity;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 按钮权限
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface ModuleButtonMapper extends SuperMapper<ModuleButtonEntity> {

    default List<ModuleButtonEntity> getList() {
        QueryWrapper<ModuleButtonEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleButtonEntity::getEnabledMark, 1);
        // 排序
        queryWrapper.lambda().orderByAsc(ModuleButtonEntity::getSortCode)
                .orderByDesc(ModuleButtonEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleButtonEntity> getEnabledMarkList(String enabledMark) {
        QueryWrapper<ModuleButtonEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleButtonEntity::getEnabledMark, enabledMark);
        // 排序
        queryWrapper.lambda().orderByAsc(ModuleButtonEntity::getSortCode)
                .orderByDesc(ModuleButtonEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleButtonEntity> getListByModuleIds(String moduleId) {
        QueryWrapper<ModuleButtonEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleButtonEntity::getModuleId, moduleId);
        // 排序
        queryWrapper.lambda().orderByAsc(ModuleButtonEntity::getSortCode)
                .orderByDesc(ModuleButtonEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleButtonEntity> getListByModuleIds(String moduleId, Pagination pagination) {
        QueryWrapper<ModuleButtonEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleButtonEntity::getModuleId, moduleId);
        //关键字查询
        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            queryWrapper.lambda().and(
                    t -> t.like(ModuleButtonEntity::getFullName, pagination.getKeyword())
                            .or().like(ModuleButtonEntity::getEnCode, pagination.getKeyword())
            );
        }
        // 排序
        queryWrapper.lambda().orderByAsc(ModuleButtonEntity::getSortCode)
                .orderByDesc(ModuleButtonEntity::getCreatorTime);

        Page<ModuleButtonEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<ModuleButtonEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), iPage.getTotal());
    }

    default ModuleButtonEntity getInfo(String id) {
        QueryWrapper<ModuleButtonEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleButtonEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default ModuleButtonEntity getInfo(String id, String moduleId) {
        QueryWrapper<ModuleButtonEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleButtonEntity::getId, id);
        queryWrapper.lambda().eq(ModuleButtonEntity::getModuleId, moduleId);
        return this.selectOne(queryWrapper);
    }

    default boolean isExistByFullName(String moduleId, String fullName, String id) {
        QueryWrapper<ModuleButtonEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleButtonEntity::getFullName, fullName).eq(ModuleButtonEntity::getModuleId, moduleId);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(ModuleButtonEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByEnCode(String moduleId, String enCode, String id) {
        QueryWrapper<ModuleButtonEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleButtonEntity::getEnCode, enCode);
        if (moduleId != null) {
            queryWrapper.lambda().eq(ModuleButtonEntity::getModuleId, moduleId);
        }
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(ModuleButtonEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default void create(ModuleButtonEntity entity) {
        entity.setId(RandomUtil.uuId());
        this.insert(entity);
    }

    default void deleteByModuleId(String moduleId) {
        QueryWrapper<ModuleButtonEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ModuleButtonEntity::getModuleId, moduleId);
        this.deleteByIds(selectList(wrapper));
    }

    default List<ModuleButtonEntity> getListByModuleIds(List<String> ids, Integer type) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<ModuleButtonEntity> queryWrapper = new QueryWrapper<>();
        List<List<String>> lists = Lists.partition(ids, 1000);
        for (List<String> list : lists) {
            queryWrapper.lambda().and(t ->
                t.in(ModuleButtonEntity::getModuleId, list).or()
            );
        }
        if (type == 1) {
            queryWrapper.lambda().eq(ModuleButtonEntity::getEnabledMark, 1);
        }
        queryWrapper.lambda().orderByAsc(ModuleButtonEntity::getSortCode).orderByDesc(ModuleButtonEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleButtonEntity> getListByIds(List<String> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<ModuleButtonEntity> queryWrapper = new QueryWrapper<>();
        List<List<String>> lists = Lists.partition(ids, 1000);
        for (List<String> list : lists) {
            queryWrapper.lambda().or().in(ModuleButtonEntity::getId, list);
        }
        queryWrapper.lambda().eq(ModuleButtonEntity::getEnabledMark, 1);
        return this.selectList(queryWrapper);
    }

}
