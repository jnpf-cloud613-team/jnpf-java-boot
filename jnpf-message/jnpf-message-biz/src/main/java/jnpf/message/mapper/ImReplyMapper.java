package jnpf.message.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.base.mapper.SuperMapper;
import jnpf.message.entity.ImReplyEntity;
import jnpf.message.model.ImReplyListModel;
import jnpf.util.UserProvider;

import java.util.List;

/**
 * 聊天会话
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-05-28
 */
public interface ImReplyMapper extends SuperMapper<ImReplyEntity> {

    /**
     * 聊天会话列表
     *
     * @return
     */
    List<ImReplyListModel> getImReplyList();

    default List<ImReplyEntity> getList() {
        QueryWrapper<ImReplyEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ImReplyEntity::getUserId, UserProvider.getUser().getUserId()).or()
                .eq(ImReplyEntity::getReceiveUserId, UserProvider.getUser().getUserId())
                .orderByDesc(ImReplyEntity::getUserId);
        return this.selectList(queryWrapper);
    }

    default boolean saveImReply(ImReplyEntity entity) {
        QueryWrapper<ImReplyEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(ImReplyEntity::getUserId, entity.getUserId())
                .eq(ImReplyEntity::getReceiveUserId, entity.getReceiveUserId());

        ImReplyEntity imReplyEntity = this.selectOne(queryWrapper);
        if (imReplyEntity != null) {
            entity.setId(imReplyEntity.getId());
            return SqlHelper.retBool(this.updateById(entity)); // 返回更新操作的结果
        }
        return SqlHelper.retBool(this.insert(entity)); // 返回插入操作的结果
    }

    default boolean relocation(String sendUserId, String receiveUserId) {
        QueryWrapper<ImReplyEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().and(t -> {
            t.eq(ImReplyEntity::getUserId, receiveUserId)
                    .eq(ImReplyEntity::getReceiveUserId, sendUserId).or();
            t.eq(ImReplyEntity::getReceiveUserId, receiveUserId)
                    .eq(ImReplyEntity::getUserId, sendUserId);
        });
        List<ImReplyEntity> list = this.selectList(queryWrapper);
        for (ImReplyEntity entity : list) {
            if (entity.getDeleteUserId() != null && !entity.getDeleteUserId().equals(sendUserId)) {
                entity.setDeleteMark(1);
                this.updateById(entity);
            }
            entity.setDeleteUserId(sendUserId);
            this.updateById(entity);
        }
        QueryWrapper<ImReplyEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ImReplyEntity::getDeleteMark, 1);
        this.delete(wrapper);
        return false;
    }

}
