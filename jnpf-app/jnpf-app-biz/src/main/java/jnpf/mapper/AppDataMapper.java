package jnpf.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.UserInfo;
import jnpf.base.mapper.SuperMapper;
import jnpf.entity.AppDataEntity;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;

import java.util.Date;
import java.util.List;

/**
 * app常用数据
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021-07-08
 */
public interface AppDataMapper extends SuperMapper<AppDataEntity> {


    default List<AppDataEntity> getList() {
        QueryWrapper<AppDataEntity> queryWrapper = new QueryWrapper<>();
        return this.selectList(queryWrapper);
    }

    default AppDataEntity getInfo(String objectId) {
        UserInfo userInfo = UserProvider.getUser();
        QueryWrapper<AppDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AppDataEntity::getObjectId, objectId).eq(AppDataEntity::getCreatorUserId, userInfo.getUserId());
        return this.selectOne(queryWrapper);
    }

    default boolean isExistByObjectId(String objectId, String systemId) {
        UserInfo userInfo = UserProvider.getUser();
        QueryWrapper<AppDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AppDataEntity::getObjectId, objectId)
                .eq(AppDataEntity::getCreatorUserId, userInfo.getUserId())
                .eq(AppDataEntity::getSystemId, systemId);
        return this.selectCount(queryWrapper) > 0;
    }

    default void create(AppDataEntity entity) {
        UserInfo userInfo = UserProvider.getUser();
        entity.setId(RandomUtil.uuId());
        entity.setCreatorUserId(userInfo.getUserId());
        entity.setCreatorTime(new Date());
        entity.setEnabledMark(1);
        entity.setSystemId(userInfo.getAppSystemId());
        this.insert(entity);
    }

    default void delete(AppDataEntity entity) {
        this.deleteById(entity.getId());
    }


}
