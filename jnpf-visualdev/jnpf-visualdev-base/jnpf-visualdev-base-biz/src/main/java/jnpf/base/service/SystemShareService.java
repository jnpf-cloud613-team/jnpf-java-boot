package jnpf.base.service;

import jnpf.base.entity.SystemShareEntity;
import jnpf.base.model.share.SystemShareVo;

import java.util.List;

/**
 * 跨应用数据Service
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/7/30 16:22:57
 */
public interface SystemShareService extends SuperService<SystemShareEntity> {

    /**
     * 获取跨应用数据
     *
     * @return
     */
    List<SystemShareEntity> getList();

    /**
     * 保存数据
     *
     * @param ids
     * @return
     */
    void save(List<String> ids);

    /**
     * 获取回显数据
     *
     * @return
     */
    List<SystemShareVo> selectedList(List<String> ids);

    /**
     * 获取下拉数据
     *
     * @return
     */
    List<SystemShareVo> selector();
}
