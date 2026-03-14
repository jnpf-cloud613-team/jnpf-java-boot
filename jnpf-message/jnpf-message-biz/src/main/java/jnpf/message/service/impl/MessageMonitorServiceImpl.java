package jnpf.message.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import jnpf.base.service.SuperServiceImpl;
import jnpf.message.entity.MessageMonitorEntity;
import jnpf.message.mapper.MessageMonitorMapper;
import jnpf.message.model.messagemonitor.MessageMonitorPagination;
import jnpf.message.service.MessageMonitorService;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 消息监控
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-22
 */
@Service
@RequiredArgsConstructor
public class MessageMonitorServiceImpl extends SuperServiceImpl<MessageMonitorMapper, MessageMonitorEntity> implements MessageMonitorService {

    private final UserService userService;

    @Override
    public List<MessageMonitorEntity> getList(MessageMonitorPagination messageMonitorPagination) {
        return this.baseMapper.getList(messageMonitorPagination);
    }

    @Override
    public List<MessageMonitorEntity> getTypeList(MessageMonitorPagination messageMonitorPagination, String dataType) {
        return this.baseMapper.getTypeList(messageMonitorPagination, dataType);
    }


    @Override
    public MessageMonitorEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public void create(MessageMonitorEntity entity) {
        this.save(entity);
    }

    @Override
    public boolean update(String id, MessageMonitorEntity entity) {
        entity.setId(id);
        return this.updateById(entity);
    }

    @Override
    public void delete(MessageMonitorEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
        }
    }

    @Override
    public void emptyMonitor() {
        this.baseMapper.emptyMonitor();
    }

    @Override
    @DSTransactional
    public boolean delete(String[] ids) {
        this.baseMapper.delete(ids);
        return true;
    }

    /**
     * 用户id转名称(多选)
     *
     * @param ids
     * @return
     */
    @Override
    public String userSelectValues(String ids) {
        if (StringUtil.isEmpty(ids)) {
            return ids;
        }
        if (ids.contains("[")) {
            List<String> nameList = new ArrayList<>();
            List<String> jsonToList = JsonUtil.getJsonToList(ids, String.class);
            for (String userId : jsonToList) {
                UserEntity info = userService.getInfo(userId);
                nameList.add(Objects.nonNull(info) ? info.getRealName() + "/" + info.getAccount() : userId);
            }
            return String.join(";", nameList);
        } else {
            List<String> userInfoList = new ArrayList<>();
            String[] idList = ids.split(",");
            if (idList.length > 0) {
                for (String id : idList) {
                    UserEntity userEntity = userService.getInfo(id);
                    if (ObjectUtil.isNotEmpty(userEntity)) {
                        String info = userEntity.getRealName() + "/" + userEntity.getAccount();
                        userInfoList.add(info);
                    }
                }
            }
            return String.join("-", userInfoList);
        }
    }
}
