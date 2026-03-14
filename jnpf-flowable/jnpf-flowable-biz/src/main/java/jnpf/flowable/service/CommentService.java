package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.CommentEntity;
import jnpf.flowable.model.comment.CommentPagination;

import java.util.List;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2024/4/24 上午9:07
 */
public interface CommentService extends SuperService<CommentEntity> {

    /**
     * 列表
     *
     * @param pagination 请求参数
     * @return
     */
    List<CommentEntity> getlist(CommentPagination pagination);

    /**
     * 列表
     *
     * @return
     */
    List<CommentEntity> getlist();

    /**
     * 列表
     *
     * @return
     */
    List<CommentEntity> getlist(List<String> idList);

    /**
     * 信息
     *
     * @param id 主键值
     * @return
     */
    CommentEntity getInfo(String id);

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    void create(CommentEntity entity) throws WorkFlowException;

    /**
     * 更新
     *
     * @param id     主键值
     * @param entity 实体对象
     * @return
     */
    void update(String id, CommentEntity entity);

    /**
     * 删除
     *
     * @param entity 实体对象
     * @return
     */
    void delete(CommentEntity entity, boolean delComment);
}
