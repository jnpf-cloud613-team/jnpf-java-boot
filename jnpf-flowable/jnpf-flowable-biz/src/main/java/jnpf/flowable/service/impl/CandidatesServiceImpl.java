package jnpf.flowable.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.flowable.entity.CandidatesEntity;
import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.entity.TemplateNodeEntity;
import jnpf.flowable.mapper.CandidatesMapper;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.service.CandidatesService;
import jnpf.flowable.util.FlowUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 16:03
 */
@Service
@RequiredArgsConstructor
public class CandidatesServiceImpl extends SuperServiceImpl<CandidatesMapper, CandidatesEntity> implements CandidatesService {

    private final FlowUtil flowUtil;

    @Override
    public List<CandidatesEntity> getList(String taskId, String nodeCode) {
        return this.baseMapper.getList(taskId, nodeCode);
    }

    @Override
    public void create(FlowModel fo, String taskId, List<TemplateNodeEntity> nodeEntityList, OperatorEntity operator) {
        flowUtil.create(fo, taskId, nodeEntityList, operator);
    }


    @Override
    public void deleteByCodes(String taskId, List<String> nodeIds) {
        this.baseMapper.deleteByCodes(taskId, nodeIds);
    }

    @Override
    public void delete(String taskId, List<String> nodeIds, List<String> userId) {
        this.baseMapper.delete(taskId, nodeIds, userId);
    }

    @Override
    public List<String> getBranch(String taskId, String nodeCode) {
        return this.baseMapper.getBranch(taskId, nodeCode);
    }

    @Override
    public void createBranch(List<String> branchList, OperatorEntity operator) {
        this.baseMapper.createBranch(branchList, operator);
    }

    @Override
    public void deleteBranch(String taskId, String nodeCode) {
        this.baseMapper.deleteBranch(taskId, nodeCode);
    }
}
