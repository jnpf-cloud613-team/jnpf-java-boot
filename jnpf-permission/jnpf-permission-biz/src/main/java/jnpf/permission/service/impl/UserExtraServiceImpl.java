package jnpf.permission.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.permission.entity.UserExtraEntity;
import jnpf.permission.mapper.UserExtraMapper;
import jnpf.permission.service.UserExtraService;
import org.springframework.stereotype.Service;

@Service
public class UserExtraServiceImpl extends SuperServiceImpl<UserExtraMapper,UserExtraEntity> implements UserExtraService {

    @Override
    public UserExtraEntity getUserExtraByUserId(String userId) {
        return this.baseMapper.getUserExtraByUserId(userId);
    }

    @Override
    public UserExtraEntity updateUserExtra(UserExtraEntity userExtraEntity) {
        return this.baseMapper.updateUserExtra(userExtraEntity);
    }

    @Override
    public Boolean deleteUserExtraByUserId(String userId) {
       return this.baseMapper.deleteUserExtraByUserId(userId);
    }
}
