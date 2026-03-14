package jnpf.flowable.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.util.StringUtil;
import com.github.yulichang.extension.mapping.wrapper.MappingQuery;
import jnpf.base.mapper.SuperMapper;
import jnpf.flowable.entity.CandidatesEntity;
import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.model.util.FlowNature;
import jnpf.util.RandomUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 16:00
 */
public interface CandidatesMapper extends SuperMapper<CandidatesEntity> {

    default List<CandidatesEntity> getList(String taskId, String nodeCode) {
        QueryWrapper<CandidatesEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(taskId)) {
            queryWrapper.lambda().eq(CandidatesEntity::getTaskId, taskId);
        }
        if (StringUtil.isNotEmpty(nodeCode)) {
            queryWrapper.lambda().eq(CandidatesEntity::getNodeCode, nodeCode);
        }
        return this.selectList(queryWrapper);
    }

    default List<CandidatesEntity> getListByCode(String taskId, String nodeCode) {
        QueryWrapper<CandidatesEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CandidatesEntity::getTaskId, taskId).eq(CandidatesEntity::getNodeCode, nodeCode);
        return this.selectList(queryWrapper);
    }

    default void deleteByCodes(String taskId, List<String> nodeIds) {
        QueryWrapper<CandidatesEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CandidatesEntity::getTaskId, taskId);
        if (CollUtil.isNotEmpty(nodeIds)) {
            queryWrapper.lambda().in(CandidatesEntity::getNodeCode, nodeIds);
        }
        List<CandidatesEntity> list = this.selectList(queryWrapper);
        if (CollUtil.isNotEmpty(list)) {
            this.deleteByIds(list);
        }
    }

    default void delete(String taskId, List<String> nodeIds, List<String> userId) {
        QueryWrapper<CandidatesEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CandidatesEntity::getTaskId, taskId).in(CandidatesEntity::getHandleId, userId);
        if (CollUtil.isNotEmpty(nodeIds)) {
            queryWrapper.lambda().in(CandidatesEntity::getNodeCode, nodeIds);
        }
        List<CandidatesEntity> list = this.selectList(queryWrapper);
        if (CollUtil.isNotEmpty(list)) {
            this.deleteByIds(list);
        }
    }

    default List<String> getBranch(String taskId, String nodeCode) {
        List<String> resList = new ArrayList<>();
        QueryWrapper<CandidatesEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CandidatesEntity::getTaskId, taskId).eq(CandidatesEntity::getNodeCode, nodeCode)
                .eq(CandidatesEntity::getType, FlowNature.BRANCH);
        List<CandidatesEntity> list = this.selectList(queryWrapper);
        if (CollUtil.isNotEmpty(list)) {
            for (CandidatesEntity entity : list) {
                if (jnpf.util.StringUtil.isNotEmpty(entity.getCandidates())) {
                    List<String> branch = Arrays.stream(entity.getCandidates().split(",")).collect(Collectors.toList());
                    resList.addAll(branch);
                }
            }
        }
        return resList;
    }

    default void createBranch(List<String> branchList, OperatorEntity operator) {
        if (CollUtil.isNotEmpty(branchList)) {
            this.deleteBranch(operator.getTaskId(), operator.getNodeCode());
            CandidatesEntity entity = new CandidatesEntity();
            entity.setId(RandomUtil.uuId());
            entity.setTaskId(operator.getTaskId());
            entity.setNodeCode(operator.getNodeCode());
            entity.setOperatorId(operator.getId());
            entity.setType(FlowNature.BRANCH);
            entity.setCandidates(String.join(",", branchList));
            this.insert(entity);
        }
    }

    default void deleteBranch(String taskId, String nodeCode) {
        QueryWrapper<CandidatesEntity> wrapper = new MappingQuery<>();
        wrapper.lambda().eq(CandidatesEntity::getTaskId, taskId).eq(CandidatesEntity::getNodeCode, nodeCode)
                .eq(CandidatesEntity::getType, FlowNature.BRANCH);
        this.delete(wrapper);
    }

}
