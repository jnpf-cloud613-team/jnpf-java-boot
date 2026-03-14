
package jnpf.message.service;

import jnpf.base.service.SuperService;

import jnpf.message.entity.WechatUserEntity;

/**
 *
 * 消息模板（新）
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
public interface WechatUserService extends SuperService<WechatUserEntity> {

    WechatUserEntity getInfoByGzhId(String userId,String gzhId);

    void create(WechatUserEntity entity);

    boolean update(String id, WechatUserEntity entity);

    void delete(WechatUserEntity entity);

}
