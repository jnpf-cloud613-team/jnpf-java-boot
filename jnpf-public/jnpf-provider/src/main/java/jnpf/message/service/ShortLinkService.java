
package jnpf.message.service;

import jnpf.base.service.SuperService;

import jnpf.message.entity.ShortLinkEntity;

/**
 *
 * 消息模板（新）
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
public interface ShortLinkService extends SuperService<ShortLinkEntity> {

    String shortLink (String link);

    ShortLinkEntity getInfoByLink(String link);
}
