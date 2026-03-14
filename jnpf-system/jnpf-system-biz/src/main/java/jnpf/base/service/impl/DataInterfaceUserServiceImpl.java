package jnpf.base.service.impl;

import cn.hutool.core.collection.CollUtil;
import jnpf.base.entity.DataInterfaceUserEntity;
import jnpf.base.mapper.DataInterfaceUserMapper;
import jnpf.base.model.interfaceoauth.InterfaceUserForm;
import jnpf.base.service.DataInterfaceUserService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.4.7
 * @copyright 引迈信息技术有限公司
 * @date 2021/9/20 9:22
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DataInterfaceUserServiceImpl extends SuperServiceImpl<DataInterfaceUserMapper, DataInterfaceUserEntity> implements DataInterfaceUserService {


    protected final AuthUtil authUtil;

    @Override
    public void saveUserList(InterfaceUserForm interfaceUserForm) {
        this.baseMapper.saveUserList(interfaceUserForm);
    }

    @Override
    public List<DataInterfaceUserEntity> select(String oauthId) {
       return this.baseMapper.select(oauthId);
    }

    @Override
    public String getInterfaceUserToken(String tenantId, String oauthId, String userKey) {
        List<DataInterfaceUserEntity> select = this.select(oauthId);
        if (CollUtil.isEmpty(select)) {
            return null;
        }
        if (StringUtil.isEmpty(userKey)) {
            throw new IllegalArgumentException("未填写UserKey，请确认");
        }
        DataInterfaceUserEntity entity = select.stream().filter(item -> item.getUserKey().equals(userKey)).findFirst().orElse(null);
        if (entity == null) {
            throw new IllegalArgumentException("UserKey不匹配，请填写正确的UserKey");
        }

        return AuthUtil.loginTempUser(entity.getUserId(), tenantId, true);
    }
}
