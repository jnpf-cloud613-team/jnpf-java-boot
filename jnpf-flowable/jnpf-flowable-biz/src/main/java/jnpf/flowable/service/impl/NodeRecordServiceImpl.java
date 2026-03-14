package jnpf.flowable.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.flowable.entity.NodeRecordEntity;
import jnpf.flowable.mapper.NodeRecordMapper;
import jnpf.flowable.model.record.NodeRecordModel;
import jnpf.flowable.service.NodeRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/6/25 17:08
 */
@Service
public class NodeRecordServiceImpl extends SuperServiceImpl<NodeRecordMapper, NodeRecordEntity> implements NodeRecordService {
    @Override
    public List<NodeRecordEntity> getList(String taskId) {
        return this.baseMapper.getList(taskId);
    }

    @Override
    public void create(NodeRecordModel model) {
        this.baseMapper.create(model);
    }

    @Override
    public void update(NodeRecordModel model) {
        this.baseMapper.update(model);
    }
}
