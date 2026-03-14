package jnpf.base.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.base.Pagination;
import jnpf.base.entity.DataInterfaceLogEntity;
import jnpf.base.mapper.DataInterfaceLogMapper;
import jnpf.base.model.interfaceoauth.PaginationIntrfaceLog;
import jnpf.base.service.DataInterfaceLogService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-06-03
 */
@Service
public class DataInterfaceLogServiceImpl extends SuperServiceImpl<DataInterfaceLogMapper, DataInterfaceLogEntity> implements DataInterfaceLogService {

    @Override
    public void create(String dateInterfaceId, Integer invokWasteTime) {
       this.baseMapper.create(dateInterfaceId, invokWasteTime);
    }
    @Override
    public void create(String dateInterfaceId, Integer invokWasteTime,String appId,String invokType) {
        this.baseMapper.create(dateInterfaceId, invokWasteTime,appId,invokType);
    }

    @Override
    public List<DataInterfaceLogEntity> getList(String invokId, Pagination pagination) {
        return this.baseMapper.getList(invokId, pagination);
    }

    @Override
    public List<DataInterfaceLogEntity> getListByIds(String appId,List<String> invokIds, PaginationIntrfaceLog pagination) {
        return this.baseMapper.getListByIds(appId,invokIds,pagination);
    }

}
