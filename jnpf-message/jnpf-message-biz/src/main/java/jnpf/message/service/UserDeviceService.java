
package jnpf.message.service;

import jnpf.base.service.SuperService;
import jnpf.message.entity.UserDeviceEntity;

import java.util.List;

/**
 * 消息模板（新）
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
public interface UserDeviceService extends SuperService<UserDeviceEntity> {

    UserDeviceEntity getInfoByUserId(String userId);

    List<String> getCidList(String userId);

    UserDeviceEntity getInfoByClientId(String clientId);

    void create(UserDeviceEntity entity);

    boolean update(String id, UserDeviceEntity entity);

    void delete(UserDeviceEntity entity);

}
