package jnpf.onlinedev.service;

import jnpf.base.service.SuperService;
import jnpf.onlinedev.entity.VisualLogEntity;
import jnpf.onlinedev.model.log.VisualLogForm;
import jnpf.base.model.VisualLogModel;
import jnpf.onlinedev.model.log.VisualLogPage;

import java.util.List;

/**
 * 数据日志service
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/8/27 18:24:10
 */
public interface VisualLogService extends SuperService<VisualLogEntity> {

    void createEventLog(VisualLogForm form);

    List<VisualLogEntity> getList(VisualLogPage pagination);

    void addLog(VisualLogForm form, List<VisualLogModel> listLog);
}
