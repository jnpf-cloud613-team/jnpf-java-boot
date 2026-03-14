package jnpf.base.service.impl;


import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import jnpf.base.entity.VisualdevReleaseEntity;
import jnpf.base.mapper.VisualdevReleaseMapper;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.VisualdevReleaseService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/3/16
 */
@Service
public class VisualdevReleaseServiceImpl extends SuperServiceImpl<VisualdevReleaseMapper, VisualdevReleaseEntity> implements VisualdevReleaseService {

    @Override
    public long beenReleased(String id) {
        return this.baseMapper.beenReleased(id);
    }

    @Override
    public List<VisualdevReleaseEntity> selectorList(String systemId) {
        return this.baseMapper.selectorList(systemId);
    }

    @Override
    public List<VisualdevReleaseEntity> selectByIds(List<String> ids, SFunction<VisualdevReleaseEntity, ?>... columns) {
        return this.baseMapper.selectByIds(ids, columns);
    }
}
