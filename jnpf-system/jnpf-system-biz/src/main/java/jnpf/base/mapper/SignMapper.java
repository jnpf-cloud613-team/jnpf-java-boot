package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.permission.entity.SignEntity;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;

import java.util.List;


/**
 * 个人签名
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 * @date 2022年9月2日 上午9:18
 */
public interface SignMapper extends SuperMapper<SignEntity> {

    default List<SignEntity> getList() {
        QueryWrapper<SignEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SignEntity::getCreatorUserId, UserProvider.getUser().getUserId())
                .orderByDesc(SignEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default boolean create(SignEntity entity) {
        QueryWrapper<SignEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SignEntity::getIsDefault, 1)
                .eq(SignEntity::getCreatorUserId, UserProvider.getUser().getUserId());
        SignEntity signEntity = this.selectOne(queryWrapper);
        if (entity.getIsDefault() == 0) {
            if (signEntity == null) {
                entity.setIsDefault(1);
            }
        } else {
            if (signEntity != null) {
                signEntity.setIsDefault(0);
                this.updateById(signEntity);
            }
        }
        entity.setId(RandomUtil.uuId());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        return this.insertOrUpdate(entity);
    }

    default boolean updateDefault(String id) {
        QueryWrapper<SignEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SignEntity::getIsDefault, 1).eq(SignEntity::getCreatorUserId, UserProvider.getUser().getUserId());
        SignEntity signEntity = this.selectOne(queryWrapper);
        if (signEntity != null) {
            signEntity.setIsDefault(0);
            this.updateById(signEntity);
        }
        SignEntity entity = this.selectById(id);
        if (entity != null) {
            entity.setIsDefault(1);
            return SqlHelper.retBool(this.updateById(entity));
        }
        return false;
    }

    default SignEntity getDefaultByUserId(String id) {
        QueryWrapper<SignEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SignEntity::getIsDefault, 1).eq(SignEntity::getCreatorUserId, id);
        return this.selectOne(queryWrapper);
    }

}
