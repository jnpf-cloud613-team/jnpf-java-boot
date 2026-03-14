package jnpf.flowable.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.flowable.entity.CirculateEntity;
import jnpf.flowable.mapper.CirculateMapper;
import jnpf.flowable.model.operator.OperatorVo;
import jnpf.flowable.model.task.TaskPagination;
import jnpf.flowable.service.CirculateService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2024/4/29 下午5:21
 */
@Service
public class CirculateServiceImpl extends SuperServiceImpl<CirculateMapper, CirculateEntity> implements CirculateService {

    @Override
    public List<CirculateEntity> getList(String taskId) {
        return this.baseMapper.getList(taskId);
    }

    @Override
    public List<CirculateEntity> getList(String taskId, String nodeCode) {
        return this.baseMapper.getList(taskId, nodeCode);
    }

    @Override
    public List<OperatorVo> getList(TaskPagination pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public List<CirculateEntity> getNodeList(String taskId, String nodeId) {
        return this.baseMapper.getNodeList(taskId, nodeId);
    }
}
