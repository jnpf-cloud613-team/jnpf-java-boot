package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.flowable.entity.CandidatesEntity;
import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.entity.TemplateNodeEntity;
import jnpf.flowable.model.task.FlowModel;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 16:01
 */
public interface CandidatesService extends SuperService<CandidatesEntity> {

    /**
     * 获取候选人
     *
     * @param taskId   任务id
     * @param nodeCode 节点编码
     */
    List<CandidatesEntity> getList(String taskId, String nodeCode);

    /**
     * 新建
     *
     * @param fo             参数类
     * @param taskId         任务id
     * @param nodeEntityList 节点集合
     * @param operator       经办实体
     */
    void create(FlowModel fo, String taskId, List<TemplateNodeEntity> nodeEntityList, OperatorEntity operator);

    /**
     * 删除
     *
     * @param taskId  任务主键
     * @param nodeIds 节点编码集合
     */
    void deleteByCodes(String taskId, List<String> nodeIds);

    /**
     * 删除
     *
     * @param taskId  任务主键
     * @param nodeIds 节点编码
     * @param userId  用户主键
     */
    void delete(String taskId, List<String> nodeIds, List<String> userId);

    /**
     * 获取选择分支
     *
     * @param taskId   任务主键
     * @param nodeCode 节点编码
     */
    List<String> getBranch(String taskId, String nodeCode);

    /**
     * 保存选择分支
     *
     * @param branchList 选择分支
     * @param operator   经办
     */
    void createBranch(List<String> branchList, OperatorEntity operator);

    /**
     * 删除选择分支
     *
     * @param taskId   任务主键
     * @param nodeCode 节点编码
     */
    void deleteBranch(String taskId, String nodeCode);
}
