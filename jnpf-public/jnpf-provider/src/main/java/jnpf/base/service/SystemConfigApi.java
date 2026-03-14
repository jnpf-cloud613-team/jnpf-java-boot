package jnpf.base.service;

import jnpf.base.ActionResult;
import jnpf.base.model.systemconfig.SysConfigModel;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/8/28 10:36
 */
public interface SystemConfigApi {
    /**
     * 列表
     */
    ActionResult<SysConfigModel> list();
}
