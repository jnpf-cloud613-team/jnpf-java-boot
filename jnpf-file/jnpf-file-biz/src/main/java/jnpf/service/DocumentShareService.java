package jnpf.service;

import jnpf.base.service.SuperService;
import jnpf.entity.DocumentShareEntity;

import java.util.List;


/**
 * 大数据测试
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
public interface DocumentShareService extends SuperService<DocumentShareEntity> {

    /**
     * 查询共享给我的文件
     *
     * @param docId
     * @param shareUserId
     * @return
     */
    DocumentShareEntity getByDocIdAndShareUserId(String docId, String shareUserId);

    List<DocumentShareEntity> getShareToMe(List<String> strings);
}
