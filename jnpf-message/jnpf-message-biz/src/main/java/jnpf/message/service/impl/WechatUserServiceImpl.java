package jnpf.message.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.service.SuperServiceImpl;
import jnpf.message.entity.WechatUserEntity;
import jnpf.message.mapper.WechatUserMapper;
import jnpf.message.service.WechatUserService;
import org.springframework.stereotype.Service;

/**
 * 消息模板（新）
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
@Service
public class WechatUserServiceImpl extends SuperServiceImpl<WechatUserMapper, WechatUserEntity> implements WechatUserService {


    @Override
    public WechatUserEntity getInfoByGzhId(String userId, String gzhId) {
        QueryWrapper<WechatUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(WechatUserEntity::getUserId, userId);
        queryWrapper.lambda().eq(WechatUserEntity::getGzhId, gzhId);
        queryWrapper.lambda().eq(WechatUserEntity::getCloseMark, 1);
        return this.getOne(queryWrapper);
    }

    @Override
    public void create(WechatUserEntity entity) {
        this.save(entity);
    }

    @Override
    public boolean update(String id, WechatUserEntity entity) {
        entity.setId(id);
        return this.updateById(entity);
    }

    @Override
    public void delete(WechatUserEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
        }
    }

}
