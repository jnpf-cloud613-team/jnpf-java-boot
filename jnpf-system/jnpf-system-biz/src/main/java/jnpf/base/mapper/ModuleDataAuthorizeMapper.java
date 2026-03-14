package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.ModuleDataAuthorizeEntity;
import jnpf.util.RandomUtil;

import java.util.List;


/**
 * 数据权限配置
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface ModuleDataAuthorizeMapper extends SuperMapper<ModuleDataAuthorizeEntity> {

    default List<ModuleDataAuthorizeEntity> getList() {
        QueryWrapper<ModuleDataAuthorizeEntity> queryWrapper = new QueryWrapper<>();
        // 排序
        queryWrapper.lambda().orderByDesc(ModuleDataAuthorizeEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleDataAuthorizeEntity> getList(String moduleId) {
        QueryWrapper<ModuleDataAuthorizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleDataAuthorizeEntity::getModuleId, moduleId);
        // 排序
        queryWrapper.lambda().orderByDesc(ModuleDataAuthorizeEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default ModuleDataAuthorizeEntity getInfo(String id) {
        QueryWrapper<ModuleDataAuthorizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleDataAuthorizeEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default void create(ModuleDataAuthorizeEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setEnabledMark(1);
        entity.setSortCode(RandomUtil.parses());
        this.insert(entity);
    }

    default void deleteByModuleId(String moduleId) {
        QueryWrapper<ModuleDataAuthorizeEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ModuleDataAuthorizeEntity::getModuleId, moduleId);
        this.deleteByIds(selectList(wrapper));
    }

    default boolean isExistByEnCode(String moduleId, String enCode, String id) {
        QueryWrapper<ModuleDataAuthorizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleDataAuthorizeEntity::getModuleId, moduleId);
        queryWrapper.lambda().eq(ModuleDataAuthorizeEntity::getEnCode, enCode);
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByFullName(String moduleId, String fullName, String id) {
        QueryWrapper<ModuleDataAuthorizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleDataAuthorizeEntity::getModuleId, moduleId);
        queryWrapper.lambda().eq(ModuleDataAuthorizeEntity::getFullName, fullName);
        return this.selectCount(queryWrapper) > 0;
    }
}
