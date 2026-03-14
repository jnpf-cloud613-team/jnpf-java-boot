package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.base.UserInfo;
import jnpf.base.entity.ModuleUserNumEntity;
import jnpf.util.UserProvider;
import jnpf.util.context.RequestContext;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;

@Mapper
public interface ModuleUseNumMapper extends SuperMapper<ModuleUserNumEntity> {

    default Boolean insertOrUpdateUseNum(String moduleId) {
        String appCode = RequestContext.getAppCode();
        UserInfo user = UserProvider.getUser();
        LambdaQueryWrapper<ModuleUserNumEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModuleUserNumEntity::getModuleId, moduleId);
        queryWrapper.eq(ModuleUserNumEntity::getSystemCode, appCode);
        queryWrapper.eq(ModuleUserNumEntity::getUserId, user.getUserId());
        queryWrapper.eq(ModuleUserNumEntity::getSystemCode, appCode);
        ModuleUserNumEntity userNumEntity = this.selectOne(queryWrapper);
        if (userNumEntity == null) {
            userNumEntity = new ModuleUserNumEntity();
            userNumEntity.setSystemCode(appCode);
            userNumEntity.setUserId(user.getUserId());
            userNumEntity.setModuleId(moduleId);
            userNumEntity.setUseNum(1);
            userNumEntity.setLastModifyTime(new Date());
            return SqlHelper.retBool(this.insert(userNumEntity));
        }
        userNumEntity.setSystemCode(appCode);
        userNumEntity.setUseNum(userNumEntity.getUseNum() + 1);
        userNumEntity.setLastModifyTime(null);
        return SqlHelper.retBool(this.updateById(userNumEntity));
    }

    default void deleteUseNum(String moduleId) {
        UserInfo user = UserProvider.getUser();
        LambdaQueryWrapper<ModuleUserNumEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModuleUserNumEntity::getModuleId, moduleId);
        queryWrapper.eq(ModuleUserNumEntity::getUserId, user.getUserId());
        this.deleteByIds(selectList(queryWrapper));
    }

}
