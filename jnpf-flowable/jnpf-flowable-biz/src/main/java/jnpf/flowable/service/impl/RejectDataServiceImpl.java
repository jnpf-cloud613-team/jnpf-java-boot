package jnpf.flowable.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.EventLogEntity;
import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.entity.RejectDataEntity;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.mapper.RejectDataMapper;
import jnpf.flowable.service.RejectDataService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/8 18:08
 */
@Service
public class RejectDataServiceImpl extends SuperServiceImpl<RejectDataMapper, RejectDataEntity> implements RejectDataService {

    @Override
    public RejectDataEntity getInfo(String id) throws WorkFlowException {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public RejectDataEntity create(TaskEntity taskEntity, List<OperatorEntity> operatorEntityList, List<EventLogEntity> eventLogList, String nodeCode) {
        return this.baseMapper.create(taskEntity, operatorEntityList, eventLogList, nodeCode);
    }

}
