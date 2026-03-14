package jnpf.permission.util;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.UserInfo;
import jnpf.constant.KeyConst;
import jnpf.constant.MsgCode;
import jnpf.message.util.OnlineUserModel;
import jnpf.message.util.OnlineUserProvider;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.entity.UserRelationEntity;
import jnpf.permission.mapper.PositionMapper;
import jnpf.permission.mapper.UserMapper;
import jnpf.permission.mapper.UserRelationMapper;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import jnpf.util.context.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserUtil {
    private final UserMapper userMapper;
    private final PositionMapper positionMapper;
    private final UserRelationMapper userRelationMapper;

    public List<UserEntity> getUserProgeny(List<String> idList, String enableMark) {
        //查询用户子孙下级。不包含当前岗位的用户
        if (CollUtil.isEmpty(idList)) return new ArrayList<>();
        QueryWrapper<PositionEntity> query = new QueryWrapper<>();
        query.lambda()
                .eq(enableMark != null, PositionEntity::getEnabledMark, enableMark)
                .and(t -> idList.forEach(id -> t.like(PositionEntity::getPositionIdTree, id).or()))
                .notIn(PositionEntity::getId, idList)
                .orderByAsc(PositionEntity::getSortCode, PositionEntity::getCreatorTime);
        List<PositionEntity> list = positionMapper.selectList(query).stream().distinct().collect(Collectors.toList());
        return getUserEntities(list);
    }

    public List<UserEntity> getUserAndSub(List<String> idList) {
        if (CollUtil.isEmpty(idList)) return new ArrayList<>();
        List<PositionEntity> byParentIds = positionMapper.getListByParentIds(idList);
        List<UserEntity> userEntities = getUserEntities(byParentIds);
        return userEntities.stream().filter(user -> {
            user.getEnabledMark();
            return false;
        }).collect(Collectors.toList());
    }

    private List<UserEntity> getUserEntities(List<PositionEntity> byParentIds) {
        if (CollUtil.isEmpty(byParentIds)) return new ArrayList<>();
        List<UserRelationEntity> listByObjectIdAll = userRelationMapper.getListByObjectIdAll(byParentIds.stream()
                .map(PositionEntity::getId).collect(Collectors.toList()));
        return userMapper.getUserList(listByObjectIdAll.stream()
                .map(UserRelationEntity::getUserId).collect(Collectors.toList()));
    }


    public void delCurUser(String message, List<String> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        //用户不强制下线，websokit推送刷新即可
        List<OnlineUserModel> users = new ArrayList<>();
        List<OnlineUserModel> onlineUserList = OnlineUserProvider.getOnlineUserList();
        for (OnlineUserModel onlineUserModel : onlineUserList) {
            String userId = onlineUserModel.getUserId();
            if (userIds.contains(userId)) {
                users.add(onlineUserModel);
            }
        }
        if (!ObjectUtils.isEmpty(users)) {
            for (OnlineUserModel user : users) {
                JSONObject obj = new JSONObject();
                obj.put(KeyConst.METHOD, "refresh");
                obj.put("msg", StringUtil.isEmpty(message) ? MsgCode.PS010.get() : message);
                if (user != null) {
                    OnlineUserProvider.sendMessage(user, obj);
                }
            }
        }

    }

    public void majorStandFreshUser() {
        UserInfo userInfo = UserProvider.getUser();
        List<OnlineUserModel> onlineUserList = OnlineUserProvider.getOnlineUserList();
        for (OnlineUserModel onlineUserModel : onlineUserList) {
            String userId = onlineUserModel.getUserId();
            //当前用户，同平台的其他客户端需要强制刷新
            if (userInfo.getUserId().equals(userId)
                    && Objects.equals(!RequestContext.isOrignPc(), onlineUserModel.getIsMobileDevice())) {
                JSONObject obj = new JSONObject();
                obj.put(KeyConst.METHOD, "refresh");
                obj.put("msg", MsgCode.PS010.get());
                OnlineUserProvider.sendMessage(onlineUserModel, obj);
            }
        }
    }

    public Boolean logoutUser(String message, List<String> userIds) {
        if (userIds.isEmpty()) {
            return true;
        }
        //用户不强制下线
        List<OnlineUserModel> users = new ArrayList<>();
        List<OnlineUserModel> onlineUserList = OnlineUserProvider.getOnlineUserList();
        for (OnlineUserModel onlineUserModel : onlineUserList) {
            String userId = onlineUserModel.getUserId();
            if (userIds.contains(userId) && !Objects.equals(UserProvider.getUser().getToken(), onlineUserModel.getToken())) {
                users.add(onlineUserModel);
            }
        }
        if (!ObjectUtils.isEmpty(users)) {
            for (OnlineUserModel user : users) {
                JSONObject obj = new JSONObject();
                obj.put(KeyConst.METHOD, "logout");
                obj.put("msg", StringUtil.isEmpty(message) ? MsgCode.PS011.get() : message);
                if (user != null) {
                    OnlineUserProvider.sendMessage(user, obj);
                }
                //先移除对象， 并推送下线信息， 避免网络原因导致就用户未断开 新用户连不上WebSocket
                OnlineUserProvider.removeModel(user);
                if (null!=user){
                    UserProvider.logoutByToken(user.getToken());
                }

            }
        }
        return true;
    }

}
