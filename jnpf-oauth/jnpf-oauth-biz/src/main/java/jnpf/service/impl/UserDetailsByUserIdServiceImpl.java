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
 * 使用用户ID获取用户信息
 */
@Service(AuthConsts.USERDETAIL_USER_ID)
@RequiredArgsConstructor
public class UserDetailsByUserIdServiceImpl implements UserDetailService {

    private static final Integer ORDER = 1;

    private final UserService userApi;

    @Override
    public UserEntity loadUserEntity(UserInfo userInfo) throws LoginException {
        UserEntity userEntity = userApi.getInfo(userInfo.getUserId());
        if (userEntity == null) {
            throw new LoginException(MsgCode.LOG101.get());
        }
        return userEntity;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

}
