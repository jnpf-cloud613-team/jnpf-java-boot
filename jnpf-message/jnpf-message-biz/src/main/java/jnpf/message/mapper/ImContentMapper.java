package jnpf.message.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.PageModel;
import jnpf.base.mapper.SuperMapper;
import jnpf.message.entity.ImContentEntity;
import jnpf.message.model.ImUnreadNumModel;
import jnpf.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 聊天内容
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface ImContentMapper extends SuperMapper<ImContentEntity> {

    List<ImUnreadNumModel> getUnreadList(@Param("receiveUserId") String receiveUserId);

    List<ImUnreadNumModel> getUnreadLists(@Param("receiveUserId") String receiveUserId);

    int readMessage(@Param("map") Map<String, String> map);

    default List<ImContentEntity> getMessageList(String sendUserId, String receiveUserId, PageModel pageModel) {
        if (pageModel == null) {
            return Collections.emptyList();
        }
        QueryWrapper<ImContentEntity> queryWrapper = new QueryWrapper<>();
        //发件人、收件人
        if (!StringUtil.isEmpty(sendUserId) && !StringUtil.isEmpty(receiveUserId)) {

            queryWrapper.lambda().and(wrapper -> {
                wrapper.eq(ImContentEntity::getSendUserId, sendUserId);
                wrapper.eq(ImContentEntity::getReceiveUserId, receiveUserId);
                wrapper.or().eq(ImContentEntity::getSendUserId, receiveUserId);
                wrapper.eq(ImContentEntity::getReceiveUserId, sendUserId);
            });
            queryWrapper.lambda().and(wrapper -> {
                wrapper.isNull(ImContentEntity::getDeleteUserId);
                wrapper.or().ne(ImContentEntity::getDeleteUserId, receiveUserId);
            });

        }

        //关键字查询
        if (pageModel.getKeyword() != null) {
            queryWrapper.lambda().like(ImContentEntity::getContent, pageModel.getKeyword());
            //排序
            pageModel.setSidx("f_send_time");
        }

        if (StringUtils.isEmpty(pageModel.getSidx())) {
            queryWrapper.lambda().orderByDesc(ImContentEntity::getSendTime);
        } else {
            queryWrapper = "asc".equalsIgnoreCase(pageModel.getSord()) ? queryWrapper.orderByAsc(pageModel.getSidx()) : queryWrapper.orderByDesc(pageModel.getSidx());
        }
        Page<ImContentEntity> page = new Page<>(pageModel.getPage(), pageModel.getRows());
        IPage<ImContentEntity> iPage = this.selectPage(page, queryWrapper);
        return pageModel.setData(iPage.getRecords(), page.getTotal());
    }

    default int getUnreadCount(String sendUserId, String receiveUserId) {
        QueryWrapper<ImContentEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ImContentEntity::getSendUserId, sendUserId).eq(ImContentEntity::getReceiveUserId, receiveUserId).eq(ImContentEntity::getEnabledMark, 0);
        return this.selectCount(queryWrapper).intValue();
    }

    default void readMessage(String sendUserId, String receiveUserId) {
        QueryWrapper<ImContentEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ImContentEntity::getSendUserId, sendUserId);
        queryWrapper.lambda().eq(ImContentEntity::getReceiveUserId, receiveUserId);
        queryWrapper.lambda().eq(ImContentEntity::getEnabledMark, 0);
        List<ImContentEntity> list = this.selectList(queryWrapper);
        for (ImContentEntity entity : list) {
            entity.setEnabledMark(1);
            entity.setReceiveTime(new Date());
            this.updateById(entity);
        }
    }

    default boolean deleteChatRecord(String sendUserId, String receiveUserId) {
        QueryWrapper<ImContentEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().and(t -> {
            t.eq(ImContentEntity::getSendUserId, receiveUserId)
                    .eq(ImContentEntity::getReceiveUserId, sendUserId).or();
            t.eq(ImContentEntity::getReceiveUserId, receiveUserId)
                    .eq(ImContentEntity::getSendUserId, sendUserId);
        });
        List<ImContentEntity> list = this.selectList(queryWrapper);
        for (ImContentEntity entity : list) {
            if (entity.getDeleteUserId() != null && !entity.getDeleteUserId().equals(sendUserId)) {
                entity.setDeleteMark(1);
                this.updateById(entity);
            }
            entity.setDeleteUserId(sendUserId);
            this.updateById(entity);
        }
        QueryWrapper<ImContentEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ImContentEntity::getDeleteMark, 1);
        this.delete(wrapper);
        return false;
    }
}

