package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.flowable.entity.SubtaskDataEntity;
import jnpf.flowable.model.task.FlowModel;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/12/6 15:33
 */
public interface SubtaskDataService extends SuperService<SubtaskDataEntity> {
    /**
     * 获取列表
     *
     * @param parentId   父实例主键
     * @param parentCode 父节点编码
     */
    List<SubtaskDataEntity> getList(String parentId, String parentCode);

    /**
     * 保存
     *
     * @param subTaskData 依次创建子流程参数集合
     */
    void save(List<FlowModel> subTaskData);
}
