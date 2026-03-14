package jnpf.base.mapper;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.SignatureUserEntity;
import jnpf.util.RandomUtil;

import java.util.List;


/**
 * 电子签章
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 * @date 2022年9月2日 上午9:18
 */
public interface SignatureUserMapper extends SuperMapper<SignatureUserEntity> {

    default List<SignatureUserEntity> getList(List<String> signatureId, List<String> userId) {
        QueryWrapper<SignatureUserEntity> queryWrapper = new QueryWrapper<>();
        if (ObjectUtil.isNotEmpty(signatureId)) {
            queryWrapper.lambda().in(SignatureUserEntity::getSignatureId, signatureId);
        }
        if (ObjectUtil.isNotEmpty(userId)) {
            queryWrapper.lambda().in(SignatureUserEntity::getUserId, userId);
        }
        return this.selectList(queryWrapper);
    }

    default List<SignatureUserEntity> getList(String signatureId) {
        QueryWrapper<SignatureUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SignatureUserEntity::getSignatureId, signatureId);
        return this.selectList(queryWrapper);
    }

    default List<SignatureUserEntity> getListByUserId(String userId) {
        QueryWrapper<SignatureUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SignatureUserEntity::getUserId, userId);
        return this.selectList(queryWrapper);
    }

    default void create(SignatureUserEntity entity) {
        entity.setId(RandomUtil.uuId());
        this.insert(entity);
    }

    default void delete(String signatureId) {
        QueryWrapper<SignatureUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SignatureUserEntity::getSignatureId, signatureId);
        this.deleteByIds(this.selectList(queryWrapper));
    }
}
