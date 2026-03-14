package jnpf.permission.service;

import jnpf.base.service.SuperService;
import jnpf.permission.entity.UserExtraEntity;


public interface UserExtraService extends SuperService<UserExtraEntity> {

    /**
     * 用户扩展属性
     * @param userId 用户id
     * @return 用户拓展属性
     */
    UserExtraEntity getUserExtraByUserId(String userId);

    /**
     * 更新或新增用户额外信息
     * @param userExtraEntity
     * @return
     */
    UserExtraEntity updateUserExtra(UserExtraEntity userExtraEntity);

    /**
     * 根据用户id删除用户额外信息
     * @param userId 用户id
     * @return 返回删除结果
     */
    Boolean deleteUserExtraByUserId(String userId);

}
