package jnpf.message.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.message.entity.ImReplyEntity;
import jnpf.message.mapper.ImReplyMapper;
import jnpf.message.model.ImReplyListModel;
import jnpf.message.service.ImReplyService;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-05-29
 */
@Service
@RequiredArgsConstructor
public class ImReplyServiceImpl extends SuperServiceImpl<ImReplyMapper, ImReplyEntity> implements ImReplyService {

    private final UserService userService;

    @Override
    public List<ImReplyEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public boolean saveImReply(ImReplyEntity entity) {
        return this.baseMapper.saveImReply(entity);
    }

    @Override
    public List<ImReplyListModel> getImReplyList() {
        List<ImReplyListModel> imReplyList = this.baseMapper.getImReplyList();
        List<ImReplyListModel> imReplyLists;
        // 过滤掉用户id和接收id相同的
        imReplyLists = imReplyList.stream().filter(t -> t.getImreplySendDeleteMark() == null).collect(Collectors.toList());
        //我发给别人
        List<ImReplyListModel> collect = imReplyLists.stream().filter(t -> t.getUserId().equals(UserProvider.getUser().getUserId())).collect(Collectors.toList());
        //头像替换成对方的
        for (ImReplyListModel imReplyListModel : collect) {
            UserEntity entity = userService.getInfo(imReplyListModel.getId());
            imReplyListModel.setHeadIcon(entity != null ? entity.getHeadIcon() : "");
        }
        //别人发给我
        List<ImReplyListModel> list = imReplyLists.stream().filter(t -> t.getId().equals(UserProvider.getUser().getUserId())).collect(Collectors.toList());
        for (ImReplyListModel model : list) {
            //移除掉互发的
            List<ImReplyListModel> collect1 = collect.stream().filter(t -> t.getId().equals(model.getUserId())).collect(Collectors.toList());
            if (!collect1.isEmpty()) {
                //判断我发给别人的时间和接收的时间大小
                //接收的大于发送的
                if (model.getLatestDate().getTime() > collect1.get(0).getLatestDate().getTime()) {
                    collect.remove(collect1.get(0));
                } else { //发送的大于接收的则跳过
                    continue;
                }
            }
            ImReplyListModel imReplyListModel = new ImReplyListModel();
            UserEntity entity = userService.getInfo(model.getUserId());
            if (entity != null) {
                imReplyListModel.setHeadIcon(entity.getHeadIcon());
                imReplyListModel.setUserId(UserProvider.getUser().getUserId());
                imReplyListModel.setId(entity.getId());
                imReplyListModel.setLatestDate(model.getLatestDate());
                imReplyListModel.setLatestMessage(model.getLatestMessage());
                imReplyListModel.setMessageType(model.getMessageType());
                if (model.getImreplySendDeleteMark() != null && !model.getImreplySendDeleteMark().equals(UserProvider.getUser().getUserId())) {
                    imReplyListModel.setSendDeleteMark(model.getSendDeleteMark());
                    imReplyListModel.setImreplySendDeleteMark(model.getImreplySendDeleteMark());
                    imReplyListModel.setDeleteMark(model.getDeleteMark());
                }
                imReplyListModel.setDeleteUserId(model.getDeleteUserId());

                collect.add(imReplyListModel);
            }
        }
        return collect;
    }

    @Override
    public boolean relocation(String sendUserId, String receiveUserId) {
        return this.baseMapper.relocation(sendUserId, receiveUserId);
    }

}
