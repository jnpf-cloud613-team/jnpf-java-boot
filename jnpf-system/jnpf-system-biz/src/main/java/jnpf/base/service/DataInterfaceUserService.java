package jnpf.base.service;

import jnpf.base.entity.DataInterfaceUserEntity;
import jnpf.base.model.interfaceoauth.InterfaceUserForm;

import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.4.7
 * @copyright 引迈信息技术有限公司
 * @date 2021/9/20 9:22
 */
public interface DataInterfaceUserService extends SuperService<DataInterfaceUserEntity> {

    /**
     * 授权用户
     *
     * @param interfaceUserForm
     */
    void saveUserList(InterfaceUserForm interfaceUserForm);

    /**
     * 根据认证接口id查询授权用户列表
     *
     * @param oauthId
     * @return
     */
    List<DataInterfaceUserEntity> select(String oauthId);

    /**
     * 通过用户密钥获取用户token
     *
     * @param oauthId
     * @param userKey
     * @return
     */
    String getInterfaceUserToken(String tenantId, String oauthId, String userKey);

}
