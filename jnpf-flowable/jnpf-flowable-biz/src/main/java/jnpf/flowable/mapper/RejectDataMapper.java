package jnpf.flowable.mapper;

import jnpf.base.mapper.SuperMapper;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.EventLogEntity;
import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.entity.RejectDataEntity;
import jnpf.flowable.entity.TaskEntity;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/8 18:06
 */
public interface RejectDataMapper extends SuperMapper<RejectDataEntity> {

    default RejectDataEntity getInfo(String id) throws WorkFlowException {
        RejectDataEntity entity = this.selectById(id);
        if (entity == null) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        return entity;
    }

    default RejectDataEntity create(TaskEntity taskEntity, List<OperatorEntity> operatorEntityList, List<EventLogEntity> eventLogList, String nodeCode) {
        RejectDataEntity entity = new RejectDataEntity();
        entity.setId(RandomUtil.uuId());
        entity.setTaskJson(JsonUtil.getObjectToString(taskEntity));
        entity.setOperatorJson(JsonUtil.getObjectToString(operatorEntityList));
        entity.setEventLogJson(JsonUtil.getObjectToString(eventLogList));
        entity.setNodeCode(nodeCode);
        this.insert(entity);
        return entity;
    }
}
