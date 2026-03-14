package jnpf.flowable.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.TemplateJsonEntity;
import jnpf.flowable.entity.TemplateNodeEntity;
import jnpf.flowable.mapper.TemplateJsonMapper;
import jnpf.flowable.mapper.TemplateNodeMapper;
import jnpf.flowable.service.TemplateNodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateNodeServiceImpl extends SuperServiceImpl<TemplateNodeMapper, TemplateNodeEntity> implements TemplateNodeService {


    private final TemplateJsonMapper templateJsonMapper;

    @Override
    public List<TemplateNodeEntity> getList(String flowId) {
        return this.baseMapper.getList(flowId);
    }

    @Override
    public List<TemplateNodeEntity> getList(List<String> flowIds, String nodeType) {
        return this.baseMapper.getList(flowIds, nodeType);
    }

    @Override
    public List<TemplateNodeEntity> getListLikeUserId(String userId) {
        return this.baseMapper.getListLikeUserId(userId);
    }

    @Override
    public TemplateNodeEntity getInfo(String id) throws WorkFlowException {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public void create(TemplateNodeEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, TemplateNodeEntity entity) {
        return this.baseMapper.update(id, entity);
    }

    @Override
    public void delete(TemplateNodeEntity entity) {
        this.baseMapper.delete(entity);
    }

    @Override
    public void deleteList(List<String> idList) {
        this.baseMapper.deleteList(idList);
    }

    @Override
    public void delete(List<String> idList) {
        this.baseMapper.delete(idList);
    }

    @Override
    public List<TemplateNodeEntity> getListStart() {
        List<TemplateJsonEntity> listOfEnable = templateJsonMapper.getListOfEnable();
        return this.baseMapper.getListStart(listOfEnable);
    }
}
