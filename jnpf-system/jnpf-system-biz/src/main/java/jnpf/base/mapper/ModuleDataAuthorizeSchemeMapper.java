package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import jnpf.base.Pagination;
import jnpf.base.entity.ModuleDataAuthorizeSchemeEntity;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;

import java.util.ArrayList;
import java.util.List;


/**
 * 数据权限方案
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface ModuleDataAuthorizeSchemeMapper extends SuperMapper<ModuleDataAuthorizeSchemeEntity> {

    default List<ModuleDataAuthorizeSchemeEntity> getList() {
        QueryWrapper<ModuleDataAuthorizeSchemeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleDataAuthorizeSchemeEntity::getEnabledMark, 1);
        // 排序
        queryWrapper.lambda().orderByDesc(ModuleDataAuthorizeSchemeEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleDataAuthorizeSchemeEntity> getEnabledMarkList(String enabledMark) {
        QueryWrapper<ModuleDataAuthorizeSchemeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleDataAuthorizeSchemeEntity::getEnabledMark, enabledMark);
        // 排序
        queryWrapper.lambda().orderByDesc(ModuleDataAuthorizeSchemeEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleDataAuthorizeSchemeEntity> getList(String moduleId) {
        QueryWrapper<ModuleDataAuthorizeSchemeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleDataAuthorizeSchemeEntity::getModuleId, moduleId);
        // 排序
        queryWrapper.lambda().orderByDesc(ModuleDataAuthorizeSchemeEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleDataAuthorizeSchemeEntity> getList(String moduleId, Pagination pagination) {
        QueryWrapper<ModuleDataAuthorizeSchemeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleDataAuthorizeSchemeEntity::getModuleId, moduleId);
        // 排序
        queryWrapper.lambda().orderByDesc(ModuleDataAuthorizeSchemeEntity::getCreatorTime);

        Page<ModuleDataAuthorizeSchemeEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<ModuleDataAuthorizeSchemeEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), iPage.getTotal());
    }

    default ModuleDataAuthorizeSchemeEntity getInfo(String id) {
        QueryWrapper<ModuleDataAuthorizeSchemeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleDataAuthorizeSchemeEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default void create(ModuleDataAuthorizeSchemeEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setEnabledMark(1);
        entity.setSortCode(RandomUtil.parses());
        this.insert(entity);
    }

    default void deleteByModuleId(String moduleId) {
        QueryWrapper<ModuleDataAuthorizeSchemeEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ModuleDataAuthorizeSchemeEntity::getModuleId, moduleId);
        this.deleteByIds(selectList(wrapper));
    }

    default Boolean isExistByFullName(String id, String fullName, String moduleId) {
        QueryWrapper<ModuleDataAuthorizeSchemeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleDataAuthorizeSchemeEntity::getModuleId, moduleId);
        queryWrapper.lambda().eq(ModuleDataAuthorizeSchemeEntity::getFullName, fullName);
        if (StringUtil.isNotEmpty(id)) {
            queryWrapper.lambda().ne(ModuleDataAuthorizeSchemeEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default Boolean isExistByEnCode(String id, String enCode, String moduleId) {
        QueryWrapper<ModuleDataAuthorizeSchemeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleDataAuthorizeSchemeEntity::getModuleId, moduleId);
        queryWrapper.lambda().eq(ModuleDataAuthorizeSchemeEntity::getEnCode, enCode);
        if (StringUtil.isNotEmpty(id)) {
            queryWrapper.lambda().ne(ModuleDataAuthorizeSchemeEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default Boolean isExistAllData(String moduleId) {
        QueryWrapper<ModuleDataAuthorizeSchemeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleDataAuthorizeSchemeEntity::getModuleId, moduleId);
        queryWrapper.lambda().eq(ModuleDataAuthorizeSchemeEntity::getAllData, 1);
        return this.selectCount(queryWrapper) > 0;
    }

    default List<ModuleDataAuthorizeSchemeEntity> getListByModuleId(List<String> ids, Integer type) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<ModuleDataAuthorizeSchemeEntity> queryWrapper = new QueryWrapper<>();
        List<List<String>> lists = Lists.partition(ids, 1000);
        for (List<String> list : lists) {
            queryWrapper.lambda().and(t ->
                t.in(ModuleDataAuthorizeSchemeEntity::getModuleId, list).or()
            );
        }
        if (type == 1) {
            queryWrapper.lambda().eq(ModuleDataAuthorizeSchemeEntity::getEnabledMark, 1);
        }

        queryWrapper.lambda().orderByAsc(ModuleDataAuthorizeSchemeEntity::getSortCode).orderByDesc(ModuleDataAuthorizeSchemeEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleDataAuthorizeSchemeEntity> getListByIds(List<String> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<ModuleDataAuthorizeSchemeEntity> queryWrapper = new QueryWrapper<>();
        List<List<String>> lists = Lists.partition(ids, 1000);
        for (List<String> list : lists) {
            queryWrapper.lambda().or().in(ModuleDataAuthorizeSchemeEntity::getId, list);
        }
        queryWrapper.lambda().eq(ModuleDataAuthorizeSchemeEntity::getEnabledMark, 1);
        return this.selectList(queryWrapper);
    }
}
