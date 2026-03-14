package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.flowable.entity.RevokeEntity;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/8/24 13:39
 */
public interface RevokeService extends SuperService<RevokeEntity> {
    /**
     * 获取撤销任务关联
     *
     * @param revokeTaskId 撤销任务主键
     */
    RevokeEntity getRevokeTask(String revokeTaskId);

    /**
     * 用于判断原任务是否能撤销
     *
     * @param taskId 任务主键
     */
    Boolean checkExist(String taskId);

    /**
     * 假删除
     *
     * @param revokeTaskId 撤销任务主键
     */
    void deleteRevoke(String revokeTaskId);

    /**
     * 根据任务id获取撤销任务id
     *
     * @param ids 任务id
     */
    List<String> getByTaskId(List<String> ids);
}
