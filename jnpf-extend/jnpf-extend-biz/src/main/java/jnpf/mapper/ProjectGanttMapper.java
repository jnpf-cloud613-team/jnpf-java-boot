package jnpf.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.base.Page;
import jnpf.base.mapper.SuperMapper;
import jnpf.entity.ProjectGanttEntity;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 订单收款
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
public interface ProjectGanttMapper extends SuperMapper<ProjectGanttEntity> {

    default List<ProjectGanttEntity> getList(Page page) {
        QueryWrapper<ProjectGanttEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProjectGanttEntity::getType, 1).orderByAsc(ProjectGanttEntity::getSortCode)
                .orderByDesc(ProjectGanttEntity::getCreatorTime);
        if (!StringUtils.isEmpty(page.getKeyword())) {
            queryWrapper.lambda().and(
                    t -> t.like(ProjectGanttEntity::getEnCode, page.getKeyword())
                            .or().like(ProjectGanttEntity::getFullName, page.getKeyword())
            );
        }
        return this.selectList(queryWrapper);
    }

    default List<ProjectGanttEntity> getTaskList(String projectId) {
        ProjectGanttEntity entity = this.getInfo(projectId);
        QueryWrapper<ProjectGanttEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProjectGanttEntity::getType, 2).eq(ProjectGanttEntity::getProjectId, projectId).orderByAsc(ProjectGanttEntity::getSortCode);
        List<ProjectGanttEntity> list = this.selectList(queryWrapper);
        list.add(entity);
        return list;
    }

    default ProjectGanttEntity getInfo(String id) {
        QueryWrapper<ProjectGanttEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProjectGanttEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default boolean allowDelete(String id) {
        QueryWrapper<ProjectGanttEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().and(t ->
                t.eq(ProjectGanttEntity::getParentId, id).or().eq(ProjectGanttEntity::getProjectId, id)
        );
        return this.selectList(queryWrapper).isEmpty();
    }

    default void delete(ProjectGanttEntity entity) {
        this.deleteById(entity.getId());
    }

    default void create(ProjectGanttEntity entity) {
        entity.setId(RandomUtil.uuId());
        if (entity.getEnabledMark() == null) {
            entity.setEnabledMark(1);
        }
        entity.setSortCode(RandomUtil.parses());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        this.insert(entity);
    }

    default boolean update(String id, ProjectGanttEntity entity) {
        entity.setId(id);
        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        return SqlHelper.retBool(this.updateById(entity));
    }

    default boolean isExistByFullName(String fullName, String id) {
        QueryWrapper<ProjectGanttEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProjectGanttEntity::getFullName, fullName);
        if (!StringUtils.isEmpty(id)) {
            queryWrapper.lambda().ne(ProjectGanttEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByEnCode(String enCode, String id) {
        QueryWrapper<ProjectGanttEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProjectGanttEntity::getEnCode, enCode);
        if (!StringUtils.isEmpty(id)) {
            queryWrapper.lambda().ne(ProjectGanttEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

}
