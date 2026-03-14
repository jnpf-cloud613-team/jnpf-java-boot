package jnpf.flowable.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.CommentEntity;
import jnpf.flowable.mapper.CommentMapper;
import jnpf.flowable.model.comment.CommentPagination;
import jnpf.flowable.model.message.FlowMsgModel;
import jnpf.flowable.service.CommentService;
import jnpf.flowable.util.FlowUtil;
import jnpf.flowable.util.MsgUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 流程评论
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends SuperServiceImpl<CommentMapper, CommentEntity> implements CommentService {


    private final MsgUtil msgUtil;

    private final FlowUtil flowUtil;


    @Override
    public List<CommentEntity> getlist(CommentPagination pagination) {
        return this.baseMapper.getlist(pagination);
    }

    @Override
    public List<CommentEntity> getlist() {
        return this.baseMapper.getlist();
    }

    @Override
    public List<CommentEntity> getlist(List<String> idList) {
        return this.baseMapper.getlist(idList);
    }

    @Override
    public CommentEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public void create(CommentEntity entity) throws WorkFlowException {
        this.baseMapper.create(entity);
        FlowMsgModel msgModel = flowUtil.sendMsg(entity);
        msgUtil.message(msgModel);
    }


    @Override
    public void update(String id, CommentEntity entity) {
        this.baseMapper.update(id, entity);
    }

    @Override
    public void delete(CommentEntity entity, boolean delComment) {
        this.baseMapper.delete(entity, delComment);
    }
}
