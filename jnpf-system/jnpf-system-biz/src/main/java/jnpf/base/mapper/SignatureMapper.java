package jnpf.base.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jnpf.base.entity.SignatureEntity;
import jnpf.base.entity.SignatureUserEntity;
import jnpf.base.model.signature.SignatureListByIdsModel;
import jnpf.base.model.signature.SignatureSelectorListVO;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;

import java.util.ArrayList;
import java.util.List;


/**
 * 电子签章
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 * @date 2022年9月2日 上午9:18
 */
public interface SignatureMapper extends SuperMapper<SignatureEntity> {

    default List<SignatureSelectorListVO> getListByIds(SignatureListByIdsModel model) {
        if (CollUtil.isEmpty(model.getIds())) {
            return new ArrayList<>();
        }
        MPJLambdaWrapper<SignatureEntity> wrapper = new MPJLambdaWrapper<>(SignatureEntity.class)
                .select(SignatureEntity::getId, SignatureEntity::getEnCode, SignatureEntity::getFullName, SignatureEntity::getIcon)
                .leftJoin(SignatureUserEntity.class, SignatureUserEntity::getSignatureId, SignatureEntity::getId)
                .select(SignatureUserEntity::getSignatureId, SignatureUserEntity::getUserId)
                .selectCollection(SignatureUserEntity.class, SignatureSelectorListVO::getSignatureUserList, map -> map
                        .result(SignatureUserEntity::getSignatureId)
                        .result(SignatureUserEntity::getUserId));
        // ids
        wrapper.in(CollUtil.isNotEmpty(model.getIds()), SignatureEntity::getId, model.getIds());
        // 是否有权限
        wrapper.eq(StringUtil.isNotEmpty(UserProvider.getLoginUserId()), SignatureUserEntity::getUserId, UserProvider.getLoginUserId());
        wrapper.orderByAsc(SignatureEntity::getSortCode).orderByDesc(SignatureEntity::getCreatorTime);
        return this.selectJoinList(SignatureSelectorListVO.class, wrapper);
    }

    default SignatureEntity getInfoById(String id) {
        QueryWrapper<SignatureEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SignatureEntity::getId, id);
        queryWrapper.lambda().orderByAsc(SignatureEntity::getSortCode).orderByDesc(SignatureEntity::getCreatorTime);
        return this.selectOne(queryWrapper);
    }

    default boolean isExistByFullName(String fullName, String id) {
        QueryWrapper<SignatureEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SignatureEntity::getFullName, fullName);
        if (StringUtil.isNotEmpty(id)) {
            queryWrapper.lambda().ne(SignatureEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByEnCode(String enCode, String id) {
        QueryWrapper<SignatureEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SignatureEntity::getEnCode, enCode);
        if (StringUtil.isNotEmpty(id)) {
            queryWrapper.lambda().ne(SignatureEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }
}
