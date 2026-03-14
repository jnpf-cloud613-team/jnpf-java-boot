package jnpf.flowable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.service.SuperServiceImpl;
import jnpf.flowable.entity.RecordEntity;
import jnpf.flowable.mapper.RecordMapper;
import jnpf.flowable.model.operator.OperatorVo;
import jnpf.flowable.model.record.RecordVo;
import jnpf.flowable.model.task.TaskPagination;
import jnpf.flowable.service.RecordService;
import jnpf.flowable.util.FlowUtil;
import jnpf.flowable.util.RecordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/23 9:22
 */
@Service
@RequiredArgsConstructor
public class RecordServiceImpl extends SuperServiceImpl<RecordMapper, RecordEntity> implements RecordService {

    
    private final RecordUtil recordUtil;
    
    private final FlowUtil flowUtil;

    @Override
    public List<RecordEntity> getList(String taskId) {
        return this.baseMapper.getList(taskId);
    }

    @Override
    public List<RecordEntity> getRecordList(String taskId, List<Integer> statusList) {
        return this.baseMapper.getRecordList(taskId, statusList);
    }

    @Override
    public List<OperatorVo> getList(TaskPagination pagination) {
        return flowUtil.getRecordList(pagination);
    }

    @Override
    public RecordEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public void create(RecordEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public void update(String id, RecordEntity entity) {
        this.baseMapper.update(id, entity);
    }

    @Override
    public void updateStatusToInvalid(String taskId, List<String> nodeCodeList) {
        this.baseMapper.updateStatusToInvalid(taskId, nodeCodeList);
    }

    @Override
    public List<RecordVo> getList(String taskId, String nodeId) {
        QueryWrapper<RecordEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(RecordEntity::getTaskId, taskId).like(RecordEntity::getNodeId, nodeId)
                .orderByDesc(RecordEntity::getHandleTime).orderByDesc(RecordEntity::getId);
        List<RecordEntity> list = this.list(queryWrapper);
        return recordUtil.getRecordList(list);
    }

}
