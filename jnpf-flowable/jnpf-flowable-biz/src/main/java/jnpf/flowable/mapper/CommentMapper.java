package jnpf.flowable.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.UserInfo;
import jnpf.base.mapper.SuperMapper;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.CommentEntity;
import jnpf.flowable.model.comment.CommentPagination;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 流程评论
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 */
public interface CommentMapper extends SuperMapper<CommentEntity> {

    default List<CommentEntity> getlist(CommentPagination pagination) {
        QueryWrapper<CommentEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CommentEntity::getTaskId, pagination.getTaskId());
        queryWrapper.lambda().isNull(CommentEntity::getDeleteMark);
        queryWrapper.lambda().orderByDesc(CommentEntity::getCreatorTime);
        Page<CommentEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<CommentEntity> userIPage = this.selectPage(page, queryWrapper);
        return pagination.setData(userIPage.getRecords(), page.getTotal());
    }

    default List<CommentEntity> getlist() {
        QueryWrapper<CommentEntity> queryWrapper = new QueryWrapper<>();
        return this.selectList(queryWrapper);
    }

    default List<CommentEntity> getlist(List<String> idList) {
        List<CommentEntity> list = new ArrayList<>();
        if (!idList.isEmpty()) {
            QueryWrapper<CommentEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(CommentEntity::getId, idList);
            list.addAll(this.selectList(queryWrapper));
        }
        return list;
    }

    default CommentEntity getInfo(String id) {
        QueryWrapper<CommentEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CommentEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default void create(CommentEntity entity) throws WorkFlowException {
        entity.setCreatorTime(new Date());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        entity.setId(RandomUtil.uuId());
        this.insert(entity);
    }

    default void update(String id, CommentEntity entity) {
        entity.setId(id);
        this.updateById(entity);
    }

    default void delete(CommentEntity entity, boolean delComment) {
        if (entity != null) {
            UserInfo userInfo = UserProvider.getUser();
            if (delComment) {
                entity.setDeleteShow(1);
            } else {
                entity.setDeleteMark(1);
            }
            entity.setDeleteTime(new Date());
            entity.setDeleteUserId(userInfo.getUserId());
            this.updateById(entity);
        }
    }
}
