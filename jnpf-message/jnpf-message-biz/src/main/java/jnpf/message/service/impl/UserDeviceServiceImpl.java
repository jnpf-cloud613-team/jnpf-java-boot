package jnpf.message.service.impl;


import jnpf.base.service.SuperServiceImpl;
import jnpf.message.entity.UserDeviceEntity;
import jnpf.message.mapper.UserDeviceMapper;
import jnpf.message.service.UserDeviceService;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 消息模板（新）
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
@Service
public class UserDeviceServiceImpl extends SuperServiceImpl<UserDeviceMapper, UserDeviceEntity> implements UserDeviceService {


    @Override
    public UserDeviceEntity getInfoByUserId(String userId) {
        return this.baseMapper.getInfoByUserId(userId);
    }

    @Override
    public List<String> getCidList(String userId) {
        return this.baseMapper.getCidList(userId);
    }

    @Override
    public UserDeviceEntity getInfoByClientId(String clientId) {
        return this.baseMapper.getInfoByClientId(clientId);
    }

    @Override
    public void create(UserDeviceEntity entity) {
        this.save(entity);
    }

    @Override
    public boolean update(String id, UserDeviceEntity entity) {
        entity.setId(id);
        return this.updateById(entity);
    }

    @Override
    public void delete(UserDeviceEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
        }
    }

}
