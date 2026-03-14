package jnpf.service.impl;

import jnpf.base.Page;
import jnpf.base.service.SuperServiceImpl;
import jnpf.entity.ProjectGanttEntity;
import jnpf.mapper.ProjectGanttMapper;
import jnpf.service.ProjectGanttService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单明细
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Service
public class ProjectGanttServiceImpl extends SuperServiceImpl<ProjectGanttMapper, ProjectGanttEntity> implements ProjectGanttService {

    @Override
    public List<ProjectGanttEntity> getList(Page page) {
        return this.baseMapper.getList(page);
    }

    @Override
    public List<ProjectGanttEntity> getTaskList(String projectId) {
        return this.baseMapper.getTaskList(projectId);
    }

    @Override
    public ProjectGanttEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public boolean allowDelete(String id) {
        return this.baseMapper.allowDelete(id);
    }

    @Override
    public void delete(ProjectGanttEntity entity) {
        this.baseMapper.delete(entity);
    }

    @Override
    public void create(ProjectGanttEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, ProjectGanttEntity entity) {
        return this.baseMapper.update(id, entity);
    }

    @Override
    public boolean isExistByFullName(String fullName, String id) {
        return this.baseMapper.isExistByFullName(fullName, id);
    }

    @Override
    public boolean isExistByEnCode(String enCode, String id) {
        return this.baseMapper.isExistByEnCode(enCode, id);
    }

}
