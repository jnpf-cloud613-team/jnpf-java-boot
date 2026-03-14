
package jnpf.message.service;


import jnpf.base.service.SuperService;
import jnpf.message.entity.MessageMonitorEntity;
import jnpf.message.model.messagemonitor.MessageMonitorPagination;

import java.util.List;

/**
 * 消息监控
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-22
 */
public interface MessageMonitorService extends SuperService<MessageMonitorEntity> {

    List<MessageMonitorEntity> getList(MessageMonitorPagination messageMonitorPagination);

    List<MessageMonitorEntity> getTypeList(MessageMonitorPagination messageMonitorPagination, String dataType);

    MessageMonitorEntity getInfo(String id);

    void delete(MessageMonitorEntity entity);

    void create(MessageMonitorEntity entity);

    boolean update(String id, MessageMonitorEntity entity);

    String userSelectValues(String ids);

    /**
     * 删除
     *
     * @param ids
     * @return
     */
    boolean delete(String[] ids);

    void emptyMonitor();
}
