package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.UserInfo;
import jnpf.base.entity.ModuleDataEntity;
import jnpf.constant.JnpfConst;
import jnpf.consts.DeviceType;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;

import java.util.Date;
import java.util.List;


/**
 * 单据规则
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface ModuleDataMapper extends SuperMapper<ModuleDataEntity> {

    default List<ModuleDataEntity> getList(String category, List<String> moduleIds) {
        UserInfo userInfo = UserProvider.getUser();
        QueryWrapper<ModuleDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleDataEntity::getCreatorUserId, userInfo.getUserId());
        queryWrapper.lambda().eq(ModuleDataEntity::getModuleType, category);
        queryWrapper.lambda().in(ModuleDataEntity::getModuleId, moduleIds);
        return this.selectList(queryWrapper);
    }

    default void create(String moduleId) {
        UserInfo userInfo = UserProvider.getUser();
        boolean pc = DeviceType.PC.getDevice().equals(userInfo.getLoginDevice());
        ModuleDataEntity entity = new ModuleDataEntity();
        entity.setId(RandomUtil.uuId());
        entity.setCreatorUserId(userInfo.getUserId());
        entity.setCreatorTime(new Date());
        entity.setEnabledMark(1);
        entity.setModuleId(moduleId);
        entity.setSystemId(pc ? userInfo.getSystemId() : userInfo.getAppSystemId());
        entity.setModuleType(pc ? JnpfConst.WEB : JnpfConst.APP);
        this.insert(entity);
    }

    default ModuleDataEntity getInfo(String objectId) {
        UserInfo userInfo = UserProvider.getUser();
        boolean pc = DeviceType.PC.getDevice().equals(userInfo.getLoginDevice());
        QueryWrapper<ModuleDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleDataEntity::getModuleId, objectId).eq(ModuleDataEntity::getCreatorUserId, userInfo.getUserId());
        queryWrapper.lambda().eq(ModuleDataEntity::getModuleType, pc ? JnpfConst.WEB : JnpfConst.APP);
        return this.selectOne(queryWrapper);
    }

    default boolean isExistByObjectId(String moduleId) {
        UserInfo userInfo = UserProvider.getUser();
        boolean pc = DeviceType.PC.getDevice().equals(userInfo.getLoginDevice());
        QueryWrapper<ModuleDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleDataEntity::getModuleId, moduleId)
                .eq(ModuleDataEntity::getModuleType, pc ? JnpfConst.WEB : JnpfConst.APP)
                .eq(ModuleDataEntity::getCreatorUserId, userInfo.getUserId());
        return this.selectCount(queryWrapper) > 0;
    }

    default void deleteByModuleId(String moduleId) {
        QueryWrapper<ModuleDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleDataEntity::getModuleId, moduleId);
        this.deleteByIds(selectList(queryWrapper));
    }
}
