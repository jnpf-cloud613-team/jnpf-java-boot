package jnpf.message.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.message.entity.ShortLinkEntity;
import jnpf.message.mapper.ShortLInkMapper;
import jnpf.message.service.ShortLinkService;
import org.springframework.stereotype.Service;

/**
 * 消息模板（新）
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
@Service
public class ShortLinkServiceImpl extends SuperServiceImpl<ShortLInkMapper, ShortLinkEntity> implements ShortLinkService {

    @Override
    public ShortLinkEntity getInfoByLink(String link) {
        return this.baseMapper.getInfoByLink(link);
    }


    @Override
    public String shortLink(String link) {
        return this.baseMapper.shortLink(link);
    }

}
