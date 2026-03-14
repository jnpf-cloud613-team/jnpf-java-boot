package jnpf.service.impl;

import jnpf.base.UserInfo;
import jnpf.constant.MsgCode;
import jnpf.consts.AuthConsts;
import jnpf.exception.LoginException;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.service.UserDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 默认使用用户名获取用户信息
 */
@Service(AuthConsts.USERDETAIL_ACCOUNT)
@RequiredArgsConstructor
public class UserDetailsByUserAccountServiceImpl implements UserDetailService {

    private final UserService userApi;

    @Override
    public UserEntity loadUserEntity(UserInfo userInfo) throws LoginException {
        UserEntity userEntity = userApi.getUserByAccount(userInfo.getUserAccount());
        if (userEntity == null) {
            throw new LoginException(MsgCode.LOG101.get());
        }
        return userEntity;
    }


    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

}
