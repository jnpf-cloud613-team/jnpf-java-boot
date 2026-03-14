package jnpf.base.service.impl;

import jnpf.base.entity.SystemTopEntity;
import jnpf.base.mapper.SystemTopMapper;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.SystemTopService;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用置顶ServiceImpl
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025-09-05
 */
@Service
public class SystemTopServiceImpl extends SuperServiceImpl<SystemTopMapper, SystemTopEntity> implements SystemTopService {

    @Override
    public void saveTop(SystemTopEntity entity, List<String> hasSysIds) {
        List<SystemTopEntity> list = this.baseMapper.getList(entity.getUserId(), entity.getType(), entity.getStandId());
        List<String> realBaseSysIds = new ArrayList<>();//实际有置顶的应用id
        //移除当前列表没有的脏数据（没有权限的数据）
        for (SystemTopEntity item : list) {
            if (hasSysIds.contains(item.getObjectId())) {
                realBaseSysIds.add(item.getObjectId());
            } else {
                this.baseMapper.deleteById(item);
            }
        }
        //置顶
        if (realBaseSysIds.size() >= 3) {
            throw new DataException(MsgCode.SYS108.get());
        }
        if (!realBaseSysIds.contains(entity.getObjectId())) {
            entity.setId(RandomUtil.uuId());
            this.baseMapper.insert(entity);
        }
    }

    @Override
    public void canleTop(SystemTopEntity entity) {
        List<SystemTopEntity> list = this.baseMapper.getList(entity.getUserId(), entity.getType(), entity.getStandId());
        SystemTopEntity delEntity = list.stream().filter(item -> item.getObjectId().equals(entity.getObjectId())).findFirst().orElse(null);
        if (delEntity != null) this.baseMapper.deleteById(delEntity);
    }

    @Override
    public List<String> getObjectIdList(String type, String standId) {
        return this.baseMapper.getList(UserProvider.getUser().getUserId(), type, standId).stream().map(SystemTopEntity::getObjectId).collect(Collectors.toList());
    }

}
