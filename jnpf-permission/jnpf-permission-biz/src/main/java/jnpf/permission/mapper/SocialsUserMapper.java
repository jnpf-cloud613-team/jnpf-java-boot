package jnpf.permission.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.permission.entity.SocialsUserEntity;

import java.util.List;

/**
 * 流程设计
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/7/14 9:49:19
 */
public interface SocialsUserMapper extends SuperMapper<SocialsUserEntity> {

    default List<SocialsUserEntity> getListByUserId(String userId) {
        QueryWrapper<SocialsUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SocialsUserEntity::getUserId, userId);
        return this.selectList(queryWrapper);
    }

    default List<SocialsUserEntity> getUserIfnoBySocialIdAndType(String socialId, String socialType) {
        QueryWrapper<SocialsUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SocialsUserEntity::getSocialId, socialId);
        queryWrapper.lambda().eq(SocialsUserEntity::getSocialType, socialType);
        return this.selectList(queryWrapper);
    }

    default List<SocialsUserEntity> getListByUserIdAndSource(String userId, String socialType) {
        QueryWrapper<SocialsUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SocialsUserEntity::getUserId, userId);
        queryWrapper.lambda().eq(SocialsUserEntity::getSocialType, socialType);
        return this.selectList(queryWrapper);
    }

    default SocialsUserEntity getInfoBySocialId(String socialId, String socialType) {
        QueryWrapper<SocialsUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SocialsUserEntity::getSocialId, socialId);
        queryWrapper.lambda().eq(SocialsUserEntity::getSocialType, socialType);
        return this.selectOne(queryWrapper);
    }

    default void deleteAllByUserId(List<String> userId) {
        if (CollUtil.isEmpty(userId)) {
            return;
        }
        QueryWrapper<SocialsUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(SocialsUserEntity::getUserId, userId);
        this.deleteByIds(selectList(queryWrapper));
    }
}
