package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import jnpf.base.Pagination;
import jnpf.base.entity.ModuleFormEntity;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;

import java.util.ArrayList;
import java.util.List;


/**
 * 表单权限
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface ModuleFormMapper extends SuperMapper<ModuleFormEntity> {

    default List<ModuleFormEntity> getList() {
        QueryWrapper<ModuleFormEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleFormEntity::getEnabledMark, 1);
        // 排序
        queryWrapper.lambda().orderByAsc(ModuleFormEntity::getSortCode)
                .orderByDesc(ModuleFormEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleFormEntity> getEnabledMarkList(String enabledMark) {
        QueryWrapper<ModuleFormEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleFormEntity::getEnabledMark, enabledMark);
        // 排序
        queryWrapper.lambda().orderByAsc(ModuleFormEntity::getSortCode)
                .orderByDesc(ModuleFormEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleFormEntity> getList(String moduleId, Pagination pagination) {
        QueryWrapper<ModuleFormEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleFormEntity::getModuleId, moduleId);
        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            queryWrapper.lambda().and(
                    t -> t.like(ModuleFormEntity::getEnCode, pagination.getKeyword()).or().like(ModuleFormEntity::getFullName, pagination.getKeyword())
            );
        }
        // 排序
        queryWrapper.lambda().orderByAsc(ModuleFormEntity::getSortCode)
                .orderByDesc(ModuleFormEntity::getCreatorTime);

        Page<ModuleFormEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<ModuleFormEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), iPage.getTotal());
    }

    default List<ModuleFormEntity> getList(String moduleId) {
        QueryWrapper<ModuleFormEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleFormEntity::getModuleId, moduleId);
        // 排序
        queryWrapper.lambda().orderByAsc(ModuleFormEntity::getSortCode)
                .orderByDesc(ModuleFormEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default ModuleFormEntity getInfo(String id) {
        QueryWrapper<ModuleFormEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleFormEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default ModuleFormEntity getInfo(String id, String moduleId) {
        QueryWrapper<ModuleFormEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleFormEntity::getId, id);
        queryWrapper.lambda().eq(ModuleFormEntity::getModuleId, moduleId);
        return this.selectOne(queryWrapper);
    }

    default boolean isExistByFullName(String moduleId, String fullName, String id) {
        QueryWrapper<ModuleFormEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleFormEntity::getFullName, fullName).eq(ModuleFormEntity::getModuleId, moduleId);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(ModuleFormEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByEnCode(String moduleId, String enCode, String id) {
        QueryWrapper<ModuleFormEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleFormEntity::getEnCode, enCode).eq(ModuleFormEntity::getModuleId, moduleId);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(ModuleFormEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default void create(ModuleFormEntity entity) {
        entity.setId(RandomUtil.uuId());
        this.insert(entity);
    }

    default void create(List<ModuleFormEntity> entitys) {
        Long sortCode = RandomUtil.parses();
        String userId = UserProvider.getUser().getUserId();
        for (ModuleFormEntity entity : entitys) {
            entity.setId(RandomUtil.uuId());
            entity.setSortCode(sortCode++);
            entity.setEnabledMark("1".equals(String.valueOf(entity.getEnabledMark())) ? 0 : 1);
            entity.setCreatorUserId(userId);
            this.insert(entity);
        }
    }
    default void deleteByModuleId(String moduleId) {
        QueryWrapper<ModuleFormEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ModuleFormEntity::getModuleId, moduleId);
        this.deleteByIds(selectList(wrapper));
    }

    default List<ModuleFormEntity> getListByModuleId(List<String> ids, Integer type) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<ModuleFormEntity> queryWrapper = new QueryWrapper<>();
        List<List<String>> lists = Lists.partition(ids, 1000);
        for (List<String> list : lists) {
            queryWrapper.lambda().and(t ->
                t.in(ModuleFormEntity::getModuleId, list).or()
            );
        }
        if (type == 1) {
            queryWrapper.lambda().eq(ModuleFormEntity::getEnabledMark, 1);
        }
        queryWrapper.lambda().orderByAsc(ModuleFormEntity::getSortCode).orderByDesc(ModuleFormEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleFormEntity> getListByIds(List<String> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<ModuleFormEntity> queryWrapper = new QueryWrapper<>();
        List<List<String>> lists = Lists.partition(ids, 1000);
        for (List<String> list : lists) {
            queryWrapper.lambda().or().in(ModuleFormEntity::getId, list);
        }
        queryWrapper.lambda().eq(ModuleFormEntity::getEnabledMark, 1);
        return this.selectList(queryWrapper);
    }

}
