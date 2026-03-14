package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.flowable.entity.NodeRecordEntity;
import jnpf.flowable.model.record.NodeRecordModel;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/6/25 17:07
 */
public interface NodeRecordService extends SuperService<NodeRecordEntity> {
    /**
     * 列表
     *
     * @param taskId 任务主键
     */
    List<NodeRecordEntity> getList(String taskId);

    /**
     * 节点记录保存
     *
     * @param model 参数
     */
    void create(NodeRecordModel model);

    /**
     * 节点记录更新
     *
     * @param model 参数
     */
    void update(NodeRecordModel model);
}
