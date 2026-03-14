package jnpf.base.service;


import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import jnpf.base.entity.VisualdevReleaseEntity;

import java.util.List;


/**
 *
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @author JNPF开发平台组
 * @date 2021/3/16
 */
public interface VisualdevReleaseService extends SuperService<VisualdevReleaseEntity> {

    long beenReleased(String id);

    List<VisualdevReleaseEntity> selectorList(String systemId);

    List<VisualdevReleaseEntity> selectByIds(List<String> ids, SFunction<VisualdevReleaseEntity, ?>... columns);
}

