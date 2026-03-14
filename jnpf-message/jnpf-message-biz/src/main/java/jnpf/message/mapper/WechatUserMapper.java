package jnpf.message.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.message.entity.WechatUserEntity;

/**
 * 消息模板（新）
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
public interface WechatUserMapper extends SuperMapper<WechatUserEntity> {

    default WechatUserEntity getInfoByGzhId(String userId, String gzhId) {
        QueryWrapper<WechatUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(WechatUserEntity::getUserId, userId);
        queryWrapper.lambda().eq(WechatUserEntity::getGzhId, gzhId);
        queryWrapper.lambda().eq(WechatUserEntity::getCloseMark, 1);
        return this.selectOne(queryWrapper);
    }
}
